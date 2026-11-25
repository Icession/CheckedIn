// Unit tests for layoutGrid.js: growing the grid with the pattern, counting cells, keyboard shortcuts.
// Node's built-in test runner (no extra library). Run them with: npm test
import assert from 'node:assert/strict'
import { describe, test } from 'node:test'
import { countCellsByType, cropGrid, growGrid, shortcutAction } from './layoutGrid.js'
import { grid, typesOf, withLabels } from './testGrids.js'

describe('growing the grid continues the pattern', () => {
  test('a new column copies the last cell of each row', () => {
    const grown = growGrid(grid('SST', 'SSA', 'TTT'), 3, 4)

    assert.deepEqual(typesOf(grown), ['SSTT', 'SSAA', 'TTTT'])
  })

  test('a new row copies the row above, including a table row', () => {
    const grown = growGrid(grid('SAS', 'TTT'), 4, 3)

    assert.deepEqual(typesOf(grown), ['SAS', 'TTT', 'TTT', 'TTT'])
  })

  test('a new row and column at once: the new row copies the grown row above it', () => {
    const grown = growGrid(grid('SA', 'ST'), 3, 3)

    assert.deepEqual(typesOf(grown), ['SAA', 'STT', 'STT'])
  })

  test('new cells have no number yet; the old ones keep theirs', () => {
    const grown = growGrid(withLabels(grid('SS'), [['1', '2']]), 2, 2)

    assert.deepEqual(grown.map((row) => row.map((cell) => cell.seatLabel)), [['1', '2'], [null, null]])
  })

  test('shrinking and growing again brings back the drawn cells, not the pattern', () => {
    const drawn = withLabels(grid('STW'), [['1', null, 'PC-1']])
    const shrunk = cropGrid(drawn, 1, 1) // what is shown; the full grid is kept
    const grownAgain = cropGrid(growGrid(drawn, 1, 3), 1, 3)

    assert.deepEqual(typesOf(shrunk), ['S'])
    assert.deepEqual(typesOf(grownAgain), ['STW'])
    assert.equal(grownAgain[0][2].seatLabel, 'PC-1')
  })
})

describe('counting cells', () => {
  test('counts each cell type for the legend', () => {
    assert.deepEqual(countCellsByType(grid('WWT', 'SWA')), { SEAT: 1, WORKSTATION: 3, TABLE: 1, AISLE: 1, NO_CHAIR: 0 })
  })
})

describe('keyboard: what a key press means', () => {
  const onCell = { tagName: 'BUTTON' }

  test('Esc clears the selection with any tool, also after clicking the grid or a radio button', () => {
    assert.deepEqual(shortcutAction({ key: 'Escape', target: onCell }), { action: 'ESCAPE' })
    assert.deepEqual(shortcutAction({ key: 'Escape', target: { tagName: 'INPUT', type: 'radio' } }), { action: 'ESCAPE' })
    assert.deepEqual(shortcutAction({ key: 'Escape', target: { tagName: 'SELECT' } }), { action: 'ESCAPE' })
  })

  test('keys typed into a text box mean nothing', () => {
    assert.equal(shortcutAction({ key: 'Escape', target: { tagName: 'INPUT', type: 'text' } }), null)
    assert.equal(shortcutAction({ key: 'q', target: { tagName: 'INPUT', type: 'number' } }), null)
  })

  test('letters choose tools, Z undoes, but not in a drop-down or with Ctrl', () => {
    assert.deepEqual(shortcutAction({ key: 'q', target: onCell }), { action: 'TOOL', tool: 'SEAT' })
    assert.deepEqual(shortcutAction({ key: 'Z', target: onCell }), { action: 'UNDO' })
    assert.equal(shortcutAction({ key: 'q', target: { tagName: 'SELECT' } }), null)
    assert.equal(shortcutAction({ key: 'z', ctrlKey: true, target: onCell }), null)
  })
})
