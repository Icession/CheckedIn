// useCurrentUser — the signed-in user that RequireRole loaded from /api/auth/me, shared with every page inside
// it (for example the menu in PageHeader), so the backend is asked only once per visit (FR-008).
// setCurrentUser lets a page update it, e.g. the profile page after a name change, so the header follows.
import { createContext, useContext } from 'react'

export const CurrentUserContext = createContext({ currentUser: null, setCurrentUser: () => {} })

/** { currentUser, setCurrentUser }. currentUser is { email, firstName, lastName, role, ... }, or null outside RequireRole. */
export default function useCurrentUser() {
  return useContext(CurrentUserContext)
}
