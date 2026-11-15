// ProfessorDashboardPage — the professor's home page at /professor (FR-008).
// Empty for now: Phase 2 adds the professor's sections and today's sessions (FR-016).
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getProfessorDashboard } from '../api/sectionApi.js'
import SignOutButton from '../components/SignOutButton.jsx'

export default function ProfessorDashboardPage() {
  const navigate = useNavigate()
  const [dashboard, setDashboard] = useState(null)

  useEffect(() => {
    getProfessorDashboard()
      .then(setDashboard)
      // Signed out, not a professor (403), or sign-in not finished: the sign-in page sends
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
        <h2>Welcome, Prof. {dashboard.lastName}!</h2>
        <p className="muted">Your sections and today's classes will appear here.</p>
      </section>
    </main>
  )
}
