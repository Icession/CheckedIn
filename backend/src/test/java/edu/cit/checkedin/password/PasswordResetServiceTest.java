package edu.cit.checkedin.password;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import edu.cit.checkedin.auth.EmailAddressChecker;
import edu.cit.checkedin.auth.Role;
import edu.cit.checkedin.auth.User;
import edu.cit.checkedin.auth.UserRepository;
import edu.cit.checkedin.integration.EmailService;
import edu.cit.checkedin.integration.EmailTemplate;
import edu.cit.checkedin.password.dto.ResetPasswordRequest;

/** Tests "Forgot password?" (Increment 2 / SRS v1.4) with a fake database, a fake email service and real BCrypt. */
@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    private static final ZoneId MANILA = ZoneId.of("Asia/Manila");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 25, 21, 30);
    private static final String EMAIL = "juan.delacruz@cit.edu";
    private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder(4);

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordResetTokenRepository tokenRepository;
    @Mock
    private PasswordLockoutRepository lockoutRepository;
    @Mock
    private EmailService emailService;
    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        service = new PasswordResetService(userRepository, tokenRepository, lockoutRepository, emailService,
                new EmailAddressChecker(List.of("cit.edu")), PASSWORD_ENCODER, new PasswordResetSettings(30, 3, 15),
                Clock.fixed(NOW.atZone(MANILA).toInstant(), MANILA), "http://localhost:5173");
    }

    // ---------- Asking for a link ----------

    @Test
    void anEmailWithoutAnAccountGetsNoLinkAndTheSameQuietAnswer() {
        when(userRepository.findByEmail("no.account@cit.edu")).thenReturn(Optional.empty());

        service.requestReset("no.account@cit.edu"); // no error: the answer never shows whether an account exists

        verify(tokenRepository, never()).save(any());
        verifyNoInteractions(emailService);
    }

    @Test
    void anAccountGetsASingleUseLinkAndOnlyTheHashOfItsTokenIsStored() {
        User juan = juan();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(juan));
        when(tokenRepository.countByUserAndCreatedAtAfter(juan, NOW.minusMinutes(15))).thenReturn(0L);

        service.requestReset("  Juan.DelaCruz@CIT.edu ");

        ArgumentCaptor<PasswordResetToken> saved = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(saved.capture());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> emailData = ArgumentCaptor.forClass(Map.class);
        verify(emailService).send(eq(EMAIL), eq(EmailTemplate.PASSWORD_RESET), emailData.capture());
        String link = emailData.getValue().get("link");
        String token = link.substring(link.indexOf("token=") + "token=".length());

        assertThat(link).startsWith("http://localhost:5173/reset-password?token=");
        assertThat(token).hasSizeGreaterThanOrEqualTo(43); // 256 random bits
        assertThat(saved.getValue().getTokenHash()).hasSize(64).isEqualTo(PasswordResetService.hashOf(token)).isNotEqualTo(token);
        assertThat(saved.getValue().getExpiresAt()).isEqualTo(NOW.plusMinutes(30));
        assertThat(emailData.getValue()).containsEntry("minutes", "30");
    }

    @Test
    void afterThreeLinksInFifteenMinutesNothingMoreIsSentAlsoQuietly() {
        User juan = juan();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(juan));
        when(tokenRepository.countByUserAndCreatedAtAfter(juan, NOW.minusMinutes(15))).thenReturn(3L);

        service.requestReset(EMAIL);

        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).send(any(), any(), anyMap());
    }

    // ---------- Using a link ----------

    @Test
    void aValidLinkSetsThePasswordEvenAnAccountsFirstOneAndUsesUpEveryOpenLink() {
        User juan = juan(); // no password yet, like an account from before Increment 2
        PasswordResetToken link = openLink(juan, "token-from-the-email", NOW.plusMinutes(10));
        PasswordResetToken olderLink = openLink(juan, "an-older-token", NOW.plusMinutes(5));
        when(tokenRepository.findByTokenHash(PasswordResetService.hashOf("token-from-the-email"))).thenReturn(Optional.of(link));
        when(tokenRepository.findByUserAndUsedAtIsNull(juan)).thenReturn(List.of(link, olderLink));
        PasswordLockout lockout = new PasswordLockout(EMAIL);
        when(lockoutRepository.findByEmail(EMAIL)).thenReturn(Optional.of(lockout));

        service.resetPassword(new ResetPasswordRequest("token-from-the-email", "sunflower42", "sunflower42"));

        assertThat(PASSWORD_ENCODER.matches("sunflower42", juan.getPasswordHash())).isTrue();
        verify(userRepository).save(juan);
        assertThat(link.getUsedAt()).isEqualTo(NOW);
        assertThat(olderLink.getUsedAt()).isEqualTo(NOW);
        verify(lockoutRepository).delete(lockout); // proving the email again lifts a lockout
    }

    @Test
    void usedExpiredAndUnknownLinksGetClearMessages() {
        User juan = juan();
        PasswordResetToken used = openLink(juan, "used-token", NOW.plusMinutes(10));
        used.markUsed(NOW.minusMinutes(1));
        when(tokenRepository.findByTokenHash(PasswordResetService.hashOf("used-token"))).thenReturn(Optional.of(used));
        when(tokenRepository.findByTokenHash(PasswordResetService.hashOf("old-token")))
                .thenReturn(Optional.of(openLink(juan, "old-token", NOW.minusMinutes(1))));
        when(tokenRepository.findByTokenHash(PasswordResetService.hashOf("made-up-token"))).thenReturn(Optional.empty());

        assertResetRefused("used-token", "This reset link was already used, or your password was changed since.");
        assertResetRefused("old-token", "This reset link has expired.");
        assertResetRefused("made-up-token", "This reset link is not valid.");
        verify(userRepository, never()).save(any());
    }

    @Test
    void theNewPasswordCantBeTheEmailAndMustBeTypedTheSameTwice() {
        User juan = juan();
        when(tokenRepository.findByTokenHash(PasswordResetService.hashOf("good-token")))
                .thenReturn(Optional.of(openLink(juan, "good-token", NOW.plusMinutes(10))));

        assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequest("good-token", EMAIL, EMAIL)))
                .hasMessageContaining("can't be your email address");
        assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequest("good-token", "sunflower42", "sunflower43")))
                .hasMessageContaining("don't match");
        assertThat(juan.getPasswordHash()).isNull();
    }

    private void assertResetRefused(String token, String messageStart) {
        assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequest(token, "sunflower42", "sunflower42")))
                .hasMessageContaining(messageStart);
    }

    private static PasswordResetToken openLink(User user, String token, LocalDateTime expiresAt) {
        return new PasswordResetToken(user, PasswordResetService.hashOf(token), expiresAt.minusMinutes(30), expiresAt);
    }

    private static User juan() {
        return new User(EMAIL, "Juan", "Dela Cruz", Role.STUDENT);
    }
}
