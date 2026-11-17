// SectionForm — the fields of a section (FR-010): subject code and title, section code,
// semester and school year. Used both to create a section and to edit one.
import { useState } from 'react'
import { SEMESTER_LABELS } from '../utils/format.js'

export default function SectionForm({ initialSection, submitLabel, onSubmit }) {
  const [section, setSection] = useState(initialSection)
  const [error, setError] = useState('')
  const [isSaving, setIsSaving] = useState(false)

  function updateField(event) {
    setSection({ ...section, [event.target.name]: event.target.value })
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setIsSaving(true)
    try {
      await onSubmit(section)
    } catch (saveError) {
      // The backend explains what is wrong, e.g. "You already have IT342 G01 for this semester..."
      setError(saveError.message)
      setIsSaving(false)
    }
  }

  return (
    <form className="form" onSubmit={handleSubmit}>
      <label htmlFor="subjectCode">Subject code</label>
      <input id="subjectCode" name="subjectCode" placeholder="IT342" value={section.subjectCode} onChange={updateField} required />

      <label htmlFor="subjectTitle">Subject title</label>
      <input
        id="subjectTitle"
        name="subjectTitle"
        placeholder="Systems Integration and Architecture 1"
        value={section.subjectTitle}
        onChange={updateField}
        required
      />

      <label htmlFor="sectionCode">Section code</label>
      <input id="sectionCode" name="sectionCode" placeholder="G01" value={section.sectionCode} onChange={updateField} required />

      <label htmlFor="semester">Semester</label>
      <select id="semester" name="semester" value={section.semester} onChange={updateField}>
        {Object.entries(SEMESTER_LABELS).map(([value, label]) => (
          <option key={value} value={value}>
            {label}
          </option>
        ))}
      </select>

      <label htmlFor="schoolYear">School year</label>
      <input id="schoolYear" name="schoolYear" placeholder="2026-2027" value={section.schoolYear} onChange={updateField} required />

      <button type="submit" className="button-primary" disabled={isSaving}>
        {isSaving ? 'Saving…' : submitLabel}
      </button>
      {error && <p className="error-text" role="alert">{error}</p>}
    </form>
  )
}
