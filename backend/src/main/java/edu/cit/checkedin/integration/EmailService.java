package edu.cit.checkedin.integration;

import java.util.Map;

/**
 * Sends emails from CheckedIn (sign-in codes now, warning emails in Phase 10).
 * Covers NFR-026: services talk to this interface, not to Brevo directly,
 * so the email provider can be replaced by changing one class (BrevoEmailService).
 */
public interface EmailService {

    /**
     * Sends one email built from a template.
     *
     * @param toAddress the recipient's email address
     * @param template  which email to send (subject and body)
     * @param data      values for the {placeholders} in the template, e.g. "code" -> "123456"
     * @return the message ID from the email provider, kept for the notification log (FR-067)
     * @throws EmailSendingException if the email could not be sent
     */
    String send(String toAddress, EmailTemplate template, Map<String, String> data);
}
