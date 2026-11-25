// AutoNumberPanel — the numbering settings and Auto-number (FR-019), stacked one field per row for the
// side panel: which cells ("Apply to"), then for each type its prefix and its "Start at" (set only by the
// professor), then where numbering starts ("Start from" a corner) and its "Direction", a preview per type such
// as "Workstations: PC-1 … PC-28 (28)" and the button.
// Nothing selected: Auto-number renumbers all from Start at (the same result every time). Cells selected:
// the block continues from "Next". Typing Start at, Enter in it or leaving it sets Next to it.
// If Auto-number can't run, pressing it says why in the builder's message spot.
import { autoNumberProblem, planAutoNumbers } from '../utils/autoNumber.js'
import { describeSelection } from '../utils/cellSelection.js'
import { cellTypeInfo } from '../utils/layoutGrid.js'
import {
  APPLY_TO_LABELS, DIRECTION_LABELS, START_CORNER_LABELS, applyToNouns, describeNumberingOrder, numberingFieldsFor,
  numberingSettingsProblem, numberingTypes,
} from '../utils/seatNumbers.js'

const TYPE_NAMES = { WORKSTATION: 'Workstations', SEAT: 'Seats' }
const PREFIX_EXAMPLES = { WORKSTATION: 'PC-', SEAT: 'none' }

// "PC-1 … PC-28 (28)", "PC-1 (1)" or "none"
function previewOf(group) {
  const { labels } = group
  if (labels.length === 0) return 'none'
  const range = labels.length === 1 ? labels[0] : `${labels[0]} … ${labels.at(-1)}`
  return `${range} (${labels.length})`
}

export default function AutoNumberPanel({ settings, onSettingsChange, cells, selection, nextNumbers, onAutoNumber, onClearSelection }) {
  const onlySelected = selection.size > 0
  const plan = planAutoNumbers(cells, settings, { selectedKeys: onlySelected ? selection : null, nextNumbers })
  const nouns = applyToNouns(settings.applyTo)

  function handleChange(event) {
    onSettingsChange(event.target.name, event.target.value)
  }

  return (
    <fieldset className="form numbering-panel">
      <legend>Numbering</legend>
      <label htmlFor="applyTo">Apply to</label>
      <select id="applyTo" name="applyTo" value={settings.applyTo} onChange={handleChange}>
        {Object.entries(APPLY_TO_LABELS).map(([value, label]) => (
          <option key={value} value={value}>{label}</option>
        ))}
      </select>

      {numberingTypes(settings.applyTo).map((cellType) => {
        const { prefixField, startField } = numberingFieldsFor(cellType)
        const typeName = cellTypeInfo(cellType).label
        return (
          <div key={cellType} className="form numbering-type">
            <label htmlFor={prefixField}>{typeName} prefix (optional)</label>
            <input id={prefixField} name={prefixField} placeholder={PREFIX_EXAMPLES[cellType]} maxLength={9} value={settings[prefixField]} onChange={handleChange} />
            <label htmlFor={startField}>{typeName} start at</label>
            <input
              id={startField}
              name={startField}
              type="number"
              min={0}
              value={settings[startField]}
              onChange={handleChange}
              onBlur={() => onSettingsChange(startField, settings[startField])}
              onKeyDown={(event) => event.key === 'Enter' && onSettingsChange(startField, settings[startField])}
            />
          </div>
        )
      })}

      {/* Radio buttons instead of drop-downs, so the long names are never cut off in the narrow panel */}
      <div className="radio-group" role="radiogroup" aria-labelledby="startCornerTitle">
        <span id="startCornerTitle" className="field-title">Start from</span>
        {Object.entries(START_CORNER_LABELS).map(([value, label]) => (
          <label key={value} className="radio-option">
            <input type="radio" name="startCorner" value={value} checked={settings.startCorner === value} onChange={handleChange} />
            {label}
          </label>
        ))}
      </div>
      <div className="radio-group" role="radiogroup" aria-labelledby="directionTitle">
        <span id="directionTitle" className="field-title">Direction</span>
        {Object.entries(DIRECTION_LABELS).map(([value, label]) => (
          <label key={value} className="radio-option">
            <input type="radio" name="direction" value={value} checked={settings.direction === value} onChange={handleChange} />
            {label}
          </label>
        ))}
      </div>

      <p className="hint-text">
        {onlySelected ? (
          <>
            <strong>{describeSelection(selection.size, settings.applyTo)}</strong>, numbered from Next{' · '}
            <button type="button" className="button-link" onClick={onClearSelection}>
              Clear selection
            </button>
          </>
        ) : (
          `Nothing selected, so Auto-number renumbers all ${nouns.many} from Start at, ${describeNumberingOrder(settings)}.`
        )}
      </p>
      {!numberingSettingsProblem(settings) && (
        <ul className="numbering-preview">
          {plan.map((group) => (
            <li key={group.cellType}>
              {TYPE_NAMES[group.cellType]}: <strong>{previewOf(group)}</strong>
            </li>
          ))}
        </ul>
      )}
      <button type="button" className="button-secondary" onClick={() => onAutoNumber(plan, autoNumberProblem(cells, settings, plan))}>
        Auto-number
      </button>
    </fieldset>
  )
}
