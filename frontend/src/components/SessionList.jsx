// SessionList — a section's weekly sessions, Monday to Saturday, with their length in hours
// (FR-012, FR-014). While the section is not archived, each session can be removed.
import {
  DAY_LABELS,
  DELIVERY_MODE_LABELS,
  SESSION_TYPE_LABELS,
  formatHours,
  formatTimeRange,
} from '../utils/format.js'

export default function SessionList({ sessions, canRemove, onRemove }) {
  if (sessions.length === 0) {
    return <p className="muted">No weekly sessions yet.</p>
  }

  return (
    <ul className="item-list">
      {sessions.map((session) => (
        <li key={session.sessionId} className="item-row">
          <strong>
            {DAY_LABELS[session.dayOfWeek]} {formatTimeRange(session.startTime, session.endTime)}
          </strong>
          <span>
            {SESSION_TYPE_LABELS[session.sessionType]} · {formatHours(session.hours)}
          </span>
          <span className="hint-text">
            {DELIVERY_MODE_LABELS[session.deliveryMode]}
            {session.roomName && ` · ${session.roomName}`}
          </span>
          {canRemove && (
            <button type="button" className="button-link button-link--danger" onClick={() => onRemove(session)}>
              Remove
            </button>
          )}
        </li>
      ))}
    </ul>
  )
}
