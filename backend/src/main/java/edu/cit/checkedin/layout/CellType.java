package edu.cit.checkedin.layout;

/**
 * What one grid cell of a layout is (FR-018). Only SEAT and WORKSTATION can hold a student;
 * TABLE (a long shared table), AISLE and NO_CHAIR are shown on the map only.
 */
public enum CellType {
    SEAT,
    WORKSTATION,
    TABLE,
    AISLE,
    NO_CHAIR;

    /** FR-020: true for the cells a student can sit in, which are the only ones counted and numbered. */
    public boolean isUsable() {
        return this == SEAT || this == WORKSTATION;
    }
}
