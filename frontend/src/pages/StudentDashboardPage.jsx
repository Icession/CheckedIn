// StudentDashboardPage — the student's home page at /student (FR-008).
// Empty for now: Phase 9 adds the student's classes, attendance and absence hours (FR-059 – FR-063).
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getStudentDashboard } from '../api/studentApi.js'
import PageHeader from '../components/PageHeader.jsx'

// Signed out (401), not a student (403), or sign-in not finished (400, e.g. no student ID yet):
// the sign-in page sends everyone to the right place. Any other error is shown, so the page
// can't bounce back and forth.
const SEND_TO_SIGN_IN = [400, 401, 403]

export default function StudentDashboardPage() {
  const navigate = useNavigate()
  const [dashboard, setDashboard] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    getStudentDashboard()
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
      <section className="card">
        <h2>Welcome, {dashboard.firstName}!</h2>
        <p>
          Student ID: <strong>{dashboard.studentNumber}</strong>
        </p>
        <p className="muted">Your classes and attendance will appear here.</p>
      </section>
    </main>
  )
}
