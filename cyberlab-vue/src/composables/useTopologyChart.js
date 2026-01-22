import * as echarts from 'echarts'
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { withDefaultPosition } from '@/utils/topologyData'

export function useTopologyChart(canvasRef, props, emit, customElements = ref([]), selectionManager = null) {
  let chart = null
  let isUnmounted = false
  const chartRef = ref(null)
  const selectedElement = ref(null)

  // 图表状态检查
  const isChartValid = () => {
    return !!chart && !isUnmounted && !chart.isDisposed() && !!canvasRef.value && document.body.contains(canvasRef.value)
  }

  // 生成图形元素
  const generateGraphicElements = () => {
    const elements = customElements.value.map(element => {
      if (!element || !element.type || !element.id) {
        return null
      }
      
      const isSelected = selectionManager?.isElementSelected?.(element.id)
      
      // 简化坐标处理，直接使用数值
      let pixelX = element.chartX || element.left || element.x || 100
      let pixelY = element.chartY || element.top || element.y || 100

      const baseStyle = element.type === 'text' ? {
        text: element.style?.text || '新文本',
        fontSize: element.style?.fontSize || 16,
        fill: element.style?.fill || '#333',
        fontWeight: element.style?.fontWeight || 'normal',
        textAlign: element.style?.textAlign || 'left',
        textVerticalAlign: element.style?.textVerticalAlign || 'top'
      } : {
        fill: element.style?.fill || 'rgba(0, 122, 255, 0.12)',
        stroke: element.style?.stroke || 'rgba(0, 122, 255, 0.5)',
        lineWidth: element.style?.lineWidth || 2,
        ...element.style
      }

      if (isSelected) {
        baseStyle.stroke = '#409eff'
        baseStyle.lineWidth = element.type === 'text' ? 2 : 3
        baseStyle.shadowBlur = element.type === 'text' ? 8 : 10
        baseStyle.shadowColor = 'rgba(64, 158, 255, 0.5)'
      }

      // 简化图形元素配置，移除可能导致问题的属性
      if (element.type === 'text') {
        return {
          type: 'text',
          id: element.id,
          left: pixelX,
          top: pixelY,
          style: baseStyle
        }
      } else if (element.type === 'rect' && element.shape?.width && element.shape?.height) {
        return {
          type: 'rect',
          id: element.id,
          left: pixelX,
          top: pixelY,
          shape: { 
            width: element.shape.width, 
            height: element.shape.height 
          },
          style: baseStyle
        }
      } else if (element.type === 'circle' && element.shape?.r) {
        return {
          type: 'circle',
          id: element.id,
          left: pixelX,
          top: pixelY,
          shape: { 
            r: element.shape.r 
          },
          style: baseStyle
        }
      }

      return null
    }).filter(Boolean)
    
    return elements
  }

  // 渲染函数
  const render = () => {
    if (!isChartValid()) return
    
    const renderedNodes = withDefaultPosition(props.nodes || []).map(node => {
      const isSelected = selectionManager?.isNodeSelected?.(node.id)
      return isSelected ? {
        ...node,
        itemStyle: {
          ...node.itemStyle,
          borderColor: '#409eff',
          borderWidth: 3,
          shadowBlur: 10,
          shadowColor: 'rgba(64, 158, 255, 0.5)'
        }
      } : node
    })

    const option = {
      animation: false,
      series: [{
        type: 'graph',
        layout: 'none',
        roam: false,
        label: {
          show: true,
          position: 'bottom',
          distance: 12,
          fontSize: 14,
          fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
          color: '#333333',
          fontWeight: 500,
          textBorderColor: 'rgba(255, 255, 255, 0.8)',
          textBorderWidth: 1.5,
          shadowBlur: 5,
          shadowColor: 'rgba(0, 0, 0, 0.1)'
        },
        symbolKeepAspect: true,
        symbolSize: [60, 60],
        data: renderedNodes,
        links: (props.links || []).map(link => ({
          source: link.source,
          target: link.target,
          label: (() => {
            let text = ''
            if (link.label) {
              text += link.label
            }
            if (link.bandwidth) {
              text += (text ? ' | ' : '') + `带宽: ${link.bandwidth}`
            }
            return text || ''
          })(),
          lineStyle: {
            opacity: 0.9,
            width: link.lineStyle?.width || 2,
            curveness: 0,
            color: link.lineStyle?.color || '#666666',
            ...link.lineStyle
          },
          // 连线箭头配置
          symbol: link.direction === 'bidirectional' ? ['circle', 'arrow'] : 
                  link.direction === 'reverse' ? ['arrow', 'circle'] :
                  link.direction === 'none' ? ['none', 'none'] :
                  ['none', 'arrow'], // 默认单向（source到target）
          symbolSize: link.direction === 'bidirectional' ? [6, 12] : 
                     link.direction === 'none' ? [0, 0] : 
                     [0, 12]
        })),
        edgeSymbol: ['circle', 'arrow'],
        edgeSymbolSize: [4, 10],
        edgeLabel: {
          show: true,
          formatter: (params) => params.data.label || '',
          fontSize: 12
        },
        emphasis: { focus: 'adjacency' },
        itemStyle: { borderWidth: 0 }
      }]
    }

    requestAnimationFrame(() => {
      if (!isChartValid()) return
      const rect = canvasRef.value.getBoundingClientRect()
      if (rect.width === 0 || rect.height === 0) {
        setTimeout(() => isChartValid() && render(), 200)
        return
      }
      try {
        chart.setOption(option, { notMerge: true, lazyUpdate: false, silent: false })
        chart.resize()
      } catch {
        // 渲染错误处理
      }
    })
  }

  // 事件绑定
  const bindEvents = () => {
    if (!chart) return

    chart.off()
    chart.getZr().off()

    // 拖拽事件
    chart.on('dragend', (params) => {
      if (isUnmounted || !chart) return
      const node = props.nodes.find(n => n.id === params.data.id)
      if (node) {
        try {
          const pointInPixel = [params.event.offsetX, params.event.offsetY]
          const pointInGrid = chart.convertFromPixel({ seriesIndex: 0 }, pointInPixel)
          node.x = pointInGrid[0]
          node.y = pointInGrid[1]
          emit('update', { ...props })
        } catch {
          // 拖拽处理错误
        }
      }
    })

    // 自定义元素拖拽
    chart.getZr().on('dragend', (e) => {
      if (isUnmounted || !chart || !e.target?.id) return
      
      let elementId = e.target.id
      if (typeof elementId === 'string' && (elementId.endsWith('-shape') || elementId.endsWith('-label'))) {
        elementId = elementId.replace(/-shape$|-label$/, '')
      }

      const elementIndex = customElements.value.findIndex(el => el.id === elementId)
      if (elementIndex === -1) return

      const element = customElements.value[elementIndex]
      let newX, newY

      if (e.target.position && Array.isArray(e.target.position)) {
        [newX, newY] = e.target.position
      } else if (e.target.x !== undefined && e.target.y !== undefined) {
        newX = e.target.x
        newY = e.target.y
      } else if (e.target.transform && e.target.transform.length >= 6) {
        newX = e.target.transform[4]
        newY = e.target.transform[5]
      } else {
        return
      }

      const updatedElement = {
        ...element,
        chartX: newX, chartY: newY,
        left: newX, top: newY,
        x: newX, y: newY
      }

      const updatedElements = [...customElements.value]
      updatedElements[elementIndex] = updatedElement
      emit('update:customElements', updatedElements)

      if (selectedElement.value?.id === elementId) {
        selectedElement.value = updatedElement
      }
    })

    // 点击选择元素
    chart.getZr().on('click', (e) => {
      if (isUnmounted || !chart) return

      let clickedElement = null
      const clickX = e.offsetX || e.zrX || 0
      const clickY = e.offsetY || e.zrY || 0

      if (e.target?.id) {
        let elementId = e.target.id
        if (typeof elementId === 'string' && (elementId.endsWith('-shape') || elementId.endsWith('-label'))) {
          elementId = elementId.replace(/-shape$|-label$/, '')
        }
        clickedElement = customElements.value.find(el => el.id === elementId)
      }

      if (!clickedElement) {
        for (const element of customElements.value) {
          if (element.type === 'rect') {
            const chartX = element.chartX || element.left || 0
            const chartY = element.chartY || element.top || 0
            const width = element.shape?.width || 100
            const height = element.shape?.height || 60

            let clickChartX, clickChartY
            try {
              [clickChartX, clickChartY] = chart.convertFromPixel({ seriesIndex: 0 }, [clickX, clickY])
            } catch {
              clickChartX = clickX
              clickChartY = clickY
            }

            if (clickChartX >= chartX && clickChartX <= chartX + width &&
                clickChartY >= chartY && clickChartY <= chartY + height) {
              clickedElement = element
              break
            }
          }
        }
      }

      selectedElement.value = clickedElement && (clickedElement.type === 'rect' || clickedElement.type === 'circle') ? clickedElement : null
      nextTick(() => setTimeout(() => render(), 100))
    })

    // 右键菜单事件处理
    chart.getZr().on('contextmenu', (e) => {
      if (isUnmounted || !chart) return
      
      e.preventDefault()
      // ZRender右键事件触发处理

      let clickedElement = null
      const clickX = e.offsetX || e.zrX || 0
      const clickY = e.offsetY || e.zrY || 0

      // 检查是否点击了自定义图形元素
      if (e.target?.id) {
        let elementId = e.target.id
        // 获取原始元素ID
        
        if (typeof elementId === 'string' && (elementId.endsWith('-shape') || elementId.endsWith('-label'))) {
          elementId = elementId.replace(/-shape$|-label$/, '')
          // 清理元素ID后缀
        }
        
        clickedElement = customElements.value.find(el => el.id === elementId)
        // 查找自定义元素
        
        if (clickedElement) {
          // 找到右键点击的元素
          
          // 计算屏幕坐标
          const rect = canvasRef.value?.getBoundingClientRect() || { left: 0, top: 0 }
          const screenX = e.event?.clientX || (clickX + rect.left)
          const screenY = e.event?.clientY || (clickY + rect.top)
          
          // 计算屏幕坐标
          
          const eventData = {
            element: clickedElement,
            x: screenX,
            y: screenY
          }
          
          if (clickedElement.type === 'text') {
            // 发送文字右键菜单事件
            emit('text-contextmenu', eventData)
          } else if (clickedElement.type === 'rect') {
            // 发送矩形右键菜单事件
            emit('rect-contextmenu', eventData)
          } else if (clickedElement.type === 'circle') {
            // 发送圆形右键菜单事件
            emit('circle-contextmenu', eventData)
          }
          return
        }
      }

      // 如果没有点击到自定义元素，检查是否点击到节点
      // 未找到自定义元素，等待ECharts节点事件
    })

    // ECharts 图表右键事件处理（包括节点和连线）
    chart.on('contextmenu', (params) => {
      if (isUnmounted || !chart) return
      
      // ECharts右键事件处理
      
      if (params.componentType === 'series' && params.seriesType === 'graph') {
        const screenX = params.event?.event?.clientX || 0
        const screenY = params.event?.event?.clientY || 0
        
        // 计算ECharts事件屏幕坐标
        
        if (params.dataType === 'edge') {
          // 连线右键事件
          const link = params.data
          if (link) {
            const eventData = {
              link: link,
              x: screenX,
              y: screenY
            }
            // 发送连线右键菜单事件
            emit('link-contextmenu', eventData)
          }
        } else if (params.dataType === 'node') {
          // 节点右键事件
          const node = params.data
          if (node) {
            const eventData = {
              node: node,
              x: screenX,
              y: screenY
            }
            // 发送节点右键菜单事件
            emit('node-contextmenu', eventData)
          }
        }
      }
    })
  }

  // 调整大小
  const resizeChart = () => {
    requestAnimationFrame(() => {
      if (!isChartValid()) return
      try {
        chart.resize()
        setTimeout(() => isChartValid() && render(), 100)
      } catch {
        // resize失败处理
      }
    })
  }

  // 初始化
  const initChart = () => {
    if (!canvasRef.value || isUnmounted) return

    const rect = canvasRef.value.getBoundingClientRect()
    if (rect.width === 0 || rect.height === 0) {
      setTimeout(initChart, 200)
      return
    }

    if (chart && !chart.isDisposed()) {
      chart.dispose()
    }

    chartRef.value = echarts.init(canvasRef.value)
    chart = chartRef.value

    bindEvents()
    window.addEventListener('resize', resizeChart)
    
    setTimeout(() => isChartValid() && render(), 50)
  }

  // 生命周期
  onMounted(() => {
    nextTick(() => setTimeout(initChart, 100))
  })

  onBeforeUnmount(() => {
    isUnmounted = true
    window.removeEventListener('resize', resizeChart)
    if (chart && !chart.isDisposed()) {
      chart.dispose()
    }
  })

  return { render, chartRef, selectedElement, resizeChart, isChartValid, generateGraphicElements }
}