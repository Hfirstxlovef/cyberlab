/**
 * 授权管理组合式函数
 * 蟑螂恶霸团队 - 红岸网络空间安全对抗平台
 * License Management Composable for CyberLab Platform
 *
 * 遵循八耻八荣原则：
 * - 完整的错误处理
 * - 友好的用户提示
 * - 详细的日志记录
 * - 防御性编程
 */

import { ref, computed, onMounted, onUnmounted } from 'vue'
import axios from 'axios'

// 全局授权状态（跨组件共享）
const licenseInfo = ref(null)
const loading = ref(false)
const error = ref(null)
let refreshTimer = null

// 缓存相关（优化性能，避免频繁请求）
let lastFetchTime = 0
const CACHE_DURATION = 5 * 60 * 1000 // 5分钟缓存

/**
 * 授权管理钩子
 * @param {Object} options 配置选项
 * @param {Boolean} options.autoRefresh 是否自动刷新（默认 true）
 * @param {Number} options.refreshInterval 刷新间隔（毫秒，默认 300000 = 5分钟）
 * @param {Boolean} options.forceRefresh 是否强制刷新，忽略缓存（默认 false）
 * @returns {Object} 授权相关状态和方法
 */
export function useLicense(options = {}) {
  const {
    autoRefresh = true,
    refreshInterval = 300000, // 5分钟刷新一次（优化性能）
    forceRefresh = false
  } = options

  /**
   * 获取授权信息
   * 遵循八耻八荣原则：完整的错误处理和用户友好的降级策略
   * 优化：添加缓存机制，减少不必要的请求
   */
  const fetchLicenseInfo = async () => {
    // 缓存优化：如果不是强制刷新且缓存有效，直接返回
    const now = Date.now()
    if (!forceRefresh && licenseInfo.value && (now - lastFetchTime < CACHE_DURATION)) {
      return
    }

    loading.value = true
    error.value = null

    try {
      // 八耻八荣：添加超时控制
      const response = await axios.get('/api/settings/license/validate', {
        timeout: 10000 // 10秒超时
      })

      // 八耻八荣：完整的响应验证
      if (!response) {
        throw new Error('服务器无响应')
      }

      if (!response.data) {
        throw new Error('服务器返回数据为空')
      }

      // 检查响应是否包含错误
      if (response.data.success === false) {
        // 服务端返回了明确的失败状态，但这不是网络错误
        console.warn('授权验证返回失败状态:', response.data.message)
        licenseInfo.value = response.data
      } else {
        // 成功获取授权信息
        licenseInfo.value = response.data
        lastFetchTime = Date.now() // 更新缓存时间
      }

    } catch (err) {
      // 八耻八荣：详细的错误分类和处理
      console.error('授权验证失败:', err)

      let userMessage = '授权验证失败'
      let technicalError = ''

      if (err.code === 'ECONNABORTED' || err.message.includes('timeout')) {
        userMessage = '授权验证超时，请检查网络连接'
        technicalError = '请求超时'
      } else if (err.code === 'ERR_NETWORK' || err.message.includes('Network Error')) {
        userMessage = '网络连接失败，无法验证授权'
        technicalError = '网络错误'
      } else if (err.response) {
        // 服务器返回了错误响应
        const status = err.response.status
        if (status === 401 || status === 403) {
          userMessage = '无权限访问授权信息'
          technicalError = `HTTP ${status}`
        } else if (status === 404) {
          userMessage = '授权验证接口不存在'
          technicalError = 'HTTP 404'
        } else if (status >= 500) {
          userMessage = '服务器错误，请稍后重试'
          technicalError = `HTTP ${status}`
        } else {
          userMessage = '授权验证失败'
          technicalError = `HTTP ${status}`
        }
      } else if (err.message) {
        technicalError = err.message
      }

      error.value = userMessage

      // 八耻八荣：提供降级的授权信息，避免系统崩溃
      licenseInfo.value = {
        success: false,
        valid: false,
        expired: true,
        daysRemaining: 0,
        status: 'error',
        statusText: '授权异常',
        statusColor: 'danger',
        message: userMessage,
        serialNumber: '未知',
        licenseCode: '未知',
        expiryDateFormatted: '未知',
        owner: '未知',
        supportContact: '蟑螂恶霸团队',
        supportEmail: 'sun740883686@foxmail.com',
        _technicalError: technicalError // 仅用于调试，不显示给用户
      }
    } finally {
      loading.value = false
    }
  }

  /**
   * 启动自动刷新
   */
  const startAutoRefresh = () => {
    if (autoRefresh && !refreshTimer) {
      refreshTimer = setInterval(() => {
        fetchLicenseInfo()
      }, refreshInterval)
    }
  }

  /**
   * 停止自动刷新
   */
  const stopAutoRefresh = () => {
    if (refreshTimer) {
      clearInterval(refreshTimer)
      refreshTimer = null
    }
  }

  /**
   * 清除缓存并强制刷新
   */
  const forceRefreshLicense = async () => {
    lastFetchTime = 0 // 清除缓存时间
    await fetchLicenseInfo()
  }

  // 计算属性：授权是否有效
  const isValid = computed(() => {
    return licenseInfo.value?.valid === true
  })

  // 计算属性：授权是否过期
  const isExpired = computed(() => {
    return licenseInfo.value?.expired === true
  })

  // 计算属性：是否临近过期
  const isNearExpiry = computed(() => {
    return licenseInfo.value?.nearExpiry === true
  })

  // 计算属性：剩余天数
  const daysRemaining = computed(() => {
    return licenseInfo.value?.daysRemaining || 0
  })

  // 计算属性：状态文本
  const statusText = computed(() => {
    return licenseInfo.value?.statusText || '未知'
  })

  // 计算属性：状态颜色
  const statusColor = computed(() => {
    return licenseInfo.value?.statusColor || 'info'
  })

  // 计算属性：提示消息
  const message = computed(() => {
    return licenseInfo.value?.message || ''
  })

  // 计算属性：序列号
  const serialNumber = computed(() => {
    return licenseInfo.value?.serialNumber || ''
  })

  // 计算属性：授权码
  const licenseCode = computed(() => {
    return licenseInfo.value?.licenseCode || ''
  })

  // 计算属性：过期日期（格式化）
  const expiryDateFormatted = computed(() => {
    return licenseInfo.value?.expiryDateFormatted || ''
  })

  // 计算属性：持有者
  const owner = computed(() => {
    return licenseInfo.value?.owner || ''
  })

  // 计算属性：联系方式
  const supportContact = computed(() => {
    return licenseInfo.value?.supportContact || '蟑螂恶霸团队'
  })

  // 计算属性：支持邮箱
  const supportEmail = computed(() => {
    return licenseInfo.value?.supportEmail || 'sun740883686@foxmail.com'
  })

  // 组件挂载时获取授权信息
  onMounted(() => {
    fetchLicenseInfo()
    startAutoRefresh()
  })

  // 组件卸载时停止自动刷新
  onUnmounted(() => {
    stopAutoRefresh()
  })

  return {
    // 状态
    licenseInfo,
    loading,
    error,

    // 计算属性
    isValid,
    isExpired,
    isNearExpiry,
    daysRemaining,
    statusText,
    statusColor,
    message,
    serialNumber,
    licenseCode,
    expiryDateFormatted,
    owner,
    supportContact,
    supportEmail,

    // 方法
    fetchLicenseInfo,
    forceRefreshLicense, // 强制刷新（清除缓存）
    startAutoRefresh,
    stopAutoRefresh
  }
}

/**
 * 获取全局授权状态（无自动刷新）
 * 用于不需要自动刷新的场景
 *
 * 八耻八荣：提供手动刷新方法，避免创建多个 useLicense 实例导致性能问题
 */
export function getLicenseInfo() {
  /**
   * 手动刷新授权信息（强制刷新，清除缓存）
   */
  const forceRefreshLicense = async () => {
    lastFetchTime = 0 // 清除缓存时间
    loading.value = true
    error.value = null

    try {
      // 八耻八荣：添加超时控制
      const response = await axios.get('/api/settings/license/validate', {
        timeout: 10000 // 10秒超时
      })

      // 八耻八荣：完整的响应验证
      if (!response) {
        throw new Error('服务器无响应')
      }

      if (!response.data) {
        throw new Error('服务器返回数据为空')
      }

      // 检查响应是否包含错误
      if (response.data.success === false) {
        console.warn('授权验证返回失败状态:', response.data.message)
        licenseInfo.value = response.data
      } else {
        // 成功获取授权信息
        licenseInfo.value = response.data
        lastFetchTime = Date.now() // 更新缓存时间
      }

    } catch (err) {
      // 八耻八荣：详细的错误分类和处理
      console.error('授权验证失败:', err)

      let userMessage = '授权验证失败'
      let technicalError = ''

      if (err.code === 'ECONNABORTED' || err.message.includes('timeout')) {
        userMessage = '授权验证超时，请检查网络连接'
        technicalError = '请求超时'
      } else if (err.code === 'ERR_NETWORK' || err.message.includes('Network Error')) {
        userMessage = '网络连接失败，无法验证授权'
        technicalError = '网络错误'
      } else if (err.response) {
        const status = err.response.status
        if (status === 401 || status === 403) {
          userMessage = '无权限访问授权信息'
          technicalError = `HTTP ${status}`
        } else if (status === 404) {
          userMessage = '授权验证接口不存在'
          technicalError = 'HTTP 404'
        } else if (status >= 500) {
          userMessage = '服务器错误，请稍后重试'
          technicalError = `HTTP ${status}`
        } else {
          userMessage = '授权验证失败'
          technicalError = `HTTP ${status}`
        }
      } else if (err.message) {
        technicalError = err.message
      }

      error.value = userMessage

      // 八耻八荣：提供降级的授权信息，避免系统崩溃
      licenseInfo.value = {
        success: false,
        valid: false,
        expired: true,
        daysRemaining: 0,
        status: 'error',
        statusText: '授权异常',
        statusColor: 'danger',
        message: userMessage,
        serialNumber: '未知',
        licenseCode: '未知',
        expiryDateFormatted: '未知',
        owner: '未知',
        supportContact: '蟑螂恶霸团队',
        supportEmail: 'sun740883686@foxmail.com',
        _technicalError: technicalError
      }
    } finally {
      loading.value = false
    }
  }

  return {
    licenseInfo,
    loading,
    error,
    isValid: computed(() => licenseInfo.value?.valid === true),
    isExpired: computed(() => licenseInfo.value?.expired === true),
    isNearExpiry: computed(() => licenseInfo.value?.nearExpiry === true),
    daysRemaining: computed(() => licenseInfo.value?.daysRemaining || 0),
    statusText: computed(() => licenseInfo.value?.statusText || '未知'),
    statusColor: computed(() => licenseInfo.value?.statusColor || 'info'),
    message: computed(() => licenseInfo.value?.message || ''),
    serialNumber: computed(() => licenseInfo.value?.serialNumber || ''),
    licenseCode: computed(() => licenseInfo.value?.licenseCode || ''),
    expiryDateFormatted: computed(() => licenseInfo.value?.expiryDateFormatted || ''),
    owner: computed(() => licenseInfo.value?.owner || ''),
    supportContact: computed(() => licenseInfo.value?.supportContact || '蟑螂恶霸团队'),
    supportEmail: computed(() => licenseInfo.value?.supportEmail || 'sun740883686@foxmail.com'),
    // 八耻八荣：提供手动刷新方法，避免重复创建 useLicense 实例
    fetchLicenseInfo: forceRefreshLicense
  }
}
