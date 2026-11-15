package edu.cit.checkedin.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Tests GitHubIdentityProvider against a fake GitHub server (NFR-008).
 * No internet connection and no real GitHub account are needed.
 */
class GitHubIdentityProviderTest {

    private static final String EMAILS_URL = "https://api.github.com/user/emails?per_page=100";

    private MockRestServiceServer fakeGitHub;
    private GitHubIdentityProvider provider;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        fakeGitHub = MockRestServiceServer.bindTo(restClientBuilder).build();
        provider = new GitHubIdentityProvider(GitHubIdentityProvider.createRestClient(restClientBuilder));
    }

    @Test
    void returnsOnlyVerifiedEmailsWithThePrimaryFirst() {
        fakeGitHub.expect(requestTo(EMAILS_URL))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andExpect(header("X-GitHub-Api-Version", "2026-03-10"))
                .andRespond(withSuccess("""
                        [
                          {"email": "juan.personal@gmail.com", "primary": false, "verified": true, "visibility": null},
                          {"email": "someone.else@cit.edu", "primary": false, "verified": false, "visibility": null},
                          {"email": "juan.delacruz@cit.edu", "primary": true, "verified": true, "visibility": "public"}
                        ]
                        """, MediaType.APPLICATION_JSON));

        List<String> verifiedEmails = provider.getVerifiedEmails("test-token");

        // The unverified @cit.edu address is left out: anyone can type an address into GitHub
        assertThat(verifiedEmails).containsExactly("juan.delacruz@cit.edu", "juan.personal@gmail.com");
        fakeGitHub.verify();
    }

    @Test
    void turnsAGitHubErrorIntoAnIdentityProviderException() {
        fakeGitHub.expect(requestTo(EMAILS_URL))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\": \"Bad credentials\"}"));

        assertThatThrownBy(() -> provider.getVerifiedEmails("test-token"))
                .isInstanceOf(IdentityProviderException.class)
                .hasMessageNotContaining("test-token");
    }
}
