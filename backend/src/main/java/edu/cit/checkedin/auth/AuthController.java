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
import edu.cit.checkedin.auth.dto.SaveNameRequest;
import edu.cit.checkedin.auth.dto.VerifyCodeRequest;

/**
 * REST endpoints for signing in and creating the account.
 * Covers FR-001 – FR-004. All rules live in LoginCodeService and AuthService; this class only
 * passes requests on. (GitHub sign-in runs through Spring Security instead, see SecurityConfig.)
 *
 * POST /api/auth/code         { email }                -> emails a 6-digit code
 * POST /api/auth/code/verify  { email, code }          -> checks the code and starts the session cookie
 * GET  /api/auth/me                                    -> who is signed in (401 when nobody is)
 * POST /api/auth/name         { firstName, lastName }  -> creates the account on first sign-in
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final LoginCodeService loginCodeService;
    private final AuthService authService;
    private final SessionService sessionService;

    public AuthController(LoginCodeService loginCodeService, AuthService authService, SessionService sessionService) {
        this.loginCodeService = loginCodeService;
        this.authService = authService;
        this.sessionService = sessionService;
    }

    @PostMapping("/code")
    public CodeSentResponse requestCode(@Valid @RequestBody RequestCodeRequest request) {
        return loginCodeService.sendCode(request.email());
    }

    @PostMapping("/code/verify")
    public CurrentUserResponse signInWithCode(@Valid @RequestBody VerifyCodeRequest request,
                                              HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        CurrentUserResponse currentUser = authService.signInWithCode(request.email(), request.code());
        sessionService.startSession(currentUser.email(), httpRequest, httpResponse);
        return currentUser;
    }

    @GetMapping("/me")
    public CurrentUserResponse getCurrentUser(Authentication authentication) {
        return authService.getCurrentUser(authentication.getName());
    }

    @PostMapping("/name")
    public CurrentUserResponse saveName(@Valid @RequestBody SaveNameRequest request,
                                        Authentication authentication, HttpServletRequest httpRequest) {
        Long githubId = sessionService.getRememberedGitHubId(httpRequest);
        return authService.saveName(authentication.getName(), githubId, request);
    }
}
