package edu.cit.checkedin.password;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import edu.cit.checkedin.auth.AuthService;
import edu.cit.checkedin.auth.EmailAddressChecker;
import edu.cit.checkedin.auth.Role;
import edu.cit.checkedin.auth.User;
import edu.cit.checkedin.auth.UserRepository;
import edu.cit.checkedin.auth.dto.CurrentUserResponse;

/** Tests password sign-in (Increment 2 / SRS v1.4) with a fake database and a real (fast) BCrypt. */
@ExtendWith(MockitoExtension.class)
class PasswordSignInServiceTest {

    private static final ZoneId MANILA = ZoneId.of("Asia/Manila");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 25, 21, 0);
    private static final String EMAIL = "juan.delacruz@cit.edu";
    private static final String PASSWORD = "sunflower42";
    private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder(4);

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordLockoutRepository lockoutRepository;
    @Mock
    private AuthService authService;
    private PasswordSignInService service;

    @BeforeEach
    void setUp() {
        service = new PasswordSignInService(userRepository, lockoutRepository, new EmailAddressChecker(List.of("cit.edu")),
                PASSWORD_ENCODER, authService, new PasswordSettings(5, 15), Clock.fixed(NOW.atZone(MANILA).toInstant(), MANILA));
    }

    @Test
    void theRightPasswordSignsInWithTheCleanedUpEmail() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(juanWithPassword()));
        when(lockoutRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        CurrentUserResponse juan = new CurrentUserResponse(EMAIL, "Juan", "Cruz", Role.STUDENT, "22-1234-567", false, false);
        when(authService.signInWithPassword(EMAIL)).thenReturn(juan);

        assertThat(service.signIn("  Juan.DelaCruz@CIT.edu ", PASSWORD)).isEqualTo(juan);
    }

    @Test
    void aWrongPasswordAWrongEmailAndAnAccountWithoutPasswordGetTheSameMessage() {
        when(lockoutRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(juanWithPassword()));
        when(userRepository.findByEmail("no.account@cit.edu")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("no.password@cit.edu")).thenReturn(Optional.of(
                new User("no.password@cit.edu", "Ana", "Reyes", Role.STUDENT)));

        assertWrongEmailOrPassword(EMAIL, "sunflower43");
        assertWrongEmailOrPassword("no.account@cit.edu", PASSWORD);
        assertWrongEmailOrPassword("no.password@cit.edu", PASSWORD);
        verify(authService, never()).signInWithPassword(any());
        verify(lockoutRepository, org.mockito.Mockito.times(3)).save(any()); // each wrong try is counted
    }

    @Test
    void theFifthWrongPasswordBlocksPasswordSignIn() {
        PasswordLockout fourWrongTries = new PasswordLockout(EMAIL);
        for (int i = 0; i < 4; i++) {
            fourWrongTries.recordWrongTry(NOW.minusMinutes(1), new PasswordSettings(5, 15));
        }
        when(lockoutRepository.findByEmail(EMAIL)).thenReturn(Optional.of(fourWrongTries));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(juanWithPassword()));

        assertThatThrownBy(() -> service.signIn(EMAIL, "sunflower43"))
                .isInstanceOfSatisfying(ResponseStatusException.class, error -> {
                    assertThat(error.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                    assertThat(error.getReason()).startsWith("Too many wrong passwords for this email. "
                            + "Password sign-in is blocked for 15 minutes.");
                });
    }

    @Test
    void whileBlockedEvenTheRightPasswordIsRefusedWithoutBeingChecked() {
        PasswordLockout blocked = new PasswordLockout(EMAIL);
        for (int i = 0; i < 5; i++) {
            blocked.recordWrongTry(NOW.minusMinutes(2), new PasswordSettings(5, 15));
        }
        when(lockoutRepository.findByEmail(EMAIL)).thenReturn(Optional.of(blocked));

        assertThatThrownBy(() -> service.signIn(EMAIL, PASSWORD)).hasMessageContaining("Too many wrong passwords");
        verify(userRepository, never()).findByEmail(any());
        verify(authService, never()).signInWithPassword(any());
    }

    @Test
    void onlyCitEmailsCanSignIn() {
        assertThatThrownBy(() -> service.signIn("juan@gmail.com", PASSWORD))
                .hasMessageContaining("Please use your CIT-U email address.");
    }

    private void assertWrongEmailOrPassword(String email, String password) {
        assertThatThrownBy(() -> service.signIn(email, password))
                .isInstanceOfSatisfying(ResponseStatusException.class, error -> {
                    assertThat(error.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(error.getReason()).isEqualTo("Email or password is incorrect.");
                });
    }

    private static User juanWithPassword() {
        User juan = new User(EMAIL, "Juan", "Cruz", Role.STUDENT);
        juan.setPasswordHash(PASSWORD_ENCODER.encode(PASSWORD));
        return juan;
    }
}
