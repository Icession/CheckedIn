// SectionList — the professor's active sections, each opening its own page (FR-010 – FR-012).
import { Link } from 'react-router-dom'
import { SEMESTER_LABELS } from '../utils/format.js'

export default function SectionList({ sections }) {
  if (sections.length === 0) {
    return <p className="muted">You have no sections yet. Create your first one.</p>
  }

  return (
    <ul className="item-list">
      {sections.map((section) => (
        <li key={section.sectionId}>
          <Link className="item-row" to={`/professor/sections/${section.sectionId}`}>
            <strong>
              {section.subjectCode} {section.sectionCode}
            </strong>
            <span>{section.subjectTitle}</span>
            <span className="hint-text">
              {SEMESTER_LABELS[section.semester]} {section.schoolYear} · {section.sessions.length}{' '}
              {section.sessions.length === 1 ? 'weekly session' : 'weekly sessions'}
            </span>
          </Link>
        </li>
      ))}
    </ul>
  )
}
