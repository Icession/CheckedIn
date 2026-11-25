// LayoutDetailsFields — the room details of a layout (FR-017): room name, room type, rows and columns.
// Used by the "New layout" form and by the layout builder. The parent keeps the values and gets every
// change as onChange(fieldName, newValue, isTyping). isTyping tells typing apart from the ▲▼ arrows, so the
// builder can wait until typing stops but apply an arrow click at once. onSizeApply (builder only) is called
// when a size box is left or Enter is pressed in it.
import { MAX_ROWS_OR_COLUMNS, ROOM_TYPE_LABELS } from '../utils/layoutGrid.js'

export default function LayoutDetailsFields({ values, onChange, onSizeApply }) {
  // Typing sends an input event that says what was typed (inputType "insertText", "deleteContentBackward"...);
  // the ▲▼ arrows and the arrow keys change the number without one (checked in Chrome)
  function handleChange(event) {
    const inputType = event.nativeEvent.inputType
    onChange(event.target.name, event.target.value, typeof inputType === 'string' && inputType !== '')
  }

  function handleSizeKeyDown(event) {
    if (event.key === 'Enter' && onSizeApply) {
      event.preventDefault()
      onSizeApply()
    }
  }

  const sizeBoxProps = { type: 'number', min: 1, max: MAX_ROWS_OR_COLUMNS, onChange: handleChange, onBlur: onSizeApply, onKeyDown: handleSizeKeyDown, required: true }

  return (
    <>
      <label htmlFor="roomName">Room name</label>
      <input id="roomName" name="roomName" placeholder="NGE207" maxLength={30} value={values.roomName} onChange={handleChange} required />

      <label htmlFor="roomType">Room type</label>
      <select id="roomType" name="roomType" value={values.roomType} onChange={handleChange}>
        {Object.entries(ROOM_TYPE_LABELS).map(([value, label]) => (
          <option key={value} value={value}>
            {label}
          </option>
        ))}
      </select>

      <div className="form-grid">
        <div className="field">
          <label htmlFor="rowCount">Rows (1–{MAX_ROWS_OR_COLUMNS})</label>
          <input id="rowCount" name="rowCount" value={values.rowCount} {...sizeBoxProps} />
        </div>
        <div className="field">
          <label htmlFor="columnCount">Columns (1–{MAX_ROWS_OR_COLUMNS})</label>
          <input id="columnCount" name="columnCount" value={values.columnCount} {...sizeBoxProps} />
        </div>
      </div>
    </>
  )
}
