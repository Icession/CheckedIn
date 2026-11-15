package edu.cit.checkedin.auth;

import java.time.LocalDateTime;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

/**
 * Database queries for emailed sign-in codes.
 * Covers NFR-008 (request limit and single newest code per email).
 */
public interface LoginCodeRepository extends JpaRepository<LoginCode, Long> {

    // How many codes this email asked for since a given time (at most 3 every 15 minutes)
    long countByEmailAndCreatedAtAfter(String email, LocalDateTime since);

    // Only the newest code (highest ID) for an email can be used. The row is locked while it is
    // checked, so two requests at the same moment cannot both use up the same wrong try.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<LoginCode> findFirstByEmailOrderByCodeIdDesc(String email);
}
