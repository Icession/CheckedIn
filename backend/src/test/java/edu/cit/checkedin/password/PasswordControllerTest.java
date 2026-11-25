package edu.cit.checkedin.password;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.server.ResponseStatusException;

import edu.cit.checkedin.auth.AuthController;
import edu.cit.checkedin.auth.AuthService;
import edu.cit.checkedin.auth.GitHubLoginHandler;
import edu.cit.checkedin.auth.GitHubUserService;
import edu.cit.checkedin.auth.LoginCodeService;
import edu.cit.checkedin.auth.Role;
import edu.cit.checkedin.auth.RoleService;
import edu.cit.checkedin.auth.SessionService;
import edu.cit.checkedin.auth.dto.CurrentUserResponse;
import edu.cit.checkedin.common.SecurityConfig;
import edu.cit.checkedin.profile.dto.ProfileResponse;
import edu.cit.checkedin.student.dto.StudentDashboardResponse;

/**
 * Tests POST /api/auth/password/sign-in (Increment 2 / SRS v1.4) with the real security rules: anyone may call it,
 * a right password starts the same session as the other sign-in methods, and no response ever holds a password.
 */
@WebMvcTest(controllers = {PasswordController.class, AuthController.class})
@Import({SecurityConfig.class, SessionService.class, GitHubLoginHandler.class})
class PasswordControllerTest {

    private static final String EMAIL = "juan.delacruz@cit.edu";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PasswordSignInService passwordSignInService;
    @MockBean
    private PasswordResetService passwordResetService;
    @MockBean
    private PasswordChangeService passwordChangeService;
    @MockBean
    private AuthService authService;
    @MockBean
    private LoginCodeService loginCodeService;
    @MockBean
    private RoleService roleService;
    @MockBean
    private GitHubUserService gitHubUserService;

    @Test
    void theRightPasswordStartsTheSameSessionAsTheOtherSignInMethods() throws Exception {
        CurrentUserResponse juan = new CurrentUserResponse(EMAIL, "Juan", "Cruz", Role.STUDENT, "22-1234-567", false, false);
        when(passwordSignInService.signIn(EMAIL, "sunflower42")).thenReturn(juan);
        when(authService.getCurrentUser(EMAIL)).thenReturn(juan);

        MvcResult result = mockMvc.perform(post("/api/auth/password/sign-in").contentType(MediaType.APPLICATION_JSON)
                        .content(body(EMAIL, "sunflower42")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andReturn();

        // The new session signs the user in on the next request
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        mockMvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk());
    }

    @Test
    void aWrongPasswordGets401WithTheSameMessageForEveryone() throws Exception {
        when(passwordSignInService.signIn(EMAIL, "wrong-password-1"))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email or password is incorrect."));

        mockMvc.perform(post("/api/auth/password/sign-in").contentType(MediaType.APPLICATION_JSON)
                        .content(body(EMAIL, "wrong-password-1")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Email or password is incorrect."));
    }

    @Test
    void anEmptyPasswordIsRefusedBeforeAnythingIsChecked() throws Exception {
        mockMvc.perform(post("/api/auth/password/sign-in").contentType(MediaType.APPLICATION_JSON).content(body(EMAIL, "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Please enter your password."));
        verifyNoInteractions(passwordSignInService);
    }

    @Test
    void noResponseSentToTheBrowserHoldsAPasswordOrAHash() {
        // Only the yes/no "hasPassword" is allowed; no text field may carry a password or a hash
        for (Class<?> response : List.of(CurrentUserResponse.class, ProfileResponse.class, StudentDashboardResponse.class)) {
            assertThat(Arrays.stream(response.getRecordComponents())
                    .filter(component -> component.getType() == String.class)
                    .map(RecordComponent::getName))
                    .as(response.getSimpleName())
                    .noneMatch(name -> name.toLowerCase().contains("password") || name.toLowerCase().contains("hash"));
        }
    }

    private static String body(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }
}
