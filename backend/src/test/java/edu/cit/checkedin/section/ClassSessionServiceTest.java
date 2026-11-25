package edu.cit.checkedin.section;

import static edu.cit.checkedin.section.SectionTestData.addSession;
import static edu.cit.checkedin.section.SectionTestData.professor;
import static edu.cit.checkedin.section.SectionTestData.section;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import edu.cit.checkedin.auth.AuthService;
import edu.cit.checkedin.auth.User;
import edu.cit.checkedin.layout.LayoutService;
import edu.cit.checkedin.layout.RoomLayout;
import edu.cit.checkedin.layout.RoomType;
import edu.cit.checkedin.section.dto.AddSessionRequest;
import edu.cit.checkedin.section.dto.SessionResponse;

/**
 * Tests adding and removing weekly sessions and choosing their layout (FR-012, FR-013, FR-015, NFR-010)
 * with a fake database. The section under test is IT342 G01, First Semester 2026-2027.
 * Layout 4 is room NGE207.
 */
@ExtendWith(MockitoExtension.class)
class ClassSessionServiceTest {

    private static final String PROFESSOR_EMAIL = "maria.santos@cit.edu";

    @Mock
    private CourseSectionRepository sectionRepository;
    @Mock
    private ClassSessionRepository sessionRepository;
    @Mock
    private AuthService authService;
    @Mock
    private LayoutService layoutService;

    private ClassSessionService service;
    private User professor;
    private CourseSection section;

    @BeforeEach
    void setUp() {
        SessionScheduleChecker checker = new SessionScheduleChecker();
        SectionService sectionService = new SectionService(sectionRepository, authService, checker);
        service = new ClassSessionService(sectionService, sectionRepository, sessionRepository, checker, layoutService);

        professor = professor(1L, PROFESSOR_EMAIL);
        section = section(10L, professor, "IT342", "G01", Semester.FIRST, "2026-2027");
        lenient().when(authService.requireAccount(PROFESSOR_EMAIL)).thenReturn(professor);
        lenient().when(sectionRepository.findById(10L)).thenReturn(Optional.of(section));
        lenient().when(sessionRepository.save(any(ClassSession.class))).thenAnswer(call -> call.getArgument(0));
        RoomLayout nge207 = new RoomLayout(professor, "NGE207", RoomType.COMPUTER_LAB, 1, 1);
        ReflectionTestUtils.setField(nge207, "layoutId", 4L);
        lenient().when(layoutService.findLayout(4L)).thenReturn(nge207);
    }

    @Test
    void addsAThursdayLecture() {
        activeSectionsAre(section);

        SessionResponse result = service.addSession(PROFESSOR_EMAIL, 10L, request(DayOfWeek.THURSDAY, "18:00", "21:00"));

        assertThat(result.hours()).isEqualTo(3.0);
        assertThat(section.getSessions()).hasSize(1);
    }

    @Test
    void refusesAnEndTimeThatIsNotLaterThanTheStart() {
        assertRejected(() -> service.addSession(PROFESSOR_EMAIL, 10L, request(DayOfWeek.THURSDAY, "21:00", "18:00")),
                "end time must be later");
        assertRejected(() -> service.addSession(PROFESSOR_EMAIL, 10L, request(DayOfWeek.THURSDAY, "18:00", "18:00")),
                "end time must be later");
    }

    @Test
    void refusesSundayClasses() {
        assertRejected(() -> service.addSession(PROFESSOR_EMAIL, 10L, request(DayOfWeek.SUNDAY, "08:00", "10:00")),
                "Monday to Saturday");
    }

    @Test
    void refusesALabThatOverlapsTheSameSectionsLecture() {
        addSession(section, 100L, DayOfWeek.THURSDAY, "18:00", "21:00", SessionType.LECTURE);
        activeSectionsAre(section);

        assertRejected(() -> service.addSession(PROFESSOR_EMAIL, 10L, request(DayOfWeek.THURSDAY, "20:00", "22:00")),
                "This overlaps IT342 G01 (lecture, Thursday 6:00 PM - 9:00 PM)");
    }

    @Test
    void refusesAnOverlapWithAnotherSectionOfTheSameTerm() {
        CourseSection other = section(11L, professor, "IT343", "G02", Semester.FIRST, "2026-2027");
        addSession(other, 110L, DayOfWeek.THURSDAY, "18:00", "21:00", SessionType.LECTURE);
        activeSectionsAre(section, other);

        assertRejected(() -> service.addSession(PROFESSOR_EMAIL, 10L, request(DayOfWeek.THURSDAY, "19:00", "20:00")),
                "IT343 G02");
    }

    @Test
    void theSameTimeInAnotherTermIsAllowed() {
        CourseSection nextSemester = section(12L, professor, "IT343", "G02", Semester.SECOND, "2026-2027");
        addSession(nextSemester, 120L, DayOfWeek.THURSDAY, "18:00", "21:00", SessionType.LECTURE);
        activeSectionsAre(section, nextSemester);

        assertThat(service.addSession(PROFESSOR_EMAIL, 10L, request(DayOfWeek.THURSDAY, "18:00", "21:00"))).isNotNull();
    }

    @Test
    void sessionsThatOnlyTouchAreAllowed() {
        addSession(section, 100L, DayOfWeek.THURSDAY, "18:00", "21:00", SessionType.LECTURE);
        activeSectionsAre(section);

        assertThat(service.addSession(PROFESSOR_EMAIL, 10L, request(DayOfWeek.THURSDAY, "21:00", "22:00"))).isNotNull();
    }

    @Test
    void nothingCanBeAddedToAnArchivedSection() {
        section.archive();

        assertThatThrownBy(() -> service.addSession(PROFESSOR_EMAIL, 10L, request(DayOfWeek.THURSDAY, "18:00", "21:00")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void removesASessionOfTheProfessorsOwnSection() {
        addSession(section, 100L, DayOfWeek.THURSDAY, "18:00", "21:00", SessionType.LECTURE);

        service.removeSession(PROFESSOR_EMAIL, 10L, 100L);

        assertThat(section.getSessions()).isEmpty();
    }

    @Test
    void aSessionOutsideTheGivenSectionCannotBeRemoved() {
        // Session 200 belongs to another professor's section; asking through section 10 must not find it
        addSession(section, 100L, DayOfWeek.THURSDAY, "18:00", "21:00", SessionType.LECTURE);

        assertThatThrownBy(() -> service.removeSession(PROFESSOR_EMAIL, 10L, 200L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThat(section.getSessions()).hasSize(1);
    }

    @Test
    void anotherProfessorsSectionGetsNoNewSessions() {
        CourseSection notMine = section(20L, professor(2L, "jose.reyes@cit.edu"), "IT342", "G03", Semester.FIRST, "2026-2027");
        when(sectionRepository.findById(20L)).thenReturn(Optional.of(notMine));

        assertThatThrownBy(() -> service.addSession(PROFESSOR_EMAIL, 20L, request(DayOfWeek.THURSDAY, "18:00", "21:00")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    // ---------- room layouts (FR-015) ----------

    @Test
    void anInPersonSessionTakesItsRoomNameFromTheLayout() {
        activeSectionsAre(section);

        SessionResponse result = service.addSession(PROFESSOR_EMAIL, 10L, new AddSessionRequest(DayOfWeek.THURSDAY,
                LocalTime.parse("18:00"), LocalTime.parse("21:00"), SessionType.LECTURE, DeliveryMode.HYBRID, "typed room", 4L));

        assertThat(result.layoutId()).isEqualTo(4L);
        assertThat(result.roomName()).isEqualTo("NGE207");
        assertThat(result.layoutMissing()).isFalse();
    }

    @Test
    void anInPersonSessionNeedsALayout() {
        activeSectionsAre(section);

        assertRejected(() -> service.addSession(PROFESSOR_EMAIL, 10L, withLayout(DeliveryMode.IN_PERSON, null)),
                "Please choose the room layout");
    }

    @Test
    void anOnlineSessionCannotHaveALayout() {
        activeSectionsAre(section);

        assertRejected(() -> service.addSession(PROFESSOR_EMAIL, 10L, withLayout(DeliveryMode.ONLINE, 4L)),
                "Online and field sessions don't use a room layout");
    }

    @Test
    void aFieldSessionKeepsItsTypedRoomName() {
        activeSectionsAre(section);

        SessionResponse result = service.addSession(PROFESSOR_EMAIL, 10L, withLayout(DeliveryMode.FIELD, null));

        assertThat(result.roomName()).isEqualTo("Cebu IT Park");
        assertThat(result.layoutId()).isNull();
        assertThat(result.layoutMissing()).isFalse();
    }

    @Test
    void anOlderInPersonSessionCanGetItsLayoutLater() {
        ClassSession older = addSession(section, 100L, DayOfWeek.THURSDAY, "18:00", "21:00", SessionType.LECTURE);
        assertThat(older.isMissingLayout()).isTrue();

        SessionResponse result = service.chooseLayout(PROFESSOR_EMAIL, 10L, 100L, 4L);

        assertThat(result.layoutId()).isEqualTo(4L);
        assertThat(result.layoutMissing()).isFalse();
        assertThat(older.getRoomName()).isNull();
        assertThat(result.roomName()).isEqualTo("NGE207");
    }

    @Test
    void anArchivedSectionsSessionKeepsItsLayout() {
        addSession(section, 100L, DayOfWeek.THURSDAY, "18:00", "21:00", SessionType.LECTURE);
        section.archive();

        assertThatThrownBy(() -> service.chooseLayout(PROFESSOR_EMAIL, 10L, 100L, 4L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    // ---------- helpers ----------

    private void activeSectionsAre(CourseSection... sections) {
        when(sectionRepository.findByProfessorAndArchived(professor, false)).thenReturn(List.of(sections));
    }

    // An in-person laboratory in room NGE207 (layout 4)
    private AddSessionRequest request(DayOfWeek day, String start, String end) {
        return new AddSessionRequest(day, LocalTime.parse(start), LocalTime.parse(end),
                SessionType.LABORATORY, DeliveryMode.IN_PERSON, null, 4L);
    }

    // A Friday 8-10 AM lecture held the given way, with or without a layout
    private AddSessionRequest withLayout(DeliveryMode deliveryMode, Long layoutId) {
        return new AddSessionRequest(DayOfWeek.FRIDAY, LocalTime.parse("08:00"), LocalTime.parse("10:00"),
                SessionType.LECTURE, deliveryMode, "Cebu IT Park", layoutId);
    }

    private static void assertRejected(ThrowingCallable call, String messagePart) {
        assertThatThrownBy(call).isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
            assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(exception.getReason()).contains(messagePart);
        });
    }
}
