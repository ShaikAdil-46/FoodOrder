import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig(({ mode }) => {
  const apiTarget = loadEnv(mode, '.', 'VITE_').VITE_API_PROXY_TARGET || 'http://localhost:8090'
  return {
    plugins: [react()],
    server: {
      proxy: {
        '/api': apiTarget,
        '/actuator': apiTarget,
      },
    },
  }
})
