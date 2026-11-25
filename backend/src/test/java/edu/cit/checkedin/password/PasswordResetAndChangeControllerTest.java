package edu.cit.checkedin.password;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import edu.cit.checkedin.auth.AuthService;
import edu.cit.checkedin.auth.GitHubLoginHandler;
import edu.cit.checkedin.auth.GitHubUserService;
import edu.cit.checkedin.auth.RoleService;
import edu.cit.checkedin.auth.SessionService;
import edu.cit.checkedin.common.SecurityConfig;
import edu.cit.checkedin.password.dto.ChangePasswordRequest;
import edu.cit.checkedin.password.dto.ResetPasswordRequest;

/**
 * Tests the forgot/reset and change password endpoints (Increment 2 / SRS v1.4) with the real security rules:
 * asking for and using a reset link needs no sign-in; changing the password needs one, and always changes the
 * password of the person signed in on the session.
 */
@WebMvcTest(controllers = PasswordController.class)
@Import({SecurityConfig.class, SessionService.class, GitHubLoginHandler.class})
class PasswordResetAndChangeControllerTest {

    private static final String JSON_RESET = "{\"token\":\"abc\",\"newPassword\":\"sunflower42\",\"confirmPassword\":\"sunflower42\"}";
    private static final String JSON_CHANGE =
            "{\"currentPassword\":\"sunflower42\",\"newPassword\":\"moonlight77\",\"confirmPassword\":\"moonlight77\"}";

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
    private RoleService roleService;
    @MockBean
    private GitHubUserService gitHubUserService;

    @Test
    void askingForAResetLinkNeedsNoSignInAndAlwaysAnswers204() throws Exception {
        mockMvc.perform(post("/api/auth/password/forgot").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"juan.delacruz@cit.edu\"}"))
                .andExpect(status().isNoContent());
        verify(passwordResetService).requestReset("juan.delacruz@cit.edu");
    }

    @Test
    void usingAResetLinkNeedsNoSignIn() throws Exception {
        mockMvc.perform(post("/api/auth/password/reset").contentType(MediaType.APPLICATION_JSON).content(JSON_RESET))
                .andExpect(status().isNoContent());
        verify(passwordResetService).resetPassword(new ResetPasswordRequest("abc", "sunflower42", "sunflower42"));
    }

    @Test
    void aUsedLinkGets400WithItsMessage() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, PasswordResetService.USED_LINK))
                .when(passwordResetService).resetPassword(any());

        mockMvc.perform(post("/api/auth/password/reset").contentType(MediaType.APPLICATION_JSON).content(JSON_RESET))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(PasswordResetService.USED_LINK));
    }

    @Test
    void aWeakNewPasswordIsRefusedBeforeTheLinkIsChecked() throws Exception {
        mockMvc.perform(post("/api/auth/password/reset").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"abc\",\"newPassword\":\"sunflower\",\"confirmPassword\":\"sunflower\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(PasswordRules.TOO_WEAK));
        verifyNoInteractions(passwordResetService);
    }

    @Test
    void changingThePasswordNeedsASignedInUser() throws Exception {
        mockMvc.perform(put("/api/profile/password").contentType(MediaType.APPLICATION_JSON).content(JSON_CHANGE))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(passwordChangeService);
    }

    @Test
    void changingThePasswordAlwaysUsesTheSessionUser() throws Exception {
        mockMvc.perform(put("/api/profile/password").session(signedInAs("maria.santos@cit.edu"))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_CHANGE))
                .andExpect(status().isNoContent());
        verify(passwordChangeService).changePassword("maria.santos@cit.edu",
                new ChangePasswordRequest("sunflower42", "moonlight77", "moonlight77"));
    }

    @Test
    void anEmptyCurrentPasswordGetsAPlainMessage() throws Exception {
        mockMvc.perform(put("/api/profile/password").session(signedInAs("maria.santos@cit.edu"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"\",\"newPassword\":\"moonlight77\",\"confirmPassword\":\"moonlight77\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Please enter your current password."));
    }

    private static MockHttpSession signedInAs(String email) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(email, null, List.of()));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return session;
    }
}
