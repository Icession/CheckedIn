// NewLayoutPage — start a new room layout at /professor/layouts/new (FR-017): room name, room type,
// rows and columns. The new grid is filled with seats (lecture room) or workstations (lab), numbered
// 1, 2, 3…, and opens in the layout builder. What is typed is kept in this browser until the layout is
// created, and offered again after a refresh (useDraftBackup).
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { createLayout } from '../api/layoutApi.js'
import LayoutDetailsFields from '../components/LayoutDetailsFields.jsx'
import PageHeader from '../components/PageHeader.jsx'
import RestoreDraftBar from '../components/RestoreDraftBar.jsx'
import useDraftBackup from '../hooks/useDraftBackup.js'

const EMPTY_FORM = { roomName: '', roomType: 'LECTURE_ROOM', rowCount: '6', columnCount: '8' }

// True when the form still holds what it started with (nothing worth keeping)
function isEmptyForm(details) {
  return Object.keys(EMPTY_FORM).every((field) => details?.[field] === EMPTY_FORM[field])
}

export default function NewLayoutPage() {
  const navigate = useNavigate()
  const [details, setDetails] = useState(EMPTY_FORM)
  const [error, setError] = useState('')
  const [isSaving, setIsSaving] = useState(false)
  const backup = useDraftBackup({ name: 'layout-new', work: details, hasChanges: !isEmptyForm(details), matchesSaved: isEmptyForm })

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setIsSaving(true)
    try {
      const created = await createLayout({
        ...details,
        rowCount: Number(details.rowCount),
        columnCount: Number(details.columnCount),
      })
      backup.clear()
      navigate(`/professor/layouts/${created.layoutId}`)
    } catch (createError) {
      // The backend explains what is wrong, e.g. "There is already a layout for NGE207..."
      setError(createError.message)
      setIsSaving(false)
    }
  }

  return (
    <main className="page">
      <PageHeader />
      <Link className="back-link" to="/professor/layouts">
        ← Back to room layouts
      </Link>
      {backup.offer && (
        <RestoreDraftBar savedAt={backup.offer.savedAt} onRestore={() => setDetails({ ...EMPTY_FORM, ...backup.restore() })} onDiscard={backup.discard} />
      )}
      <section className="card narrow-card">
        <h2>New layout</h2>
        <p className="hint-text">
          Count the rows from the front of the room (the board) to the back. You can paint aisles, tables and
          empty spots in the next step.
        </p>
        <form className="form" onSubmit={handleSubmit}>
          <LayoutDetailsFields values={details} onChange={(name, value) => setDetails({ ...details, [name]: value })} />
          <button type="submit" className="button-primary" disabled={isSaving}>
            {isSaving ? 'Creating…' : 'Create and start drawing'}
          </button>
          {error && <p className="error-text" role="alert">{error}</p>}
        </form>
      </section>
    </main>
  )
}
