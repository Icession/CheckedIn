// healthApi.js — calls for GET /api/health (Phase 0 setup check).
import { apiGet } from './apiClient.js'

/** Returns { status, database, time } from the backend. */
export function getHealth() {
  return apiGet('/api/health')
}
