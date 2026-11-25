package edu.cit.checkedin.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
import edu.cit.checkedin.auth.dto.CurrentUserResponse;
import edu.cit.checkedin.auth.dto.SaveNameRequest;
import edu.cit.checkedin.common.SecurityConfig;

/**
 * Tests the sign-in endpoints together with the real security rules (SecurityConfig)
 * and the real session handling (SessionService). The services are faked here;
 * LoginCodeServiceTest and AuthServiceTest test their rules.
 */
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, SessionService.class, GitHubLoginHandler.class})
class AuthControllerTest {

    private static final String EMAIL = "juan@cit.edu";
    private static final String SIGN_IN_JSON = "{\"email\": \"juan@cit.edu\", \"code\": \"123456\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LoginCodeService loginCodeService;

    @MockBean
    private AuthService authService;

    // Needed by SecurityConfig; GitHub sign-in itself is tested in GitHubSignInFlowTest
    @MockBean
    private GitHubUserService gitHubUserService;

    // Needed by SecurityConfig's role rules; RoleAccessTest tests those rules
    @MockBean
    private RoleService roleService;

    @Test
    void meSays401InPlainLanguageWhenNobodyIsSignedIn() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Please sign in first."));
    }

    @Test
    void askingForACodeWorksWithoutBeingSignedIn() throws Exception {
        when(loginCodeService.sendCode(EMAIL)).thenReturn(new CodeSentResponse(EMAIL, 10));

        mockMvc.perform(post("/api/auth/code").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"juan@cit.edu\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.expiresInMinutes").value(10));
    }

    @Test
    void aCorrectCodeStartsASessionThatTheNextRequestRecognizes() throws Exception {
        MockHttpSession session = signInWithCode();
        when(authService.getCurrentUser(EMAIL)).thenReturn(CurrentUserResponse.withoutAccount(EMAIL));

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.needsName").value(true));
    }

    @Test
    void signingInReplacesAnyOlderSession() throws Exception {
        when(authService.signInWithCode(EMAIL, "123456")).thenReturn(CurrentUserResponse.withoutAccount(EMAIL));
        MockHttpSession olderSession = new MockHttpSession();

        MvcResult signIn = mockMvc.perform(post("/api/auth/code/verify").session(olderSession)
                        .contentType(MediaType.APPLICATION_JSON).content(SIGN_IN_JSON))
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
        verifyNoInteractions(authService);
    }

    // ---------- POST /api/auth/name ----------

    @Test
    void savingTheNameNeedsASignedInUser() throws Exception {
        mockMvc.perform(post("/api/auth/name").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\": \"Juan\", \"lastName\": \"Dela Cruz\", \"password\": \"sunflower42\", \"confirmPassword\": \"sunflower42\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(authService);
    }

    @Test
    void savingTheNameCreatesTheAccountForTheSignedInEmail() throws Exception {
        MockHttpSession session = signInWithCode();
        when(authService.saveName(eq(EMAIL), isNull(), eq(new SaveNameRequest("Juan", "Dela Cruz", "sunflower42", "sunflower42"))))
                .thenReturn(new CurrentUserResponse(EMAIL, "Juan", "Dela Cruz", Role.STUDENT, null, false, true));

        mockMvc.perform(post("/api/auth/name").session(session).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\": \"  Juan \", \"lastName\": \"Dela   Cruz\", \"password\": \"sunflower42\", \"confirmPassword\": \"sunflower42\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.needsName").value(false));
    }

    @Test
    void anEmailCodeSessionNeverCarriesAnOldGitHubId() throws Exception {
        // An older session from a GitHub sign-in on the same browser
        MockHttpSession olderSession = new MockHttpSession();
        olderSession.setAttribute(SessionService.GITHUB_ID_SESSION_KEY, 583231L);
        when(authService.signInWithCode(EMAIL, "123456")).thenReturn(CurrentUserResponse.withoutAccount(EMAIL));
        MvcResult signIn = mockMvc.perform(post("/api/auth/code/verify").session(olderSession)
                        .contentType(MediaType.APPLICATION_JSON).content(SIGN_IN_JSON))
                .andReturn();
        MockHttpSession newSession = (MockHttpSession) signIn.getRequest().getSession(false);
        when(authService.saveName(eq(EMAIL), isNull(), eq(new SaveNameRequest("Juan", "Dela Cruz", "sunflower42", "sunflower42"))))
                .thenReturn(new CurrentUserResponse(EMAIL, "Juan", "Dela Cruz", Role.STUDENT, null, false, true));

        // Matches only when the GitHub ID passed on is null
        mockMvc.perform(post("/api/auth/name").session(newSession).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\": \"Juan\", \"lastName\": \"Dela Cruz\", \"password\": \"sunflower42\", \"confirmPassword\": \"sunflower42\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Juan"));
    }

    @Test
    void anInvalidNameGetsAPlainLanguageMessage() throws Exception {
        MockHttpSession session = signInWithCode();

        mockMvc.perform(post("/api/auth/name").session(session).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\": \"Juan2\", \"lastName\": \"Dela Cruz\", \"password\": \"sunflower42\", \"confirmPassword\": \"sunflower42\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Please use only letters, spaces, hyphens (-), apostrophes (') and periods (.) in your first name."));
    }

    // ---------- POST /api/auth/link-student-id ----------

    @Test
    void linkingAStudentIdNeedsASignedInUser() throws Exception {
        mockMvc.perform(post("/api/auth/link-student-id").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentNumber\": \"12-3456-789\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(authService);
    }

    @Test
    void aWronglyFormattedStudentIdGetsAPlainLanguageMessage() throws Exception {
        MockHttpSession session = signInWithCode();

        mockMvc.perform(post("/api/auth/link-student-id").session(session).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentNumber\": \"123456789\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Please enter your student ID in the format 00-0000-000, for example 12-3456-789."));
    }

    @Test
    void aValidStudentIdIsLinkedForTheSignedInEmail() throws Exception {
        MockHttpSession session = signInWithCode();
        when(authService.linkStudentNumber(EMAIL, "12-3456-789"))
                .thenReturn(new CurrentUserResponse(EMAIL, "Juan", "Dela Cruz", Role.STUDENT, "12-3456-789", false, false));

        mockMvc.perform(post("/api/auth/link-student-id").session(session).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentNumber\": \" 12-3456-789 \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentNumber").value("12-3456-789"))
                .andExpect(jsonPath("$.needsStudentId").value(false));
    }

    /** Signs in with a (faked) correct code and returns the new session. */
    private MockHttpSession signInWithCode() throws Exception {
        when(authService.signInWithCode(EMAIL, "123456")).thenReturn(CurrentUserResponse.withoutAccount(EMAIL));
        MvcResult signIn = mockMvc.perform(post("/api/auth/code/verify")
                        .contentType(MediaType.APPLICATION_JSON).content(SIGN_IN_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andReturn();
        return (MockHttpSession) signIn.getRequest().getSession(false);
    }
}
