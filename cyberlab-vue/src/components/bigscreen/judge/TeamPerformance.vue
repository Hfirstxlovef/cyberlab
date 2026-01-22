<template>
  <div class="team-performance">
    <h3 class="chart-title">团队表现</h3>
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
  
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      backgroundColor: 'rgba(0, 0, 0, 0.8)',
      borderColor: '#9c27b0',
      textStyle: {
        color: '#fff'
      }
    },
    legend: {
      data: ['红队', '蓝队'],
      textStyle: {
        color: 'rgba(255, 255, 255, 0.8)'
      },
      top: 10
    },
    radar: {
      indicator: [
        { name: '攻击效率', max: 100 },
        { name: '防御能力', max: 100 },
        { name: '响应速度', max: 100 },
        { name: '技术水平', max: 100 },
        { name: '团队协作', max: 100 },
        { name: '创新能力', max: 100 }
      ],
      shape: 'polygon',
      splitNumber: 4,
      axisName: {
        color: 'rgba(255, 255, 255, 0.7)',
        fontSize: 12
      },
      splitLine: {
        lineStyle: {
          color: 'rgba(255, 255, 255, 0.1)'
        }
      },
      splitArea: {
        show: true,
        areaStyle: {
          color: ['rgba(156, 39, 176, 0.05)', 'rgba(156, 39, 176, 0.02)']
        }
      },
      axisLine: {
        lineStyle: {
          color: 'rgba(255, 255, 255, 0.2)'
        }
      }
    },
    series: [
      {
        name: '团队表现',
        type: 'radar',
        data: [
          {
            value: [85, 45, 78, 88, 82, 75],
            name: '红队',
            lineStyle: {
              color: '#f44336',
              width: 2
            },
            itemStyle: {
              color: '#f44336'
            },
            areaStyle: {
              color: 'rgba(244, 67, 54, 0.3)'
            }
          },
          {
            value: [48, 92, 85, 76, 88, 70],
            name: '蓝队',
            lineStyle: {
              color: '#2196f3',
              width: 2
            },
            itemStyle: {
              color: '#2196f3'
            },
            areaStyle: {
              color: 'rgba(33, 150, 243, 0.3)'
            }
          }
        ]
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
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeChart)
  chartInstance?.dispose()
})
</script>

<style scoped>
.team-performance {
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