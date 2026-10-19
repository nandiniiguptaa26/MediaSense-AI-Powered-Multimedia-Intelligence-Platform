import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
    plugins: [react()],
    server: {
        port: 3000,
        proxy: {
            '/upload':     { target: 'http://localhost:8081', changeOrigin: true },
            '/convert':    { target: 'http://localhost:8080', changeOrigin: true, timeout: 3000000, proxyTimeout: 3000000 },
            '/download':   { target: 'http://localhost:8082', changeOrigin: true },
            '/qna':         { target: 'http://localhost:8083', changeOrigin: true },
        }
    }
})