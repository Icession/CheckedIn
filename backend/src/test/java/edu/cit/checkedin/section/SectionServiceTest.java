package edu.cit.checkedin.section;

import static edu.cit.checkedin.section.SectionTestData.addSession;
import static edu.cit.checkedin.section.SectionTestData.professor;
import static edu.cit.checkedin.section.SectionTestData.section;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import edu.cit.checkedin.auth.AuthService;
import edu.cit.checkedin.auth.User;
import edu.cit.checkedin.section.dto.SaveSectionRequest;
import edu.cit.checkedin.section.dto.SectionResponse;

/** Tests the section rules (FR-010, FR-011, FR-013, NFR-010) with a fake database. */
@ExtendWith(MockitoExtension.class)
class SectionServiceTest {

    private static final String PROFESSOR_EMAIL = "maria.santos@cit.edu";

    @Mock
    private CourseSectionRepository sectionRepository;
    @Mock
    private AuthService authService;

    private SectionService service;
    private User professor;

    @BeforeEach
    void setUp() {
        service = new SectionService(sectionRepository, authService, new SessionScheduleChecker());
        professor = professor(1L, PROFESSOR_EMAIL);
        lenient().when(authService.requireAccount(PROFESSOR_EMAIL)).thenReturn(professor);
    }

    // ---------- create (FR-010) ----------

    @Test
    void createsASectionForTheSignedInProfessor() {
        when(sectionRepository.findByProfessorAndArchived(professor, false)).thenReturn(List.of());
        when(sectionRepository.save(any(CourseSection.class))).thenAnswer(call -> call.getArgument(0));

        SectionResponse result = service.createSection(PROFESSOR_EMAIL, firstSemester("IT342", "G01"));

        assertThat(result.subjectCode()).isEqualTo("IT342");
        assertThat(result.sectionCode()).isEqualTo("G01");
        assertThat(result.archived()).isFalse();
    }

    @Test
    void refusesTheSameActiveSectionTwice() {
        when(sectionRepository.findByProfessorAndArchived(professor, false))
                .thenReturn(List.of(section(5L, professor, "IT342", "G01", Semester.FIRST, "2026-2027")));

        assertRejected(() -> service.createSection(PROFESSOR_EMAIL, firstSemester("IT342", "G01")),
                HttpStatus.CONFLICT, "You already have IT342 G01");
        verify(sectionRepository, never()).save(any());
    }

    @Test
    void allowsTheSameSectionCodesInAnotherTerm() {
        when(sectionRepository.findByProfessorAndArchived(professor, false))
                .thenReturn(List.of(section(5L, professor, "IT342", "G01", Semester.SECOND, "2025-2026")));
        when(sectionRepository.save(any(CourseSection.class))).thenAnswer(call -> call.getArgument(0));

        assertThat(service.createSection(PROFESSOR_EMAIL, firstSemester("IT342", "G01")).sectionCode()).isEqualTo("G01");
    }

    @Test
    void refusesASchoolYearThatIsNotTwoYearsInARow() {
        SaveSectionRequest request = new SaveSectionRequest("IT342", "Systems Integration", "G01", Semester.FIRST, "2026-2028");

        assertRejected(() -> service.createSection(PROFESSOR_EMAIL, request), HttpStatus.BAD_REQUEST, "two years in a row");
    }

    // ---------- edit and archive (FR-011) ----------

    @Test
    void editingDoesNotCountTheSectionItselfAsADuplicate() {
        CourseSection section = section(5L, professor, "IT342", "G01", Semester.FIRST, "2026-2027");
        when(sectionRepository.findById(5L)).thenReturn(Optional.of(section));
        when(sectionRepository.findByProfessorAndArchived(professor, false)).thenReturn(List.of(section));

        SaveSectionRequest newTitle = new SaveSectionRequest("IT342", "New Title", "G01", Semester.FIRST, "2026-2027");

        assertThat(service.updateSection(PROFESSOR_EMAIL, 5L, newTitle).subjectTitle()).isEqualTo("New Title");
    }

    @Test
    void movingASectionToATermWhereItsSessionsClashIsRefused() {
        CourseSection moving = section(5L, professor, "IT342", "G01", Semester.FIRST, "2026-2027");
        addSession(moving, 50L, DayOfWeek.THURSDAY, "18:00", "21:00", SessionType.LECTURE);
        CourseSection alreadyThere = section(6L, professor, "IT343", "G02", Semester.SECOND, "2026-2027");
        addSession(alreadyThere, 60L, DayOfWeek.THURSDAY, "19:00", "20:00", SessionType.LABORATORY);
        when(sectionRepository.findById(5L)).thenReturn(Optional.of(moving));
        when(sectionRepository.findByProfessorAndArchived(professor, false)).thenReturn(List.of(moving, alreadyThere));

        SaveSectionRequest toSecondSemester = new SaveSectionRequest("IT342", "Some Subject", "G01", Semester.SECOND, "2026-2027");

        assertRejected(() -> service.updateSection(PROFESSOR_EMAIL, 5L, toSecondSemester),
                HttpStatus.BAD_REQUEST, "would overlap IT343 G02 (laboratory, Thursday 7:00 PM - 8:00 PM)");
        assertThat(moving.getSemester()).isEqualTo(Semester.FIRST);
    }

    @Test
    void anArchivedSectionCannotBeEdited() {
        CourseSection section = section(5L, professor, "IT342", "G01", Semester.FIRST, "2026-2027");
        section.archive();
        when(sectionRepository.findById(5L)).thenReturn(Optional.of(section));

        assertRejected(() -> service.updateSection(PROFESSOR_EMAIL, 5L, firstSemester("IT342", "G01")),
                HttpStatus.CONFLICT, "archived");
    }

    @Test
    void archivingMakesTheSectionReadOnlyAndCanBeRepeated() {
        CourseSection section = section(5L, professor, "IT342", "G01", Semester.FIRST, "2026-2027");
        when(sectionRepository.findById(5L)).thenReturn(Optional.of(section));

        assertThat(service.archiveSection(PROFESSOR_EMAIL, 5L).archived()).isTrue();
        assertThat(service.archiveSection(PROFESSOR_EMAIL, 5L).archived()).isTrue();
    }

    // ---------- ownership (NFR-010) ----------

    @Test
    void anotherProfessorsSectionIsForbidden() {
        User otherProfessor = professor(2L, "jose.reyes@cit.edu");
        when(sectionRepository.findById(7L))
                .thenReturn(Optional.of(section(7L, otherProfessor, "IT342", "G03", Semester.FIRST, "2026-2027")));

        assertRejected(() -> service.getSection(PROFESSOR_EMAIL, 7L), HttpStatus.FORBIDDEN, "another professor");
    }

    @Test
    void anUnknownSectionIsNotFound() {
        when(sectionRepository.findById(99L)).thenReturn(Optional.empty());

        assertRejected(() -> service.getSection(PROFESSOR_EMAIL, 99L), HttpStatus.NOT_FOUND, "does not exist");
    }

    // ---------- helpers ----------

    private SaveSectionRequest firstSemester(String subjectCode, String sectionCode) {
        return new SaveSectionRequest(subjectCode, "Systems Integration and Architecture 1", sectionCode,
                Semester.FIRST, "2026-2027");
    }

    private static void assertRejected(ThrowingCallable call, HttpStatus status, String messagePart) {
        assertThatThrownBy(call).isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
            assertThat(exception.getStatusCode()).isEqualTo(status);
            assertThat(exception.getReason()).contains(messagePart);
        });
    }
}
