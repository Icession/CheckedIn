// studentApi.js — calls for the student side (Feature 9: the student's attendance page).
import { apiGet } from './apiClient.js'

/** The student's dashboard: { firstName, lastName, studentNumber }. Phase 9 adds classes and attendance. */
export function getStudentDashboard() {
  return apiGet('/api/student/dashboard')
}
