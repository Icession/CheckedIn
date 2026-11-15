package edu.cit.checkedin.integration;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Reads the verified email addresses of a GitHub account (GET https://api.github.com/user/emails).
 * Covers FR-002 (a GitHub sign-in is accepted only with a verified @cit.edu email) and NFR-026.
 * Needs the "user:email" permission, which we ask for when the user signs in.
 */
@Component
public class GitHubIdentityProvider implements IdentityProvider {

    private static final Logger log = LoggerFactory.getLogger(GitHubIdentityProvider.class);

    private static final String GITHUB_API_URL = "https://api.github.com";
    // The GitHub REST API version this code was written against
    private static final String GITHUB_API_VERSION = "2026-03-10";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    private final RestClient restClient;

    /** Used by Spring: adds the time limits. */
    @Autowired
    public GitHubIdentityProvider(RestClient.Builder restClientBuilder) {
        this(createRestClient(restClientBuilder.requestFactory(requestFactoryWithTimeouts())));
    }

    /** Used by the tests, which pass a RestClient connected to a fake GitHub server. */
    GitHubIdentityProvider(RestClient restClient) {
        this.restClient = restClient;
    }

    /** Builds the HTTP client for GitHub's API with the headers GitHub asks for. */
    static RestClient createRestClient(RestClient.Builder restClientBuilder) {
        return restClientBuilder
                .baseUrl(GITHUB_API_URL)
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", GITHUB_API_VERSION)
                .build();
    }

    @Override
    public List<String> getVerifiedEmails(String accessToken) {
        GitHubEmail[] emails;
        try {
            emails = restClient.get()
                    .uri("/user/emails?per_page=100")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(GitHubEmail[].class);
        } catch (RestClientException exception) {
            // The token is only in the request header, so it never appears in this log line
            log.error("Could not read the email list from GitHub", exception);
            throw new IdentityProviderException("Could not read the email list from GitHub.", exception);
        }
        return keepVerifiedPrimaryFirst(emails);
    }

    // Drops unverified addresses (anyone can add those) and puts the primary address first
    private List<String> keepVerifiedPrimaryFirst(GitHubEmail[] emails) {
        List<String> verifiedEmails = new ArrayList<>();
        if (emails == null) {
            return verifiedEmails;
        }
        for (GitHubEmail email : emails) {
            if (!email.verified()) {
                continue;
            }
            if (email.primary()) {
                verifiedEmails.add(0, email.email());
            } else {
                verifiedEmails.add(email.email());
            }
        }
        return verifiedEmails;
    }

    private static SimpleClientHttpRequestFactory requestFactoryWithTimeouts() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        return requestFactory;
    }

    /** One item of GitHub's answer. GitHub also sends "visibility", which we don't need. */
    record GitHubEmail(String email, boolean primary, boolean verified) {
    }
}
