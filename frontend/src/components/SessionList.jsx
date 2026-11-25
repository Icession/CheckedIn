// SessionList — a section's weekly sessions, Monday to Saturday, with their length in hours and their
// room (FR-012, FR-014, FR-015). While the section is not archived, each session can be removed, and an
// in-person or hybrid session can get or change its room layout (one session at a time).
import { useState } from 'react'
import { Link } from 'react-router-dom'
import SessionLayoutForm from './SessionLayoutForm.jsx'
import {
  DAY_LABELS,
  DELIVERY_MODE_LABELS,
  SESSION_TYPE_LABELS,
  formatHours,
  formatTimeRange,
  usesLayout,
} from '../utils/format.js'

export default function SessionList({ sectionId, sessions, layouts, canChange, onRemove, onLayoutChosen }) {
  const [sessionIdChoosingLayout, setSessionIdChoosingLayout] = useState(null)

  if (sessions.length === 0) {
    return <p className="muted">No weekly sessions yet.</p>
  }

  function handleLayoutSaved(savedSession) {
    setSessionIdChoosingLayout(null)
    onLayoutChosen(savedSession)
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
            {session.layoutId ? (
              <>
                {' · '}
                <Link to={`/professor/layouts/${session.layoutId}`}>{session.roomName}</Link>
              </>
            ) : (
              session.roomName && ` · ${session.roomName}`
            )}
          </span>
          {session.layoutMissing && <span className="error-text">This session has no room layout yet.</span>}

          {canChange && sessionIdChoosingLayout === session.sessionId && (
            <SessionLayoutForm
              sectionId={sectionId}
              session={session}
              layouts={layouts}
              onSaved={handleLayoutSaved}
              onCancel={() => setSessionIdChoosingLayout(null)}
            />
          )}
          {canChange && sessionIdChoosingLayout !== session.sessionId && (
            <div className="row">
              {usesLayout(session.deliveryMode) && (
                <button type="button" className="button-link" onClick={() => setSessionIdChoosingLayout(session.sessionId)}>
                  {session.layoutId ? 'Change layout' : 'Choose layout'}
                </button>
              )}
              <button type="button" className="button-link button-link--danger" onClick={() => onRemove(session)}>
                Remove
              </button>
            </div>
          )}
        </li>
      ))}
    </ul>
  )
}
