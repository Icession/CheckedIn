// SignInPage — the first page of CheckedIn. Sign in with a code sent to your CIT-U email,
// or with GitHub; on the first sign-in, enter your name. Covers FR-001 – FR-004.
// The dashboards come in step 1i.
import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { getCurrentUser } from '../api/authApi.js'
import SendCodeForm from '../components/SendCodeForm.jsx'
import VerifyCodeForm from '../components/VerifyCodeForm.jsx'
import GitHubSignInButton from '../components/GitHubSignInButton.jsx'
import NameForm from '../components/NameForm.jsx'

const ROLE_LABELS = { PROFESSOR: 'Professor', STUDENT: 'Student' }

// When GitHub sign-in fails, the backend sends the user back here with ?signInError=<code>
const SIGN_IN_ERROR_MESSAGES = {
  'not-cit-email':
    'Please use your CIT-U email address. Add your @cit.edu email to your GitHub account and ' +
    'verify it, or sign in with an email code below.',
  'github-failed': 'GitHub sign-in did not finish. Please try again, or sign in with an email code below.',
}

export default function SignInPage() {
  // First we ask the backend whether the session cookie still signs us in
  const [isChecking, setIsChecking] = useState(true)
  const [currentUser, setCurrentUser] = useState(null)
  // Set after a code was emailed: { email, expiresInMinutes }
  const [codeSentTo, setCodeSentTo] = useState(null)
  const [searchParams] = useSearchParams()
  const gitHubError = SIGN_IN_ERROR_MESSAGES[searchParams.get('signInError')]

  useEffect(() => {
    getCurrentUser()
      .then(setCurrentUser)
      .catch(() => setCurrentUser(null))
      .finally(() => setIsChecking(false))
  }, [])

  return (
    <main className="page page--center">
      <h1 className="brand">CheckedIn</h1>
      <p className="muted">Seat-map attendance for CIT-U classes</p>

      <section className="card sign-in-card">
        {isChecking && <p className="muted">Loading…</p>}

        {/* First sign-in: no account yet until the name is saved (FR-003) */}
        {!isChecking && currentUser && currentUser.needsName && (
          <NameForm email={currentUser.email} onSaved={setCurrentUser} />
        )}

        {/* Temporary: step 1i replaces this card with the professor and student dashboards */}
        {!isChecking && currentUser && !currentUser.needsName && (
          <div>
            <h2>Welcome, {currentUser.firstName}!</h2>
            <p>
              Signed in as <strong>{currentUser.firstName} {currentUser.lastName}</strong> (
              {currentUser.email}).
            </p>
            <p>
              Role: <strong>{ROLE_LABELS[currentUser.role]}</strong>
            </p>
          </div>
        )}

        {!isChecking && !currentUser && !codeSentTo && (
          <>
            {gitHubError && <p className="error-text" role="alert">{gitHubError}</p>}
            <SendCodeForm onCodeSent={setCodeSentTo} />
            <p className="divider-text">or</p>
            <GitHubSignInButton />
          </>
        )}

        {!isChecking && !currentUser && codeSentTo && (
          <VerifyCodeForm
            codeSentTo={codeSentTo}
            onSignedIn={setCurrentUser}
            onCodeResent={setCodeSentTo}
            onUseDifferentEmail={() => setCodeSentTo(null)}
          />
        )}
      </section>
    </main>
  )
}
