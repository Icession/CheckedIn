// VerifyCodeForm — sign-in step 2: the user types the 6-digit code from the email (FR-001).
import { useState } from 'react'
import { requestSignInCode, verifySignInCode } from '../api/authApi.js'

const CODE_LENGTH = 6

export default function VerifyCodeForm({ codeSentTo, onSignedIn, onCodeResent, onUseDifferentEmail }) {
  const [code, setCode] = useState('')
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [isBusy, setIsBusy] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setNotice('')
    setIsBusy(true)
    try {
      onSignedIn(await verifySignInCode(codeSentTo.email, code))
    } catch (verifyError) {
      setError(verifyError.message)
      setIsBusy(false)
    }
  }

  async function handleSendNewCode() {
    setError('')
    setNotice('')
    setIsBusy(true)
    try {
      onCodeResent(await requestSignInCode(codeSentTo.email))
      setCode('')
      setNotice('We sent a new code. Only the newest code works.')
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setIsBusy(false)
    }
  }

  return (
    <form className="form" onSubmit={handleSubmit}>
      <h2>Enter your code</h2>
      <p>
        We sent a 6-digit code to <strong>{codeSentTo.email}</strong>. It expires in{' '}
        {codeSentTo.expiresInMinutes} minutes.
      </p>
      {/* SRS 1.6: emails from CheckedIn may land in the Junk folder of CIT-U Outlook */}
      <p className="hint-text">No email after a minute? Check your Junk Email folder.</p>

      <label htmlFor="code">6-digit code</label>
      <input
        id="code"
        className="code-input"
        type="text"
        inputMode="numeric"
        autoComplete="one-time-code"
        value={code}
        // Keep the digits only, then cut to 6, so a pasted "123 456" or "    123456" still works.
        // (No maxLength here: the browser would cut the pasted text BEFORE the spaces are removed.)
        onChange={(event) => setCode(event.target.value.replace(/\D/g, '').slice(0, CODE_LENGTH))}
        required
      />
      <button type="submit" className="button-primary" disabled={isBusy || code.length !== CODE_LENGTH}>
        Sign in
      </button>

      {notice && <p className="notice-text">{notice}</p>}
      {error && <p className="error-text" role="alert">{error}</p>}

      <div className="row">
        <button type="button" className="button-link" onClick={handleSendNewCode} disabled={isBusy}>
          Send a new code
        </button>
        <button type="button" className="button-link" onClick={onUseDifferentEmail} disabled={isBusy}>
          Use a different email
        </button>
      </div>
    </form>
  )
}
