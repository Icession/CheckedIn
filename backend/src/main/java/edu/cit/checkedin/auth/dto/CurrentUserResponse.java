package edu.cit.checkedin.auth.dto;

/**
 * JSON describing who is signed in (returned after sign-in and by GET /api/auth/me).
 * For now only the verified email; step 1g adds the name and role.
 *
 * @param email the signed-in user's verified @cit.edu address
 */
public record CurrentUserResponse(String email) {
}
