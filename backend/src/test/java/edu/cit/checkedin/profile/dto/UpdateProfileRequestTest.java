package edu.cit.checkedin.profile.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Checks the name rules of the profile page (Increment 2 / SRS v1.4) with the validator the backend uses. */
class UpdateProfileRequestTest {

    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Ma. Cristina", "Dela Cruz", "De los Santos", "O'Neil", "Jean-Paul", "Niño", "Peña", "José"})
    void acceptsFilipinoAndOtherRealNames(String name) {
        assertThat(problemsWith(name, name)).isEmpty();
    }

    @Test
    void trimsAndSqueezesSpacesBeforeChecking() {
        UpdateProfileRequest request = new UpdateProfileRequest("  Ma.   Cristina ", " Dela  Cruz ");

        assertThat(request.firstName()).isEqualTo("Ma. Cristina");
        assertThat(request.lastName()).isEqualTo("Dela Cruz");
    }

    @Test
    void bothNamesAreRequired() {
        assertThat(problemsWith("   ", "Cruz")).containsExactly("Please enter your first name.");
        assertThat(problemsWith("Juan", "")).containsExactly("Please enter your last name.");
        assertThat(problemsWith("Juan", null)).containsExactly("Please enter your last name.");
    }

    @Test
    void allowsOneToFiftyCharacters() {
        assertThat(problemsWith("A", "B")).isEmpty();
        assertThat(problemsWith("A".repeat(50), "Cruz")).isEmpty();
        assertThat(problemsWith("Juan", "C".repeat(51))).containsExactly("Your last name can have at most 50 characters.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Juan2", "<script>", "Juan_Carlos", "...", "Juan@cit"})
    void rejectsDigitsSymbolsAndNamesWithoutALetter(String firstName) {
        assertThat(problemsWith(firstName, "Cruz")).containsExactly(
                "Please use only letters, spaces, hyphens (-), apostrophes (') and periods (.) in your first name.");
    }

    private List<String> problemsWith(String firstName, String lastName) {
        return validator.validate(new UpdateProfileRequest(firstName, lastName)).stream()
                .map(ConstraintViolation::getMessage)
                .toList();
    }
}
