// NameForm — first sign-in: every new user enters their first and last name (FR-003) and, since Increment 2 /
// SRS v1.4, creates a password, whichever way they signed in (email code or GitHub). The account is created only
// when this form is saved, so leaving the page before that simply shows this form again at the next sign-in.
import { useState } from 'react'
import { saveName } from '../api/authApi.js'
import { PASSWORD_MAX_LENGTH, PASSWORD_RULES_HINT, newPasswordProblem } from '../utils/passwordRules.js'

export default function NameForm({ email, onSaved }) {
  const [firstName, setFirstName] = useState('')
  const [lastName, setLastName] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState('')
  const [isSaving, setIsSaving] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    // The same password rules as the backend, so most mistakes show at once (the backend checks again)
    const passwordProblem = newPasswordProblem(password, confirmPassword, email)
    if (passwordProblem) {
      setError(passwordProblem)
      return
    }
    setError('')
    setIsSaving(true)
    try {
      onSaved(await saveName(firstName, lastName, password, confirmPassword))
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

      <label htmlFor="newPassword">Create a password</label>
      <input id="newPassword" type="password" autoComplete="new-password" maxLength={PASSWORD_MAX_LENGTH + 10}
        value={password} onChange={(event) => setPassword(event.target.value)} required />

      <label htmlFor="confirmPassword">Confirm password</label>
      <input id="confirmPassword" type="password" autoComplete="new-password" maxLength={PASSWORD_MAX_LENGTH + 10}
        value={confirmPassword} onChange={(event) => setConfirmPassword(event.target.value)} required />
      <p className="hint-text">{PASSWORD_RULES_HINT} You can then also sign in with your email and this password.</p>

      <button type="submit" className="button-primary" disabled={isSaving}>
        {isSaving ? 'Saving…' : 'Continue'}
      </button>
      {error && <p className="error-text" role="alert">{error}</p>}
    </form>
  )
}
