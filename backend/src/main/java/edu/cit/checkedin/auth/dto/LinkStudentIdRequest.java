package edu.cit.checkedin.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * JSON for POST /api/auth/link-student-id: the student ID a student enters once (FR-005).
 * Accepted only in the CIT-U format 00-0000-000, for example 12-3456-789.
 *
 * @param studentNumber the student ID, with spaces at both ends removed
 */
public record LinkStudentIdRequest(
        @NotBlank(message = "Please enter your student ID.")
        @Pattern(regexp = STUDENT_ID_PATTERN,
                message = "Please enter your student ID in the format 00-0000-000, for example 12-3456-789.")
        String studentNumber) {

    // Empty is let through here so that only the "Please enter..." message shows for a blank ID
    static final String STUDENT_ID_PATTERN = "^$|^\\d{2}-\\d{4}-\\d{3}$";

    /** Runs before the checks above: removes spaces at both ends (" 12-3456-789 " -> "12-3456-789"). */
    public LinkStudentIdRequest {
        studentNumber = (studentNumber == null) ? null : studentNumber.trim();
    }
}
