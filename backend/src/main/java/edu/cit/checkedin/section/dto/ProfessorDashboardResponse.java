package edu.cit.checkedin.section.dto;

import edu.cit.checkedin.auth.User;

/**
 * JSON for GET /api/professor/dashboard. Phase 2 adds today's sessions in time order (FR-016).
 *
 * @param firstName the professor's first name
 * @param lastName  the professor's last name
 */
public record ProfessorDashboardResponse(String firstName, String lastName) {

    public static ProfessorDashboardResponse from(User professor) {
        return new ProfessorDashboardResponse(professor.getFirstName(), professor.getLastName());
    }
}
