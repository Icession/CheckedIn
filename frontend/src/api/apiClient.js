// apiClient.js — the one place that talks to the backend.
// Every feature calls these helpers instead of using fetch() directly,
// so error handling and the base URL live in a single file.

// Empty in development (the Vite proxy forwards /api to the backend).
// Set VITE_API_BASE_URL in .env when the backend runs somewhere else.
const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''

/**
 * Sends a GET request and returns the JSON body.
 * Throws an Error with a readable message if the request fails.
 */
export async function apiGet(path) {
  const response = await fetch(`${BASE_URL}${path}`, {
    credentials: 'include', // send the session cookie (used from Phase 2)
  })
  if (!response.ok) {
    throw new Error(`Request to ${path} failed with status ${response.status}`)
  }
  return response.json()
}
