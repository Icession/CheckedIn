// RestoreDraftBar — offers work that was kept in this browser after a refresh or a closed tab
// (see useDraftBackup): "You have unsaved changes from 11:40 AM. Restore / Discard."
import { formatTimeOfDay } from '../utils/format.js'

export default function RestoreDraftBar({ savedAt, onRestore, onDiscard }) {
  return (
    <div className="restore-bar" role="status">
      <span>You have unsaved changes from {formatTimeOfDay(savedAt)}.</span>
      <button type="button" className="button-primary" onClick={onRestore}>
        Restore
      </button>
      <button type="button" className="button-secondary" onClick={onDiscard}>
        Discard
      </button>
    </div>
  )
}
