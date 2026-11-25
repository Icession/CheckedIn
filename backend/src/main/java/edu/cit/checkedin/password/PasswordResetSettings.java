package edu.cit.checkedin.password;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The reset-link rules, read from application.properties ("checkedin.password-reset.*").
 * Increment 2 / SRS v1.4: a link works once and for 30 minutes, and one email can ask for at most 3 links every
 * 15 minutes. The numbers live in the settings, not in the code.
 *
 * @param expiryMinutes        how long a reset link works
 * @param maxRequests          links one email may ask for within the request window
 * @param requestWindowMinutes length of the request window
 */
@ConfigurationProperties(prefix = "checkedin.password-reset")
public record PasswordResetSettings(int expiryMinutes, int maxRequests, int requestWindowMinutes) {
}
