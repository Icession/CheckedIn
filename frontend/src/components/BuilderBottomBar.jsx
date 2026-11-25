// BuilderBottomBar — the bar at the bottom of the layout builder that stays visible while scrolling:
// Save layout, "You have unsaved changes", Undo, and the one message spot of the builder. It shows one
// message at a time: the result of the last action, or else a warning about numbers used twice (FR-019).
export default function BuilderBottomBar({ canSave, isSaving, hasChanges, onSave, canUndo, onUndo, message, duplicateLabels }) {
  const duplicateWarning = duplicateLabels.size > 0
    ? `${[...duplicateLabels].join(', ')} ${duplicateLabels.size === 1 ? 'is' : 'are'} used by more than one seat (shown in red). Give each its own label before saving.`
    : ''
  const shown = message.text ? message : duplicateWarning ? { text: duplicateWarning, isError: true } : null

  return (
    <div className="builder-bottom-bar">
      <button type="button" className="button-primary" onClick={onSave} disabled={!canSave}>
        {isSaving ? 'Saving…' : 'Save layout'}
      </button>
      {hasChanges && <span className="hint-text">You have unsaved changes.</span>}
      <button type="button" className="button-link" onClick={onUndo} disabled={!canUndo}>
        Undo
      </button>
      {shown && (
        <p className={`${shown.isError ? 'error-text' : 'notice-text'} bottom-bar-message`} role={shown.isError ? 'alert' : 'status'}>
          {shown.text}
        </p>
      )}
    </div>
  )
}
