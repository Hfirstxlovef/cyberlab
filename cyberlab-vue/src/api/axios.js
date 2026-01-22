import axios from 'axios'
import { ElMessage } from 'element-plus'
import envConfig from '../config/env'
import { getToken, removeToken } from '@/utils/auth'

// 创建 Axios 实例
const instance = axios.create({
  baseURL: '/api',
  timeout: envConfig.TIMEOUT,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器：为每个请求自动添加 Authorization 头（如果有 token）
instance.interceptors.request.use(
  (config) => {
    const token = getToken()

    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器：处理响应统一逻辑
instance.interceptors.response.use(
  (response) => {
    // blob类型（文件下载）需要返回完整response对象，避免破坏二进制数据
    if (response.config.responseType === 'blob') {
      return response
    }
    // 如果后端返回的是 { code, message, data } 结构，可在此判断 code === 200
    return response.data // 直接返回 data，方便调用方使用
  },
  (error) => {
    
    if (error.response) {
      const code = error.response.status
      const message = error.response.data?.message || '请求出错'
      const url = error.config?.url || 'unknown'

      switch (code) {
        case 401:
          ElMessage.error('登录已过期，请重新登录')
          removeToken()
          // 跳转到登录页
          if (window.location.pathname !== '/login') {
            window.location.href = '/login'
          }
          break
        case 403:
          // 检查是否有详细的错误信息
          const detailedMessage = error.response.data?.message || '权限不足，无法访问该资源'
          const errorCode = error.response.data?.code
          
          if (errorCode === 'ACCESS_DENIED') {
            const requiredRoles = error.response.data?.requiredRoles
            const currentRole = error.response.data?.currentRole
            
            // 为系统设置相关的API提供更友好的错误提示
            if (url.includes('/settings')) {
              if (requiredRoles && requiredRoles.includes('admin')) {
                ElMessage.error('系统设置功能仅限管理员使用，请联系管理员获取权限')
              } else {
                ElMessage.error(detailedMessage)
              }
            } else if (requiredRoles) {
              ElMessage.error(`${detailedMessage}。需要以下角色之一：${requiredRoles.join(', ')}`)
            } else {
              ElMessage.error(detailedMessage)
            }
            
          } else {
            ElMessage.error(detailedMessage)
          }
          break
        case 404:
          ElMessage.error('接口不存在')
          break
        case 500:
          ElMessage.error('服务器内部错误')
          break
        default:
          ElMessage.error(message)
      }
    }

    return Promise.reject(error)
  }
)

export default instance