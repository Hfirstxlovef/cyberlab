<template>
  <div class="judge-dashboard">
    <div class="dashboard-header">
      <div class="header-content">
        <h1>⚖️ 裁判驾驶舱（评审面板）</h1>
        <p>网络空间安全攻防演练 - 全局态势监控</p>
      </div>
    </div>

    <!-- 评分总览卡片 -->
    <div class="overview-stats">
      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon">🏆</div>
          <div class="stat-info">
            <div class="stat-number">{{ judgeStats.drillCount }}</div>
            <div class="stat-label">演练场次</div>
          </div>
        </div>
      </el-card>

      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon">🗡️</div>
          <div class="stat-info">
            <div class="stat-number">{{ judgeStats.redSuccessRate }}%</div>
            <div class="stat-label">红队成功率</div>
          </div>
        </div>
      </el-card>

      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon">🛡️</div>
          <div class="stat-info">
            <div class="stat-number">{{ judgeStats.blueDefenseRate }}%</div>
            <div class="stat-label">蓝队防御率</div>
          </div>
        </div>
      </el-card>

      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon">📋</div>
          <div class="stat-info">
            <div class="stat-number">{{ judgeStats.pendingCount }}</div>
            <div class="stat-label">待评审数量</div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 图表分析区域 -->
    <div class="charts-section">
      <el-card class="chart-card">
        <template #header>
          <span>📈 成果质量评估</span>
        </template>
        <div ref="qualityChart" class="chart-container"></div>
      </el-card>

      <el-card class="chart-card">
        <template #header>
          <span>🎯 红蓝队对比分析</span>
        </template>
        <div ref="comparisonChart" class="chart-container"></div>
      </el-card>
    </div>

    <!-- 待评审列表和操作区域 -->
    <div class="review-section">
      <el-card class="review-card">
        <template #header>
          <div class="review-header">
            <span>📋 待评审提交列表</span>
            <div class="review-filters">
              <el-select v-model="filterTeam" placeholder="队伍" size="small" style="width: 100px;">
                <el-option label="全部" value="" />
                <el-option label="红队" value="red" />
                <el-option label="蓝队" value="blue" />
              </el-select>
              <el-select v-model="filterStatus" placeholder="状态" size="small" style="width: 100px;">
                <el-option label="全部" value="" />
                <el-option label="待审核" value="pending" />
                <el-option label="可疑" value="suspicious" />
              </el-select>
            </div>
          </div>
        </template>
        <el-table :data="filteredSubmissions" size="small" max-height="400">
          <el-table-column prop="time" label="提交时间" width="120" />
          <el-table-column prop="team" label="队伍" width="80">
            <template #default="scope">
              <el-tag :type="scope.row.team === 'red' ? 'danger' : 'primary'">
                {{ scope.row.team === 'red' ? '红队' : '蓝队' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="type" label="类型" width="100" />
          <el-table-column prop="description" label="描述" />
          <el-table-column prop="status" label="状态" width="80">
            <template #default="scope">
              <el-tag :type="getStatusColor(scope.row.status)">{{ scope.row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200">
            <template #default="scope">
              <el-button size="small" type="success" @click="approveSubmission(scope.row)">
                通过
              </el-button>
              <el-button size="small" type="danger" @click="rejectSubmission(scope.row)">
                驳回
              </el-button>
              <el-button size="small" type="warning" @click="markSuspicious(scope.row)">
                可疑
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import { getJudgeStats, getPendingSubmissions, approveAchievement, rejectAchievement } from '@/api/dashboard'

const router = useRouter()

// 数据状态
const judgeStats = ref({
  drillCount: 0,
  redSuccessRate: 0,
  blueDefenseRate: 0,
  pendingCount: 0
})

const submissions = ref([])
const filterTeam = ref('')
const filterStatus = ref('')

// 图表引用
const qualityChart = ref(null)
const comparisonChart = ref(null)

// 过滤后的提交列表
const filteredSubmissions = computed(() => {
  return submissions.value.filter(item => {
    const teamMatch = !filterTeam.value || item.team === filterTeam.value
    const statusMatch = !filterStatus.value || item.status === filterStatus.value
    return teamMatch && statusMatch
  })
})

// 获取裁判统计数据
const fetchJudgeStats = async () => {
  try {
    const stats = await getJudgeStats()
    judgeStats.value = stats || {
      drillCount: 5,
      redSuccessRate: 72,
      blueDefenseRate: 85,
      pendingCount: 12
    }

    const pending = await getPendingSubmissions()
    submissions.value = pending || [
      { id: 1, time: '14:30', team: 'red', type: 'SQL注入', description: '成功获取用户数据', status: '待审核' },
      { id: 2, time: '14:25', team: 'blue', type: '拦截记录', description: '成功拦截XSS攻击', status: '待审核' },
      { id: 3, time: '14:20', team: 'red', type: '权限提升', description: '获取管理员权限', status: '可疑' }
    ]
  } catch (error) {
  }
}

// 初始化质量评估图表
const initQualityChart = () => {
  const chart = echarts.init(qualityChart.value)
  const option = {
    title: { text: '成果质量分布' },
    tooltip: { trigger: 'axis' },
    radar: {
      indicator: [
        { name: '技术难度', max: 100 },
        { name: '创新性', max: 100 },
        { name: '完整性', max: 100 },
        { name: '实用性', max: 100 },
        { name: '文档质量', max: 100 }
      ]
    },
    series: [{
      type: 'radar',
      data: [
        {
          value: [85, 75, 90, 80, 85],
          name: '红队平均',
          itemStyle: { color: '#ff4757' }
        },
        {
          value: [80, 70, 95, 85, 90],
          name: '蓝队平均',
          itemStyle: { color: '#3742fa' }
        }
      ]
    }]
  }
  chart.setOption(option)
}

// 初始化对比分析图表
const initComparisonChart = () => {
  const chart = echarts.init(comparisonChart.value)
  const option = {
    title: { text: '红蓝队成果对比' },
    tooltip: { trigger: 'axis' },
    legend: { data: ['红队', '蓝队'] },
    xAxis: {
      type: 'category',
      data: ['第1轮', '第2轮', '第3轮', '第4轮', '第5轮']
    },
    yAxis: { type: 'value' },
    series: [
      {
        name: '红队',
        type: 'bar',
        data: [12, 15, 18, 22, 25],
        itemStyle: { color: '#ff4757' }
      },
      {
        name: '蓝队',
        type: 'bar',
        data: [8, 12, 15, 18, 20],
        itemStyle: { color: '#3742fa' }
      }
    ]
  }
  chart.setOption(option)
}

// 审核操作
const approveSubmission = async (submission) => {
  try {
    await approveAchievement(submission.id, 'judge')
    ElMessage.success('审核通过')
    fetchJudgeStats()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const rejectSubmission = async (submission) => {
  try {
    const { value: reason } = await ElMessageBox.prompt('请输入驳回原因', '驳回提交', {
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })

    await rejectAchievement(submission.id, 'judge', reason)
    ElMessage.success('已驳回')
    fetchJudgeStats()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

const markSuspicious = (submission) => {
  submission.status = '可疑'
  ElMessage.warning('已标记为可疑')
}
const getStatusColor = (status) => {
  const colors = {
    '待审核': 'warning',
    '可疑': 'danger',
    '已通过': 'success',
    '已驳回': 'info'
  }
  return colors[status] || 'info'
}

onMounted(async () => {
  await fetchJudgeStats()
  await nextTick()
  initQualityChart()
  initComparisonChart()
})
</script>

<style scoped>
/* ========== Apple 风格裁判仪表盘 ========== */

.judge-dashboard {
  padding: var(--spacing-lg, 32px);
  background: linear-gradient(135deg,
    rgba(251, 251, 253, 1) 0%,
    rgba(245, 245, 247, 0.98) 50%,
    rgba(248, 248, 250, 1) 100%);
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

.overview-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
  margin-bottom: var(--spacing-2xl, 64px);
}

.stat-card {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.9) 0%,
    rgba(248, 248, 248, 0.8) 100%);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: none;
  border-radius: var(--radius-xl, 20px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  transition: all 0.4s cubic-bezier(0.19, 1, 0.22, 1);
}

.stat-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
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
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: var(--spacing-2xl, 64px);
}

.chart-card {
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 0.5px solid rgba(0, 0, 0, 0.04);
  border-radius: var(--radius-xl, 20px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  transition: all 0.4s cubic-bezier(0.19, 1, 0.22, 1);
}

.chart-card:hover {
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
}

.chart-container {
  height: 300px;
  padding: var(--spacing-sm, 16px);
}

.review-section {
  margin-bottom: var(--spacing-2xl, 64px);
}

.review-card {
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 0.5px solid rgba(0, 0, 0, 0.04);
  border-radius: var(--radius-xl, 20px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  transition: all 0.4s cubic-bezier(0.19, 1, 0.22, 1);
}

.review-card:hover {
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.review-header span {
  font-size: var(--font-lg, 18px);
  font-weight: var(--font-weight-semibold, 600);
  color: var(--apple-text-primary, #1d1d1f);
}

.review-filters {
  display: flex;
  gap: 12px;
}

/* 美化表格 */
:deep(.el-table) {
  background: transparent;
  color: var(--apple-text-primary, #1d1d1f);
}

:deep(.el-table th.el-table__cell) {
  background: rgba(245, 245, 247, 0.6);
  color: var(--apple-text-secondary, #6e6e73);
  font-weight: var(--font-weight-medium, 500);
  border-bottom: 0.5px solid rgba(0, 0, 0, 0.06);
  font-size: 14px;
}

:deep(.el-table tr) {
  background: transparent;
  transition: all 0.3s ease;
}

:deep(.el-table tbody tr:hover > td) {
  background: rgba(245, 245, 247, 0.4) !important;
}

:deep(.el-table td.el-table__cell) {
  border-bottom: 0.5px solid rgba(0, 0, 0, 0.04);
  color: var(--apple-text-primary, #1d1d1f);
  font-size: 14px;
}

/* 美化按钮 */
:deep(.el-button--success) {
  background: linear-gradient(135deg, #34c759 0%, #30d158 100%);
  border: none;
  border-radius: 8px;
  font-weight: 500;
  transition: all 0.3s ease;
}

:deep(.el-button--success:hover) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(52, 199, 89, 0.3);
}

:deep(.el-button--danger) {
  background: linear-gradient(135deg, #ff3b30 0%, #ff453a 100%);
  border: none;
  border-radius: 8px;
  font-weight: 500;
  transition: all 0.3s ease;
}

:deep(.el-button--danger:hover) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(255, 59, 48, 0.3);
}

:deep(.el-button--warning) {
  background: linear-gradient(135deg, #ff9500 0%, #ff9f0a 100%);
  border: none;
  border-radius: 8px;
  font-weight: 500;
  transition: all 0.3s ease;
}

:deep(.el-button--warning:hover) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(255, 149, 0, 0.3);
}

/* 美化标签 */
:deep(.el-tag) {
  border: none;
  border-radius: 6px;
  font-weight: 500;
  padding: 4px 12px;
}

:deep(.el-tag--danger) {
  background: rgba(255, 59, 48, 0.1);
  color: #ff3b30;
}

:deep(.el-tag--primary) {
  background: rgba(0, 122, 255, 0.1);
  color: #007aff;
}

:deep(.el-tag--warning) {
  background: rgba(255, 149, 0, 0.1);
  color: #ff9500;
}

:deep(.el-tag--success) {
  background: rgba(52, 199, 89, 0.1);
  color: #34c759;
}

:deep(.el-tag--info) {
  background: rgba(142, 142, 147, 0.1);
  color: #8e8e93;
}

/* 美化选择器 */
:deep(.el-select) {
  --el-select-border-color-hover: rgba(0, 122, 255, 0.4);
}

:deep(.el-select .el-input__wrapper) {
  background: rgba(255, 255, 255, 0.8);
  border-radius: 8px;
  border: 0.5px solid rgba(0, 0, 0, 0.08);
  box-shadow: none;
  transition: all 0.3s ease;
}

:deep(.el-select .el-input__wrapper:hover) {
  border-color: rgba(0, 122, 255, 0.4);
  box-shadow: 0 2px 8px rgba(0, 122, 255, 0.1);
}

:deep(.el-select .el-input__wrapper.is-focus) {
  border-color: #007aff;
  box-shadow: 0 0 0 3px rgba(0, 122, 255, 0.1);
}

/* Element Plus 卡片覆盖 */
:deep(.el-card) {
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 0.5px solid rgba(0, 0, 0, 0.04);
  border-radius: var(--radius-xl, 20px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
}

:deep(.el-card__header) {
  font-size: var(--font-lg, 18px);
  font-weight: var(--font-weight-semibold, 600);
  color: var(--apple-text-primary, #1d1d1f);
  border-bottom: 0.5px solid rgba(0, 0, 0, 0.06);
  padding: var(--spacing-md, 24px);
}

@media (max-width: 768px) {
  .charts-section {
    grid-template-columns: 1fr;
  }
}

/* ========== Apple 风格优化结束 ========== */
</style>