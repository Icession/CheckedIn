package edu.cit.checkedin.section.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import edu.cit.checkedin.section.DeliveryMode;
import edu.cit.checkedin.section.SessionType;

/**
 * JSON for adding a weekly session to a section (FR-012), e.g.
 * { "dayOfWeek": "THURSDAY", "startTime": "18:00", "endTime": "21:00",
 *   "sessionType": "LECTURE", "deliveryMode": "IN_PERSON", "layoutId": 4 }.
 * The time rules (end after start, no overlaps, Monday to Saturday) are checked by SessionScheduleChecker,
 * the layout rule (FR-015) by ClassSessionService.
 *
 * @param dayOfWeek    MONDAY to SATURDAY
 * @param startTime    e.g. 18:00
 * @param endTime      e.g. 21:00
 * @param sessionType  LECTURE or LABORATORY
 * @param deliveryMode IN_PERSON, ONLINE, HYBRID or FIELD
 * @param roomName     optional, only for online and field sessions, e.g. "Cebu IT Park" (at most 30 characters);
 *                     in-person and hybrid sessions take the room name from their layout
 * @param layoutId     FR-015: required for in-person and hybrid sessions, empty for online and field ones
 */
public record AddSessionRequest(
        @NotNull(message = "Please choose the day.")
        DayOfWeek dayOfWeek,

        @NotNull(message = "Please enter the start time.")
        LocalTime startTime,

        @NotNull(message = "Please enter the end time.")
        LocalTime endTime,

        @NotNull(message = "Please choose lecture or laboratory.")
        SessionType sessionType,

        @NotNull(message = "Please choose how the class is held (in person, online, hybrid or field).")
        DeliveryMode deliveryMode,

        @Size(max = 30, message = "The room name can have at most 30 characters.")
        String roomName,

        Long layoutId) {

    /** Runs before the checks above: trims the room name, and an empty one becomes "no room". */
    public AddSessionRequest {
        roomName = (roomName == null || roomName.isBlank()) ? null : roomName.trim().replaceAll("\\s+", " ");
    }
}
