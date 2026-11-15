package edu.cit.checkedin.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
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

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import edu.cit.checkedin.auth.dto.CodeSentResponse;
import edu.cit.checkedin.integration.EmailSendingException;
import edu.cit.checkedin.integration.EmailService;
import edu.cit.checkedin.integration.EmailTemplate;

/**
 * Tests the sign-in code rules (FR-001, FR-002, NFR-008) with a fake database and a fake email
 * service. The clock is fixed at 8:00 AM, so expiry can be tested exactly.
 */
@ExtendWith(MockitoExtension.class)
class LoginCodeServiceTest {

    private static final ZoneId MANILA = ZoneId.of("Asia/Manila");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 11, 8, 0);
    private static final String EMAIL = "juan.delacruz@cit.edu";

    @Mock
    private LoginCodeRepository loginCodeRepository;
    @Mock
    private EmailService emailService;

    // Cost 4 is the lowest BCrypt setting; it keeps the tests fast (the app uses the default, 10)
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private LoginCodeService service;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(NOW.atZone(MANILA).toInstant(), MANILA);
        service = new LoginCodeService(loginCodeRepository, emailService,
                new EmailAddressChecker(List.of("cit.edu")), passwordEncoder,
                new LoginCodeSettings(10, 5, 3, 15), fixedClock);
    }

    // ---------- sendCode ----------

    @Test
    void rejectsEmailsOutsideCitEdu() {
        assertRejected(() -> service.sendCode("juan@gmail.com"),
                HttpStatus.BAD_REQUEST, "Please use your CIT-U email address.");
        verifyNoInteractions(loginCodeRepository, emailService);
    }

    @Test
    void sendsACodeAndStoresOnlyItsHash() {
        when(loginCodeRepository.save(any(LoginCode.class))).thenAnswer(call -> call.getArgument(0));

        CodeSentResponse response = service.sendCode("  Juan.DelaCruz@CIT.edu ");

        ArgumentCaptor<LoginCode> savedCode = ArgumentCaptor.forClass(LoginCode.class);
        verify(loginCodeRepository).save(savedCode.capture());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> emailData = ArgumentCaptor.forClass(Map.class);
        verify(emailService).send(eq(EMAIL), eq(EmailTemplate.LOGIN_CODE), emailData.capture());

        String emailedCode = emailData.getValue().get("code");
        assertThat(emailedCode).matches("\\d{6}");
        assertThat(savedCode.getValue().getCodeHash()).isNotEqualTo(emailedCode);
        assertThat(passwordEncoder.matches(emailedCode, savedCode.getValue().getCodeHash())).isTrue();
        assertThat(savedCode.getValue().getEmail()).isEqualTo(EMAIL);
        assertThat(savedCode.getValue().getExpiresAt()).isEqualTo(NOW.plusMinutes(10));
        assertThat(response).isEqualTo(new CodeSentResponse(EMAIL, 10));
    }

    @Test
    void refusesAFourthCodeWithin15Minutes() {
        when(loginCodeRepository.countByEmailAndCreatedAtAfter(EMAIL, NOW.minusMinutes(15))).thenReturn(3L);

        assertRejected(() -> service.sendCode(EMAIL), HttpStatus.TOO_MANY_REQUESTS, "3 codes in the last 15 minutes");
        verify(loginCodeRepository, never()).save(any());
        verifyNoInteractions(emailService);
    }

    @Test
    void deletesTheCodeWhenTheEmailCannotBeSent() {
        when(loginCodeRepository.save(any(LoginCode.class))).thenAnswer(call -> call.getArgument(0));
        when(emailService.send(anyString(), any(), anyMap())).thenThrow(new EmailSendingException("Brevo is down"));

        assertRejected(() -> service.sendCode(EMAIL), HttpStatus.SERVICE_UNAVAILABLE, "couldn't send the code");
        // Deleted, so this failed attempt does not count toward the 3-codes limit
        verify(loginCodeRepository).delete(any(LoginCode.class));
    }

    // ---------- checkCode ----------

    @Test
    void correctCodeSignsInAndMarksTheCodeUsed() {
        LoginCode loginCode = newestCodeIs("123456", NOW.plusMinutes(9));

        String verifiedEmail = service.checkCode(" JUAN.delacruz@cit.edu", "123456");

        assertThat(verifiedEmail).isEqualTo(EMAIL);
        assertThat(loginCode.isUsed()).isTrue();
    }

    @Test
    void wrongCodeCountsATryAndSaysHowManyAreLeft() {
        LoginCode loginCode = newestCodeIs("123456", NOW.plusMinutes(9));

        assertRejected(() -> service.checkCode(EMAIL, "999999"), HttpStatus.BAD_REQUEST, "You have 4 tries left.");
        assertThat(loginCode.getAttemptCount()).isEqualTo(1);
        assertThat(loginCode.isUsed()).isFalse();
    }

    @Test
    void afterFiveWrongTriesEvenTheRightCodeIsRefused() {
        newestCodeIs("123456", NOW.plusMinutes(9));
        for (int wrongTry = 1; wrongTry <= 4; wrongTry++) {
            assertRejected(() -> service.checkCode(EMAIL, "999999"), HttpStatus.BAD_REQUEST, "Wrong code.");
        }
        assertRejected(() -> service.checkCode(EMAIL, "999999"), HttpStatus.BAD_REQUEST, "That was your last try");

        assertRejected(() -> service.checkCode(EMAIL, "123456"), HttpStatus.BAD_REQUEST, "Too many wrong tries");
    }

    @Test
    void expiredCodeIsRefused() {
        newestCodeIs("123456", NOW);

        assertRejected(() -> service.checkCode(EMAIL, "123456"), HttpStatus.BAD_REQUEST, "expired");
    }

    @Test
    void usedCodeIsRefused() {
        LoginCode loginCode = newestCodeIs("123456", NOW.plusMinutes(9));
        loginCode.markUsed(NOW.minusMinutes(1));

        assertRejected(() -> service.checkCode(EMAIL, "123456"), HttpStatus.BAD_REQUEST, "already used");
    }

    @Test
    void refusesWhenNoCodeWasSent() {
        when(loginCodeRepository.findFirstByEmailOrderByCodeIdDesc(EMAIL)).thenReturn(Optional.empty());

        assertRejected(() -> service.checkCode(EMAIL, "123456"), HttpStatus.BAD_REQUEST, "Click \"Send code\" first");
    }

    // ---------- helpers ----------

    /** Makes the fake database return one code (sent a minute ago) as the newest code for EMAIL. */
    private LoginCode newestCodeIs(String code, LocalDateTime expiresAt) {
        LoginCode loginCode = new LoginCode(EMAIL, passwordEncoder.encode(code), NOW.minusMinutes(1), expiresAt);
        when(loginCodeRepository.findFirstByEmailOrderByCodeIdDesc(EMAIL)).thenReturn(Optional.of(loginCode));
        return loginCode;
    }

    private static void assertRejected(ThrowingCallable call, HttpStatus status, String messagePart) {
        assertThatThrownBy(call).isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
            assertThat(exception.getStatusCode()).isEqualTo(status);
            assertThat(exception.getReason()).contains(messagePart);
        });
    }
}
