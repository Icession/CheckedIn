// SignOutButton — signs out of CheckedIn (FR-009), with a reminder for shared computers:
// signing out of CheckedIn does not sign you out of GitHub, and GitHub would sign the next
// person in as you without asking.
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { signOut } from '../api/authApi.js'

export default function SignOutButton() {
  const navigate = useNavigate()
  const [isSigningOut, setIsSigningOut] = useState(false)

  async function handleSignOut() {
    setIsSigningOut(true)
    try {
      await signOut()
    } catch {
      // Even if the request failed, go back to the sign-in page, which shows the real state
    }
    navigate('/', { replace: true })
  }

  return (
    <div className="sign-out">
      <button type="button" className="button-secondary" onClick={handleSignOut} disabled={isSigningOut}>
        {isSigningOut ? 'Signing out…' : 'Sign out'}
      </button>
      <p className="hint-text">
        Shared computer?{' '}
        <a href="https://github.com/logout" target="_blank" rel="noreferrer">
          Sign out of GitHub
        </a>{' '}
        too.
      </p>
    </div>
  )
}
