package edu.cit.checkedin.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

/** Tests what happens after GitHub sign-in succeeds or fails. */
class GitHubLoginHandlerTest {

    private final GitHubLoginHandler handler = new GitHubLoginHandler(new SessionService());
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @AfterEach
    void forgetTheSignIn() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void successStartsAFreshSessionNamedAfterTheCitEmail() throws Exception {
        MockHttpSession sessionDuringGitHubSignIn = new MockHttpSession();
        request.setSession(sessionDuringGitHubSignIn);
        DefaultOAuth2User githubUser = new DefaultOAuth2User(List.of(), Map.of(
                GitHubUserService.EMAIL_ATTRIBUTE, "juan.delacruz@cit.edu",
                GitHubUserService.GITHUB_ID_ATTRIBUTE, 583231L), GitHubUserService.EMAIL_ATTRIBUTE);

        handler.onAuthenticationSuccess(request, response, new OAuth2AuthenticationToken(githubUser, List.of(), "github"));

        // The session that held GitHub's token is gone; the new one holds only our sign-in
        assertThat(sessionDuringGitHubSignIn.isInvalid()).isTrue();
        HttpSession newSession = request.getSession(false);
        SecurityContext savedSignIn = (SecurityContext) newSession.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertThat(savedSignIn.getAuthentication().getName()).isEqualTo("juan.delacruz@cit.edu");
        assertThat(newSession.getAttribute(SessionService.GITHUB_ID_SESSION_KEY)).isEqualTo(583231L);
        assertThat(response.getRedirectedUrl()).isEqualTo("/");
    }

    @Test
    void noCitEmailGoesBackToTheSignInPageWithThatReason() throws Exception {
        handler.onAuthenticationFailure(request, response,
                new OAuth2AuthenticationException(new OAuth2Error(GitHubUserService.NOT_CIT_EMAIL), "no @cit.edu"));

        assertThat(response.getRedirectedUrl()).isEqualTo("/?signInError=not-cit-email");
    }

    @Test
    void anyOtherProblemUsesTheGeneralReason() throws Exception {
        // For example a forged or expired "state" value
        handler.onAuthenticationFailure(request, response,
                new OAuth2AuthenticationException(new OAuth2Error("authorization_request_not_found")));

        assertThat(response.getRedirectedUrl()).isEqualTo("/?signInError=github-failed");
    }
}
