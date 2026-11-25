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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import edu.cit.checkedin.auth.AuthService;
import edu.cit.checkedin.auth.Role;
import edu.cit.checkedin.auth.User;
import edu.cit.checkedin.auth.UserRepository;
import edu.cit.checkedin.password.dto.ChangePasswordRequest;

/** Tests "Change password" on the profile page (Increment 2 / SRS v1.4) with a fake database and real BCrypt. */
@ExtendWith(MockitoExtension.class)
class PasswordChangeServiceTest {

    private static final ZoneId MANILA = ZoneId.of("Asia/Manila");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 25, 21, 30);
    private static final String EMAIL = "maria.santos@cit.edu";
    private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder(4);

    @Mock
    private AuthService authService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordResetService passwordResetService;
    private PasswordChangeService service;

    @BeforeEach
    void setUp() {
        service = new PasswordChangeService(authService, userRepository, PASSWORD_ENCODER, passwordResetService,
                Clock.fixed(NOW.atZone(MANILA).toInstant(), MANILA));
    }

    @Test
    void afterAChangeOnlyTheNewPasswordWorksAndOpenResetLinksAreUsedUp() {
        User maria = mariaWithPassword("sunflower42");
        when(authService.requireAccount(EMAIL)).thenReturn(maria);

        service.changePassword(EMAIL, new ChangePasswordRequest("sunflower42", "moonlight77", "moonlight77"));

        assertThat(PASSWORD_ENCODER.matches("moonlight77", maria.getPasswordHash())).isTrue();
        assertThat(PASSWORD_ENCODER.matches("sunflower42", maria.getPasswordHash())).isFalse();
        verify(userRepository).save(maria);
        verify(passwordResetService).useUpOpenLinks(maria, NOW);
    }

    @Test
    void aWrongCurrentPasswordChangesNothing() {
        when(authService.requireAccount(EMAIL)).thenReturn(mariaWithPassword("sunflower42"));

        assertRefused(new ChangePasswordRequest("sunflower43", "moonlight77", "moonlight77"),
                "Your current password is incorrect. Please try again.");
    }

    @Test
    void theNewPasswordMustDifferFromTheCurrentOneAndBeTypedTheSameTwice() {
        when(authService.requireAccount(EMAIL)).thenReturn(mariaWithPassword("sunflower42"));

        assertRefused(new ChangePasswordRequest("sunflower42", "sunflower42", "sunflower42"),
                "Your new password must be different from your current one.");
        assertRefused(new ChangePasswordRequest("sunflower42", "moonlight77", "moonlight78"),
                "The two passwords don't match. Please type the same password in both boxes.");
        assertRefused(new ChangePasswordRequest("sunflower42", EMAIL, EMAIL),
                "Your password can't be your email address. Please choose another one.");
    }

    @Test
    void anAccountWithoutAPasswordIsSentToForgotPassword() {
        when(authService.requireAccount(EMAIL)).thenReturn(new User(EMAIL, "Maria", "Santos", Role.PROFESSOR));

        assertRefused(new ChangePasswordRequest("anything1", "moonlight77", "moonlight77"),
                "Your account has no password yet. Use \"Forgot password?\" on the sign-in page to set one.");
    }

    private void assertRefused(ChangePasswordRequest request, String message) {
        assertThatThrownBy(() -> service.changePassword(EMAIL, request)).hasMessageContaining(message);
        verify(userRepository, never()).save(any());
        verify(passwordResetService, never()).useUpOpenLinks(any(), any());
    }

    private static User mariaWithPassword(String password) {
        User maria = new User(EMAIL, "Maria", "Santos", Role.PROFESSOR);
        maria.setPasswordHash(PASSWORD_ENCODER.encode(password));
        return maria;
    }
}
