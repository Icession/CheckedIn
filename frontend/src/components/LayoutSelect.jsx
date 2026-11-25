// LayoutSelect — a drop-down list of every professor's room layouts, for in-person and hybrid
// sessions (FR-015, FR-021). The value is the chosen layoutId as text ('' means none chosen yet).
import { Link } from 'react-router-dom'
import { ROOM_TYPE_LABELS, formatSeatCount } from '../utils/layoutGrid.js'

export default function LayoutSelect({ id, value, layouts, onChange }) {
  return (
    <>
      <select id={id} value={value} onChange={(event) => onChange(event.target.value)} required>
        <option value="">Choose a room layout…</option>
        {layouts.map((layout) => (
          <option key={layout.layoutId} value={layout.layoutId}>
            {layout.roomName} – {ROOM_TYPE_LABELS[layout.roomType]}, {formatSeatCount(layout.usableSeatCount)}
          </option>
        ))}
      </select>
      <span className="hint-text">
        Room not in the list? <Link to="/professor/layouts/new">Draw a new layout</Link>
      </span>
    </>
  )
}
