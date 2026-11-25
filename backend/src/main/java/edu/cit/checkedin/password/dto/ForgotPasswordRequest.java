package edu.cit.checkedin.password.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * JSON for POST /api/auth/password/forgot (Increment 2 / SRS v1.4): the email that should get a reset link.
 *
 * @param email the user's @cit.edu address
 */
public record ForgotPasswordRequest(
        @NotBlank(message = "Please enter your CIT-U email address.")
        @Email(message = "Please enter a valid email address, like juan.delacruz@cit.edu.")
        @Size(max = 120, message = "This email address is too long.")
        String email) {
}
