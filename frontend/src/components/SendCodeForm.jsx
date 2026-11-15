// SendCodeForm — sign-in step 1: the user types their CIT-U email and asks for a code (FR-001).
import { useState } from 'react'
import { requestSignInCode } from '../api/authApi.js'

export default function SendCodeForm({ onCodeSent }) {
  const [email, setEmail] = useState('')
  const [error, setError] = useState('')
  const [isSending, setIsSending] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setIsSending(true)
    try {
      onCodeSent(await requestSignInCode(email))
    } catch (requestError) {
      setError(requestError.message)
      setIsSending(false)
    }
  }

  return (
    <form className="form" onSubmit={handleSubmit}>
      <h2>Sign in</h2>
      <label htmlFor="email">CIT-U email address</label>
      <input
        id="email"
        type="email"
        autoComplete="email"
        placeholder="juan.delacruz@cit.edu"
        value={email}
        onChange={(event) => setEmail(event.target.value)}
        required
      />
      <button type="submit" className="button-primary" disabled={isSending}>
        {isSending ? 'Sending…' : 'Send code'}
      </button>
      {error && <p className="error-text" role="alert">{error}</p>}
    </form>
  )
}
