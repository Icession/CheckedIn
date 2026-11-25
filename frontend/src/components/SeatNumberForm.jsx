// SeatNumberForm — changes the number of one seat or workstation (FR-019), e.g. 12 or PC-12.
// A number another seat already has is refused right away (the backend checks again on save).
// An empty number means "give it the next free number when saving".
import { useState } from 'react'
import { cellTypeInfo } from '../utils/layoutGrid.js'
import { positionAsSeen } from '../utils/layoutView.js'
import { findSeatWithLabel, isValidSeatLabel, normalizeSeatLabel } from '../utils/seatLabels.js'

export default function SeatNumberForm({ cells, row, column, onApply, onCancel }) {
  const cell = cells[row][column]
  // Rows counted from the front, columns from the screen's left, as in the grid's tooltips
  const seen = (position) => positionAsSeen(position, cells.length, cells[0].length)
  const [number, setNumber] = useState(cell.seatLabel ?? '')
  const [error, setError] = useState('')
  const typeName = cellTypeInfo(cell.cellType).label.toLowerCase()

  function handleSubmit(event) {
    event.preventDefault()
    const label = normalizeSeatLabel(number)
    if (label !== null && !isValidSeatLabel(label)) {
      setError('A seat number can only have letters, numbers and hyphens (at most 10), for example 12 or PC-12.')
      return
    }
    const otherSeat = label === null ? null : findSeatWithLabel(cells, label, row, column)
    if (otherSeat) {
      const { rowNumber, columnNumber } = seen(otherSeat)
      setError(`${label} is already used by another seat (row ${rowNumber}, column ${columnNumber}). Choose another number.`)
      return
    }
    onApply(label)
  }

  return (
    <form className="form" onSubmit={handleSubmit}>
      <label htmlFor="seatNumber">
        Label of the {typeName} in row {seen({ row, column }).rowNumber}, column {seen({ row, column }).columnNumber}
      </label>
      <input
        id="seatNumber"
        value={number}
        maxLength={10}
        placeholder="12 or PC-12"
        onChange={(event) => {
          setNumber(event.target.value)
          setError('')
        }}
        autoFocus
      />
      <div className="row">
        <button type="submit" className="button-primary">
          Use this label
        </button>
        <button type="button" className="button-link" onClick={onCancel}>
          Cancel
        </button>
      </div>
      {error ? (
        <p className="error-text" role="alert">{error}</p>
      ) : (
        <p className="hint-text">Leave it empty to get the next free number when you save.</p>
      )}
    </form>
  )
}
