package edu.cit.checkedin.layout.dto;

import java.util.Locale;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import edu.cit.checkedin.layout.CellType;

/**
 * One grid cell inside SaveLayoutRequest (FR-018, FR-019), e.g. { "cellType": "WORKSTATION", "seatLabel": "12" }.
 * An empty number means "give this seat the next number automatically". Numbers on tables, aisles and
 * "no chair" cells are ignored. The number is cleaned up first, so " pc-12 " becomes "PC-12".
 *
 * @param cellType  SEAT, WORKSTATION, TABLE, AISLE or NO_CHAIR
 * @param seatLabel the seat or workstation number, e.g. 12 or PC-12 (optional, at most 10 characters)
 */
public record CellRequest(
        @NotNull(message = "Each cell of the grid needs a type. Please reload the page and try again.")
        CellType cellType,

        @Size(max = 10, message = "A seat number can have at most 10 characters.")
        @Pattern(regexp = "^[A-Z0-9-]+$",
                message = "A seat number can only have letters, numbers and hyphens, for example 12 or PC-12.")
        String seatLabel) {

    /** Runs before the checks above: an empty number becomes null, others are trimmed and capitalized. */
    public CellRequest {
        seatLabel = (seatLabel == null || seatLabel.isBlank()) ? null : seatLabel.trim().toUpperCase(Locale.ROOT);
    }
}
