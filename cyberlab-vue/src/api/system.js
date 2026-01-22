import request from './axios'

// 系统相关API
export const getSystemLogs = (params) => {
  return request.get('/logs', { params })
}

export const getDashboardStats = () => {
  return request.get('/dashboard/stats')
}

export const healthCheck = () => {
  return request.get('/health')
}

// 获取系统设置（公共接口，无需认证）
export const getSystemSettings = () => {
  return request.get('/settings/public')
}