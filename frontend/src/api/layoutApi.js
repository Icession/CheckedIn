// layoutApi.js — calls for room and laboratory layouts (Feature 3: FR-017 – FR-021).
import { apiGet, apiPost, apiPut } from './apiClient.js'

/** Every professor's layouts whose room name contains the text (all of them when it is empty) (FR-021). */
export function searchLayouts(searchText) {
  return apiGet(`/api/professor/layouts?search=${encodeURIComponent(searchText)}`)
}

/** One layout with its whole grid: cells[row][column] = { seatId, cellType, seatLabel }. */
export function getLayout(layoutId) {
  return apiGet(`/api/professor/layouts/${layoutId}`)
}

/** Creates a layout: { roomName, roomType, rowCount, columnCount } (FR-017). */
export function createLayout(layout) {
  return apiPost('/api/professor/layouts', layout)
}

/** Saves the edited room details and grid; only the layout's creator may do this (FR-017 – FR-021). */
export function saveLayout(layoutId, layout) {
  return apiPut(`/api/professor/layouts/${layoutId}`, layout)
}
