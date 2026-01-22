<template>
  <div class="drill-topology-viewer">
    <div v-if="topologyData && hasValidData" class="topology-container">
      <!-- ECharts拓扑图表 -->
      <div class="topology-chart" ref="chartRef" :style="{ height: height + 'px' }"></div>
      
      <!-- 拓扑信息面板 -->
      <div class="topology-info">
        <el-card size="small">
          <div class="info-item">
            <span class="info-label">节点数:</span>
            <span class="info-value">{{ nodeCount }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">连线数:</span>
            <span class="info-value">{{ linkCount }}</span>
          </div>
        </el-card>
      </div>
    </div>
    
    <div v-else class="no-data">
      <el-empty description="拓扑数据为空或格式不正确">
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
import { ElEmpty, ElCard, ElIcon } from 'element-plus'
import * as echarts from 'echarts'

const props = defineProps({
  topologyData: {
    type: Object,
    default: () => null
  },
  width: {
    type: Number,
    default: 800
  },
  height: {
    type: Number,
    default: 300
  },
  showLabels: {
    type: Boolean,
    default: true
  }
})

const chartRef = ref(null)
let chartInstance = null

// 检查是否有有效的拓扑数据
const hasValidData = computed(() => {
  const isValid = props.topologyData && 
         ((props.topologyData.nodes && props.topologyData.nodes.length > 0) || 
          (props.topologyData.links && props.topologyData.links.length > 0) ||
          (props.topologyData.customElements && props.topologyData.customElements.length > 0))
  
  if (props.topologyData) {
  }
  
  return isValid
})

// 节点数量
const nodeCount = computed(() => {
  return props.topologyData?.nodes?.length || 0
})

// 连线数量
const linkCount = computed(() => {
  return props.topologyData?.links?.length || 0
})

// 获取设备图标路径
const getDeviceIcon = (nodeType) => {
  const iconMap = {
    'server': '/icons/PcServer.png',
    'webserver': '/icons/webserver.png',
    'mail_server': '/icons/mail_server.png',
    'storage_server': '/icons/storage_server.png',
    'firewall': '/icons/firewall.png',
    'router': '/icons/router.png',
    'pc': '/icons/pc.png',
    'laptop': '/icons/laptop.png',
    'database': '/icons/database.png',
    'dns': '/icons/DNS.png',
    'switch': '/icons/ethernet_switch.png',
    'main_switch': '/icons/main_switch.png',
    'fiber_switch': '/icons/fiber_switch.png',
    'default': '/icons/pc.png'
  }
  return iconMap[nodeType?.toLowerCase()] || iconMap['default']
}

// 获取节点颜色
const getNodeColor = (nodeType) => {
  const colorMap = {
    'server': '#28a745',
    'webserver': '#28a745',
    'mail_server': '#28a745',
    'storage_server': '#28a745',
    'firewall': '#dc3545',
    'router': '#ffc107',
    'pc': '#17a2b8',
    'laptop': '#17a2b8',
    'database': '#6f42c1',
    'dns': '#fd7e14',
    'switch': '#20c997',
    'main_switch': '#20c997',
    'fiber_switch': '#20c997',
    'default': '#6c757d'
  }
  return colorMap[nodeType?.toLowerCase()] || colorMap['default']
}

// 处理节点数据为ECharts格式
const processedNodes = computed(() => {
  if (!props.topologyData?.nodes) return []
  
  return props.topologyData.nodes.map((node, index) => {
    const iconPath = getDeviceIcon(node.type)
    const symbol = `image:///icons/${iconPath.split('/').pop()}` // 统一使用三斜杠格式
    
    
    return {
      id: node.id,
      name: node.name || node.id,
      value: [node.x || (index * 100 + 100), node.y || (index * 100 + 100)],
      symbol: symbol,
      symbolSize: 40,
      itemStyle: {
        color: getNodeColor(node.type)
      },
      label: {
        show: props.showLabels,
        position: 'bottom',
        fontSize: 12,
        color: '#333'
      },
      category: node.type || 'default',
      originalData: node
    }
  })
})

// 处理连线数据为ECharts格式
const processedLinks = computed(() => {
  if (!props.topologyData?.links) return []
  
  return props.topologyData.links.map(link => ({
    source: link.source,
    target: link.target,
    lineStyle: {
      color: '#999',
      width: 2
    },
    label: {
      show: false
    }
  }))
})

// 初始化ECharts图表
const initChart = async () => {
  if (!chartRef.value || !hasValidData.value) return
  
  await nextTick()
  
  // 销毁现有实例
  if (chartInstance) {
    chartInstance.dispose()
  }
  
  chartInstance = echarts.init(chartRef.value)
  
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: (params) => {
        if (params.dataType === 'node') {
          const node = params.data.originalData
          return `
            <div style="padding: 8px;">
              <div style="font-weight: bold; margin-bottom: 4px;">${params.data.name}</div>
              <div>类型: ${node.type || '未知'}</div>
              <div>ID: ${node.id}</div>
              ${node.ip ? `<div>IP: ${node.ip}</div>` : ''}
            </div>
          `
        } else if (params.dataType === 'edge') {
          return `连接: ${params.data.source} → ${params.data.target}`
        }
        return ''
      }
    },
    animationDurationUpdate: 1500,
    animationEasingUpdate: 'quinticInOut',
    series: [
      {
        name: '网络拓扑',
        type: 'graph',
        layout: 'force',
        data: processedNodes.value,
        links: processedLinks.value,
        categories: [
          { name: 'server', itemStyle: { color: '#28a745' } },
          { name: 'firewall', itemStyle: { color: '#dc3545' } },
          { name: 'router', itemStyle: { color: '#ffc107' } },
          { name: 'pc', itemStyle: { color: '#17a2b8' } },
          { name: 'database', itemStyle: { color: '#6f42c1' } },
          { name: 'dns', itemStyle: { color: '#fd7e14' } },
          { name: 'switch', itemStyle: { color: '#20c997' } },
          { name: 'default', itemStyle: { color: '#6c757d' } }
        ],
        roam: true,
        focusNodeAdjacency: true,
        draggable: true,
        force: {
          repulsion: 200,
          gravity: 0.1,
          edgeLength: 150,
          layoutAnimation: true
        },
        lineStyle: {
          color: 'source',
          curveness: 0.3
        },
        emphasis: {
          focus: 'adjacency',
          lineStyle: {
            width: 3
          }
        }
      }
    ]
  }
  
  chartInstance.setOption(option)
  
  // 添加点击事件
  chartInstance.on('click', (params) => {
    if (params.dataType === 'node') {
    }
  })
}

// 更新图表大小
const resizeChart = () => {
  if (chartInstance) {
    chartInstance.resize()
  }
}

// 监听拓扑数据变化
watch(() => props.topologyData, (newData) => {
  if (newData) {
  }
  if (newData && hasValidData.value) {
    initChart()
  } else {
  }
}, { deep: true })

// 监听尺寸变化
watch([() => props.width, () => props.height], () => {
  nextTick(() => {
    resizeChart()
  })
})

onMounted(() => {
  if (hasValidData.value) {
    initChart()
  }
  window.addEventListener('resize', resizeChart)
})

onUnmounted(() => {
  if (chartInstance) {
    chartInstance.dispose()
  }
  window.removeEventListener('resize', resizeChart)
})
</script>

<style scoped>
.drill-topology-viewer {
  width: 100%;
  height: 100%;
  position: relative;
}

.topology-container {
  width: 100%;
  height: 100%;
  position: relative;
}

.topology-chart {
  width: 100%;
  background: #f8f9fa;
  border-radius: 4px;
  overflow: hidden;
}

.topology-info {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 10;
}

.info-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
  font-size: 12px;
}

.info-item:last-child {
  margin-bottom: 0;
}

.info-label {
  color: #909399;
  margin-right: 8px;
}

.info-value {
  color: #303133;
  font-weight: 500;
}

.no-data {
  height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .topology-info {
    position: static;
    margin-top: 10px;
  }
}
</style>