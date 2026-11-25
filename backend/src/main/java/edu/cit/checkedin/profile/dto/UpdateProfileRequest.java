package edu.cit.checkedin.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import edu.cit.checkedin.auth.NameRules;

/**
 * JSON for PUT /api/profile: the only things a user may change on their profile, their first and last name
 * (Increment 2 / SRS v1.4: edit profile). Same rules as the name form on first sign-in (see NameRules), so
 * "Ma. Cristina" and "Dela Cruz" pass. There is no email, role, student ID or user ID here on purpose: the
 * email is the identity, the role comes from PROFESSOR_EMAILS, and the student ID links to class records.
 * Extra fields sent in the JSON are ignored.
 *
 * @param firstName the new first name, cleaned up (see NameRules.cleanUp)
 * @param lastName  the new last name, cleaned up (see NameRules.cleanUp)
 */
public record UpdateProfileRequest(
        @NotBlank(message = NameRules.FIRST_NAME_MISSING)
        @Size(max = NameRules.MAX_LENGTH, message = NameRules.FIRST_NAME_TOO_LONG)
        @Pattern(regexp = NameRules.PATTERN, message = NameRules.FIRST_NAME_CHARACTERS)
        String firstName,

        @NotBlank(message = NameRules.LAST_NAME_MISSING)
        @Size(max = NameRules.MAX_LENGTH, message = NameRules.LAST_NAME_TOO_LONG)
        @Pattern(regexp = NameRules.PATTERN, message = NameRules.LAST_NAME_CHARACTERS)
        String lastName) {

    /** Runs before the checks above, so "  Ma.   Cristina " is checked and saved as "Ma. Cristina". */
    public UpdateProfileRequest {
        firstName = NameRules.cleanUp(firstName);
        lastName = NameRules.cleanUp(lastName);
    }
}
