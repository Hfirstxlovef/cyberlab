import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import fs from 'fs'

export default defineConfig(({ command, mode }) => {
  // 检查是否禁用HMR (用于WebSocket连接问题的备用方案)
  const disableHMR = process.env.DISABLE_HMR === 'true'
  
  return {
    plugins: [
      vue(),
      vueDevTools()
    ],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      },
      extensions: ['.js', '.vue', '.json', '.ts'] // 添加更多支持的扩展名
    },
    build: {
      rollupOptions: {
        // 优化代码分割
        output: {
          manualChunks: {
            vendor: ['vue', 'vue-router', 'element-plus'],
            utils: ['axios']
          }
        }
      },
      // 提高构建性能
      chunkSizeWarningLimit: 1000,
      // 生产环境移除 console 和 debugger
      minify: 'terser',
      terserOptions: {
        compress: {
          // 生产环境移除 console.* (保留 console.error 和 console.warn 用于错误追踪)
          drop_console: mode === 'production',
          drop_debugger: true,
          // 保留重要的 console 方法
          pure_funcs: mode === 'production' ? ['console.log', 'console.info', 'console.debug'] : []
        },
        format: {
          // 移除注释
          comments: false
        }
      }
    },
    server: {
      host: 'localhost',
      port: 5443,
      open: false,
      cors: true,
      // 启用 HTTPS（使用 mkcert 生成的证书）
      https: {
        key: fs.readFileSync('./localhost+2-key.pem'),
        cert: fs.readFileSync('./localhost+2.pem')
      },
      proxy: {
        '/api': {
          target: 'https://localhost:8443',
          changeOrigin: true,
          secure: false  // 自签名证书，需要设置为 false
        },
        '/uploads': {
          target: 'https://localhost:8443',
          changeOrigin: true,
          secure: false
        }
      },
      // 改善开发服务器的热更新
      hmr: disableHMR ? false : {
        port: 5443,
        host: 'localhost',
        protocol: 'wss',  // HTTPS 环境使用 wss
        overlay: true,
        // 添加WebSocket配置
        clientPort: 5443,
        // 添加超时配置
        timeout: 30000
      },
      // 添加WebSocket配置
      ws: {
        port: 5443,
        host: 'localhost'
      }
    },
    // 优化依赖预构建
    optimizeDeps: {
      include: ['vue', 'vue-router', 'element-plus', 'axios'],
      exclude: []
    }
  }
})