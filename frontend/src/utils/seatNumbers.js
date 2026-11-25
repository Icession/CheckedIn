// seatNumbers.js — the numbering settings of the layout builder (FR-019) and the order cells are numbered in.
// Settings: "Apply to", and per type a prefix and a "Start at"; plus where numbering starts (a corner of the
// room as seen on screen, see layoutView.js) and its direction. Two numbers per type: "Start at" is set only
// by the professor; "Next" is the running counter for dragging and for numbering a selected block (it lives in the draft, see layoutDraft.js).
// Labels are plain numbers with an optional prefix, e.g. 12 or PC-12; checking them is in seatLabels.js.
import { cellTypeInfo, isUsable, positionKey, startingCellType } from './layoutGrid.js'
import { toScreenPosition } from './layoutView.js'
import { guessPrefix } from './seatLabels.js'

const PREFIX_PATTERN = /^[A-Z0-9-]*$/

// Which cells Auto-number and the Number tool may number
export const APPLY_TO_LABELS = { WORKSTATION: 'Workstations only', SEAT: 'Seats only', BOTH: 'Both' }

// Where numbering starts. The front row is the one next to the "Front of room" bar (at the bottom); left and
// right are the sides of the screen (the room is seen from the teacher's desk, so the screen's left is the
// students' right).
export const START_CORNER_LABELS = {
  FRONT_LEFT: 'Front row, left side of the screen',
  FRONT_RIGHT: 'Front row, right side of the screen',
  BACK_LEFT: 'Back row, left side of the screen',
  BACK_RIGHT: 'Back row, right side of the screen',
}

// How numbering goes from one row to the next
export const DIRECTION_LABELS = { SAME: 'Same direction every row', SNAKE: 'Snake (every other row turns back)' }

// What "Apply to" numbers, in words: [one, many], e.g. "1 seat selected", "5 seats selected"
const APPLY_TO_NOUNS = {
  WORKSTATION: ['workstation', 'workstations'],
  SEAT: ['seat', 'seats'],
  BOTH: ['seat or workstation', 'seats and workstations'],
}

/** The words for what "Apply to" numbers: applyToNouns('SEAT') -> { one: 'seat', many: 'seats' }. */
export function applyToNouns(applyTo) {
  const [one, many] = APPLY_TO_NOUNS[applyTo]
  return { one, many }
}

// ---------- The numbering order ----------

/**
 * The cells in numbering order, as [{ row, column }, ...]. Numbering goes row by row, starting from a corner:
 * the front rows first (FRONT_...) or the back rows first (BACK_...), each row from the left side of the
 * screen (..._LEFT) or the right side (..._RIGHT). With 'SNAKE', every other row turns back.
 * Tables, aisles and "no chair" cells are skipped; a row without such cells does not count, so the snake keeps
 * alternating. cellType keeps only one type, selectedKeys only the selected cells.
 */
export function cellsInNumberingOrder(cells, {
  startCorner = 'FRONT_LEFT', direction = 'SAME', cellType = null, selectedKeys = null,
} = {}) {
  const rows = cells.map((_, row) => row) // row 0 is the front row
  if (startCorner.startsWith('BACK')) rows.reverse()
  const startsOnScreenLeft = startCorner.endsWith('LEFT')
  const screenColumn = (position) => toScreenPosition(position, cells.length, cells[0].length).column
  const positions = []
  let rowsNumbered = 0
  for (const row of rows) {
    const inRow = []
    cells[row].forEach((cell, column) => {
      const isRightType = cellType === null ? isUsable(cell.cellType) : cell.cellType === cellType
      const isIncluded = selectedKeys === null || selectedKeys.has(positionKey(row, column))
      if (isRightType && isIncluded) inRow.push({ row, column })
    })
    if (inRow.length === 0) continue
    inRow.sort((first, second) => screenColumn(first) - screenColumn(second)) // as seen: screen left to right
    const turnsBack = direction === 'SNAKE' && rowsNumbered % 2 === 1
    const goesScreenLeftToRight = turnsBack ? !startsOnScreenLeft : startsOnScreenLeft
    if (!goesScreenLeftToRight) inRow.reverse()
    positions.push(...inRow)
    rowsNumbered++
  }
  return positions
}

/** "from the front row, left side of the screen, same direction every row" (for hints and messages). */
export function describeNumberingOrder(settings) {
  const corner = START_CORNER_LABELS[settings.startCorner].toLowerCase()
  return `from the ${corner}, ${settings.direction === 'SNAKE' ? 'snaking' : 'same direction every row'}`
}

// ---------- The settings shared by Auto-number and the Number tool ----------

// Seats and workstations each have their own prefix and their own "Start at" box
const PREFIX_FIELD = { WORKSTATION: 'workstationPrefix', SEAT: 'seatPrefix' }
const START_FIELD = { WORKSTATION: 'workstationStartAt', SEAT: 'seatStartAt' }

/**
 * The starting settings: workstations in a lab, seats in a lecture room, Start at 1, from the front row on the
 * left side of the screen, seen from the teacher's desk.
 * The prefixes are taken from the labels already in the grid (they aren't saved), e.g. "WS-".
 */
export function defaultNumberingSettings(roomType, cells = []) {
  return {
    applyTo: startingCellType(roomType),
    workstationPrefix: guessPrefix(cells, 'WORKSTATION'),
    seatPrefix: guessPrefix(cells, 'SEAT'),
    workstationStartAt: '1',
    seatStartAt: '1',
    startCorner: 'FRONT_LEFT',
    direction: 'SAME',
  }
}

/** The cell types that "Apply to" numbers, e.g. 'BOTH' -> ['WORKSTATION', 'SEAT']. */
export function numberingTypes(applyTo) {
  return applyTo === 'BOTH' ? ['WORKSTATION', 'SEAT'] : [applyTo]
}

/** The names of one type's two settings, e.g. 'SEAT' -> { prefixField: 'seatPrefix', startField: 'seatStartAt' }. */
export function numberingFieldsFor(cellType) {
  return { prefixField: PREFIX_FIELD[cellType], startField: START_FIELD[cellType] }
}

/** The prefix of one cell type, cleaned like a seat number: " pc-" -> "PC-". */
export function prefixFor(settings, cellType) {
  return settings[PREFIX_FIELD[cellType]].trim().toUpperCase()
}

/** The "Start at" number of one cell type: the number Auto-number of all cells begins with. */
export function startFor(settings, cellType) {
  return Number(settings[START_FIELD[cellType]])
}

/** The first "Next" of each type: its Start at, e.g. { WORKSTATION: 1, SEAT: 1 }. */
export function firstNumbers(settings) {
  return { WORKSTATION: startFor(settings, 'WORKSTATION'), SEAT: startFor(settings, 'SEAT') }
}

/** Which type a Start at box belongs to: 'seatStartAt' -> 'SEAT'; null for the other fields. */
export function cellTypeOfStartField(fieldName) {
  return Object.keys(START_FIELD).find((cellType) => START_FIELD[cellType] === fieldName) ?? null
}

/** True when the Number tool or Auto-number may number this cell: a seat or workstation that "Apply to" includes. */
export function canNumberCell(cell, applyTo) {
  return isUsable(cell.cellType) && numberingTypes(applyTo).includes(cell.cellType)
}

/** For the hint: "WS-13", or "workstation WS-13, seat 4" when both types are numbered (the Next counters). */
export function describeNextLabels(settings, nextNumbers) {
  if (numberingSettingsProblem(settings)) return 'fix the numbering settings first'
  const types = numberingTypes(settings.applyTo)
  const typeName = (cellType) => (types.length === 1 ? '' : `${cellTypeInfo(cellType).label.toLowerCase()} `)
  return types.map((cellType) => typeName(cellType) + formatSeatLabel(prefixFor(settings, cellType), nextNumbers[cellType])).join(', ')
}

/** A plain-language problem with the settings themselves, or '' when they are fine. */
export function numberingSettingsProblem(settings) {
  for (const cellType of numberingTypes(settings.applyTo)) {
    const typeName = cellTypeInfo(cellType).label.toLowerCase()
    const typedStart = String(settings[START_FIELD[cellType]]).trim()
    const start = Number(typedStart)
    if (typedStart === '' || !Number.isInteger(start) || start < 0) {
      return `The ${typeName} “Start at” must be a whole number, for example 1.`
    }
    if (!PREFIX_PATTERN.test(prefixFor(settings, cellType))) {
      return `The ${typeName} prefix can only have letters, numbers and hyphens, for example PC-.`
    }
  }
  return ''
}

// ---------- Building labels ----------

/** A plain label, no zero padding: ("PC-", 7) -> "PC-7", ("", 12) -> "12". */
export function formatSeatLabel(prefix, number) {
  return `${prefix}${number}`
}

/** count labels in a row, e.g. ("PC-", 21, 3) -> ["PC-21", "PC-22", "PC-23"]. */
export function buildSeatLabels(prefix, startAt, count) {
  return Array.from({ length: count }, (_, index) => formatSeatLabel(prefix, startAt + index))
}
