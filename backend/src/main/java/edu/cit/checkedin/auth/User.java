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

import org.hibernate.annotations.Check;

/**
 * A person who signs in to CheckedIn with their @cit.edu email (emailed code or GitHub):
 * a professor or a student. One row in the "users" table (see the ERD in the SRS, Figure 2).
 * Covers FR-003 (account created on first sign-in), FR-004 (role) and FR-005 (linked student ID).
 * Increment 2 / SRS v1.4 adds an optional password, stored only as a BCrypt hash.
 */
@Entity
// "user" is a reserved word in PostgreSQL, so the table is named "users"
@Table(name = "users")
// The database itself refuses a student ID in any other format, even one typed by hand in the
// Supabase table editor (where typing NULL would otherwise save the text "NULL")
@Check(name = "users_student_number_format",
        constraints = "student_number IS NULL OR student_number ~ '^[0-9]{2}-[0-9]{4}-[0-9]{3}$'")
// Only a BCrypt hash (60 characters, starting with $2) can be saved, never a password typed in by hand
@Check(name = "users_password_hash_is_bcrypt",
        constraints = "password_hash IS NULL OR (length(password_hash) = 60 AND password_hash LIKE '$2%')")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    // The numeric account ID from GitHub. Filled only for users who have signed in with GitHub.
    @Column(name = "github_id", unique = true)
    private Long githubId;

    // Both sign-in methods end with a proven @cit.edu email, so we find users by their email
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

    // Increment 2 / SRS v1.4: the BCrypt hash of the password, never the password itself. Null until the user
    // creates one (accounts from before Increment 2 set one with "Forgot password?"). No response DTO has this field,
    // so it never reaches the browser.
    @Column(name = "password_hash", length = 60)
    private String passwordHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    /** Empty constructor required by JPA. Our own code uses the constructor below. */
    protected User() {
    }

    public User(String email, String firstName, String lastName, Role role) {
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
    // There are no setters for userId and createdAt because they never change.

    public Long getUserId() {
        return userId;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Long getGithubId() {
        return githubId;
    }

    public void setGithubId(Long githubId) {
        this.githubId = githubId;
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
