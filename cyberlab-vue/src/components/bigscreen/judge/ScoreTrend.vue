<template>
  <div class="score-trend">
    <h3 class="chart-title">得分趋势</h3>
    <div ref="chartRef" class="chart-content"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  data: {
    type: Object,
    default: () => ({})
  }
})

const chartRef = ref(null)
let chartInstance = null

const initChart = () => {
  if (!chartRef.value) return
  
  chartInstance = echarts.init(chartRef.value)
  
  const timeData = []
  const redScores = []
  const blueScores = []
  
  const now = new Date()
  for (let i = 30; i >= 0; i--) {
    const time = new Date(now.getTime() - i * 2 * 60000)
    timeData.push(time.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }))
    
    const redBase = 500 + Math.random() * 50
    const blueBase = 450 + Math.random() * 50
    redScores.push(Math.round(redBase + (30 - i) * 15 + Math.random() * 30))
    blueScores.push(Math.round(blueBase + (30 - i) * 12 + Math.random() * 25))
  }
  
  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(0, 0, 0, 0.8)',
      borderColor: '#9c27b0',
      textStyle: {
        color: '#fff'
      },
      axisPointer: {
        type: 'cross',
        label: {
          backgroundColor: '#9c27b0'
        }
      }
    },
    legend: {
      data: ['红队得分', '蓝队得分'],
      textStyle: {
        color: 'rgba(255, 255, 255, 0.8)'
      },
      top: 10
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: timeData,
      axisLine: {
        lineStyle: {
          color: 'rgba(255, 255, 255, 0.3)'
        }
      },
      axisLabel: {
        color: 'rgba(255, 255, 255, 0.6)',
        fontSize: 11,
        rotate: 45
      },
      splitLine: {
        show: false
      }
    },
    yAxis: {
      type: 'value',
      axisLine: {
        lineStyle: {
          color: 'rgba(255, 255, 255, 0.3)'
        }
      },
      axisLabel: {
        color: 'rgba(255, 255, 255, 0.6)',
        fontSize: 11
      },
      splitLine: {
        lineStyle: {
          color: 'rgba(255, 255, 255, 0.1)'
        }
      }
    },
    series: [
      {
        name: '红队得分',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        sampling: 'average',
        itemStyle: {
          color: '#f44336'
        },
        lineStyle: {
          width: 2,
          color: '#f44336'
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            {
              offset: 0,
              color: 'rgba(244, 67, 54, 0.3)'
            },
            {
              offset: 1,
              color: 'rgba(244, 67, 54, 0.05)'
            }
          ])
        },
        data: redScores
      },
      {
        name: '蓝队得分',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        sampling: 'average',
        itemStyle: {
          color: '#2196f3'
        },
        lineStyle: {
          width: 2,
          color: '#2196f3'
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            {
              offset: 0,
              color: 'rgba(33, 150, 243, 0.3)'
            },
            {
              offset: 1,
              color: 'rgba(33, 150, 243, 0.05)'
            }
          ])
        },
        data: blueScores
      }
    ]
  }
  
  chartInstance.setOption(option)
}

const resizeChart = () => {
  chartInstance?.resize()
}

onMounted(() => {
  initChart()
  window.addEventListener('resize', resizeChart)
  
  const timer = setInterval(() => {
    initChart()
  }, 30000)
  
  onUnmounted(() => {
    clearInterval(timer)
    window.removeEventListener('resize', resizeChart)
    chartInstance?.dispose()
  })
})
</script>

<style scoped>
.score-trend {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.chart-title {
  margin: 0 0 15px 0;
  font-size: 18px;
  color: #9c27b0;
  font-weight: 500;
}

.chart-content {
  flex: 1;
  min-height: 250px;
}
</style>