// 环境配置
const config = {
  // API基础URL
  API_BASE_URL: import.meta.env.VITE_API_BASE_URL || 'https://localhost:8443',
  
  // 请求超时时间
  TIMEOUT: import.meta.env.MODE === 'production' ? 10000 : 15000,
  
  // 应用标题
  APP_TITLE: import.meta.env.VITE_APP_TITLE || 'CyberLab',
  
  // 当前环境
  NODE_ENV: import.meta.env.MODE,
  
  // 是否为开发环境
  isDevelopment: import.meta.env.DEV,
  
  // 是否为生产环境
  isProduction: import.meta.env.PROD
}

export default config