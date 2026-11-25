// LayoutListPage — all room layouts at /professor/layouts (FR-021): search by room name, open one,
// or draw a new one. Every professor sees every layout; only its creator can change it.
import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { searchLayouts } from '../api/layoutApi.js'
import LayoutList from '../components/LayoutList.jsx'
import PageHeader from '../components/PageHeader.jsx'

// Signed out (401) or not a professor (403): the sign-in page sends everyone to the right place
const SEND_TO_SIGN_IN = [401, 403]
const SEARCH_DELAY_MS = 300

export default function LayoutListPage() {
  const navigate = useNavigate()
  const [searchText, setSearchText] = useState('')
  const [layouts, setLayouts] = useState(null)
  const [error, setError] = useState('')

  // Searches shortly after the professor stops typing. An older, slower answer is ignored.
  useEffect(() => {
    let isOutdated = false
    const timer = setTimeout(() => {
      searchLayouts(searchText)
        .then((found) => {
          if (isOutdated) return
          setLayouts(found)
          setError('')
        })
        .catch((loadError) =>
          SEND_TO_SIGN_IN.includes(loadError.status) ? navigate('/', { replace: true }) : setError(loadError.message),
        )
    }, SEARCH_DELAY_MS)
    return () => {
      isOutdated = true
      clearTimeout(timer)
    }
  }, [searchText, navigate])

  return (
    <main className="page">
      <PageHeader />
      <Link className="back-link" to="/professor">
        ← Back to dashboard
      </Link>
      <section className="card">
        <div className="card-title-row">
          <h2>Room layouts</h2>
          <Link className="button-primary" to="/professor/layouts/new">
            New layout
          </Link>
        </div>
        <p className="hint-text">
          Draw each room or lab once. Every professor can find a layout and use it for their sessions; only the
          professor who drew it can change it.
        </p>
        <div className="form">
          <label htmlFor="layoutSearch">Search by room name</label>
          <input
            id="layoutSearch"
            type="search"
            placeholder="NGE207"
            value={searchText}
            onChange={(event) => setSearchText(event.target.value)}
          />
        </div>
        {error && <p className="error-text">{error}</p>}
        {!error && !layouts && <p className="muted">Loading…</p>}
        {layouts && <div className="list-spacing"><LayoutList layouts={layouts} /></div>}
      </section>
    </main>
  )
}
