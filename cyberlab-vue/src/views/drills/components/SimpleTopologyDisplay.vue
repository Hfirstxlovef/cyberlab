<template>
  <div class="simple-topology-display" ref="containerRef">
    <div v-if="hasValidData" class="topology-chart" ref="chartRef"></div>
    <div v-else class="no-data">
      <el-empty description="暂无拓扑数据">
        <template #image>
          <el-icon size="80"><Connection /></el-icon>
        </template>
      </el-empty>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { Connection } from '@element-plus/icons-vue'
import * as echarts from 'echarts'

const props = defineProps({
  topologyData: {
    type: Object,
    default: () => null
  },
  height: {
    type: Number,
    default: 400
  }
})

const containerRef = ref(null)
const chartRef = ref(null)
let chartInstance = null

// 检查是否有有效的拓扑数据
const hasValidData = computed(() => {
  return props.topologyData && 
         (props.topologyData.nodes?.length > 0 || props.topologyData.links?.length > 0)
})

// 设备图标映射
const getDeviceIcon = (nodeType) => {
  const iconMap = {
    'server': 'image:///icons/storage_server.png',
    'firewall': 'image:///icons/firewall.png',
    'dns': 'image:///icons/DNS.png',
    'pc': 'image:///icons/laptop.png',
    'router': 'image:///icons/router.png',
    'database': 'image:///icons/database.png',
    'mail': 'image:///icons/mail_server.png',
    'switch_fiber': 'image:///icons/fiber_switch.png',
    'switch_ethernet': 'image:///icons/ethernet_switch.png',
    'web': 'image:///icons/webserver.png'
  }
  
  const iconPath = iconMap[nodeType?.toLowerCase()] || 'image:///icons/pc.png'
  
  // 添加调试信息
    nodeType,
    iconPath
  })
  
  return iconPath
}

// 处理节点数据
const processNodes = () => {
  if (!props.topologyData?.nodes) return []
  
  return props.topologyData.nodes.map(node => ({
    id: node.id,
    name: node.name || node.id,
    symbol: getDeviceIcon(node.type || node.iconName),
    symbolSize: [50, 50],
    x: node.x,
    y: node.y,
    itemStyle: {
      color: '#409EFF'
    },
    label: {
      show: true,
      position: 'bottom',
      fontSize: 12,
      color: '#606266'
    }
  }))
}

// 处理连线数据
const processLinks = () => {
  if (!props.topologyData?.links) return []
  
  return props.topologyData.links.map(link => ({
    source: link.source,
    target: link.target,
    label: {
      show: !!link.label,
      formatter: link.label || ''
    },
    lineStyle: {
      color: '#909399',
      width: 2
    }
  }))
}

// 初始化图表
const initChart = async () => {
  if (!chartRef.value || !hasValidData.value) return

  await nextTick()
  
  if (chartInstance) {
    chartInstance.dispose()
  }

  chartInstance = echarts.init(chartRef.value)
  
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: (params) => {
        if (params.dataType === 'node') {
          return `${params.data.name}<br/>类型: 网络设备`
        } else if (params.dataType === 'edge') {
          return `连接: ${params.data.source} → ${params.data.target}`
        }
        return ''
      }
    },
    series: [{
      type: 'graph',
      layout: 'none', // 使用固定布局
      symbolSize: 50,
      roam: true, // 启用缩放和平移
      data: processNodes(),
      links: processLinks(),
      emphasis: {
        focus: 'adjacency',
        itemStyle: {
          borderColor: '#409EFF',
          borderWidth: 2
        }
      },
      lineStyle: {
        color: '#909399',
        width: 2,
        curveness: 0
      }
    }]
  }

  chartInstance.setOption(option)
  
  // 自适应大小
  const resizeChart = () => {
    if (chartInstance) {
      chartInstance.resize()
    }
  }
  
  window.addEventListener('resize', resizeChart)
  
  // 清理函数
  onUnmounted(() => {
    window.removeEventListener('resize', resizeChart)
  })
}

// 监听数据变化
watch(() => props.topologyData, () => {
  if (hasValidData.value) {
    initChart()
  }
}, { deep: true })

// 监听容器高度变化
watch(() => props.height, () => {
  if (chartInstance) {
    chartInstance.resize()
  }
})

onMounted(() => {
  if (hasValidData.value) {
    initChart()
  }
})

onUnmounted(() => {
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
})
</script>

<style scoped>
.simple-topology-display {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.topology-chart {
  width: 100%;
  height: 100%;
  min-height: 300px;
}

.no-data {
  width: 100%;
  height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .topology-chart {
    min-height: 250px;
  }
  
  .no-data {
    height: 250px;
  }
}
</style>