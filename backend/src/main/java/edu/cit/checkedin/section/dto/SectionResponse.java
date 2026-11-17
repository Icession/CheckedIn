package edu.cit.checkedin.section.dto;

import java.util.Comparator;
import java.util.List;

import edu.cit.checkedin.section.ClassSession;
import edu.cit.checkedin.section.CourseSection;
import edu.cit.checkedin.section.Semester;

/**
 * JSON for one section with its weekly sessions (FR-010 – FR-012).
 * Sessions are listed Monday to Saturday, earliest first. (The day is stored as text, so the
 * database would sort FRIDAY before MONDAY; that is why the sorting happens here in Java.)
 *
 * @param sectionId    the section's ID
 * @param subjectCode  e.g. IT342
 * @param subjectTitle e.g. Systems Integration and Architecture 1
 * @param sectionCode  e.g. G01
 * @param semester     FIRST, SECOND or SUMMER
 * @param schoolYear   e.g. 2026-2027
 * @param archived     true when the section is read-only (FR-011)
 * @param sessions     the weekly sessions, in week order
 */
public record SectionResponse(Long sectionId, String subjectCode, String subjectTitle, String sectionCode,
                              Semester semester, String schoolYear, boolean archived, List<SessionResponse> sessions) {

    private static final Comparator<ClassSession> WEEK_ORDER =
            Comparator.comparing(ClassSession::getDayOfWeek).thenComparing(ClassSession::getStartTime);

    public static SectionResponse from(CourseSection section) {
        List<SessionResponse> sessions = section.getSessions().stream()
                .sorted(WEEK_ORDER)
                .map(SessionResponse::from)
                .toList();
        return new SectionResponse(section.getSectionId(), section.getSubjectCode(), section.getSubjectTitle(),
                section.getSectionCode(), section.getSemester(), section.getSchoolYear(), section.isArchived(), sessions);
    }
}
