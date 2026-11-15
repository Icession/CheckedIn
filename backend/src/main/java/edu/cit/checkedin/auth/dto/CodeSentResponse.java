package edu.cit.checkedin.auth.dto;

/**
 * JSON returned by POST /api/auth/code after the code was emailed.
 *
 * @param email            the cleaned-up address the code was sent to
 * @param expiresInMinutes how long the code works, so the page can tell the user
 */
public record CodeSentResponse(String email, int expiresInMinutes) {
}
