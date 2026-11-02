import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  build: {
    cssCodeSplit: false,
    rollupOptions: {
      output: {
        entryFileNames: 'appfiles/applicationbundle.js',
        chunkFileNames: 'appfiles/applicationchunk.js',
        assetFileNames: (assetInfo) => {
          if (assetInfo.name?.endsWith('.css')) {
            return 'appfiles/applicationstyles.css'
          }
          return 'appfiles/[name][extname]'
        },
      },
    },
  },
})
