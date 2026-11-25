package edu.cit.checkedin.password;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import edu.cit.checkedin.auth.SessionService;
import edu.cit.checkedin.auth.dto.CurrentUserResponse;
import edu.cit.checkedin.password.dto.ChangePasswordRequest;
import edu.cit.checkedin.password.dto.ForgotPasswordRequest;
import edu.cit.checkedin.password.dto.PasswordSignInRequest;
import edu.cit.checkedin.password.dto.ResetPasswordRequest;

/**
 * Password endpoints (Increment 2 / SRS v1.4).
 *
 * POST /api/auth/password/sign-in { email, password } -> the signed-in user, and the same 8-hour session cookie as
 *      the other sign-in methods (NFR-009). 401 "Email or password is incorrect." / 429 after too many wrong tries.
 * POST /api/auth/password/forgot { email } -> 204 always the same (emails a reset link only if the account exists)
 * POST /api/auth/password/reset { token, newPassword, confirmPassword } -> 204, or 400 for a used/expired/unknown link
 * PUT  /api/profile/password { currentPassword, newPassword, confirmPassword } -> 204 (signed-in user only)
 * 204 No Content: nothing to send back; the page shows its own message.
 */
@RestController
public class PasswordController {

    private final PasswordSignInService passwordSignInService;
    private final PasswordResetService passwordResetService;
    private final PasswordChangeService passwordChangeService;
    private final SessionService sessionService;

    public PasswordController(PasswordSignInService passwordSignInService, PasswordResetService passwordResetService,
                              PasswordChangeService passwordChangeService, SessionService sessionService) {
        this.passwordSignInService = passwordSignInService;
        this.passwordResetService = passwordResetService;
        this.passwordChangeService = passwordChangeService;
        this.sessionService = sessionService;
    }

    @PostMapping("/api/auth/password/sign-in")
    public CurrentUserResponse signIn(@Valid @RequestBody PasswordSignInRequest request,
                                      HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        CurrentUserResponse currentUser = passwordSignInService.signIn(request.email(), request.password());
        sessionService.startSession(currentUser.email(), httpRequest, httpResponse);
        return currentUser;
    }

    @PostMapping("/api/auth/password/forgot")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request.email());
    }

    @PostMapping("/api/auth/password/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
    }

    @PutMapping("/api/profile/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request, Authentication authentication) {
        passwordChangeService.changePassword(authentication.getName(), request);
    }
}
