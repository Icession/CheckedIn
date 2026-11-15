package edu.cit.checkedin.auth;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Database queries for users.
 * Spring Data writes the SQL for us from the method names.
 * Covers FR-003 (find or create the user at sign-in) and FR-006 (one student ID per account).
 */
public interface UserRepository extends JpaRepository<User, Long> {

    // FR-003: after an email-code or GitHub sign-in, look up the user by their proven email
    Optional<User> findByEmail(String email);

    // FR-001: which account (if any) already uses this GitHub account
    Optional<User> findByGithubId(Long githubId);

    // FR-006: reject a student ID that is already linked to another account
    boolean existsByStudentNumber(String studentNumber);
}
