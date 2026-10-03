import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  // Where `npm run dev` forwards /api — a local backend by default, or the shared dev server.
  const apiTarget = env.VITE_API_TARGET || 'http://localhost:8080'

  return {
    plugins: [react(), tailwindcss()],
    server: {
      proxy: {
        '/api': {
          target: apiTarget,
          changeOrigin: true,
          // Browser requests are same-origin through Vite; the backend should not
          // treat the forwarded development request as a cross-origin request.
          configure(proxy) {
            proxy.on('proxyReq', (request) => request.removeHeader('origin'))
          },
        },
      },
    },
  }
})
