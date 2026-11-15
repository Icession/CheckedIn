// SignInPage — the first page of CheckedIn. Sign in with a code sent to your CIT-U email,
// or with GitHub. On the first sign-in, enter your name; students also link their student ID.
// Then it opens the dashboard for the user's role. Covers FR-001 – FR-005 and FR-008.
import { useEffect, useState } from 'react'
import { Navigate, useSearchParams } from 'react-router-dom'
import { getCurrentUser } from '../api/authApi.js'
import SendCodeForm from '../components/SendCodeForm.jsx'
import VerifyCodeForm from '../components/VerifyCodeForm.jsx'
import GitHubSignInButton from '../components/GitHubSignInButton.jsx'
import NameForm from '../components/NameForm.jsx'
import StudentIdForm from '../components/StudentIdForm.jsx'

// FR-008: professors only see professor pages, students only student pages
const DASHBOARD_PATHS = { PROFESSOR: '/professor', STUDENT: '/student' }

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
  const isSignedIn = !isChecking && currentUser !== null

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

        {/* First sign-in, step 1: no account yet until the name is saved (FR-003) */}
        {isSignedIn && currentUser.needsName && <NameForm email={currentUser.email} onSaved={setCurrentUser} />}

        {/* First sign-in, step 2: a student links their student ID once (FR-005) */}
        {isSignedIn && !currentUser.needsName && currentUser.needsStudentId && (
          <StudentIdForm onLinked={setCurrentUser} />
        )}

        {/* Sign-in finished: open the dashboard for the user's role */}
        {isSignedIn && !currentUser.needsName && !currentUser.needsStudentId && (
          <Navigate to={DASHBOARD_PATHS[currentUser.role]} replace />
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
