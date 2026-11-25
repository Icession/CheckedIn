// LayoutView — a layout shown read-only, for professors who did not make it (FR-021):
// they can look at it and use it for their sessions, but only its creator can change it.
import LayoutGrid from './LayoutGrid.jsx'
import { ROOM_TYPE_LABELS, formatSeatCount } from '../utils/layoutGrid.js'

export default function LayoutView({ layout }) {
  return (
    <section className="card">
      <div className="card-title-row">
        <h2>{layout.roomName}</h2>
        <strong>{formatSeatCount(layout.usableSeatCount)}</strong>
      </div>
      <p className="hint-text">
        {ROOM_TYPE_LABELS[layout.roomType]} · {layout.rowCount} rows × {layout.columnCount} columns · made by{' '}
        {layout.createdByName}
      </p>
      <p className="notice-banner">
        Only {layout.createdByName} can change this layout. You can still use it for your sessions.
      </p>
      <LayoutGrid cells={layout.cells} readOnly />
    </section>
  )
}
