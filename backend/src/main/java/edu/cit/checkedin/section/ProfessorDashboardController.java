package edu.cit.checkedin.section;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.cit.checkedin.auth.AuthService;
import edu.cit.checkedin.section.dto.ProfessorDashboardResponse;

/**
 * The professor's dashboard. Covers FR-008: only professors may call it (the rule is in
 * SecurityConfig; anyone else gets HTTP 403). Empty for now: Phase 2 adds today's sessions (FR-016).
 *
 * GET /api/professor/dashboard -> { firstName, lastName }
 */
@RestController
@RequestMapping("/api/professor/dashboard")
public class ProfessorDashboardController {

    private final AuthService authService;

    public ProfessorDashboardController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping
    public ProfessorDashboardResponse getDashboard(Authentication authentication) {
        return ProfessorDashboardResponse.from(authService.requireAccount(authentication.getName()));
    }
}
