// layoutDraft.js — the layout while it is being edited in the browser (FR-017 – FR-019), kept as one plain
// object so that one Undo can bring back a whole earlier state (also a resize: old size and old numbers):
//   { roomName, roomType, rowCount, columnCount, gridSize, cells, nextNumbers }
// - rowCount / columnCount: what is typed in the boxes (text). gridSize: the size in use; a typed size is
//   applied shortly after typing stops (see useLayoutDraft), so typing "12" never applies "1" first.
// - cells: the full grid. It can be bigger than gridSize after shrinking; only the shown part is saved.
// - nextNumbers: the "Next" counter of each type, for dragging and for numbering a selected block.
import { applyAutoNumbers, nextAfterPlan, planAutoNumbers } from './autoNumber.js'
import { cropGrid, growGrid, isValidSize, replaceTopLeft, startingCellType } from './layoutGrid.js'
import { findDuplicateLabels } from './seatLabels.js'
import { describeNumberingOrder, firstNumbers, numberingSettingsProblem } from './seatNumbers.js'

/** The draft of a layout as loaded or saved; Next starts at each type's Start at. */
export function startDraft(layout, settings) {
  return {
    roomName: layout.roomName,
    roomType: layout.roomType,
    rowCount: String(layout.rowCount),
    columnCount: String(layout.columnCount),
    gridSize: { rows: layout.rowCount, columns: layout.columnCount },
    cells: layout.cells,
    nextNumbers: firstNumbers(settings),
  }
}

/** The part of the grid that is shown and saved. */
export function shownCellsOf(draft) {
  return cropGrid(draft.cells, draft.gridSize.rows, draft.gridSize.columns)
}

/** The draft after the shown part of the grid changed (drawing, numbering). */
export function withShownCells(draft, shownCells) {
  return { ...draft, cells: replaceTopLeft(draft.cells, shownCells) }
}

/** FR-017: both typed sizes are whole numbers from 1 to 20. */
export function typedSizeIsValid(draft) {
  return isValidSize(draft.rowCount) && isValidSize(draft.columnCount)
}

/** True when the typed size is the size in use (nothing left to apply). */
export function typedSizeIsApplied(draft) {
  return Number(draft.rowCount) === draft.gridSize.rows && Number(draft.columnCount) === draft.gridSize.columns
}

/**
 * Applies the typed size. New cells continue the pattern (growGrid), then every seat and workstation is
 * renumbered from its Start at in the numbering order (Start from, Direction), so the numbers follow the grid:
 * 10 -> 12 columns gives row 1 = 1–12 and the next row starts at 13 (from the front left). Cells that come back after shrinking keep their type but get new numbers,
 * so no old label returns as a duplicate. Returns { draft, message }; the same draft when nothing changes.
 */
export function resizeDraft(draft, settings) {
  if (!typedSizeIsValid(draft) || typedSizeIsApplied(draft)) {
    return { draft, message: '' }
  }
  const gridSize = { rows: Number(draft.rowCount), columns: Number(draft.columnCount) }
  const grown = growGrid(draft.cells, gridSize.rows, gridSize.columns, startingCellType(draft.roomType))
  const shown = cropGrid(grown, gridSize.rows, gridSize.columns)
  const bothTypes = { ...settings, applyTo: 'BOTH' }
  if (numberingSettingsProblem(bothTypes)) {
    const message = 'The room size changed, but the numbers were not redone: fix the numbering settings first.'
    return { draft: { ...draft, gridSize, cells: grown }, message }
  }
  const plan = planAutoNumbers(shown, bothTypes)
  const renumbered = applyAutoNumbers(shown, plan)
  const resized = { ...draft, gridSize, cells: replaceTopLeft(grown, renumbered), nextNumbers: nextAfterPlan(draft.nextNumbers, plan) }
  return { draft: resized, message: resizeMessage(renumbered, settings) }
}

// "Numbers were redone from the front left, same direction every row, because the room size changed. ..."
function resizeMessage(renumbered, settings) {
  const message = `Numbers were redone ${describeNumberingOrder(settings)}, because the room size changed. Press Undo to go back.`
  const sharedNumbers = findDuplicateLabels(renumbered).size > 0
  return sharedNumbers ? `${message} Seats and workstations now share some numbers – give one of them a prefix.` : message
}

/** The JSON the backend expects when saving (SaveLayoutRequest): the room details and the shown grid. */
export function saveRequestOf(draft) {
  return {
    roomName: draft.roomName,
    roomType: draft.roomType,
    rowCount: draft.gridSize.rows,
    columnCount: draft.gridSize.columns,
    cells: shownCellsOf(draft).map((cellsInRow) => cellsInRow.map(({ cellType, seatLabel }) => ({ cellType, seatLabel }))),
  }
}
