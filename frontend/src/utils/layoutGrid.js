// layoutGrid.js — names of the room and cell types, the builder's tools and keyboard shortcuts, and small
// helpers that change a layout grid in the browser while the professor draws it (FR-017 – FR-020).
// Seat numbers are in seatNumbers.js, Auto-number in autoNumber.js, numbering by dragging in numberingDrag.js,
// selecting in cellSelection.js, and the layout being edited (with resizing) in layoutDraft.js.
// A grid is a list of rows, the front row first: cells[row][column] = { seatId, cellType, seatLabel }.
// The backend checks everything again on save.

// ---------- Names, tools and keyboard shortcuts ----------

export const ROOM_TYPE_LABELS = { LECTURE_ROOM: 'Lecture room', COMPUTER_LAB: 'Computer laboratory' }

export const MAX_ROWS_OR_COLUMNS = 20

// The cell types in the order of the toolbar, with the letter shown on a cell without a number
export const CELL_TYPES = [
  { value: 'SEAT', label: 'Seat', letter: 'S' },
  { value: 'WORKSTATION', label: 'Workstation', letter: 'W' },
  { value: 'TABLE', label: 'Table', letter: 'T' },
  { value: 'AISLE', label: 'Aisle', letter: '' },
  { value: 'NO_CHAIR', label: 'No chair', letter: '×' },
]

// Two extra tools that don't paint: numbering seats, and selecting cells for Auto-number (FR-019)
export const NUMBER_TOOL = 'NUMBER'
export const SELECT_TOOL = 'SELECT'

// Every keyboard shortcut of the layout builder, all on the left hand: key -> tool
export const TOOL_SHORTCUTS = {
  Q: 'SEAT',
  W: 'WORKSTATION',
  E: 'TABLE',
  R: 'AISLE',
  T: 'NO_CHAIR',
  A: NUMBER_TOOL,
  S: SELECT_TOOL,
}
export const UNDO_SHORTCUT = 'Z'
export const CLEAR_SELECTION_KEY = 'Escape'

/** The toolbar entry of a cell type, e.g. 'TABLE' -> { value: 'TABLE', label: 'Table', letter: 'T' }. */
export function cellTypeInfo(cellType) {
  return CELL_TYPES.find((type) => type.value === cellType)
}

/** The name of any tool, e.g. 'NO_CHAIR' -> 'No chair', NUMBER_TOOL -> 'Number'. */
export function toolLabel(tool) {
  const otherTools = { [NUMBER_TOOL]: 'Number', [SELECT_TOOL]: 'Select' }
  return otherTools[tool] ?? cellTypeInfo(tool).label
}

/** The key of a tool, e.g. 'SEAT' -> 'Q'. */
export function shortcutForTool(tool) {
  return Object.keys(TOOL_SHORTCUTS).find((key) => TOOL_SHORTCUTS[key] === tool)
}

// Inputs where keys are typed as text; other inputs (radio buttons, checkboxes) don't take letters
const TEXT_INPUT_TYPES = ['text', 'number', 'search', 'email', 'url', 'tel', 'password']

/** True when keys pressed in this element are typed into it, e.g. the room name or a prefix box. */
export function isTypingIn(element) {
  const tag = element?.tagName
  return tag === 'TEXTAREA' || element?.isContentEditable === true || (tag === 'INPUT' && TEXT_INPUT_TYPES.includes(element.type))
}

/**
 * What a key press means in the layout builder: { action: 'TOOL', tool }, { action: 'UNDO' },
 * { action: 'ESCAPE' } or null. Keys typed into a text box, or held with Ctrl, Alt or Cmd, mean nothing.
 * Letter keys also mean nothing in a drop-down list, where they pick an option.
 */
export function shortcutAction(event) {
  if (event.ctrlKey || event.altKey || event.metaKey || isTypingIn(event.target)) return null
  if (event.key === CLEAR_SELECTION_KEY) return { action: 'ESCAPE' }
  if (event.target?.tagName === 'SELECT') return null
  const key = event.key.toUpperCase()
  if (key === UNDO_SHORTCUT) return { action: 'UNDO' }
  if (TOOL_SHORTCUTS[key]) return { action: 'TOOL', tool: TOOL_SHORTCUTS[key] }
  return null
}

/** The CSS class that colors a cell, e.g. 'NO_CHAIR' -> 'layout-cell--no-chair'. */
export function cellClassName(cellType) {
  return `layout-cell--${cellType.toLowerCase().replace('_', '-')}`
}

// ---------- Cells and the grid ----------

/** FR-020: only seats and workstations can hold a student. */
export function isUsable(cellType) {
  return cellType === 'SEAT' || cellType === 'WORKSTATION'
}

/** A new layout starts as seats in a lecture room and as workstations in a lab (as on the backend). */
export function startingCellType(roomType) {
  return roomType === 'COMPUTER_LAB' ? 'WORKSTATION' : 'SEAT'
}

/** FR-017: true for a whole number from 1 to 20 (the inputs give text, e.g. "8"). */
export function isValidSize(value) {
  const number = Number(value)
  return Number.isInteger(number) && number >= 1 && number <= MAX_ROWS_OR_COLUMNS
}

/** FR-020: "1 usable seat", "40 usable seats" (seats and workstations only). */
export function formatSeatCount(count) {
  return `${count} usable ${count === 1 ? 'seat' : 'seats'}`
}

export function countUsableSeats(cells) {
  return cells.flat().filter((cell) => isUsable(cell.cellType)).length
}

/** How many cells of each type the grid has, e.g. { SEAT: 12, WORKSTATION: 28, TABLE: 0, AISLE: 6, NO_CHAIR: 2 }. */
export function countCellsByType(cells) {
  const counts = Object.fromEntries(CELL_TYPES.map((type) => [type.value, 0]))
  for (const cell of cells.flat()) {
    counts[cell.cellType]++
  }
  return counts
}

/**
 * A copy of the grid that is at least rowCount x columnCount. It never removes cells, so making the grid
 * smaller and then bigger again brings back what was drawn. New cells continue the pattern: a new column
 * copies the type of the last cell in its row, a new row copies the row above. New cells have no number yet.
 */
export function growGrid(cells, rowCount, columnCount, fallbackCellType = 'SEAT') {
  const rows = Math.max(cells.length, rowCount)
  const columns = Math.max(cells[0]?.length ?? 0, columnCount)
  const grown = []
  for (let row = 0; row < rows; row++) {
    const cellsInRow = []
    for (let column = 0; column < columns; column++) {
      const existing = cells[row]?.[column]
      if (existing) {
        cellsInRow.push(existing) // drawn before (also cells that were hidden by making the grid smaller)
        continue
      }
      const cellType = patternTypeFor(cells, grown, row, column) ?? fallbackCellType
      cellsInRow.push({ seatId: null, cellType, seatLabel: null })
    }
    grown.push(cellsInRow)
  }
  return grown
}

// The type a new cell copies: in an existing row, the row's last cell; in a new row, the cell just above it
function patternTypeFor(oldCells, grownSoFar, row, column) {
  if (row < oldCells.length) {
    return oldCells[row].at(-1)?.cellType ?? null
  }
  return grownSoFar[row - 1]?.[column]?.cellType ?? null
}

/** The top-left rowCount x columnCount part of the grid: what is shown and saved. */
export function cropGrid(cells, rowCount, columnCount) {
  return cells.slice(0, rowCount).map((cellsInRow) => cellsInRow.slice(0, columnCount))
}

/** A copy of the (bigger) grid with its top-left part replaced by the shown part after a change. */
export function replaceTopLeft(cells, shownPart) {
  return cells.map((cellsInRow, row) => cellsInRow.map((cell, column) => shownPart[row]?.[column] ?? cell))
}

/** A copy of the grid with one cell changed, e.g. updateCell(cells, 0, 2, { seatLabel: '12' }). */
export function updateCell(cells, row, column, changes) {
  return cells.map((cellsInRow, rowIndex) =>
    rowIndex !== row
      ? cellsInRow
      : cellsInRow.map((cell, columnIndex) => (columnIndex === column ? { ...cell, ...changes } : cell)),
  )
}

/** Paints one cell. A cell that can no longer hold a student loses its number (as on the backend). */
export function paintCell(cells, row, column, cellType) {
  const cell = cells[row][column]
  if (cell.cellType === cellType) {
    return cells
  }
  return updateCell(cells, row, column, { cellType, seatLabel: isUsable(cellType) ? cell.seatLabel : null })
}

/** "2-5" for row 2, column 5. Used to remember which cells are selected or already touched. */
export function positionKey(row, column) {
  return `${row}-${column}`
}
