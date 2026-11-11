package edu.cit.checkedin.integration;

/**
 * Thrown when an email could not be sent (email service not set up, rejected, or unreachable).
 * The caller decides what to tell the user; NFR-023 says a failed email must never stop
 * professors from checking and saving attendance.
 */
public class EmailSendingException extends RuntimeException {

    public EmailSendingException(String message) {
        super(message);
    }

    public EmailSendingException(String message, Throwable cause) {
        super(message, cause);
    }
}
