package edu.cit.checkedin.integration;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Sends emails through Brevo's transactional email API (POST https://api.brevo.com/v3/smtp/email).
 * Covers FR-001 (sign-in codes are emailed) and NFR-026 (Brevo is used only through EmailService).
 * The API key and sender come from backend/.env (see README, "Set up Brevo").
 */
@Service
public class BrevoEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(BrevoEmailService.class);

    private static final String BREVO_API_URL = "https://api.brevo.com/v3";
    // Without time limits, a slow Brevo would make a sign-in request wait forever
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    private final RestClient restClient;
    private final String apiKey;
    private final String senderEmail;
    private final String senderName;

    /** Used by Spring: reads the Brevo settings and adds the time limits. */
    @Autowired
    public BrevoEmailService(RestClient.Builder restClientBuilder,
                             @Value("${brevo.api-key}") String apiKey,
                             @Value("${brevo.sender-email}") String senderEmail,
                             @Value("${brevo.sender-name}") String senderName) {
        this(createRestClient(restClientBuilder.requestFactory(requestFactoryWithTimeouts()), apiKey),
                apiKey, senderEmail, senderName);
    }

    /** Used by the tests, which pass a RestClient connected to a fake Brevo server. */
    BrevoEmailService(RestClient restClient, String apiKey, String senderEmail, String senderName) {
        this.restClient = restClient;
        this.apiKey = apiKey;
        this.senderEmail = senderEmail;
        this.senderName = senderName;
    }

    /** Builds the HTTP client for Brevo: Brevo's address, plus our API key on every request. */
    static RestClient createRestClient(RestClient.Builder restClientBuilder, String apiKey) {
        return restClientBuilder
                .baseUrl(BREVO_API_URL)
                .defaultHeader("api-key", apiKey)
                .build();
    }

    @Override
    public String send(String toAddress, EmailTemplate template, Map<String, String> data) {
        if (apiKey.isBlank() || senderEmail.isBlank()) {
            throw new EmailSendingException(
                    "Brevo is not set up. Add BREVO_API_KEY and BREVO_SENDER_EMAIL to backend/.env (see README).");
        }
        BrevoEmailRequest request = new BrevoEmailRequest(
                new BrevoContact(senderEmail, senderName),
                List.of(new BrevoContact(toAddress, null)),
                template.getSubject(),
                template.fillBody(data));
        return postToBrevo(request, template);
    }

    // Sends the request and turns every kind of Brevo failure into one EmailSendingException
    private String postToBrevo(BrevoEmailRequest request, EmailTemplate template) {
        try {
            BrevoEmailResponse response = restClient.post()
                    .uri("/smtp/email")
                    .body(request)
                    .retrieve()
                    .body(BrevoEmailResponse.class);
            if (response == null || response.messageId() == null) {
                throw new EmailSendingException("Brevo accepted the " + template + " email but returned no message ID.");
            }
            return response.messageId();
        } catch (RestClientResponseException exception) {
            // Brevo answered with an error, e.g. wrong API key, unverified sender, or blocked IP address
            log.error("Brevo rejected the {} email ({}): {}",
                    template, exception.getStatusCode(), exception.getResponseBodyAsString());
            throw new EmailSendingException("Brevo rejected the email (" + exception.getStatusCode() + ").", exception);
        } catch (RestClientException exception) {
            // Brevo could not be reached, e.g. no internet connection or it took too long to answer
            log.error("Could not reach Brevo to send the {} email", template, exception);
            throw new EmailSendingException("Could not reach Brevo.", exception);
        }
    }

    private static SimpleClientHttpRequestFactory requestFactoryWithTimeouts() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        return requestFactory;
    }

    // ---------- JSON that Brevo expects and returns ----------

    /** One sender or recipient. A null name is left out of the JSON instead of being sent as null. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record BrevoContact(String email, String name) {
    }

    /** Body of POST /smtp/email. We send plain text only, so no HTML can be slipped into an email. */
    record BrevoEmailRequest(BrevoContact sender, List<BrevoContact> to, String subject, String textContent) {
    }

    /** Brevo's answer: the ID of the queued message. */
    record BrevoEmailResponse(String messageId) {
    }
}
