package edu.cit.checkedin.layout.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import edu.cit.checkedin.layout.RoomLayout;
import edu.cit.checkedin.layout.RoomType;

/**
 * JSON for saving an edited layout (FR-017 – FR-019): the room details plus the whole grid, row by row
 * from the front of the room, e.g. { "roomName": "NGE207", "roomType": "COMPUTER_LAB", "rowCount": 2,
 * "columnCount": 3, "cells": [[{...}, {...}, {...}], [{...}, {...}, {...}]] }.
 * LayoutGridChecker makes sure the grid really has rowCount rows of columnCount cells.
 */
public record SaveLayoutRequest(
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
        Integer columnCount,

        // "@Valid" inside the lists checks every single cell with the rules in CellRequest
        @NotNull(message = "The grid is missing. Please reload the page and try again.")
        List<List<@Valid CellRequest>> cells) {

    /** Runs before the checks above: trims spaces and writes the room name in capital letters. */
    public SaveLayoutRequest {
        roomName = RoomNames.clean(roomName);
    }
}
