// profileApi.js — the signed-in user's own profile (Increment 2 / SRS v1.4). There is no user ID in these
// calls: the backend always uses the person signed in on this session.
import { apiGet, apiPut } from './apiClient.js'

/** { email, firstName, lastName, role, studentNumber, createdOn } of the signed-in user. */
export function getProfile() {
  return apiGet('/api/profile')
}

/** Saves a new first and last name. Returns the updated profile; throws with the backend's message (400). */
export function updateProfile(firstName, lastName) {
  return apiPut('/api/profile', { firstName, lastName })
}
