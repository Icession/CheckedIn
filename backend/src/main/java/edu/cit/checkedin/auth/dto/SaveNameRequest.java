package edu.cit.checkedin.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import edu.cit.checkedin.auth.NameRules;

/**
 * JSON for POST /api/auth/name: the name every new user enters on first sign-in (FR-003).
 * The rules are shared with the profile page (see NameRules): required, at most 50 characters, letters
 * from any language, spaces, hyphens, apostrophes and periods, e.g. "Dela Cruz", "O'Neil", "Ma. Clara".
 *
 * @param firstName the user's first name, cleaned up (see NameRules.cleanUp)
 * @param lastName  the user's last name, cleaned up (see NameRules.cleanUp)
 */
public record SaveNameRequest(
        @NotBlank(message = NameRules.FIRST_NAME_MISSING)
        @Size(max = NameRules.MAX_LENGTH, message = NameRules.FIRST_NAME_TOO_LONG)
        @Pattern(regexp = NameRules.PATTERN, message = NameRules.FIRST_NAME_CHARACTERS)
        String firstName,

        @NotBlank(message = NameRules.LAST_NAME_MISSING)
        @Size(max = NameRules.MAX_LENGTH, message = NameRules.LAST_NAME_TOO_LONG)
        @Pattern(regexp = NameRules.PATTERN, message = NameRules.LAST_NAME_CHARACTERS)
        String lastName) {

    /** Runs before the checks above, so "  Juan   Carlos " is checked and saved as "Juan Carlos". */
    public SaveNameRequest {
        firstName = NameRules.cleanUp(firstName);
        lastName = NameRules.cleanUp(lastName);
    }
}
