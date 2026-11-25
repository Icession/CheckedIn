// LayoutLegend — what each cell color means, with live counts of seats and workstations (FR-018, FR-020),
// and how many of them have no number yet (marked with a dashed outline). Shown while a drawing tool is chosen.
import { CELL_TYPES, cellClassName, countCellsByType, isUsable } from '../utils/layoutGrid.js'
import { countUnnumbered } from '../utils/seatLabels.js'

export default function LayoutLegend({ cells }) {
  const counts = countCellsByType(cells)
  const unnumbered = countUnnumbered(cells)

  return (
    <div className="layout-legend">
      <h3>Legend</h3>
      <ul className="legend-list">
        {CELL_TYPES.map((type) => (
          <li key={type.value}>
            <span className={`tool-swatch ${cellClassName(type.value)}`}>{type.letter}</span>
            <span>{type.label}</span>
            {isUsable(type.value) && <strong className="legend-count">{counts[type.value]}</strong>}
          </li>
        ))}
      </ul>
      <p className="legend-total">
        Usable total <strong>{counts.SEAT + counts.WORKSTATION}</strong>
      </p>
      {unnumbered > 0 && (
        <p className="legend-unnumbered">
          <span className="tool-swatch legend-swatch--unnumbered" aria-hidden="true" />
          Not numbered yet <strong className="legend-count">{unnumbered}</strong>
        </p>
      )}
      <p className="hint-text">Only seats and workstations can hold a student. Tables, aisles and “no chair” spots are shown on the map only.</p>
    </div>
  )
}
