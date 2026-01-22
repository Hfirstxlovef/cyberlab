<template>
  <div class="defense-effectiveness">
    <div class="chart-header">
      <h3 class="chart-title">🛡️ 防护效果统计</h3>
      <div class="effectiveness-rate">
        <span class="rate-label">整体防护率</span>
        <span class="rate-value">{{ overallRate }}%</span>
      </div>
    </div>
    <div class="chart-content">
      <div class="defense-metrics">
        <div class="metric-item" v-for="item in metrics" :key="item.label">
          <div class="metric-header">
            <span class="metric-icon">{{ item.icon }}</span>
            <span class="metric-label">{{ item.label }}</span>
          </div>
          <div class="metric-value">
            <span class="value">{{ item.value }}</span>
            <span class="unit">{{ item.unit }}</span>
          </div>
          <div class="metric-progress">
            <div class="progress-bar">
              <div 
                class="progress-fill" 
                :style="{ width: item.percentage + '%', backgroundColor: item.color }"
              ></div>
            </div>
            <span class="percentage">{{ item.percentage }}%</span>
          </div>
        </div>
      </div>
      <div class="defense-chart" ref="chartRef"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  data: {
    type: Array,
    default: () => []
  }
})

const chartRef = ref(null)
let chartInstance = null

const overallRate = computed(() => {
  if (!props.data || props.data.length === 0) return 92
  const total = props.data.reduce((sum, item) => sum + (item.success || 0), 0)
  const attempts = props.data.reduce((sum, item) => sum + (item.total || 0), 0)
  return attempts > 0 ? Math.round((total / attempts) * 100) : 0
})

const metrics = computed(() => {
  return [
    {
      icon: '🔒',
      label: '攻击拦截',
      value: 1247,
      unit: '次',
      percentage: 95,
      color: '#4682b4'
    },
    {
      icon: '🚫',
      label: '恶意流量阻断',
      value: 856,
      unit: '次',
      percentage: 88,
      color: '#1e90ff'
    },
    {
      icon: '🔍',
      label: '漏洞修复',
      value: 42,
      unit: '个',
      percentage: 100,
      color: '#00bfff'
    },
    {
      icon: '⚡',
      label: '响应速度',
      value: 1.2,
      unit: '秒',
      percentage: 96,
      color: '#87ceeb'
    }
  ]
})

const initChart = () => {
  if (!chartRef.value) return
  
  chartInstance = echarts.init(chartRef.value)
  
  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(0, 0, 0, 0.8)',
      borderColor: '#4682b4',
      borderWidth: 1,
      textStyle: {
        color: '#fff'
      }
    },
    grid: {
      top: 20,
      left: 50,
      right: 20,
      bottom: 30
    },
    xAxis: {
      type: 'category',
      data: ['00:00', '04:00', '08:00', '12:00', '16:00', '20:00', '24:00'],
      axisLine: {
        lineStyle: {
          color: 'rgba(255, 255, 255, 0.2)'
        }
      },
      axisLabel: {
        color: 'rgba(255, 255, 255, 0.6)'
      }
    },
    yAxis: {
      type: 'value',
      name: '防护成功率(%)',
      nameTextStyle: {
        color: 'rgba(255, 255, 255, 0.6)'
      },
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
          color: 'rgba(255, 255, 255, 0.1)'
        }
      }
    },
    series: [
      {
        name: '防护成功率',
        type: 'line',
        smooth: true,
        data: [92, 94, 93, 95, 91, 96, 94],
        lineStyle: {
          color: '#4682b4',
          width: 3
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(70, 130, 180, 0.4)' },
            { offset: 1, color: 'rgba(70, 130, 180, 0.1)' }
          ])
        },
        itemStyle: {
          color: '#4682b4'
        }
      }
    ]
  }
  
  chartInstance.setOption(option)
}

const resizeChart = () => {
  if (chartInstance) {
    chartInstance.resize()
  }
}

watch(() => props.data, () => {
  if (chartInstance) {
    // 更新图表数据
    const newData = props.data.map(item => item.rate || 90 + Math.random() * 10)
    chartInstance.setOption({
      series: [{
        data: newData.length > 0 ? newData : [92, 94, 93, 95, 91, 96, 94]
      }]
    })
  }
}, { deep: true })

onMounted(() => {
  initChart()
  window.addEventListener('resize', resizeChart)
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeChart)
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
})
</script>

<style scoped>
.defense-effectiveness {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.chart-title {
  font-size: 18px;
  font-weight: 600;
  color: #4682b4;
  margin: 0;
}

.effectiveness-rate {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 16px;
  background: rgba(70, 130, 180, 0.1);
  border-radius: 20px;
  border: 1px solid rgba(70, 130, 180, 0.3);
}

.rate-label {
  color: rgba(255, 255, 255, 0.6);
  font-size: 14px;
}

.rate-value {
  font-size: 20px;
  font-weight: bold;
  color: #4682b4;
}

.chart-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.defense-metrics {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
}

.metric-item {
  padding: 12px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.metric-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.metric-icon {
  font-size: 18px;
}

.metric-label {
  color: rgba(255, 255, 255, 0.6);
  font-size: 14px;
}

.metric-value {
  display: flex;
  align-items: baseline;
  gap: 5px;
  margin-bottom: 8px;
}

.metric-value .value {
  font-size: 24px;
  font-weight: bold;
  color: #ffffff;
}

.metric-value .unit {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

.metric-progress {
  display: flex;
  align-items: center;
  gap: 10px;
}

.progress-bar {
  flex: 1;
  height: 6px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 3px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.5s ease;
}

.percentage {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  min-width: 35px;
  text-align: right;
}

.defense-chart {
  flex: 1;
  min-height: 200px;
}
</style>