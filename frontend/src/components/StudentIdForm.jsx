// StudentIdForm — a student's first sign-in: link their student ID once (FR-005).
// The ID connects the student to their classes and cannot be changed later, so the form
// shows it back once ("Is this your student ID?") before saving.
import { useState } from 'react'
import { linkStudentId } from '../api/authApi.js'

// The same format the backend checks. Checked here too, so the confirm step only shows a complete ID.
const STUDENT_ID_FORMAT = /^\d{2}-\d{4}-\d{3}$/
const FORMAT_MESSAGE = 'Please enter your student ID in the format 00-0000-000, for example 12-3456-789.'

// Adds the dashes while typing, so "123456789" becomes "12-3456-789".
// (Phone number keypads often have no "-" key.)
function formatStudentId(typedText) {
  const digits = typedText.replace(/\D/g, '').slice(0, 9)
  if (digits.length <= 2) return digits
  if (digits.length <= 6) return `${digits.slice(0, 2)}-${digits.slice(2)}`
  return `${digits.slice(0, 2)}-${digits.slice(2, 6)}-${digits.slice(6)}`
}

export default function StudentIdForm({ onLinked }) {
  const [studentNumber, setStudentNumber] = useState('')
  const [isConfirming, setIsConfirming] = useState(false)
  const [error, setError] = useState('')
  const [isSaving, setIsSaving] = useState(false)

  function handleContinue(event) {
    event.preventDefault()
    if (!STUDENT_ID_FORMAT.test(studentNumber)) {
      setError(FORMAT_MESSAGE)
      return
    }
    setError('')
    setIsConfirming(true)
  }

  async function handleConfirm() {
    setError('')
    setIsSaving(true)
    try {
      onLinked(await linkStudentId(studentNumber))
    } catch (linkError) {
      // For example "This student ID is already linked to another account..."
      setError(linkError.message)
      setIsSaving(false)
      setIsConfirming(false)
    }
  }

  if (isConfirming) {
    return (
      <div className="form">
        <h2>Is this your student ID?</h2>
        <p className="student-id-preview">{studentNumber}</p>
        <p className="hint-text">You can't change it later, so please check it carefully.</p>
        <button type="button" className="button-primary" onClick={handleConfirm} disabled={isSaving}>
          {isSaving ? 'Saving…' : 'Yes, link it'}
        </button>
        <button type="button" className="button-link" onClick={() => setIsConfirming(false)} disabled={isSaving}>
          No, let me fix it
        </button>
      </div>
    )
  }

  return (
    <form className="form" onSubmit={handleContinue}>
      <h2>Link your student ID</h2>
      <p>Your student ID connects you to your classes. You only enter it once.</p>

      <label htmlFor="studentNumber">Student ID</label>
      <input
        id="studentNumber"
        type="text"
        inputMode="numeric"
        autoComplete="off"
        placeholder="12-3456-789"
        value={studentNumber}
        onChange={(event) => setStudentNumber(formatStudentId(event.target.value))}
        required
      />
      <button type="submit" className="button-primary">
        Continue
      </button>
      {error && <p className="error-text" role="alert">{error}</p>}
    </form>
  )
}
