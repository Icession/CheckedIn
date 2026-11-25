// Unit tests for seatLabels.js: checking seat and workstation labels (FR-019).
// Node's built-in test runner (no extra library). Run them with: npm test
import assert from 'node:assert/strict'
import { describe, test } from 'node:test'
import { describeCell, findDuplicateLabels, findSeatWithLabel, guessPrefix, isValidSeatLabel, normalizeSeatLabel } from './seatLabels.js'
import { grid, withLabels } from './testGrids.js'

describe('checking labels', () => {
  test('a typed number is cleaned like the backend does', () => {
    assert.equal(normalizeSeatLabel(' pc-01 '), 'PC-01')
    assert.equal(normalizeSeatLabel('   '), null)
  })

  test('only letters, digits and hyphens, at most 10 characters', () => {
    assert.equal(isValidSeatLabel('PC-1'), true)
    assert.equal(isValidSeatLabel('PC_1'), false)
    assert.equal(isValidSeatLabel('LAB-A-PC-001'), false)
  })

  test('a number used twice is found; empty numbers and non-seat cells are ignored', () => {
    const cells = withLabels(grid('WAW', 'WWW'), [['PC-1'], [null, 'PC-1']])

    assert.deepEqual([...findDuplicateLabels(cells)], ['PC-1'])
  })

  test('finds which other seat already has a number, but not the seat itself', () => {
    const cells = withLabels(grid('WW', 'WW'), [['PC-1', 'PC-2'], ['PC-3', 'PC-4']])

    assert.deepEqual(findSeatWithLabel(cells, 'PC-3', 0, 0), { row: 1, column: 0 })
    assert.equal(findSeatWithLabel(cells, 'PC-3', 1, 0), null)
  })

  test('a cell is described with rows from the front and columns from the screen’s left', () => {
    const cells = grid('WSSS', 'SSSS')

    assert.equal(describeCell(cells, { row: 0, column: 0 }), 'the workstation in row 1, column 4') // stored column 0 is on the screen's right
  })

  test('the prefix most labels of a type use', () => {
    const cells = withLabels(grid('WWWS'), [['WS-1', 'WS-2', 'X9', '4']])

    assert.equal(guessPrefix(cells, 'WORKSTATION'), 'WS-')
    assert.equal(guessPrefix(cells, 'SEAT'), '')
  })
})
