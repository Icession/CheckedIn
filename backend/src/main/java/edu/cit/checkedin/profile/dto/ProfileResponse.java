package edu.cit.checkedin.profile.dto;

import java.time.LocalDate;

import edu.cit.checkedin.auth.Role;
import edu.cit.checkedin.auth.User;

/**
 * What the profile page shows about the signed-in user (Increment 2 / SRS v1.4: view profile).
 * Never contains anything secret (no IDs, no GitHub ID, no sign-in codes).
 *
 * @param email         the @cit.edu email, which identifies the account (read-only)
 * @param firstName     editable
 * @param lastName      editable
 * @param role          PROFESSOR or STUDENT, from the professor list right now (FR-004, FR-008; read-only)
 * @param studentNumber the linked student ID, e.g. "22-1234-567"; null for professors (FR-005; read-only)
 * @param createdOn     the day the account was created, in Philippine time (NFR-035)
 */
public record ProfileResponse(String email, String firstName, String lastName, Role role,
                              String studentNumber, LocalDate createdOn) {

    public static ProfileResponse from(User user, Role currentRole) {
        return new ProfileResponse(user.getEmail(), user.getFirstName(), user.getLastName(), currentRole,
                user.getStudentNumber(), user.getCreatedAt().toLocalDate());
    }
}
