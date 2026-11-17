// TodaySessionList — the professor's classes for today, earliest first (FR-016).
// Each one opens its section for now; Phase 6 opens the attendance page instead.
import { Link } from 'react-router-dom'
import { DAY_LABELS, DELIVERY_MODE_LABELS, SESSION_TYPE_LABELS, formatTimeRange } from '../utils/format.js'

export default function TodaySessionList({ today, sessions }) {
  if (sessions.length === 0) {
    return <p className="muted">No classes today ({DAY_LABELS[today]}).</p>
  }

  return (
    <ul className="item-list">
      {sessions.map((session) => (
        <li key={session.sessionId}>
          <Link className="item-row" to={`/professor/sections/${session.sectionId}`}>
            <strong>{formatTimeRange(session.startTime, session.endTime)}</strong>
            <span>
              {session.subjectCode} {session.sectionCode} · {SESSION_TYPE_LABELS[session.sessionType]}
            </span>
            <span className="hint-text">
              {DELIVERY_MODE_LABELS[session.deliveryMode]}
              {session.roomName && ` · ${session.roomName}`}
            </span>
          </Link>
        </li>
      ))}
    </ul>
  )
}
