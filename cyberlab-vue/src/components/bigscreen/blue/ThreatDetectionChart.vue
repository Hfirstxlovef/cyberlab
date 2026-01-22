<template>
  <div class="threat-detection-chart">
    <div class="chart-header">
      <h3 class="chart-title">🎯 威胁检测分布</h3>
      <div class="legend">
        <span class="legend-item critical">
          <i class="legend-dot"></i>严重
        </span>
        <span class="legend-item high">
          <i class="legend-dot"></i>高危
        </span>
        <span class="legend-item medium">
          <i class="legend-dot"></i>中危
        </span>
        <span class="legend-item low">
          <i class="legend-dot"></i>低危
        </span>
      </div>
    </div>
    <div class="chart-container" ref="chartRef"></div>
    <div class="threat-list">
      <div class="threat-item" v-for="threat in topThreats" :key="threat.name">
        <div class="threat-info">
          <span class="threat-name">{{ threat.name }}</span>
          <span :class="['threat-level', threat.level]">{{ threat.count }}次</span>
        </div>
        <div class="threat-bar">
          <div 
            class="threat-bar-fill" 
            :class="threat.level"
            :style="{ width: `${(threat.count / maxThreatCount) * 100}%` }"
          ></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, computed } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  data: {
    type: Array,
    default: () => []
  }
})

const chartRef = ref(null)
let chartInstance = null

const mockThreats = [
  { name: 'SQL注入', value: 156, level: 'critical' },
  { name: 'XSS攻击', value: 128, level: 'high' },
  { name: '暴力破解', value: 98, level: 'high' },
  { name: '端口扫描', value: 234, level: 'medium' },
  { name: '异常登录', value: 87, level: 'medium' },
  { name: 'DDoS攻击', value: 45, level: 'critical' },
  { name: '文件上传', value: 67, level: 'high' },
  { name: '权限提升', value: 34, level: 'critical' }
]

const topThreats = computed(() => {
  const threats = props.data.length > 0 ? props.data : mockThreats
  return threats
    .sort((a, b) => b.value - a.value)
    .slice(0, 5)
    .map(t => ({
      name: t.name,
      count: t.value,
      level: t.level
    }))
})

const maxThreatCount = computed(() => {
  return Math.max(...topThreats.value.map(t => t.count))
})

const initChart = () => {
  if (!chartRef.value) return

  chartInstance = echarts.init(chartRef.value)
  updateChart()
}

const updateChart = () => {
  if (!chartInstance) return

  const threats = props.data.length > 0 ? props.data : mockThreats
  
  const option = {
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(0, 0, 0, 0.8)',
      borderColor: '#4682b4',
      borderWidth: 1,
      textStyle: {
        color: '#fff'
      },
      formatter: (params) => {
        const level = threats.find(t => t.name === params.name)?.level || 'low'
        const levelText = {
          critical: '严重',
          high: '高危',
          medium: '中危',
          low: '低危'
        }[level]
        return `
          <div style="padding: 8px;">
            <div style="font-weight: bold; margin-bottom: 4px;">${params.name}</div>
            <div>检测次数: ${params.value}</div>
            <div>占比: ${params.percent}%</div>
            <div>威胁等级: ${levelText}</div>
          </div>
        `
      }
    },
    series: [
      {
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['50%', '50%'],
        data: threats.map(threat => ({
          name: threat.name,
          value: threat.value,
          itemStyle: {
            color: {
              critical: '#f44336',
              high: '#ff9800',
              medium: '#ffc107',
              low: '#4caf50'
            }[threat.level]
          }
        })),
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        },
        label: {
          show: false
        },
        labelLine: {
          show: false
        }
      }
    ]
  }

  chartInstance.setOption(option)
}

const handleResize = () => {
  chartInstance?.resize()
}

watch(() => props.data, () => {
  updateChart()
}, { deep: true })

onMounted(() => {
  initChart()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
})
</script>

<style scoped>
.threat-detection-chart {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.chart-title {
  font-size: 18px;
  font-weight: 600;
  color: #ffffff;
  margin: 0;
}

.legend {
  display: flex;
  gap: 16px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}

.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}

.legend-item.critical .legend-dot {
  background: #f44336;
}

.legend-item.high .legend-dot {
  background: #ff9800;
}

.legend-item.medium .legend-dot {
  background: #ffc107;
}

.legend-item.low .legend-dot {
  background: #4caf50;
}

.chart-container {
  flex: 1;
  min-height: 200px;
}

.threat-list {
  margin-top: 16px;
  padding: 12px;
  background: rgba(70, 130, 180, 0.05);
  border-radius: 8px;
}

.threat-item {
  margin-bottom: 12px;
}

.threat-item:last-child {
  margin-bottom: 0;
}

.threat-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.threat-name {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
}

.threat-level {
  font-size: 12px;
  font-weight: 600;
  padding: 2px 6px;
  border-radius: 4px;
}

.threat-level.critical {
  background: rgba(244, 67, 54, 0.2);
  color: #f44336;
}

.threat-level.high {
  background: rgba(255, 152, 0, 0.2);
  color: #ff9800;
}

.threat-level.medium {
  background: rgba(255, 193, 7, 0.2);
  color: #ffc107;
}

.threat-level.low {
  background: rgba(76, 175, 80, 0.2);
  color: #4caf50;
}

.threat-bar {
  width: 100%;
  height: 4px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 2px;
  overflow: hidden;
}

.threat-bar-fill {
  height: 100%;
  border-radius: 2px;
  transition: width 0.3s ease;
}

.threat-bar-fill.critical {
  background: linear-gradient(90deg, #f44336, #ff6b6b);
}

.threat-bar-fill.high {
  background: linear-gradient(90deg, #ff9800, #ffb347);
}

.threat-bar-fill.medium {
  background: linear-gradient(90deg, #ffc107, #ffd54f);
}

.threat-bar-fill.low {
  background: linear-gradient(90deg, #4caf50, #66bb6a);
}
</style>