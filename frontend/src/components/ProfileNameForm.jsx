// ProfileNameForm — changes the user's first and last name (Increment 2 / SRS v1.4: edit profile).
// The names are checked here with the same rules as the backend (nameRules.js), which checks them again.
import { useState } from 'react'
import { updateProfile } from '../api/profileApi.js'
import { NAME_MAX_LENGTH, cleanUpName, nameProblem } from '../utils/nameRules.js'

export default function ProfileNameForm({ profile, onSaved }) {
  const [firstName, setFirstName] = useState(profile.firstName)
  const [lastName, setLastName] = useState(profile.lastName)
  const [message, setMessage] = useState({ text: '', isError: false })
  const [isSaving, setIsSaving] = useState(false)
  const isUnchanged = cleanUpName(firstName) === profile.firstName && cleanUpName(lastName) === profile.lastName

  async function handleSubmit(event) {
    event.preventDefault()
    const problem = nameProblem(firstName, 'first') || nameProblem(lastName, 'last')
    if (problem) {
      setMessage({ text: problem, isError: true })
      return
    }
    setIsSaving(true)
    try {
      const saved = await updateProfile(cleanUpName(firstName), cleanUpName(lastName))
      setFirstName(saved.firstName)
      setLastName(saved.lastName)
      onSaved(saved)
      setMessage({ text: 'Your name was saved.', isError: false })
    } catch (saveError) {
      setMessage({ text: saveError.message, isError: true }) // e.g. "Please enter your last name."
    }
    setIsSaving(false)
  }

  function handleChange(setName) {
    return (event) => {
      setName(event.target.value)
      setMessage({ text: '', isError: false })
    }
  }

  return (
    <form className="form" onSubmit={handleSubmit} noValidate>
      <label htmlFor="profileFirstName">First name</label>
      <input id="profileFirstName" type="text" autoComplete="given-name" maxLength={NAME_MAX_LENGTH + 10}
        value={firstName} onChange={handleChange(setFirstName)} />

      <label htmlFor="profileLastName">Last name</label>
      <input id="profileLastName" type="text" autoComplete="family-name" maxLength={NAME_MAX_LENGTH + 10}
        value={lastName} onChange={handleChange(setLastName)} />

      <p className="hint-text">Letters, spaces, hyphens, apostrophes and periods, up to {NAME_MAX_LENGTH} characters.</p>
      <button type="submit" className="button-primary" disabled={isSaving || isUnchanged}>
        {isSaving ? 'Saving…' : 'Save name'}
      </button>
      {message.text && (
        <p className={message.isError ? 'error-text' : 'notice-text'} role={message.isError ? 'alert' : 'status'}>
          {message.text}
        </p>
      )}
    </form>
  )
}
