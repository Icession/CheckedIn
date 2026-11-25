// layoutView.js — how a room layout is drawn on screen (FR-023): from the teacher's desk, facing the students.
// The stored rows and columns never change (row 0 is the front row, next to the board); only the drawing is
// turned around fully (180°): the front row is drawn at the bottom, next to the "Front of room" bar, and a
// student's left is on the screen's right. The layout builder uses these functions, and the Phase 5 seat
// picker can use them too.

/** Where a stored cell is drawn: its { row, column } on screen, counted from the top left of the grid. */
export function toScreenPosition({ row, column }, rowCount, columnCount) {
  return { row: rowCount - 1 - row, column: columnCount - 1 - column }
}

/** Which stored cell is drawn at a screen position (turning around 180° twice gives the same place back). */
export function fromScreenPosition(screenPosition, rowCount, columnCount) {
  return toScreenPosition(screenPosition, rowCount, columnCount)
}

/**
 * The position people read: rows counted from the front (row 1 = the front row, at the bottom) and columns
 * from the left of the screen, so "row 1, column 1" is the bottom-left cell.
 */
export function positionAsSeen(position, rowCount, columnCount) {
  return { rowNumber: position.row + 1, columnNumber: toScreenPosition(position, rowCount, columnCount).column + 1 }
}
