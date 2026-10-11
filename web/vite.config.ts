import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// 参照原系统：前端调用 /prod-api，本地开发代理到后端（默认指向原 OA 便于联调）。
// 接自有后端时把 target 改成你的网关地址即可。
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/prod-api': {
        target: 'https://jgoa.aijng.com',
        changeOrigin: true,
        // 原系统接口前缀即为 /prod-api，不做 rewrite
      },
    },
  },
})
