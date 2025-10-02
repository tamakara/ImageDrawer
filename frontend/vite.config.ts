import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': '/src'
    }
  },
  server: {
    port: 3000,
    open: true,
    proxy: {
      // 代理所有后端API路径到8080端口
      '/query': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false
      },
      '/image': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false
      },
      '/tag': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false
      },
      '/file': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false
      }
    }
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
    rollupOptions: {
      output: {
        manualChunks: {
          vendor: ['vue', 'pinia'],
          ui: ['element-plus']
        }
      }
    }
  }
})
