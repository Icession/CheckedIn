package edu.cit.checkedin.layout.dto;

import java.util.ArrayList;
import java.util.List;

import edu.cit.checkedin.auth.User;
import edu.cit.checkedin.layout.RoomLayout;
import edu.cit.checkedin.layout.RoomType;
import edu.cit.checkedin.layout.Seat;

/**
 * One layout with its whole grid, for the layout builder page (FR-017 – FR-021, FR-023).
 * "cells" is a list of rows, the front row first, so cells[row][column] is one cell.
 *
 * @param usableSeatCount FR-020: seats and workstations only
 * @param createdByName   e.g. "Maria Santos"
 * @param canEdit         FR-021: true only for the professor who made the layout
 */
public record LayoutResponse(
        Long layoutId,
        String roomName,
        RoomType roomType,
        int rowCount,
        int columnCount,
        int usableSeatCount,
        String createdByName,
        boolean canEdit,
        List<List<CellResponse>> cells) {

    /** Builds the response for the professor who is looking at it (the viewer). */
    public static LayoutResponse from(RoomLayout layout, User viewer) {
        return new LayoutResponse(layout.getLayoutId(), layout.getRoomName(), layout.getRoomType(),
                layout.getRowCount(), layout.getColumnCount(), layout.getUsableSeats().size(),
                fullName(layout.getCreatedBy()), layout.isCreatedBy(viewer), toGrid(layout));
    }

    static String fullName(User user) {
        return user.getFirstName() + " " + user.getLastName();
    }

    // Puts every cell in its place: grid.get(row).get(column)
    private static List<List<CellResponse>> toGrid(RoomLayout layout) {
        List<List<CellResponse>> grid = new ArrayList<>();
        for (int row = 0; row < layout.getRowCount(); row++) {
            List<CellResponse> cellsInRow = new ArrayList<>();
            for (int column = 0; column < layout.getColumnCount(); column++) {
                cellsInRow.add(null);
            }
            grid.add(cellsInRow);
        }
        for (Seat seat : layout.getSeats()) {
            grid.get(seat.getRowIndex()).set(seat.getColumnIndex(), CellResponse.from(seat));
        }
        return grid;
    }
}
