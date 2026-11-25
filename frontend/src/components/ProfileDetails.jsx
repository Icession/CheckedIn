// ProfileDetails — the read-only part of the profile (Increment 2 / SRS v1.4: view profile): email, role,
// student ID (students only) and the day the account was created. Each one says why it can't be changed here.
import { formatDate } from '../utils/format.js'
import { ROLE_LABELS } from '../utils/roles.js'

export default function ProfileDetails({ profile }) {
  return (
    <dl className="profile-details">
      <dt>Email</dt>
      <dd>
        {profile.email}
        <span className="hint-text">Your sign-in identity, so it can&apos;t be changed.</span>
      </dd>

      <dt>Role</dt>
      <dd>
        {ROLE_LABELS[profile.role]}
        <span className="hint-text">
          {profile.role === 'PROFESSOR'
            ? 'You’re on the school’s list of professors.'
            : 'Professors are set by the school’s list; everyone else is a student.'}
        </span>
      </dd>

      {profile.role === 'STUDENT' && profile.studentNumber && (
        <>
          <dt>Student ID</dt>
          <dd>
            {profile.studentNumber}
            <span className="hint-text">Linked to your class records, so it can&apos;t be changed here.</span>
          </dd>
        </>
      )}

      <dt>Account created</dt>
      <dd>{formatDate(profile.createdOn)}</dd>
    </dl>
  )
}
