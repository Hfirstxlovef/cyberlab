// import { ElMessage } from 'element-plus'

// API错误处理工具
export class ApiErrorHandler {
  static handle403Error(error, fallbackData = null) {
    
    // 不显示错误消息，静默处理
    if (fallbackData) {
      return Promise.resolve(fallbackData)
    }
    
    return Promise.reject(error)
  }

  static handleNetworkError(error, fallbackData = null) {
    
    if (fallbackData) {
      return Promise.resolve(fallbackData)
    }
    
    return Promise.reject(error)
  }

  static createMockResponse(data) {
    return {
      data,
      status: 200,
      statusText: 'OK',
      headers: {},
      config: {}
    }
  }
}

// 通用API调用包装器
export function apiCallWithFallback(apiCall, fallbackData) {
  return apiCall().catch(error => {
    if (error.response?.status === 403) {
      return ApiErrorHandler.handle403Error(error, fallbackData)
    }
    
    if (!error.response) {
      return ApiErrorHandler.handleNetworkError(error, fallbackData)
    }
    
    throw error
  })
}

// 创建API包装器 - 兼容旧版本调用
export function createApiWrapper(apiCall, fallbackData) {
  return () => apiCallWithFallback(apiCall, fallbackData)
}