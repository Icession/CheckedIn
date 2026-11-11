package edu.cit.checkedin.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

/** Checks that {placeholders} in an email body are filled in with the given values. */
class EmailTemplateTest {

    @Test
    void fillsInThePlaceholders() {
        String body = EmailTemplate.TEST_EMAIL.fillBody(Map.of("sentAt", "Nov 11, 2026 8:00 AM"));

        assertThat(body).contains("sent at Nov 11, 2026 8:00 AM").doesNotContain("{sentAt}");
    }

    @Test
    void leavesTheBodyReadableWhenNoValuesAreGiven() {
        String body = EmailTemplate.TEST_EMAIL.fillBody(Map.of());

        assertThat(body).startsWith("Hello!");
    }
}
