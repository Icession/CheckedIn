package edu.cit.checkedin.layout;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.hibernate.annotations.Check;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import edu.cit.checkedin.auth.User;

/**
 * A room drawn as a grid, e.g. "NGE207, computer laboratory, 6 rows x 8 columns", with one Seat per cell.
 * One row in the "room_layouts" table (see the ERD). Covers FR-017 (room name, type, 1-20 rows and
 * columns), FR-019 (automatic numbers in reading order), FR-020 (usable seat count) and
 * FR-021 (one layout per room name, shared by all professors, changed only by its creator).
 */
@Entity
@Table(name = "room_layouts", uniqueConstraints =
        @UniqueConstraint(name = "room_layouts_unique_room_name", columnNames = "room_name"))
@Check(name = "room_layouts_size_1_to_20",
        constraints = "row_count BETWEEN 1 AND 20 AND column_count BETWEEN 1 AND 20")
public class RoomLayout {

    /** FR-017: a layout has 1 to 20 rows and 1 to 20 columns. */
    public static final int MAX_ROWS_OR_COLUMNS = 20;

    // Reading order: the front row first (row 0), left to right inside each row
    private static final Comparator<Seat> READING_ORDER =
            Comparator.comparingInt(Seat::getRowIndex).thenComparingInt(Seat::getColumnIndex);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "layout_id")
    private Long layoutId;

    // The professor who drew it; only they may change it (FR-021)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    // Saved in capitals, e.g. "NGE207"
    @Column(name = "room_name", length = 30, nullable = false)
    private String roomName;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", length = 12, nullable = false)
    private RoomType roomType;

    // "short" so the database columns are SMALLINT, as in the ERD
    @Column(name = "row_count", nullable = false)
    private short rowCount;

    @Column(name = "column_count", nullable = false)
    private short columnCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Every cell of the grid; removing one from this list deletes its row (orphanRemoval)
    @OneToMany(mappedBy = "layout", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Seat> seats = new ArrayList<>();

    /** Empty constructor required by JPA. Our own code uses the constructor below. */
    protected RoomLayout() {
    }

    /** A new layout starts as a full grid of seats (lecture room) or workstations (lab), numbered 1, 2, 3... */
    public RoomLayout(User createdBy, String roomName, RoomType roomType, int rowCount, int columnCount) {
        this.createdBy = createdBy;
        this.roomName = roomName;
        this.roomType = roomType;
        this.rowCount = (short) rowCount;
        this.columnCount = (short) columnCount;
        CellType startingType = (roomType == RoomType.COMPUTER_LAB) ? CellType.WORKSTATION : CellType.SEAT;
        for (int row = 0; row < rowCount; row++) {
            for (int column = 0; column < columnCount; column++) {
                addSeat(new Seat(row, column, startingType));
            }
        }
        numberNewSeats();
    }

    /** Sets the creation time automatically right before the layout is first saved. */
    @PrePersist
    private void recordCreationTime() {
        this.createdAt = LocalDateTime.now();
    }

    /** Called after every edit, including edits that only change cells. */
    public void markUpdated() {
        this.updatedAt = LocalDateTime.now();
    }

    /** FR-021: only the professor who drew the layout may change it. */
    public boolean isCreatedBy(User user) {
        return createdBy.getUserId().equals(user.getUserId());
    }

    /** FR-020: the cells a student can sit in (seats and workstations), in reading order. */
    public List<Seat> getUsableSeats() {
        return seats.stream().filter(Seat::isUsable).sorted(READING_ORDER).toList();
    }

    public Optional<Seat> findSeat(int row, int column) {
        return seats.stream().filter(seat -> seat.isAt(row, column)).findFirst();
    }

    /** Adds a cell and links it back to this layout. */
    public void addSeat(Seat seat) {
        seat.setLayout(this);
        seats.add(seat);
    }

    /** Changes the grid size and deletes the cells that are now outside it. New cells are added by the caller. */
    public void changeSize(int newRowCount, int newColumnCount) {
        this.rowCount = (short) newRowCount;
        this.columnCount = (short) newColumnCount;
        seats.removeIf(seat -> seat.getRowIndex() >= newRowCount || seat.getColumnIndex() >= newColumnCount);
    }

    /**
     * FR-019: gives every seat and workstation that has no number yet the next number, in reading order.
     * Numbers continue after the highest plain number already used, so numbers the professor typed are
     * kept, e.g. with 1-40 taken, two new workstations become 41 and 42. In a new layout this gives 1, 2, 3...
     */
    public void numberNewSeats() {
        long nextNumber = highestPlainNumber() + 1;
        for (Seat seat : getUsableSeats()) {
            if (seat.getSeatLabel() == null) {
                seat.setSeatLabel(String.valueOf(nextNumber));
                nextNumber++;
            }
        }
    }

    // The highest number among labels made only of digits ("12" counts, "PC-12" doesn't); 0 if none
    private long highestPlainNumber() {
        return seats.stream()
                .map(Seat::getSeatLabel)
                .filter(label -> label != null && label.chars().allMatch(Character::isDigit))
                .mapToLong(Long::parseLong)
                .max()
                .orElse(0);
    }

    // ---------- Getters and setters (no setters for the ID, creator and times) ----------

    public Long getLayoutId() {
        return layoutId;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public void setRoomType(RoomType roomType) {
        this.roomType = roomType;
    }

    public int getRowCount() {
        return rowCount;
    }

    public int getColumnCount() {
        return columnCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<Seat> getSeats() {
        return seats;
    }
}
