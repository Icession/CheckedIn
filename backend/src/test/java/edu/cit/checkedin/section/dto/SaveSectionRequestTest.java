package edu.cit.checkedin.section.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import edu.cit.checkedin.section.Semester;

/** Checks how section input is cleaned up and checked (FR-010). */
class SaveSectionRequestTest {

    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void codesAreTrimmedAndWrittenInCapitals() {
        SaveSectionRequest request = new SaveSectionRequest(" it342 ", "  Systems   Integration ", "g01 ",
                Semester.FIRST, " 2026-2027 ");

        assertThat(request.subjectCode()).isEqualTo("IT342");
        assertThat(request.subjectTitle()).isEqualTo("Systems Integration");
        assertThat(request.sectionCode()).isEqualTo("G01");
        assertThat(request.schoolYear()).isEqualTo("2026-2027");
        assertThat(problemsWith(request)).isEmpty();
    }

    @Test
    void aSchoolYearInAnotherFormatGetsAClearMessage() {
        SaveSectionRequest request = new SaveSectionRequest("IT342", "Title", "G01", Semester.FIRST, "2026/27");

        assertThat(problemsWith(request))
                .containsExactly("Please enter the school year as two years, for example 2026-2027.");
    }

    @Test
    void aBlankSchoolYearGetsOneMessage() {
        SaveSectionRequest request = new SaveSectionRequest("IT342", "Title", "G01", Semester.FIRST, " ");

        assertThat(problemsWith(request)).containsExactly("Please enter the school year, for example 2026-2027.");
    }

    @Test
    void symbolsInTheCodesAreRefused() {
        SaveSectionRequest request = new SaveSectionRequest("IT_342", "Title", "G/01", Semester.FIRST, "2026-2027");

        assertThat(problemsWith(request)).containsExactlyInAnyOrder(
                "The subject code can only have letters, numbers, spaces and hyphens, for example IT342.",
                "The section code can only have letters, numbers and hyphens, for example G01.");
    }

    @Test
    void aMissingSemesterGetsAClearMessage() {
        SaveSectionRequest request = new SaveSectionRequest("IT342", "Title", "G01", null, "2026-2027");

        assertThat(problemsWith(request)).containsExactly("Please choose the semester.");
    }

    private List<String> problemsWith(SaveSectionRequest request) {
        return validator.validate(request).stream().map(ConstraintViolation::getMessage).toList();
    }
}
