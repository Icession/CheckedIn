package edu.cit.checkedin.auth;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * What happens after "Sign in with GitHub" finishes (FR-001, FR-002).
 * Success: starts the same 8-hour session as the email-code sign-in (NFR-009) and opens the app.
 * Failure: goes back to the sign-in page with a short error code that the page explains.
 */
@Component
public class GitHubLoginHandler implements AuthenticationSuccessHandler, AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(GitHubLoginHandler.class);

    private final SessionService sessionService;

    public GitHubLoginHandler(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User githubUser = (OAuth2User) authentication.getPrincipal();
        String verifiedEmail = githubUser.getAttribute(GitHubUserService.EMAIL_ATTRIBUTE);
        Long githubId = githubUser.getAttribute(GitHubUserService.GITHUB_ID_ATTRIBUTE);

        // A fresh session that holds only the verified email, exactly like the email-code sign-in.
        // This also throws away the GitHub access token: we needed it only once, to read the emails.
        sessionService.startSession(verifiedEmail, request, response);
        sessionService.rememberGitHubId(githubId, request);
        response.sendRedirect("/");
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String errorCode = errorCodeFor(exception);
        log.warn("GitHub sign-in failed ({}): {}", errorCode, exception.getMessage());
        response.sendRedirect("/?signInError=" + errorCode);
    }

    // Only fixed codes go into the address, never the exception's own text
    private String errorCodeFor(AuthenticationException exception) {
        if (exception instanceof OAuth2AuthenticationException oauthException
                && GitHubUserService.NOT_CIT_EMAIL.equals(oauthException.getError().getErrorCode())) {
            return GitHubUserService.NOT_CIT_EMAIL;
        }
        return GitHubUserService.GITHUB_FAILED;
    }
}
