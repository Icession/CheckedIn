package edu.cit.checkedin.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * JSON for POST /api/auth/code: the email that should receive a sign-in code (FR-001).
 *
 * @param email the user's @cit.edu address
 */
public record RequestCodeRequest(
        @NotBlank(message = "Please enter your CIT-U email address.")
        @Email(message = "Please enter a valid email address, like juan.delacruz@cit.edu.")
        @Size(max = 120, message = "This email address is too long.")
        String email) {
}
