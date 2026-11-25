// useCellDrag — how a mouse press and drag across grid cells works, kept apart from what each tool does.
// - A press calls onPressStart(row, column) with the pressed cell.
// - While the button is down, onTouchCell(row, column) is called once for every cell the mouse touches.
// - Letting go calls onPressEnd(); a press without a drag also calls onTap(row, column).
// touchStartCellAtOnce: true -> the pressed cell counts right away (painting); false -> it counts only when
// the mouse moves on to another cell (Number tool, where a tap types a label instead).
// cancelPress() stops a drag in progress (Esc). Touch screens and the keyboard send one click per cell instead.
import { useEffect, useRef } from 'react'
import { positionKey } from '../utils/layoutGrid.js'

export default function useCellDrag({ touchStartCellAtOnce, onPressStart, onTouchCell, onPressEnd, onTap }) {
  // The press in progress: where it started, the cells touched so far, whether the mouse moved on or was cancelled
  const press = useRef(null)
  const ignoreNextClick = useRef(false)

  // A press ends when the button is let go, anywhere on the page. No dependency list: the listener is
  // renewed after every render, so it always uses the newest handlers.
  useEffect(() => {
    window.addEventListener('pointerup', finishPress)
    return () => window.removeEventListener('pointerup', finishPress)
  })

  function touch(row, column) {
    const key = positionKey(row, column)
    if (press.current.touched.has(key)) return // each cell counts once per drag
    press.current.touched.add(key)
    onTouchCell(row, column)
  }

  function handlePointerDown(row, column, event) {
    if (event.pointerType !== 'mouse') return
    press.current = { start: { row, column }, touched: new Set(), dragged: false, cancelled: false }
    onPressStart(row, column)
    if (touchStartCellAtOnce) {
      touch(row, column)
    }
  }

  function handlePointerEnter(row, column, event) {
    const current = press.current
    if (!current || current.cancelled) return
    if (event.buttons === 0) {
      press.current = null // the button was let go outside the page
      onPressEnd()
      return
    }
    if (!current.dragged && !touchStartCellAtOnce) {
      touch(current.start.row, current.start.column) // the cell where the drag started counts too
    }
    current.dragged = true
    touch(row, column)
  }

  function finishPress() {
    const current = press.current
    if (!current) return
    press.current = null
    if (!current.cancelled) {
      onPressEnd()
      if (!current.dragged) onTap(current.start.row, current.start.column)
    }
    // The browser sends a click right after this, but the press has already been handled
    ignoreNextClick.current = true
    setTimeout(() => {
      ignoreNextClick.current = false
    }, 0)
  }

  // Esc during a drag: touch no more cells; letting go later does nothing
  function cancelPress() {
    if (press.current) {
      press.current.cancelled = true
    }
  }

  return {
    handlePointerDown,
    handlePointerEnter,
    cancelPress,
    isClickAlreadyHandled: () => ignoreNextClick.current,
  }
}
