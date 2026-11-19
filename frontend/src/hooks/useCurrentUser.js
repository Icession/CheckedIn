// useCurrentUser — the signed-in user that RequireRole loaded from /api/auth/me, shared with every page inside
// it (for example the menu in PageHeader), so the backend is asked only once per visit (FR-008).
import { createContext, useContext } from 'react'

export const CurrentUserContext = createContext(null)

/** The signed-in user ({ email, firstName, lastName, role, ... }), or null outside RequireRole. */
export default function useCurrentUser() {
  return useContext(CurrentUserContext)
}
