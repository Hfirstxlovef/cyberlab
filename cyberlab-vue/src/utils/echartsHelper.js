// ECharts容器准备就绪检查工具函数
// 使用方法：
// import { waitForEChartsContainer } from '@/utils/echartsHelper'
// const validation = await waitForEChartsContainer(container, 'chartType')

/**
 * 等待ECharts容器准备就绪 - 避免DOM尺寸警告
 * @param {HTMLElement} container - ECharts容器元素
 * @param {string} chartType - 图表类型（用于日志）
 * @param {number} maxAttempts - 最大尝试次数
 * @returns {Promise<{valid: boolean, reason?: string, size?: {width: number, height: number}}>}
 */
export const waitForEChartsContainer = async (container, chartType = 'chart', maxAttempts = 10) => {
  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    const validation = validateEChartsContainer(container, chartType)
    if (validation.valid) {
      return validation
    }
    
    // 如果是尺寸问题，等待DOM布局完成
    if (validation.reason.includes('尺寸为0') || validation.reason.includes('尺寸不足')) {
      await new Promise(resolve => setTimeout(resolve, 50 * attempt)) // 递增延迟
      continue
    }
    
    // 其他问题直接返回
    return validation
  }
  
  // 最终尝试失败
  return { valid: false, reason: '等待容器准备超时' }
}

/**
 * ECharts容器尺寸验证
 * @param {HTMLElement} container - 容器元素
 * @param {string} chartType - 图表类型
 * @returns {{valid: boolean, reason?: string, size?: {width: number, height: number}}}
 */
export const validateEChartsContainer = (container, chartType) => {
  if (!container) {
    return { valid: false, reason: '容器不存在' }
  }

  // 检查容器是否在DOM中
  if (!document.body.contains(container)) {
    return { valid: false, reason: '容器不在DOM中' }
  }

  // 检查容器自身是否可见
  const style = window.getComputedStyle(container)
  if (style.display === 'none' || style.visibility === 'hidden') {
    return { valid: false, reason: '容器不可见' }
  }
  
  // 检查父元素链是否可见
  let parent = container.parentElement
  while (parent && parent !== document.body) {
    const parentStyle = window.getComputedStyle(parent)
    if (parentStyle.display === 'none' || parentStyle.visibility === 'hidden') {
      return { valid: false, reason: '父级容器不可见' }
    }
    parent = parent.parentElement
  }

  // 智能尺寸检测 - 多种方法综合判断
  const rect = container.getBoundingClientRect()
  const clientSize = { width: container.clientWidth, height: container.clientHeight }
  const offsetSize = { width: container.offsetWidth, height: container.offsetHeight }
  
  // ECharts警告检测：确保clientWidth和clientHeight不为0
  if (clientSize.width === 0 || clientSize.height === 0) {
    return { 
      valid: false, 
      reason: `ECharts容器尺寸为0 (client: ${clientSize.width}x${clientSize.height})`
    }
  }
  
  // 使用最大尺寸值进行判断，提高检测准确性
  const maxWidth = Math.max(rect.width, clientSize.width, offsetSize.width)
  const maxHeight = Math.max(rect.height, clientSize.height, offsetSize.height)
  
  // 基础最小尺寸要求
  const minWidth = 100
  const minHeight = 80
  
  const hasValidSize = maxWidth >= minWidth && maxHeight >= minHeight
  
  if (!hasValidSize) {
    return { 
      valid: false, 
      reason: `尺寸不足 (${Math.round(maxWidth)}x${Math.round(maxHeight)} < ${minWidth}x${minHeight})`
    }
  }
  
  return { valid: true, size: { width: maxWidth, height: maxHeight } }
}