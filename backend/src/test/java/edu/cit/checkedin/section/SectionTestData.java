package edu.cit.checkedin.section;

import java.time.DayOfWeek;
import java.time.LocalTime;

import org.springframework.test.util.ReflectionTestUtils;

import edu.cit.checkedin.auth.Role;
import edu.cit.checkedin.auth.User;

/** Small helpers that build users, sections and sessions for the section tests. */
final class SectionTestData {

    private SectionTestData() {
    }

    static User professor(Long userId, String email) {
        User professor = new User(email, "Maria", "Santos", Role.PROFESSOR);
        // IDs are normally given by the database
        ReflectionTestUtils.setField(professor, "userId", userId);
        return professor;
    }

    static CourseSection section(Long sectionId, User professor, String subjectCode, String sectionCode,
                                 Semester semester, String schoolYear) {
        CourseSection section = new CourseSection(professor, subjectCode, "Some Subject", sectionCode, semester, schoolYear);
        ReflectionTestUtils.setField(section, "sectionId", sectionId);
        return section;
    }

    /** Adds a session (e.g. "18:00" to "21:00") to the section and gives it an ID. */
    static ClassSession addSession(CourseSection section, Long sessionId, DayOfWeek day, String start, String end,
                                   SessionType sessionType) {
        ClassSession session = new ClassSession(day, LocalTime.parse(start), LocalTime.parse(end),
                sessionType, DeliveryMode.IN_PERSON, "NGE207");
        ReflectionTestUtils.setField(session, "sessionId", sessionId);
        section.addSession(session);
        return session;
    }
}
