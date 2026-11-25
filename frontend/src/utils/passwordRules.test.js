// Unit tests for passwordRules.js (Increment 2 / SRS v1.4). Same cases as the backend's SaveNameRequestTest and
// AuthServiceTest, so both sides agree. Node's built-in test runner. Run them with: npm test
import assert from 'node:assert/strict'
import { describe, test } from 'node:test'
import { newPasswordProblem } from './passwordRules.js'

const EMAIL = 'juan.delacruz@cit.edu'
const TOO_WEAK = 'Your password needs at least 8 characters, with at least one letter and one number.'

describe('password rules', () => {
  test('8–64 characters with a letter and a number pass', () => {
    for (const password of ['sunflower42', 'Ñandú2026', 'a1b2c3d4', 'correct horse battery 9', 'a1' + 'x'.repeat(62)]) {
      assert.equal(newPasswordProblem(password, password, EMAIL), '', password)
    }
  })

  test('too short, or without a letter or a number', () => {
    for (const password of ['short1', 'abcdefgh', '12345678', '        ']) {
      assert.equal(newPasswordProblem(password, password, EMAIL), TOO_WEAK, password)
    }
  })

  test('at most 64 characters', () => {
    const tooLong = 'a1' + 'x'.repeat(63)
    assert.equal(newPasswordProblem(tooLong, tooLong, EMAIL), 'Your password can have at most 64 characters.')
  })

  test('both boxes are required and must match', () => {
    assert.equal(newPasswordProblem('', '', EMAIL), 'Please create a password.')
    assert.equal(newPasswordProblem('sunflower42', '', EMAIL), 'Please type your password again under "Confirm password".')
    assert.equal(newPasswordProblem('sunflower42', 'sunflower43', EMAIL),
      "The two passwords don't match. Please type the same password in both boxes.")
  })

  test('the email itself, in any capitals, is not allowed', () => {
    const sameAsEmail = 'Juan.DelaCruz1@cit.edu'
    assert.equal(newPasswordProblem(sameAsEmail, sameAsEmail, 'juan.delacruz1@cit.edu'),
      "Your password can't be your email address. Please choose another one.")
  })
})
