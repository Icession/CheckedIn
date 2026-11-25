// LayoutGrid — a room drawn as a grid of cells (FR-018, FR-023), seen from the teacher's desk: the room is
// turned around (see layoutView.js), so the front row is at the bottom, just above the "Front of room" bar.
// Cells are drawn in screen order, but every cell keeps its stored row and column, so clicks, drags and
// selections always reach the right cell. On small screens the grid scrolls sideways (NFR-018).
// Without the "on..." handlers (readOnly) it only shows the room.
import LayoutCell from './LayoutCell.jsx'
import { isUsable, positionKey } from '../utils/layoutGrid.js'
import { fromScreenPosition, positionAsSeen } from '../utils/layoutView.js'

const NO_KEYS = new Set()

export default function LayoutGrid({
  cells, selectedCell, selectedKeys = NO_KEYS, duplicateLabels = NO_KEYS, readOnly,
  onCellClick, onCellPointerDown, onCellPointerEnter,
}) {
  const rowCount = cells.length
  const columnCount = cells[0]?.length ?? 0

  // Screen positions from the top left, each showing the stored cell that is drawn there
  const screenPositions = []
  for (let screenRow = 0; screenRow < rowCount; screenRow++) {
    for (let screenColumn = 0; screenColumn < columnCount; screenColumn++) {
      screenPositions.push(fromScreenPosition({ row: screenRow, column: screenColumn }, rowCount, columnCount))
    }
  }

  return (
    <div className="layout-scroll">
      <div className="layout-map">
        <div className="layout-grid" style={{ gridTemplateColumns: `repeat(${columnCount}, var(--cell-size))` }}>
          {screenPositions.map(({ row, column }) => {
            const cell = cells[row][column]
            return (
              <LayoutCell
                key={positionKey(row, column)}
                cell={cell}
                positionSeen={positionAsSeen({ row, column }, rowCount, columnCount)}
                readOnly={readOnly}
                isSelected={selectedCell?.row === row && selectedCell?.column === column}
                isInSelection={selectedKeys.has(positionKey(row, column))}
                hasDuplicateNumber={isUsable(cell.cellType) && duplicateLabels.has(cell.seatLabel)}
                onClick={() => onCellClick?.(row, column)}
                onPointerDown={(event) => onCellPointerDown?.(row, column, event)}
                onPointerEnter={(event) => onCellPointerEnter?.(row, column, event)}
              />
            )
          })}
        </div>
        <div className="layout-front">Front of room (board / teacher&apos;s desk)</div>
      </div>
    </div>
  )
}
