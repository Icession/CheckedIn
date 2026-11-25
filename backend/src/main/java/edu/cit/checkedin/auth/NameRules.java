package edu.cit.checkedin.auth;

/**
 * The rules for first and last names, shared by the name form on first sign-in (FR-003) and the profile page
 * (Increment 2 / SRS v1.4: edit profile), so a name that was accepted once is always accepted again.
 * A name is required, has at most 50 characters, and may use letters from any language (so ñ and accents
 * work), spaces, hyphens, apostrophes and periods, e.g. "Ma. Cristina", "Dela Cruz", "O'Neil".
 * The frontend checks the same rules in frontend/src/utils/nameRules.js.
 */
public final class NameRules {

    public static final int MAX_LENGTH = 50;

    // Empty is let through here so that only the "Please enter..." message shows for a blank name.
    // Otherwise: at least one letter (\p{L} = a letter in any language, \p{M} = an accent mark),
    // plus spaces, periods, apostrophes (straight ' and the curly ’ that phone keyboards type) and hyphens.
    public static final String PATTERN = "^$|^(?=.*\\p{L})[\\p{L}\\p{M} .'’-]+$";

    // The messages, in one place so both forms say the same thing
    public static final String FIRST_NAME_MISSING = "Please enter your first name.";
    public static final String LAST_NAME_MISSING = "Please enter your last name.";
    public static final String FIRST_NAME_TOO_LONG = "Your first name can have at most " + MAX_LENGTH + " characters.";
    public static final String LAST_NAME_TOO_LONG = "Your last name can have at most " + MAX_LENGTH + " characters.";
    public static final String FIRST_NAME_CHARACTERS =
            "Please use only letters, spaces, hyphens (-), apostrophes (') and periods (.) in your first name.";
    public static final String LAST_NAME_CHARACTERS =
            "Please use only letters, spaces, hyphens (-), apostrophes (') and periods (.) in your last name.";

    private NameRules() {
    }

    /** Removes spaces at both ends and turns runs of spaces into one: "  Juan   Carlos " -> "Juan Carlos". */
    public static String cleanUp(String name) {
        return (name == null) ? null : name.trim().replaceAll("\\s+", " ");
    }
}
