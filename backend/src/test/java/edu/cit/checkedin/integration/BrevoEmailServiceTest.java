package edu.cit.checkedin.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Tests BrevoEmailService against a fake Brevo server (MockRestServiceServer).
 * No real email is sent and no internet connection is needed.
 */
class BrevoEmailServiceTest {

    private static final String API_KEY = "test-api-key";
    private static final String BREVO_SEND_URL = "https://api.brevo.com/v3/smtp/email";

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer fakeBrevo;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        fakeBrevo = MockRestServiceServer.bindTo(restClientBuilder).build();
    }

    private BrevoEmailService createService(String apiKey) {
        RestClient restClient = BrevoEmailService.createRestClient(restClientBuilder, apiKey);
        return new BrevoEmailService(restClient, apiKey, "checkedin.sender@example.com", "CheckedIn");
    }

    @Test
    void sendsTheEmailAndReturnsBrevosMessageId() {
        fakeBrevo.expect(requestTo(BREVO_SEND_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", API_KEY))
                .andExpect(jsonPath("$.sender.email").value("checkedin.sender@example.com"))
                .andExpect(jsonPath("$.sender.name").value("CheckedIn"))
                .andExpect(jsonPath("$.to[0].email").value("student@cit.edu"))
                .andExpect(jsonPath("$.to[0].name").doesNotExist())
                .andExpect(jsonPath("$.subject").value("CheckedIn test email"))
                .andExpect(jsonPath("$.textContent").value(containsString("sent at Nov 11, 2026 8:00 AM")))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"messageId\": \"<202611110800.12345@smtp-relay.mailin.fr>\"}"));

        String messageId = createService(API_KEY)
                .send("student@cit.edu", EmailTemplate.TEST_EMAIL, Map.of("sentAt", "Nov 11, 2026 8:00 AM"));

        assertThat(messageId).isEqualTo("<202611110800.12345@smtp-relay.mailin.fr>");
        fakeBrevo.verify();
    }

    @Test
    void throwsWhenBrevoRejectsTheEmail() {
        fakeBrevo.expect(requestTo(BREVO_SEND_URL))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\": \"unauthorized\", \"message\": \"Key not found\"}"));

        BrevoEmailService service = createService(API_KEY);

        assertThatThrownBy(() -> service.send("student@cit.edu", EmailTemplate.TEST_EMAIL, Map.of()))
                .isInstanceOf(EmailSendingException.class)
                .hasMessageContaining("401")
                // The API key must never appear in an error message
                .hasMessageNotContaining(API_KEY);
        fakeBrevo.verify();
    }

    @Test
    void throwsWithoutCallingBrevoWhenTheApiKeyIsMissing() {
        BrevoEmailService service = createService("");

        assertThatThrownBy(() -> service.send("student@cit.edu", EmailTemplate.TEST_EMAIL, Map.of()))
                .isInstanceOf(EmailSendingException.class)
                .hasMessageContaining("BREVO_API_KEY");
        // No request was expected, so verify() fails if one was sent
        fakeBrevo.verify();
    }
}
