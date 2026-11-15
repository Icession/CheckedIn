package edu.cit.checkedin.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The sign-in code rules, read from application.properties ("checkedin.login-code.*").
 * Covers NFR-008: codes expire after 10 minutes, allow 5 wrong tries, and an email can ask
 * for at most 3 codes every 15 minutes. The numbers live in the settings, not in the code.
 *
 * @param expiryMinutes        how long a code works
 * @param maxWrongTries        wrong tries allowed before the code stops working
 * @param maxRequests          codes one email may ask for within the request window
 * @param requestWindowMinutes length of the request window
 */
@ConfigurationProperties(prefix = "checkedin.login-code")
public record LoginCodeSettings(int expiryMinutes, int maxWrongTries, int maxRequests, int requestWindowMinutes) {
}
