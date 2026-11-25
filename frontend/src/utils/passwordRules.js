// passwordRules.js — the password rules (Increment 2 / SRS v1.4), the same as the backend's PasswordRules.java,
// which checks again: 8–64 characters, at least one letter and one number, not the email, typed the same twice.

export const PASSWORD_MIN_LENGTH = 8
export const PASSWORD_MAX_LENGTH = 64

/** Shown under the password boxes. */
export const PASSWORD_RULES_HINT =
  `${PASSWORD_MIN_LENGTH}–${PASSWORD_MAX_LENGTH} characters, with at least one letter and one number. Not your email.`

/** What is wrong with a new password, in plain words (the backend's words), or '' when it is fine. */
export function newPasswordProblem(password, confirmPassword, email) {
  if (!password) return 'Please create a password.'
  if (password.length > PASSWORD_MAX_LENGTH) return `Your password can have at most ${PASSWORD_MAX_LENGTH} characters.`
  const hasLetter = /\p{L}/u.test(password)
  const hasNumber = /\d/.test(password)
  if (password.length < PASSWORD_MIN_LENGTH || !hasLetter || !hasNumber) {
    return `Your password needs at least ${PASSWORD_MIN_LENGTH} characters, with at least one letter and one number.`
  }
  if (!confirmPassword) return 'Please type your password again under "Confirm password".'
  if (password.toLowerCase() === (email ?? '').toLowerCase()) {
    return "Your password can't be your email address. Please choose another one."
  }
  if (password !== confirmPassword) return "The two passwords don't match. Please type the same password in both boxes."
  return ''
}
