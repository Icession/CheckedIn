// useLayoutDraft — the layout being edited (see layoutDraft.js), its numbering settings, and undo.
// Every change that can be undone calls rememberForUndo() first; Undo brings back the whole earlier draft.
// - A resize renumbers the seats; quick resizes in a row (arrow clicks, typing then fixing) share one undo step.
// - "Start at" (in the settings) is changed only by the professor; typing it (or pressing Enter in it, or
//   leaving the box) also sets that type's "Next", so retyping the same number restarts from it.
import { useLayoutEffect, useRef, useState } from 'react'
import useUndoSteps from './useUndoSteps.js'
import { resizeDraft, shownCellsOf, startDraft, withShownCells } from '../utils/layoutDraft.js'
import { cellTypeOfStartField, defaultNumberingSettings } from '../utils/seatNumbers.js'

// Resizes less than this far apart, with no other change between them, are undone together
const RESIZES_GROUPED_WITHIN_MS = 1500

export default function useLayoutDraft(layout) {
  const [numbering, setNumbering] = useState(() => defaultNumberingSettings(layout.roomType, layout.cells))
  const [draft, setDraft] = useState(() => startDraft(layout, defaultNumberingSettings(layout.roomType, layout.cells)))
  const undoSteps = useUndoSteps()
  // The newest draft and settings, for a resize that runs a moment after typing stops (a timer started
  // earlier would otherwise see the values from when it was started)
  const latest = useRef({ draft, numbering })
  useLayoutEffect(() => {
    latest.current = { draft, numbering }
  })
  // Counts the undo steps made, so a resize can tell whether anything else happened since the last resize
  const stepsMade = useRef(0)
  const lastResize = useRef({ at: 0, stepsMade: -1 })

  function rememberForUndo() {
    undoSteps.remember(draft)
    stepsMade.current++
  }

  // Returns true when there was something to undo
  function undo() {
    const draftBefore = undoSteps.takeLast()
    if (!draftBefore) return false
    setDraft(draftBefore)
    stepsMade.current++ // the next resize starts a new undo step
    return true
  }

  // Typing in the room details; a typed size is applied by applySize()
  function changeDetail(fieldName, value) {
    setDraft({ ...draft, [fieldName]: value })
  }

  /**
   * Applies the typed size (with typedChanges, e.g. { columnCount: '9' }, for a value typed just now):
   * resize and renumber. Only valid 1–20 sizes are applied. Returns the message to show ('' if none).
   */
  function applySize(typedChanges = {}) {
    const before = latest.current.draft
    const { draft: resized, message } = resizeDraft({ ...before, ...typedChanges }, latest.current.numbering)
    if (resized.gridSize === before.gridSize) return ''
    const now = Date.now()
    const continuesQuickResizes = now - lastResize.current.at < RESIZES_GROUPED_WITHIN_MS && lastResize.current.stepsMade === stepsMade.current
    if (!continuesQuickResizes) {
      // Undo brings back the size that was in use, not a size typed just now
      undoSteps.remember({ ...before, rowCount: String(before.gridSize.rows), columnCount: String(before.gridSize.columns) })
      stepsMade.current++
    }
    lastResize.current = { at: now, stepsMade: stepsMade.current }
    setDraft(resized)
    return message
  }

  // The drawing changed the shown cells (may be called many times during one drag)
  function changeShownCells(shownCells) {
    setDraft((current) => withShownCells(current, shownCells))
  }

  function setNextNumbers(nextNumbers) {
    setDraft((current) => ({ ...current, nextNumbers }))
  }

  function changeNumbering(fieldName, value) {
    setNumbering({ ...numbering, [fieldName]: value })
    const cellType = cellTypeOfStartField(fieldName)
    const start = Number(value)
    if (cellType && String(value).trim() !== '' && Number.isInteger(start) && start >= 0) {
      setNextNumbers({ ...draft.nextNumbers, [cellType]: start }) // changing Start at sets Next to the same value
    }
  }

  // Restore button: the work kept in this browser (see useDraftBackup), as one undo step
  function restoreFrom(backup) {
    rememberForUndo()
    setDraft(backup.draft)
    setNumbering({ ...numbering, ...backup.numbering })
  }

  // After saving: the saved layout, keeping the Next counters
  function resetTo(savedLayout) {
    setDraft({ ...startDraft(savedLayout, numbering), nextNumbers: draft.nextNumbers })
  }

  return {
    draft,
    shownCells: shownCellsOf(draft),
    numbering,
    nextNumbers: draft.nextNumbers,
    canUndo: undoSteps.canUndo,
    rememberForUndo,
    undo,
    changeDetail,
    applySize,
    changeShownCells,
    setNextNumbers,
    changeNumbering,
    restoreFrom,
    resetTo,
  }
}
