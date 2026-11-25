// SectionPage — one section at /professor/sections/:sectionId (FR-010 – FR-015): its details,
// its weekly sessions with their hours and room layouts, adding and removing sessions, editing and archiving.
// An archived section is shown read-only (FR-011).
import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { searchLayouts } from '../api/layoutApi.js'
import { archiveSection, getSection, removeSession } from '../api/sectionApi.js'
import PageHeader from '../components/PageHeader.jsx'
import SessionForm from '../components/SessionForm.jsx'
import SessionList from '../components/SessionList.jsx'
import { DAY_LABELS, SEMESTER_LABELS, formatTimeRange } from '../utils/format.js'

export default function SectionPage() {
  const { sectionId } = useParams()
  const navigate = useNavigate()
  const [section, setSection] = useState(null)
  const [loadError, setLoadError] = useState('')
  const [actionError, setActionError] = useState('')
  const [layouts, setLayouts] = useState([])

  const loadSection = useCallback(() => {
    getSection(sectionId)
      .then(setSection)
      .catch((error) => (error.status === 401 ? navigate('/', { replace: true }) : setLoadError(error.message)))
  }, [sectionId, navigate])

  useEffect(() => {
    loadSection()
  }, [loadSection])

  // FR-015, FR-021: every professor's layouts, for the "Room layout" lists of the sessions
  useEffect(() => {
    searchLayouts('')
      .then(setLayouts)
      .catch((error) => setActionError(error.message))
  }, [])

  async function handleRemove(session) {
    const when = `${DAY_LABELS[session.dayOfWeek]} ${formatTimeRange(session.startTime, session.endTime)}`
    if (!window.confirm(`Remove the ${when} session?`)) return
    setActionError('')
    try {
      await removeSession(section.sectionId, session.sessionId)
      loadSection()
    } catch (error) {
      setActionError(error.message)
    }
  }

  async function handleArchive() {
    const name = `${section.subjectCode} ${section.sectionCode}`
    if (!window.confirm(`Archive ${name}? It becomes read-only and leaves your dashboard. This can't be undone.`)) return
    setActionError('')
    try {
      setSection(await archiveSection(section.sectionId))
    } catch (error) {
      setActionError(error.message)
    }
  }

  return (
    <main className="page">
      <PageHeader />
      <Link className="back-link" to="/professor">
        ← Back to dashboard
      </Link>
      {loadError && <p className="error-text">{loadError}</p>}
      {!loadError && !section && <p className="muted">Loading…</p>}

      {section && (
        <>
          <section className="card">
            <div className="card-title-row">
              <h2>
                {section.subjectCode} {section.sectionCode}
              </h2>
              {!section.archived && (
                <Link className="button-secondary" to={`/professor/sections/${section.sectionId}/edit`}>
                  Edit
                </Link>
              )}
            </div>
            <p>{section.subjectTitle}</p>
            <p className="hint-text">
              {SEMESTER_LABELS[section.semester]} {section.schoolYear}
            </p>
            {section.archived && (
              <p className="notice-banner">This section is archived: it is read-only and hidden from your dashboard.</p>
            )}
          </section>

          <section className="card">
            <h2>Weekly sessions</h2>
            <SessionList
              sectionId={section.sectionId}
              sessions={section.sessions}
              layouts={layouts}
              canChange={!section.archived}
              onRemove={handleRemove}
              onLayoutChosen={loadSection}
            />
            {actionError && <p className="error-text" role="alert">{actionError}</p>}
            {!section.archived && <SessionForm sectionId={section.sectionId} layouts={layouts} onAdded={loadSection} />}
          </section>

          {!section.archived && (
            <section className="card">
              <h2>Archive this section</h2>
              <p className="hint-text">
                When the term is over, archive the section. It becomes read-only and leaves your dashboard. This
                can't be undone.
              </p>
              <button type="button" className="button-secondary" onClick={handleArchive}>
                Archive section
              </button>
            </section>
          )}
        </>
      )}
    </main>
  )
}
