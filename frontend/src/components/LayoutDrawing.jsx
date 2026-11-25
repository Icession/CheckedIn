// LayoutDrawing — the "Draw the room" card of the layout builder (FR-018 – FR-020, FR-023): one toolbar,
// a short hint, and the grid with a side panel (legend and counts, or the numbering settings). It edits the
// shown cells of the layout draft (useLayoutDraft). A mouse press and drag is handled by useDrawingPress; a
// tap or the keyboard changes one cell here. Esc clears the selection and cancels a drag. Messages go to the
// one message spot in the bottom bar (onMessage).
import { useState } from 'react'
import AutoNumberPanel from './AutoNumberPanel.jsx'
import BuilderToolbar from './BuilderToolbar.jsx'
import LayoutGrid from './LayoutGrid.jsx'
import LayoutLegend from './LayoutLegend.jsx'
import SeatNumberForm from './SeatNumberForm.jsx'
import ToolHint from './ToolHint.jsx'
import useDrawingPress from '../hooks/useDrawingPress.js'
import useToolShortcuts from '../hooks/useToolShortcuts.js'
import { applyAutoNumbers, nextAfterPlan } from '../utils/autoNumber.js'
import { changeSelection, numberableSelection, selectionModeAt } from '../utils/cellSelection.js'
import { NUMBER_TOOL, SELECT_TOOL, countUsableSeats, formatSeatCount, isUsable, paintCell, startingCellType, updateCell } from '../utils/layoutGrid.js'
import { findDuplicateLabels } from '../utils/seatLabels.js'
import { describeNextLabels } from '../utils/seatNumbers.js'

const NO_MESSAGE = { text: '', isError: false }

export default function LayoutDrawing({ layoutDraft, onMessage, onUndo }) {
  const { shownCells: cells, numbering, nextNumbers } = layoutDraft
  const [tool, setTool] = useState(() => startingCellType(layoutDraft.draft.roomType))
  const [selectedCell, setSelectedCell] = useState(null) // { row, column } whose label is being typed
  const [selectedKeys, setSelectedKeys] = useState(() => new Set()) // cells chosen with the Select tool

  const selection = numberableSelection(cells, selectedKeys, numbering.applyTo)
  const isNumbering = tool === NUMBER_TOOL || tool === SELECT_TOOL

  useToolShortcuts({ onChooseTool: chooseTool, onUndo: undo, onEscape: handleEscape })
  const drawingPress = useDrawingPress({
    tool, layoutDraft, selection, setSelectedKeys, onMessage,
    onTap: (row, column) => {
      if (tool === NUMBER_TOOL) openLabelForm(row, column)
    },
  })

  function chooseTool(newTool) {
    setTool(newTool)
    setSelectedCell(null)
  }

  function handleNumberingChange(fieldName, value) {
    layoutDraft.changeNumbering(fieldName, value)
    onMessage(NO_MESSAGE)
  }

  // One change of the shown cells, as one undo step
  function changeCells(newCells) {
    layoutDraft.rememberForUndo()
    layoutDraft.changeShownCells(newCells)
  }

  function undo() {
    setSelectedCell(null)
    onUndo()
  }

  // Esc: cancel a drag in progress and clear the selection
  function handleEscape() {
    drawingPress.cancelPress()
    setSelectedKeys(new Set())
    onMessage(NO_MESSAGE)
  }

  function openLabelForm(row, column) {
    setSelectedCell(isUsable(cells[row][column].cellType) ? { row, column } : null)
  }

  // Touch and keyboard: one cell per tap (a mouse press is handled by useDrawingPress)
  function handleCellClick(row, column) {
    if (drawingPress.isClickAlreadyHandled()) return
    onMessage(NO_MESSAGE)
    if (tool === NUMBER_TOOL) {
      openLabelForm(row, column)
    } else if (tool === SELECT_TOOL) {
      const mode = selectionModeAt(selection, row, column)
      setSelectedKeys((keys) => changeSelection(keys, cells, row, column, mode, numbering.applyTo))
    } else {
      const newCells = paintCell(cells, row, column, tool)
      if (newCells !== cells) changeCells(newCells)
    }
  }

  function handleLabelTyped(seatLabel) {
    changeCells(updateCell(cells, selectedCell.row, selectedCell.column, { seatLabel }))
    setSelectedCell(null)
    onMessage(NO_MESSAGE)
  }

  // Auto-number: all cells from Start at, or the selected block from Next; afterwards Next = last number + 1
  function handleAutoNumber(plan, problem) {
    if (problem) {
      onMessage({ text: problem, isError: true })
      return
    }
    changeCells(applyAutoNumbers(cells, plan))
    layoutDraft.setNextNumbers(nextAfterPlan(nextNumbers, plan))
    setSelectedKeys(new Set())
    setSelectedCell(null)
    const count = plan.reduce((sum, group) => sum + group.positions.length, 0)
    onMessage({ text: `Numbered ${count === 1 ? '1 seat' : `${count} seats`}. Check the numbers, then save the layout.`, isError: false })
  }

  return (
    <section className="card">
      <div className="card-title-row">
        <h2>Draw the room</h2>
        <strong>{formatSeatCount(countUsableSeats(cells))}</strong>
      </div>
      <BuilderToolbar selectedTool={tool} onSelectTool={chooseTool} canUndo={layoutDraft.canUndo} onUndo={undo} />
      <ToolHint tool={tool} applyTo={numbering.applyTo} nextLabels={describeNextLabels(numbering, nextNumbers)} />

      <div className="builder-main">
        <LayoutGrid
          cells={cells}
          selectedCell={selectedCell}
          selectedKeys={selection}
          duplicateLabels={findDuplicateLabels(cells)}
          onCellClick={handleCellClick}
          onCellPointerDown={drawingPress.handlePointerDown}
          onCellPointerEnter={drawingPress.handlePointerEnter}
        />
        <aside className="builder-side-panel" aria-label={isNumbering ? 'Numbering' : 'Legend'}>
          {selectedCell && (
            <SeatNumberForm
              key={`${selectedCell.row}-${selectedCell.column}`}
              cells={cells}
              row={selectedCell.row}
              column={selectedCell.column}
              onApply={handleLabelTyped}
              onCancel={() => setSelectedCell(null)}
            />
          )}
          {isNumbering ? (
            <AutoNumberPanel
              settings={numbering}
              onSettingsChange={handleNumberingChange}
              cells={cells}
              selection={selection}
              nextNumbers={nextNumbers}
              onAutoNumber={handleAutoNumber}
              onClearSelection={() => setSelectedKeys(new Set())}
            />
          ) : (
            <LayoutLegend cells={cells} />
          )}
        </aside>
      </div>
    </section>
  )
}
