// 统一导出所有API接口
export * from './auth'
export * from './user'
export * from './asset'
export * from './topology'
export * from './achievement'
export * from './range'
export * from './system'

// 导出axios实例
export { default as request } from './axios'

// 系统设置相关API
export {
  getSystemSettings,
  saveSystemSettings,
  uploadLogo,
  getSetting
} from './settings'