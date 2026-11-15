package edu.cit.checkedin.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import edu.cit.checkedin.common.SecurityConfig;

/**
 * Tests the GitHub sign-in steps that Spring Security runs for us, with our real SecurityConfig
 * (NFR-008): the redirect to GitHub carries a "state" value but never the client secret, and a
 * callback with a forged "state" signs nobody in.
 */
@WebMvcTest(controllers = AuthController.class, properties = {
    "spring.security.oauth2.client.registration.github.client-id=test-client-id",
    "spring.security.oauth2.client.registration.github.client-secret=test-client-secret"
})
@Import({SecurityConfig.class, SessionService.class, GitHubLoginHandler.class})
class GitHubSignInFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LoginCodeService loginCodeService;

    @MockBean
    private GitHubUserService gitHubUserService;

    @MockBean
    private AuthService authService;

    // Needed by SecurityConfig's role rules; RoleAccessTest tests those rules
    @MockBean
    private RoleService roleService;

    @Test
    void theButtonSendsTheBrowserToGitHubWithAStateButNeverTheSecret() throws Exception {
        MvcResult result = mockMvc.perform(get("/oauth2/authorization/github"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String gitHubAddress = result.getResponse().getRedirectedUrl();
        assertThat(gitHubAddress)
                .startsWith("https://github.com/login/oauth/authorize?")
                .contains("client_id=test-client-id")
                .contains("scope=user:email")
                .contains("state=")
                .contains("redirect_uri=http://localhost/login/oauth2/code/github")
                .doesNotContain("test-client-secret");
    }

    @Test
    void aCallbackWithAForgedStateIsRejectedAndSignsNobodyIn() throws Exception {
        MvcResult start = mockMvc.perform(get("/oauth2/authorization/github")).andReturn();
        MockHttpSession session = (MockHttpSession) start.getRequest().getSession(false);

        mockMvc.perform(get("/login/oauth2/code/github")
                        .param("code", "code-from-github")
                        .param("state", "forged-state")
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?signInError=github-failed"));

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isUnauthorized());
        // Spring stopped before asking GitHub for anything
        verifyNoInteractions(gitHubUserService);
    }

    @Test
    void signedOutApiCallsStillGet401JsonNotARedirectToGitHub() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Please sign in first."));
    }
}
