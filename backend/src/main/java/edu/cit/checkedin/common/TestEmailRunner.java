package edu.cit.checkedin.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import edu.cit.checkedin.integration.EmailSendingException;
import edu.cit.checkedin.integration.EmailService;
import edu.cit.checkedin.integration.EmailTemplate;

/**
 * Setup check (step 1d): sends one test email to prove that Brevo works and that the email
 * reaches a @cit.edu inbox. It exists ONLY when the app is started with
 * --checkedin.test-email-to=you@cit.edu (see README, "Set up Brevo"), and it is not a web
 * endpoint, so nobody can use it to send emails.
 */
@Component
@ConditionalOnProperty(name = "checkedin.test-email-to")
public class TestEmailRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(TestEmailRunner.class);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

    private final EmailService emailService;
    private final String recipient;

    public TestEmailRunner(EmailService emailService, @Value("${checkedin.test-email-to}") String recipient) {
        this.emailService = emailService;
        this.recipient = recipient;
    }

    @Override
    public void run(String... args) {
        String sentAt = LocalDateTime.now().format(TIME_FORMAT);
        try {
            String messageId = emailService.send(recipient, EmailTemplate.TEST_EMAIL, Map.of("sentAt", sentAt));
            log.info("TEST EMAIL SENT to {} at {}. Brevo message ID: {}", recipient, sentAt, messageId);
        } catch (EmailSendingException exception) {
            log.error("TEST EMAIL NOT SENT: {}", exception.getMessage());
        }
    }
}
