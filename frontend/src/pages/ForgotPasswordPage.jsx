// ForgotPasswordPage — /forgot-password (Increment 2 / SRS v1.4): asks for a reset link by email. This is also how
// an account from before Increment 2 sets its first password. The answer is always the same, so the page never
// shows whether an email has an account.
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { requestPasswordReset } from '../api/authApi.js'

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('')
  const [error, setError] = useState('')
  const [isSending, setIsSending] = useState(false)
  const [isSent, setIsSent] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setIsSending(true)
    try {
      await requestPasswordReset(email)
      setIsSent(true)
    } catch (requestError) {
      setError(requestError.message) // e.g. "Please use your CIT-U email address."
    }
    setIsSending(false)
  }

  return (
    <main className="page page--center">
      <h1 className="brand">CheckedIn</h1>
      <section className="card sign-in-card">
        {isSent ? (
          <div className="form">
            <h2>Check your email</h2>
            <p role="status">If that email has an account, we sent a reset link. It works once and only for a short time.</p>
            <p className="hint-text">Nothing in your Inbox after a few minutes? Check Junk Email, or ask again.</p>
          </div>
        ) : (
          <form className="form" onSubmit={handleSubmit}>
            <h2>Forgot password?</h2>
            <p>Enter your CIT-U email. We&apos;ll send you a link to set a new password (or your first one).</p>
            <label htmlFor="forgotEmail">CIT-U email address</label>
            <input id="forgotEmail" type="email" autoComplete="email" placeholder="juan.delacruz@cit.edu"
              value={email} onChange={(event) => setEmail(event.target.value)} required />
            <button type="submit" className="button-primary" disabled={isSending}>
              {isSending ? 'Sending…' : 'Send reset link'}
            </button>
            {error && <p className="error-text" role="alert">{error}</p>}
          </form>
        )}
        <Link to="/">Back to sign in</Link>
      </section>
    </main>
  )
}
