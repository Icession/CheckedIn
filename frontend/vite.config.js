import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Vite settings for the CheckedIn frontend.
// During development, any request that starts with /api is forwarded
// to the Spring Boot backend, so the browser never has to deal with CORS.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
