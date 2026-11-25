// nameRules.js — the rules for first and last names (FR-003; Increment 2 / SRS v1.4: edit profile). They are
// the same as the backend's NameRules.java, which checks again: required, at most 50 characters, letters from
// any language (ñ and accents work), spaces, hyphens, apostrophes and periods ("Ma. Cristina", "Dela Cruz").

export const NAME_MAX_LENGTH = 50

// At least one letter (\p{L} = a letter in any language, \p{M} = an accent mark), plus spaces, periods,
// apostrophes (straight ' and the curly ’ that phone keyboards type) and hyphens
const NAME_PATTERN = /^(?=.*\p{L})[\p{L}\p{M} .'’-]+$/u

/** Like the backend: no spaces at the ends, one space between words ("  Juan   Carlos " -> "Juan Carlos"). */
export function cleanUpName(name) {
  return (name ?? '').trim().replace(/\s+/g, ' ')
}

/** What is wrong with a name, in plain words, or '' when it is fine. which: 'first' or 'last'. */
export function nameProblem(name, which) {
  const cleaned = cleanUpName(name)
  if (cleaned === '') return `Please enter your ${which} name.`
  if (cleaned.length > NAME_MAX_LENGTH) return `Your ${which} name can have at most ${NAME_MAX_LENGTH} characters.`
  if (!NAME_PATTERN.test(cleaned)) {
    return `Please use only letters, spaces, hyphens (-), apostrophes (') and periods (.) in your ${which} name.`
  }
  return ''
}
