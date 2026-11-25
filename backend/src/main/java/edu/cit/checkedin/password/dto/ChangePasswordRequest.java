package edu.cit.checkedin.password.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import edu.cit.checkedin.password.PasswordRules;

/**
 * JSON for PUT /api/profile/password (Increment 2 / SRS v1.4: change password). There is no email or user ID here:
 * the backend always changes the password of the person signed in on this session.
 *
 * @param currentPassword the password the user has now (checked with BCrypt)
 * @param newPassword     the new password (PasswordRules; must also differ from the current one)
 * @param confirmPassword the new password typed again
 */
public record ChangePasswordRequest(
        @NotEmpty(message = "Please enter your current password.")
        @Size(max = 200, message = "Your current password is incorrect. Please try again.")
        String currentPassword,

        @NotEmpty(message = PasswordRules.MISSING)
        @Size(max = PasswordRules.MAX_LENGTH, message = PasswordRules.TOO_LONG)
        @Pattern(regexp = PasswordRules.PATTERN, message = PasswordRules.TOO_WEAK)
        String newPassword,

        @NotEmpty(message = PasswordRules.CONFIRM_MISSING)
        String confirmPassword) {
}
