// Unit tests for draftBackup.js (keeping unsaved work in the browser) and the time shown for it.
// Node's built-in test runner (no extra library). Run them with: npm test
import assert from 'node:assert/strict'
import { describe, test } from 'node:test'
import { backupKey, readBackup, removeBackup, writeBackup } from './draftBackup.js'
import { formatTimeOfDay } from './format.js'

// A pretend localStorage, and one that refuses everything (like a blocked or full localStorage)
function pretendStorage() {
  const items = new Map()
  return { getItem: (key) => items.get(key) ?? null, setItem: (key, value) => items.set(key, value), removeItem: (key) => items.delete(key) }
}
const refusingStorage = {
  getItem() { throw new Error('blocked') },
  setItem() { throw new Error('full') },
  removeItem() { throw new Error('blocked') },
}

describe('keeping unsaved work in the browser', () => {
  test('a copy is kept with the time and read back', () => {
    const storage = pretendStorage()
    const key = backupKey('layout-12')

    writeBackup(storage, key, { roomName: 'GLE201' }, new Date('2026-11-19T03:40:00Z'))

    assert.equal(key, 'checkedin.unsavedWork.layout-12')
    assert.deepEqual(readBackup(storage, key), { version: 1, savedAt: '2026-11-19T03:40:00.000Z', data: { roomName: 'GLE201' } })
  })

  test('after removing it there is no copy', () => {
    const storage = pretendStorage()
    writeBackup(storage, 'k', { a: 1 })

    removeBackup(storage, 'k')

    assert.equal(readBackup(storage, 'k'), null)
  })

  test('a blocked or full localStorage never breaks the page: there is just no copy', () => {
    assert.equal(writeBackup(refusingStorage, 'k', { a: 1 }), false)
    assert.equal(readBackup(refusingStorage, 'k'), null)
    assert.doesNotThrow(() => removeBackup(refusingStorage, 'k'))
    assert.equal(readBackup(null, 'k'), null) // localStorage not available at all
  })

  test('a broken copy, or one from an older version of the app, is ignored', () => {
    const storage = pretendStorage()
    storage.setItem('broken', '{not json')
    storage.setItem('old', JSON.stringify({ version: 0, savedAt: '2026-11-19T03:40:00Z', data: {} }))

    assert.equal(readBackup(storage, 'broken'), null)
    assert.equal(readBackup(storage, 'old'), null)
  })
})

describe('the time shown for kept work (Philippine time)', () => {
  // Spaces are compared loosely: browsers put a narrow space before "AM"
  const plain = (text) => text.replace(/\s/g, ' ')

  test('a time today shows only the time', () => {
    assert.equal(plain(formatTimeOfDay('2026-11-19T03:40:00Z', new Date('2026-11-19T08:00:00Z'))), '11:40 AM')
  })

  test('a time on another day also shows the day', () => {
    assert.equal(plain(formatTimeOfDay('2026-11-18T03:40:00Z', new Date('2026-11-19T08:00:00Z'))), 'Nov 18, 11:40 AM')
  })
})
