package edu.cit.checkedin.layout;

import static edu.cit.checkedin.layout.LayoutTestData.gridOf;
import static edu.cit.checkedin.layout.LayoutTestData.numbersOf;
import static edu.cit.checkedin.layout.LayoutTestData.professor;
import static edu.cit.checkedin.layout.LayoutTestData.saveRequest;
import static edu.cit.checkedin.layout.LayoutTestData.savedLayout;
import static edu.cit.checkedin.layout.LayoutTestData.unchangedGrid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import edu.cit.checkedin.auth.AuthService;
import edu.cit.checkedin.auth.User;
import edu.cit.checkedin.layout.dto.CellRequest;
import edu.cit.checkedin.layout.dto.CreateLayoutRequest;
import edu.cit.checkedin.layout.dto.LayoutResponse;
import edu.cit.checkedin.layout.dto.LayoutSummaryResponse;

/** Tests the layout rules (FR-017 – FR-021) with a fake database. */
@ExtendWith(MockitoExtension.class)
class LayoutServiceTest {

    private static final String MARIA_EMAIL = "maria@cit.edu";

    @Mock
    private RoomLayoutRepository layoutRepository;
    @Mock
    private AuthService authService;

    private LayoutService service;
    private User maria;
    private User jose;

    @BeforeEach
    void setUp() {
        service = new LayoutService(layoutRepository, authService, new LayoutGridChecker());
        maria = professor(1L, "Maria", "Santos");
        jose = professor(2L, "Jose", "Rizal");
        lenient().when(authService.requireAccount(MARIA_EMAIL)).thenReturn(maria);
        lenient().when(layoutRepository.saveAndFlush(any(RoomLayout.class))).thenAnswer(call -> call.getArgument(0));
    }

    // ---------- create (FR-017, FR-019, FR-020) ----------

    @Test
    void createsANumberedGridForTheProfessor() {
        when(layoutRepository.findByRoomName("NGE207")).thenReturn(Optional.empty());

        LayoutResponse result = service.createLayout(MARIA_EMAIL,
                new CreateLayoutRequest(" nge207 ", RoomType.COMPUTER_LAB, 2, 3));

        assertThat(result.roomName()).isEqualTo("NGE207");
        assertThat(result.usableSeatCount()).isEqualTo(6);
        assertThat(result.canEdit()).isTrue();
        assertThat(numbersOf(result)).containsExactly(List.of("1", "2", "3"), List.of("4", "5", "6"));
    }

    @Test
    void refusesARoomThatAlreadyHasALayout() {
        when(layoutRepository.findByRoomName("NGE207"))
                .thenReturn(Optional.of(savedLayout(9L, jose, "NGE207", RoomType.COMPUTER_LAB, 1, 1)));

        assertRejected(() -> service.createLayout(MARIA_EMAIL, new CreateLayoutRequest("NGE207", RoomType.COMPUTER_LAB, 2, 2)),
                HttpStatus.CONFLICT, "There is already a layout for NGE207, made by Jose Rizal.");
        verify(layoutRepository, never()).saveAndFlush(any());
    }

    @Test
    void twoProfessorsSavingTheSameRoomAtOnceGetThePlainMessage() {
        when(layoutRepository.findByRoomName("NGE207")).thenReturn(Optional.empty());
        when(layoutRepository.saveAndFlush(any(RoomLayout.class))).thenThrow(new DataIntegrityViolationException("insert",
                new SQLException("duplicate key value violates unique constraint \"room_layouts_unique_room_name\"")));

        assertRejected(() -> service.createLayout(MARIA_EMAIL, new CreateLayoutRequest("NGE207", RoomType.COMPUTER_LAB, 2, 2)),
                HttpStatus.CONFLICT, "There is already a layout for NGE207.");
    }

    // ---------- view and search (FR-021) ----------

    @Test
    void everyProfessorCanOpenALayoutButOnlyTheCreatorMayEditIt() {
        when(layoutRepository.findWithSeatsByLayoutId(9L))
                .thenReturn(Optional.of(savedLayout(9L, jose, "GLE201", RoomType.LECTURE_ROOM, 2, 2)));

        LayoutResponse result = service.getLayout(MARIA_EMAIL, 9L);

        assertThat(result.canEdit()).isFalse();
        assertThat(result.createdByName()).isEqualTo("Jose Rizal");
    }

    @Test
    void searchShowsWhichLayoutsAreMine() {
        when(layoutRepository.findByRoomNameContainingIgnoreCaseOrderByRoomNameAsc("nge")).thenReturn(List.of(
                savedLayout(9L, jose, "NGE205", RoomType.COMPUTER_LAB, 1, 2),
                savedLayout(10L, maria, "NGE207", RoomType.COMPUTER_LAB, 2, 2)));

        List<LayoutSummaryResponse> results = service.searchLayouts(MARIA_EMAIL, " nge ");

        assertThat(results).extracting(LayoutSummaryResponse::roomName).containsExactly("NGE205", "NGE207");
        assertThat(results).extracting(LayoutSummaryResponse::mine).containsExactly(false, true);
        assertThat(results).extracting(LayoutSummaryResponse::usableSeatCount).containsExactly(2, 4);
    }

    @Test
    void aMissingLayoutGetsA404() {
        when(layoutRepository.findWithSeatsByLayoutId(99L)).thenReturn(Optional.empty());

        assertRejected(() -> service.getLayout(MARIA_EMAIL, 99L), HttpStatus.NOT_FOUND, "This room layout does not exist.");
    }

    // ---------- save (FR-018, FR-019, FR-021) ----------

    @Test
    void onlyTheCreatorCanSaveChanges() {
        RoomLayout josesRoom = savedLayout(9L, jose, "GLE201", RoomType.LECTURE_ROOM, 2, 2);
        when(layoutRepository.findWithSeatsByLayoutId(9L)).thenReturn(Optional.of(josesRoom));

        assertRejected(() -> service.saveLayout(MARIA_EMAIL, 9L, saveRequest(josesRoom, unchangedGrid(josesRoom))),
                HttpStatus.FORBIDDEN, "Only Jose Rizal, who made this layout, can change it.");
        verify(layoutRepository, never()).flush();
    }

    @Test
    void changingACellKeepsItsSeatIdAndRemovesItsNumber() {
        RoomLayout lab = mariasLab(2, 2);
        Long seatIdOfCell01 = lab.findSeat(0, 1).orElseThrow().getSeatId();
        List<List<CellRequest>> grid = unchangedGrid(lab);
        grid.get(0).set(1, new CellRequest(CellType.AISLE, null));

        LayoutResponse result = service.saveLayout(MARIA_EMAIL, 10L, saveRequest(lab, grid));

        assertThat(result.cells().get(0).get(1).seatId()).isEqualTo(seatIdOfCell01);
        assertThat(result.cells().get(0).get(1).cellType()).isEqualTo(CellType.AISLE);
        assertThat(numbersOf(result)).containsExactly(Arrays.asList("1", null), List.of("3", "4"));
        assertThat(result.usableSeatCount()).isEqualTo(3);
    }

    @Test
    void twoSeatsCanSwapNumbers() {
        RoomLayout lab = mariasLab(1, 2);
        List<String> numbersWhenFirstSaved = new ArrayList<>();
        // The database would refuse "2" twice, so both numbers must be empty when the first save happens
        doAnswer(call -> {
            lab.getSeats().forEach(seat -> numbersWhenFirstSaved.add(seat.getSeatLabel()));
            return null;
        }).when(layoutRepository).flush();
        List<List<CellRequest>> grid = List.of(List.of(
                new CellRequest(CellType.WORKSTATION, "2"), new CellRequest(CellType.WORKSTATION, "1")));

        LayoutResponse result = service.saveLayout(MARIA_EMAIL, 10L, saveRequest(lab, grid));

        assertThat(numbersWhenFirstSaved).containsOnlyNulls();
        assertThat(numbersOf(result)).containsExactly(List.of("2", "1"));
    }

    @Test
    void newRowsGetTheNextNumbers() {
        RoomLayout lab = mariasLab(1, 2);
        List<List<CellRequest>> grid = new ArrayList<>(unchangedGrid(lab));
        grid.addAll(gridOf(1, 2, CellType.WORKSTATION));

        LayoutResponse result = service.saveLayout(MARIA_EMAIL, 10L, saveRequest(lab, grid));

        assertThat(numbersOf(result)).containsExactly(List.of("1", "2"), List.of("3", "4"));
        assertThat(lab.getSeats()).hasSize(4);
    }

    @Test
    void removedColumnsAreDeleted() {
        RoomLayout lab = mariasLab(2, 3);
        List<List<CellRequest>> grid = unchangedGrid(lab).stream().map(row -> row.subList(0, 1)).toList();

        LayoutResponse result = service.saveLayout(MARIA_EMAIL, 10L, saveRequest(lab, grid));

        assertThat(numbersOf(result)).containsExactly(List.of("1"), List.of("4"));
        assertThat(lab.getSeats()).hasSize(2);
    }

    @Test
    void duplicateNumbersAreRefusedBeforeAnythingIsSaved() {
        RoomLayout lab = mariasLab(1, 2);
        List<List<CellRequest>> grid = List.of(List.of(
                new CellRequest(CellType.WORKSTATION, "7"), new CellRequest(CellType.WORKSTATION, "7")));

        assertRejected(() -> service.saveLayout(MARIA_EMAIL, 10L, saveRequest(lab, grid)),
                HttpStatus.BAD_REQUEST, "Number 7 is used for two seats.");
        verify(layoutRepository, never()).flush();
    }

    // A lab of Maria's (layout 10) that the fake database will return
    private RoomLayout mariasLab(int rows, int columns) {
        RoomLayout lab = savedLayout(10L, maria, "NGE207", RoomType.COMPUTER_LAB, rows, columns);
        when(layoutRepository.findWithSeatsByLayoutId(10L)).thenReturn(Optional.of(lab));
        lenient().when(layoutRepository.findByRoomName("NGE207")).thenReturn(Optional.of(lab));
        return lab;
    }

    private static void assertRejected(ThrowingCallable call, HttpStatus status, String messagePart) {
        assertThatThrownBy(call).isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
            assertThat(exception.getStatusCode()).isEqualTo(status);
            assertThat(exception.getReason()).contains(messagePart);
        });
    }
}
