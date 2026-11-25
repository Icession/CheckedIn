// seatLabels.js — checking seat and workstation labels in the layout builder (FR-019): the label rule,
// labels used twice, unnumbered seats, and guessing the prefix existing labels use.
// The numbering settings and the numbering order are in seatNumbers.js.
import { cellTypeInfo, isUsable } from './layoutGrid.js'
import { positionAsSeen } from './layoutView.js'

// The same rule as the backend (CellRequest): letters, digits and hyphens, at most 10 characters
const SEAT_LABEL_PATTERN = /^[A-Z0-9-]{1,10}$/

/** " pc-01 " -> "PC-01" (as the backend saves it). An empty text -> null: "give it the next free number". */
export function normalizeSeatLabel(text) {
  const cleaned = text.trim().toUpperCase()
  return cleaned === '' ? null : cleaned
}

/** True for a number the backend accepts, e.g. "12" or "PC-12"; false for "PC_12" or a too long one. */
export function isValidSeatLabel(label) {
  return SEAT_LABEL_PATTERN.test(label)
}

/** The numbers used by more than one seat or workstation, e.g. Set { "PC-1" }. Empty numbers don't count. */
export function findDuplicateLabels(cells) {
  const seen = new Set()
  const duplicates = new Set()
  for (const cell of cells.flat()) {
    if (!isUsable(cell.cellType) || cell.seatLabel === null) continue
    if (seen.has(cell.seatLabel)) {
      duplicates.add(cell.seatLabel)
    }
    seen.add(cell.seatLabel)
  }
  return duplicates
}

/** Where another seat already has this number, as { row, column }, or null. The cell at (row, column) is skipped. */
export function findSeatWithLabel(cells, label, row, column) {
  for (const [otherRow, cellsInRow] of cells.entries()) {
    for (const [otherColumn, cell] of cellsInRow.entries()) {
      const isSameCell = otherRow === row && otherColumn === column
      if (!isSameCell && isUsable(cell.cellType) && cell.seatLabel === label) {
        return { row: otherRow, column: otherColumn }
      }
    }
  }
  return null
}

/** How many seats and workstations have no number yet (they are marked in the grid). */
export function countUnnumbered(cells) {
  return cells.flat().filter((cell) => isUsable(cell.cellType) && cell.seatLabel === null).length
}

/** "the workstation in row 2, column 3": rows counted from the front, columns from the screen's left (see layoutView.js). */
export function describeCell(cells, position) {
  const typeName = cellTypeInfo(cells[position.row][position.column].cellType).label.toLowerCase()
  const { rowNumber, columnNumber } = positionAsSeen(position, cells.length, cells[0].length)
  return `the ${typeName} in row ${rowNumber}, column ${columnNumber}`
}

// "WS-12" -> { prefix: "WS-", number: 12 }; null for a label that doesn't end in digits
function splitLabel(label) {
  const match = /^(.*?)([0-9]+)$/.exec(label)
  return match ? { prefix: match[1], number: Number(match[2]) } : null
}

/** The prefix most labels of this type use, e.g. "WS-" when workstations are WS-1 … WS-40; '' when none. */
export function guessPrefix(cells, cellType) {
  const timesUsed = new Map()
  for (const cell of cells.flat()) {
    const parts = cell.cellType === cellType && cell.seatLabel ? splitLabel(cell.seatLabel) : null
    if (parts) timesUsed.set(parts.prefix, (timesUsed.get(parts.prefix) ?? 0) + 1)
  }
  let mostUsed = ''
  for (const [prefix, count] of timesUsed) {
    if (count > (timesUsed.get(mostUsed) ?? 0)) mostUsed = prefix
  }
  return mostUsed
}
