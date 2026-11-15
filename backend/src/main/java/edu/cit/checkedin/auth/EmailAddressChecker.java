package edu.cit.checkedin.auth;

import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The one place that cleans up email addresses and checks their domain.
 * Covers FR-002: only CIT-U addresses (@cit.edu, set in application.properties) may sign in.
 * Used by both sign-in methods: emailed code and GitHub.
 */
@Component
public class EmailAddressChecker {

    private final List<String> allowedDomains;

    public EmailAddressChecker(@Value("${checkedin.auth.allowed-email-domains}") List<String> allowedDomains) {
        this.allowedDomains = allowedDomains.stream()
                .map(domain -> domain.trim().toLowerCase(Locale.ROOT))
                .toList();
    }

    /**
     * Removes spaces and makes the email lowercase, so "Kurt@CIT.edu " and "kurt@cit.edu"
     * are always treated as the same person.
     */
    public String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * True only when the (normalized) email ends with "@" plus an allowed domain.
     * Checking "@cit.edu" instead of just "cit.edu" rejects look-alikes such as "x@evil-cit.edu".
     */
    public boolean isAllowed(String normalizedEmail) {
        return allowedDomains.stream().anyMatch(domain -> normalizedEmail.endsWith("@" + domain));
    }
}
