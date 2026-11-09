package edu.cit.checkedin.auth;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * A person who signs in to CheckedIn with their CIT-U Microsoft account: a professor or a student.
 * One row in the "users" table (see the ERD in the SRS, Figure 2).
 * Covers FR-003 (account created on first sign-in), FR-004 (role) and FR-005 (linked student ID).
 */
@Entity
// "user" is a reserved word in PostgreSQL, so the table is named "users"
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    // The ID Microsoft gives this account. It never changes, so we use it to find the user at each sign-in.
    @Column(name = "ms_object_id", length = 64, nullable = false, unique = true)
    private String msObjectId;

    @Column(name = "email", length = 120, nullable = false, unique = true)
    private String email;

    @Column(name = "first_name", length = 60, nullable = false)
    private String firstName;

    @Column(name = "last_name", length = 60, nullable = false)
    private String lastName;

    // Stored as text ("PROFESSOR"/"STUDENT"), so the database stays readable
    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 20, nullable = false)
    private Role role;

    // Format 00-0000-000, filled only for students. PostgreSQL allows many NULLs in a unique column,
    // so professors (no student number) do not clash, but two students can never share one ID.
    @Column(name = "student_number", columnDefinition = "char(11)", unique = true)
    private String studentNumber;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    /** Empty constructor required by JPA. Our own code uses the constructor below. */
    protected User() {
    }

    public User(String msObjectId, String email, String firstName, String lastName, Role role) {
        this.msObjectId = msObjectId;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
    }

    /** Sets the creation time automatically right before the user is first saved. */
    @PrePersist
    private void recordCreationTime() {
        this.createdAt = LocalDateTime.now();
    }

    public boolean isProfessor() {
        return role == Role.PROFESSOR;
    }

    /**
     * Saves the student ID the student typed on first sign-in (FR-005).
     * The format and "already linked" checks happen in the service before this is called.
     */
    public void linkStudentNumber(String studentNumber) {
        this.studentNumber = studentNumber;
    }

    // ---------- Getters and setters ----------
    // There are no setters for userId, msObjectId, and createdAt because they never change.

    public Long getUserId() {
        return userId;
    }

    public String getMsObjectId() {
        return msObjectId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
}
