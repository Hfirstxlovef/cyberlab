<template>
  <div class="attack-success-chart">
    <h3 class="chart-title">攻击成功率趋势</h3>
    <div ref="chartRef" class="chart-content"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  data: {
    type: Array,
    default: () => []
  }
})

const chartRef = ref(null)
let chartInstance = null

const initChart = () => {
  if (!chartRef.value) return

  chartInstance = echarts.init(chartRef.value)

  const option = {
    backgroundColor: 'transparent',
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: ['00:00', '04:00', '08:00', '12:00', '16:00', '20:00', '24:00'],
      axisLine: {
        lineStyle: {
          color: 'rgba(255, 255, 255, 0.15)',
          width: 1
        }
      },
      axisLabel: {
        color: 'rgba(255, 255, 255, 0.6)',
        fontSize: 11,
        fontFamily: 'SF Mono, Consolas, monospace'
      },
      axisTick: {
        show: false
      }
    },
    yAxis: {
      type: 'value',
      axisLine: {
        show: false
      },
      axisLabel: {
        color: 'rgba(255, 255, 255, 0.6)',
        formatter: '{value}%',
        fontSize: 11,
        fontFamily: 'SF Mono, Consolas, monospace'
      },
      splitLine: {
        lineStyle: {
          color: 'rgba(255, 255, 255, 0.05)',
          type: 'dashed'
        }
      },
      axisTick: {
        show: false
      }
    },
    series: [
      {
        name: '攻击成功率',
        type: 'line',
        smooth: true,
        data: [65, 72, 68, 80, 75, 82, 78],
        lineStyle: {
          color: '#ff3b30',
          width: 3,
          shadowColor: 'rgba(255, 59, 48, 0.5)',
          shadowBlur: 10,
          shadowOffsetY: 2
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(255, 59, 48, 0.25)' },
            { offset: 0.5, color: 'rgba(255, 59, 48, 0.1)' },
            { offset: 1, color: 'rgba(255, 59, 48, 0.02)' }
          ])
        },
        itemStyle: {
          color: '#ff3b30',
          borderColor: '#ffffff',
          borderWidth: 2,
          shadowColor: 'rgba(255, 59, 48, 0.6)',
          shadowBlur: 8
        },
        emphasis: {
          itemStyle: {
            color: '#ff6b59',
            borderColor: '#ffffff',
            borderWidth: 3,
            shadowColor: 'rgba(255, 59, 48, 0.8)',
            shadowBlur: 15
          }
        },
        symbol: 'circle',
        symbolSize: 6,
        showSymbol: false
      }
    ],
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(10, 10, 10, 0.95)',
      borderColor: 'rgba(255, 59, 48, 0.3)',
      borderWidth: 1,
      textStyle: {
        color: '#ffffff',
        fontSize: 13,
        fontFamily: '-apple-system, BlinkMacSystemFont, "SF Pro Display"'
      },
      padding: [10, 15],
      formatter: '{b0}<br/><span style="color:#ff3b30">●</span> 成功率: {c0}%',
      axisPointer: {
        type: 'line',
        lineStyle: {
          color: 'rgba(255, 59, 48, 0.3)',
          type: 'dashed'
        }
      }
    }
  }

  chartInstance.setOption(option)
}

const updateChart = () => {
  if (!chartInstance || !props.data.length) return
  
  const xData = props.data.map(item => item.time)
  const yData = props.data.map(item => item.rate)
  
  chartInstance.setOption({
    xAxis: { data: xData },
    series: [{ data: yData }]
  })
}

watch(() => props.data, updateChart, { deep: true })

onMounted(() => {
  initChart()
  
  window.addEventListener('resize', () => {
    chartInstance?.resize()
  })
})
</script>

<style scoped>
/* ============================================
   攻击成功率趋势 - 深色科技风 + Apple优雅
   ============================================ */

.attack-success-chart {
  height: 100%;
  display: flex;
  flex-direction: column;
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", "Helvetica Neue", sans-serif;
}

.chart-title {
  margin: 0 0 20px 0;
  font-size: 18px;
  font-weight: 700;
  color: #ff3b30;
  letter-spacing: -0.3px;
  text-shadow: 0 0 20px rgba(255, 59, 48, 0.4);
  position: relative;
  padding-bottom: 12px;
}

.chart-title::after {
  content: '';
  position: absolute;
  left: 0;
  bottom: 0;
  width: 40px;
  height: 2px;
  background: linear-gradient(90deg, #ff3b30 0%, transparent 100%);
  box-shadow: 0 0 10px rgba(255, 59, 48, 0.5);
}

.chart-content {
  flex: 1;
  min-height: 250px;
}
</style>