// LayoutCell — one square of a layout grid (FR-018, FR-019). Its color and shape show the cell type,
// and it shows the seat or workstation number, or the type's letter when there is no number.
// A number used twice is shown in red; a seat without a number has a dashed outline. It is a button, so it
// works with a tap, a mouse and the keyboard. positionSeen = { rowNumber, columnNumber } as people read them
// (row 1 = the front row, at the bottom; columns from the screen's left; see layoutView.js).
import { cellClassName, cellTypeInfo, isUsable } from '../utils/layoutGrid.js'

export default function LayoutCell({
  cell, positionSeen, isSelected, isInSelection, hasDuplicateNumber, readOnly, onClick, onPointerDown, onPointerEnter,
}) {
  const type = cellTypeInfo(cell.cellType)
  const number = cell.seatLabel ? ` ${cell.seatLabel}` : ''
  const noNumberYet = isUsable(cell.cellType) && !cell.seatLabel // shown with a dashed outline
  const warning = hasDuplicateNumber ? ' (this number is used twice)' : noNumberYet ? ' (no number yet)' : ''
  const description = `Row ${positionSeen.rowNumber}, column ${positionSeen.columnNumber}: ${type.label}${number}${warning}`
  const classes = [
    'layout-cell',
    cellClassName(cell.cellType),
    cell.seatLabel ? '' : 'layout-cell--no-number',
    isSelected ? 'layout-cell--selected' : '',
    isInSelection ? 'layout-cell--in-selection' : '',
    hasDuplicateNumber ? 'layout-cell--duplicate' : '',
  ].join(' ')

  return (
    <button
      type="button"
      className={classes}
      title={description}
      aria-label={description}
      disabled={readOnly}
      onClick={onClick}
      onPointerDown={onPointerDown}
      onPointerEnter={onPointerEnter}
    >
      {cell.seatLabel ?? type.letter}
    </button>
  )
}
