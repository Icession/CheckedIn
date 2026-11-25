package edu.cit.checkedin.layout;

import static edu.cit.checkedin.layout.LayoutTestData.professor;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import edu.cit.checkedin.auth.User;

/** Tests the grid, numbering and counting rules of a layout (FR-017 – FR-020). */
class RoomLayoutTest {

    private final User maria = professor(1L, "Maria", "Santos");

    @Test
    void aNewLabIsAFullGridOfWorkstationsNumberedInReadingOrder() {
        RoomLayout lab = new RoomLayout(maria, "NGE207", RoomType.COMPUTER_LAB, 2, 3);

        assertThat(lab.getSeats()).hasSize(6).allMatch(seat -> seat.getCellType() == CellType.WORKSTATION);
        assertThat(labelsInReadingOrder(lab)).containsExactly("1", "2", "3", "4", "5", "6");
        assertThat(lab.findSeat(1, 0).orElseThrow().getSeatLabel()).isEqualTo("4");
    }

    @Test
    void aNewLectureRoomStartsWithSeats() {
        RoomLayout room = new RoomLayout(maria, "GLE201", RoomType.LECTURE_ROOM, 3, 4);

        assertThat(room.getSeats()).hasSize(12).allMatch(seat -> seat.getCellType() == CellType.SEAT);
    }

    @Test
    void onlySeatsAndWorkstationsAreUsable() {
        RoomLayout room = new RoomLayout(maria, "GLE201", RoomType.LECTURE_ROOM, 2, 3);
        room.findSeat(0, 1).orElseThrow().changeType(CellType.TABLE);
        room.findSeat(1, 1).orElseThrow().changeType(CellType.AISLE);
        room.findSeat(1, 2).orElseThrow().changeType(CellType.NO_CHAIR);
        room.findSeat(0, 2).orElseThrow().changeType(CellType.WORKSTATION);

        assertThat(room.getUsableSeats()).hasSize(3);
    }

    @Test
    void aCellThatCanNoLongerHoldAStudentLosesItsNumber() {
        RoomLayout lab = new RoomLayout(maria, "NGE207", RoomType.COMPUTER_LAB, 1, 2);
        Seat seat = lab.findSeat(0, 0).orElseThrow();

        seat.changeType(CellType.AISLE);
        seat.setSeatLabel("9");

        assertThat(seat.getSeatLabel()).isNull();
    }

    @Test
    void newSeatsContinueAfterTheHighestNumberAndTypedNumbersAreKept() {
        RoomLayout lab = new RoomLayout(maria, "NGE207", RoomType.COMPUTER_LAB, 1, 3);
        lab.findSeat(0, 0).orElseThrow().setSeatLabel("PC-12");
        lab.findSeat(0, 1).orElseThrow().setSeatLabel("40");
        lab.findSeat(0, 2).orElseThrow().setSeatLabel(null);

        lab.numberNewSeats();

        assertThat(labelsInReadingOrder(lab)).containsExactly("PC-12", "40", "41");
    }

    @Test
    void emptyingEveryNumberRenumbersFromOne() {
        RoomLayout lab = new RoomLayout(maria, "NGE207", RoomType.COMPUTER_LAB, 2, 2);
        lab.findSeat(0, 1).orElseThrow().changeType(CellType.AISLE);
        lab.getSeats().forEach(seat -> seat.setSeatLabel(null));

        lab.numberNewSeats();

        assertThat(labelsInReadingOrder(lab)).containsExactly("1", "2", "3");
    }

    @Test
    void shrinkingTheGridRemovesTheCellsOutsideIt() {
        RoomLayout room = new RoomLayout(maria, "GLE201", RoomType.LECTURE_ROOM, 3, 3);

        room.changeSize(2, 1);

        assertThat(room.getSeats()).hasSize(2)
                .allMatch(seat -> seat.getRowIndex() < 2 && seat.getColumnIndex() < 1);
        assertThat(room.getRowCount()).isEqualTo(2);
        assertThat(room.getColumnCount()).isEqualTo(1);
    }

    @Test
    void onlyTheCreatorCountsAsTheCreator() {
        RoomLayout room = new RoomLayout(maria, "GLE201", RoomType.LECTURE_ROOM, 1, 1);

        assertThat(room.isCreatedBy(professor(1L, "Maria", "Santos"))).isTrue();
        assertThat(room.isCreatedBy(professor(2L, "Jose", "Rizal"))).isFalse();
    }

    private static List<String> labelsInReadingOrder(RoomLayout layout) {
        return layout.getUsableSeats().stream().map(Seat::getSeatLabel).toList();
    }
}
