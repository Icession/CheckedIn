// apiClient.js — the one place that talks to the backend.
// Every feature calls these helpers instead of using fetch() directly,
// so error handling and the base URL live in a single file.

// Empty in development (the Vite proxy forwards /api to the backend).
// Set VITE_API_BASE_URL in .env when the backend runs somewhere else.
export const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''

/** Sends a GET request and returns the JSON body. */
export function apiGet(path) {
  return sendRequest(path, { method: 'GET' })
}

/** Sends a POST request with a JSON body and returns the JSON answer. */
export function apiPost(path, body) {
  return sendRequest(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
}

/**
 * Sends the request and returns the JSON answer.
 * When something goes wrong it throws an Error whose message can be shown to the user as-is.
 */
async function sendRequest(path, options) {
  let response
  try {
    response = await fetch(`${BASE_URL}${path}`, {
      ...options,
      credentials: 'include', // send the session cookie
    })
  } catch {
    throw new Error('Cannot reach CheckedIn. Check your internet connection and try again.')
  }

  if (!response.ok) {
    // The backend explains every error as { "message": "..." } (see GlobalExceptionHandler)
    const errorBody = await response.json().catch(() => null)
    const error = new Error(errorBody?.message ?? `Something went wrong (error ${response.status}). Please try again.`)
    error.status = response.status
    throw error
  }
  return response.json()
}
