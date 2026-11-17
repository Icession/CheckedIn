package edu.cit.checkedin.section;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.cit.checkedin.section.dto.ProfessorDashboardResponse;

/**
 * The professor's dashboard. Covers FR-016 (today's sessions in time order) and FR-008
 * (only professors may call it; the rule is in SecurityConfig, anyone else gets HTTP 403).
 *
 * GET /api/professor/dashboard -> { firstName, lastName, today, todaySessions, sections }
 */
@RestController
@RequestMapping("/api/professor/dashboard")
public class ProfessorDashboardController {

    private final ProfessorDashboardService dashboardService;

    public ProfessorDashboardController(ProfessorDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public ProfessorDashboardResponse getDashboard(Authentication authentication) {
        return dashboardService.getDashboard(authentication.getName());
    }
}
