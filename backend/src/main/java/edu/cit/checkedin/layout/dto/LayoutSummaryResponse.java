package edu.cit.checkedin.layout.dto;

import edu.cit.checkedin.auth.User;
import edu.cit.checkedin.layout.RoomLayout;
import edu.cit.checkedin.layout.RoomType;

/**
 * One layout in the search results and in the session form's layout list (FR-020, FR-021),
 * e.g. "NGE207 - computer laboratory, 40 workstations, made by Maria Santos". The grid itself is left out.
 *
 * @param usableSeatCount FR-020: seats and workstations only
 * @param mine            true when the viewer made this layout (only they may change it)
 */
public record LayoutSummaryResponse(
        Long layoutId,
        String roomName,
        RoomType roomType,
        int rowCount,
        int columnCount,
        int usableSeatCount,
        String createdByName,
        boolean mine) {

    public static LayoutSummaryResponse from(RoomLayout layout, User viewer) {
        return new LayoutSummaryResponse(layout.getLayoutId(), layout.getRoomName(), layout.getRoomType(),
                layout.getRowCount(), layout.getColumnCount(), layout.getUsableSeats().size(),
                LayoutResponse.fullName(layout.getCreatedBy()), layout.isCreatedBy(viewer));
    }
}
