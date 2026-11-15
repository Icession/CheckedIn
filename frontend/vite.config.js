import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Where the Spring Boot backend runs during development
const BACKEND_URL = 'http://localhost:8080'

// GitHub sign-in pages. changeOrigin: false keeps the browser's address (localhost:5173) in the
// request, so Spring tells GitHub to send the user back to localhost:5173, not to the backend.
const SIGN_IN_PROXY = { target: BACKEND_URL, changeOrigin: false }

// Vite settings for the CheckedIn frontend.
// During development, these paths are forwarded to the Spring Boot backend, so the browser
// sees one address (localhost:5173) and never has to deal with CORS or cross-site cookies.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': BACKEND_URL,
      // "Sign in with GitHub" starts here...
      '/oauth2': SIGN_IN_PROXY,
      // ...and GitHub sends the user back here
      '/login/oauth2': SIGN_IN_PROXY,
    },
  },
})
