import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { VitePWA } from 'vite-plugin-pwa'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  // Where `npm run dev` forwards /api - a local backend by default, or the shared dev server.
  const apiTarget = env.VITE_API_TARGET || 'http://localhost:8080'

  return {
    plugins: [
      react(),
      tailwindcss(),
      // Driver's phone loses signal constantly, so the app shell is precached:
      // the app opens and the cached stop lists render after a reload with no network.
      VitePWA({
        registerType: 'autoUpdate',
        injectRegister: 'auto',
        includeAssets: ['favicon.svg', 'icons.svg'],
        manifest: {
          name: 'Waypoint Relay - Driver',
          short_name: 'Waypoint',
          description: 'Driver app for Waypoint Relay',
          theme_color: '#10131B',
          background_color: '#10131B',
          display: 'standalone',
          start_url: '/',
          icons: [
            { src: '/favicon.svg', sizes: 'any', type: 'image/svg+xml', purpose: 'any' },
          ],
        },
        workbox: {
          globPatterns: ['**/*.{js,css,html,svg,woff2}'],
          // Any route, opened offline, serves the app shell.
          navigateFallback: 'index.html',
        },
      }),
    ],
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
