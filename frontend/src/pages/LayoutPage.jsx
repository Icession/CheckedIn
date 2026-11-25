// LayoutPage — one room layout at /professor/layouts/:layoutId. Its creator gets the layout builder
// (LayoutEditor); other professors see it read-only and can use it for their sessions (FR-021).
import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { getLayout } from '../api/layoutApi.js'
import LayoutEditor from '../components/LayoutEditor.jsx'
import LayoutView from '../components/LayoutView.jsx'
import PageHeader from '../components/PageHeader.jsx'

// Signed out (401) or not a professor (403): the sign-in page sends everyone to the right place
const SEND_TO_SIGN_IN = [401, 403]

export default function LayoutPage() {
  const { layoutId } = useParams()
  const navigate = useNavigate()
  const [layout, setLayout] = useState(null)
  const [loadError, setLoadError] = useState('')

  useEffect(() => {
    getLayout(layoutId)
      .then(setLayout)
      .catch((error) => (SEND_TO_SIGN_IN.includes(error.status) ? navigate('/', { replace: true }) : setLoadError(error.message)))
  }, [layoutId, navigate])

  return (
    <main className="page">
      <PageHeader />
      <Link className="back-link" to="/professor/layouts">
        ← Back to room layouts
      </Link>
      {loadError && <p className="error-text">{loadError}</p>}
      {!loadError && !layout && <p className="muted">Loading…</p>}
      {layout && (layout.canEdit ? <LayoutEditor layout={layout} onSaved={setLayout} /> : <LayoutView layout={layout} />)}
    </main>
  )
}
