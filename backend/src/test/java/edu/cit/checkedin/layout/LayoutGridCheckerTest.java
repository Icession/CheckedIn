package edu.cit.checkedin.layout;

import static edu.cit.checkedin.layout.LayoutTestData.gridOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import edu.cit.checkedin.layout.dto.CellRequest;
import edu.cit.checkedin.layout.dto.SaveLayoutRequest;

/** Tests the grid size check (FR-017) and the "no duplicate numbers" rule (FR-019). */
class LayoutGridCheckerTest {

    private final LayoutGridChecker checker = new LayoutGridChecker();

    @Test
    void aGridOfTheRightSizePasses() {
        assertThatCode(() -> checker.checkGridSize(request(2, 3, gridOf(2, 3, CellType.SEAT)))).doesNotThrowAnyException();
    }

    @Test
    void aGridWithTooFewRowsIsRefused() {
        assertBadRequest(() -> checker.checkGridSize(request(3, 3, gridOf(2, 3, CellType.SEAT))),
                "The grid does not match 3 rows and 3 columns.");
    }

    @Test
    void aRowWithTooManyCellsIsRefused() {
        List<List<CellRequest>> grid = gridOf(2, 3, CellType.SEAT);
        grid.get(1).add(new CellRequest(CellType.SEAT, null));

        assertBadRequest(() -> checker.checkGridSize(request(2, 3, grid)), "The grid does not match");
    }

    @Test
    void aMissingCellIsRefused() {
        List<List<CellRequest>> grid = gridOf(2, 2, CellType.SEAT);
        grid.get(0).set(1, null);

        assertBadRequest(() -> checker.checkGridSize(request(2, 2, grid)), "The grid does not match");
    }

    @Test
    void theSameNumberOnTwoSeatsIsRefused() {
        List<List<CellRequest>> grid = gridOf(1, 3, CellType.WORKSTATION);
        grid.get(0).set(0, new CellRequest(CellType.WORKSTATION, "12"));
        grid.get(0).set(2, new CellRequest(CellType.SEAT, "12"));

        assertBadRequest(() -> checker.checkNoDuplicateNumbers(grid),
                "Number 12 is used for two seats. Each seat and workstation needs its own number.");
    }

    @Test
    void emptyNumbersAndNumbersOnAislesAreNotDuplicates() {
        List<List<CellRequest>> grid = gridOf(1, 3, CellType.SEAT);
        grid.get(0).set(0, new CellRequest(CellType.SEAT, "5"));
        // An aisle's number is ignored (it is never saved), so it can't clash with seat 5
        grid.get(0).set(1, new CellRequest(CellType.AISLE, "5"));

        assertThatCode(() -> checker.checkNoDuplicateNumbers(grid)).doesNotThrowAnyException();
    }

    private static SaveLayoutRequest request(int rows, int columns, List<List<CellRequest>> grid) {
        return new SaveLayoutRequest("GLE201", RoomType.LECTURE_ROOM, rows, columns, grid);
    }

    private static void assertBadRequest(ThrowingCallable call, String messageStart) {
        assertThatThrownBy(call).isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
            assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(exception.getReason()).startsWith(messageStart);
        });
    }
}
