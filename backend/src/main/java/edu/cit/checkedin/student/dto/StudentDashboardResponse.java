package edu.cit.checkedin.student.dto;

import edu.cit.checkedin.auth.User;

/**
 * JSON for GET /api/student/dashboard. Phase 9 adds classes, attendance and absence hours.
 *
 * @param firstName     the student's first name
 * @param lastName      the student's last name
 * @param studentNumber the linked student ID (format 00-0000-000)
 */
public record StudentDashboardResponse(String firstName, String lastName, String studentNumber) {

    public static StudentDashboardResponse from(User student) {
        return new StudentDashboardResponse(student.getFirstName(), student.getLastName(), student.getStudentNumber());
    }
}
