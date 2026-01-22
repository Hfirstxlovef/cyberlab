<template>
  <div class="attack-path-analysis">
    <h3 class="chart-title">攻击路径分析</h3>
    <div ref="chartRef" class="chart-content"></div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
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
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      triggerOn: 'mousemove',
      backgroundColor: 'rgba(10, 10, 10, 0.95)',
      borderColor: 'rgba(255, 59, 48, 0.3)',
      borderWidth: 1,
      textStyle: {
        color: '#ffffff',
        fontSize: 13,
        fontFamily: '-apple-system, BlinkMacSystemFont, "SF Pro Display"'
      },
      padding: [10, 15]
    },
    series: [
      {
        type: 'tree',
        data: [{
          name: '入口点',
          value: 'Entry',
          itemStyle: {
            color: '#ff3b30',
            borderColor: '#ff6b59',
            borderWidth: 2,
            shadowColor: 'rgba(255, 59, 48, 0.6)',
            shadowBlur: 15
          },
          children: [
            {
              name: 'Web服务',
              value: '192.168.1.100',
              itemStyle: {
                color: '#ff9500',
                borderColor: '#ffad33',
                borderWidth: 2,
                shadowColor: 'rgba(255, 149, 0, 0.6)',
                shadowBlur: 12
              },
              children: [
                {
                  name: 'SQL注入',
                  value: 'Exploit',
                  itemStyle: {
                    color: '#34c759',
                    borderColor: '#5dd17d',
                    borderWidth: 2,
                    shadowColor: 'rgba(52, 199, 89, 0.6)',
                    shadowBlur: 10
                  }
                },
                {
                  name: 'XSS攻击',
                  value: 'Exploit',
                  itemStyle: {
                    color: '#34c759',
                    borderColor: '#5dd17d',
                    borderWidth: 2,
                    shadowColor: 'rgba(52, 199, 89, 0.6)',
                    shadowBlur: 10
                  }
                }
              ]
            },
            {
              name: 'API网关',
              value: '192.168.1.101',
              itemStyle: {
                color: '#ff9500',
                borderColor: '#ffad33',
                borderWidth: 2,
                shadowColor: 'rgba(255, 149, 0, 0.6)',
                shadowBlur: 12
              },
              children: [
                {
                  name: '认证绕过',
                  value: 'Exploit',
                  itemStyle: {
                    color: '#34c759',
                    borderColor: '#5dd17d',
                    borderWidth: 2,
                    shadowColor: 'rgba(52, 199, 89, 0.6)',
                    shadowBlur: 10
                  }
                },
                {
                  name: '权限提升',
                  value: 'Exploit',
                  itemStyle: {
                    color: '#007aff',
                    borderColor: '#3395ff',
                    borderWidth: 2,
                    shadowColor: 'rgba(0, 122, 255, 0.6)',
                    shadowBlur: 10
                  }
                }
              ]
            }
          ]
        }],
        top: '5%',
        left: '10%',
        bottom: '5%',
        right: '20%',
        symbolSize: 12,
        orient: 'LR',
        label: {
          position: 'left',
          verticalAlign: 'middle',
          align: 'right',
          fontSize: 12,
          fontWeight: 500,
          color: 'rgba(255, 255, 255, 0.9)',
          fontFamily: '-apple-system, BlinkMacSystemFont, "SF Pro Display"',
          shadowColor: 'rgba(0, 0, 0, 0.5)',
          shadowBlur: 5
        },
        leaves: {
          label: {
            position: 'right',
            verticalAlign: 'middle',
            align: 'left'
          }
        },
        emphasis: {
          focus: 'descendant',
          itemStyle: {
            borderWidth: 3,
            shadowBlur: 20
          }
        },
        expandAndCollapse: true,
        animationDuration: 550,
        animationDurationUpdate: 750,
        lineStyle: {
          color: 'rgba(255, 59, 48, 0.3)',
          width: 2,
          curveness: 0.5,
          shadowColor: 'rgba(255, 59, 48, 0.4)',
          shadowBlur: 8
        }
      }
    ]
  }

  chartInstance.setOption(option)
}

onMounted(() => {
  initChart()
  
  window.addEventListener('resize', () => {
    chartInstance?.resize()
  })
})
</script>

<style scoped>
/* ============================================
   攻击路径分析 - 深色科技风 + Apple优雅
   ============================================ */

.attack-path-analysis {
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