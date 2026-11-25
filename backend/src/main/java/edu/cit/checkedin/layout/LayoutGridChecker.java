package edu.cit.checkedin.layout;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import edu.cit.checkedin.layout.dto.CellRequest;
import edu.cit.checkedin.layout.dto.SaveLayoutRequest;

/**
 * Checks an edited grid before it is saved: it must match the number of rows and columns (FR-017),
 * and no two seats or workstations may share a number (FR-019).
 */
@Component
public class LayoutGridChecker {

    /** The grid must have exactly rowCount rows of columnCount cells, with no cell missing. */
    public void checkGridSize(SaveLayoutRequest request) {
        List<List<CellRequest>> cells = request.cells();
        boolean rightSize = cells.size() == request.rowCount()
                && cells.stream().allMatch(row -> row != null
                        && row.size() == request.columnCount()
                        && row.stream().allMatch(Objects::nonNull));
        if (!rightSize) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The grid does not match " + request.rowCount()
                    + " rows and " + request.columnCount() + " columns. Please reload the page and try again.");
        }
    }

    /** FR-019: no two seats or workstations in one layout may share a number. Empty numbers don't count. */
    public void checkNoDuplicateNumbers(List<List<CellRequest>> cells) {
        Set<String> numbersSeen = new HashSet<>();
        for (List<CellRequest> row : cells) {
            for (CellRequest cell : row) {
                boolean hasNumber = cell.cellType().isUsable() && cell.seatLabel() != null;
                // Set.add is false when the number is already in the set
                if (hasNumber && !numbersSeen.add(cell.seatLabel())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Number " + cell.seatLabel()
                            + " is used for two seats. Each seat and workstation needs its own number.");
                }
            }
        }
    }
}
