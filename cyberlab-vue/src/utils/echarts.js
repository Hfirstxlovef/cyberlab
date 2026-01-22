// 按需导入ECharts核心和所需组件（优化打包体积）
import * as echarts from 'echarts/core'
import { LineChart, BarChart, PieChart, RadarChart, ScatterChart, GraphChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  GridComponent,
  LegendComponent,
  DataZoomComponent,
  ToolboxComponent,
  MarkLineComponent,
  MarkPointComponent
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

// 注册所需组件
echarts.use([
  LineChart,
  BarChart,
  PieChart,
  RadarChart,
  ScatterChart,
  GraphChart,
  TitleComponent,
  TooltipComponent,
  GridComponent,
  LegendComponent,
  DataZoomComponent,
  ToolboxComponent,
  MarkLineComponent,
  MarkPointComponent,
  CanvasRenderer
])

export { echarts }

/**
 * 安全初始化ECharts实例
 * 解决"Can't get DOM width or height"警告
 * @param {HTMLElement} container - DOM容器元素
 * @param {Object} options - 初始化选项
 * @returns {Object} 初始化结果
 */
export const safeInitECharts = (container, options = {}) => {
  if (!container) {
    return { success: false, error: '容器元素不存在' }
  }

  // 确保容器可见且有尺寸
  const rect = container.getBoundingClientRect()
  const clientSize = { width: container.clientWidth, height: container.clientHeight }
  
    rect: { width: rect.width, height: rect.height },
    client: { width: clientSize.width, height: clientSize.height }
  })

  // 如果尺寸为0，设置最小尺寸
  if (clientSize.width <= 0 || clientSize.height <= 0) {
    
    if (!container.style.minWidth) {
      container.style.minWidth = '400px'
    }
    if (!container.style.minHeight) {
      container.style.minHeight = '300px'
    }
    
    // 强制重新计算布局
    container.offsetHeight
    
    // 重新检查尺寸
    const newClientSize = { width: container.clientWidth, height: container.clientHeight }
    
    if (newClientSize.width <= 0 || newClientSize.height <= 0) {
      return { 
        success: false, 
        error: '容器尺寸异常，可能被隐藏或样式设置有问题'
      }
    }
  }

  try {
    // 初始化ECharts实例
    const chartInstance = echarts.init(container, null, {
      width: 'auto',
      height: 'auto',
      ...options
    })
    
    
    return {
      success: true,
      instance: chartInstance
    }
  } catch (error) {
    return {
      success: false,
      error: error.message
    }
  }
}

/**
 * 等待DOM元素准备就绪
 * @param {HTMLElement} element - DOM元素
 * @param {number} timeout - 超时时间(ms)
 * @returns {Promise<boolean>}
 */
export const waitForElementReady = (element, timeout = 3000) => {
  return new Promise((resolve) => {
    if (!element) {
      resolve(false)
      return
    }

    const checkElement = () => {
      const rect = element.getBoundingClientRect()
      const clientSize = { width: element.clientWidth, height: element.clientHeight }
      
      if (rect.width > 0 && rect.height > 0 && clientSize.width > 0 && clientSize.height > 0) {
        resolve(true)
        return
      }

      // 继续等待
      setTimeout(checkElement, 100)
    }

    // 设置超时
    setTimeout(() => resolve(false), timeout)
    
    // 开始检查
    checkElement()
  })
}

/**
 * 安全调整ECharts尺寸
 * @param {Object} chartInstance - ECharts实例
 */
export const safeResizeChart = (chartInstance) => {
  if (chartInstance && typeof chartInstance.resize === 'function') {
    try {
      chartInstance.resize()
    } catch (error) {
    }
  }
}

/**
 * 安全销毁ECharts实例
 * @param {Object} chartInstance - ECharts实例
 */
export const safeDisposeChart = (chartInstance) => {
  if (chartInstance && typeof chartInstance.dispose === 'function') {
    try {
      chartInstance.dispose()
    } catch (error) {
    }
  }
}