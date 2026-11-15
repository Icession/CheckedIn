package edu.cit.checkedin.auth;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import edu.cit.checkedin.auth.dto.CodeSentResponse;
import edu.cit.checkedin.integration.EmailSendingException;
import edu.cit.checkedin.integration.EmailService;
import edu.cit.checkedin.integration.EmailTemplate;

/**
 * Creates, emails, and checks one-time 6-digit sign-in codes.
 * Covers FR-001 (sign in with an emailed code), FR-002 (only @cit.edu addresses) and
 * NFR-008 (codes stored only as hashes, expire after 10 minutes, 5 wrong tries, work once,
 * at most 3 codes per email every 15 minutes).
 */
@Service
public class LoginCodeService {

    private final LoginCodeRepository loginCodeRepository;
    private final EmailService emailService;
    private final EmailAddressChecker emailAddressChecker;
    private final PasswordEncoder passwordEncoder;
    private final LoginCodeSettings settings;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public LoginCodeService(LoginCodeRepository loginCodeRepository, EmailService emailService,
                            EmailAddressChecker emailAddressChecker, PasswordEncoder passwordEncoder,
                            LoginCodeSettings settings, Clock clock) {
        this.loginCodeRepository = loginCodeRepository;
        this.emailService = emailService;
        this.emailAddressChecker = emailAddressChecker;
        this.passwordEncoder = passwordEncoder;
        this.settings = settings;
        this.clock = clock;
    }

    /** Creates a new code, saves only its hash, and emails the code to the user. */
    public CodeSentResponse sendCode(String email) {
        String normalizedEmail = emailAddressChecker.normalize(email);
        if (!emailAddressChecker.isAllowed(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please use your CIT-U email address.");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        checkRequestLimit(normalizedEmail, now);

        String code = generateCode();
        LoginCode loginCode = loginCodeRepository.save(new LoginCode(normalizedEmail,
                passwordEncoder.encode(code), now, now.plusMinutes(settings.expiryMinutes())));
        emailCode(loginCode, code);
        return new CodeSentResponse(normalizedEmail, settings.expiryMinutes());
    }

    /**
     * Checks the code the user typed against the newest code sent to that email, and marks it used.
     * Returns the verified (normalized) email. Otherwise it throws with a message that tells the user
     * what to do. "noRollbackFor" keeps the wrong-try count saved even though we throw an error,
     * so nobody can get around the 5-try limit.
     */
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public String checkCode(String email, String code) {
        String normalizedEmail = emailAddressChecker.normalize(email);
        LoginCode loginCode = loginCodeRepository.findFirstByEmailOrderByCodeIdDesc(normalizedEmail)
                .orElseThrow(() -> badRequest("We have no sign-in code for this email. Click \"Send code\" first."));
        LocalDateTime now = LocalDateTime.now(clock);
        checkCodeCanStillBeUsed(loginCode, now);

        if (!passwordEncoder.matches(code, loginCode.getCodeHash())) {
            loginCode.recordWrongTry();
            throw badRequest(wrongCodeMessage(loginCode));
        }
        loginCode.markUsed(now);
        return normalizedEmail;
    }

    private void checkRequestLimit(String normalizedEmail, LocalDateTime now) {
        LocalDateTime windowStart = now.minusMinutes(settings.requestWindowMinutes());
        long recentRequests = loginCodeRepository.countByEmailAndCreatedAtAfter(normalizedEmail, windowStart);
        if (recentRequests >= settings.maxRequests()) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "You asked for " + settings.maxRequests() + " codes in the last "
                            + settings.requestWindowMinutes() + " minutes. Please use the newest code we sent,"
                            + " or wait a few minutes and try again.");
        }
    }

    // A random number from 000000 to 999999, always written with 6 digits
    private String generateCode() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    private void emailCode(LoginCode loginCode, String code) {
        try {
            emailService.send(loginCode.getEmail(), EmailTemplate.LOGIN_CODE,
                    Map.of("code", code, "minutes", String.valueOf(settings.expiryMinutes())));
        } catch (EmailSendingException exception) {
            // The email never left, so this code must not count toward the request limit
            loginCodeRepository.delete(loginCode);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "We couldn't send the code right now. Please try again in a few minutes.");
        }
    }

    private void checkCodeCanStillBeUsed(LoginCode loginCode, LocalDateTime now) {
        if (loginCode.isUsed()) {
            throw badRequest("This code was already used. Please ask for a new code.");
        }
        if (loginCode.isExpired(now)) {
            throw badRequest("This code has expired. Please ask for a new code.");
        }
        if (loginCode.getAttemptCount() >= settings.maxWrongTries()) {
            throw badRequest("Too many wrong tries. Please ask for a new code.");
        }
    }

    private String wrongCodeMessage(LoginCode loginCode) {
        int triesLeft = settings.maxWrongTries() - loginCode.getAttemptCount();
        if (triesLeft <= 0) {
            return "Wrong code. That was your last try, so please ask for a new code.";
        }
        return "Wrong code. You have " + triesLeft + (triesLeft == 1 ? " try" : " tries") + " left.";
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
