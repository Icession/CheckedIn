// App.jsx is the list of every page (route) in CheckedIn.
// To find which component shows a URL, look here first.
import { Routes, Route } from 'react-router-dom'
import SignInPage from './pages/SignInPage.jsx'
import ProfessorDashboardPage from './pages/ProfessorDashboardPage.jsx'
import SectionFormPage from './pages/SectionFormPage.jsx'
import SectionPage from './pages/SectionPage.jsx'
import LayoutListPage from './pages/LayoutListPage.jsx'
import NewLayoutPage from './pages/NewLayoutPage.jsx'
import LayoutPage from './pages/LayoutPage.jsx'
import StudentDashboardPage from './pages/StudentDashboardPage.jsx'
import SetupCheckPage from './pages/SetupCheckPage.jsx'
import NotFoundPage from './pages/NotFoundPage.jsx'

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<SignInPage />} />
      {/* FR-008: each page asks the backend, which answers 403 to the other role */}
      <Route path="/professor" element={<ProfessorDashboardPage />} />
      <Route path="/professor/sections/new" element={<SectionFormPage />} />
      <Route path="/professor/sections/:sectionId" element={<SectionPage />} />
      <Route path="/professor/sections/:sectionId/edit" element={<SectionFormPage />} />
      <Route path="/professor/layouts" element={<LayoutListPage />} />
      <Route path="/professor/layouts/new" element={<NewLayoutPage />} />
      <Route path="/professor/layouts/:layoutId" element={<LayoutPage />} />
      <Route path="/student" element={<StudentDashboardPage />} />
      <Route path="/setup" element={<SetupCheckPage />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}
