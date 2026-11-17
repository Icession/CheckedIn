package edu.cit.checkedin.section;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;

import org.hibernate.annotations.Check;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * One weekly meeting time of a section, e.g. "Thursday 6:00 PM - 9:00 PM, lecture, in person".
 * One row in the "class_sessions" table (see the ERD). Covers FR-012 (day, times, type, delivery
 * mode, room), FR-013 (overlap check) and FR-014 (length in hours).
 * The link to a room layout (layout_id) is added in Phase 3.
 */
@Entity
@Table(name = "class_sessions")
// The database itself also refuses Sunday and an end time that is not after the start time,
// even when a row is typed by hand in the Supabase table editor (FR-012, FR-013)
@Check(name = "class_sessions_day_monday_to_saturday",
        constraints = "day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY')")
@Check(name = "class_sessions_end_after_start", constraints = "end_time > start_time")
public class ClassSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Long sessionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private CourseSection section;

    // Stored as text, e.g. "THURSDAY" (Java's DayOfWeek); Sunday is refused (SessionScheduleChecker and the CHECK above)
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", length = 9, nullable = false)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", length = 10, nullable = false)
    private SessionType sessionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_mode", length = 10, nullable = false)
    private DeliveryMode deliveryMode;

    // Optional, e.g. "NGE207"; online and field sessions usually have none
    @Column(name = "room_name", length = 30)
    private String roomName;

    /** Empty constructor required by JPA. Our own code uses the constructor below. */
    protected ClassSession() {
    }

    public ClassSession(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime,
                        SessionType sessionType, DeliveryMode deliveryMode, String roomName) {
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.sessionType = sessionType;
        this.deliveryMode = deliveryMode;
        this.roomName = roomName;
    }

    /** FR-014: how long the session is, in whole minutes (Phase 7 adds these up for absence hours). */
    public long getMinutes() {
        return Duration.between(startTime, endTime).toMinutes();
    }

    /** FR-014: how long the session is, in hours, e.g. 6:00 PM - 9:00 PM = 3.0 and 7:30 - 9:00 = 1.5. */
    public double getHours() {
        return getMinutes() / 60.0;
    }

    /** FR-015: in-person and hybrid sessions need a room layout (seat map); online and field ones don't. */
    public boolean needsLayout() {
        return deliveryMode == DeliveryMode.IN_PERSON || deliveryMode == DeliveryMode.HYBRID;
    }

    /**
     * FR-013: true when both sessions are on the same day and their times cross. Sessions that only
     * touch (one ends at 10:30, the next starts at 10:30) do not overlap.
     */
    public boolean overlapsWith(ClassSession other) {
        return dayOfWeek == other.dayOfWeek
                && startTime.isBefore(other.endTime)
                && other.startTime.isBefore(endTime);
    }

    // ---------- Getters (a session is added or removed, not edited) ----------

    public Long getSessionId() {
        return sessionId;
    }

    public CourseSection getSection() {
        return section;
    }

    /** Set by CourseSection.addSession, which keeps both sides of the link in step. */
    void setSection(CourseSection section) {
        this.section = section;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public SessionType getSessionType() {
        return sessionType;
    }

    public DeliveryMode getDeliveryMode() {
        return deliveryMode;
    }

    public String getRoomName() {
        return roomName;
    }
}
