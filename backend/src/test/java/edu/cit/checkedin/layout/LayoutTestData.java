package edu.cit.checkedin.layout;

import java.util.ArrayList;
import java.util.List;

import org.springframework.test.util.ReflectionTestUtils;

import edu.cit.checkedin.auth.Role;
import edu.cit.checkedin.auth.User;
import edu.cit.checkedin.layout.dto.CellRequest;
import edu.cit.checkedin.layout.dto.LayoutResponse;
import edu.cit.checkedin.layout.dto.SaveLayoutRequest;

/** Small helpers that build users, layouts and grids for the layout tests. */
final class LayoutTestData {

    private LayoutTestData() {
    }

    static User professor(Long userId, String firstName, String lastName) {
        User professor = new User(firstName.toLowerCase() + "@cit.edu", firstName, lastName, Role.PROFESSOR);
        // IDs are normally given by the database
        ReflectionTestUtils.setField(professor, "userId", userId);
        return professor;
    }

    /** A saved layout: gives the layout and each of its cells an ID, as the database would. */
    static RoomLayout savedLayout(Long layoutId, User creator, String roomName, RoomType roomType, int rows, int columns) {
        RoomLayout layout = new RoomLayout(creator, roomName, roomType, rows, columns);
        ReflectionTestUtils.setField(layout, "layoutId", layoutId);
        long seatId = layoutId * 1000;
        for (Seat seat : layout.getSeats()) {
            ReflectionTestUtils.setField(seat, "seatId", seatId++);
        }
        return layout;
    }

    /** The grid as the builder page would send it back unchanged. */
    static List<List<CellRequest>> unchangedGrid(RoomLayout layout) {
        List<List<CellRequest>> grid = new ArrayList<>();
        for (int row = 0; row < layout.getRowCount(); row++) {
            List<CellRequest> cellsInRow = new ArrayList<>();
            for (int column = 0; column < layout.getColumnCount(); column++) {
                Seat seat = layout.findSeat(row, column).orElseThrow();
                cellsInRow.add(new CellRequest(seat.getCellType(), seat.getSeatLabel()));
            }
            grid.add(cellsInRow);
        }
        return grid;
    }

    /** A rows x columns grid where every cell is the same type with no number. */
    static List<List<CellRequest>> gridOf(int rows, int columns, CellType cellType) {
        List<List<CellRequest>> grid = new ArrayList<>();
        for (int row = 0; row < rows; row++) {
            List<CellRequest> cellsInRow = new ArrayList<>();
            for (int column = 0; column < columns; column++) {
                cellsInRow.add(new CellRequest(cellType, null));
            }
            grid.add(cellsInRow);
        }
        return grid;
    }

    static SaveLayoutRequest saveRequest(RoomLayout layout, List<List<CellRequest>> grid) {
        return new SaveLayoutRequest(layout.getRoomName(), layout.getRoomType(), grid.size(), grid.get(0).size(), grid);
    }

    /** The numbers of a response's grid, row by row, e.g. [["1", "2"], [null, "3"]]. */
    static List<List<String>> numbersOf(LayoutResponse response) {
        return response.cells().stream()
                .map(row -> row.stream().map(cell -> cell.seatLabel()).toList())
                .toList();
    }
}
