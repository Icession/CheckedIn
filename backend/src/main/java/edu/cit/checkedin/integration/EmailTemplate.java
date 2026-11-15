package edu.cit.checkedin.integration;

import java.util.Map;

/**
 * Every email CheckedIn can send: its subject and its plain-text body.
 * Words in braces, like {sentAt}, are placeholders filled in when the email is sent.
 * The texts live here in the code (not in the Brevo dashboard) so they are easy to find and review.
 * Warning emails are added in Phase 10.
 */
public enum EmailTemplate {

    // FR-001: the one-time code for signing in (step 1e)
    LOGIN_CODE(
            "Your CheckedIn sign-in code",
            """
            Your CheckedIn sign-in code is:

                {code}

            It works once and expires in {minutes} minutes.

            If you did not try to sign in to CheckedIn, you can ignore this email.
            """),

    // Setup check (step 1d): proves that emails from CheckedIn reach a @cit.edu inbox
    TEST_EMAIL(
            "CheckedIn test email",
            """
            Hello!

            This is a test email from CheckedIn, sent at {sentAt}.

            If you are reading this in your Inbox (not in Junk), emails from CheckedIn are reaching you.
            """);

    private final String subject;
    private final String body;

    EmailTemplate(String subject, String body) {
        this.subject = subject;
        this.body = body;
    }

    public String getSubject() {
        return subject;
    }

    /** Returns the body with each {name} placeholder replaced by its value from data. */
    public String fillBody(Map<String, String> data) {
        String filledBody = body;
        for (Map.Entry<String, String> entry : data.entrySet()) {
            filledBody = filledBody.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return filledBody;
    }
}
