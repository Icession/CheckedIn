package edu.cit.checkedin.layout.dto;

import edu.cit.checkedin.layout.CellType;
import edu.cit.checkedin.layout.Seat;

/**
 * One grid cell as sent to the frontend, e.g. { "seatId": 81, "cellType": "WORKSTATION", "seatLabel": "12" }.
 *
 * @param seatId    stays the same when the layout is edited (seat assignments in Phase 5 point to it)
 * @param cellType  SEAT, WORKSTATION, TABLE, AISLE or NO_CHAIR
 * @param seatLabel the seat or workstation number; null for the other cell types
 */
public record CellResponse(Long seatId, CellType cellType, String seatLabel) {

    public static CellResponse from(Seat seat) {
        return new CellResponse(seat.getSeatId(), seat.getCellType(), seat.getSeatLabel());
    }
}
