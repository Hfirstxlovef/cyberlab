<template>
  <div class="defense-trend-chart">
    <div class="chart-header">
      <h3 class="chart-title">🛡️ 防护趋势分析</h3>
      <div class="chart-controls">
        <button 
          v-for="period in periods" 
          :key="period.value"
          :class="['period-btn', { active: selectedPeriod === period.value }]"
          @click="selectedPeriod = period.value"
        >
          {{ period.label }}
        </button>
      </div>
    </div>
    <div class="chart-container" ref="chartRef"></div>
    <div class="chart-stats">
      <div class="stat-item">
        <span class="stat-label">总拦截</span>
        <span class="stat-value">{{ totalBlocked }}</span>
      </div>
      <div class="stat-item">
        <span class="stat-label">成功率</span>
        <span class="stat-value success">{{ successRate }}%</span>
      </div>
      <div class="stat-item">
        <span class="stat-label">峰值防护</span>
        <span class="stat-value">{{ peakDefense }}/min</span>
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
const selectedPeriod = ref('24h')
let chartInstance = null

const periods = [
  { label: '1小时', value: '1h' },
  { label: '24小时', value: '24h' },
  { label: '7天', value: '7d' }
]

const totalBlocked = computed(() => {
  const mockData = getMockData()
  return mockData.reduce((sum, item) => sum + item.blocked, 0)
})

const successRate = computed(() => {
  const mockData = getMockData()
  const total = mockData.reduce((sum, item) => sum + item.attacks, 0)
  const blocked = mockData.reduce((sum, item) => sum + item.blocked, 0)
  return total > 0 ? ((blocked / total) * 100).toFixed(1) : 0
})

const peakDefense = computed(() => {
  const mockData = getMockData()
  return Math.max(...mockData.map(item => item.blocked))
})

const getMockData = () => {
  if (props.data && props.data.length > 0) {
    return props.data
  }
  
  // 生成模拟数据
  const hours = selectedPeriod.value === '1h' ? 12 : selectedPeriod.value === '24h' ? 24 : 168
  return Array.from({ length: hours }, (_, i) => {
    const baseAttacks = 50 + Math.random() * 100
    const blockRate = 0.85 + Math.random() * 0.1
    return {
      time: selectedPeriod.value === '7d' 
        ? `Day ${Math.floor(i/24) + 1}` 
        : `${String(i % 24).padStart(2, '0')}:00`,
      attacks: Math.floor(baseAttacks),
      blocked: Math.floor(baseAttacks * blockRate),
      passed: Math.floor(baseAttacks * (1 - blockRate))
    }
  })
}

const initChart = () => {
  if (!chartRef.value) return

  chartInstance = echarts.init(chartRef.value)
  updateChart()
}

const updateChart = () => {
  if (!chartInstance) return

  const mockData = getMockData()
  
  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(0, 0, 0, 0.8)',
      borderColor: '#4682b4',
      borderWidth: 1,
      textStyle: {
        color: '#fff'
      },
      formatter: (params) => {
        const time = params[0].axisValue
        return `
          <div style="padding: 8px;">
            <div style="font-weight: bold; margin-bottom: 4px;">${time}</div>
            <div style="color: #4caf50;">✓ 已拦截: ${params[0].value}</div>
            <div style="color: #ff9800;">⚡ 总攻击: ${params[1].value}</div>
            <div style="color: #f44336;">✗ 未拦截: ${params[2].value}</div>
          </div>
        `
      }
    },
    grid: {
      top: 20,
      right: 20,
      bottom: 20,
      left: 50,
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: mockData.map(item => item.time),
      axisLine: {
        lineStyle: {
          color: 'rgba(255, 255, 255, 0.2)'
        }
      },
      axisLabel: {
        color: 'rgba(255, 255, 255, 0.6)',
        fontSize: 11
      }
    },
    yAxis: {
      type: 'value',
      axisLine: {
        lineStyle: {
          color: 'rgba(255, 255, 255, 0.2)'
        }
      },
      axisLabel: {
        color: 'rgba(255, 255, 255, 0.6)'
      },
      splitLine: {
        lineStyle: {
          color: 'rgba(255, 255, 255, 0.05)'
        }
      }
    },
    series: [
      {
        name: '已拦截',
        type: 'line',
        data: mockData.map(item => item.blocked),
        smooth: true,
        symbol: 'circle',
        symbolSize: 4,
        lineStyle: {
          color: '#4caf50',
          width: 2
        },
        itemStyle: {
          color: '#4caf50'
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(76, 175, 80, 0.3)' },
            { offset: 1, color: 'rgba(76, 175, 80, 0)' }
          ])
        }
      },
      {
        name: '总攻击',
        type: 'line',
        data: mockData.map(item => item.attacks),
        smooth: true,
        symbol: 'circle',
        symbolSize: 4,
        lineStyle: {
          color: '#ff9800',
          width: 2
        },
        itemStyle: {
          color: '#ff9800'
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(255, 152, 0, 0.2)' },
            { offset: 1, color: 'rgba(255, 152, 0, 0)' }
          ])
        }
      },
      {
        name: '未拦截',
        type: 'line',
        data: mockData.map(item => item.passed),
        smooth: true,
        symbol: 'circle',
        symbolSize: 4,
        lineStyle: {
          color: '#f44336',
          width: 2,
          type: 'dashed'
        },
        itemStyle: {
          color: '#f44336'
        }
      }
    ]
  }

  chartInstance.setOption(option)
}

const handleResize = () => {
  chartInstance?.resize()
}

watch(selectedPeriod, () => {
  updateChart()
})

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
.defense-trend-chart {
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
  display: flex;
  align-items: center;
  gap: 8px;
}

.chart-controls {
  display: flex;
  gap: 8px;
}

.period-btn {
  padding: 6px 12px;
  background: rgba(70, 130, 180, 0.1);
  border: 1px solid rgba(70, 130, 180, 0.3);
  color: rgba(255, 255, 255, 0.6);
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.period-btn:hover {
  background: rgba(70, 130, 180, 0.2);
  color: rgba(255, 255, 255, 0.8);
}

.period-btn.active {
  background: linear-gradient(135deg, #4682b4 0%, #1e90ff 100%);
  border-color: transparent;
  color: #ffffff;
}

.chart-container {
  flex: 1;
  min-height: 200px;
}

.chart-stats {
  display: flex;
  justify-content: space-around;
  padding: 12px;
  background: rgba(70, 130, 180, 0.05);
  border-radius: 8px;
  margin-top: 12px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.stat-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.stat-value {
  font-size: 18px;
  font-weight: 600;
  color: #ffffff;
}

.stat-value.success {
  color: #4caf50;
}
</style>