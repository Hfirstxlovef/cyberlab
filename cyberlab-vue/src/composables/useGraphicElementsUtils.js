import { ref } from 'vue'

export function useGraphicElementsUtils() {
  
  // 添加选中元素状态
  const selectedElement = ref(null)

  // 计算点到线段的距离
  const distanceToLine = (px, py, x1, y1, x2, y2) => {
    const A = px - x1
    const B = py - y1
    const C = x2 - x1
    const D = y2 - y1

    const dot = A * C + B * D
    const lenSq = C * C + D * D

    if (lenSq === 0) {
      return Math.sqrt(A * A + B * B)
    }

    let param = dot / lenSq

    let xx, yy

    if (param < 0) {
      xx = x1
      yy = y1
    } else if (param > 1) {
      xx = x2
      yy = y2
    } else {
      xx = x1 + param * C
      yy = y1 + param * D
    }

    const dx = px - xx
    const dy = py - yy
    return Math.sqrt(dx * dx + dy * dy)
  }

  // 智能坐标选择工具
  const getElementPosition = (element) => {
    let finalX, finalY, positionConfig

    // 优先级：chartX/chartY > x/y > left/top
    if (typeof element.chartX === 'number' && typeof element.chartY === 'number') {
      finalX = element.chartX
      finalY = element.chartY
      positionConfig = { position: [finalX, finalY] }
    } else if (typeof element.x === 'number' && typeof element.y === 'number') {
      finalX = element.x
      finalY = element.y
      positionConfig = { position: [finalX, finalY] }
    } else if (typeof element.left === 'number' && typeof element.top === 'number') {
      finalX = element.left
      finalY = element.top
      positionConfig = { left: finalX, top: finalY }
    } else {
      // 所有坐标都无效，使用默认坐标
      finalX = 100
      finalY = 100
      positionConfig = { position: [finalX, finalY] }
    }

    return { finalX, finalY, positionConfig }
  }

  // 创建文本图形元素
  const createTextGraphic = (element) => {
    const { positionConfig } = getElementPosition(element)
    
    return {
      type: 'text',
      id: `graphic-${element.id}`,
      $action: 'replace',
      ...positionConfig,
      draggable: element.draggable !== false,
      z: 1000,
      style: {
        text: element.style?.text || '新文本',
        fontSize: element.style?.fontSize || 16,
        fill: element.style?.fill || '#333',
        fontWeight: element.style?.fontWeight || 'normal',
        textAlign: element.style?.textAlign || 'left',
        textVerticalAlign: element.style?.textVerticalAlign || 'top'
      }
    }
  }

  // 创建矩形图形元素
  const createRectGraphic = (element) => {
    if (!element.shape?.width || !element.shape?.height) {
      return null
    }

    const { positionConfig } = getElementPosition(element)

    return {
      type: 'group',
      id: `graphic-${element.id}`,
      $action: 'replace',
      ...positionConfig,
      draggable: element.draggable !== false,
      z: 1000,
      children: [{
        type: 'rect',
        shape: {
          width: element.shape.width,
          height: element.shape.height
        },
        style: {
          fill: element.style?.fill || 'rgba(0, 122, 255, 0.12)',
          stroke: element.style?.stroke || 'rgba(0, 122, 255, 0.5)',
          lineWidth: element.style?.lineWidth || 2,
          ...element.style
        }
      }]
    }
  }

  // 创建圆形图形元素
  const createCircleGraphic = (element) => {
    if (!element.shape?.r) {
      return null
    }

    const { positionConfig } = getElementPosition(element)

    return {
      type: 'group',
      id: `graphic-${element.id}`,
      $action: 'replace',
      ...positionConfig,
      draggable: element.draggable !== false,
      z: 1000,
      children: [{
        type: 'circle',
        shape: {
          r: element.shape.r
        },
        style: {
          fill: element.style?.fill || 'rgba(255, 149, 0, 0.08)',
          stroke: element.style?.stroke || 'rgba(255, 149, 0, 0.6)',
          lineWidth: element.style?.lineWidth || 1.5,
          ...element.style
        }
      }]
    }
  }

  // 创建多边形图形元素
  const createPolygonGraphic = (element) => {
    const { positionConfig } = getElementPosition(element)

    return {
      type: 'polygon',
      id: `graphic-${element.id}`,
      $action: 'replace',
      ...positionConfig,
      draggable: element.draggable !== false,
      z: 1000,
      shape: {
        points: element.shape?.points || [
          [20, 0], [80, 0], [100, 60], [0, 60]
        ]
      },
      style: {
        fill: element.style?.fill || 'rgba(255, 193, 7, 0.08)',
        stroke: element.style?.stroke || 'rgba(255, 193, 7, 0.6)',
        lineWidth: element.style?.lineWidth || 1.5,
        ...element.style
      }
    }
  }

  // 处理图形元素转换
  const processGraphicElement = (element) => {
    if (!element || !element.type || !element.id) {
      return null
    }

    const { finalX, finalY } = getElementPosition(element)

    switch (element.type) {
      case 'text':
        return createTextGraphic(element)
      case 'rect':
        return createRectGraphic(element)
      case 'circle':
        return createCircleGraphic(element)
      case 'polygon':
        return createPolygonGraphic(element)
      default:
        return null
    }
  }

  // 批量处理图形元素
  const processGraphicElements = (customElements) => {
    return customElements.map(processGraphicElement).filter(element => element !== null)
  }

  // 查找最近的自定义元素
  const findNearestCustomElement = (customElements, clickX, clickY, maxDistance = 50) => {
    let nearestElement = null
    let minDistance = Infinity

    customElements.forEach(el => {
      const elementX = el.chartX || el.left || el.x || 0
      const elementY = el.chartY || el.top || el.y || 0
      const distance = Math.sqrt(
        Math.pow(elementX - clickX, 2) + Math.pow(elementY - clickY, 2)
      )

      if (distance < minDistance && distance < maxDistance) {
        minDistance = distance
        nearestElement = el
      }
    })

    return nearestElement
  }

  // 查找被点击的元素（区域检测）
  const findClickedElement = (customElements, clickX, clickY, chart) => {
    for (const element of customElements) {
      if (element.type === 'rect') {
        // 统一使用图表坐标进行检测
        const chartX = element.chartX || element.left || 0
        const chartY = element.chartY || element.top || 0
        const width = element.shape?.width || 100
        const height = element.shape?.height || 60

        // 将点击像素坐标转换为图表坐标
        let clickChartX, clickChartY
        try {
          const chartCoords = chart.convertFromPixel({ seriesIndex: 0 }, [clickX, clickY])
          clickChartX = chartCoords[0]
          clickChartY = chartCoords[1]
        } catch {
          // 如果转换失败，假设坐标系一致  
          clickChartX = clickX
          clickChartY = clickY
        }

        if (clickChartX >= chartX && clickChartX <= chartX + width &&
          clickChartY >= chartY && clickChartY <= chartY + height) {
          return element
        }
      } else if (element.type === 'circle') {
        const centerX = element.chartX || element.left || 0
        const centerY = element.chartY || element.top || 0
        const radius = element.shape?.r || 30

        // 将点击像素坐标转换为图表坐标
        let clickChartX, clickChartY
        try {
          const chartCoords = chart.convertFromPixel({ seriesIndex: 0 }, [clickX, clickY])
          clickChartX = chartCoords[0]
          clickChartY = chartCoords[1]
        } catch {
          clickChartX = clickX
          clickChartY = clickY
        }

        const distance = Math.sqrt(
          Math.pow(clickChartX - centerX, 2) + Math.pow(clickChartY - centerY, 2)
        )

        if (distance <= radius) {
          return element
        }
      }
    }
    return null
  }

  // 菜单边界检查
  const getValidMenuPosition = (screenX, screenY, menuWidth = 200, menuHeight = 300) => {
    const validX = Math.max(10, Math.min(window.innerWidth - menuWidth - 10, screenX))
    const validY = Math.max(10, Math.min(window.innerHeight - menuHeight - 10, screenY))
    return { validX, validY }
  }

  return {
    selectedElement,
    distanceToLine,
    getElementPosition,
    createTextGraphic,
    createRectGraphic,
    createCircleGraphic,
    createPolygonGraphic,
    processGraphicElement,
    processGraphicElements,
    findNearestCustomElement,
    findClickedElement,
    getValidMenuPosition
  }
}