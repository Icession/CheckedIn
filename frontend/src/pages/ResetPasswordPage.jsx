// ResetPasswordPage — /reset-password?token=… (Increment 2 / SRS v1.4): the page the emailed link opens. The user
// types the new password twice (the same rules as everywhere, checked here and again by the backend). A used,
// expired or unknown link gets the backend's clear message.
import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { resetPassword } from '../api/authApi.js'
import { PASSWORD_MAX_LENGTH, PASSWORD_RULES_HINT, newPasswordProblem } from '../utils/passwordRules.js'

export default function ResetPasswordPage() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token') ?? ''
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState(token ? '' : 'This reset link is not complete. Please copy the whole link from the email.')
  const [isSaving, setIsSaving] = useState(false)
  const [isDone, setIsDone] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    // The email isn't known on this page; the backend also checks that the password isn't the email
    const problem = newPasswordProblem(newPassword, confirmPassword, '')
    if (problem) {
      setError(problem)
      return
    }
    setError('')
    setIsSaving(true)
    try {
      await resetPassword(token, newPassword, confirmPassword)
      setIsDone(true)
    } catch (resetError) {
      setError(resetError.message) // e.g. "This reset link has expired. Please ask for a new one."
    }
    setIsSaving(false)
  }

  return (
    <main className="page page--center">
      <h1 className="brand">CheckedIn</h1>
      <section className="card sign-in-card">
        {isDone ? (
          <div className="form">
            <h2>Password saved</h2>
            <p role="status">You can now sign in with your email and your new password.</p>
            <Link className="button-primary" to="/">Sign in</Link>
          </div>
        ) : (
          <form className="form" onSubmit={handleSubmit}>
            <h2>Set a new password</h2>
            <label htmlFor="resetNewPassword">New password</label>
            <input id="resetNewPassword" type="password" autoComplete="new-password" maxLength={PASSWORD_MAX_LENGTH + 10}
              value={newPassword} onChange={(event) => setNewPassword(event.target.value)} required />
            <label htmlFor="resetConfirmPassword">Confirm password</label>
            <input id="resetConfirmPassword" type="password" autoComplete="new-password" maxLength={PASSWORD_MAX_LENGTH + 10}
              value={confirmPassword} onChange={(event) => setConfirmPassword(event.target.value)} required />
            <p className="hint-text">{PASSWORD_RULES_HINT}</p>
            <button type="submit" className="button-primary" disabled={isSaving || !token}>
              {isSaving ? 'Saving…' : 'Save password'}
            </button>
            {error && <p className="error-text" role="alert">{error}</p>}
            {error && <Link to="/forgot-password">Ask for a new link</Link>}
          </form>
        )}
      </section>
    </main>
  )
}
