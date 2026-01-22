import { ref } from 'vue'

/**
 * 绘图工具管理 Composable
 * 管理当前选择的绘图工具和相关操作
 */
export function useDrawingTool() {
  // 当前选择的工具
  const currentTool = ref(null)
  
  // 设置当前工具
  const setCurrentTool = (tool) => {
    currentTool.value = tool
    
    // 添加视觉反馈
    if (tool) {
      document.body.style.cursor = tool === 'text' ? 'text' : 'crosshair'
    } else {
      document.body.style.cursor = 'default'
    }
  }
  
  // 创建新的文字元素
  const createTextElement = (x, y, convertPixelToChart) => {
    const { chartX, chartY } = convertPixelToChart(x, y)
    
    return {
      type: 'text',
      id: `text-${Date.now()}`,
      chartX,
      chartY,
      left: x,
      top: y,
      style: {
        text: '双击编辑文字',
        fontSize: 16,
        fill: '#333333',
        fontWeight: 'normal',
        textAlign: 'left',
        textVerticalAlign: 'top'
      },
      draggable: true,
      pinned: false
    }
  }
  
  // 创建新的矩形元素
  const createRectElement = (x, y, convertPixelToChart) => {
    const { chartX, chartY } = convertPixelToChart(x, y)
    
    return {
      type: 'rect',
      id: `rect-${Date.now()}`,
      chartX,
      chartY,
      left: x,
      top: y,
      shape: {
        width: 100,
        height: 60
      },
      style: {
        fill: 'rgba(0, 122, 255, 0.1)',
        stroke: '#007aff',
        lineWidth: 2
      },
      draggable: true
    }
  }
  
  // 创建新的圆形元素
  const createCircleElement = (x, y, convertPixelToChart) => {
    const { chartX, chartY } = convertPixelToChart(x, y)
    
    return {
      type: 'circle',
      id: `circle-${Date.now()}`,
      chartX,
      chartY,
      left: x,
      top: y,
      shape: {
        r: 30
      },
      style: {
        fill: 'rgba(255, 149, 0, 0.1)',
        stroke: '#ff9500',
        lineWidth: 2
      },
      draggable: true
    }
  }
  
  // 创建新的梯形元素
  const createTrapezoidElement = (x, y, convertPixelToChart) => {
    const { chartX, chartY } = convertPixelToChart(x, y)
    
    return {
      type: 'polygon',
      id: `trapezoid-${Date.now()}`,
      chartX,
      chartY,
      left: x,
      top: y,
      shape: {
        points: [
          [20, 0],   // 上边左点
          [80, 0],   // 上边右点
          [100, 60], // 下边右点
          [0, 60]    // 下边左点
        ]
      },
      style: {
        fill: 'rgba(255, 193, 7, 0.1)',
        stroke: '#ffc107',
        lineWidth: 2
      },
      draggable: true
    }
  }
  
  // 根据工具类型创建对应元素
  const createElement = (tool, x, y, convertPixelToChart) => {
    switch (tool) {
      case 'text':
        return createTextElement(x, y, convertPixelToChart)
      case 'rect':
        return createRectElement(x, y, convertPixelToChart)
      case 'circle':
        return createCircleElement(x, y, convertPixelToChart)
      case 'trapezoid':
        return createTrapezoidElement(x, y, convertPixelToChart)
      default:
        return null
    }
  }
  
  // 清理工具状态
  const cleanup = () => {
    // 重置鼠标样式
    document.body.style.cursor = 'default'
    currentTool.value = null
  }
  
  return {
    currentTool,
    setCurrentTool,
    createElement,
    createTextElement,
    createRectElement,
    createCircleElement,
    createTrapezoidElement,
    cleanup
  }
}