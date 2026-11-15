// GitHubSignInButton — "Sign in with GitHub" (FR-001). A plain link, because the whole page
// travels to GitHub and back; a fetch() call cannot follow GitHub's sign-in pages.
import { GITHUB_SIGN_IN_URL } from '../api/authApi.js'

export default function GitHubSignInButton() {
  return (
    <div className="form">
      <a className="button-github" href={GITHUB_SIGN_IN_URL}>
        Sign in with GitHub
      </a>
      <p className="hint-text">Your GitHub account needs a verified @cit.edu email address.</p>
    </div>
  )
}
