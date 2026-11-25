// Unit tests for layoutView.js: the room drawn from the teacher's desk (FR-023).
// Node's built-in test runner (no extra library). Run them with: npm test
import assert from 'node:assert/strict'
import { describe, test } from 'node:test'
import { fromScreenPosition, positionAsSeen, toScreenPosition } from './layoutView.js'

// A room of 3 rows x 4 columns; row 0 is the front row
const ROWS = 3
const COLUMNS = 4

describe('the room seen from the teacher’s desk: turned around fully (180°)', () => {
  test('the front row is drawn at the bottom, and the students’ left on the screen’s right', () => {
    assert.deepEqual(toScreenPosition({ row: 0, column: 0 }, ROWS, COLUMNS), { row: 2, column: 3 })
    assert.deepEqual(toScreenPosition({ row: 2, column: 3 }, ROWS, COLUMNS), { row: 0, column: 0 })
  })

  test('a screen position leads back to the same stored cell, for every cell', () => {
    for (let row = 0; row < ROWS; row++) {
      for (let column = 0; column < COLUMNS; column++) {
        const onScreen = toScreenPosition({ row, column }, ROWS, COLUMNS)
        assert.deepEqual(fromScreenPosition(onScreen, ROWS, COLUMNS), { row, column })
      }
    }
  })

  test('a row added at the back appears at the top of the screen', () => {
    assert.deepEqual(toScreenPosition({ row: 3, column: 0 }, ROWS + 1, COLUMNS), { row: 0, column: 3 })
  })

  test('positions as people read them: row 1 is the front row, columns from the screen’s left', () => {
    assert.deepEqual(positionAsSeen({ row: 0, column: 0 }, ROWS, COLUMNS), { rowNumber: 1, columnNumber: 4 })
    assert.deepEqual(positionAsSeen({ row: 0, column: 3 }, ROWS, COLUMNS), { rowNumber: 1, columnNumber: 1 })
  })
})
