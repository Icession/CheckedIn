// cellSelection.js — the Select tool: choosing which cells Auto-number numbers (FR-019).
// Like painting and erasing: a tap or drag that starts on a selected cell unselects, otherwise it selects.
// Only cells the current "Apply to" can number are ever selected.
import { positionKey } from './layoutGrid.js'
import { applyToNouns, canNumberCell } from './seatNumbers.js'

/** The selected cells that the current "Apply to" can number; others (e.g. a cell painted into a table later) don't count. */
export function numberableSelection(cells, selectedKeys, applyTo) {
  const numberable = new Set()
  cells.forEach((cellsInRow, row) =>
    cellsInRow.forEach((cell, column) => {
      const key = positionKey(row, column)
      if (selectedKeys.has(key) && canNumberCell(cell, applyTo)) {
        numberable.add(key)
      }
    }),
  )
  return numberable
}

/** 'UNSELECT' when the tap or drag starts on a selected cell, otherwise 'SELECT'. */
export function selectionModeAt(selection, row, column) {
  return selection.has(positionKey(row, column)) ? 'UNSELECT' : 'SELECT'
}

/** A copy of the selection with one cell added ('SELECT') or removed ('UNSELECT'). Cells that can't be numbered are never added. */
export function changeSelection(selectedKeys, cells, row, column, mode, applyTo) {
  const changed = new Set(selectedKeys)
  const key = positionKey(row, column)
  if (mode === 'UNSELECT') {
    changed.delete(key)
  } else if (canNumberCell(cells[row][column], applyTo)) {
    changed.add(key)
  }
  return changed
}

/** "1 seat selected", "5 workstations selected". */
export function describeSelection(count, applyTo) {
  const nouns = applyToNouns(applyTo)
  return `${count} ${count === 1 ? nouns.one : nouns.many} selected`
}
