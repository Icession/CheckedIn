// testGrids.js — small helpers for the unit tests of the layout builder (used only by the *.test.js files).
// grid() and labelsOf() use the stored order (row 0 = the front row). The ...OnScreen helpers use the picture the
// professor sees: the room turned around (see layoutView.js), top row first = the back row, bottom row = the front.
import { positionKey } from './layoutGrid.js'
import { fromScreenPosition } from './layoutView.js'

// Draws a grid from short text rows: W = workstation, S = seat, T = table, A = aisle, N = no chair
const TYPE_BY_LETTER = { W: 'WORKSTATION', S: 'SEAT', T: 'TABLE', A: 'AISLE', N: 'NO_CHAIR' }

export function grid(...rows) {
  return rows.map((row) => [...row].map((letter) => ({ seatId: null, cellType: TYPE_BY_LETTER[letter], seatLabel: null })))
}

/** The numbers of a grid, row by row, e.g. [['1', null], ...]. */
export function labelsOf(cells) {
  return cells.map((cellsInRow) => cellsInRow.map((cell) => cell.seatLabel))
}

/** The cell types of a grid as text rows, the opposite of grid(), e.g. ['WTW', 'AAA']. */
export function typesOf(cells) {
  const letterByType = Object.fromEntries(Object.entries(TYPE_BY_LETTER).map(([letter, type]) => [type, letter]))
  return cells.map((cellsInRow) => cellsInRow.map((cell) => letterByType[cell.cellType]).join(''))
}

/** A copy of the grid with the given labels written row by row (null = no number). */
export function withLabels(cells, labelRows) {
  return cells.map((cellsInRow, row) => cellsInRow.map((cell, column) => ({ ...cell, seatLabel: labelRows[row]?.[column] ?? null })))
}

/** Turns rows around fully (180°): the last row comes first, and each row is read backwards. */
export function turnAround(rows) {
  return [...rows].reverse().map((row) => [...row].reverse())
}

/** A grid drawn as seen on screen, top row first, e.g. gridOnScreen('WWW', 'WTW'): the front row is 'WTW'. */
export function gridOnScreen(...rowsAsSeen) {
  return turnAround(grid(...rowsAsSeen))
}

/** The numbers as seen on screen, top row first. */
export function labelsOnScreen(cells) {
  return turnAround(labelsOf(cells))
}

/** The cell types as seen on screen, top row first. */
export function typesOnScreen(cells) {
  return typesOf(turnAround(cells))
}

/** Like withLabels, with the labels given as seen on screen, top row first. */
export function withLabelsOnScreen(cells, labelRowsAsSeen) {
  return turnAround(withLabels(turnAround(cells), labelRowsAsSeen))
}

/** The keys of the cells at these screen positions ({ row, column } counted from the top left of the screen). */
export function keysOnScreen(cells, screenPositions) {
  return new Set(screenPositions.map((screenPosition) => {
    const { row, column } = fromScreenPosition(screenPosition, cells.length, cells[0].length)
    return positionKey(row, column)
  }))
}
