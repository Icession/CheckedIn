// LayoutList — room layouts found by the search, each opening its own page (FR-020, FR-021).
import { Link } from 'react-router-dom'
import { ROOM_TYPE_LABELS, formatSeatCount } from '../utils/layoutGrid.js'

export default function LayoutList({ layouts }) {
  if (layouts.length === 0) {
    return <p className="muted">No layouts found. Check the room name, or draw a new layout.</p>
  }

  return (
    <ul className="item-list">
      {layouts.map((layout) => (
        <li key={layout.layoutId}>
          <Link className="item-row" to={`/professor/layouts/${layout.layoutId}`}>
            <strong>{layout.roomName}</strong>
            <span>
              {ROOM_TYPE_LABELS[layout.roomType]} · {formatSeatCount(layout.usableSeatCount)}
            </span>
            <span className="hint-text">{layout.mine ? 'Made by you' : `Made by ${layout.createdByName}`}</span>
          </Link>
        </li>
      ))}
    </ul>
  )
}
