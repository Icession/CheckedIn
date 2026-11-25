// SessionForm — adds a weekly session to a section (FR-012): day, start and end time, lecture or
// laboratory, how the class is held, and where. In-person and hybrid sessions choose a room layout;
// online and field sessions may type a place instead (FR-015). The backend refuses overlaps and an
// end time that is not later than the start (FR-013), and explains why.
import { useState } from 'react'
import { addSession } from '../api/sectionApi.js'
import LayoutSelect from './LayoutSelect.jsx'
import { DAY_LABELS, DELIVERY_MODE_LABELS, SCHOOL_DAYS, SESSION_TYPE_LABELS, usesLayout } from '../utils/format.js'

const EMPTY_SESSION = {
  dayOfWeek: 'MONDAY',
  startTime: '',
  endTime: '',
  sessionType: 'LECTURE',
  deliveryMode: 'IN_PERSON',
  roomName: '',
  layoutId: '',
}

// The JSON for the backend: a layout for in-person and hybrid sessions, a typed place for the others
function toAddRequest(session) {
  const { layoutId, roomName, ...times } = session
  return usesLayout(session.deliveryMode)
    ? { ...times, roomName: null, layoutId: layoutId === '' ? null : Number(layoutId) }
    : { ...times, roomName, layoutId: null }
}

export default function SessionForm({ sectionId, layouts, onAdded }) {
  const [session, setSession] = useState(EMPTY_SESSION)
  const [error, setError] = useState('')
  const [isSaving, setIsSaving] = useState(false)

  function updateField(event) {
    setSession({ ...session, [event.target.name]: event.target.value })
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setIsSaving(true)
    try {
      onAdded(await addSession(sectionId, toAddRequest(session)))
      setSession(EMPTY_SESSION)
    } catch (addError) {
      // For example "This overlaps IT342 G01 (lecture, Thursday 6:00 PM - 9:00 PM)..."
      setError(addError.message)
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <form className="form" onSubmit={handleSubmit}>
      <h3>Add a weekly session</h3>
      <div className="form-grid">
        <div className="field">
          <label htmlFor="dayOfWeek">Day</label>
          <select id="dayOfWeek" name="dayOfWeek" value={session.dayOfWeek} onChange={updateField}>
            {SCHOOL_DAYS.map((day) => (
              <option key={day} value={day}>
                {DAY_LABELS[day]}
              </option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor="sessionType">Type</label>
          <select id="sessionType" name="sessionType" value={session.sessionType} onChange={updateField}>
            {Object.entries(SESSION_TYPE_LABELS).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor="startTime">Starts at</label>
          <input id="startTime" name="startTime" type="time" value={session.startTime} onChange={updateField} required />
        </div>
        <div className="field">
          <label htmlFor="endTime">Ends at</label>
          <input id="endTime" name="endTime" type="time" value={session.endTime} onChange={updateField} required />
        </div>
        <div className="field">
          <label htmlFor="deliveryMode">How it is held</label>
          <select id="deliveryMode" name="deliveryMode" value={session.deliveryMode} onChange={updateField}>
            {Object.entries(DELIVERY_MODE_LABELS).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        </div>
        {usesLayout(session.deliveryMode) ? (
          <div className="field">
            <label htmlFor="layoutId">Room layout</label>
            <LayoutSelect
              id="layoutId"
              value={session.layoutId}
              layouts={layouts}
              onChange={(layoutId) => setSession({ ...session, layoutId })}
            />
          </div>
        ) : (
          <div className="field">
            <label htmlFor="roomName">Place (optional)</label>
            <input id="roomName" name="roomName" placeholder="Cebu IT Park" maxLength={30} value={session.roomName} onChange={updateField} />
          </div>
        )}
      </div>
      <button type="submit" className="button-primary" disabled={isSaving}>
        {isSaving ? 'Adding…' : 'Add session'}
      </button>
      {error && <p className="error-text" role="alert">{error}</p>}
    </form>
  )
}
