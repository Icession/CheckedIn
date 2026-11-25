// PasswordSignInForm — sign in with a CIT-U email and a password (Increment 2 / SRS v1.4), a third way next to the
// email code and GitHub. A wrong email, a wrong password and an account without a password all get the same
// message, so the hint for accounts without a password is shown after every failed try.
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { signInWithPassword } from '../api/authApi.js'

export default function PasswordSignInForm({ onSignedIn }) {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [showNoPasswordHint, setShowNoPasswordHint] = useState(false)
  const [isSigningIn, setIsSigningIn] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setIsSigningIn(true)
    try {
      onSignedIn(await signInWithPassword(email, password))
    } catch (signInError) {
      setError(signInError.message) // "Email or password is incorrect." or the lockout message
      setShowNoPasswordHint(signInError.status === 401)
      setPassword('')
      setIsSigningIn(false)
    }
  }

  return (
    <form className="form" onSubmit={handleSubmit}>
      <h2>Sign in with password</h2>
      <label htmlFor="passwordEmail">CIT-U email address</label>
      <input id="passwordEmail" type="email" autoComplete="username" placeholder="juan.delacruz@cit.edu"
        value={email} onChange={(event) => setEmail(event.target.value)} required />

      <label htmlFor="signInPassword">Password</label>
      <input id="signInPassword" type="password" autoComplete="current-password"
        value={password} onChange={(event) => setPassword(event.target.value)} required />

      <button type="submit" className="button-primary" disabled={isSigningIn}>
        {isSigningIn ? 'Signing in…' : 'Sign in'}
      </button>
      {error && <p className="error-text" role="alert">{error}</p>}
      {showNoPasswordHint && (
        <p className="hint-text">Never set a password? Use &ldquo;Forgot password?&rdquo; to set one.</p>
      )}
      <Link to="/forgot-password">Forgot password?</Link>
    </form>
  )
}
