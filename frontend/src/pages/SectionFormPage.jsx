// SectionFormPage — create a section at /professor/sections/new, or edit one at
// /professor/sections/:sectionId/edit. Covers FR-010 and FR-011 (archived sections can't be edited).
import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { createSection, getSection, updateSection } from '../api/sectionApi.js'
import PageHeader from '../components/PageHeader.jsx'
import SectionForm from '../components/SectionForm.jsx'
import { currentSchoolYear } from '../utils/format.js'

export default function SectionFormPage() {
  const { sectionId } = useParams()
  const isEditing = sectionId !== undefined
  const navigate = useNavigate()
  const [initialSection, setInitialSection] = useState(() =>
    isEditing
      ? null
      : { subjectCode: '', subjectTitle: '', sectionCode: '', semester: 'FIRST', schoolYear: currentSchoolYear() },
  )
  const [loadError, setLoadError] = useState('')

  useEffect(() => {
    if (!isEditing) return
    getSection(sectionId)
      .then((section) => {
        if (section.archived) {
          setLoadError('This section is archived, so it can no longer be changed.')
          return
        }
        const { subjectCode, subjectTitle, sectionCode, semester, schoolYear } = section
        setInitialSection({ subjectCode, subjectTitle, sectionCode, semester, schoolYear })
      })
      .catch((error) => (error.status === 401 ? navigate('/', { replace: true }) : setLoadError(error.message)))
  }, [isEditing, sectionId, navigate])

  async function handleSubmit(section) {
    const saved = isEditing ? await updateSection(sectionId, section) : await createSection(section)
    navigate(`/professor/sections/${saved.sectionId}`)
  }

  return (
    <main className="page">
      <PageHeader />
      <Link className="back-link" to={isEditing ? `/professor/sections/${sectionId}` : '/professor'}>
        ← Back
      </Link>
      <section className="card narrow-card">
        <h2>{isEditing ? 'Edit section' : 'New section'}</h2>
        {loadError && <p className="error-text">{loadError}</p>}
        {!loadError && !initialSection && <p className="muted">Loading…</p>}
        {initialSection && (
          <SectionForm
            initialSection={initialSection}
            submitLabel={isEditing ? 'Save changes' : 'Create section'}
            onSubmit={handleSubmit}
          />
        )}
      </section>
    </main>
  )
}
