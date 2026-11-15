// StudentDashboardPage — the student's home page at /student (FR-008).
// Empty for now: Phase 9 adds the student's classes, attendance and absence hours (FR-059 – FR-063).
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getStudentDashboard } from '../api/studentApi.js'
import SignOutButton from '../components/SignOutButton.jsx'

export default function StudentDashboardPage() {
  const navigate = useNavigate()
  const [dashboard, setDashboard] = useState(null)

  useEffect(() => {
    getStudentDashboard()
      .then(setDashboard)
      // Signed out, not a student (403), or sign-in not finished: the sign-in page sends
      // everyone to the right place
      .catch(() => navigate('/', { replace: true }))
  }, [navigate])

  if (!dashboard) {
    return (
      <main className="page page--center">
        <p className="muted">Loading…</p>
      </main>
    )
  }

  return (
    <main className="page">
      <header className="dashboard-header">
        <h1 className="brand">CheckedIn</h1>
        <SignOutButton />
      </header>
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
