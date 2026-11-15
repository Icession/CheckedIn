// authApi.js — calls for signing in (Feature 1: FR-001, FR-002).
import { apiGet, apiPost } from './apiClient.js'

/** Emails a 6-digit sign-in code. Returns { email, expiresInMinutes }. */
export function requestSignInCode(email) {
  return apiPost('/api/auth/code', { email })
}

/** Checks the code; on success the backend starts the session cookie. Returns { email }. */
export function verifySignInCode(email, code) {
  return apiPost('/api/auth/code/verify', { email, code })
}

/** Who is signed in: { email }. Throws (status 401) when nobody is. */
export function getCurrentUser() {
  return apiGet('/api/auth/me')
}
