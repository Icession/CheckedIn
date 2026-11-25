// LayoutEditor — the layout builder for the layout's creator (FR-017 – FR-020, FR-023): the room details,
// the drawing (LayoutDrawing) and the bottom bar with Save, Undo and the one message spot. The layout being
// edited, its numbering settings and undo live in useLayoutDraft.
// - The room size updates live: 400 ms after typing stops, at once with the ▲▼ arrows; it renumbers the seats.
// - Unsaved work is kept in this browser and offered again after a refresh (useDraftBackup).
// Save stays off while a number is used twice (FR-019); the backend checks everything again.
import { useEffect, useMemo, useRef, useState } from 'react'
import { saveLayout } from '../api/layoutApi.js'
import BuilderBottomBar from './BuilderBottomBar.jsx'
import LayoutDetailsFields from './LayoutDetailsFields.jsx'
import LayoutDrawing from './LayoutDrawing.jsx'
import RestoreDraftBar from './RestoreDraftBar.jsx'
import useDraftBackup from '../hooks/useDraftBackup.js'
import useLayoutDraft from '../hooks/useLayoutDraft.js'
import { saveRequestOf, startDraft, typedSizeIsValid } from '../utils/layoutDraft.js'
import { findDuplicateLabels } from '../utils/seatLabels.js'

const NO_MESSAGE = { text: '', isError: false }
const TYPING_PAUSE_MS = 400 // a typed size is applied this long after the last key, so "12" never shows 1 first
const SIZE_FIELDS = ['rowCount', 'columnCount']

export default function LayoutEditor({ layout, onSaved }) {
  const layoutDraft = useLayoutDraft(layout)
  const { draft, numbering } = layoutDraft
  const [message, setMessage] = useState(NO_MESSAGE) // the one message spot, in the bottom bar
  const [isSaving, setIsSaving] = useState(false)
  const sizeTimer = useRef(null)

  const saveRequest = saveRequestOf(draft)
  const savedVersion = JSON.stringify(saveRequestOf(startDraft(layout, numbering)))
  const hasChanges = JSON.stringify(saveRequest) !== savedVersion
  const duplicateLabels = findDuplicateLabels(layoutDraft.shownCells)
  const canSave = !isSaving && hasChanges && typedSizeIsValid(draft) && duplicateLabels.size === 0

  // A kept copy is offered only when it differs from the saved layout; a broken copy is ignored
  function matchesSaved(work) {
    try {
      return JSON.stringify(saveRequestOf(work.draft)) === savedVersion
    } catch {
      return true
    }
  }
  const work = useMemo(() => ({ draft, numbering }), [draft, numbering])
  const backup = useDraftBackup({ name: `layout-${layout.layoutId}`, work, hasChanges, matchesSaved })

  useEffect(() => () => clearTimeout(sizeTimer.current), []) // no resize after leaving the page

  function showResizeMessage(text) {
    if (text) setMessage({ text, isError: false })
  }

  function handleDetailChange(fieldName, value, isTyping) {
    layoutDraft.changeDetail(fieldName, value)
    setMessage(NO_MESSAGE)
    if (!SIZE_FIELDS.includes(fieldName)) return
    clearTimeout(sizeTimer.current)
    if (isTyping) {
      sizeTimer.current = setTimeout(() => showResizeMessage(layoutDraft.applySize()), TYPING_PAUSE_MS)
    } else {
      showResizeMessage(layoutDraft.applySize({ [fieldName]: value })) // the ▲▼ arrows: at once
    }
  }

  // Enter or leaving a size box: apply now instead of waiting
  function handleSizeApply() {
    clearTimeout(sizeTimer.current)
    showResizeMessage(layoutDraft.applySize())
  }

  function handleUndo() {
    if (layoutDraft.undo()) setMessage(NO_MESSAGE)
  }

  function handleRestore() {
    layoutDraft.restoreFrom(backup.restore())
    setMessage({ text: 'Your unsaved changes are back. Save the layout to keep them.', isError: false })
  }

  async function handleSave() {
    setMessage(NO_MESSAGE)
    setIsSaving(true)
    try {
      const saved = await saveLayout(layout.layoutId, saveRequest)
      onSaved(saved)
      layoutDraft.resetTo(saved)
      backup.clear()
      setMessage({ text: 'Layout saved.', isError: false })
    } catch (saveError) {
      // The backend explains what is wrong, e.g. "Number 12 is used for two seats..."
      setMessage({ text: saveError.message, isError: true })
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <>
      {backup.offer && <RestoreDraftBar savedAt={backup.offer.savedAt} onRestore={handleRestore} onDiscard={backup.discard} />}

      <section className="card narrow-card">
        <h2>Room details</h2>
        <div className="form">
          <LayoutDetailsFields values={draft} onChange={handleDetailChange} onSizeApply={handleSizeApply} />
          {!typedSizeIsValid(draft) && <p className="error-text">Rows and columns must each be from 1 to 20.</p>}
        </div>
      </section>

      <LayoutDrawing layoutDraft={layoutDraft} onMessage={setMessage} onUndo={handleUndo} />

      <BuilderBottomBar
        canSave={canSave}
        isSaving={isSaving}
        hasChanges={hasChanges}
        onSave={handleSave}
        canUndo={layoutDraft.canUndo}
        onUndo={handleUndo}
        message={message}
        duplicateLabels={duplicateLabels}
      />
    </>
  )
}
