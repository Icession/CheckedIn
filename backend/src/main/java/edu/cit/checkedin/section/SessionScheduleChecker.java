package edu.cit.checkedin.section;

import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * The FR-013 time rules for weekly sessions, kept in one place because both adding a session and
 * moving a section to another term must follow them:
 * - classes are Monday to Saturday, and the end time must be later than the start time;
 * - a session may not overlap another session of the same professor on the same day. Only sessions
 *   in the professor's ACTIVE sections of the SAME semester and school year count, so next
 *   semester's sections and archived sections never block a time slot.
 */
@Component
public class SessionScheduleChecker {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    /** Refuses Sunday classes and an end time that is not later than the start time. */
    public void checkTimes(ClassSession session) {
        if (session.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw badRequest("Classes can only be from Monday to Saturday.");
        }
        if (!session.getEndTime().isAfter(session.getStartTime())) {
            throw badRequest("The end time must be later than the start time.");
        }
    }

    /**
     * Refuses the new session if it overlaps a session in the professor's active sections of the given
     * term. skippedSection (may be null) is left out: used when a whole section moves to another term.
     */
    public void checkNoOverlap(ClassSession newSession, List<CourseSection> activeSections, Semester semester,
                               String schoolYear, CourseSection skippedSection) {
        findOverlap(newSession, activeSections, semester, schoolYear, skippedSection).ifPresent(existing -> {
            throw badRequest("This overlaps " + describe(existing) + ". Please choose another time.");
        });
    }

    /** The first session (in the given term, outside skippedSection) that the new session overlaps, if any. */
    public Optional<ClassSession> findOverlap(ClassSession newSession, List<CourseSection> activeSections,
                                              Semester semester, String schoolYear, CourseSection skippedSection) {
        return activeSections.stream()
                .filter(section -> section != skippedSection)
                .filter(section -> section.isSameTermAs(semester, schoolYear))
                .flatMap(section -> section.getSessions().stream())
                .filter(existing -> existing.overlapsWith(newSession))
                .findFirst();
    }

    /** For messages, e.g. "IT342 G01 (lecture, Thursday 6:00 PM - 9:00 PM)". */
    public String describe(ClassSession session) {
        CourseSection section = session.getSection();
        return section.getSubjectCode() + " " + section.getSectionCode()
                + " (" + session.getSessionType().name().toLowerCase(Locale.ROOT) + ", "
                + session.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " "
                + TIME_FORMAT.format(session.getStartTime()) + " - " + TIME_FORMAT.format(session.getEndTime()) + ")";
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
