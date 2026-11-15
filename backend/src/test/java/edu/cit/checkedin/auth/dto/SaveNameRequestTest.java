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

/** Checks the name rules (FR-003) with the same validator the backend uses for requests. */
class SaveNameRequestTest {

    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Dela Cruz", "O'Neil", "O’Neil", "Ma. Clara", "Peña", "José", "Jean-Luc", "Ñoño"})
    void acceptsRealNames(String name) {
        assertThat(problemsWith(name, name)).isEmpty();
    }

    @Test
    void removesOuterSpacesAndSqueezesInnerSpaces() {
        SaveNameRequest request = new SaveNameRequest("  Juan   Carlos ", " Dela   Cruz ");

        assertThat(request.firstName()).isEqualTo("Juan Carlos");
        assertThat(request.lastName()).isEqualTo("Dela Cruz");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Juan2", "<script>", "Juan_Carlos", "...", "-", "Juan@cit"})
    void rejectsOtherCharactersAndNamesWithoutALetter(String firstName) {
        assertThat(problemsWith(firstName, "Cruz")).containsExactly(
                "Please use only letters, spaces, hyphens (-), apostrophes (') and periods (.) in your first name.");
    }

    @Test
    void aBlankNameGetsOneClearMessage() {
        assertThat(problemsWith("   ", "Cruz")).containsExactly("Please enter your first name.");
        assertThat(problemsWith("Juan", null)).containsExactly("Please enter your last name.");
    }

    @Test
    void allowsSixtyCharactersButNotSixtyOne() {
        assertThat(problemsWith("A".repeat(60), "Cruz")).isEmpty();
        assertThat(problemsWith("A".repeat(61), "Cruz"))
                .containsExactly("Your first name can have at most 60 characters.");
    }

    private List<String> problemsWith(String firstName, String lastName) {
        return validator.validate(new SaveNameRequest(firstName, lastName)).stream()
                .map(ConstraintViolation::getMessage)
                .toList();
    }
}
