import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  // Load env file based on `mode`
  const env = loadEnv(mode, process.cwd(), '')

  // Parse allowed hosts from env (comma-separated)
  // Check both loadEnv result and process.env (for Docker)
  const allowedHostsStr = env.VITE_ALLOWED_HOSTS || process.env.VITE_ALLOWED_HOSTS
  const allowedHosts = allowedHostsStr
    ? allowedHostsStr.split(',').map(h => h.trim())
    : true  // Allow all hosts if not specified (safe for dev server)

  // Determine backend URL: use VITE_BACKEND_URL if set, otherwise construct from host/port
  // In Docker: VITE_BACKEND_URL=http://backend:8000
  // Local dev: defaults to http://127.0.0.1:8001
  const backendUrl = process.env.VITE_BACKEND_URL || 
    env.VITE_BACKEND_URL || 
    `http://127.0.0.1:${env.VITE_BACKEND_PORT || '8001'}`

  return {
    plugins: [react()],
    server: {
      host: true,
      port: 3002,
      allowedHosts,
      proxy: {
        '/api': {
          target: backendUrl,
          changeOrigin: true,
          secure: false,
        },
      },
    },
    define: {
      'process.env.VITE_API_URL': JSON.stringify(
        process.env.VITE_API_URL || '/api/v1'
      ),
    },
  }
})
