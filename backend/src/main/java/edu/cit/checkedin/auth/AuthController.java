package edu.cit.checkedin.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.cit.checkedin.auth.dto.CodeSentResponse;
import edu.cit.checkedin.auth.dto.CurrentUserResponse;
import edu.cit.checkedin.auth.dto.RequestCodeRequest;
import edu.cit.checkedin.auth.dto.VerifyCodeRequest;

/**
 * REST endpoints for signing in with an emailed code.
 * Covers FR-001 and FR-002. All rules live in LoginCodeService; this class only passes requests on.
 *
 * POST /api/auth/code         { email }        -> emails a 6-digit code
 * POST /api/auth/code/verify  { email, code }  -> checks the code and starts the session cookie
 * GET  /api/auth/me                            -> who is signed in (401 when nobody is)
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final LoginCodeService loginCodeService;
    private final SessionService sessionService;

    public AuthController(LoginCodeService loginCodeService, SessionService sessionService) {
        this.loginCodeService = loginCodeService;
        this.sessionService = sessionService;
    }

    @PostMapping("/code")
    public CodeSentResponse requestCode(@Valid @RequestBody RequestCodeRequest request) {
        return loginCodeService.sendCode(request.email());
    }

    @PostMapping("/code/verify")
    public CurrentUserResponse signInWithCode(@Valid @RequestBody VerifyCodeRequest request,
                                              HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String verifiedEmail = loginCodeService.checkCode(request.email(), request.code());
        sessionService.startSession(verifiedEmail, httpRequest, httpResponse);
        return new CurrentUserResponse(verifiedEmail);
    }

    @GetMapping("/me")
    public CurrentUserResponse getCurrentUser(Authentication authentication) {
        return new CurrentUserResponse(authentication.getName());
    }
}
