package edu.cit.checkedin.layout;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import edu.cit.checkedin.auth.AuthService;
import edu.cit.checkedin.auth.User;
import edu.cit.checkedin.layout.dto.CellRequest;
import edu.cit.checkedin.layout.dto.CreateLayoutRequest;
import edu.cit.checkedin.layout.dto.LayoutResponse;
import edu.cit.checkedin.layout.dto.LayoutSummaryResponse;
import edu.cit.checkedin.layout.dto.SaveLayoutRequest;

/**
 * Creating, searching, viewing and editing room layouts.
 * Covers FR-017 (create), FR-018 (cell types), FR-019 (numbers), FR-020 (usable seat count) and
 * FR-021 (unique room names; every professor can find and use a layout, only its creator can change it).
 */
@Service
public class LayoutService {

    private static final String UNIQUE_ROOM_NAME_RULE = "room_layouts_unique_room_name";

    private final RoomLayoutRepository layoutRepository;
    private final AuthService authService;
    private final LayoutGridChecker gridChecker;

    public LayoutService(RoomLayoutRepository layoutRepository, AuthService authService, LayoutGridChecker gridChecker) {
        this.layoutRepository = layoutRepository;
        this.authService = authService;
        this.gridChecker = gridChecker;
    }

    /** FR-021: every professor's layouts whose room name contains the text (all of them when it is empty). */
    @Transactional(readOnly = true)
    public List<LayoutSummaryResponse> searchLayouts(String professorEmail, String searchText) {
        User viewer = authService.requireAccount(professorEmail);
        String text = (searchText == null) ? "" : searchText.trim();
        return layoutRepository.findByRoomNameContainingIgnoreCaseOrderByRoomNameAsc(text).stream()
                .map(layout -> LayoutSummaryResponse.from(layout, viewer))
                .toList();
    }

    @Transactional(readOnly = true)
    public LayoutResponse getLayout(String professorEmail, Long layoutId) {
        User viewer = authService.requireAccount(professorEmail);
        return LayoutResponse.from(findLayoutWithSeats(layoutId), viewer);
    }

    /** FR-017: a new grid of seats (lecture room) or workstations (lab), numbered 1, 2, 3... */
    @Transactional
    public LayoutResponse createLayout(String professorEmail, CreateLayoutRequest request) {
        User professor = authService.requireAccount(professorEmail);
        checkRoomNameFree(request.roomName(), null);
        RoomLayout layout = new RoomLayout(professor, request.roomName(), request.roomType(),
                request.rowCount(), request.columnCount());
        saveCatchingDuplicateName(layout);
        return LayoutResponse.from(layout, professor);
    }

    /** FR-017 – FR-019: saves the edited room details and grid. FR-021: only the creator may do this. */
    @Transactional
    public LayoutResponse saveLayout(String professorEmail, Long layoutId, SaveLayoutRequest request) {
        User professor = authService.requireAccount(professorEmail);
        RoomLayout layout = findLayoutWithSeats(layoutId);
        requireCreator(layout, professor);
        gridChecker.checkGridSize(request);
        gridChecker.checkNoDuplicateNumbers(request.cells());
        checkRoomNameFree(request.roomName(), layout);

        // Two seats may swap numbers (12 <-> 13). Empty every number that changes and save that first,
        // otherwise the database's "one number per layout" rule would see 12 twice for a moment
        clearNumbersThatChange(layout, request);
        layoutRepository.flush();

        layout.setRoomName(request.roomName());
        layout.setRoomType(request.roomType());
        layout.changeSize(request.rowCount(), request.columnCount());
        applyCells(layout, request.cells());
        layout.numberNewSeats();
        layout.markUpdated();
        saveCatchingDuplicateName(layout);
        return LayoutResponse.from(layout, professor);
    }

    /** FR-015: the layout a session will use; any professor's layout may be used (FR-021). */
    public RoomLayout findLayout(Long layoutId) {
        return layoutRepository.findById(layoutId).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "This room layout does not exist. Please choose another one."));
    }

    private RoomLayout findLayoutWithSeats(Long layoutId) {
        return layoutRepository.findWithSeatsByLayoutId(layoutId).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "This room layout does not exist."));
    }

    // FR-021: everyone may look at and use a layout, but only the professor who made it may change it
    private void requireCreator(RoomLayout layout, User professor) {
        if (!layout.isCreatedBy(professor)) {
            User creator = layout.getCreatedBy();
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only " + creator.getFirstName() + " "
                    + creator.getLastName() + ", who made this layout, can change it. You can still use it for your sessions.");
        }
    }

    // FR-021: one layout per room, shared by all professors
    private void checkRoomNameFree(String roomName, RoomLayout layoutBeingEdited) {
        layoutRepository.findByRoomName(roomName)
                .filter(existing -> layoutBeingEdited == null || !existing.getLayoutId().equals(layoutBeingEdited.getLayoutId()))
                .ifPresent(existing -> {
                    User creator = existing.getCreatedBy();
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "There is already a layout for " + roomName
                            + ", made by " + creator.getFirstName() + " " + creator.getLastName()
                            + ". Search for " + roomName + " and use that layout instead.");
                });
    }

    // The check above can miss two professors saving the same room name at the same moment;
    // the database's unique rule catches that case, and the professor gets the same plain message
    private void saveCatchingDuplicateName(RoomLayout layout) {
        try {
            layoutRepository.saveAndFlush(layout);
        } catch (DataIntegrityViolationException exception) {
            String reason = String.valueOf(exception.getMostSpecificCause().getMessage());
            if (!reason.contains(UNIQUE_ROOM_NAME_RULE)) {
                throw exception;
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "There is already a layout for "
                    + layout.getRoomName() + ". Search for it and use that layout instead.");
        }
    }

    // Empties the number of every cell whose number changes, disappears, or is outside the new grid
    private void clearNumbersThatChange(RoomLayout layout, SaveLayoutRequest request) {
        for (Seat seat : layout.getSeats()) {
            String newNumber = requestedNumber(request, seat.getRowIndex(), seat.getColumnIndex());
            if (seat.getSeatLabel() != null && !seat.getSeatLabel().equals(newNumber)) {
                seat.setSeatLabel(null);
            }
        }
    }

    // The number the request gives this spot; null if it is outside the new grid, not a seat, or empty
    private String requestedNumber(SaveLayoutRequest request, int row, int column) {
        if (row >= request.rowCount() || column >= request.columnCount()) {
            return null;
        }
        CellRequest cell = request.cells().get(row).get(column);
        return cell.cellType().isUsable() ? cell.seatLabel() : null;
    }

    // Updates every cell in place, so it keeps its seat_id, and adds the cells of new rows or columns
    private void applyCells(RoomLayout layout, List<List<CellRequest>> cells) {
        for (int row = 0; row < cells.size(); row++) {
            for (int column = 0; column < cells.get(row).size(); column++) {
                CellRequest cell = cells.get(row).get(column);
                Seat seat = findOrAddSeat(layout, row, column, cell.cellType());
                seat.changeType(cell.cellType());
                seat.setSeatLabel(cell.seatLabel());
            }
        }
    }

    private Seat findOrAddSeat(RoomLayout layout, int row, int column, CellType cellType) {
        return layout.findSeat(row, column).orElseGet(() -> {
            Seat newSeat = new Seat(row, column, cellType);
            layout.addSeat(newSeat);
            return newSeat;
        });
    }
}
