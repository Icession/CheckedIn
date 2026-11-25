// autoNumber.js — Auto-number (FR-019), in the order set by "Start from" and "Direction" (cellsInNumberingOrder):
// - nothing selected: a full renumber of the chosen type(s) from "Start at" (the same result every time);
// - cells selected: the selected block continues from "Next", e.g. a left block 1–20, then a right block 21…
// Afterwards Next = last number + 1. A numbering that would repeat a label is refused.
import { isUsable, positionKey } from './layoutGrid.js'
import { describeCell, isValidSeatLabel } from './seatLabels.js'
import {
  applyToNouns, buildSeatLabels, cellsInNumberingOrder, numberingSettingsProblem, numberingTypes, prefixFor, startFor,
} from './seatNumbers.js'

/**
 * What Auto-number would do: for each type in "Apply to", its first number, its cells in order and their new
 * labels, e.g. [{ cellType: 'WORKSTATION', start: 1, positions: [...], labels: ['PC-1', 'PC-2'] }].
 * Each type counts on its own: from Start at, or from Next when only selected cells are numbered.
 */
export function planAutoNumbers(cells, settings, { selectedKeys = null, nextNumbers = null } = {}) {
  return numberingTypes(settings.applyTo).map((cellType) => {
    const start = selectedKeys ? nextNumbers[cellType] : startFor(settings, cellType)
    const { startCorner, direction } = settings
    const positions = cellsInNumberingOrder(cells, { startCorner, direction, cellType, selectedKeys })
    const labels = buildSeatLabels(prefixFor(settings, cellType), start, positions.length)
    return { cellType, start, positions, labels }
  })
}

/**
 * Labels must stay unique in the layout. Returns a message naming the label that would clash, either
 * between the new seat and workstation numbers, or with a cell that keeps its number; '' when none does.
 */
export function findNumberingClash(cells, plan) {
  const typeOfNewLabel = new Map()
  for (const group of plan) {
    for (const label of group.labels) {
      const otherType = typeOfNewLabel.get(label)
      if (otherType && otherType !== group.cellType) {
        return `“${label}” would be used by both a seat and a workstation. Add a prefix to one of them, for example PC- for workstations.`
      }
      typeOfNewLabel.set(label, group.cellType)
    }
  }
  // The cells that keep their number (they are not numbered now), by label
  const numberedKeys = new Set(plan.flatMap((group) => group.positions.map(({ row, column }) => positionKey(row, column))))
  const keptLabelOwners = new Map()
  cells.forEach((cellsInRow, row) => cellsInRow.forEach((cell, column) => {
    const keepsItsNumber = isUsable(cell.cellType) && cell.seatLabel !== null && !numberedKeys.has(positionKey(row, column))
    if (keepsItsNumber) keptLabelOwners.set(cell.seatLabel, { row, column })
  }))
  // The first new number, in numbering order, that a kept number already uses
  const clash = plan.flatMap((group) => group.labels).find((label) => keptLabelOwners.has(label))
  if (!clash) return ''
  return `“${clash}” is already used by ${describeCell(cells, keptLabelOwners.get(clash))}, which keeps its number. `
    + 'Add a prefix (for example PC- for workstations) or change Start at.'
}

/** Why Auto-number can't be applied, in plain words, or '' when it can. */
export function autoNumberProblem(cells, settings, plan) {
  const settingsProblem = numberingSettingsProblem(settings)
  if (settingsProblem) return settingsProblem
  if (plan.every((group) => group.positions.length === 0)) {
    return `There are no ${applyToNouns(settings.applyTo).many} to number.`
  }
  const tooLong = plan.flatMap((group) => group.labels).find((label) => !isValidSeatLabel(label))
  if (tooLong) return `“${tooLong}” is too long: a number can have at most 10 characters. Use a shorter prefix.`
  return findNumberingClash(cells, plan)
}

/** A copy of the grid where positions[i] gets labels[i]; all other cells stay the same. */
export function setSeatLabels(cells, positions, labels) {
  const labelByKey = new Map(positions.map((position, index) => [positionKey(position.row, position.column), labels[index]]))
  return cells.map((cellsInRow, row) =>
    cellsInRow.map((cell, column) => {
      const key = positionKey(row, column)
      return labelByKey.has(key) ? { ...cell, seatLabel: labelByKey.get(key) } : cell
    }),
  )
}

/** Applies a plan from planAutoNumbers; cells of the other type keep their numbers. */
export function applyAutoNumbers(cells, plan) {
  return plan.reduce((current, group) => setSeatLabels(current, group.positions, group.labels), cells)
}

/** After numbering with a plan: Next of each numbered type is its last number + 1 (Start at is not touched). */
export function nextAfterPlan(nextNumbers, plan) {
  const numbered = plan.filter((group) => group.positions.length > 0)
  return { ...nextNumbers, ...Object.fromEntries(numbered.map((group) => [group.cellType, group.start + group.positions.length])) }
}
