// ProfilePage — /profile, the signed-in user's own profile, for professors and students (Increment 2 / SRS v1.4).
// Shows the read-only details (ProfileDetails), lets the user change their name (ProfileNameForm) and their password
// (ChangePasswordForm), or, for an account without a password yet, points to "Forgot password?" to set one.
// After a save, the name in the page header changes too.
import { useEffect, useState } from 'react'
import { getProfile } from '../api/profileApi.js'
import PageHeader from '../components/PageHeader.jsx'
import ProfileDetails from '../components/ProfileDetails.jsx'
import ProfileNameForm from '../components/ProfileNameForm.jsx'
import ChangePasswordForm from '../components/ChangePasswordForm.jsx'
import { Link } from 'react-router-dom'
import useCurrentUser from '../hooks/useCurrentUser.js'

export default function ProfilePage() {
  const { currentUser, setCurrentUser } = useCurrentUser()
  const [profile, setProfile] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    getProfile()
      .then(setProfile)
      .catch((loadError) => setError(loadError.message))
  }, [])

  function handleSaved(savedProfile) {
    setProfile(savedProfile)
    setCurrentUser({ ...currentUser, firstName: savedProfile.firstName, lastName: savedProfile.lastName })
  }

  return (
    <main className="page">
      <PageHeader />
      {error && <p className="error-text">{error}</p>}
      {!error && !profile && <p className="muted">Loading…</p>}
      {profile && (
        <>
          <section className="card">
            <h2>My profile</h2>
            <ProfileDetails profile={profile} />
          </section>
          <section className="card">
            <h2>My name</h2>
            <ProfileNameForm profile={profile} onSaved={handleSaved} />
          </section>
          <section className="card">
            {profile.hasPassword ? (
              <>
                <h2>Change password</h2>
                <ChangePasswordForm email={profile.email} />
              </>
            ) : (
              <>
                <h2>Set a password</h2>
                <p>Your account has no password yet, so you sign in with an email code or GitHub.</p>
                <Link to="/forgot-password">Set a password with &ldquo;Forgot password?&rdquo;</Link>
              </>
            )}
          </section>
        </>
      )}
    </main>
  )
}
