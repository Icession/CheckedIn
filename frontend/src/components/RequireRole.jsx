// RequireRole — the frontend route guard (FR-008): only the given role may open the pages inside it.
// - Not signed in, or sign-in not finished (no name or student ID yet) -> the sign-in page (/)
// - Signed in with the other role -> their own dashboard
// - While asking the backend who is signed in: "Loading…"
// It keeps people out of the wrong screens. The real protection stays in the backend, which answers 401 or
// 403 to every request from the wrong person (SecurityConfig).
import { useEffect, useState } from 'react'
import { Navigate, Outlet } from 'react-router-dom'
import { getCurrentUser } from '../api/authApi.js'
import { CurrentUserContext } from '../hooks/useCurrentUser.js'
import { DASHBOARD_PATHS } from '../utils/roles.js'

export default function RequireRole({ role }) {
  const [isChecking, setIsChecking] = useState(true)
  const [currentUser, setCurrentUser] = useState(null)

  // Asked once when the first page of this role opens; moving between its pages keeps the answer
  useEffect(() => {
    getCurrentUser()
      .then(setCurrentUser)
      .catch(() => setCurrentUser(null)) // 401: nobody is signed in
      .finally(() => setIsChecking(false))
  }, [])

  if (isChecking) {
    return (
      <main className="page">
        <p className="muted">Loading…</p>
      </main>
    )
  }
  const signInIsFinished = currentUser !== null && !currentUser.needsName && !currentUser.needsStudentId
  if (!signInIsFinished) {
    return <Navigate to="/" replace />
  }
  if (currentUser.role !== role) {
    return <Navigate to={DASHBOARD_PATHS[currentUser.role]} replace />
  }
  return (
    <CurrentUserContext.Provider value={currentUser}>
      <Outlet />
    </CurrentUserContext.Provider>
  )
}
