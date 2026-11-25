package edu.cit.checkedin.section.dto;

import java.time.LocalTime;

import edu.cit.checkedin.section.ClassSession;
import edu.cit.checkedin.section.CourseSection;
import edu.cit.checkedin.section.DeliveryMode;
import edu.cit.checkedin.section.SessionType;

/**
 * JSON for one of today's sessions on the professor's dashboard (FR-016), with its section.
 *
 * @param sessionId    the session's ID (Phase 6 opens its attendance page)
 * @param sectionId    the section's ID
 * @param subjectCode  e.g. IT342
 * @param subjectTitle e.g. Systems Integration and Architecture 1
 * @param sectionCode  e.g. G01
 * @param startTime    e.g. 18:00:00
 * @param endTime      e.g. 21:00:00
 * @param hours        e.g. 3.0
 * @param sessionType  LECTURE or LABORATORY
 * @param deliveryMode IN_PERSON, ONLINE, HYBRID or FIELD
 * @param roomName     the layout's room name, e.g. NGE207, or the typed room name, or null
 */
public record TodaySessionResponse(Long sessionId, Long sectionId, String subjectCode, String subjectTitle,
                                   String sectionCode, LocalTime startTime, LocalTime endTime, double hours,
                                   SessionType sessionType, DeliveryMode deliveryMode, String roomName) {

    public static TodaySessionResponse from(ClassSession session) {
        CourseSection section = session.getSection();
        return new TodaySessionResponse(session.getSessionId(), section.getSectionId(), section.getSubjectCode(),
                section.getSubjectTitle(), section.getSectionCode(), session.getStartTime(), session.getEndTime(),
                session.getHours(), session.getSessionType(), session.getDeliveryMode(), session.currentRoomName());
    }
}
