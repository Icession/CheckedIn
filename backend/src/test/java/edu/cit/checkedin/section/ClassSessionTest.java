package edu.cit.checkedin.section;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

/** Checks session length (FR-014), the overlap rule (FR-013) and which sessions need a layout (FR-015). */
class ClassSessionTest {

    @Test
    void sixToNinePmIsThreeHours() {
        ClassSession lecture = thursday("18:00", "21:00");

        assertThat(lecture.getHours()).isEqualTo(3.0);
        assertThat(lecture.getMinutes()).isEqualTo(180);
    }

    @Test
    void halfHoursCountAsHalves() {
        assertThat(thursday("07:30", "09:00").getHours()).isEqualTo(1.5);
    }

    @Test
    void sessionsWhoseTimesCrossOverlap() {
        assertThat(thursday("18:00", "21:00").overlapsWith(thursday("20:00", "22:00"))).isTrue();
        // One inside the other, and two identical sessions
        assertThat(thursday("18:00", "21:00").overlapsWith(thursday("19:00", "20:00"))).isTrue();
        assertThat(thursday("18:00", "21:00").overlapsWith(thursday("18:00", "21:00"))).isTrue();
    }

    @Test
    void sessionsThatOnlyTouchDoNotOverlap() {
        assertThat(thursday("09:00", "10:30").overlapsWith(thursday("10:30", "12:00"))).isFalse();
        assertThat(thursday("10:30", "12:00").overlapsWith(thursday("09:00", "10:30"))).isFalse();
    }

    @Test
    void theSameTimesOnDifferentDaysDoNotOverlap() {
        ClassSession friday = new ClassSession(DayOfWeek.FRIDAY, LocalTime.parse("18:00"), LocalTime.parse("21:00"),
                SessionType.LECTURE, DeliveryMode.IN_PERSON, null);

        assertThat(thursday("18:00", "21:00").overlapsWith(friday)).isFalse();
    }

    @Test
    void onlyInPersonAndHybridSessionsNeedALayout() {
        assertThat(session(DeliveryMode.IN_PERSON).needsLayout()).isTrue();
        assertThat(session(DeliveryMode.HYBRID).needsLayout()).isTrue();
        assertThat(session(DeliveryMode.ONLINE).needsLayout()).isFalse();
        assertThat(session(DeliveryMode.FIELD).needsLayout()).isFalse();
    }

    private ClassSession thursday(String start, String end) {
        return new ClassSession(DayOfWeek.THURSDAY, LocalTime.parse(start), LocalTime.parse(end),
                SessionType.LECTURE, DeliveryMode.IN_PERSON, "NGE207");
    }

    private ClassSession session(DeliveryMode deliveryMode) {
        return new ClassSession(DayOfWeek.THURSDAY, LocalTime.parse("18:00"), LocalTime.parse("21:00"),
                SessionType.LECTURE, deliveryMode, null);
    }
}
