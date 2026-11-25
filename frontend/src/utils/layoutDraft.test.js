// Unit tests for layoutDraft.js: resizing renumbers in order, without duplicates, as one undoable change.
// Node's built-in test runner (no extra library). Run them with: npm test
import assert from 'node:assert/strict'
import { describe, test } from 'node:test'
import { resizeDraft, saveRequestOf, shownCellsOf, startDraft } from './layoutDraft.js'
import { findDuplicateLabels } from './seatLabels.js'
import { defaultNumberingSettings } from './seatNumbers.js'
import { grid, labelsOf, labelsOnScreen, turnAround, typesOnScreen } from './testGrids.js'

const SETTINGS = defaultNumberingSettings('LECTURE_ROOM')

// A lecture room draft drawn as seen on screen (top row first = the back row, the bottom row is the front),
// its seats numbered 1, 2, 3... from the front row, left side of the screen
function roomDraft(...rowsAsSeen) {
  const cellsAsSeen = grid(...rowsAsSeen)
  let number = 0
  for (const cellsInRow of [...cellsAsSeen].reverse()) {
    for (const cell of cellsInRow) {
      if (cell.cellType === 'SEAT') cell.seatLabel = String(++number)
    }
  }
  const layout = { roomName: 'GLE201', roomType: 'LECTURE_ROOM', rowCount: rowsAsSeen.length, columnCount: rowsAsSeen[0].length }
  return startDraft({ ...layout, cells: turnAround(cellsAsSeen) }, SETTINGS)
}

// Types a new size and applies it (Enter or leaving the box)
function resize(draft, rows, columns) {
  return resizeDraft({ ...draft, rowCount: String(rows), columnCount: String(columns) }, SETTINGS)
}

// The pictures below are the screen: top row first = the back row, the bottom row is the front row
describe('numbers follow the grid size', () => {
  test('10 -> 12 columns: the front row = 1–12 and the next seat row starts at 13', () => {
    const { draft } = resize(roomDraft('S'.repeat(10), 'S'.repeat(10)), 2, 12)
    const onScreen = labelsOnScreen(shownCellsOf(draft))

    assert.deepEqual(onScreen[1], ['1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12'])
    assert.equal(onScreen[0][0], '13') // the back row, on the screen's left
  })

  test('a new row appears at the back (the top), numbers keep the order, and Next = last number + 1', () => {
    const { draft, message } = resize(roomDraft('SAS', 'SSS'), 3, 3)

    assert.deepEqual(labelsOnScreen(shownCellsOf(draft)), [['6', null, '7'], ['4', null, '5'], ['1', '2', '3']])
    assert.equal(draft.nextNumbers.SEAT, 8)
    assert.equal(message, 'Numbers were redone from the front row, left side of the screen, same direction every row, because the room size changed. Press Undo to go back.')
  })

  test('the front row reads 1–4 from the screen’s left, which is the students’ right', () => {
    const { draft } = resizeDraft({ ...roomDraft('SSS'), columnCount: '4' }, SETTINGS)

    assert.deepEqual(labelsOnScreen(shownCellsOf(draft)), [['1', '2', '3', '4']])
    assert.deepEqual(labelsOf(shownCellsOf(draft)), [['4', '3', '2', '1']]) // stored order: the first stored column is on the screen's right
  })

  test('a table row is copied when a row is added', () => {
    const { draft } = resize(roomDraft('TTT', 'SSS'), 3, 3)

    assert.deepEqual(typesOnScreen(shownCellsOf(draft)), ['TTT', 'TTT', 'SSS'])
  })

  test('shrinking and growing again gives no duplicates; the cells that come back keep their type', () => {
    const start = roomDraft('TSSSS', 'TSSSS') // columns come and go on the screen's left
    const shrunk = resize(start, 2, 3).draft
    const grownAgain = resize(shrunk, 2, 5).draft

    assert.deepEqual(typesOnScreen(shownCellsOf(grownAgain)), ['TSSSS', 'TSSSS'])
    assert.deepEqual(labelsOnScreen(shownCellsOf(grownAgain)), [[null, '5', '6', '7', '8'], [null, '1', '2', '3', '4']])
    assert.equal(findDuplicateLabels(shownCellsOf(grownAgain)).size, 0)
  })

  test('Start at is never changed by a resize', () => {
    const before = { ...SETTINGS }

    resize(roomDraft('SS'), 1, 4)

    assert.deepEqual(SETTINGS, before)
  })
})

describe('applying the typed size', () => {
  test('a typed size is not applied until Enter or leaving the box (typing "12" never applies "1")', () => {
    const typing = { ...roomDraft('SSS', 'SSS'), rowCount: '1' } // the first key of "12"

    assert.equal(shownCellsOf(typing).length, 2)
  })

  test('an invalid or unchanged size changes nothing', () => {
    const draft = roomDraft('SSS')

    for (const [rows, columns] of [[21, 3], [1, 3]]) {
      const result = resize(draft, rows, columns)
      assert.deepEqual(result.draft.gridSize, draft.gridSize)
      assert.equal(result.draft.cells, draft.cells)
      assert.equal(result.message, '')
    }
  })
})

describe('one Undo restores both the size and the labels', () => {
  test('the draft from before the resize is left untouched, with its old size and old numbers', () => {
    const before = roomDraft('SSSS', 'SSSS')
    const beforeAsSaved = JSON.stringify(saveRequestOf(before))

    const { draft: after } = resize(before, 2, 2)

    assert.deepEqual(after.gridSize, { rows: 2, columns: 2 })
    assert.equal(JSON.stringify(saveRequestOf(before)), beforeAsSaved) // what Undo brings back
    assert.deepEqual(before.gridSize, { rows: 2, columns: 4 })
    assert.deepEqual(labelsOnScreen(shownCellsOf(before)), [['5', '6', '7', '8'], ['1', '2', '3', '4']])
  })
})

describe('saving', () => {
  test('only the shown part of the grid is saved', () => {
    const shrunk = resize(roomDraft('SSSS'), 1, 2).draft

    // saved in stored order: the first stored column is the one on the screen's right
    assert.deepEqual(saveRequestOf(shrunk).cells, [[{ cellType: 'SEAT', seatLabel: '2' }, { cellType: 'SEAT', seatLabel: '1' }]])
    assert.equal(saveRequestOf(shrunk).columnCount, 2)
  })
})
