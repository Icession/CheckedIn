// Unit tests for cellSelection.js: the Select tool (FR-019).
// Node's built-in test runner (no extra library). Run them with: npm test
import assert from 'node:assert/strict'
import { describe, test } from 'node:test'
import { changeSelection, describeSelection, numberableSelection, selectionModeAt } from './cellSelection.js'
import { positionKey } from './layoutGrid.js'
import { grid } from './testGrids.js'

const cells = grid('WWT', 'SWA')

// Selects or unselects the given cells in one drag, like the Select tool does
function dragSelect(selectedKeys, touchedCells, applyTo = 'WORKSTATION') {
  const [startRow, startColumn] = touchedCells[0]
  const mode = selectionModeAt(numberableSelection(cells, selectedKeys, applyTo), startRow, startColumn)
  return touchedCells.reduce((keys, [row, column]) => changeSelection(keys, cells, row, column, mode, applyTo), selectedKeys)
}

describe('selecting cells for Auto-number (Select tool)', () => {
  test('a drag that starts on an unselected cell selects, one that starts on a selected cell unselects', () => {
    const selected = dragSelect(new Set(), [[0, 0], [0, 1], [1, 1]])
    const afterUnselect = dragSelect(selected, [[0, 1], [1, 1]])

    assert.deepEqual([...selected], [positionKey(0, 0), positionKey(0, 1), positionKey(1, 1)])
    assert.deepEqual([...afterUnselect], [positionKey(0, 0)])
  })

  test('tapping a selected cell unselects it, tapping it again selects it', () => {
    const once = dragSelect(new Set([positionKey(0, 0)]), [[0, 0]])

    assert.deepEqual([...once], [])
    assert.deepEqual([...dragSelect(once, [[0, 0]])], [positionKey(0, 0)])
  })

  test('tables, aisles and the type that "Apply to" leaves out are never selected', () => {
    const selected = dragSelect(new Set(), [[0, 1], [0, 2], [1, 0], [1, 1], [1, 2]])

    assert.deepEqual([...selected], [positionKey(0, 1), positionKey(1, 1)])
  })

  test('the count is shown in words', () => {
    assert.equal(describeSelection(5, 'SEAT'), '5 seats selected')
    assert.equal(describeSelection(1, 'WORKSTATION'), '1 workstation selected')
  })
})
