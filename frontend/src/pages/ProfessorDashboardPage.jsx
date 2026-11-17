// ProfessorDashboardPage — the professor's home page at /professor (FR-008, FR-016):
// today's classes in time order, and the professor's active sections (archived ones are hidden).
import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { getProfessorDashboard } from '../api/sectionApi.js'
import PageHeader from '../components/PageHeader.jsx'
import TodaySessionList from '../components/TodaySessionList.jsx'
import SectionList from '../components/SectionList.jsx'

// Signed out (401), not a professor (403), or sign-in not finished (400): the sign-in page
// sends everyone to the right place. Any other error is shown, so the page can't bounce back and forth.
const SEND_TO_SIGN_IN = [400, 401, 403]

export default function ProfessorDashboardPage() {
  const navigate = useNavigate()
  const [dashboard, setDashboard] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    getProfessorDashboard()
      .then(setDashboard)
      .catch((loadError) =>
        SEND_TO_SIGN_IN.includes(loadError.status) ? navigate('/', { replace: true }) : setError(loadError.message),
      )
  }, [navigate])

  if (!dashboard) {
    return (
      <main className="page page--center">
        {error ? <p className="error-text">{error}</p> : <p className="muted">Loading…</p>}
      </main>
    )
  }

  return (
    <main className="page">
      <PageHeader />
      <h2>Welcome, Prof. {dashboard.lastName}!</h2>

      <section className="card">
        <h2>Today's classes</h2>
        <TodaySessionList today={dashboard.today} sessions={dashboard.todaySessions} />
      </section>

      <section className="card">
        <div className="card-title-row">
          <h2>My sections</h2>
          <Link className="button-primary" to="/professor/sections/new">
            New section
          </Link>
        </div>
        <SectionList sections={dashboard.sections} />
      </section>
    </main>
  )
}
