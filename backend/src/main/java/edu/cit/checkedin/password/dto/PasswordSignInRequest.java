package edu.cit.checkedin.password.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/**
 * JSON for POST /api/auth/password/sign-in (Increment 2 / SRS v1.4: sign in with a password).
 *
 * @param email    the user's @cit.edu address (cleaned up and checked by EmailAddressChecker)
 * @param password the password as typed (never trimmed, never stored or logged)
 */
public record PasswordSignInRequest(
        @NotBlank(message = "Please enter your CIT-U email address.")
        @Size(max = 120, message = "This email address is too long.")
        String email,

        @NotEmpty(message = "Please enter your password.")
        @Size(max = 200, message = "Email or password is incorrect.")
        String password) {
}
