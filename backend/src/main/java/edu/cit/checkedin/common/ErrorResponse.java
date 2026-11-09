package edu.cit.checkedin.common;

/**
 * JSON the backend returns for every error: { "message": "..." }.
 * The message says what went wrong and how to fix it (NFR-020).
 *
 * @param message plain-language explanation shown to the user
 */
public record ErrorResponse(String message) {
}
