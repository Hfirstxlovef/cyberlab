// 全局类型定义
declare global {
  // 确保Promise类型可用
  interface Window {
    // 可以在这里添加window对象的扩展
  }
}

// Vue相关类型
declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

// 环境变量类型
interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string
  readonly VITE_APP_TITLE?: string
  readonly MODE: string
  readonly DEV: boolean
  readonly PROD: boolean
  readonly SSR: boolean
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

export {}