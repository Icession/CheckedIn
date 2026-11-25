package edu.cit.checkedin.password;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import edu.cit.checkedin.auth.SessionService;
import edu.cit.checkedin.auth.dto.CurrentUserResponse;
import edu.cit.checkedin.password.dto.PasswordSignInRequest;

/**
 * Password endpoints (Increment 2 / SRS v1.4).
 *
 * POST /api/auth/password/sign-in { email, password } -> the signed-in user, and the same 8-hour session cookie as
 *      the other sign-in methods (NFR-009). 401 "Email or password is incorrect." / 429 after too many wrong tries.
 */
@RestController
public class PasswordController {

    private final PasswordSignInService passwordSignInService;
    private final SessionService sessionService;

    public PasswordController(PasswordSignInService passwordSignInService, SessionService sessionService) {
        this.passwordSignInService = passwordSignInService;
        this.sessionService = sessionService;
    }

    @PostMapping("/api/auth/password/sign-in")
    public CurrentUserResponse signIn(@Valid @RequestBody PasswordSignInRequest request,
                                      HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        CurrentUserResponse currentUser = passwordSignInService.signIn(request.email(), request.password());
        sessionService.startSession(currentUser.email(), httpRequest, httpResponse);
        return currentUser;
    }
}
