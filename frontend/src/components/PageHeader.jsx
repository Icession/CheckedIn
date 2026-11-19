// PageHeader — the top of every signed-in page (FR-008): the CheckedIn name, a menu with only the pages of
// the user's role, the user's name with a role badge, and the sign-out button. The user comes from
// RequireRole (useCurrentUser). The current page is highlighted: NavLink marks its link "active".
import { NavLink } from 'react-router-dom'
import SignOutButton from './SignOutButton.jsx'
import useCurrentUser from '../hooks/useCurrentUser.js'
import { NAV_LINKS, ROLE_LABELS } from '../utils/roles.js'

export default function PageHeader() {
  const currentUser = useCurrentUser()

  return (
    <header className="dashboard-header">
      <h1 className="brand">CheckedIn</h1>
      {currentUser && (
        <nav className="main-nav" aria-label="Main menu">
          {NAV_LINKS[currentUser.role].map((link) => (
            // "end": a link is highlighted only on its own page, so Dashboard is not lit on deeper pages
            <NavLink key={link.to} to={link.to} end className="nav-link">
              {link.label}
            </NavLink>
          ))}
        </nav>
      )}
      <div className="header-user">
        {currentUser && (
          <p className="user-name">
            {currentUser.firstName} {currentUser.lastName}{' '}
            <span className="role-badge">{ROLE_LABELS[currentUser.role]}</span>
          </p>
        )}
        <SignOutButton />
      </div>
    </header>
  )
}
