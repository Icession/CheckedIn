// BuilderToolbar — the one toolbar of the layout builder: the five cell types (FR-018) | Number and Select
// (FR-019) | Undo. Each button shows its keyboard key (e.g. Q), and the active tool is highlighted.
import {
  CELL_TYPES, NUMBER_TOOL, SELECT_TOOL, UNDO_SHORTCUT, cellClassName, shortcutForTool, toolLabel,
} from '../utils/layoutGrid.js'

const DRAWING_TOOLS = CELL_TYPES.map((type) => ({ value: type.value, letter: type.letter, swatchClass: cellClassName(type.value) }))
const NUMBERING_TOOLS = [
  { value: NUMBER_TOOL, letter: '#', swatchClass: 'tool-swatch--number' },
  { value: SELECT_TOOL, letter: '', swatchClass: 'tool-swatch--select' },
]

export default function BuilderToolbar({ selectedTool, onSelectTool, canUndo, onUndo }) {
  function toolButton(tool) {
    const isSelected = selectedTool === tool.value
    const key = shortcutForTool(tool.value)
    return (
      <button
        key={tool.value}
        type="button"
        role="radio"
        aria-checked={isSelected}
        aria-keyshortcuts={key}
        title={`${toolLabel(tool.value)} (key ${key})`}
        className={`tool-button${isSelected ? ' tool-button--selected' : ''}`}
        onClick={() => onSelectTool(tool.value)}
      >
        <span className={`tool-swatch ${tool.swatchClass}`}>{tool.letter}</span>
        {toolLabel(tool.value)}
        <kbd className="tool-key">{key}</kbd>
      </button>
    )
  }

  return (
    <div className="builder-toolbar" role="toolbar" aria-label="Layout tools">
      <div className="toolbar-group" role="radiogroup" aria-label="Draw">
        {DRAWING_TOOLS.map(toolButton)}
      </div>
      <div className="toolbar-group" role="radiogroup" aria-label="Number">
        {NUMBERING_TOOLS.map(toolButton)}
      </div>
      <div className="toolbar-group">
        <button type="button" className="tool-button" onClick={onUndo} disabled={!canUndo} aria-keyshortcuts={UNDO_SHORTCUT} title={`Undo (key ${UNDO_SHORTCUT})`}>
          Undo
          <kbd className="tool-key">{UNDO_SHORTCUT}</kbd>
        </button>
      </div>
    </div>
  )
}
