// sectionApi.js — calls for the professor side (Feature 2: sections and sessions).
import { apiGet } from './apiClient.js'

/** The professor's dashboard: { firstName, lastName }. Phase 2 adds today's sessions. */
export function getProfessorDashboard() {
  return apiGet('/api/professor/dashboard')
}
