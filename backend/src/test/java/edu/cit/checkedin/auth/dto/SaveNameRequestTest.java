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
        SaveNameRequest request = new SaveNameRequest("  Juan   Carlos ", " Dela   Cruz ", "sunflower42", "sunflower42");

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
    void allowsFiftyCharactersButNotFiftyOne() {
        assertThat(problemsWith("A".repeat(50), "Cruz")).isEmpty();
        assertThat(problemsWith("A".repeat(51), "Cruz"))
                .containsExactly("Your first name can have at most 50 characters.");
    }

    // ---------- Increment 2 / SRS v1.4: the first password ----------

    @ParameterizedTest
    @ValueSource(strings = {"sunflower42", "Ñandú2026", "a1b2c3d4", "correct horse battery 9"})
    void acceptsPasswordsWithAtLeastEightCharactersALetterAndANumber(String password) {
        assertThat(passwordProblems(password, password)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"short1", "abcdefgh", "12345678", "        "})
    void refusesShortPasswordsAndPasswordsWithoutALetterOrANumber(String password) {
        assertThat(passwordProblems(password, password)).containsExactly(
                "Your password needs at least 8 characters, with at least one letter and one number.");
    }

    @Test
    void allowsSixtyFourCharactersButNotSixtyFive() {
        String sixtyFour = "a1" + "x".repeat(62);
        assertThat(passwordProblems(sixtyFour, sixtyFour)).isEmpty();
        assertThat(passwordProblems(sixtyFour + "x", "x")).containsExactly("Your password can have at most 64 characters.");
    }

    @Test
    void bothPasswordBoxesAreRequired() {
        assertThat(passwordProblems("", "")).containsExactlyInAnyOrder(
                "Please create a password.", "Please type your password again under \"Confirm password\".");
    }

    private List<String> passwordProblems(String password, String confirmPassword) {
        return validator.validate(new SaveNameRequest("Juan", "Cruz", password, confirmPassword)).stream()
                .map(ConstraintViolation::getMessage)
                .toList();
    }

    private List<String> problemsWith(String firstName, String lastName) {
        return validator.validate(new SaveNameRequest(firstName, lastName, "sunflower42", "sunflower42")).stream()
                .map(ConstraintViolation::getMessage)
                .toList();
    }
}
