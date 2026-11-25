package edu.cit.checkedin.profile;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;

import edu.cit.checkedin.auth.AuthService;
import edu.cit.checkedin.auth.GitHubLoginHandler;
import edu.cit.checkedin.auth.GitHubUserService;
import edu.cit.checkedin.auth.Role;
import edu.cit.checkedin.auth.RoleService;
import edu.cit.checkedin.auth.SessionService;
import edu.cit.checkedin.common.SecurityConfig;
import edu.cit.checkedin.profile.dto.ProfileResponse;
import edu.cit.checkedin.profile.dto.UpdateProfileRequest;

/**
 * Tests GET and PUT /api/profile (Increment 2 / SRS v1.4) with the real security rules: signed-out people get
 * 401, and the profile is always the one of the person signed in on the session, whatever the body says.
 */
@WebMvcTest(controllers = ProfileController.class)
@Import({SecurityConfig.class, SessionService.class, GitHubLoginHandler.class})
class ProfileControllerTest {

    private static final String JUAN = "juan.delacruz@cit.edu";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProfileService profileService;
    @MockBean
    private RoleService roleService;
    @MockBean
    private AuthService authService;
    @MockBean
    private GitHubUserService gitHubUserService;

    @Test
    void signedOutPeopleGet401() throws Exception {
        mockMvc.perform(get("/api/profile")).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/profile").contentType(MediaType.APPLICATION_JSON).content(names("Juan", "Cruz")))
                .andExpect(status().isUnauthorized());
        verify(profileService, never()).updateName(any(), any());
    }

    @Test
    void showsTheProfileOfThePersonSignedIn() throws Exception {
        when(profileService.getProfile(JUAN)).thenReturn(juansProfile("Juan", "Cruz"));

        mockMvc.perform(get("/api/profile").session(signedInAs(JUAN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(JUAN))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.studentNumber").value("22-1234-567"))
                .andExpect(jsonPath("$.createdOn").value("2026-11-10"));
    }

    @Test
    void anEmailOrUserIdInTheBodyIsIgnored() throws Exception {
        when(profileService.updateName(eq(JUAN), any())).thenReturn(juansProfile("Ma. Cristina", "Dela Cruz"));
        String body = "{\"firstName\":\"Ma. Cristina\",\"lastName\":\"Dela Cruz\","
                + "\"email\":\"maria.santos@cit.edu\",\"userId\":1,\"role\":\"PROFESSOR\"}";

        mockMvc.perform(put("/api/profile").session(signedInAs(JUAN)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(JUAN))
                .andExpect(jsonPath("$.firstName").value("Ma. Cristina"));
        verify(profileService).updateName(JUAN, new UpdateProfileRequest("Ma. Cristina", "Dela Cruz"));
    }

    @Test
    void aBrokenNameGets400WithAPlainMessage() throws Exception {
        mockMvc.perform(put("/api/profile").session(signedInAs(JUAN)).contentType(MediaType.APPLICATION_JSON)
                        .content(names("  ", "Cruz")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Please enter your first name."));
        verify(profileService, never()).updateName(any(), any());
    }

    private static String names(String firstName, String lastName) {
        return "{\"firstName\":\"" + firstName + "\",\"lastName\":\"" + lastName + "\"}";
    }

    private static ProfileResponse juansProfile(String firstName, String lastName) {
        return new ProfileResponse(JUAN, firstName, lastName, Role.STUDENT, "22-1234-567", LocalDate.of(2026, 11, 10), true);
    }

    private static MockHttpSession signedInAs(String email) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(email, null, List.of()));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return session;
    }
}
