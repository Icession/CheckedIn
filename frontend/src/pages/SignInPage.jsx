// SignInPage — the first page of CheckedIn: sign in with a code sent to your CIT-U email.
// Covers FR-001 and FR-002. Step 1 asks for the email, step 2 for the code.
// The GitHub button is added in step 1f; the name form in 1g; the dashboards in 1i.
import { useEffect, useState } from 'react'
import { getCurrentUser } from '../api/authApi.js'
import SendCodeForm from '../components/SendCodeForm.jsx'
import VerifyCodeForm from '../components/VerifyCodeForm.jsx'

export default function SignInPage() {
  // First we ask the backend whether the session cookie still signs us in
  const [isChecking, setIsChecking] = useState(true)
  const [currentUser, setCurrentUser] = useState(null)
  // Set after a code was emailed: { email, expiresInMinutes }
  const [codeSentTo, setCodeSentTo] = useState(null)

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

        {/* Temporary (step 1e): steps 1g and 1i replace this with the name form and the dashboards */}
        {!isChecking && currentUser && (
          <div>
            <h2>You are signed in</h2>
            <p>
              Signed in as <strong>{currentUser.email}</strong>.
            </p>
          </div>
        )}

        {!isChecking && !currentUser && !codeSentTo && <SendCodeForm onCodeSent={setCodeSentTo} />}

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
