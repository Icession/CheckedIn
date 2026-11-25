// Unit tests for nameRules.js (FR-003; Increment 2 / SRS v1.4). Same cases as the backend's
// UpdateProfileRequestTest, so both sides agree. Node's built-in test runner. Run them with: npm test
import assert from 'node:assert/strict'
import { describe, test } from 'node:test'
import { cleanUpName, nameProblem } from './nameRules.js'

describe('name rules', () => {
  test('Filipino and other real names pass', () => {
    for (const name of ['Ma. Cristina', 'Dela Cruz', 'De los Santos', "O'Neil", 'O’Neil', 'Jean-Paul', 'Niño', 'Peña', 'José']) {
      assert.equal(nameProblem(name, 'first'), '', name)
    }
  })

  test('spaces are trimmed and squeezed like the backend does', () => {
    assert.equal(cleanUpName('  Ma.   Cristina '), 'Ma. Cristina')
    assert.equal(nameProblem('  Dela   Cruz ', 'last'), '')
  })

  test('a name is required', () => {
    assert.equal(nameProblem('   ', 'first'), 'Please enter your first name.')
    assert.equal(nameProblem(undefined, 'last'), 'Please enter your last name.')
  })

  test('1 to 50 characters', () => {
    assert.equal(nameProblem('A', 'first'), '')
    assert.equal(nameProblem('A'.repeat(50), 'first'), '')
    assert.equal(nameProblem('A'.repeat(51), 'last'), 'Your last name can have at most 50 characters.')
  })

  test('digits, symbols and names without a letter are refused', () => {
    for (const name of ['Juan2', '<script>', 'Juan_Carlos', '...', 'Juan@cit']) {
      assert.equal(nameProblem(name, 'first'),
        "Please use only letters, spaces, hyphens (-), apostrophes (') and periods (.) in your first name.", name)
    }
  })
})
