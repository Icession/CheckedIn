// Unit tests for seatNumbers.js: the numbering settings and the numbering order (FR-019).
// Node's built-in test runner (no extra library). Run them with: npm test
import assert from 'node:assert/strict'
import { describe, test } from 'node:test'
import {
  buildSeatLabels, cellTypeOfStartField, cellsInNumberingOrder, defaultNumberingSettings, describeNextLabels,
  describeNumberingOrder, firstNumbers, formatSeatLabel, numberingSettingsProblem,
} from './seatNumbers.js'
import { fromScreenPosition } from './layoutView.js'
import { grid, withLabels } from './testGrids.js'

// Numbering settings as the panel holds them; only the changes are given
function settings(changes = {}) {
  return { ...defaultNumberingSettings('COMPUTER_LAB'), ...changes }
}

// The grid AS SEEN ON SCREEN (from the teacher's desk: the front row at the bottom), each numbered cell showing
// its place in the order (1, 2, 3...), '·' for cells that are not numbered (here the aisle)
function orderOnScreen(cells, order) {
  const place = new Map(cellsInNumberingOrder(cells, order).map(({ row, column }, index) => [`${row}-${column}`, index + 1]))
  return cells.map((cellsInRow, screenRow) => cellsInRow.map((_, screenColumn) => {
    const { row, column } = fromScreenPosition({ row: screenRow, column: screenColumn }, cells.length, cellsInRow.length)
    return place.get(`${row}-${column}`) ?? '·'
  }).join(' '))
}

// 3 rows x 4 columns with an aisle down the third stored column. Row 0 is the front row. On screen the room is
// turned around, so the front row is the bottom row and the aisle is the second column.
const ROOM = grid('SSAS', 'SSAS', 'SSAS')

describe('the numbering order: 4 starting corners x 2 directions, the aisle skipped', () => {
  const cases = [
    ['FRONT_LEFT', 'SAME', ['7 · 8 9', '4 · 5 6', '1 · 2 3']],
    ['FRONT_LEFT', 'SNAKE', ['7 · 8 9', '6 · 5 4', '1 · 2 3']],
    ['FRONT_RIGHT', 'SAME', ['9 · 8 7', '6 · 5 4', '3 · 2 1']],
    ['FRONT_RIGHT', 'SNAKE', ['9 · 8 7', '4 · 5 6', '3 · 2 1']],
    ['BACK_LEFT', 'SAME', ['1 · 2 3', '4 · 5 6', '7 · 8 9']],
    ['BACK_LEFT', 'SNAKE', ['1 · 2 3', '6 · 5 4', '7 · 8 9']],
    ['BACK_RIGHT', 'SAME', ['3 · 2 1', '6 · 5 4', '9 · 8 7']],
    ['BACK_RIGHT', 'SNAKE', ['3 · 2 1', '4 · 5 6', '9 · 8 7']],
  ]
  for (const [startCorner, direction, expectedOnScreen] of cases) {
    test(`${startCorner}, ${direction}`, () => {
      assert.deepEqual(orderOnScreen(ROOM, { startCorner, direction }), expectedOnScreen)
    })
  }

  test('a row without seats (all aisle) does not break the snake', () => {
    assert.deepEqual(orderOnScreen(grid('SS', 'AA', 'SS'), { startCorner: 'FRONT_LEFT', direction: 'SNAKE' }), ['4 3', '· ·', '1 2'])
  })

  test('only one type, or only selected cells, can be put in order', () => {
    assert.deepEqual(cellsInNumberingOrder(grid('WSW'), { cellType: 'SEAT' }), [{ row: 0, column: 1 }])
    assert.deepEqual(cellsInNumberingOrder(grid('SSS'), { selectedKeys: new Set(['0-2']) }), [{ row: 0, column: 2 }])
  })

  test('the order is described for the hints and messages', () => {
    assert.equal(describeNumberingOrder(settings()), 'from the front row, left side of the screen, same direction every row')
    assert.equal(describeNumberingOrder(settings({ startCorner: 'BACK_RIGHT', direction: 'SNAKE' })), 'from the back row, right side of the screen, snaking')
  })
})

describe('the settings', () => {
  test('the default: workstations in a lab, seats in a lecture room, Start at 1, front row left', () => {
    assert.equal(defaultNumberingSettings('COMPUTER_LAB').applyTo, 'WORKSTATION')
    assert.equal(defaultNumberingSettings('LECTURE_ROOM').applyTo, 'SEAT')
    assert.equal(defaultNumberingSettings('LECTURE_ROOM').seatStartAt, '1')
    assert.equal(defaultNumberingSettings('LECTURE_ROOM').startCorner, 'FRONT_LEFT')
    assert.equal(defaultNumberingSettings('LECTURE_ROOM').direction, 'SAME')
  })

  test('the prefix boxes start with the prefix the labels already use (prefixes are not saved)', () => {
    const cells = withLabels(grid('WWWS'), [['WS-1', 'WS-2', 'X9', '4']])

    assert.equal(defaultNumberingSettings('COMPUTER_LAB', cells).workstationPrefix, 'WS-')
    assert.equal(defaultNumberingSettings('COMPUTER_LAB', cells).seatPrefix, '')
  })

  test('a prefix goes in front of every number; labels are never padded with zeros', () => {
    assert.deepEqual(buildSeatLabels('PC-', 1, 3), ['PC-1', 'PC-2', 'PC-3'])
    assert.equal(formatSeatLabel('PC-', 120), 'PC-120')
  })

  test('bad settings get a plain message', () => {
    assert.equal(numberingSettingsProblem(settings({ workstationStartAt: '1.5' })), 'The workstation “Start at” must be a whole number, for example 1.')
    assert.match(numberingSettingsProblem(settings({ workstationPrefix: 'PC_' })), /^The workstation prefix can only have/)
    assert.equal(numberingSettingsProblem(settings()), '')
  })
})

describe('two numbers per type: Start at (set by the professor) and Next (the counter)', () => {
  test('Next starts at each type’s Start at', () => {
    assert.deepEqual(firstNumbers(settings({ workstationStartAt: '21', seatStartAt: '1' })), { WORKSTATION: 21, SEAT: 1 })
  })

  test('a Start at box belongs to one type (typing it also sets that type’s Next)', () => {
    assert.equal(cellTypeOfStartField('seatStartAt'), 'SEAT')
    assert.equal(cellTypeOfStartField('workstationStartAt'), 'WORKSTATION')
    assert.equal(cellTypeOfStartField('seatPrefix'), null)
  })

  test('the hint shows Next, with the prefix', () => {
    assert.equal(describeNextLabels(settings({ workstationPrefix: 'WS-' }), { WORKSTATION: 13, SEAT: 1 }), 'WS-13')
    assert.equal(describeNextLabels(settings({ applyTo: 'BOTH', workstationPrefix: 'WS-' }), { WORKSTATION: 13, SEAT: 4 }), 'workstation WS-13, seat 4')
  })
})
