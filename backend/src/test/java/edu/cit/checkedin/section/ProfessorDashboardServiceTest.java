package edu.cit.checkedin.section;

import static edu.cit.checkedin.section.SectionTestData.addSession;
import static edu.cit.checkedin.section.SectionTestData.professor;
import static edu.cit.checkedin.section.SectionTestData.section;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.cit.checkedin.auth.AuthService;
import edu.cit.checkedin.auth.User;
import edu.cit.checkedin.section.dto.ProfessorDashboardResponse;

/** Tests that the dashboard asks for today's sessions (FR-016), with the clock fixed on a Thursday. */
@ExtendWith(MockitoExtension.class)
class ProfessorDashboardServiceTest {

    private static final ZoneId MANILA = ZoneId.of("Asia/Manila");
    private static final LocalDate A_THURSDAY = LocalDate.of(2026, 11, 19);
    private static final String PROFESSOR_EMAIL = "maria.santos@cit.edu";

    @Mock
    private AuthService authService;
    @Mock
    private SectionService sectionService;
    @Mock
    private ClassSessionRepository sessionRepository;

    @Test
    void showsTodaysSessionsInTheOrderTheDatabaseSortedThem() {
        assertThat(A_THURSDAY.getDayOfWeek()).isEqualTo(DayOfWeek.THURSDAY);
        Clock thursdayMorning = Clock.fixed(A_THURSDAY.atTime(7, 0).atZone(MANILA).toInstant(), MANILA);
        ProfessorDashboardService service =
                new ProfessorDashboardService(authService, sectionService, sessionRepository, thursdayMorning);

        User professor = professor(1L, PROFESSOR_EMAIL);
        CourseSection section = section(10L, professor, "IT342", "G01", Semester.FIRST, "2026-2027");
        ClassSession lecture = addSession(section, 100L, DayOfWeek.THURSDAY, "13:00", "15:00", SessionType.LECTURE);
        ClassSession lab = addSession(section, 101L, DayOfWeek.THURSDAY, "18:00", "21:00", SessionType.LABORATORY);
        when(authService.requireAccount(PROFESSOR_EMAIL)).thenReturn(professor);
        when(sessionRepository.findActiveSessionsOn(professor, DayOfWeek.THURSDAY)).thenReturn(List.of(lecture, lab));
        when(sectionService.listSections(PROFESSOR_EMAIL, false)).thenReturn(List.of());

        ProfessorDashboardResponse dashboard = service.getDashboard(PROFESSOR_EMAIL);

        assertThat(dashboard.today()).isEqualTo(DayOfWeek.THURSDAY);
        assertThat(dashboard.todaySessions()).extracting("sessionId").containsExactly(100L, 101L);
        assertThat(dashboard.todaySessions().get(1).hours()).isEqualTo(3.0);
        assertThat(dashboard.todaySessions().get(0).subjectCode()).isEqualTo("IT342");
    }
}
