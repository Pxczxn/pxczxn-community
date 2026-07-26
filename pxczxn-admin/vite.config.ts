import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

const backendTarget = process.env.VITE_DEV_BACKEND_URL || 'http://localhost:8849'

export default defineConfig({
  plugins: [vue()],
  base: '/',
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  build: {
    outDir: resolve(__dirname, '../pxczxn-backend/mars-starter/src/main/resources/static'),
    emptyOutDir: true
  },
  server: {
    port: 8848,
    strictPort: true,
    proxy: {
      '/api': {
        target: backendTarget,
        changeOrigin: true
      },
      '/admin-api': {
        target: backendTarget,
        changeOrigin: true
      },
      '/ws': {
        target: backendTarget.replace(/^http/, 'ws'),
        ws: true,
        changeOrigin: true
      }
    }
  }
})
