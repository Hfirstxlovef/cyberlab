// 令牌自动刷新工具

import { getToken, getRefreshToken, setToken, setRefreshToken, clearUserInfo, getStorageType, setUserId } from './auth'
import { refreshToken as refreshTokenAPI } from '@/api/auth'
import { useAuth } from '@/composables/useAuth'

// 刷新状态管理
let isRefreshing = false
let failedQueue = []

const processQueue = (error, token = null) => {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error)
    } else {
      prom.resolve(token)
    }
  })
  
  failedQueue = []
}

// 刷新令牌的主要函数
export async function refreshAccessToken() {
  // 如果正在刷新中，返回等待的Promise
  if (isRefreshing) {
    return new Promise((resolve, reject) => {
      failedQueue.push({ resolve, reject })
    })
  }

  const refreshToken = getRefreshToken()
  if (!refreshToken) {
    return null
  }

  isRefreshing = true

  try {
    const response = await refreshTokenAPI(refreshToken)
    
    if (response && response.token && response.refreshToken) {
      const storageType = getStorageType()
      const rememberMe = storageType === 'localStorage'

      // 更新令牌
      setToken(response.token, rememberMe)
      setRefreshToken(response.refreshToken, rememberMe)

      // 更新用户ID（如果后端返回了）
      if (response.userId) {
        setUserId(response.userId, rememberMe)
      }

      // 更新全局认证状态
      const { updateAuthState } = useAuth()
      updateAuthState(response.token, response.username, response.role, response.userId)

      // 处理等待队列
      processQueue(null, response.token)

      return response.token
    } else {
      throw new Error('刷新响应格式异常')
    }
  } catch (error) {
    
    // 刷新失败，清除所有令牌信息
    clearUserInfo()
    
    // 更新全局认证状态
    const { clearAuthState } = useAuth()
    clearAuthState()
    
    // 处理等待队列
    processQueue(error, null)
    
    // 跳转到登录页
    if (typeof window !== 'undefined') {
      window.location.href = '/login'
    }
    
    return null
  } finally {
    isRefreshing = false
  }
}

// 检查令牌是否需要刷新
export function shouldRefreshToken() {
  const token = getToken()
  if (!token) return false
  
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    const currentTime = Date.now() / 1000
    const timeToExpire = payload.exp - currentTime
    
    // 如果令牌在5分钟内过期，则需要刷新
    return timeToExpire < 5 * 60
  } catch (error) {
    return true
  }
}

// 自动刷新令牌（如果需要）
export async function autoRefreshToken() {
  if (shouldRefreshToken()) {
    return await refreshAccessToken()
  }
  return getToken()
}

// 设置定时刷新
let refreshTimer = null

export function startAutoRefresh() {
  // 清除现有定时器
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
  
  // 每分钟检查一次
  refreshTimer = setInterval(async () => {
    const token = getToken()
    if (token && shouldRefreshToken()) {
      await refreshAccessToken()
    }
  }, 60000) // 1分钟
  
}

export function stopAutoRefresh() {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

// 获取令牌剩余有效时间
export function getTokenRemainingTime() {
  const token = getToken()
  if (!token) return 0
  
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    const currentTime = Date.now() / 1000
    return Math.max(0, (payload.exp - currentTime) * 1000)
  } catch (error) {
    return 0
  }
}