// PageHeader — the top of every signed-in page: the CheckedIn name and the sign-out button.
import SignOutButton from './SignOutButton.jsx'

export default function PageHeader() {
  return (
    <header className="dashboard-header">
      <h1 className="brand">CheckedIn</h1>
      <SignOutButton />
    </header>
  )
}
