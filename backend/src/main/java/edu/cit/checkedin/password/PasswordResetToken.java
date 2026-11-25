package edu.cit.checkedin.password;

import java.time.LocalDateTime;

import org.hibernate.annotations.Check;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import edu.cit.checkedin.auth.User;

/**
 * One "reset your password" link (Increment 2 / SRS v1.4: forgot password). The link holds a long random token;
 * only its SHA-256 hash is stored, so someone who reads the database still can't use a link. A link works once and
 * for 30 minutes (PasswordResetSettings). Setting a new password uses up every open link of that user.
 */
@Entity
@Table(name = "password_reset_tokens")
@Check(name = "password_reset_tokens_hash_is_sha256", constraints = "length(token_hash) = 64")
@Check(name = "password_reset_tokens_expire_after_creation", constraints = "expires_at > created_at")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_id")
    private Long tokenId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // SHA-256 of the token in the link, as 64 hex characters (never the token itself)
    @Column(name = "token_hash", length = 64, nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // Set when the link was used, or when a later password change made it useless
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** Empty constructor required by JPA. */
    protected PasswordResetToken() {
    }

    public PasswordResetToken(User user, String tokenHash, LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public boolean isExpired(LocalDateTime now) {
        return !now.isBefore(expiresAt);
    }

    public void markUsed(LocalDateTime now) {
        this.usedAt = now;
    }

    // ---------- Getters (no setters: a link only changes by being used) ----------

    public Long getTokenId() {
        return tokenId;
    }

    public User getUser() {
        return user;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
