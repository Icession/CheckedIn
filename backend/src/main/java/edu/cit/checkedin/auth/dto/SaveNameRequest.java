package edu.cit.checkedin.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * JSON for POST /api/auth/name: the name every new user enters on first sign-in (FR-003).
 * Names may have letters from any language (so ñ and accents work), spaces, hyphens,
 * apostrophes and periods, e.g. "Dela Cruz", "O'Neil", "Ma. Clara". At most 60 characters each,
 * like the first_name and last_name columns in the ERD.
 *
 * @param firstName the user's first name, cleaned up as described below
 * @param lastName  the user's last name, cleaned up as described below
 */
public record SaveNameRequest(
        @NotBlank(message = "Please enter your first name.")
        @Size(max = 60, message = "Your first name can have at most 60 characters.")
        @Pattern(regexp = NAME_PATTERN, message = "Please use only letters, spaces, hyphens (-), "
                + "apostrophes (') and periods (.) in your first name.")
        String firstName,

        @NotBlank(message = "Please enter your last name.")
        @Size(max = 60, message = "Your last name can have at most 60 characters.")
        @Pattern(regexp = NAME_PATTERN, message = "Please use only letters, spaces, hyphens (-), "
                + "apostrophes (') and periods (.) in your last name.")
        String lastName) {

    // Empty is let through here so that only the "Please enter..." message shows for a blank name.
    // Otherwise: at least one letter (\p{L} = a letter in any language, \p{M} = an accent mark),
    // plus spaces, periods, apostrophes (straight ' and the curly ’ that phone keyboards type) and hyphens.
    static final String NAME_PATTERN = "^$|^(?=.*\\p{L})[\\p{L}\\p{M} .'’-]+$";

    /**
     * Runs before the checks above: removes spaces at both ends and turns runs of spaces into
     * one, so "  Juan   Carlos " is saved as "Juan Carlos".
     */
    public SaveNameRequest {
        firstName = cleanUp(firstName);
        lastName = cleanUp(lastName);
    }

    private static String cleanUp(String name) {
        return (name == null) ? null : name.trim().replaceAll("\\s+", " ");
    }
}
