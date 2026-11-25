package edu.cit.checkedin.password;

import java.time.LocalDateTime;

import org.hibernate.annotations.Check;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Counts wrong passwords for one email and blocks password sign-in after too many (Increment 2 / SRS v1.4):
 * 5 wrong tries -> blocked for 15 minutes (PasswordSettings). One row per email that was typed, even an email
 * without an account, so the answer never shows whether an account exists. Email-code and GitHub sign-in are
 * not affected.
 */
@Entity
@Table(name = "password_lockouts")
@Check(name = "password_lockouts_wrong_tries_not_negative", constraints = "wrong_tries >= 0")
public class PasswordLockout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lockout_id")
    private Long lockoutId;

    @Column(name = "email", length = 120, nullable = false, unique = true)
    private String email;

    // Wrong tries since the last lockout (or since tries stopped counting)
    @Column(name = "wrong_tries", nullable = false)
    private int wrongTries;

    @Column(name = "last_wrong_try_at")
    private LocalDateTime lastWrongTryAt;

    // Password sign-in for this email is blocked until this time; null when it isn't blocked
    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    /** Empty constructor required by JPA. */
    protected PasswordLockout() {
    }

    public PasswordLockout(String email) {
        this.email = email;
    }

    public boolean isLocked(LocalDateTime now) {
        return lockedUntil != null && now.isBefore(lockedUntil);
    }

    /**
     * One more wrong password. Tries older than the lockout window no longer count, so 4 wrong tries yesterday and
     * 1 today don't block anyone. The last allowed wrong try starts the lockout and the count starts again.
     */
    public void recordWrongTry(LocalDateTime now, PasswordSettings settings) {
        boolean earlierTriesExpired = lastWrongTryAt == null
                || lastWrongTryAt.isBefore(now.minusMinutes(settings.lockoutMinutes()));
        wrongTries = earlierTriesExpired ? 1 : wrongTries + 1;
        lastWrongTryAt = now;
        if (wrongTries >= settings.maxWrongTries()) {
            lockedUntil = now.plusMinutes(settings.lockoutMinutes());
            wrongTries = 0;
        }
    }

    // ---------- Getters (no setters: only recordWrongTry changes a lockout) ----------

    public Long getLockoutId() {
        return lockoutId;
    }

    public String getEmail() {
        return email;
    }

    public int getWrongTries() {
        return wrongTries;
    }

    public LocalDateTime getLastWrongTryAt() {
        return lastWrongTryAt;
    }

    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }
}
