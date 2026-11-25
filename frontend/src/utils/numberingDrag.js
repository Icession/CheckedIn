// numberingDrag.js — the Number tool as a renumbering brush (FR-019). Dragging across cells gives each one
// the next label of its type, in the order the cells are touched, starting from "Next".
// - Each cell is numbered at most once per drag.
// - If an untouched cell of the same type already has the label, it gives it up (it becomes unnumbered and
//   is marked in the grid); the drag goes on. A label of the other type blocks the drag instead.
// - After the drag, Next is the last number used in it + 1 (drag.nextNumbers). Start at is not touched.
import { positionKey, updateCell } from './layoutGrid.js'
import { describeCell, isValidSeatLabel } from './seatLabels.js'
import { canNumberCell, formatSeatLabel, prefixFor } from './seatNumbers.js'

/**
 * The start of one drag: the grid, the Next number of each type, the cells this drag has numbered, and the
 * cells that lost their label to it.
 */
export function startNumberingDrag(cells, nextNumbers) {
  return {
    cells,
    nextNumbers,
    numberedKeys: new Set(),
    unnumberedKeys: new Set(),
    problem: '',
  }
}

/** One cell touched during a drag. Returns the new drag; the old one is not changed. */
export function numberCellInDrag(drag, row, column, settings) {
  const key = positionKey(row, column)
  const cell = drag.cells[row][column]
  if (drag.problem || drag.numberedKeys.has(key) || !canNumberCell(cell, settings.applyTo)) {
    return drag
  }
  const label = formatSeatLabel(prefixFor(settings, cell.cellType), drag.nextNumbers[cell.cellType])
  if (!isValidSeatLabel(label)) {
    return { ...drag, problem: `“${label}” is too long: a number can have at most 10 characters. Use a shorter prefix.` }
  }
  const owners = untouchedCellsWithLabel(drag, label, row, column)
  const otherTypeOwner = owners.find((owner) => drag.cells[owner.row][owner.column].cellType !== cell.cellType)
  if (otherTypeOwner) {
    return { ...drag, problem: `${label} is already used by ${describeCell(drag.cells, otherTypeOwner)}. `
      + 'Seats and workstations can’t share a number: give one of them a prefix.' }
  }
  return giveLabel(drag, row, column, label, owners)
}

// The touched cell gets the label; untouched cells of the same type that had it give it up
function giveLabel(drag, row, column, label, owners) {
  const key = positionKey(row, column)
  let cells = updateCell(drag.cells, row, column, { seatLabel: label })
  const unnumberedKeys = new Set(drag.unnumberedKeys)
  unnumberedKeys.delete(key) // it may have lost its label earlier in this drag
  for (const owner of owners) {
    cells = updateCell(cells, owner.row, owner.column, { seatLabel: null })
    unnumberedKeys.add(positionKey(owner.row, owner.column))
  }
  const cellType = drag.cells[row][column].cellType
  return {
    cells,
    nextNumbers: { ...drag.nextNumbers, [cellType]: drag.nextNumbers[cellType] + 1 },
    numberedKeys: new Set(drag.numberedKeys).add(key),
    unnumberedKeys,
    problem: '',
  }
}

// The seats and workstations, other than this cell and those numbered in this drag, that have this label
function untouchedCellsWithLabel(drag, label, row, column) {
  const owners = []
  drag.cells.forEach((cellsInRow, otherRow) =>
    cellsInRow.forEach((cell, otherColumn) => {
      const isThisCell = otherRow === row && otherColumn === column
      const numberedInThisDrag = drag.numberedKeys.has(positionKey(otherRow, otherColumn))
      if (!isThisCell && !numberedInThisDrag && cell.seatLabel === label) {
        owners.push({ row: otherRow, column: otherColumn })
      }
    }),
  )
  return owners
}

/** "3 seats lost their numbers – they're marked so you can number them.", or '' when none did. */
export function describeLostLabels(drag) {
  const count = drag.unnumberedKeys.size
  if (count === 0) return ''
  const types = new Set([...drag.unnumberedKeys].map((key) => {
    const [row, column] = key.split('-').map(Number)
    return drag.cells[row][column].cellType
  }))
  const noun = types.size > 1 ? 'seats and workstations' : (types.has('SEAT') ? 'seat' : 'workstation')
  if (count === 1) {
    return `1 ${noun} lost its number – it’s marked so you can number it.`
  }
  const plural = types.size > 1 ? noun : `${noun}s`
  return `${count} ${plural} lost their numbers – they’re marked so you can number them.`
}
