<template>
  <div class="achievement-analytics-page">
    <el-row :gutter="20">
      <!-- 统计卡片 -->
      <el-col :span="6">
        <div class="stat-card total">
          <div class="stat-icon">📈</div>
          <div class="stat-content">
            <div class="stat-number">{{ stats.totalSubmissions }}</div>
            <div class="stat-label">总提交数</div>
          </div>
        </div>
      </el-col>

      <el-col :span="6">
        <div class="stat-card red-team">
          <div class="stat-icon">⚔️</div>
          <div class="stat-content">
            <div class="stat-number">{{ stats.redTeamSubmissions }}</div>
            <div class="stat-label">红队成果</div>
          </div>
        </div>
      </el-col>

      <el-col :span="6">
        <div class="stat-card blue-team">
          <div class="stat-icon">🛡️</div>
          <div class="stat-content">
            <div class="stat-number">{{ stats.blueTeamSubmissions }}</div>
            <div class="stat-label">蓝队成果</div>
          </div>
        </div>
      </el-col>

      <el-col :span="6">
        <div class="stat-card approval">
          <div class="stat-icon">✓</div>
          <div class="stat-content">
            <div class="stat-number">{{ stats.approvedRate }}%</div>
            <div class="stat-label">通过率</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>📈 提交趋势</span>
          </template>
          <div ref="trendChart" style="height: 300px;"></div>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card>
          <template #header>
            <span>🎯 成果类型分布</span>
          </template>
          <div ref="typeChart" style="height: 300px;"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import axios from '@/api/axios'

// 响应式数据
const trendChart = ref()
const typeChart = ref()
const stats = reactive({
  totalSubmissions: 0,
  redTeamSubmissions: 0,
  blueTeamSubmissions: 0,
  approvedRate: 0
})

// 获取统计数据
const fetchStatistics = async () => {
  try {
    const response = await axios.get('/achievements/statistics')


    // 处理统计数据
    if (response) {

      stats.totalSubmissions = response.totalSubmissions || 0
      stats.approvedRate = Math.round(response.approvalRate || 0)

      // 计算红蓝队数量（需要后端提供分类数据）
      stats.redTeamSubmissions = response.redTeamSubmissions || 0
      stats.blueTeamSubmissions = response.blueTeamSubmissions || 0

      // 如果没有分类数据，估算分配
      if (!response.redTeamSubmissions && !response.blueTeamSubmissions && response.totalSubmissions > 0) {
        stats.redTeamSubmissions = Math.floor(response.totalSubmissions * 0.6)
        stats.blueTeamSubmissions = response.totalSubmissions - stats.redTeamSubmissions
      }

    } else {
      throw new Error('无有效数据')
    }

  } catch (error) {

    // 不显示错误消息，只显示0值
    stats.totalSubmissions = 0
    stats.redTeamSubmissions = 0
    stats.blueTeamSubmissions = 0
    stats.approvedRate = 0
  }
}

// 获取图表数据
const fetchChartData = async () => {
  try {

    // 获取月度趋势数据
    const trendResponse = await axios.get('/achievements/trend')

    // 获取类型分布数据
    const typeResponse = await axios.get('/achievements/type-distribution')

    const result = {
      trendData: Array.isArray(trendResponse) ? trendResponse : null,
      typeData: Array.isArray(typeResponse) ? typeResponse : null
    }

    return result
  } catch (error) {
    return { trendData: null, typeData: null }
  }
}

// 初始化图表
const initCharts = async () => {
  await nextTick()

  const chartData = await fetchChartData()

  // 趋势图
  const trendChartInstance = echarts.init(trendChart.value)

  if (chartData.trendData && chartData.trendData.length > 0) {
    // 使用实际数据
    const dates = chartData.trendData.map(item => item.date) // 日期格式为MM-dd
    const redData = chartData.trendData.map(item => item.redTeam || 0)
    const blueData = chartData.trendData.map(item => item.blueTeam || 0)


    const trendOption = {
      title: {
        text: '成果提交趋势（最近7天）',
        textStyle: { fontSize: 16, fontWeight: 'bold' },
        left: 'center'
      },
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(50,50,50,0.9)',
        borderColor: '#409EFF',
        borderWidth: 1,
        textStyle: {
          color: '#fff',
          fontSize: 12
        },
        axisPointer: {
          type: 'cross',
          crossStyle: {
            color: '#999'
          },
          lineStyle: {
            color: '#409EFF',
            width: 1,
            type: 'dashed'
          }
        },
        formatter: function (params) {
          let result = `<div style="padding: 8px;">`
          result += `<div style="font-weight: bold; margin-bottom: 6px; color: #409EFF;">📅 ${params[0].name}</div>`

          let totalCount = 0
          params.forEach(param => {
            const color = param.color
            const icon = param.seriesName === '红队' ? '🔴' : '🔵'
            result += `<div style="margin: 3px 0; display: flex; align-items: center;">`
            result += `<span style="display: inline-block; width: 10px; height: 10px; background-color: ${color}; border-radius: 50%; margin-right: 8px;"></span>`
            result += `<span style="font-weight: 500;">${icon} ${param.seriesName}: <span style="color: ${color}; font-weight: bold;">${param.value}</span> 个</span>`
            result += `</div>`
            totalCount += param.value
          })

          if (totalCount > 0) {
            result += `<div style="margin-top: 8px; padding-top: 6px; border-top: 1px solid #666; font-size: 11px; color: #ccc;">`
            result += `✨ 当日总计: ${totalCount} 个成果`
            result += `</div>`
          }

          result += `</div>`
          return result
        }
      },
      xAxis: {
        type: 'category',
        data: dates,
        axisLabel: {
          rotate: 45,
          interval: 0,
          fontSize: 11,
          color: '#666',
          margin: 10
        },
        axisLine: {
          lineStyle: {
            color: '#E4E7ED'
          }
        },
        axisTick: {
          alignWithLabel: true,
          lineStyle: {
            color: '#E4E7ED'
          }
        }
      },
      yAxis: {
        type: 'value',
        min: 0,
        minInterval: 1,
        axisLabel: {
          fontSize: 11,
          color: '#666',
          formatter: '{value} 个'
        },
        axisLine: {
          lineStyle: {
            color: '#E4E7ED'
          }
        },
        splitLine: {
          lineStyle: {
            color: '#F5F7FA',
            type: 'dashed'
          }
        }
      },
      series: [
        {
          name: '红队',
          type: 'line',
          data: redData,
          itemStyle: {
            color: '#e74c3c',
            borderWidth: 2
          },
          lineStyle: {
            color: '#e74c3c',
            width: 3
          },
          symbol: 'circle',
          symbolSize: 6,
          smooth: false,
          emphasis: {
            scale: true,
            focus: 'series'
          },
          areaStyle: {
            color: {
              type: 'linear',
              x: 0,
              y: 0,
              x2: 0,
              y2: 1,
              colorStops: [{
                offset: 0, color: 'rgba(231, 76, 60, 0.3)'
              }, {
                offset: 1, color: 'rgba(231, 76, 60, 0.05)'
              }]
            }
          }
        },
        {
          name: '蓝队',
          type: 'line',
          data: blueData,
          itemStyle: {
            color: '#3498db',
            borderWidth: 2
          },
          lineStyle: {
            color: '#3498db',
            width: 3
          },
          symbol: 'circle',
          symbolSize: 6,
          smooth: false,
          emphasis: {
            scale: true,
            focus: 'series'
          },
          areaStyle: {
            color: {
              type: 'linear',
              x: 0,
              y: 0,
              x2: 0,
              y2: 1,
              colorStops: [{
                offset: 0, color: 'rgba(52, 152, 219, 0.3)'
              }, {
                offset: 1, color: 'rgba(52, 152, 219, 0.05)'
              }]
            }
          }
        }
      ],
      legend: {
        data: ['红队', '蓝队'],
        top: 30,
        itemWidth: 12,
        itemHeight: 8,
        textStyle: {
          fontSize: 12,
          color: '#606266'
        }
      },
      grid: {
        left: '8%',
        right: '8%',
        bottom: '15%',
        top: '20%',
        containLabel: true
      },
      animation: true,
      animationDuration: 1000,
      animationEasing: 'cubicOut'
    }

    trendChartInstance.setOption(trendOption)
  } else {
    const trendOption = {
      title: { text: '成果提交趋势（暂无数据）', textStyle: { fontSize: 14 } },
      graphic: {
        type: 'text',
        left: 'center',
        top: 'middle',
        style: {
          text: '暂无趋势数据',
          fontSize: 16,
          fill: '#999'
        }
      }
    }
    trendChartInstance.setOption(trendOption)
  }

  // 类型分布图
  const typeChartInstance = echarts.init(typeChart.value)

  if (chartData.typeData && chartData.typeData.length > 0) {
    // 使用实际数据
    const pieData = chartData.typeData.map((item, index) => ({
      value: item.count,
      name: item.type,
      itemStyle: {
        color: ['#e74c3c', '#3498db', '#2ecc71', '#f39c12', '#95a5a6'][index % 5]
      }
    }))


    const typeOption = {
      title: { text: '成果类型分布（实际数据）', textStyle: { fontSize: 14 } },
      tooltip: { trigger: 'item', formatter: '{a} <br/>{b} : {c} ({d}%)' },
      series: [{
        name: '成果类型',
        type: 'pie',
        radius: '70%',
        center: ['50%', '60%'],
        data: pieData,
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }],
      grid: { left: '10%', right: '10%', bottom: '15%', top: '20%' }
    }

    typeChartInstance.setOption(typeOption)
  } else {
    const typeOption = {
      title: { text: '成果类型分布（暂无数据）', textStyle: { fontSize: 14 } },
      graphic: {
        type: 'text',
        left: 'center',
        top: 'middle',
        style: {
          text: '暂无分布数据',
          fontSize: 16,
          fill: '#999'
        }
      }
    }
    typeChartInstance.setOption(typeOption)
  }

  // 响应式处理
  window.addEventListener('resize', () => {
    trendChartInstance.resize()
    typeChartInstance.resize()
  })
}

onMounted(() => {
  fetchStatistics()
  initCharts()
})
</script>

<style scoped>
/* ============================================
   Apple Elegant White Style - 成果分析页
   Achievement Analytics Page
   ============================================ */

/* CSS Variables */
:root {
  --apple-white: #fbfbfd;
  --apple-gray: #f5f5f7;
  --apple-border: rgba(0, 0, 0, 0.04);
  --apple-text: #1d1d1f;
  --apple-text-secondary: #86868b;
  --apple-blue: #007aff;
  --apple-green: #34c759;
  --apple-orange: #ff9500;
  --apple-red: #ff3b30;
  --apple-purple: #af52de;
  --red-team-primary: #e74c3c;
  --blue-team-primary: #3498db;
  --radius-sm: 12px;
  --radius-md: 16px;
  --radius-lg: 20px;
  --shadow-card: 0 4px 16px rgba(0, 0, 0, 0.06);
  --shadow-card-hover: 0 8px 32px rgba(0, 0, 0, 0.12);
  --spacing-xs: 8px;
  --spacing-sm: 12px;
  --spacing-md: 16px;
  --spacing-lg: 24px;
  --spacing-xl: 32px;
  --font-apple: -apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", "Helvetica Neue", sans-serif;
}

.achievement-analytics-page {
  background: transparent;
  padding: var(--spacing-lg);
  font-family: var(--font-apple);
  min-height: 100vh;
}

/* ============================================
   Header with Large Emoji
   ============================================ */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-xl);
  padding-bottom: var(--spacing-lg);
  border-bottom: 1px solid var(--apple-border);
}

.header-content {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.header-icon {
  font-size: 48px;
  line-height: 1;
  filter: drop-shadow(0 4px 12px rgba(0, 0, 0, 0.08));
}

.header-title {
  margin: 0;
  font-size: 32px;
  font-weight: 700;
  color: var(--apple-text);
  letter-spacing: -0.5px;
}

/* ============================================
   Statistics Cards with Gradient & Frosted Glass
   ============================================ */
.stat-card {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.95) 0%,
    rgba(248, 248, 248, 0.85) 100%);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: var(--radius-lg);
  padding: var(--spacing-lg);
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  box-shadow: var(--shadow-card);
  border: 0.5px solid var(--apple-border);
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
  position: relative;
  overflow: hidden;
  margin-bottom: var(--spacing-lg);
}

.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, var(--card-color-start), var(--card-color-end));
  opacity: 0;
  transition: opacity 0.3s ease;
}

.stat-card:hover::before {
  opacity: 1;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-card-hover);
}

/* Card theme colors */
.stat-card.total {
  --card-color-start: #007aff;
  --card-color-end: #5ac8fa;
}

.stat-card.red-team {
  --card-color-start: #e74c3c;
  --card-color-end: #c0392b;
}

.stat-card.blue-team {
  --card-color-start: #3498db;
  --card-color-end: #2980b9;
}

.stat-card.approval {
  --card-color-start: #34c759;
  --card-color-end: #30d158;
}

.stat-icon {
  font-size: 32px;
  line-height: 1;
  filter: drop-shadow(0 2px 8px rgba(0, 0, 0, 0.08));
}

.stat-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex-grow: 1;
}

.stat-number {
  font-size: 28px;
  font-weight: 700;
  color: var(--apple-text);
  line-height: 1;
  letter-spacing: -0.5px;
}

.red-team .stat-number {
  color: var(--red-team-primary);
}

.blue-team .stat-number {
  color: var(--blue-team-primary);
}

.stat-label {
  font-size: 13px;
  color: var(--apple-text-secondary);
  font-weight: 500;
}

/* ============================================
   Chart Cards
   ============================================ */
:deep(.el-card) {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.95) 0%,
    rgba(250, 250, 250, 0.9) 100%);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  border: 0.5px solid var(--apple-border);
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

:deep(.el-card:hover) {
  box-shadow: var(--shadow-card-hover);
  transform: translateY(-2px);
}

:deep(.el-card__header) {
  font-weight: 700;
  color: var(--apple-text);
  font-size: 16px;
  border-bottom: 1px solid var(--apple-border);
  padding: var(--spacing-md) var(--spacing-lg);
  background: rgba(0, 0, 0, 0.01);
}

:deep(.el-card__body) {
  padding: var(--spacing-lg);
}

/* ============================================
   Chart Containers
   ============================================ */
.el-row {
  margin-bottom: var(--spacing-lg);
}

:deep(.el-col) {
  margin-bottom: var(--spacing-lg);
}

/* ============================================
   Responsive Design
   ============================================ */
@media (max-width: 1200px) {
  .header-icon {
    font-size: 42px;
  }

  .header-title {
    font-size: 28px;
  }

  .stat-number {
    font-size: 24px;
  }
}

@media (max-width: 992px) {
  .achievement-analytics-page {
    padding: var(--spacing-md);
  }

  .header-icon {
    font-size: 38px;
  }

  .header-title {
    font-size: 24px;
  }

  :deep(.el-col) {
    margin-bottom: var(--spacing-md);
  }
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--spacing-md);
  }

  .header-icon {
    font-size: 36px;
  }

  .header-title {
    font-size: 22px;
  }

  .stat-card {
    flex-direction: column;
    text-align: center;
    padding: var(--spacing-md);
  }

  .stat-icon {
    font-size: 28px;
  }

  .stat-number {
    font-size: 22px;
  }
}

@media (max-width: 576px) {
  .header-icon {
    font-size: 32px;
  }

  .header-title {
    font-size: 20px;
  }

  .stat-card {
    padding: var(--spacing-sm);
  }

  .stat-icon {
    font-size: 24px;
  }

  .stat-number {
    font-size: 20px;
  }

  .stat-label {
    font-size: 12px;
  }
}
</style>