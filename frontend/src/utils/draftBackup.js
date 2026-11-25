// draftBackup.js — a copy of unsaved work kept in this browser (localStorage), so a refresh or a closed tab
// doesn't lose it. A backup is { version, savedAt, data }, stored as JSON under a key like
// "checkedin.unsavedWork.layout-12" (or "...layout-new" for a new layout).
// localStorage can be full, switched off or blocked (some private windows): every access is wrapped in
// try/catch, and then there is simply no backup. The functions take the storage as a parameter, so the
// tests can use a pretend storage.

// Bump this when the shape of the saved data changes; backups made by an older version are then ignored
const BACKUP_VERSION = 1
const KEY_PREFIX = 'checkedin.unsavedWork.'

/** The storage key for one piece of work, e.g. backupKey('layout-12'). */
export function backupKey(name) {
  return KEY_PREFIX + name
}

/** The backup under this key, or null when there is none, it can't be read, or it is from an older version. */
export function readBackup(storage, key) {
  try {
    const text = storage?.getItem(key)
    if (!text) return null
    const backup = JSON.parse(text)
    return backup?.version === BACKUP_VERSION && typeof backup.savedAt === 'string' ? backup : null
  } catch {
    return null
  }
}

/** Keeps the data under this key, with the time. Returns false when the browser doesn't allow it. */
export function writeBackup(storage, key, data, now = new Date()) {
  try {
    storage.setItem(key, JSON.stringify({ version: BACKUP_VERSION, savedAt: now.toISOString(), data }))
    return true
  } catch {
    return false
  }
}

export function removeBackup(storage, key) {
  try {
    storage?.removeItem(key)
  } catch {
    // nothing to remove, or no access: either way there is no backup
  }
}
