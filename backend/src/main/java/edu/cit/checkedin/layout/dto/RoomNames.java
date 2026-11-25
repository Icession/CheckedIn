package edu.cit.checkedin.layout.dto;

import java.util.Locale;

/** The room name rules shared by CreateLayoutRequest and SaveLayoutRequest (FR-017, FR-021). */
final class RoomNames {

    // Empty is let through here so that only the "Please enter..." message shows for a blank name
    static final String ALLOWED = "^$|^[A-Z0-9 -]+$";
    static final String ALLOWED_MESSAGE =
            "The room name can only have letters, numbers, spaces and hyphens, for example NGE207.";

    private RoomNames() {
    }

    /** "  nge   207 " becomes "NGE 207", so the same room is never saved twice in different spellings. */
    static String clean(String roomName) {
        return (roomName == null) ? null : roomName.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }
}
