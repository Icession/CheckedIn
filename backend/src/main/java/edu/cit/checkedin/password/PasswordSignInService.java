package edu.cit.checkedin.password;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import edu.cit.checkedin.auth.AuthService;
import edu.cit.checkedin.auth.EmailAddressChecker;
import edu.cit.checkedin.auth.User;
import edu.cit.checkedin.auth.UserRepository;
import edu.cit.checkedin.auth.dto.CurrentUserResponse;

/**
 * Password sign-in (Increment 2 / SRS v1.4), a third way to sign in next to the email code and GitHub (FR-001).
 * - A wrong email, a wrong password and an account without a password all get the same answer, so nobody can
 *   find out which emails have accounts. A missing account even costs the same BCrypt work as a wrong password.
 * - After 5 wrong passwords for one email, password sign-in for that email is blocked for 15 minutes
 *   (PasswordLockout, numbers in application.properties). The email code and GitHub keep working.
 * Deliberately NOT @Transactional: a wrong try must stay saved even though an error is thrown afterwards.
 */
@Service
public class PasswordSignInService {

    static final String WRONG_EMAIL_OR_PASSWORD = "Email or password is incorrect.";

    private final UserRepository userRepository;
    private final PasswordLockoutRepository lockoutRepository;
    private final EmailAddressChecker emailAddressChecker;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final PasswordSettings settings;
    private final Clock clock;
    // A real BCrypt hash to check against when an email has no password, so that case takes as long as a real check
    private final String hashForEqualTiming;

    public PasswordSignInService(UserRepository userRepository, PasswordLockoutRepository lockoutRepository,
                                 EmailAddressChecker emailAddressChecker, PasswordEncoder passwordEncoder,
                                 AuthService authService, PasswordSettings settings, Clock clock) {
        this.userRepository = userRepository;
        this.lockoutRepository = lockoutRepository;
        this.emailAddressChecker = emailAddressChecker;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
        this.settings = settings;
        this.clock = clock;
        this.hashForEqualTiming = passwordEncoder.encode("not-a-real-password-1");
    }

    /** Checks the email and password; on success returns the signed-in user (the controller starts the session). */
    public CurrentUserResponse signIn(String email, String password) {
        String normalizedEmail = emailAddressChecker.normalize(email);
        if (!emailAddressChecker.isAllowed(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please use your CIT-U email address.");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        PasswordLockout lockout = lockoutRepository.findByEmail(normalizedEmail)
                .orElseGet(() -> new PasswordLockout(normalizedEmail));
        if (lockout.isLocked(now)) {
            throw tooManyWrongTries();
        }
        if (!passwordIsCorrect(normalizedEmail, password)) {
            lockout.recordWrongTry(now, settings);
            lockoutRepository.save(lockout);
            throw lockout.isLocked(now) ? tooManyWrongTries()
                    : new ResponseStatusException(HttpStatus.UNAUTHORIZED, WRONG_EMAIL_OR_PASSWORD);
        }
        if (lockout.getLockoutId() != null) {
            lockoutRepository.delete(lockout); // a correct password clears the wrong tries
        }
        return authService.signInWithPassword(normalizedEmail);
    }

    private boolean passwordIsCorrect(String normalizedEmail, String password) {
        String storedHash = userRepository.findByEmail(normalizedEmail).map(User::getPasswordHash).orElse(null);
        if (storedHash == null) {
            passwordEncoder.matches(password, hashForEqualTiming); // same work as a real check, result ignored
            return false;
        }
        return passwordEncoder.matches(password, storedHash);
    }

    private ResponseStatusException tooManyWrongTries() {
        return new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many wrong passwords for this email. "
                + "Password sign-in is blocked for " + settings.lockoutMinutes() + " minutes. "
                + "You can still sign in with an email code or GitHub.");
    }
}
