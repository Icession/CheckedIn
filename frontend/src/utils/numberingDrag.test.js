// Unit tests for numberingDrag.js: the Number tool as a renumbering brush, starting from Next (FR-019).
// Node's built-in test runner (no extra library). Run them with: npm test
import assert from 'node:assert/strict'
import { describe, test } from 'node:test'
import { describeLostLabels, numberCellInDrag, startNumberingDrag } from './numberingDrag.js'
import { defaultNumberingSettings, describeNextLabels, firstNumbers } from './seatNumbers.js'
import { grid, labelsOf, withLabels } from './testGrids.js'

function settings(changes = {}) {
  return { ...defaultNumberingSettings('LECTURE_ROOM'), ...changes }
}

// Drags the Number tool across the given cells, in that order, starting from Next; returns the finished drag
function drag(cells, numbering, touchedCells, nextNumbers = firstNumbers(numbering)) {
  return touchedCells.reduce(
    (current, [row, column]) => numberCellInDrag(current, row, column, numbering),
    startNumberingDrag(cells, nextNumbers),
  )
}

const ROW_1 = [0, 1, 2, 3].map((column) => [0, column])
const ROW_2 = [0, 1, 2, 3].map((column) => [1, column])

describe('numbering by dragging', () => {
  test('cells are numbered in the order they are touched, starting from Next', () => {
    const result = drag(grid('SSS'), settings({ seatPrefix: 'S-' }), [[0, 2], [0, 0], [0, 1]], { WORKSTATION: 1, SEAT: 5 })

    assert.deepEqual(labelsOf(result.cells), [['S-6', 'S-7', 'S-5']])
  })

  test('each cell is numbered at most once per drag: going back over a cell does nothing', () => {
    const result = drag(grid('SSS'), settings(), [[0, 0], [0, 1], [0, 0], [0, 1], [0, 2], [0, 1]])

    assert.deepEqual(labelsOf(result.cells), [['1', '2', '3']])
  })

  test('tables, aisles, no-chair cells and the type that "Apply to" leaves out are skipped', () => {
    const result = drag(grid('STWANS'), settings(), [[0, 0], [0, 1], [0, 2], [0, 3], [0, 4], [0, 5]])

    assert.deepEqual(labelsOf(result.cells), [['1', null, null, null, null, '2']])
  })
})

describe('Next after a drag', () => {
  test('is the last number used in the drag + 1, and the next drag continues from it', () => {
    const numbering = settings()
    const first = drag(grid('SSSS', 'SSSS'), numbering, ROW_1)
    const second = drag(first.cells, numbering, ROW_2, first.nextNumbers)

    assert.equal(first.nextNumbers.SEAT, 5)
    assert.equal(describeNextLabels(numbering, first.nextNumbers), '5') // the hint shows Next
    assert.deepEqual(labelsOf(second.cells), [['1', '2', '3', '4'], ['5', '6', '7', '8']])
    assert.equal(second.nextNumbers.SEAT, 9)
  })

  test('is not "the highest number in the room + 1"', () => {
    const room = withLabels(grid('SSSS', 'SSSS'), [[null, null, null, null], ['50', '51', '52', '53']])

    const result = drag(room, settings(), ROW_1)

    assert.equal(result.nextNumbers.SEAT, 5)
  })

  test('Start at is never changed by a drag; the other type keeps its Next', () => {
    const numbering = settings({ workstationStartAt: '21' })
    const before = { ...numbering }

    const result = drag(grid('SSW'), numbering, [[0, 0], [0, 1], [0, 2]], { WORKSTATION: 21, SEAT: 1 })

    assert.deepEqual(numbering, before)
    assert.deepEqual(result.nextNumbers, { WORKSTATION: 21, SEAT: 3 })
  })
})

describe('the renumbering brush: untouched cells of the same type give up their labels', () => {
  test('an untouched seat that has the label loses it, and the drag is not blocked', () => {
    const room = withLabels(grid('SSSS', 'SSSS'), [['1', '2', '3', '4'], ['5', '6', '7', '8']])

    const result = drag(room, settings(), ROW_2) // row 2 from 1: row 1's seats 1–4 give up their numbers

    assert.equal(result.problem, '')
    assert.deepEqual(labelsOf(result.cells), [[null, null, null, null], ['1', '2', '3', '4']])
    assert.equal(describeLostLabels(result), '4 seats lost their numbers – they’re marked so you can number them.')
  })

  test('a seat that lost its label earlier in the drag and is then touched gets a new one', () => {
    const result = drag(withLabels(grid('SSS'), [['1', '2', '3']]), settings(), [[0, 2], [0, 1], [0, 0]])

    assert.deepEqual(labelsOf(result.cells), [['3', '2', '1']])
    assert.equal(describeLostLabels(result), '')
  })

  test('one lost label is described in the singular', () => {
    const result = drag(withLabels(grid('SS'), [['1', null]]), settings(), [[0, 1]])

    assert.equal(describeLostLabels(result), '1 seat lost its number – it’s marked so you can number it.')
  })

  test('a label of the other type blocks the drag with one message', () => {
    const result = drag(withLabels(grid('WSS'), [['1']]), settings({ applyTo: 'SEAT' }), [[0, 1], [0, 2]])

    // stored column 0 is drawn on the screen's right, so it is column 3 as the professor reads it
    assert.equal(result.problem, '1 is already used by the workstation in row 1, column 3. '
      + 'Seats and workstations can’t share a number: give one of them a prefix.')
    assert.deepEqual(labelsOf(result.cells), [['1', null, null]])
  })
})
