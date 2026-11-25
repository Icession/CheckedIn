package edu.cit.checkedin.layout;

import org.hibernate.annotations.Check;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * One grid cell of a room layout: a seat, workstation, table, aisle or "no chair" spot (FR-018).
 * One row in the "seats" table (see the ERD); every cell of the grid has a row, even aisles.
 * Rows and columns start at 0, and row 0 is the front of the room (FR-023).
 * A cell keeps the same seat_id when the layout is edited, so seat assignments (Phase 5) stay linked.
 */
@Entity
@Table(name = "seats", uniqueConstraints = {
        // One cell per spot in the grid
        @UniqueConstraint(name = "seats_one_cell_per_spot", columnNames = {"layout_id", "row_index", "column_index"}),
        // FR-019: no two seats or workstations in one layout share a number (empty numbers don't count)
        @UniqueConstraint(name = "seats_unique_label_per_layout", columnNames = {"layout_id", "seat_label"})
})
// Only seats and workstations have a number; tables, aisles and "no chair" spots never do
@Check(name = "seats_label_only_on_usable_cells",
        constraints = "seat_label IS NULL OR cell_type IN ('SEAT', 'WORKSTATION')")
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seat_id")
    private Long seatId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "layout_id", nullable = false)
    private RoomLayout layout;

    // "short" so the database column is a SMALLINT, as in the ERD
    @Column(name = "row_index", nullable = false)
    private short rowIndex;

    @Column(name = "column_index", nullable = false)
    private short columnIndex;

    @Enumerated(EnumType.STRING)
    @Column(name = "cell_type", length = 11, nullable = false)
    private CellType cellType;

    // The seat or workstation number, e.g. "12" or "PC-12"; empty (null) for the other cell types
    @Column(name = "seat_label", length = 10)
    private String seatLabel;

    /** Empty constructor required by JPA. Our own code uses the constructor below. */
    protected Seat() {
    }

    public Seat(int rowIndex, int columnIndex, CellType cellType) {
        this.rowIndex = (short) rowIndex;
        this.columnIndex = (short) columnIndex;
        this.cellType = cellType;
    }

    /** True when a student can sit here (SEAT or WORKSTATION). */
    public boolean isUsable() {
        return cellType.isUsable();
    }

    /** True when this cell is at the given spot in the grid. */
    public boolean isAt(int row, int column) {
        return rowIndex == row && columnIndex == column;
    }

    /** Changes what the cell is. A cell that can no longer hold a student loses its number. */
    public void changeType(CellType newType) {
        this.cellType = newType;
        if (!newType.isUsable()) {
            this.seatLabel = null;
        }
    }

    // ---------- Getters and setters ----------

    public Long getSeatId() {
        return seatId;
    }

    public RoomLayout getLayout() {
        return layout;
    }

    /** Set by RoomLayout.addSeat, which keeps both sides of the link in step. */
    void setLayout(RoomLayout layout) {
        this.layout = layout;
    }

    public int getRowIndex() {
        return rowIndex;
    }

    public int getColumnIndex() {
        return columnIndex;
    }

    public CellType getCellType() {
        return cellType;
    }

    public String getSeatLabel() {
        return seatLabel;
    }

    /** Only seats and workstations keep a number (see the CHECK rule above). */
    public void setSeatLabel(String seatLabel) {
        this.seatLabel = isUsable() ? seatLabel : null;
    }
}
