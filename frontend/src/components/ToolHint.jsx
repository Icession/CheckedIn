// ToolHint — one short line under the toolbar that says what the chosen tool does. For the Number tool it
// also shows the next label, e.g. "Next: PC-4" (FR-019).
import { NUMBER_TOOL, SELECT_TOOL } from '../utils/layoutGrid.js'
import { applyToNouns } from '../utils/seatNumbers.js'

// What each drawing tool draws, e.g. "Tap or drag to draw tables."
const DRAWN_THINGS = { SEAT: 'seats', WORKSTATION: 'workstations', TABLE: 'tables', AISLE: 'aisles', NO_CHAIR: '“no chair” spots' }

export default function ToolHint({ tool, applyTo, nextLabels }) {
  const nouns = applyToNouns(applyTo)
  let hint = `Tap or drag to draw ${DRAWN_THINGS[tool]}. Z undoes.`
  if (tool === NUMBER_TOOL) {
    hint = `Drag across ${nouns.many} to number them in order, or tap one to type its label. Next: ${nextLabels}`
  } else if (tool === SELECT_TOOL) {
    hint = `Drag to select ${nouns.many}. Drag from a selected ${nouns.one} to unselect. Esc clears.`
  }
  return <p className="builder-hint">{hint}</p>
}
