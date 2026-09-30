import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({

  base: '/Snuff-AC/',
  plugins: [react()],
  build: {
    rollupOptions: {
      input: {
        main: 'index.html',
        changelog: 'changelog.html',
      },
    },
  },
})
