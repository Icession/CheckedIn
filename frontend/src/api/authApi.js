// authApi.js — calls for signing in and creating the account (Feature 1: FR-001 – FR-004).
import { apiGet, apiPost, BASE_URL } from './apiClient.js'

// "Sign in with GitHub" is a normal link, not a fetch: the whole page goes to the backend,
// which sends it on to GitHub and back (Spring Security handles those steps).
export const GITHUB_SIGN_IN_URL = `${BASE_URL}/oauth2/authorization/github`

/** Emails a 6-digit sign-in code. Returns { email, expiresInMinutes }. */
export function requestSignInCode(email) {
  return apiPost('/api/auth/code', { email })
}

/**
 * Checks the code; on success the backend starts the session cookie.
 * Returns the current user: { email, firstName, lastName, role, needsName }.
 */
export function verifySignInCode(email, code) {
  return apiPost('/api/auth/code/verify', { email, code })
}

/** Who is signed in: { email, firstName, lastName, role, needsName }. Throws (status 401) when nobody is. */
export function getCurrentUser() {
  return apiGet('/api/auth/me')
}

/** First sign-in: saves the name, which creates the account (FR-003). Returns the current user. */
export function saveName(firstName, lastName) {
  return apiPost('/api/auth/name', { firstName, lastName })
}

/** A student's first sign-in: links their student ID once (FR-005). Returns the current user. */
export function linkStudentId(studentNumber) {
  return apiPost('/api/auth/link-student-id', { studentNumber })
}

/** Signs out of CheckedIn (FR-009): the backend ends the session and deletes the cookie. */
export function signOut() {
  return apiPost('/api/auth/sign-out', {})
}
