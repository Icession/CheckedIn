package edu.cit.checkedin.password;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * The password rules (Increment 2 / SRS v1.4), shared by every form that sets a password (the name form on first
 * sign-in now; the reset and change forms later): 8–64 characters, at least one letter and one number, not the
 * same as the email, and typed the same way twice. The frontend checks the same rules in utils/passwordRules.js.
 * Passwords themselves are never stored, only their BCrypt hash (users.password_hash).
 */
public final class PasswordRules {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 64;

    // Empty is let through here so that only the "Please create a password." message shows. Otherwise: at least
    // MIN_LENGTH characters, with at least one letter (\p{L}, any language) and at least one digit.
    public static final String PATTERN = "^$|^(?=.*\\p{L})(?=.*\\d).{" + MIN_LENGTH + ",}$";

    // The messages, in one place so every form says the same thing
    public static final String MISSING = "Please create a password.";
    public static final String TOO_LONG = "Your password can have at most " + MAX_LENGTH + " characters.";
    public static final String TOO_WEAK =
            "Your password needs at least " + MIN_LENGTH + " characters, with at least one letter and one number.";
    public static final String CONFIRM_MISSING = "Please type your password again under \"Confirm password\".";
    public static final String SAME_AS_EMAIL = "Your password can't be your email address. Please choose another one.";
    public static final String DO_NOT_MATCH = "The two passwords don't match. Please type the same password in both boxes.";

    private PasswordRules() {
    }

    /**
     * The checks the request annotations can't do, because they need the email or both boxes.
     * Throws 400 with a plain message when the password is the email or the confirmation is different.
     */
    public static void checkNotEmailAndConfirmed(String password, String confirmPassword, String email) {
        if (password.equalsIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, SAME_AS_EMAIL);
        }
        if (!password.equals(confirmPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, DO_NOT_MATCH);
        }
    }
}
