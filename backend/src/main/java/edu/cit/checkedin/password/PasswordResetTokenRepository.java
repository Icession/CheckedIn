package edu.cit.checkedin.password;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.cit.checkedin.auth.User;

/** Database access for reset links (Increment 2 / SRS v1.4: forgot password). */
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /** How many links the user asked for since the given time (at most 3 per 15 minutes). */
    long countByUserAndCreatedAtAfter(User user, LocalDateTime since);

    /** The user's links that were not used yet (expired ones included; using them up does no harm). */
    List<PasswordResetToken> findByUserAndUsedAtIsNull(User user);
}
