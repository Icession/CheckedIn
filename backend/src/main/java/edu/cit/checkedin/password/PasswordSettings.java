package edu.cit.checkedin.password;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The password sign-in limits, read from application.properties ("checkedin.password.*").
 * Increment 2 / SRS v1.4: after 5 wrong passwords for one email, password sign-in for that email is blocked for
 * 15 minutes. The numbers live in the settings, not in the code.
 *
 * @param maxWrongTries  wrong passwords in a row (within the lockout window) that start a lockout
 * @param lockoutMinutes how long the lockout lasts, and how long a wrong try keeps counting
 */
@ConfigurationProperties(prefix = "checkedin.password")
public record PasswordSettings(int maxWrongTries, int lockoutMinutes) {
}
