// useToolShortcuts — the keyboard shortcuts of the layout builder: Q W E R T A S choose a tool, Z undoes,
// Esc clears the selection (and cancels a drag in progress). What each key means is decided by
// shortcutAction in layoutGrid.js; keys typed into a text box are left alone.
// The listener sits on the whole window, so the keys work wherever the focus is (a grid cell, a button...).
import { useEffect } from 'react'
import { shortcutAction } from '../utils/layoutGrid.js'

export default function useToolShortcuts({ onChooseTool, onUndo, onEscape }) {
  // No dependency list: the listener is renewed after every render, so it always calls the newest handlers
  useEffect(() => {
    function handleKeyDown(event) {
      const shortcut = shortcutAction(event)
      if (!shortcut) return
      event.preventDefault()
      if (shortcut.action === 'TOOL') {
        onChooseTool(shortcut.tool)
      } else if (shortcut.action === 'UNDO') {
        onUndo()
      } else {
        onEscape()
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  })
}
