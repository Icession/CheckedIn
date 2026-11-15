package edu.cit.checkedin.auth.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Checks the student ID format 00-0000-000 (FR-005) with the backend's own validator. */
class LinkStudentIdRequestTest {

    private static final String FORMAT_MESSAGE =
            "Please enter your student ID in the format 00-0000-000, for example 12-3456-789.";

    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void acceptsTheCitUFormatAndRemovesOuterSpaces() {
        LinkStudentIdRequest request = new LinkStudentIdRequest("  12-3456-789 ");

        assertThat(request.studentNumber()).isEqualTo("12-3456-789");
        assertThat(problemsWith("12-3456-789")).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"123456789", "12-345-6789", "12-3456-7890", "1-3456-789", "ab-cdef-ghi", "12 3456 789", "12-3456-78a"})
    void rejectsEverythingElseWithTheFormatMessage(String studentNumber) {
        assertThat(problemsWith(studentNumber)).containsExactly(FORMAT_MESSAGE);
    }

    @Test
    void aBlankIdGetsOneClearMessage() {
        assertThat(problemsWith("   ")).containsExactly("Please enter your student ID.");
    }

    private List<String> problemsWith(String studentNumber) {
        return validator.validate(new LinkStudentIdRequest(studentNumber)).stream()
                .map(ConstraintViolation::getMessage)
                .toList();
    }
}
