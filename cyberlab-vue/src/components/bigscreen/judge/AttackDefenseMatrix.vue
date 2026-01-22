<template>
  <div class="attack-defense-matrix">
    <h3 class="chart-title">攻防矩阵</h3>
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
  
  const hours = ['00:00', '02:00', '04:00', '06:00', '08:00', '10:00', 
                  '12:00', '14:00', '16:00', '18:00', '20:00', '22:00']
  const systems = ['Web服务', '数据库', '文件服务', 'API网关', '邮件系统']
  
  const data = []
  for (let i = 0; i < hours.length; i++) {
    for (let j = 0; j < systems.length; j++) {
      const value = Math.floor(Math.random() * 10)
      if (value > 0) {
        data.push([i, j, value])
      }
    }
  }
  
  const option = {
    tooltip: {
      position: 'top',
      backgroundColor: 'rgba(0, 0, 0, 0.8)',
      borderColor: '#9c27b0',
      textStyle: {
        color: '#fff'
      },
      formatter: function (params) {
        return `${hours[params.value[0]]}<br/>
                ${systems[params.value[1]]}<br/>
                攻击次数: ${params.value[2]}`
      }
    },
    grid: {
      height: '75%',
      top: '10%',
      left: '15%',
      right: '5%'
    },
    xAxis: {
      type: 'category',
      data: hours,
      splitArea: {
        show: true,
        areaStyle: {
          color: ['rgba(255, 255, 255, 0.02)', 'rgba(255, 255, 255, 0.05)']
        }
      },
      axisLine: {
        lineStyle: {
          color: 'rgba(255, 255, 255, 0.3)'
        }
      },
      axisLabel: {
        color: 'rgba(255, 255, 255, 0.6)',
        fontSize: 11,
        rotate: 45
      }
    },
    yAxis: {
      type: 'category',
      data: systems,
      splitArea: {
        show: true,
        areaStyle: {
          color: ['rgba(255, 255, 255, 0.02)', 'rgba(255, 255, 255, 0.05)']
        }
      },
      axisLine: {
        lineStyle: {
          color: 'rgba(255, 255, 255, 0.3)'
        }
      },
      axisLabel: {
        color: 'rgba(255, 255, 255, 0.6)',
        fontSize: 11
      }
    },
    visualMap: {
      min: 0,
      max: 10,
      calculable: true,
      orient: 'horizontal',
      left: 'center',
      bottom: '0%',
      textStyle: {
        color: 'rgba(255, 255, 255, 0.6)'
      },
      inRange: {
        color: ['#313695', '#4575b4', '#74add1', '#abd9e9', '#e0f3f8', 
                '#fee090', '#fdae61', '#f46d43', '#d73027', '#a50026']
      }
    },
    series: [{
      name: '攻防热力图',
      type: 'heatmap',
      data: data,
      label: {
        show: true,
        color: '#fff',
        fontSize: 10
      },
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowColor: 'rgba(156, 39, 176, 0.5)'
        }
      }
    }]
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
  }, 60000)
  
  onUnmounted(() => {
    clearInterval(timer)
    window.removeEventListener('resize', resizeChart)
    chartInstance?.dispose()
  })
})
</script>

<style scoped>
.attack-defense-matrix {
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