package edu.cit.checkedin.section;

import java.util.Comparator;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import edu.cit.checkedin.auth.AuthService;
import edu.cit.checkedin.auth.User;
import edu.cit.checkedin.section.dto.SaveSectionRequest;
import edu.cit.checkedin.section.dto.SectionResponse;

/**
 * Creating, editing, archiving and listing a professor's sections.
 * Covers FR-010 (create), FR-011 (edit; archive makes a section read-only and hides it),
 * FR-013 (moving a section to another term must not create overlaps) and NFR-010
 * (a professor can only see and change their own sections).
 */
@Service
public class SectionService {

    private static final Comparator<CourseSection> BY_CODES =
            Comparator.comparing(CourseSection::getSubjectCode).thenComparing(CourseSection::getSectionCode);

    private final CourseSectionRepository sectionRepository;
    private final AuthService authService;
    private final SessionScheduleChecker scheduleChecker;

    public SectionService(CourseSectionRepository sectionRepository, AuthService authService,
                          SessionScheduleChecker scheduleChecker) {
        this.sectionRepository = sectionRepository;
        this.authService = authService;
        this.scheduleChecker = scheduleChecker;
    }

    /** The professor's active sections (or archived ones), sorted by subject code and section code. */
    @Transactional(readOnly = true)
    public List<SectionResponse> listSections(String professorEmail, boolean archived) {
        User professor = authService.requireAccount(professorEmail);
        return sectionRepository.findByProfessorAndArchived(professor, archived).stream()
                .sorted(BY_CODES)
                .map(SectionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SectionResponse getSection(String professorEmail, Long sectionId) {
        return SectionResponse.from(findOwnedSection(professorEmail, sectionId));
    }

    @Transactional
    public SectionResponse createSection(String professorEmail, SaveSectionRequest request) {
        User professor = authService.requireAccount(professorEmail);
        checkSchoolYear(request.schoolYear());
        checkNotDuplicate(sectionRepository.findByProfessorAndArchived(professor, false), request, null);
        CourseSection section = new CourseSection(professor, request.subjectCode(), request.subjectTitle(),
                request.sectionCode(), request.semester(), request.schoolYear());
        return SectionResponse.from(sectionRepository.save(section));
    }

    @Transactional
    public SectionResponse updateSection(String professorEmail, Long sectionId, SaveSectionRequest request) {
        CourseSection section = findOwnedSection(professorEmail, sectionId);
        requireNotArchived(section);
        checkSchoolYear(request.schoolYear());
        List<CourseSection> activeSections = sectionRepository.findByProfessorAndArchived(section.getProfessor(), false);
        checkNotDuplicate(activeSections, request, section);
        if (!section.isSameTermAs(request.semester(), request.schoolYear())) {
            checkSessionsFitNewTerm(section, activeSections, request);
        }
        section.setSubjectCode(request.subjectCode());
        section.setSubjectTitle(request.subjectTitle());
        section.setSectionCode(request.sectionCode());
        section.setSemester(request.semester());
        section.setSchoolYear(request.schoolYear());
        return SectionResponse.from(section);
    }

    /** FR-011: makes the section read-only and hides it from the dashboard. Archiving twice is harmless. */
    @Transactional
    public SectionResponse archiveSection(String professorEmail, Long sectionId) {
        CourseSection section = findOwnedSection(professorEmail, sectionId);
        section.archive();
        return SectionResponse.from(section);
    }

    /**
     * NFR-010: the section, but only if it belongs to the signed-in professor
     * (404 if it does not exist, 403 if it belongs to another professor).
     */
    public CourseSection findOwnedSection(String professorEmail, Long sectionId) {
        User professor = authService.requireAccount(professorEmail);
        CourseSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "This section does not exist."));
        if (!section.getProfessor().getUserId().equals(professor.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This section belongs to another professor.");
        }
        return section;
    }

    /** FR-011: an archived section is read-only. */
    public void requireNotArchived(CourseSection section) {
        if (section.isArchived()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This section is archived, so it can no longer be changed.");
        }
    }

    // "2026-2027" is fine; "2026-2028" or "2027-2026" is not
    private void checkSchoolYear(String schoolYear) {
        int firstYear = Integer.parseInt(schoolYear.substring(0, 4));
        int secondYear = Integer.parseInt(schoolYear.substring(5, 9));
        if (secondYear != firstYear + 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The school year must be two years in a row, for example 2026-2027.");
        }
    }

    // Only active sections count: an archived copy is read-only, so it can't split the attendance
    private void checkNotDuplicate(List<CourseSection> activeSections, SaveSectionRequest request,
                                   CourseSection sectionBeingEdited) {
        boolean duplicate = activeSections.stream()
                .filter(existing -> existing != sectionBeingEdited)
                .anyMatch(existing -> existing.getSubjectCode().equals(request.subjectCode())
                        && existing.getSectionCode().equals(request.sectionCode())
                        && existing.isSameTermAs(request.semester(), request.schoolYear()));
        if (duplicate) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already have " + request.subjectCode() + " "
                    + request.sectionCode() + " for this semester and school year (" + request.schoolYear() + ").");
        }
    }

    // FR-013: moving a section to another term must not put its sessions on top of that term's sessions
    private void checkSessionsFitNewTerm(CourseSection section, List<CourseSection> activeSections,
                                         SaveSectionRequest request) {
        for (ClassSession session : section.getSessions()) {
            scheduleChecker.findOverlap(session, activeSections, request.semester(), request.schoolYear(), section)
                    .ifPresent(existing -> {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "In the new semester and school year, "
                                + scheduleChecker.describe(session) + " would overlap " + scheduleChecker.describe(existing)
                                + ". Remove or change one of them first.");
                    });
        }
    }
}
