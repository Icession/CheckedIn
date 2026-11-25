// App.jsx is the list of every page (route) in CheckedIn.
// To find which component shows a URL, look here first.
import { Routes, Route } from 'react-router-dom'
import SignInPage from './pages/SignInPage.jsx'
import ProfessorDashboardPage from './pages/ProfessorDashboardPage.jsx'
import SectionFormPage from './pages/SectionFormPage.jsx'
import SectionPage from './pages/SectionPage.jsx'
import StudentDashboardPage from './pages/StudentDashboardPage.jsx'
import SetupCheckPage from './pages/SetupCheckPage.jsx'
import NotFoundPage from './pages/NotFoundPage.jsx'
import RequireRole from './components/RequireRole.jsx'
import ProfilePage from './pages/ProfilePage.jsx'
import ForgotPasswordPage from './pages/ForgotPasswordPage.jsx'
import ResetPasswordPage from './pages/ResetPasswordPage.jsx'

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<SignInPage />} />
      {/* Increment 2 / SRS v1.4: "Forgot password?" and the page its emailed link opens (no sign-in needed) */}
      <Route path="/forgot-password" element={<ForgotPasswordPage />} />
      <Route path="/reset-password" element={<ResetPasswordPage />} />
      {/* FR-008: RequireRole lets only one role open these pages (signed out -> sign-in page, other role ->
          their own dashboard). The backend still answers 401/403 to every request from the wrong person. */}
      <Route element={<RequireRole allowedRoles={['PROFESSOR']} />}>
        <Route path="/professor" element={<ProfessorDashboardPage />} />
        <Route path="/professor/sections/new" element={<SectionFormPage />} />
        <Route path="/professor/sections/:sectionId" element={<SectionPage />} />
        <Route path="/professor/sections/:sectionId/edit" element={<SectionFormPage />} />
      </Route>
      <Route element={<RequireRole allowedRoles={['STUDENT']} />}>
        <Route path="/student" element={<StudentDashboardPage />} />
      </Route>
      {/* Increment 2 / SRS v1.4: each user's own profile, for both roles */}
      <Route element={<RequireRole allowedRoles={['PROFESSOR', 'STUDENT']} />}>
        <Route path="/profile" element={<ProfilePage />} />
      </Route>
      <Route path="/setup" element={<SetupCheckPage />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}
