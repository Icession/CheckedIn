package edu.cit.checkedin.auth.dto;

import edu.cit.checkedin.auth.Role;
import edu.cit.checkedin.auth.User;

/**
 * JSON describing who is signed in (returned after sign-in, after each first-sign-in step, and by
 * GET /api/auth/me). It tells the page what to show next:
 * needsName (FR-003): the account is created only after the name is entered;
 * needsStudentId (FR-005): a student must enter their student ID once.
 *
 * @param email          the signed-in user's verified @cit.edu address
 * @param firstName      null until the account exists
 * @param lastName       null until the account exists
 * @param role           PROFESSOR or STUDENT; null until the account exists
 * @param studentNumber  the linked student ID, or null
 * @param needsName      true when the page must show the name form first
 * @param needsStudentId true when a student has not linked a student ID yet
 */
public record CurrentUserResponse(String email, String firstName, String lastName, Role role,
                                  String studentNumber, boolean needsName, boolean needsStudentId) {

    /** Someone who proved their email but has no account yet (they still have to enter their name). */
    public static CurrentUserResponse withoutAccount(String verifiedEmail) {
        return new CurrentUserResponse(verifiedEmail, null, null, null, null, true, false);
    }

    public static CurrentUserResponse from(User user) {
        boolean needsStudentId = user.getRole() == Role.STUDENT && user.getStudentNumber() == null;
        return new CurrentUserResponse(user.getEmail(), user.getFirstName(), user.getLastName(), user.getRole(),
                user.getStudentNumber(), false, needsStudentId);
    }
}
