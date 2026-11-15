package edu.cit.checkedin.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * JSON for POST /api/auth/code/verify: the email and the 6-digit code from the email (FR-001).
 *
 * @param email the same address the code was sent to
 * @param code  the 6 digits the user typed
 */
public record VerifyCodeRequest(
        @NotBlank(message = "Please enter your CIT-U email address.")
        String email,

        @NotBlank(message = "Please enter the 6-digit code from the email.")
        @Pattern(regexp = "^\\d{6}$", message = "The code has exactly 6 digits. Please check it and try again.")
        String code) {
}
