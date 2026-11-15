package edu.cit.checkedin.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import edu.cit.checkedin.auth.dto.CodeSentResponse;
import edu.cit.checkedin.common.SecurityConfig;

/**
 * Tests the sign-in endpoints together with the real security rules (SecurityConfig)
 * and the real session handling (SessionService). The code rules themselves are faked here;
 * LoginCodeServiceTest tests them.
 */
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, SessionService.class, GitHubLoginHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LoginCodeService loginCodeService;

    // Needed by SecurityConfig; GitHub sign-in itself is tested in GitHubSignInFlowTest
    @MockBean
    private GitHubUserService gitHubUserService;

    @Test
    void meSays401InPlainLanguageWhenNobodyIsSignedIn() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Please sign in first."));
    }

    @Test
    void askingForACodeWorksWithoutBeingSignedIn() throws Exception {
        when(loginCodeService.sendCode("juan@cit.edu")).thenReturn(new CodeSentResponse("juan@cit.edu", 10));

        mockMvc.perform(post("/api/auth/code").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"juan@cit.edu\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("juan@cit.edu"))
                .andExpect(jsonPath("$.expiresInMinutes").value(10));
    }

    @Test
    void aCorrectCodeStartsASessionThatTheNextRequestRecognizes() throws Exception {
        when(loginCodeService.checkCode("juan@cit.edu", "123456")).thenReturn("juan@cit.edu");

        MvcResult signIn = mockMvc.perform(post("/api/auth/code/verify").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"juan@cit.edu\", \"code\": \"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("juan@cit.edu"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) signIn.getRequest().getSession(false);

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("juan@cit.edu"));
    }

    @Test
    void signingInReplacesAnyOlderSession() throws Exception {
        when(loginCodeService.checkCode("juan@cit.edu", "123456")).thenReturn("juan@cit.edu");
        MockHttpSession olderSession = new MockHttpSession();

        MvcResult signIn = mockMvc.perform(post("/api/auth/code/verify").session(olderSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"juan@cit.edu\", \"code\": \"123456\"}"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(olderSession.isInvalid()).isTrue();
        assertThat(signIn.getRequest().getSession(false)).isNotSameAs(olderSession);
    }

    @Test
    void aCodeThatIsNotSixDigitsIsRejectedBeforeAnyCheck() throws Exception {
        mockMvc.perform(post("/api/auth/code/verify").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"juan@cit.edu\", \"code\": \"12ab\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The code has exactly 6 digits. Please check it and try again."));
        verifyNoInteractions(loginCodeService);
    }
}
