package edu.cit.checkedin.auth;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * One emailed sign-in code. One row in the "login_codes" table (see the ERD in the SRS).
 * Covers NFR-008: only a hash of the code is stored, with its expiry, wrong-try count, and
 * the time it was used. There is no link to "users", because a code can be asked for
 * before the person's account exists.
 */
@Entity
@Table(name = "login_codes", indexes = @Index(name = "idx_login_codes_email", columnList = "email"))
public class LoginCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "code_id")
    private Long codeId;

    @Column(name = "email", length = 120, nullable = false)
    private String email;

    // BCrypt hash of the 6-digit code; the code itself is never saved
    @Column(name = "code_hash", length = 100, nullable = false)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // "short" so the database column is a SMALLINT, as in the ERD
    @Column(name = "attempt_count", nullable = false)
    private short attemptCount;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Empty constructor required by JPA. Our own code uses the constructor below. */
    protected LoginCode() {
    }

    public LoginCode(String email, String codeHash, LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.email = email;
        this.codeHash = codeHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.attemptCount = 0;
    }

    public boolean isExpired(LocalDateTime now) {
        return !now.isBefore(expiresAt);
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public void recordWrongTry() {
        attemptCount++;
    }

    /** A code works only once (NFR-008). */
    public void markUsed(LocalDateTime now) {
        this.usedAt = now;
    }

    // ---------- Getters (no setters: a code changes only through the methods above) ----------

    public Long getCodeId() {
        return codeId;
    }

    public String getEmail() {
        return email;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
