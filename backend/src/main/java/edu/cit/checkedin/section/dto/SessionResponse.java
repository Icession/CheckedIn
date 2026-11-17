package edu.cit.checkedin.section.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

import edu.cit.checkedin.section.ClassSession;
import edu.cit.checkedin.section.DeliveryMode;
import edu.cit.checkedin.section.SessionType;

/**
 * JSON for one weekly session (FR-012), including its length in hours (FR-014).
 *
 * @param sessionId    the session's ID
 * @param dayOfWeek    MONDAY to SATURDAY
 * @param startTime    e.g. 18:00:00
 * @param endTime      e.g. 21:00:00
 * @param hours        e.g. 3.0 or 1.5
 * @param sessionType  LECTURE or LABORATORY
 * @param deliveryMode IN_PERSON, ONLINE, HYBRID or FIELD
 * @param roomName     e.g. NGE207, or null
 */
public record SessionResponse(Long sessionId, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime,
                              double hours, SessionType sessionType, DeliveryMode deliveryMode, String roomName) {

    public static SessionResponse from(ClassSession session) {
        return new SessionResponse(session.getSessionId(), session.getDayOfWeek(), session.getStartTime(),
                session.getEndTime(), session.getHours(), session.getSessionType(), session.getDeliveryMode(),
                session.getRoomName());
    }
}
