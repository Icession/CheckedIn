// useDrawingPress — what one mouse press does with the chosen tool in the layout builder (FR-018, FR-019),
// built on useCellDrag: paint the cells, select or unselect them, or renumber them with the Number tool
// (see numberingDrag.js), starting from "Next". A whole drag is one undo step, and Next counts along.
// cancelPress() (Esc) takes back a drag in progress. A Number-tool tap without a drag calls onTap instead.
import { useRef } from 'react'
import useCellDrag from './useCellDrag.js'
import { changeSelection, selectionModeAt } from '../utils/cellSelection.js'
import { NUMBER_TOOL, SELECT_TOOL, paintCell } from '../utils/layoutGrid.js'
import { describeLostLabels, numberCellInDrag, startNumberingDrag } from '../utils/numberingDrag.js'
import { numberingSettingsProblem } from '../utils/seatNumbers.js'

const NO_MESSAGE = { text: '', isError: false }

export default function useDrawingPress({ tool, layoutDraft, selection, setSelectedKeys, onMessage, onTap }) {
  const { shownCells: cells, numbering, nextNumbers } = layoutDraft
  // What the current press is doing: the grid so far, select or unselect, the numbering drag, and whether
  // it changed anything yet (the first change makes the undo step)
  const pressChanges = useRef(null)
  const cellDrag = useCellDrag({
    touchStartCellAtOnce: tool !== NUMBER_TOOL, // the Number tool waits: a tap types a label instead
    onPressStart: startPress,
    onTouchCell: changeDuringPress,
    onPressEnd: finishPress,
    onTap,
  })

  function startPress(row, column) {
    const settingsProblem = tool === NUMBER_TOOL ? numberingSettingsProblem(numbering) : ''
    pressChanges.current = {
      cells,
      selectMode: selectionModeAt(selection, row, column), // like painting and erasing
      numberingDrag: { ...startNumberingDrag(cells, nextNumbers), problem: settingsProblem },
      problemShown: false,
      changed: false,
    }
    onMessage(NO_MESSAGE)
  }

  // One cell touched during the press: select or unselect it, renumber it (Number tool) or paint it
  function changeDuringPress(row, column) {
    const current = pressChanges.current
    if (!current) return
    if (tool === SELECT_TOOL) {
      setSelectedKeys((keys) => changeSelection(keys, cells, row, column, current.selectMode, numbering.applyTo))
      return
    }
    const newCells = tool === NUMBER_TOOL ? numberDuringPress(current, row, column) : paintCell(current.cells, row, column, tool)
    if (newCells === current.cells) return
    if (!current.changed) {
      layoutDraft.rememberForUndo() // one undo step for the whole drag
      current.changed = true
    }
    current.cells = newCells
    layoutDraft.changeShownCells(newCells)
  }

  // The Number tool as a renumbering brush; Next counts along
  function numberDuringPress(current, row, column) {
    const before = current.numberingDrag
    const after = numberCellInDrag(before, row, column, numbering)
    current.numberingDrag = after
    if (after.problem && !current.problemShown) {
      current.problemShown = true // a clash with the other type (or bad settings) stops the drag; say so once
      onMessage({ text: after.problem, isError: true })
    }
    if (after.cells !== before.cells) {
      layoutDraft.setNextNumbers(after.nextNumbers)
    }
    return after.cells
  }

  // Letting go after a Number-tool drag: say once how many seats lost their numbers to it
  function finishPress() {
    const current = pressChanges.current
    pressChanges.current = null
    const lostLabels = current && tool === NUMBER_TOOL ? describeLostLabels(current.numberingDrag) : ''
    if (lostLabels) {
      const problem = current.numberingDrag.problem
      onMessage(problem ? { text: `${problem} ${lostLabels}`, isError: true } : { text: lostLabels, isError: false })
    }
  }

  // Esc during a drag: stop it and take back what it changed (cells and Next)
  function cancelPress() {
    const current = pressChanges.current
    if (!current) return
    cellDrag.cancelPress()
    pressChanges.current = null
    if (current.changed) {
      layoutDraft.undo()
    }
  }

  return {
    handlePointerDown: cellDrag.handlePointerDown,
    handlePointerEnter: cellDrag.handlePointerEnter,
    isClickAlreadyHandled: cellDrag.isClickAlreadyHandled,
    cancelPress,
  }
}
