package edu.cit.checkedin.layout.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import edu.cit.checkedin.layout.RoomLayout;
import edu.cit.checkedin.layout.RoomType;

/**
 * JSON for creating a layout (FR-017), e.g.
 * { "roomName": "NGE207", "roomType": "COMPUTER_LAB", "rowCount": 6, "columnCount": 8 }.
 * The room name is cleaned up first (spaces trimmed, capital letters), so "nge207 " becomes "NGE207".
 *
 * @param roomName    e.g. NGE207 (at most 30 characters, unique)
 * @param roomType    LECTURE_ROOM or COMPUTER_LAB
 * @param rowCount    1 to 20
 * @param columnCount 1 to 20
 */
public record CreateLayoutRequest(
        @NotBlank(message = "Please enter the room name, for example NGE207.")
        @Size(max = 30, message = "The room name can have at most 30 characters.")
        @Pattern(regexp = RoomNames.ALLOWED, message = RoomNames.ALLOWED_MESSAGE)
        String roomName,

        @NotNull(message = "Please choose the room type.")
        RoomType roomType,

        @NotNull(message = "Please enter the number of rows (1 to 20).")
        @Min(value = 1, message = "The number of rows must be from 1 to 20.")
        @Max(value = RoomLayout.MAX_ROWS_OR_COLUMNS, message = "The number of rows must be from 1 to 20.")
        Integer rowCount,

        @NotNull(message = "Please enter the number of columns (1 to 20).")
        @Min(value = 1, message = "The number of columns must be from 1 to 20.")
        @Max(value = RoomLayout.MAX_ROWS_OR_COLUMNS, message = "The number of columns must be from 1 to 20.")
        Integer columnCount) {

    /** Runs before the checks above: trims spaces and writes the room name in capital letters. */
    public CreateLayoutRequest {
        roomName = RoomNames.clean(roomName);
    }
}
