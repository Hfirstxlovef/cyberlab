import { ref } from 'vue'

/**
 * 元素调整大小功能 Composable  
 * 管理矩形和圆形元素的调整大小功能
 */
export function useElementResize(chartRef) {
  // 当前选中的元素
  const selectedElement = ref(null)

  // 调整大小状态
  const resizeState = ref({
    isResizing: false,
    element: null,
    handle: null,
    startPos: { x: 0, y: 0 },
    originalSize: null
  })

  // 获取矩形控制点
  const getRectHandles = (rectElement) => {
    if (!rectElement || !chartRef.value) return []

    const { chartX, chartY, shape } = rectElement
    const { width, height } = shape

    const canvasRect = chartRef.value.getDom().getBoundingClientRect()
    if (!canvasRect) return []

    // 将图表坐标转换为像素坐标
    let pixelCoords
    try {
      pixelCoords = chartRef.value.convertToPixel({ seriesIndex: 0 }, [chartX, chartY])
    } catch (error) {
      pixelCoords = [chartX, chartY]
    }

    const x = pixelCoords[0]
    const y = pixelCoords[1]

    // 尝试转换宽高以适配缩放
    let pixelWidth, pixelHeight
    try {
      const rightCoords = chartRef.value.convertToPixel({ seriesIndex: 0 }, [chartX + width, chartY])
      const bottomCoords = chartRef.value.convertToPixel({ seriesIndex: 0 }, [chartX, chartY + height])
      pixelWidth = Math.abs(rightCoords[0] - x)
      pixelHeight = Math.abs(bottomCoords[1] - y)
    } catch (error) {
      pixelWidth = width
      pixelHeight = height
    }

    const handleSize = 8
    const half = handleSize / 2

    return [
      // 四个角
      {
        x: x - half,
        y: y - half,
        direction: 'nw',
        cursor: 'nw-resize'
      },
      {
        x: x + pixelWidth - half,
        y: y - half,
        direction: 'ne',
        cursor: 'ne-resize'
      },
      {
        x: x - half,
        y: y + pixelHeight - half,
        direction: 'sw',
        cursor: 'sw-resize'
      },
      {
        x: x + pixelWidth - half,
        y: y + pixelHeight - half,
        direction: 'se',
        cursor: 'se-resize'
      },
      // 四个边的中点
      {
        x: x + pixelWidth / 2 - half,
        y: y - half,
        direction: 'n',
        cursor: 'n-resize'
      },
      {
        x: x + pixelWidth - half,
        y: y + pixelHeight / 2 - half,
        direction: 'e',
        cursor: 'e-resize'
      },
      {
        x: x + pixelWidth / 2 - half,
        y: y + pixelHeight - half,
        direction: 's',
        cursor: 's-resize'
      },
      {
        x: x - half,
        y: y + pixelHeight / 2 - half,
        direction: 'w',
        cursor: 'w-resize'
      }
    ]
  }

  // 获取圆形控制点
  const getCircleHandles = (circleElement) => {
    if (!circleElement || !chartRef.value) return []

    const { chartX, chartY, shape } = circleElement
    const { r: radius } = shape

    const canvasRect = chartRef.value.getDom().getBoundingClientRect()
    if (!canvasRect) return []

    // 将图表坐标转换为像素坐标
    let pixelCoords
    try {
      pixelCoords = chartRef.value.convertToPixel({ seriesIndex: 0 }, [chartX, chartY])
    } catch (error) {
      pixelCoords = [chartX, chartY]
    }

    const centerX = pixelCoords[0]
    const centerY = pixelCoords[1]

    // 尝试转换半径以适配缩放
    let pixelRadius
    try {
      const radiusTestCoords = chartRef.value.convertToPixel({ seriesIndex: 0 }, [chartX + radius, chartY])
      pixelRadius = Math.abs(radiusTestCoords[0] - centerX)
    } catch (error) {
      pixelRadius = radius
    }

    const handleSize = 8
    const half = handleSize / 2

    return [
      // 四个方向的控制点
      {
        x: centerX - half,
        y: centerY - pixelRadius - half,
        direction: 'n'
      },
      {
        x: centerX + pixelRadius - half,
        y: centerY - half,
        direction: 'e'
      },
      {
        x: centerX - half,
        y: centerY + pixelRadius - half,
        direction: 's'
      },
      {
        x: centerX - pixelRadius - half,
        y: centerY - half,
        direction: 'w'
      }
    ]
  }

  // 开始调整大小
  const startResize = (e, element, handle) => {
    e.preventDefault()
    e.stopPropagation()

    resizeState.value = {
      isResizing: true,
      element,
      handle,
      startPos: { x: e.clientX, y: e.clientY },
      originalSize: element.type === 'rect'
        ? { width: element.shape.width, height: element.shape.height }
        : { radius: element.shape.r }
    }

    // 添加全局鼠标事件监听器
    document.addEventListener('mousemove', handleMouseMove)
    document.addEventListener('mouseup', handleMouseUp)

    // 设置光标样式
    document.body.style.cursor = handle.cursor || 'resize'
  }

  // 处理鼠标移动
  const handleMouseMove = (e) => {
    if (!resizeState.value.isResizing) return

    const { element, handle, startPos, originalSize } = resizeState.value
    const deltaX = e.clientX - startPos.x
    const deltaY = e.clientY - startPos.y

    if (element.type === 'rect') {
      handleRectResize(element, handle, deltaX, deltaY, originalSize)
    } else if (element.type === 'circle') {
      handleCircleResize(element, handle, deltaX, deltaY, originalSize)
    }
  }

  // 处理矩形调整大小
  const handleRectResize = (element, handle, deltaX, deltaY, originalSize) => {
    const { width: originalWidth, height: originalHeight } = originalSize
    let newWidth = originalWidth
    let newHeight = originalHeight

    // 根据控制点方向调整尺寸
    switch (handle.direction) {
      case 'se': // 右下角
        newWidth = Math.max(20, originalWidth + deltaX)
        newHeight = Math.max(20, originalHeight + deltaY)
        break
      case 'sw': // 左下角
        newWidth = Math.max(20, originalWidth - deltaX)
        newHeight = Math.max(20, originalHeight + deltaY)
        break
      case 'ne': // 右上角
        newWidth = Math.max(20, originalWidth + deltaX)
        newHeight = Math.max(20, originalHeight - deltaY)
        break
      case 'nw': // 左上角
        newWidth = Math.max(20, originalWidth - deltaX)
        newHeight = Math.max(20, originalHeight - deltaY)
        break
      case 'e': // 右边
        newWidth = Math.max(20, originalWidth + deltaX)
        break
      case 'w': // 左边
        newWidth = Math.max(20, originalWidth - deltaX)
        break
      case 's': // 下边
        newHeight = Math.max(20, originalHeight + deltaY)
        break
      case 'n': // 上边
        newHeight = Math.max(20, originalHeight - deltaY)
        break
    }

    // 更新元素尺寸
    element.shape.width = newWidth
    element.shape.height = newHeight
  }

  // 处理圆形调整大小
  const handleCircleResize = (element, handle, deltaX, deltaY, originalSize) => {
    const { radius: originalRadius } = originalSize

    // 计算到中心的距离变化
    const distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY)
    const isExpanding = deltaX > 0 || deltaY > 0
    const deltaRadius = isExpanding ? distance : -distance

    const newRadius = Math.max(10, originalRadius + deltaRadius * 0.5)

    // 更新元素半径
    element.shape.r = newRadius
  }

  // 处理鼠标释放
  const handleMouseUp = () => {
    if (!resizeState.value.isResizing) return

    // 清理状态
    resizeState.value = {
      isResizing: false,
      element: null,
      handle: null,
      startPos: { x: 0, y: 0 },
      originalSize: null
    }

    // 移除全局事件监听器
    document.removeEventListener('mousemove', handleMouseMove)
    document.removeEventListener('mouseup', handleMouseUp)

    // 重置光标样式
    document.body.style.cursor = 'default'

    // 触发重新渲染
    // 这里需要传入渲染函数，在使用时提供
  }

  // 设置选中元素
  const setSelectedElement = (element) => {
    selectedElement.value = element
  }

  // 清除选中
  const clearSelection = () => {
    selectedElement.value = null
  }

  // 清理资源
  const cleanup = () => {
    // 移除事件监听器
    document.removeEventListener('mousemove', handleMouseMove)
    document.removeEventListener('mouseup', handleMouseUp)

    // 重置状态
    selectedElement.value = null
    resizeState.value = {
      isResizing: false,
      element: null,
      handle: null,
      startPos: { x: 0, y: 0 },
      originalSize: null
    }

    // 重置光标样式
    document.body.style.cursor = 'default'
  }

  return {
    selectedElement,
    resizeState,
    getRectHandles,
    getCircleHandles,
    startResize,
    setSelectedElement,
    clearSelection,
    cleanup
  }
}