<template>
  <div class="attack-trend-chart">
    <div class="chart-header">
      <h3>📈 攻防行为趋势</h3>
      <div class="legend">
        <span class="legend-item red">红队攻击</span>
        <span class="legend-item blue">蓝队防守</span>
      </div>
    </div>
    <div ref="chartRef" class="chart-container"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  data: {
    type: Array,
    default: () => []
  }
})

const chartRef = ref(null)
let chart = null

const initChart = () => {
  if (!chartRef.value) return

  chart = echarts.init(chartRef.value)

  const option = {
    grid: {
      top: 40,
      left: 50,
      right: 30,
      bottom: 40
    },
    xAxis: {
      type: 'category',
      data: props.data.map(item => item.time) || [],
      axisLine: {
        lineStyle: {
          color: 'rgba(0, 0, 0, 0.1)'
        }
      },
      axisLabel: {
        color: '#86868b',
        fontFamily: '-apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", "Helvetica Neue", sans-serif'
      }
    },
    yAxis: {
      type: 'value',
      axisLine: {
        lineStyle: {
          color: 'rgba(0, 0, 0, 0.1)'
        }
      },
      axisLabel: {
        color: '#86868b',
        fontFamily: '-apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", "Helvetica Neue", sans-serif'
      },
      splitLine: {
        lineStyle: {
          color: 'rgba(0, 0, 0, 0.04)'
        }
      }
    },
    series: [
      {
        name: '红队攻击',
        type: 'line',
        data: props.data.map(item => item.redTeam) || [],
        lineStyle: {
          color: '#ff3b30',
          width: 3,
          shadowColor: '#ff3b30',
          shadowBlur: 10
        },
        itemStyle: {
          color: '#ff3b30',
          borderWidth: 2,
          borderColor: '#ffffff'
        },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(255, 59, 48, 0.3)' },
              { offset: 1, color: 'rgba(255, 59, 48, 0.05)' }
            ]
          }
        },
        smooth: true,
        symbol: 'circle',
        symbolSize: 8
      },
      {
        name: '蓝队防守',
        type: 'line',
        data: props.data.map(item => item.blueTeam) || [],
        lineStyle: {
          color: '#007aff',
          width: 3,
          shadowColor: '#007aff',
          shadowBlur: 10
        },
        itemStyle: {
          color: '#007aff',
          borderWidth: 2,
          borderColor: '#ffffff'
        },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(0, 122, 255, 0.3)' },
              { offset: 1, color: 'rgba(0, 122, 255, 0.05)' }
            ]
          }
        },
        smooth: true,
        symbol: 'circle',
        symbolSize: 8
      }
    ],
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: 'rgba(0, 0, 0, 0.08)',
      borderWidth: 1,
      textStyle: {
        color: '#1d1d1f',
        fontFamily: '-apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", "Helvetica Neue", sans-serif'
      },
      extraCssText: 'backdrop-filter: blur(10px); -webkit-backdrop-filter: blur(10px); box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);'
    },
    animation: true,
    animationDuration: 2000,
    animationEasing: 'cubicOut'
  }

  chart.setOption(option)
}

const updateChart = () => {
  if (chart) {
    chart.setOption({
      xAxis: {
        data: props.data.map(item => item.time) || []
      },
      series: [
        {
          data: props.data.map(item => item.redTeam) || []
        },
        {
          data: props.data.map(item => item.blueTeam) || []
        }
      ]
    })
  }
}

watch(() => props.data, () => {
  nextTick(() => {
    updateChart()
  })
}, { deep: true })

onMounted(() => {
  nextTick(() => {
    initChart()
  })
})
</script>

<style scoped>
/* ============================================
   Apple Elegant White Style - Attack Trend Chart
   ============================================ */

:root {
  --apple-text: #1d1d1f;
  --apple-text-secondary: #86868b;
  --apple-red: #ff3b30;
  --apple-blue: #007aff;
  --font-apple: -apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", "Helvetica Neue", sans-serif;
}

.attack-trend-chart {
  height: 100%;
  display: flex;
  flex-direction: column;
  font-family: var(--font-apple);
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.chart-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--apple-text);
  letter-spacing: -0.3px;
}

.legend {
  display: flex;
  gap: 20px;
}

.legend-item {
  font-size: 14px;
  font-weight: 500;
  position: relative;
  padding-left: 15px;
}

.legend-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 10px;
  height: 3px;
  border-radius: 2px;
}

.legend-item.red {
  color: var(--apple-red);
}

.legend-item.red::before {
  background: var(--apple-red);
  box-shadow: 0 0 8px rgba(255, 59, 48, 0.3);
}

.legend-item.blue {
  color: var(--apple-blue);
}

.legend-item.blue::before {
  background: var(--apple-blue);
  box-shadow: 0 0 8px rgba(0, 122, 255, 0.3);
}

.chart-container {
  flex: 1;
  min-height: 0;
}
</style>