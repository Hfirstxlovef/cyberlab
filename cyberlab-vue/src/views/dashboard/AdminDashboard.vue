<template>
  <div class="admin-dashboard">
    <div class="dashboard-header">
      <div class="header-content">
        <h1>🔵 管理员驾驶舱（控制面板）</h1>
        <p>网络空间安全攻防演练 - 全局态势监控</p>
      </div>
    </div>

    <!-- 统计卡片区域 -->
    <div class="stats-cards">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon">🎯</div>
              <div class="stat-info">
                <div class="stat-number">{{ drillStats.running }}</div>
                <div class="stat-label">运行演练数</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon">🗡️</div>
              <div class="stat-info">
                <div class="stat-number">{{ teamStats.redTeams }}</div>
                <div class="stat-label">红队数量</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon">🛡️</div>
              <div class="stat-info">
                <div class="stat-number">{{ teamStats.blueTeams }}</div>
                <div class="stat-label">蓝队数量</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon">⚠️</div>
              <div class="stat-info">
                <div class="stat-number">{{ systemStats.alerts }}</div>
                <div class="stat-label">系统异常数</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 图表和控制区域 -->
    <div class="charts-section">
      <el-row :gutter="20">
        <!-- 攻防趋势图 -->
        <el-col :span="12">
          <el-card class="chart-card">
            <template #header>
              <div class="chart-header">
                <span>📈 攻防趋势图</span>
                <el-radio-group v-model="trendDays" size="small" @change="loadTrendData">
                  <el-radio-button :value="1">1天</el-radio-button>
                  <el-radio-button :value="3">3天</el-radio-button>
                  <el-radio-button :value="7">7天</el-radio-button>
                </el-radio-group>
              </div>
            </template>
            <div v-loading="trendLoading" class="chart-container">
              <div ref="trendChart" style="width: 100%; height: 100%;"></div>
            </div>
          </el-card>
        </el-col>

        <!-- 系统资源监控 -->
        <el-col :span="12">
          <el-card class="chart-card">
            <template #header>
              <span>💻 系统资源分配</span>
            </template>
            <div ref="resourceChart" class="chart-container"></div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 实时成果流 -->
      <el-row :gutter="20" style="margin-top: 20px;">
        <el-col :span="24">
          <AchievementFeed />
        </el-col>
      </el-row>
    </div>

    <!-- 最近活动日志 -->
    <div class="recent-activities">
      <el-card>
        <template #header>
          <span>📋 最近提交/告警日志</span>
        </template>
        <el-table :data="recentLogs" style="width: 100%">
          <el-table-column prop="time" label="时间" width="180" />
          <el-table-column prop="type" label="类型" width="120">
            <template #default="scope">
              <el-tag :type="getLogTypeColor(scope.row.type)">{{ scope.row.type }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="user" label="用户" width="120" />
          <el-table-column prop="action" label="操作" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="scope">
              <el-tag :type="scope.row.status === '成功' ? 'success' : 'danger'">
                {{ scope.row.status }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import AchievementFeed from '@/components/AchievementFeed.vue'
import { getAdminStats, getAdminTrendData, getAdminResourceData } from '@/api/dashboard'

const router = useRouter()

// 数据状态
const drillStats = ref({
  running: 0,
  total: 0,
  completed: 0
})

const teamStats = ref({
  redTeams: 0,
  blueTeams: 0,
  judges: 0
})

const systemStats = ref({
  alerts: 0,
  resources: 85,
  uptime: '99.9%'
})

const recentLogs = ref([])
const trendData = ref(null)
const resourceData = ref(null)
const loading = ref(true)

// ✅ 添加：时间范围状态（1天/3天/7天）
const trendDays = ref(7)

// ✅ 添加：图表加载状态
const trendLoading = ref(false)

// 获取管理员统计数据
const fetchAdminData = async () => {
  try {
    loading.value = true
    const stats = await getAdminStats()

    if (stats) {
      // 更新演练统计
      drillStats.value.running = stats.runningDrills || 0
      drillStats.value.total = stats.totalAchievements || 0
      drillStats.value.completed = stats.approvedAchievements || 0

      // 更新团队统计
      teamStats.value.redTeams = stats.redTeamCount || 0
      teamStats.value.blueTeams = stats.blueTeamCount || 0
      teamStats.value.judges = stats.totalUsers ? stats.totalUsers - stats.redTeamCount - stats.blueTeamCount : 0

      // 更新系统统计
      systemStats.value.alerts = stats.systemAlerts || 0
      systemStats.value.resources = stats.systemHealth || 85

      // 更新最近日志
      if (stats.recentLogs && stats.recentLogs.length > 0) {
        recentLogs.value = stats.recentLogs.map(log => ({
          time: log.time || '',
          type: log.type || '系统日志',
          user: log.user || 'unknown',
          action: log.action || '',
          status: log.status || '成功'
        }))
      }

      ElMessage.success('数据加载成功')
    }

    // 获取图表数据
    const [trend, resource] = await Promise.all([
      getAdminTrendData(trendDays.value),  // ✅ 修复：传入天数参数
      getAdminResourceData()
    ])

    trendData.value = trend
    resourceData.value = resource

  } catch (error) {
    console.error('获取管理员统计数据失败:', error)
    ElMessage.warning('数据加载失败，显示默认数据')

    // 使用默认数据
    drillStats.value = { running: 3, total: 15, completed: 12 }
    teamStats.value = { redTeams: 8, blueTeams: 6, judges: 3 }
    systemStats.value = { alerts: 2, resources: 85, uptime: '99.9%' }
    recentLogs.value = [
      {
        time: '14:30:25',
        type: '红队提交',
        user: 'red_team_01',
        action: '提交SQL注入漏洞报告',
        status: '成功'
      },
      {
        time: '14:28:15',
        type: '蓝队防御',
        user: 'blue_team_02',
        action: '上传防御日志',
        status: '成功'
      },
      {
        time: '14:25:10',
        type: '系统告警',
        user: 'system',
        action: '检测到异常登录尝试',
        status: '警告'
      }
    ]
  } finally {
    loading.value = false
  }
}

// ✅ 添加：单独加载趋势图数据（用于时间范围切换）
const loadTrendData = async () => {
  try {
    trendLoading.value = true
    const trend = await getAdminTrendData(trendDays.value)
    trendData.value = trend

    // 重新初始化趋势图
    await nextTick()
    initTrendChart()

    ElMessage.success(`已切换到${trendDays.value}天数据`)
  } catch (error) {
    console.error('加载趋势数据失败:', error)
    ElMessage.error('趋势数据加载失败')
  } finally {
    trendLoading.value = false
  }
}

// 图表引用
const trendChart = ref(null)
const resourceChart = ref(null)

// 初始化趋势图
const initTrendChart = () => {
  if (!trendChart.value) return

  const chart = echarts.init(trendChart.value)

  // 使用真实数据或默认数据
  const timeLabels = trendData.value?.timeLabels || ['09:00', '10:00', '11:00', '12:00', '13:00', '14:00', '15:00']
  const redData = trendData.value?.redTeamData || [2, 5, 3, 8, 6, 12, 9]
  const blueData = trendData.value?.blueTeamData || [1, 3, 2, 6, 4, 8, 7]

  const option = {
    title: {
      text: '红蓝队提交数量趋势',
      left: 'center'
    },
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['红队提交', '蓝队提交'],
      bottom: 0
    },
    xAxis: {
      type: 'category',
      data: timeLabels
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '红队提交',
        type: 'line',
        data: redData,
        itemStyle: { color: '#ff4757' }
      },
      {
        name: '蓝队提交',
        type: 'line',
        data: blueData,
        itemStyle: { color: '#3742fa' }
      }
    ]
  }
  chart.setOption(option)
}

// 初始化资源图
const initResourceChart = () => {
  if (!resourceChart.value) return

  const chart = echarts.init(resourceChart.value)

  // 使用真实数据或默认数据
  const resourceList = resourceData.value?.resourceList || [
    { value: 35, name: '靶机资源' },
    { value: 25, name: '容器资源' },
    { value: 20, name: '网络带宽' },
    { value: 20, name: '存储空间' }
  ]

  const option = {
    title: {
      text: '系统资源使用情况',
      left: 'center'
    },
    tooltip: {
      trigger: 'item'
    },
    series: [
      {
        name: '资源使用',
        type: 'pie',
        radius: '60%',
        data: resourceList,
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }
    ]
  }
  chart.setOption(option)
}

// 工具函数
const getLogTypeColor = (type) => {
  const colorMap = {
    '红队提交': 'danger',
    '蓝队防御': 'primary',
    '系统告警': 'warning',
    '管理操作': 'success'
  }
  return colorMap[type] || 'info'
}

// 操作函数（已清理）

onMounted(async () => {
  // 先获取数据
  await fetchAdminData()

  // 然后初始化图表
  await nextTick()
  initTrendChart()
  initResourceChart()
})
</script>

<style scoped>
/* ========== Apple 风格管理员仪表盘 ========== */

.admin-dashboard {
  padding: var(--spacing-lg, 32px);
  background: var(--apple-white, #fbfbfd);
  min-height: 100vh;
  font-family: var(--font-apple, -apple-system, BlinkMacSystemFont, "SF Pro Display", sans-serif);
}

.dashboard-header {
  margin-bottom: var(--spacing-2xl, 64px);
  position: relative;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  min-height: 120px;
  margin-top: -80px;
}

.header-content {
  text-align: center;
  position: relative;
  top: 0;
}

.dashboard-header h1 {
  color: var(--apple-text-primary, #1d1d1f);
  margin: 0 0 var(--spacing-sm, 16px) 0;
  font-size: var(--font-3xl, 48px);
  font-weight: var(--font-weight-semibold, 600);
  letter-spacing: var(--letter-spacing-tight, -0.5px);
  line-height: 1.1;
}

.dashboard-header p {
  margin: 0;
  color: var(--apple-text-secondary, #6e6e73);
  font-size: var(--font-lg, 18px);
  font-weight: var(--font-weight-regular, 400);
}

.stats-cards {
  margin-bottom: var(--spacing-2xl, 64px);
}

.stat-card {
  /* Apple 产品卡片风格 */
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.9) 0%,
    rgba(248, 248, 248, 0.8) 100%);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: none;
  border-radius: var(--radius-xl, 20px);
  box-shadow: var(--shadow-card);
  transition: all var(--duration-slow, 0.4s) var(--ease-out);
}

.stat-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--shadow-card-hover);
}

.stat-content {
  display: flex;
  align-items: center;
  padding: var(--spacing-md, 24px);
}

.stat-icon {
  font-size: 48px;
  margin-right: var(--spacing-md, 24px);
}

.stat-number {
  font-size: var(--font-2xl, 32px);
  font-weight: var(--font-weight-semibold, 600);
  color: var(--apple-text-primary, #1d1d1f);
  letter-spacing: -0.5px;
}

.stat-label {
  color: var(--apple-text-secondary, #6e6e73);
  font-size: var(--font-md, 16px);
  font-weight: var(--font-weight-regular, 400);
  margin-top: 4px;
}

.charts-section {
  margin-bottom: var(--spacing-2xl, 64px);
}

.chart-card {
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 0.5px solid rgba(0, 0, 0, 0.04);
  border-radius: var(--radius-xl, 20px);
  box-shadow: var(--shadow-card);
  transition: all var(--duration-slow, 0.4s) var(--ease-out);
}

.chart-card:hover {
  box-shadow: var(--shadow-lg);
}

.chart-container {
  height: 320px;
  padding: var(--spacing-sm, 16px);
}

.recent-activities {
  margin-bottom: var(--spacing-2xl, 64px);
}

/* Element Plus 卡片覆盖 */
:deep(.el-card) {
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 0.5px solid rgba(0, 0, 0, 0.04);
  border-radius: var(--radius-xl, 20px);
  box-shadow: var(--shadow-card);
}

:deep(.el-card__header) {
  font-size: var(--font-lg, 18px);
  font-weight: var(--font-weight-semibold, 600);
  color: var(--apple-text-primary, #1d1d1f);
  border-bottom: 0.5px solid rgba(0, 0, 0, 0.06);
  padding: var(--spacing-md, 24px);
}

/* ✅ 添加：图表 header 样式 */
.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.chart-header span {
  font-size: 18px;
  font-weight: 600;
}

/* Apple 风格时间选择按钮 */
:deep(.el-radio-group) {
  background: rgba(0, 0, 0, 0.04);
  border-radius: 8px;
  padding: 2px;
}

:deep(.el-radio-button) {
  --el-radio-button-checked-bg-color: #007aff;
  --el-radio-button-checked-text-color: #ffffff;
}

:deep(.el-radio-button__inner) {
  border: none !important;
  border-radius: 6px !important;
  background: transparent;
  color: #1d1d1f;
  font-size: 13px;
  font-weight: 500;
  padding: 6px 12px;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

:deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: #007aff !important;
  color: #ffffff !important;
  box-shadow: 0 2px 8px rgba(0, 122, 255, 0.3);
  transform: translateY(-1px);
}

:deep(.el-radio-button:hover .el-radio-button__inner) {
  background: rgba(0, 122, 255, 0.1);
  color: #007aff;
}

/* ========== Apple 风格优化结束 ========== */
</style>