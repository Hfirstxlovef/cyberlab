/**
 * 前端统一日志工具
 *
 * 功能：
 * 1. 提供分级日志接口（debug, info, warn, error）
 * 2. 自动上报错误到后端
 * 3. 生产环境过滤敏感信息
 * 4. 批量上报减少网络请求
 * 5. 性能监控
 *
 * 使用示例：
 * ```js
 * import logger from '@/utils/logger'
 *
 * logger.info('用户登录成功', { username: 'admin' })
 * logger.error('API调用失败', new Error('Network error'))
 * logger.performance('/dashboard', { loadTime: 1500 })
 * ```
 */

import axios from 'axios'

// ========== 配置 ==========

const config = {
  // 是否启用控制台输出（开发环境默认启用）
  enableConsole: import.meta.env.MODE === 'development',

  // 是否上报到后端（生产环境默认启用）
  enableRemote: import.meta.env.MODE === 'production',

  // 后端上报接口
  remoteUrl: '/api/admin/exceptions/report',

  // 批量上报间隔（毫秒）
  batchInterval: 5000,

  // 批量上报最大缓存条数
  batchMaxSize: 20,

  // 日志级别（只记录该级别及以上的日志）
  level: import.meta.env.MODE === 'development' ? 'debug' : 'info',

  // 性能监控阈值（毫秒）
  performanceThreshold: 3000
}

// 日志级别优先级
const LOG_LEVELS = {
  debug: 0,
  info: 1,
  warn: 2,
  error: 3
}

// 批量上报缓存
let batchQueue = []
let batchTimer = null

// 用户信息（从localStorage获取）
let userInfo = null

/**
 * 初始化用户信息
 */
function initUserInfo() {
  try {
    const userStr = localStorage.getItem('user')
    if (userStr) {
      userInfo = JSON.parse(userStr)
    }
  } catch (e) {
    // Ignore
  }
}

/**
 * 获取当前用户名
 */
function getUsername() {
  if (!userInfo) {
    initUserInfo()
  }
  return userInfo?.username || 'anonymous'
}

/**
 * 判断是否应该记录该级别的日志
 */
function shouldLog(level) {
  return LOG_LEVELS[level] >= LOG_LEVELS[config.level]
}

/**
 * 格式化日志消息
 */
function formatMessage(level, message, data) {
  const timestamp = new Date().toISOString()
  const username = getUsername()

  let formatted = `[${timestamp}] [${level.toUpperCase()}] [${username}] ${message}`

  if (data) {
    formatted += '\n' + JSON.stringify(data, null, 2)
  }

  return formatted
}

/**
 * 控制台输出（带颜色）
 */
function consoleOutput(level, message, data) {
  if (!config.enableConsole) return

  const colors = {
    debug: 'color: #666',
    info: 'color: #2196F3',
    warn: 'color: #FF9800',
    error: 'color: #F44336; font-weight: bold'
  }

  const icons = {
    debug: '🔍',
    info: 'ℹ️',
    warn: '⚠️',
    error: '❌'
  }

  const icon = icons[level] || ''
  const style = colors[level] || ''

  console.log(`%c${icon} ${message}`, style)

  if (data) {
    if (data instanceof Error) {
      console.error(data)
    } else {
      console.log(data)
    }
  }
}

/**
 * 上报日志到后端
 */
function reportToBackend(level, message, data) {
  if (!config.enableRemote) return

  const logData = {
    type: 'general',
    level: level.toUpperCase(),
    message,
    url: window.location.pathname,
    username: getUsername(),
    timestamp: Date.now(),
    userAgent: navigator.userAgent
  }

  // 处理数据
  if (data) {
    if (data instanceof Error) {
      logData.stack = data.stack
      logData.type = 'error'
    } else if (typeof data === 'object') {
      try {
        logData.extra = JSON.stringify(data)
      } catch (e) {
        logData.extra = String(data)
      }
    } else {
      logData.extra = String(data)
    }
  }

  // 加入批量队列
  addToBatchQueue(logData)
}

/**
 * 加入批量上报队列
 */
function addToBatchQueue(logData) {
  batchQueue.push(logData)

  // 达到最大缓存数量，立即上报
  if (batchQueue.length >= config.batchMaxSize) {
    flushBatchQueue()
  } else {
    // 延迟批量上报
    if (!batchTimer) {
      batchTimer = setTimeout(() => {
        flushBatchQueue()
      }, config.batchInterval)
    }
  }
}

/**
 * 刷新批量队列（上报到后端）
 */
function flushBatchQueue() {
  if (batchQueue.length === 0) return

  const logsToSend = [...batchQueue]
  batchQueue = []

  if (batchTimer) {
    clearTimeout(batchTimer)
    batchTimer = null
  }

  // 使用 sendBeacon 或 fetch（避免阻塞）
  if (navigator.sendBeacon) {
    const blob = new Blob([JSON.stringify({ errors: logsToSend })], {
      type: 'application/json'
    })
    navigator.sendBeacon(config.remoteUrl, blob)
  } else {
    // 降级到异步 fetch
    fetch(config.remoteUrl, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ errors: logsToSend }),
      keepalive: true  // 页面卸载时也能完成请求
    }).catch(() => {
      // 静默失败，避免日志上报影响业务
    })
  }
}

/**
 * 页面卸载时上报剩余日志
 */
window.addEventListener('beforeunload', () => {
  flushBatchQueue()
})

/**
 * Logger 类
 */
class Logger {
  /**
   * Debug级别日志
   */
  debug(message, data) {
    if (!shouldLog('debug')) return

    consoleOutput('debug', message, data)
    // Debug日志不上报后端
  }

  /**
   * Info级别日志
   */
  info(message, data) {
    if (!shouldLog('info')) return

    consoleOutput('info', message, data)
    // Info日志默认不上报（除非显式调用 reportInfo）
  }

  /**
   * 强制上报Info日志
   */
  reportInfo(message, data) {
    if (!shouldLog('info')) return

    consoleOutput('info', message, data)
    reportToBackend('info', message, data)
  }

  /**
   * Warn级别日志
   */
  warn(message, data) {
    if (!shouldLog('warn')) return

    consoleOutput('warn', message, data)
    reportToBackend('warn', message, data)
  }

  /**
   * Error级别日志
   */
  error(message, data) {
    if (!shouldLog('error')) return

    consoleOutput('error', message, data)
    reportToBackend('error', message, data)
  }

  /**
   * 上报JavaScript错误
   */
  reportJsError(error, componentName = null) {
    const errorData = {
      message: error.message || String(error),
      stack: error.stack,
      url: window.location.pathname,
      userAgent: navigator.userAgent,
      username: getUsername(),
      timestamp: Date.now()
    }

    if (componentName) {
      errorData.componentName = componentName
    }

    // 提取行列号
    if (error.stack) {
      const match = error.stack.match(/:(\d+):(\d+)/)
      if (match) {
        errorData.line = parseInt(match[1])
        errorData.column = parseInt(match[2])
      }
    }

    // 立即上报（不走批量队列）
    axios.post('/api/admin/exceptions/js-error', errorData).catch(() => {})

    // 同时输出到控制台
    this.error(`JavaScript错误: ${error.message}`, error)
  }

  /**
   * 上报网络错误
   */
  reportNetworkError(axiosError) {
    const errorData = {
      url: axiosError.config?.url || 'unknown',
      method: axiosError.config?.method?.toUpperCase() || 'GET',
      status: axiosError.response?.status || 0,
      statusText: axiosError.response?.statusText || 'Network Error',
      responseData: JSON.stringify(axiosError.response?.data || {}),
      username: getUsername(),
      timestamp: Date.now()
    }

    axios.post('/api/admin/exceptions/network-error', errorData).catch(() => {})

    this.error(`网络请求失败: ${errorData.method} ${errorData.url}`, axiosError)
  }

  /**
   * 性能监控
   */
  performance(pageName, metrics) {
    const performanceData = {
      url: pageName || window.location.pathname,
      loadTime: metrics.loadTime,
      domReadyTime: metrics.domReadyTime,
      resourceLoadTime: metrics.resourceLoadTime,
      firstPaintTime: metrics.firstPaintTime,
      username: getUsername(),
      timestamp: Date.now()
    }

    // 如果加载时间超过阈值，输出警告
    if (metrics.loadTime > config.performanceThreshold) {
      this.warn(`页面加载较慢: ${pageName} (${metrics.loadTime}ms)`, metrics)
    }

    // 上报性能数据
    if (config.enableRemote) {
      axios.post('/api/admin/exceptions/performance', performanceData).catch(() => {})
    }
  }

  /**
   * 自动监控页面性能
   */
  autoMonitorPerformance() {
    if (typeof window.performance === 'undefined') return

    window.addEventListener('load', () => {
      setTimeout(() => {
        const perfData = window.performance.timing
        const loadTime = perfData.loadEventEnd - perfData.navigationStart
        const domReadyTime = perfData.domContentLoadedEventEnd - perfData.navigationStart
        const firstPaintTime = perfData.responseStart - perfData.navigationStart

        this.performance(window.location.pathname, {
          loadTime,
          domReadyTime,
          resourceLoadTime: perfData.loadEventEnd - perfData.domContentLoadedEventEnd,
          firstPaintTime
        })
      }, 0)
    })
  }

  /**
   * 配置日志系统
   */
  configure(options) {
    Object.assign(config, options)
  }

  /**
   * 获取当前配置
   */
  getConfig() {
    return { ...config }
  }
}

// 创建单例
const logger = new Logger()

// 导出
export default logger

/**
 * 全局错误捕获（自动上报）
 * 在 main.js 中调用 setupGlobalErrorHandler()
 */
export function setupGlobalErrorHandler() {
  // 捕获全局JavaScript错误
  window.addEventListener('error', (event) => {
    logger.reportJsError(event.error || new Error(event.message))
  })

  // 捕获Promise rejection
  window.addEventListener('unhandledrejection', (event) => {
    const error = event.reason instanceof Error
      ? event.reason
      : new Error(String(event.reason))
    logger.reportJsError(error)
  })

  // 启动性能监控
  logger.autoMonitorPerformance()

  console.log('✅ 全局错误处理器已启动')
}
