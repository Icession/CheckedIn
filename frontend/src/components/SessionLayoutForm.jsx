// SessionLayoutForm — chooses or changes the room layout of an existing in-person or hybrid session
// (FR-015), for example a session added before layouts existed, or a class that moved rooms.
import { useState } from 'react'
import { chooseSessionLayout } from '../api/sectionApi.js'
import LayoutSelect from './LayoutSelect.jsx'

export default function SessionLayoutForm({ sectionId, session, layouts, onSaved, onCancel }) {
  const [layoutId, setLayoutId] = useState(session.layoutId ? String(session.layoutId) : '')
  const [error, setError] = useState('')
  const [isSaving, setIsSaving] = useState(false)
  const selectId = `layoutFor${session.sessionId}`

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setIsSaving(true)
    try {
      onSaved(await chooseSessionLayout(sectionId, session.sessionId, Number(layoutId)))
    } catch (saveError) {
      setError(saveError.message)
      setIsSaving(false)
    }
  }

  return (
    <form className="form session-layout-form" onSubmit={handleSubmit}>
      <label htmlFor={selectId}>Room layout</label>
      <LayoutSelect id={selectId} value={layoutId} layouts={layouts} onChange={setLayoutId} />
      <div className="row">
        <button type="submit" className="button-primary" disabled={isSaving || layoutId === ''}>
          {isSaving ? 'Saving…' : 'Use this layout'}
        </button>
        <button type="button" className="button-link" onClick={onCancel}>
          Cancel
        </button>
      </div>
      {error && <p className="error-text" role="alert">{error}</p>}
    </form>
  )
}
