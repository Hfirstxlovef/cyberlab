// API请求工具类，包含重试机制和错误处理
import { getToken, getRefreshToken, isTokenExpired } from './auth'
import { refreshAccessToken } from './tokenRefresh'

export class ApiRequest {
  constructor(options = {}) {
    this.maxRetries = options.maxRetries || 3
    this.retryDelay = options.retryDelay || 1000
    this.backoffMultiplier = options.backoffMultiplier || 2
  }

  // 延时函数
  delay(ms) {
    return new Promise(resolve => setTimeout(resolve, ms))
  }

  // 获取有效token
  async getValidToken() {
    const token = getToken()
    
    if (!token || isTokenExpired()) {
      return await refreshAccessToken()
    }
    
    return token
  }

  // 带重试的fetch请求
  async fetchWithRetry(url, options = {}, retryCount = 0) {
    try {
      // 获取有效token
      const token = await this.getValidToken()
      
      const response = await fetch(url, {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
          ...options.headers
        },
        ...options
      })

      // 处理401错误（token无效）
      if (response.status === 401 && retryCount === 0) {
        try {
          const newToken = await refreshAccessToken()
          if (newToken) {
            return this.fetchWithRetry(url, options, retryCount + 1)
          }
        } catch (refreshError) {
        }
        return response // 返回原始401响应
      }

      // 如果是网络错误或服务器错误，可以重试
      if (!response.ok && this.shouldRetry(response.status) && retryCount < this.maxRetries) {
        const delay = this.retryDelay * Math.pow(this.backoffMultiplier, retryCount)
        
        await this.delay(delay)
        return this.fetchWithRetry(url, options, retryCount + 1)
      }

      return response
    } catch (error) {
      // 网络错误可以重试
      if (retryCount < this.maxRetries && this.isNetworkError(error)) {
        const delay = this.retryDelay * Math.pow(this.backoffMultiplier, retryCount)
        
        await this.delay(delay)
        return this.fetchWithRetry(url, options, retryCount + 1)
      }
      
      throw error
    }
  }

  // 判断是否应该重试
  shouldRetry(status) {
    // 只对临时性错误重试：503服务不可用、408请求超时、429请求过多
    // 500/502是服务器bug或未实现API，重试无意义
    return status === 503 || status === 408 || status === 429
  }

  // 判断是否是网络错误
  isNetworkError(error) {
    return error instanceof TypeError && 
           (error.message.includes('NetworkError') || 
            error.message.includes('fetch') ||
            error.message.includes('Failed to fetch'))
  }

  // 安全的JSON解析
  async safeJsonParse(response) {
    try {
      return await response.json()
    } catch (error) {
      return null
    }
  }

  // 通用的API调用方法
  async get(url, options = {}) {
    try {
      const response = await this.fetchWithRetry(url, { 
        method: 'GET', 
        ...options 
      })

      if (response.ok) {
        const data = await this.safeJsonParse(response)
        return { success: true, data, status: response.status }
      } else {
        return { 
          success: false, 
          error: `HTTP ${response.status}: ${response.statusText}`,
          status: response.status,
          data: null
        }
      }
    } catch (error) {
      return { 
        success: false, 
        error: error.message,
        status: 0,
        data: null
      }
    }
  }

  // POST请求
  async post(url, data, options = {}) {
    return this.request(url, {
      method: 'POST',
      body: JSON.stringify(data),
      ...options
    })
  }

  // PUT请求
  async put(url, data, options = {}) {
    return this.request(url, {
      method: 'PUT',
      body: JSON.stringify(data),
      ...options
    })
  }

  // DELETE请求
  async delete(url, options = {}) {
    return this.request(url, {
      method: 'DELETE',
      ...options
    })
  }

  // 通用请求方法
  async request(url, options = {}) {
    try {
      const response = await this.fetchWithRetry(url, options)

      if (response.ok) {
        const data = await this.safeJsonParse(response)
        return { success: true, data, status: response.status }
      } else {
        return { 
          success: false, 
          error: `HTTP ${response.status}: ${response.statusText}`,
          status: response.status,
          data: null
        }
      }
    } catch (error) {
      return { 
        success: false, 
        error: error.message,
        status: 0,
        data: null
      }
    }
  }
}

// 创建默认实例
export const apiRequest = new ApiRequest({
  maxRetries: 3,
  retryDelay: 1000,
  backoffMultiplier: 1.5
})

// 便捷的导出函数
export const { get, post, put, delete: del } = apiRequest