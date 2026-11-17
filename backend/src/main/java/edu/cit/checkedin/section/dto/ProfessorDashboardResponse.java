package edu.cit.checkedin.section.dto;

import java.time.DayOfWeek;
import java.util.List;

/**
 * JSON for GET /api/professor/dashboard (FR-016): today's sessions in time order, and the
 * professor's active sections (archived ones are hidden, FR-011).
 *
 * @param firstName     the professor's first name
 * @param lastName      the professor's last name
 * @param today         today's weekday in Philippine time, e.g. THURSDAY
 * @param todaySessions today's sessions, earliest first
 * @param sections      the active sections, sorted by subject code and section code
 */
public record ProfessorDashboardResponse(String firstName, String lastName, DayOfWeek today,
                                         List<TodaySessionResponse> todaySessions, List<SectionResponse> sections) {
}
