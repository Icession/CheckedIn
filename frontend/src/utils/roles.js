// roles.js — the two roles (FR-004) and what each one may open (FR-008): its dashboard and its menu.
// Only pages that exist are in the menus.

/** Where each role lands after sign-in, and where a wrong-role visitor is sent back to. */
export const DASHBOARD_PATHS = { PROFESSOR: '/professor', STUDENT: '/student' }

/** The badge next to the user's name. */
export const ROLE_LABELS = { PROFESSOR: 'Professor', STUDENT: 'Student' }

/** The menu in the page header, per role. Profile is for both roles (Increment 2 / SRS v1.4). */
export const NAV_LINKS = {
  PROFESSOR: [
    { to: '/professor', label: 'Dashboard' },
    { to: '/professor/sections/new', label: 'New section' },
    { to: '/profile', label: 'Profile' },
  ],
  STUDENT: [
    { to: '/student', label: 'Dashboard' },
    { to: '/profile', label: 'Profile' },
  ],
}
