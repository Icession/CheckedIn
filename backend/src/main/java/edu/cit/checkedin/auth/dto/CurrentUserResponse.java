package edu.cit.checkedin.auth.dto;

import edu.cit.checkedin.auth.Role;
import edu.cit.checkedin.auth.User;

/**
 * JSON describing who is signed in (returned after sign-in, after saving the name, and by
 * GET /api/auth/me). Covers FR-003: needsName is true until the person has entered their name,
 * because the account is created only then.
 *
 * @param email     the signed-in user's verified @cit.edu address
 * @param firstName null until the account exists
 * @param lastName  null until the account exists
 * @param role      PROFESSOR or STUDENT; null until the account exists
 * @param needsName true when the page must show the name form first
 */
public record CurrentUserResponse(String email, String firstName, String lastName, Role role, boolean needsName) {

    /** Someone who proved their email but has no account yet (they still have to enter their name). */
    public static CurrentUserResponse withoutAccount(String verifiedEmail) {
        return new CurrentUserResponse(verifiedEmail, null, null, null, true);
    }

    public static CurrentUserResponse from(User user) {
        return new CurrentUserResponse(user.getEmail(), user.getFirstName(), user.getLastName(), user.getRole(), false);
    }
}
