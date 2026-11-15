package edu.cit.checkedin.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import edu.cit.checkedin.common.SecurityConfig;
import edu.cit.checkedin.section.ProfessorDashboardController;
import edu.cit.checkedin.student.StudentDashboardController;

/**
 * Tests FR-008 (professor pages only for professors, student pages only for students, HTTP 403
 * otherwise) and FR-009 (sign out) with the real security rules. The professor list is faked,
 * so the test can change someone's role between two requests.
 */
@WebMvcTest(controllers = {ProfessorDashboardController.class, StudentDashboardController.class, AuthController.class})
@Import({SecurityConfig.class, SessionService.class, GitHubLoginHandler.class})
class RoleAccessTest {

    private static final String PROFESSOR = "maria.santos@cit.edu";
    private static final String STUDENT = "juan.delacruz@cit.edu";
    private static final String WRONG_ROLE_MESSAGE =
            "Sorry, you can't open this page. Professor pages are only for professors, and student pages only for students.";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RoleService roleService;
    @MockBean
    private AuthService authService;
    @MockBean
    private LoginCodeService loginCodeService;
    @MockBean
    private GitHubUserService gitHubUserService;

    @Test
    void signedOutPeopleGet401OnBothDashboards() throws Exception {
        mockMvc.perform(get("/api/professor/dashboard")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/student/dashboard")).andExpect(status().isUnauthorized());
    }

    @Test
    void aProfessorOpensTheProfessorDashboardButGets403OnTheStudentOne() throws Exception {
        when(roleService.roleFor(PROFESSOR)).thenReturn(Role.PROFESSOR);
        when(authService.requireAccount(PROFESSOR)).thenReturn(new User(PROFESSOR, "Maria", "Santos", Role.PROFESSOR));
        MockHttpSession session = signedInAs(PROFESSOR);

        mockMvc.perform(get("/api/professor/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Maria"));
        mockMvc.perform(get("/api/student/dashboard").session(session))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(WRONG_ROLE_MESSAGE));
    }

    @Test
    void aStudentOpensTheStudentDashboardButGets403OnTheProfessorOne() throws Exception {
        when(roleService.roleFor(STUDENT)).thenReturn(Role.STUDENT);
        User student = new User(STUDENT, "Juan", "Dela Cruz", Role.STUDENT);
        student.linkStudentNumber("12-3456-789");
        when(authService.requireStudentWithId(STUDENT)).thenReturn(student);
        MockHttpSession session = signedInAs(STUDENT);

        mockMvc.perform(get("/api/student/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentNumber").value("12-3456-789"));
        mockMvc.perform(get("/api/professor/dashboard").session(session))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(WRONG_ROLE_MESSAGE));
    }

    @Test
    void someoneTakenOffTheProfessorListLosesAccessOnTheVeryNextRequest() throws Exception {
        when(authService.requireAccount(PROFESSOR)).thenReturn(new User(PROFESSOR, "Maria", "Santos", Role.PROFESSOR));
        // First request: on the list. Second request: no longer on the list.
        when(roleService.roleFor(PROFESSOR)).thenReturn(Role.PROFESSOR, Role.STUDENT);
        MockHttpSession sameSession = signedInAs(PROFESSOR);

        mockMvc.perform(get("/api/professor/dashboard").session(sameSession)).andExpect(status().isOk());
        mockMvc.perform(get("/api/professor/dashboard").session(sameSession)).andExpect(status().isForbidden());
    }

    @Test
    void signingOutEndsTheSessionAndDeletesTheCookie() throws Exception {
        MockHttpSession session = signedInAs(STUDENT);

        MvcResult signOut = mockMvc.perform(post("/api/auth/sign-out").session(session))
                .andExpect(status().isNoContent())
                .andReturn();

        assertThat(session.isInvalid()).isTrue();
        assertThat(signOut.getResponse().getHeader("Set-Cookie"))
                .startsWith("CHECKEDIN_SESSION=")
                .contains("Max-Age=0")
                .contains("HttpOnly");
        // The old session no longer signs anyone in
        mockMvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void signingOutWhenAlreadySignedOutIsHarmless() throws Exception {
        mockMvc.perform(post("/api/auth/sign-out")).andExpect(status().isNoContent());
    }

    /** A session in which the given email is signed in, as SessionService.startSession leaves it. */
    private MockHttpSession signedInAs(String email) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(email, null, List.of()));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return session;
    }
}
