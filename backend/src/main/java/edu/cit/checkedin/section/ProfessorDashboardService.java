package edu.cit.checkedin.section;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.cit.checkedin.auth.AuthService;
import edu.cit.checkedin.auth.User;
import edu.cit.checkedin.section.dto.ProfessorDashboardResponse;
import edu.cit.checkedin.section.dto.SectionResponse;
import edu.cit.checkedin.section.dto.TodaySessionResponse;

/**
 * The professor's dashboard. Covers FR-016 (today's sessions in time order) and FR-011
 * (archived sections are hidden). "Today" is taken from the shared Clock in Philippine time (NFR-035).
 */
@Service
public class ProfessorDashboardService {

    private final AuthService authService;
    private final SectionService sectionService;
    private final ClassSessionRepository sessionRepository;
    private final Clock clock;

    public ProfessorDashboardService(AuthService authService, SectionService sectionService,
                                     ClassSessionRepository sessionRepository, Clock clock) {
        this.authService = authService;
        this.sectionService = sectionService;
        this.sessionRepository = sessionRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ProfessorDashboardResponse getDashboard(String professorEmail) {
        User professor = authService.requireAccount(professorEmail);
        DayOfWeek today = LocalDate.now(clock).getDayOfWeek();
        List<TodaySessionResponse> todaySessions = sessionRepository.findActiveSessionsOn(professor, today).stream()
                .map(TodaySessionResponse::from)
                .toList();
        List<SectionResponse> sections = sectionService.listSections(professorEmail, false);
        return new ProfessorDashboardResponse(professor.getFirstName(), professor.getLastName(), today,
                todaySessions, sections);
    }
}
