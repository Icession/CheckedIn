// useUndoSteps — the layout builder's undo list (Z): earlier states, the newest last. Only the last 30 are kept.
// A state is a whole layout draft (see layoutDraft.js), so one Undo brings back size, cells and numbers together.
import { useState } from 'react'

const STEPS_KEPT = 30

export default function useUndoSteps() {
  const [steps, setSteps] = useState([])

  return {
    canUndo: steps.length > 0,
    // Call before a change, with the state as it was
    remember: (stateBefore) => setSteps((current) => [...current, stateBefore].slice(-STEPS_KEPT)),
    // The state before the last change (or null), which is then removed from the list
    takeLast: () => {
      if (steps.length === 0) return null
      setSteps((current) => current.slice(0, -1))
      return steps.at(-1)
    },
  }
}
