// NameForm — first sign-in: every new user enters their first and last name (FR-003).
// The account is created only when this form is saved, so leaving the page before that
// simply shows this form again at the next sign-in.
import { useState } from 'react'
import { saveName } from '../api/authApi.js'

export default function NameForm({ email, onSaved }) {
  const [firstName, setFirstName] = useState('')
  const [lastName, setLastName] = useState('')
  const [error, setError] = useState('')
  const [isSaving, setIsSaving] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setIsSaving(true)
    try {
      onSaved(await saveName(firstName, lastName))
    } catch (saveError) {
      // The backend explains what is wrong, e.g. "Please enter your last name."
      setError(saveError.message)
      setIsSaving(false)
    }
  }

  return (
    <form className="form" onSubmit={handleSubmit}>
      <h2>Welcome to CheckedIn</h2>
      <p>
        You signed in as <strong>{email}</strong>. Please enter your name as it appears in your
        class list.
      </p>

      <label htmlFor="firstName">First name</label>
      <input
        id="firstName"
        type="text"
        autoComplete="given-name"
        placeholder="Juan"
        value={firstName}
        onChange={(event) => setFirstName(event.target.value)}
        required
      />

      <label htmlFor="lastName">Last name</label>
      <input
        id="lastName"
        type="text"
        autoComplete="family-name"
        placeholder="Dela Cruz"
        value={lastName}
        onChange={(event) => setLastName(event.target.value)}
        required
      />

      <button type="submit" className="button-primary" disabled={isSaving}>
        {isSaving ? 'Saving…' : 'Continue'}
      </button>
      {error && <p className="error-text" role="alert">{error}</p>}
    </form>
  )
}
