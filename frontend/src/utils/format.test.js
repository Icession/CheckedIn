// Unit tests for format.js. Node's built-in test runner. Run them with: npm test
import assert from 'node:assert/strict'
import { describe, test } from 'node:test'
import { formatDate } from './format.js'

describe('formatDate (the profile page, Increment 2 / SRS v1.4)', () => {
  test('a date from the backend is shown in words, without time zone shifts', () => {
    assert.equal(formatDate('2026-11-10'), 'November 10, 2026')
    assert.equal(formatDate('2027-01-01'), 'January 1, 2027')
  })
})
