<template>
  <div ref="chartRef" class="topology-chart"></div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  nodes: {
    type: Array,
    default: () => []
  },
  links: {
    type: Array,
    default: () => []
  },
  scale: {
    type: Number,
    default: 1
  },
  layout: {
    type: String,
    default: 'none'
  },
  curveness: {
    type: Number,
    default: 0
  }
})

const emit = defineEmits(['node-contextmenu', 'link-contextmenu', 'canvas-click'])

const chartRef = ref(null)
let chart = null
let initAttempts = 0
let isInitializing = false  // 防止并发初始化
let isDisposed = false      // 跟踪实例是否已被销毁

// 初始化图表
const initChart = () => {
  if (!chartRef.value) {
    return
  }

  // 防止并发初始化
  if (isInitializing) {
    return
  }

  // 防止无限循环
  initAttempts++
  if (initAttempts > 20) {
    console.warn(`ECharts初始化尝试次数过多，停止重试 ${initAttempts}`)
    isInitializing = false
    return
  }

  isInitializing = true

  // 确保容器有足够的尺寸
  const container = chartRef.value
  const parent = container.parentElement

  // 检查容器和父容器的尺寸
  const containerRect = container.getBoundingClientRect()
  const parentRect = parent?.getBoundingClientRect()

  // 强制解析CSS calc()值为具体像素值
  const forceContainerResize = () => {
    // 强制触发CSS重新计算
    container.style.display = 'none'
    container.offsetHeight // 强制重排
    container.style.display = ''

    // 获取计算后的实际尺寸
    const computedStyle = window.getComputedStyle(container)
    const computedWidth = computedStyle.width
    const computedHeight = computedStyle.height


    // 如果calc()还未生效，设置最小尺寸
    if (container.getBoundingClientRect().height < 100) {
      container.style.minHeight = '500px'
      container.style.minWidth = '400px'
      container.style.height = '500px'
    }
  }

  // 如果容器高度为0或宽度为0，强制设置尺寸
  if (containerRect.height === 0 || containerRect.width === 0) {
    forceContainerResize()

    // 等待样式生效后再初始化
    setTimeout(() => {
      initChart()
    }, 50)
    return
  }

  // 即使容器有尺寸，也执行一次强制计算确保稳定
  forceContainerResize()

  // 销毁已存在的实例
  if (chart && !isDisposed) {
    try {
      chart.dispose()
    } catch (error) {
      console.warn('销毁ECharts实例时出错:', error)
    }
    chart = null
  }

  // 重置状态
  isDisposed = false

  try {
    chart = echarts.init(chartRef.value)

    // 绑定事件
    bindEvents()
    updateChart()

    // 标记初始化完成
    isInitializing = false

    // 初始化完成后立即触发一次resize
    setTimeout(() => {
      if (chart && !isDisposed) {
        chart.resize()
        // 主动触发全局resize事件模拟全屏切换效果
        window.dispatchEvent(new Event('resize'))

        // 检查图表可见性并尝试修复
        setTimeout(() => {
          // 如果发现图表不可见，尝试修复
          const canvases = chartRef.value?.querySelectorAll('canvas')
          if (canvases && canvases.length > 0) {
            let hasVisibleCanvas = false
            canvases.forEach(canvas => {
              const style = window.getComputedStyle(canvas)
              if (style.visibility !== 'hidden' && style.display !== 'none' &&
                  style.opacity !== '0' && canvas.width > 0 && canvas.height > 0) {
                hasVisibleCanvas = true
              }
            })

            if (!hasVisibleCanvas) {
              console.warn('检测到图表不可见，尝试修复...')
              // 强制重新渲染
              chart.resize()
              setTimeout(() => {
                chart.resize()
              }, 200)
            }
          }
        }, 300)
      }
    }, 100)
  } catch (error) {
    console.error('ECharts初始化失败:', error)
    isInitializing = false
    chart = null
  }
}

// 绑定事件
const bindEvents = () => {
  if (!chart) return
  
  // 点击事件
  chart.on('click', (params) => {
    if (params.event) {
      emit('canvas-click', {
        x: params.event.offsetX,
        y: params.event.offsetY,
        event: params.event
      })
    }
  })
  
  // 空白区域点击事件
  chart.getZr().on('click', (params) => {
    emit('canvas-click', {
      x: params.offsetX,
      y: params.offsetY,
      event: params
    })
  })
  
  // ECharts右键事件处理
  chart.on('contextmenu', (params) => {
    // ECharts右键事件处理
    
    if (params.componentType === 'series' && params.seriesType === 'graph') {
      const screenX = params.event?.event?.clientX || 0
      const screenY = params.event?.event?.clientY || 0
      
      if (params.dataType === 'edge') {
        // 连线右键事件
        const link = params.data
        if (link) {
          emit('link-contextmenu', {
            link,
            x: screenX,
            y: screenY
          })
        }
      } else if (params.dataType === 'node') {
        // 节点右键事件  
        const node = params.data
        if (node) {
          emit('node-contextmenu', {
            node,
            x: screenX,
            y: screenY
          })
        }
      }
    }
  })
}


// 更新图表
const updateChart = () => {
  if (!chart) return
  
  // 更新图表，处理连线数据
  
  const option = {
    backgroundColor: '#ffffff',
    series: [{
      type: 'graph',
      layout: props.layout,
      data: props.nodes.map(node => {
        // 修复和标准化图标路径格式
        let symbol = node.symbol || 'circle'
        
        if (symbol.includes('image:')) {
          // 处理各种可能的路径格式问题
          if (symbol.includes('image:///')) {
            // 三斜杠格式，转换为两斜杠
            symbol = symbol.replace('image:///', 'image://')
          } else if (symbol.startsWith('image://icons/') && !symbol.startsWith('image:///')) {
            // 缺少前导斜杠的情况，补充前导斜杠
            symbol = symbol.replace('image://icons/', 'image:///icons/')
          }
          
          // 验证图标路径是否包含文件扩展名
          if (!symbol.includes('.png') && !symbol.includes('.svg') && !symbol.includes('.jpg')) {
          }
          
        }
        
        return {
          id: node.id,
          name: node.name,
          x: props.layout === 'none' ? (node.x || 100) : undefined,
          y: props.layout === 'none' ? (node.y || 100) : undefined,
          symbol: symbol,
          symbolSize: node.symbolSize || [60, 60],
          draggable: props.layout === 'none',
          itemStyle: {
            color: '#409EFF'
          },
          label: {
            show: true,
            position: 'bottom',
            fontSize: 12
          }
        }
      }),
      links: props.links.map(link => ({
        source: link.source,
        target: link.target,
        label: {
          show: !!(link.label || link.bandwidth),
          formatter: () => {
            let text = ''
            if (link.label) {
              text += link.label
            }
            if (link.bandwidth) {
              text += (text ? ' | ' : '') + `带宽: ${link.bandwidth}`
            }
            return text || ''
          },
          fontSize: 10
        },
        lineStyle: {
          color: link.lineStyle?.color || '#606266',
          width: link.lineStyle?.width || 2,
          curveness: link.lineStyle?.curveness ?? props.curveness,
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
      roam: true,
      emphasis: {
        focus: 'adjacency'
      },
      // 力导向布局配置
      force: props.layout === 'force' ? {
        repulsion: Math.max(100, props.nodes.length * 20),
        gravity: 0.1,
        edgeLength: Math.max(100, props.nodes.length * 5),
        layoutAnimation: true
      } : undefined,
      // 环形布局配置
      circular: props.layout === 'circular' ? {
        rotateLabel: true
      } : undefined
    }]
  }
  
  chart.setOption(option, true)

  // 添加图标加载状态监控
  chart.on('finished', () => {
    
    // 检查所有image://类型的symbol是否正确渲染
    const imageNodes = props.nodes.filter(node => 
      node.symbol && node.symbol.includes('image:')
    )
    
    if (imageNodes.length > 0) {
    }
  })
}

// 处理窗口大小变化
let resizeTimer = null
const handleResize = () => {
  if (resizeTimer) clearTimeout(resizeTimer)
  resizeTimer = setTimeout(() => {
    if (chart) {
      chart.resize()
    }
  }, 150)
}

// 暴露图表实例给父组件
const getChart = () => chart

// 组件挂载
onMounted(() => {
  initAttempts = 0

  // 使用多重延迟确保容器完全准备好
  nextTick(() => {
    // 第一次尝试
    setTimeout(() => {
      initChart()
    }, 50)

    // 备用尝试，确保在页面完全加载后初始化
    setTimeout(() => {
      if (!chart && chartRef.value) {
        initChart()
      }
    }, 500)
  })

  window.addEventListener('resize', handleResize)

  // 监听全屏变化事件
  document.addEventListener('fullscreenchange', () => {
    setTimeout(() => {
      if (chart) {
        chart.resize()
      }
    }, 100)
  })

  // 启动最终可见性检查
  setTimeout(ensureChartVisibility, 2000) // 2秒后启动最终检查
})

// 监听props变化
watch(() => [props.nodes, props.links, props.layout, props.curveness], () => {
  if (chart) {
    updateChart()
  } else if (chartRef.value) {
    // 如果图表还未初始化但容器存在，尝试初始化
    initChart()
  }
}, { deep: true })

// 添加最终的图表可见性保障机制
const ensureChartVisibility = () => {
  if (!chart || !chartRef.value) return

  // 延迟检查图表是否真正可见
  setTimeout(() => {
    const container = chartRef.value
    const containerRect = container.getBoundingClientRect()
    const canvases = container.querySelectorAll('canvas')


    // 如果有数据但图表不可见，强制修复
    if (props.nodes.length > 0 && containerRect.height > 100) {
      let needsRepair = false

      if (canvases.length === 0) {
        console.warn('警告：容器中没有找到canvas元素')
        needsRepair = true
      } else {
        // 检查canvas是否真正可见
        const hasValidCanvas = Array.from(canvases).some(canvas => {
          const canvasStyle = window.getComputedStyle(canvas)
          return canvas.width > 0 && canvas.height > 0 &&
                 canvasStyle.display !== 'none' &&
                 canvasStyle.visibility !== 'hidden'
        })

        if (!hasValidCanvas) {
          console.warn('警告：所有canvas元素都不可见')
          needsRepair = true
        }
      }

      if (needsRepair) {
        // 强制重新初始化
        setTimeout(() => {
          initAttempts = 0
          isInitializing = false
          forceReinit()
        }, 100)
      }
    }
  }, 1000) // 1秒后进行最终检查
}


// 组件卸载时清理
onUnmounted(() => {
  isDisposed = true
  if (chart) {
    try {
      chart.dispose()
    } catch (error) {
      console.warn('销毁ECharts实例时出错:', error)
    }
    chart = null
  }
  if (resizeTimer) clearTimeout(resizeTimer)
  window.removeEventListener('resize', handleResize)
  document.removeEventListener('fullscreenchange', handleResize)
})

// 暴露强制重新初始化方法
const forceReinit = () => {
  initAttempts = 0
  isInitializing = false
  isDisposed = false
  initChart()
}

// 检查实例状态的方法
const getChartStatus = () => {
  return {
    hasChart: !!chart,
    isInitializing,
    isDisposed,
    initAttempts,
    containerExists: !!chartRef.value
  }
}

// 暴露resize方法
const resize = () => {
  if (chart) {
    chart.resize()
  }
}

// 暴露给父组件的方法
defineExpose({
  getChart,
  updateChart,
  forceReinit,
  resize,
  getChartStatus
})
</script>

<style scoped>
.topology-chart {
  width: 100%;
  height: 100%;
  min-height: 400px;
  /* 确保容器有稳定的显示框架 */
  min-width: 400px;
  position: relative;
  /* 防止内容溢出 */
  overflow: hidden;
  /* 确保容器立即可见 */
  background-color: transparent;
}
</style>