import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  build: {
    cssCodeSplit: false,
    rollupOptions: {
      output: {        entryFileNames: 'assets/applicationbundle.js',
        chunkFileNames: 'assets/applicationchunk.js',
        assetFileNames: (assetInfo) => {
          if (assetInfo.name?.endsWith('.css')) {
            return 'assets/applicationstyles.css'
          }
          return 'assets/[name][extname]'
        },
      },
    },
  },
})


