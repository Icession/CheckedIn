// SetupCheckPage — shows whether the backend and the database are running (Phase 0 setup check).
// Open it at http://localhost:5173/setup.
import { useEffect, useState } from 'react'
import { getHealth } from '../api/healthApi.js'
import StatusPill from '../components/StatusPill.jsx'

export default function SetupCheckPage() {
  const [health, setHealth] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    getHealth()
      .then(setHealth)
      .catch(() => setError('Cannot reach the backend. Is Spring Boot running on port 8080?'))
  }, [])

  return (
    <main className="page page--center">
      <h1 className="brand">CheckedIn</h1>
      <p className="muted">Seat-map attendance for CIT-U classes</p>

      <section className="card">
        <h2>Setup check</h2>
        {error && <p className="error-text">{error}</p>}
        {!error && !health && <p className="muted">Checking…</p>}
        {health && (
          <div className="row">
            <StatusPill label="Backend" isUp={health.status === 'UP'} />
            <StatusPill label="Database" isUp={health.database === 'UP'} />
          </div>
        )}
      </section>
    </main>
  )
}
