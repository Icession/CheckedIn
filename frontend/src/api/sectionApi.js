// sectionApi.js — calls for the professor side (Feature 2: sections and weekly sessions).
import { apiDelete, apiGet, apiPost, apiPut } from './apiClient.js'

/** The professor's dashboard: { firstName, lastName, today, todaySessions, sections } (FR-016). */
export function getProfessorDashboard() {
  return apiGet('/api/professor/dashboard')
}

/** One section with its weekly sessions (FR-010 – FR-012). */
export function getSection(sectionId) {
  return apiGet(`/api/professor/sections/${sectionId}`)
}

/** Creates a section: { subjectCode, subjectTitle, sectionCode, semester, schoolYear } (FR-010). */
export function createSection(section) {
  return apiPost('/api/professor/sections', section)
}

/** Edits a section (FR-011). */
export function updateSection(sectionId, section) {
  return apiPut(`/api/professor/sections/${sectionId}`, section)
}

/** Archives a section: it becomes read-only and leaves the dashboard; there is no undo (FR-011). */
export function archiveSection(sectionId) {
  return apiPost(`/api/professor/sections/${sectionId}/archive`, {})
}

/**
 * Adds a weekly session (FR-012, FR-013):
 * { dayOfWeek, startTime, endTime, sessionType, deliveryMode, roomName }.
 */
export function addSession(sectionId, session) {
  return apiPost(`/api/professor/sections/${sectionId}/sessions`, session)
}

/** Removes a weekly session from a section. */
export function removeSession(sectionId, sessionId) {
  return apiDelete(`/api/professor/sections/${sectionId}/sessions/${sessionId}`)
}
