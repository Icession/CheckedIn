// ChangePasswordForm — "Change password" on the profile page (Increment 2 / SRS v1.4): current password, new
// password, confirm. Checked here with the same rules as the backend, which also checks the current password and
// that the new one is different. The user stays signed in afterwards.
import { useState } from 'react'
import { changePassword } from '../api/profileApi.js'
import { PASSWORD_MAX_LENGTH, PASSWORD_RULES_HINT, newPasswordProblem } from '../utils/passwordRules.js'

const EMPTY_FORM = { currentPassword: '', newPassword: '', confirmPassword: '' }

export default function ChangePasswordForm({ email }) {
  const [form, setForm] = useState(EMPTY_FORM)
  const [message, setMessage] = useState({ text: '', isError: false })
  const [isSaving, setIsSaving] = useState(false)

  function handleChange(event) {
    setForm({ ...form, [event.target.name]: event.target.value })
    setMessage({ text: '', isError: false })
  }

  async function handleSubmit(event) {
    event.preventDefault()
    const problem = !form.currentPassword
      ? 'Please enter your current password.'
      : newPasswordProblem(form.newPassword, form.confirmPassword, email)
    if (problem) {
      setMessage({ text: problem, isError: true })
      return
    }
    setIsSaving(true)
    try {
      await changePassword(form.currentPassword, form.newPassword, form.confirmPassword)
      setForm(EMPTY_FORM)
      setMessage({ text: 'Your password was changed. Use the new one next time you sign in.', isError: false })
    } catch (changeError) {
      setMessage({ text: changeError.message, isError: true }) // e.g. "Your current password is incorrect."
    }
    setIsSaving(false)
  }

  return (
    <form className="form" onSubmit={handleSubmit} noValidate>
      <label htmlFor="currentPassword">Current password</label>
      <input id="currentPassword" name="currentPassword" type="password" autoComplete="current-password"
        value={form.currentPassword} onChange={handleChange} />
      <label htmlFor="changeNewPassword">New password</label>
      <input id="changeNewPassword" name="newPassword" type="password" autoComplete="new-password"
        maxLength={PASSWORD_MAX_LENGTH + 10} value={form.newPassword} onChange={handleChange} />
      <label htmlFor="changeConfirmPassword">Confirm new password</label>
      <input id="changeConfirmPassword" name="confirmPassword" type="password" autoComplete="new-password"
        maxLength={PASSWORD_MAX_LENGTH + 10} value={form.confirmPassword} onChange={handleChange} />
      <p className="hint-text">{PASSWORD_RULES_HINT}</p>
      <button type="submit" className="button-primary" disabled={isSaving}>
        {isSaving ? 'Saving…' : 'Change password'}
      </button>
      {message.text && (
        <p className={message.isError ? 'error-text' : 'notice-text'} role={message.isError ? 'alert' : 'status'}>
          {message.text}
        </p>
      )}
    </form>
  )
}
