// useDraftBackup — keeps unsaved work safe in this browser, and warns before it is lost.
// - While there are unsaved changes, every change is copied to localStorage under `name` (with the time);
//   when nothing is unsaved any more, the copy is removed (see utils/draftBackup.js).
// - On opening the page, a copy that differs from the saved version is offered (`offer` = { savedAt, data })
//   until the professor picks restore() or discard(). Until then no new copy is written, so the offered one
//   can't be overwritten by accident.
// - While there are unsaved changes, closing the tab or reloading asks first (beforeunload), and so does
//   following a link inside the app, e.g. "Back to room layouts".
// - clear() removes the copy after a successful save.
import { useEffect, useState } from 'react'
import { backupKey, readBackup, removeBackup, writeBackup } from '../utils/draftBackup.js'

const LEAVE_QUESTION = 'You have unsaved changes. Leave this page anyway? (A copy stays in this browser for later.)'

// localStorage itself can be blocked (e.g. some private windows); then there is no backup, but the page works
function browserStorage() {
  try {
    return window.localStorage
  } catch {
    return null
  }
}

/**
 * name: e.g. 'layout-12' or 'layout-new'. work: the data to keep (a new object only when it changes).
 * hasChanges: is there unsaved work? matchesSaved(data): true when a kept copy is the same as the saved version.
 */
export default function useDraftBackup({ name, work, hasChanges, matchesSaved }) {
  const key = backupKey(name)
  const [offer, setOffer] = useState(() => {
    const backup = readBackup(browserStorage(), key)
    return backup && !matchesSaved(backup.data) ? backup : null
  })

  // A copy on every change (or none when nothing is unsaved), but never over a copy that is being offered
  useEffect(() => {
    if (offer) return
    if (hasChanges) {
      writeBackup(browserStorage(), key, work)
    } else {
      removeBackup(browserStorage(), key)
    }
  }, [key, work, hasChanges, offer])

  // Closing the tab or reloading with unsaved changes: the browser asks first
  useEffect(() => {
    if (!hasChanges) return undefined
    function askFirst(event) {
      event.preventDefault()
      event.returnValue = '' // older browsers need this to show the question
    }
    window.addEventListener('beforeunload', askFirst)
    return () => window.removeEventListener('beforeunload', askFirst)
  }, [hasChanges])

  // Following a link inside the app with unsaved changes: ask first. This listens on the whole page before
  // React Router does (capture phase), and preventDefault() keeps the professor on the page.
  useEffect(() => {
    if (!hasChanges) return undefined
    function askBeforeLeaving(event) {
      const link = event.target.closest?.('a[href]')
      const opensElsewhere = link?.target === '_blank' || event.ctrlKey || event.metaKey || event.shiftKey || event.button !== 0
      if (!link || opensElsewhere) return
      if (!window.confirm(LEAVE_QUESTION)) {
        event.preventDefault()
      }
    }
    document.addEventListener('click', askBeforeLeaving, true)
    return () => document.removeEventListener('click', askBeforeLeaving, true)
  }, [hasChanges])

  return {
    offer,
    // Restore button: returns the kept data, and copies are written again from now on
    restore() {
      setOffer(null)
      return offer.data
    },
    discard() {
      removeBackup(browserStorage(), key)
      setOffer(null)
    },
    clear() {
      removeBackup(browserStorage(), key)
    },
  }
}
