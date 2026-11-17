package edu.cit.checkedin.section;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import edu.cit.checkedin.section.dto.AddSessionRequest;
import edu.cit.checkedin.section.dto.SessionResponse;

/**
 * Adding and removing the weekly sessions of a section.
 * Covers FR-012 (add sessions), FR-013 (times and overlaps, see SessionScheduleChecker) and
 * NFR-010 (only in the professor's own, non-archived sections).
 * (Not called "SessionService": that name is taken by the sign-in session in the auth package.)
 */
@Service
public class ClassSessionService {

    private final SectionService sectionService;
    private final CourseSectionRepository sectionRepository;
    private final ClassSessionRepository sessionRepository;
    private final SessionScheduleChecker scheduleChecker;

    public ClassSessionService(SectionService sectionService, CourseSectionRepository sectionRepository,
                               ClassSessionRepository sessionRepository, SessionScheduleChecker scheduleChecker) {
        this.sectionService = sectionService;
        this.sectionRepository = sectionRepository;
        this.sessionRepository = sessionRepository;
        this.scheduleChecker = scheduleChecker;
    }

    @Transactional
    public SessionResponse addSession(String professorEmail, Long sectionId, AddSessionRequest request) {
        CourseSection section = sectionService.findOwnedSection(professorEmail, sectionId);
        sectionService.requireNotArchived(section);
        ClassSession newSession = new ClassSession(request.dayOfWeek(), request.startTime(), request.endTime(),
                request.sessionType(), request.deliveryMode(), request.roomName());

        scheduleChecker.checkTimes(newSession);
        // Compared with every session in the professor's active sections of this term,
        // including this section's other sessions (e.g. its lab against its lecture)
        scheduleChecker.checkNoOverlap(newSession, sectionRepository.findByProfessorAndArchived(section.getProfessor(), false),
                section.getSemester(), section.getSchoolYear(), null);

        section.addSession(newSession);
        return SessionResponse.from(sessionRepository.save(newSession));
    }

    @Transactional
    public void removeSession(String professorEmail, Long sectionId, Long sessionId) {
        CourseSection section = sectionService.findOwnedSection(professorEmail, sectionId);
        sectionService.requireNotArchived(section);
        // Looked up only inside the professor's own section, so nobody can remove someone else's session
        ClassSession session = section.getSessions().stream()
                .filter(existing -> existing.getSessionId().equals(sessionId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "This session does not exist in this section."));
        // Phase 6: refuse this once the session has class meetings with attendance
        section.removeSession(session);
    }
}
