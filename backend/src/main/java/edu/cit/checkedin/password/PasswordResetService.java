package edu.cit.checkedin.password;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import edu.cit.checkedin.auth.EmailAddressChecker;
import edu.cit.checkedin.auth.User;
import edu.cit.checkedin.auth.UserRepository;
import edu.cit.checkedin.integration.EmailSendingException;
import edu.cit.checkedin.integration.EmailService;
import edu.cit.checkedin.integration.EmailTemplate;
import edu.cit.checkedin.password.dto.ResetPasswordRequest;

/**
 * "Forgot password?" (Increment 2 / SRS v1.4): emails a single-use reset link, then saves the new password.
 * This is also how an account from before Increment 2 sets its first password.
 * - The answer to "send me a link" is always the same, so it never shows whether an email has an account. Over the
 *   limit (3 links per 15 minutes) nothing is sent, also silently.
 * - The link holds a random 256-bit token; only its SHA-256 hash is stored (PasswordResetToken), 30 minutes, once.
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    static final String UNKNOWN_LINK = "This reset link is not valid. Please copy the whole link from the email, or ask for a new one.";
    static final String USED_LINK = "This reset link was already used, or your password was changed since. Please ask for a new one.";
    static final String EXPIRED_LINK = "This reset link has expired. Please ask for a new one.";

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordLockoutRepository lockoutRepository;
    private final EmailService emailService;
    private final EmailAddressChecker emailAddressChecker;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetSettings settings;
    private final Clock clock;
    private final String frontendUrl;

    public PasswordResetService(UserRepository userRepository, PasswordResetTokenRepository tokenRepository,
                                PasswordLockoutRepository lockoutRepository, EmailService emailService,
                                EmailAddressChecker emailAddressChecker, PasswordEncoder passwordEncoder,
                                PasswordResetSettings settings, Clock clock,
                                @Value("${checkedin.frontend-url}") String frontendUrl) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.lockoutRepository = lockoutRepository;
        this.emailService = emailService;
        this.emailAddressChecker = emailAddressChecker;
        this.passwordEncoder = passwordEncoder;
        this.settings = settings;
        this.clock = clock;
        this.frontendUrl = frontendUrl;
    }

    /** Emails a reset link if the email has an account and is under the limit; otherwise does nothing, silently. */
    @Transactional
    public void requestReset(String email) {
        String normalizedEmail = emailAddressChecker.normalize(email);
        if (!emailAddressChecker.isAllowed(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please use your CIT-U email address.");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        Optional<User> user = userRepository.findByEmail(normalizedEmail);
        if (user.isEmpty() || asked(user.get(), now) >= settings.maxRequests()) {
            return;
        }
        String token = newToken();
        tokenRepository.save(new PasswordResetToken(user.get(), hashOf(token), now, now.plusMinutes(settings.expiryMinutes())));
        sendLink(normalizedEmail, token);
    }

    /** Saves the new password from a reset link, then uses up every open link of that user. */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        LocalDateTime now = LocalDateTime.now(clock);
        PasswordResetToken resetToken = tokenRepository.findByTokenHash(hashOf(request.token()))
                .orElseThrow(() -> badRequest(UNKNOWN_LINK));
        if (resetToken.isUsed()) {
            throw badRequest(USED_LINK);
        }
        if (resetToken.isExpired(now)) {
            throw badRequest(EXPIRED_LINK);
        }
        User user = resetToken.getUser();
        PasswordRules.checkNotEmailAndConfirmed(request.newPassword(), request.confirmPassword(), user.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        useUpOpenLinks(user, now);
        // Proving the email again also lifts a password sign-in lockout
        lockoutRepository.findByEmail(user.getEmail()).ifPresent(lockoutRepository::delete);
    }

    /** Marks every open reset link of the user as used (after a reset, and after a password change in Step 4). */
    public void useUpOpenLinks(User user, LocalDateTime now) {
        tokenRepository.findByUserAndUsedAtIsNull(user).forEach(openLink -> openLink.markUsed(now));
    }

    /** SHA-256 of a token, as 64 hex characters. */
    static String hashOf(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("Java always has SHA-256", impossible);
        }
    }

    private long asked(User user, LocalDateTime now) {
        return tokenRepository.countByUserAndCreatedAtAfter(user, now.minusMinutes(settings.requestWindowMinutes()));
    }

    // 32 random bytes, written with URL-safe letters (43 characters)
    private static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // A failed email is only logged (never the link): telling the browser would show that the account exists
    private void sendLink(String email, String token) {
        String link = frontendUrl + "/reset-password?token=" + token;
        try {
            emailService.send(email, EmailTemplate.PASSWORD_RESET,
                    Map.of("link", link, "minutes", String.valueOf(settings.expiryMinutes())));
        } catch (EmailSendingException exception) {
            log.warn("Could not email a password reset link: {}", exception.getMessage());
        }
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
