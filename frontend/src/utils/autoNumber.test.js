// Unit tests for autoNumber.js: Auto-number (FR-019) — all cells from Start at, or a selected block from Next.
// Node's built-in test runner (no extra library). Run them with: npm test
import assert from 'node:assert/strict'
import { describe, test } from 'node:test'
import {
  applyAutoNumbers, autoNumberProblem, findNumberingClash, nextAfterPlan, planAutoNumbers,
} from './autoNumber.js'
import { defaultNumberingSettings, firstNumbers } from './seatNumbers.js'
import { grid, gridOnScreen, keysOnScreen, labelsOnScreen, withLabelsOnScreen } from './testGrids.js'

// Numbering settings as the panel holds them; only the changes are given
function settings(changes = {}) {
  return { ...defaultNumberingSettings('COMPUTER_LAB'), ...changes }
}

// Presses Auto-number like the panel does; returns the new cells and the new Next counters.
// The grids below are pictures of the screen: top row first = the back row, the bottom row is the front row.
function autoNumber(cells, numbering, { selectedKeys = null, nextNumbers = firstNumbers(numbering) } = {}) {
  const plan = planAutoNumbers(cells, numbering, { selectedKeys, nextNumbers })
  return { cells: applyAutoNumbers(cells, plan), nextNumbers: nextAfterPlan(nextNumbers, plan) }
}

const LAB_6_X_6 = grid('WWWWWW', 'WWWWWW', 'WWWWWW', 'WWWWWW', 'WWWWWW', 'WWWWWW')

describe('Auto-number with nothing selected: a full renumber from Start at', () => {
  test('from the front row, left side: the bottom row first, left to right, skipping tables, aisles and no-chair cells', () => {
    const { cells } = autoNumber(gridOnScreen('WWN', 'AAA', 'WTW'), settings({ workstationPrefix: 'PC-' }))

    assert.deepEqual(labelsOnScreen(cells), [['PC-3', 'PC-4', null], [null, null, null], ['PC-1', null, 'PC-2']])
  })

  test('pressing it twice gives the same labels (1–36 every time, not 37–72)', () => {
    const numbering = settings()
    const first = autoNumber(LAB_6_X_6, numbering)
    const second = autoNumber(first.cells, numbering, { nextNumbers: first.nextNumbers })

    assert.deepEqual(labelsOnScreen(second.cells), labelsOnScreen(first.cells))
    assert.equal(labelsOnScreen(second.cells)[0][5], '36') // the last one: the back row, on the screen's right
  })

  test('afterwards Next = last number + 1, and Start at is not changed', () => {
    const numbering = settings({ workstationStartAt: '21' })
    const before = { ...numbering }

    const { nextNumbers } = autoNumber(grid('W'.repeat(18)), numbering)

    assert.equal(nextNumbers.WORKSTATION, 39)
    assert.deepEqual(numbering, before) // Start at never changes by itself
  })

  test('snake: every other row turns back, and an aisle row does not break it', () => {
    const { cells } = autoNumber(gridOnScreen('WWW', 'WWW', 'AAA', 'WWW'), settings({ direction: 'SNAKE' }))

    assert.deepEqual(labelsOnScreen(cells), [['7', '8', '9'], ['6', '5', '4'], [null, null, null], ['1', '2', '3']])
  })

  test('"Workstations only" leaves the seats and their numbers alone', () => {
    const { cells } = autoNumber(withLabelsOnScreen(gridOnScreen('WWS'), [[null, null, 'S-9']]), settings({ workstationPrefix: 'PC-' }))

    assert.deepEqual(labelsOnScreen(cells), [['PC-1', 'PC-2', 'S-9']])
  })

  test('"Both": each type counts from its own Start at, with its own prefix', () => {
    const numbering = settings({ applyTo: 'BOTH', workstationPrefix: 'PC-', workstationStartAt: '21', seatStartAt: '1' })

    const { cells } = autoNumber(gridOnScreen('SWS', 'WWS'), numbering)

    assert.deepEqual(labelsOnScreen(cells), [['2', 'PC-23', '3'], ['PC-21', 'PC-22', '1']])
  })

  test('from the back row, right side: the top row first, right to left', () => {
    const { cells } = autoNumber(gridOnScreen('WW', 'WW'), settings({ startCorner: 'BACK_RIGHT' }))

    assert.deepEqual(labelsOnScreen(cells), [['2', '1'], ['4', '3']])
  })
})

describe('Auto-number with cells selected: the block continues from Next', () => {
  // A row of 6 workstations: a block on the screen's left (columns 1–3) and one on its right (columns 4–6)
  const ROW = gridOnScreen('WWWWWW')
  const LEFT = keysOnScreen(ROW, [0, 1, 2].map((column) => ({ row: 0, column })))
  const RIGHT = keysOnScreen(ROW, [3, 4, 5].map((column) => ({ row: 0, column })))

  test('a left block 1–3, then a right block 4–6', () => {
    const numbering = settings()
    const left = autoNumber(ROW, numbering, { selectedKeys: LEFT })
    const right = autoNumber(left.cells, numbering, { selectedKeys: RIGHT, nextNumbers: left.nextNumbers })

    assert.deepEqual(labelsOnScreen(left.cells), [['1', '2', '3', null, null, null]])
    assert.deepEqual(labelsOnScreen(right.cells), [['1', '2', '3', '4', '5', '6']])
    assert.equal(right.nextNumbers.WORKSTATION, 7)
  })

  test('a block starts from Next, not from Start at', () => {
    const { cells } = autoNumber(ROW, settings({ workstationStartAt: '1' }), {
      selectedKeys: RIGHT, nextNumbers: { WORKSTATION: 21, SEAT: 1 },
    })

    assert.deepEqual(labelsOnScreen(cells), [[null, null, null, '21', '22', '23']])
  })
})

describe('Auto-number refuses a label that would be used twice', () => {
  test('"Both" without prefixes would give seats and workstations the same numbers', () => {
    const cells = grid('WWW', 'SSS')

    assert.equal(findNumberingClash(cells, planAutoNumbers(cells, settings({ applyTo: 'BOTH' }))),
      '“1” would be used by both a seat and a workstation. Add a prefix to one of them, for example PC- for workstations.')
  })

  test('numbering seats 1, 2, 3 is refused when workstations already have 1, 2, 3', () => {
    const { cells } = autoNumber(gridOnScreen('SSS', 'WWW'), settings()) // the workstations in front: 1, 2, 3 from the left
    const numbering = settings({ applyTo: 'SEAT' })

    assert.match(autoNumberProblem(cells, numbering, planAutoNumbers(cells, numbering)),
      /^“1” is already used by the workstation in row 1, column 1, which keeps its number\./)
  })
})
