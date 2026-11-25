package edu.cit.checkedin.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import edu.cit.checkedin.auth.NameRules;
import edu.cit.checkedin.password.PasswordRules;

/**
 * JSON for POST /api/auth/name: what every new user enters on first sign-in, whichever way they signed in:
 * their name (FR-003) and, since Increment 2 / SRS v1.4, their first password.
 * Name rules are shared with the profile page (NameRules): required, at most 50 characters, letters from any
 * language, spaces, hyphens, apostrophes and periods, e.g. "Dela Cruz", "O'Neil", "Ma. Clara".
 * Password rules (PasswordRules): 8–64 characters with a letter and a number; AuthService also checks that it
 * isn't the email and that both boxes match.
 *
 * @param firstName       the user's first name, cleaned up (see NameRules.cleanUp)
 * @param lastName        the user's last name, cleaned up (see NameRules.cleanUp)
 * @param password        the new password as typed (never trimmed; only its hash is saved)
 * @param confirmPassword the same password typed again
 */
public record SaveNameRequest(
        @NotBlank(message = NameRules.FIRST_NAME_MISSING)
        @Size(max = NameRules.MAX_LENGTH, message = NameRules.FIRST_NAME_TOO_LONG)
        @Pattern(regexp = NameRules.PATTERN, message = NameRules.FIRST_NAME_CHARACTERS)
        String firstName,

        @NotBlank(message = NameRules.LAST_NAME_MISSING)
        @Size(max = NameRules.MAX_LENGTH, message = NameRules.LAST_NAME_TOO_LONG)
        @Pattern(regexp = NameRules.PATTERN, message = NameRules.LAST_NAME_CHARACTERS)
        String lastName,

        @NotEmpty(message = PasswordRules.MISSING)
        @Size(max = PasswordRules.MAX_LENGTH, message = PasswordRules.TOO_LONG)
        @Pattern(regexp = PasswordRules.PATTERN, message = PasswordRules.TOO_WEAK)
        String password,

        @NotEmpty(message = PasswordRules.CONFIRM_MISSING)
        String confirmPassword) {

    /** Runs before the checks above, so "  Juan   Carlos " is checked and saved as "Juan Carlos". */
    public SaveNameRequest {
        firstName = NameRules.cleanUp(firstName);
        lastName = NameRules.cleanUp(lastName);
    }
}
