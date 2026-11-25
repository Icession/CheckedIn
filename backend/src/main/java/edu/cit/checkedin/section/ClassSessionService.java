package edu.cit.checkedin.section;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import edu.cit.checkedin.layout.LayoutService;
import edu.cit.checkedin.section.dto.AddSessionRequest;
import edu.cit.checkedin.section.dto.SessionResponse;

/**
 * Adding and removing the weekly sessions of a section, and choosing their room layout.
 * Covers FR-012 (add sessions), FR-013 (times and overlaps, see SessionScheduleChecker),
 * FR-015 (a layout for in-person and hybrid sessions only) and NFR-010 (only in the professor's own,
 * non-archived sections). Any professor's layout may be used (FR-021).
 * (Not called "SessionService": that name is taken by the sign-in session in the auth package.)
 */
@Service
public class ClassSessionService {

    private final SectionService sectionService;
    private final CourseSectionRepository sectionRepository;
    private final ClassSessionRepository sessionRepository;
    private final SessionScheduleChecker scheduleChecker;
    private final LayoutService layoutService;

    public ClassSessionService(SectionService sectionService, CourseSectionRepository sectionRepository,
                               ClassSessionRepository sessionRepository, SessionScheduleChecker scheduleChecker,
                               LayoutService layoutService) {
        this.sectionService = sectionService;
        this.sectionRepository = sectionRepository;
        this.sessionRepository = sessionRepository;
        this.scheduleChecker = scheduleChecker;
        this.layoutService = layoutService;
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
        attachLayout(newSession, request.layoutId());

        section.addSession(newSession);
        return SessionResponse.from(sessionRepository.save(newSession));
    }

    /** FR-015: chooses or changes the layout of an in-person or hybrid session (e.g. the class moved rooms). */
    @Transactional
    public SessionResponse chooseLayout(String professorEmail, Long sectionId, Long sessionId, Long layoutId) {
        CourseSection section = sectionService.findOwnedSection(professorEmail, sectionId);
        sectionService.requireNotArchived(section);
        ClassSession session = findSessionIn(section, sessionId);
        attachLayout(session, layoutId);
        return SessionResponse.from(session);
    }

    @Transactional
    public void removeSession(String professorEmail, Long sectionId, Long sessionId) {
        CourseSection section = sectionService.findOwnedSection(professorEmail, sectionId);
        sectionService.requireNotArchived(section);
        ClassSession session = findSessionIn(section, sessionId);
        // Phase 6: refuse this once the session has class meetings with attendance
        section.removeSession(session);
    }

    // Looked up only inside the professor's own section, so nobody can change someone else's session
    private ClassSession findSessionIn(CourseSection section, Long sessionId) {
        return section.getSessions().stream()
                .filter(existing -> existing.getSessionId().equals(sessionId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "This session does not exist in this section."));
    }

    // FR-015: in-person and hybrid sessions need a room layout; online and field sessions can't have one
    private void attachLayout(ClassSession session, Long layoutId) {
        if (session.needsLayout() && layoutId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please choose the room layout for this "
                    + "in-person or hybrid session. If the room has no layout yet, draw it under Room layouts first.");
        }
        if (!session.needsLayout() && layoutId != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Online and field sessions don't use a room layout. Leave the layout empty.");
        }
        if (layoutId != null) {
            session.chooseLayout(layoutService.findLayout(layoutId));
        }
    }
}
