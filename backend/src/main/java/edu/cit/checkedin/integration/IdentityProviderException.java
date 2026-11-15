package edu.cit.checkedin.integration;

/**
 * Thrown when the sign-in provider (GitHub) could not be asked for the user's emails,
 * for example because it was down or rejected the token. The sign-in then fails with a
 * "please try again" message instead of an error page.
 */
public class IdentityProviderException extends RuntimeException {

    public IdentityProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
