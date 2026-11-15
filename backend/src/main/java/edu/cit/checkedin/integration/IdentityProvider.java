package edu.cit.checkedin.integration;

import java.util.List;

/**
 * Reads which email addresses an outside sign-in service (GitHub) has verified for the user.
 * Covers NFR-026: the sign-in code talks to this interface, not to GitHub directly,
 * so the provider can be replaced by changing one class (GitHubIdentityProvider).
 */
public interface IdentityProvider {

    /**
     * Returns the email addresses on the user's account that the provider has VERIFIED,
     * with the user's primary address first. Unverified addresses are left out.
     *
     * @param accessToken the short-lived token the provider gave us for this sign-in
     * @throws IdentityProviderException if the list could not be read
     */
    List<String> getVerifiedEmails(String accessToken);
}
