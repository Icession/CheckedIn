package edu.cit.checkedin.password;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import edu.cit.checkedin.auth.AuthService;
import edu.cit.checkedin.auth.User;
import edu.cit.checkedin.auth.UserRepository;
import edu.cit.checkedin.password.dto.ChangePasswordRequest;

/**
 * Change password on the profile page (Increment 2 / SRS v1.4). The signed-in user proves the current password
 * (BCrypt check), then the new one must follow the rules, match "Confirm new password" and differ from the current
 * one. Afterwards the old password no longer works and every open reset link is used up. The session stays.
 */
@Service
public class PasswordChangeService {

    static final String NO_PASSWORD_YET =
            "Your account has no password yet. Use \"Forgot password?\" on the sign-in page to set one.";
    static final String WRONG_CURRENT_PASSWORD = "Your current password is incorrect. Please try again.";
    static final String SAME_AS_CURRENT = "Your new password must be different from your current one.";

    private final AuthService authService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetService passwordResetService;
    private final Clock clock;

    public PasswordChangeService(AuthService authService, UserRepository userRepository, PasswordEncoder passwordEncoder,
                                 PasswordResetService passwordResetService, Clock clock) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordResetService = passwordResetService;
        this.clock = clock;
    }

    /** Changes the password of the signed-in user (verifiedEmail comes from the session, never from the browser). */
    @Transactional
    public void changePassword(String verifiedEmail, ChangePasswordRequest request) {
        User user = authService.requireAccount(verifiedEmail);
        if (user.getPasswordHash() == null) {
            throw badRequest(NO_PASSWORD_YET);
        }
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw badRequest(WRONG_CURRENT_PASSWORD);
        }
        PasswordRules.checkNotEmailAndConfirmed(request.newPassword(), request.confirmPassword(), verifiedEmail);
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw badRequest(SAME_AS_CURRENT);
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        passwordResetService.useUpOpenLinks(user, LocalDateTime.now(clock));
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
