package edu.cit.checkedin.password.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import edu.cit.checkedin.password.PasswordRules;

/**
 * JSON for POST /api/auth/password/reset (Increment 2 / SRS v1.4): the token from the reset link and the new
 * password, with the same rules as everywhere else (PasswordRules).
 *
 * @param token           the token from the link (/reset-password?token=...)
 * @param newPassword     the new password as typed (only its hash is saved)
 * @param confirmPassword the same password typed again
 */
public record ResetPasswordRequest(
        @NotBlank(message = "This reset link is not complete. Please copy the whole link from the email.")
        @Size(max = 100, message = "This reset link is not valid. Please copy the whole link from the email.")
        String token,

        @NotEmpty(message = PasswordRules.MISSING)
        @Size(max = PasswordRules.MAX_LENGTH, message = PasswordRules.TOO_LONG)
        @Pattern(regexp = PasswordRules.PATTERN, message = PasswordRules.TOO_WEAK)
        String newPassword,

        @NotEmpty(message = PasswordRules.CONFIRM_MISSING)
        String confirmPassword) {
}
