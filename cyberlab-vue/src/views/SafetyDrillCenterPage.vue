<template>
  <div class="safety-drill-page" :class="themeClass">
    <!-- 错误状态显示 -->
    <el-alert
      v-if="componentError"
      title="页面加载错误"
      type="error"
      description="页面模块加载失败，请刷新页面重试"
      show-icon
      :closable="false"
      class="error-alert"
    >
      <template #default>
        <el-button type="primary" @click="window.location.reload()">刷新页面</el-button>
      </template>
    </el-alert>

    <!-- Dashboard Header with large emoji and centered title -->
    <div class="dashboard-header" v-if="!componentError">
      <div class="header-content">
        <h2>🛡️ 安全演练中心</h2>
        <p class="page-description">管理和监控安全演练，提升攻防对抗能力</p>
      </div>
    </div>

    <!-- 统计卡片 -->
    <el-row v-if="!componentError" :gutter="20" class="stats-cards">
      <el-col :xs="24" :sm="8" :md="8">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon running">🎯</div>
            <div class="stat-info">
              <div class="stat-number">{{ runningCount }}</div>
              <div class="stat-label">运行中</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="8" :md="8">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon paused">⏸️</div>
            <div class="stat-info">
              <div class="stat-number">{{ pausedCount }}</div>
              <div class="stat-label">已暂停</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="8" :md="8">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon total">📊</div>
            <div class="stat-info">
              <div class="stat-number">{{ totalCount }}</div>
              <div class="stat-label">演练总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 操作按钮卡片 -->
    <el-card v-if="!componentError" class="action-card">
      <div class="action-buttons-wrapper">
        <el-button v-if="role === 'admin'" type="primary" :icon="Plus" @click="handleCreate" size="large">
          新建演练
        </el-button>
        <el-button :icon="Refresh" @click="fetchDrills" size="large" :loading="loading">
          刷新列表
        </el-button>
      </div>
    </el-card>

    <!-- 演练列表 -->
    <el-card v-if="!componentError" class="content-card">
      <el-table
        :data="drills"
        stripe
        style="width: 100%"
        v-loading="loading"
        element-loading-text="正在加载演练数据..."
        class="drill-table"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="演练名称" min-width="180">
          <template #default="scope">
            <el-link type="primary" @click="goToDetail(scope.row.id)">
              {{ scope.row.name }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="scope">
            <el-tag
              :type="getStatusType(scope.row.status)"
              size="small"
            >
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="topologyProjectId" label="网络拓扑" width="160">
          <template #default="scope">
            <div v-if="scope.row.topologyProjectId" class="topology-status">
              <el-tag type="success" size="small">已关联</el-tag>
              <span class="topology-id">{{ scope.row.topologyProjectId }}</span>
            </div>
            <el-tag v-else type="info" size="small">未配置</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="vulnerabilityConfig" label="漏洞配置" width="120">
          <template #default="scope">
            {{ scope.row.vulnerabilityConfig ? '已配置' : '未配置' }}
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="scope">
            {{ scope.row.createdAt ? new Date(scope.row.createdAt).toLocaleString() : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <div class="action-buttons">
              <el-button
                v-if="scope.row.status === 'running'"
                size="small"
                type="warning"
                @click="handlePause(scope.row)"
                :loading="scope.row.loading"
              >
                暂停
              </el-button>
              <el-button
                v-else-if="scope.row.status === 'paused'"
                size="small"
                type="success"
                @click="handleStart(scope.row)"
                :loading="scope.row.loading"
              >
                启动
              </el-button>
              <el-button
                v-else
                size="small"
                type="primary"
                @click="handleStart(scope.row)"
                :loading="scope.row.loading"
              >
                启动
              </el-button>
              <el-button size="small" type="danger" @click="handleDelete(scope.row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        layout="prev, pager, next"
        :page-size="10"
        :total="drills?.length || 0"
        @current-change="handlePageChange"
        v-if="!loading && drills && drills.length > 0"
      />

      <!-- 空状态显示 -->
      <div v-if="!loading && (!drills || drills.length === 0)" class="empty-state">
        <el-empty description="暂无演练数据">
          <el-button v-if="role === 'admin'" type="primary" :icon="Plus" @click="handleCreate">创建第一个演练</el-button>
        </el-empty>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onErrorCaptured } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { getUserRole } from '@/utils/auth'

// 错误处理
const componentError = ref(null)

// 添加错误捕获
onErrorCaptured((error, instance, info) => {
  componentError.value = error
  ElMessage.error('页面加载出错，请刷新重试')
  return false
})

// 动态导入API，避免模块加载错误
let getAllRanges, pauseRange, deleteRange, startRange

const loadAPI = async () => {
  try {
    const rangeAPI = await import('@/api/range')
    getAllRanges = rangeAPI.getAllRanges
    pauseRange = rangeAPI.pauseRange
    deleteRange = rangeAPI.deleteRange
    startRange = rangeAPI.startRange
  } catch (error) {
    ElMessage.error('系统模块加载失败，请刷新页面重试')
    throw error
  }
}

const router = useRouter()
const drills = ref([]) // 确保初始化为空数组
const loading = ref(false) // 添加加载状态

// 主题支持
const role = getUserRole() || ''
const themeClass = computed(() => role === 'blue' ? 'theme-blue' : 'theme-admin')

// 计算统计数据
const runningCount = computed(() => {
  return drills.value.filter(d => d.status === 'running').length
})

const pausedCount = computed(() => {
  return drills.value.filter(d => d.status === 'paused').length
})

const totalCount = computed(() => {
  return drills.value.length
})

const fetchDrills = async () => {
  if (!getAllRanges) {
    await loadAPI()
  }

  loading.value = true
  try {
    const response = await getAllRanges()
    // 修复：axios拦截器已经返回了data，不需要再访问response.data

    // 验证响应数据并设置默认值
    if (Array.isArray(response)) {
      drills.value = response
    } else if (response && Array.isArray(response.data)) {
      drills.value = response.data
    } else {
      drills.value = []
      ElMessage.warning('演练数据格式异常，显示空列表')
    }

  } catch (error) {
    // 确保失败时drills仍为空数组，不是undefined
    drills.value = []
    ElMessage.error('获取演练数据失败')
  } finally {
    loading.value = false
  }
}

const handleCreate = () => {
  router.push('/drills/create')
}

const handlePause = async (drill) => {
  if (!pauseRange) {
    await loadAPI()
  }

  // 设置单个演练的loading状态
  drill.loading = true

  try {
    await pauseRange(drill.id)
    ElMessage.success(`已暂停演练 ${drill.name}`)
    // 重新获取数据以更新状态
    await fetchDrills()
  } catch (error) {
    ElMessage.error('暂停失败')
  } finally {
    drill.loading = false
  }
}

const handleStart = async (drill) => {
  if (!startRange) {
    await loadAPI()
  }

  // 设置单个演练的loading状态
  drill.loading = true

  try {
    await startRange(drill.id)
    ElMessage.success(`已启动演练 ${drill.name}`)
    // 重新获取数据以更新状态
    await fetchDrills()
  } catch (error) {
    ElMessage.error('启动失败')
  } finally {
    drill.loading = false
  }
}

const handlePageChange = () => {
  // 分页逻辑预留
}

const handleDelete = async (drill) => {
  if (!deleteRange) {
    await loadAPI()
  }

  try {
    await deleteRange(drill.id)
    ElMessage.success('删除成功')
    // 重新获取数据以更新列表
    await fetchDrills()
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

const goToDetail = (id) => {
  router.push(`/drills/${id}`)
}

// 状态相关的工具函数
const getStatusType = (status) => {
  const statusMap = {
    'running': 'success',
    'paused': 'warning',
    'deleted': 'danger',
    'stopped': 'info'
  }
  return statusMap[status] || 'info'
}

const getStatusText = (status) => {
  const statusTextMap = {
    'running': '运行中',
    'paused': '已暂停',
    'deleted': '已删除',
    'stopped': '已停止'
  }
  return statusTextMap[status] || status
}

onMounted(() => {
  fetchDrills()
})
</script>

<style scoped>
/* ============================================
   Apple Elegant White Style - 苹果高雅白风格
   ============================================ */

/* CSS Variables for consistency */
:root {
  --apple-white: #fbfbfd;
  --apple-gray: #f5f5f7;
  --apple-border: rgba(0, 0, 0, 0.04);
  --apple-text: #1d1d1f;
  --apple-text-secondary: #3c3c43;
  --apple-blue: #007aff;
  --apple-green: #34c759;
  --apple-orange: #ff9500;
  --apple-red: #ff3b30;
  --radius-sm: 12px;
  --radius-md: 16px;
  --radius-lg: 20px;
  --radius-xl: 24px;
  --shadow-card: 0 4px 16px rgba(0, 0, 0, 0.06);
  --shadow-card-hover: 0 8px 32px rgba(0, 0, 0, 0.12);
  --spacing-xs: 8px;
  --spacing-sm: 12px;
  --spacing-md: 16px;
  --spacing-lg: 24px;
  --spacing-xl: 32px;
  --spacing-2xl: 48px;
  --font-apple: -apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", "Helvetica Neue", sans-serif;
}

.safety-drill-page {
  padding: var(--spacing-xl);
  background: var(--apple-white);
  min-height: 100vh;
  font-family: var(--font-apple);
}

/* ============================================
   Dashboard Header - Large Emoji & Centered Title
   ============================================ */
.dashboard-header {
  text-align: center;
  margin-bottom: var(--spacing-2xl);
  padding: var(--spacing-xl) 0;
}

.header-content h2 {
  font-size: 48px;
  font-weight: 700;
  color: var(--apple-text);
  margin: 0 0 var(--spacing-sm) 0;
  letter-spacing: -0.5px;
  line-height: 1.1;
}

.page-description {
  font-size: 17px;
  color: var(--apple-text-secondary);
  margin: 0;
  font-weight: 400;
}

/* ============================================
   Statistics Cards - 毛玻璃效果
   ============================================ */
.stats-cards {
  margin-bottom: var(--spacing-xl);
}

.stat-card {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.95) 0%,
    rgba(248, 248, 248, 0.85) 100%);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 0.5px solid var(--apple-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  transition: all 0.4s cubic-bezier(0.19, 1, 0.22, 1);
  overflow: hidden;
  cursor: default;
}

.stat-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--shadow-card-hover);
}

.stat-card :deep(.el-card__body) {
  padding: var(--spacing-lg);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.stat-icon {
  font-size: 48px;
  line-height: 1;
  filter: drop-shadow(0 2px 8px rgba(0, 0, 0, 0.1));
}

.stat-icon.running {
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.05);
  }
}

.stat-info {
  flex: 1;
}

.stat-number {
  font-size: 36px;
  font-weight: 700;
  color: var(--apple-text);
  line-height: 1;
  margin-bottom: var(--spacing-xs);
  font-variant-numeric: tabular-nums;
}

.stat-label {
  font-size: 14px;
  color: var(--apple-text-secondary);
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

/* ============================================
   Action Card - 操作按钮区域
   ============================================ */
.action-card {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.95) 0%,
    rgba(248, 248, 248, 0.85) 100%);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 0.5px solid var(--apple-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  margin-bottom: var(--spacing-xl);
  transition: all 0.3s ease;
}

.action-card :deep(.el-card__body) {
  padding: var(--spacing-lg);
}

.action-buttons-wrapper {
  display: flex;
  gap: var(--spacing-md);
  flex-wrap: wrap;
  justify-content: center;
}

.action-buttons-wrapper .el-button {
  min-width: 140px;
  height: 44px;
  font-size: 15px;
  font-weight: 500;
  border-radius: var(--radius-sm);
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

.action-buttons-wrapper .el-button--primary {
  background: var(--apple-blue);
  border-color: var(--apple-blue);
}

.action-buttons-wrapper .el-button--primary:hover {
  background: #0051d5;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.3);
}

/* ============================================
   Content Card - 演练列表
   ============================================ */
.content-card {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.95) 0%,
    rgba(248, 248, 248, 0.85) 100%);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 0.5px solid var(--apple-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  transition: all 0.3s ease;
}

.content-card :deep(.el-card__body) {
  padding: var(--spacing-lg);
}

/* ============================================
   Table Styling
   ============================================ */
.drill-table {
  border-radius: var(--radius-md);
  overflow: hidden;
}

.drill-table :deep(.el-table__header-wrapper) {
  background: var(--apple-gray);
}

.drill-table :deep(.el-table__header th) {
  background: transparent;
  color: var(--apple-text);
  font-weight: 600;
  font-size: 13px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.drill-table :deep(.el-table__row) {
  transition: all 0.2s ease;
}

.drill-table :deep(.el-table__row:hover) {
  background: rgba(0, 122, 255, 0.03);
}

.topology-status {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
}

.topology-id {
  font-size: 12px;
  color: #3c3c43;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.action-buttons {
  display: flex;
  gap: var(--spacing-xs);
  flex-wrap: wrap;
}

.action-buttons .el-button {
  margin: 0;
  border-radius: var(--radius-sm);
  font-weight: 500;
  transition: all 0.2s ease;
}

.action-buttons .el-button:hover {
  transform: translateY(-1px);
}

/* ============================================
   Pagination
   ============================================ */
.pagination {
  margin-top: var(--spacing-lg);
  text-align: center;
}

.pagination :deep(.el-pager li) {
  border-radius: var(--radius-sm);
  transition: all 0.2s ease;
}

.pagination :deep(.el-pager li.is-active) {
  background: var(--apple-blue);
}

/* ============================================
   Empty State
   ============================================ */
.empty-state {
  padding: var(--spacing-2xl) 0;
  text-align: center;
}

.empty-state .el-empty {
  padding: var(--spacing-xl) 0;
}

.empty-state .el-button {
  min-width: 180px;
  height: 44px;
  font-size: 15px;
  font-weight: 500;
  border-radius: var(--radius-sm);
}

/* ============================================
   Error Alert
   ============================================ */
.error-alert {
  margin-bottom: var(--spacing-xl);
  border-radius: var(--radius-md);
}

/* ============================================
   Responsive Design - Mobile Optimization
   ============================================ */
@media (max-width: 768px) {
  .safety-drill-page {
    padding: var(--spacing-md);
  }

  .header-content h2 {
    font-size: 36px;
  }

  .page-description {
    font-size: 15px;
  }

  .stats-cards {
    margin-bottom: var(--spacing-lg);
  }

  .stat-card {
    margin-bottom: var(--spacing-md);
  }

  .stat-icon {
    font-size: 36px;
  }

  .stat-number {
    font-size: 28px;
  }

  .stat-label {
    font-size: 12px;
  }

  .action-buttons-wrapper {
    flex-direction: column;
  }

  .action-buttons-wrapper .el-button {
    width: 100%;
  }

  .action-buttons {
    flex-direction: column;
    width: 100%;
  }

  .action-buttons .el-button {
    width: 100%;
  }
}

@media (max-width: 576px) {
  .header-content h2 {
    font-size: 32px;
  }

  .dashboard-header {
    margin-bottom: var(--spacing-lg);
    padding: var(--spacing-md) 0;
  }
}

/* ==================== 蓝队主题样式 ==================== */
/* 主容器 */
.safety-drill-page.theme-blue {
  background: linear-gradient(135deg, #0a1428 0%, #0d1a2d 50%, #0f1620 100%) !important;
  min-height: 100vh;
  padding: var(--spacing-xl);
}

/* Dashboard 头部区域 */
.safety-drill-page.theme-blue .dashboard-header {
  background: linear-gradient(135deg,
    rgba(20, 30, 50, 0.85) 0%,
    rgba(13, 26, 45, 0.95) 100%) !important;
  border: 1px solid rgba(70, 130, 180, 0.35) !important;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.45),
              0 0 20px rgba(70, 130, 180, 0.08) !important;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  margin-bottom: var(--spacing-xl);
  padding: var(--spacing-xl);
}

.safety-drill-page.theme-blue .header-content h2 {
  color: #ffffff !important;
  text-shadow: 0 0 20px rgba(0, 212, 255, 0.3) !important;
  font-weight: 600;
}

.safety-drill-page.theme-blue .header-content p {
  color: rgba(255, 255, 255, 0.75) !important;
}

/* 统计卡片 */
.safety-drill-page.theme-blue :deep(.el-card) {
  background: linear-gradient(135deg,
    rgba(20, 30, 50, 0.85) 0%,
    rgba(13, 26, 45, 0.95) 100%) !important;
  border: 1px solid rgba(70, 130, 180, 0.35) !important;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.45),
              0 0 20px rgba(70, 130, 180, 0.08) !important;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1) !important;
}

.safety-drill-page.theme-blue :deep(.el-card:hover) {
  transform: translateY(-4px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.5),
              0 0 30px rgba(70, 130, 180, 0.15) !important;
  border-color: rgba(70, 130, 180, 0.6) !important;
}

.safety-drill-page.theme-blue :deep(.el-card .el-card__body) {
  padding: var(--spacing-lg);
}

/* 统计卡片数值 */
.safety-drill-page.theme-blue .stat-value {
  color: #00d4ff !important;
  text-shadow: 0 0 15px rgba(0, 212, 255, 0.4) !important;
  font-weight: 700;
}

.safety-drill-page.theme-blue .stat-label {
  color: rgba(255, 255, 255, 0.7) !important;
  font-weight: 500;
}

/* Action Card 和 Content Card */
.safety-drill-page.theme-blue .action-card,
.safety-drill-page.theme-blue .content-card {
  background: linear-gradient(135deg,
    rgba(20, 30, 50, 0.85) 0%,
    rgba(13, 26, 45, 0.95) 100%) !important;
  border: 1px solid rgba(70, 130, 180, 0.35) !important;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.45),
              0 0 20px rgba(70, 130, 180, 0.08) !important;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
}

/* 表格容器 */
.safety-drill-page.theme-blue .table-container {
  background: linear-gradient(135deg,
    rgba(13, 26, 45, 0.95) 0%,
    rgba(10, 20, 40, 0.98) 100%) !important;
  border: 1px solid rgba(70, 130, 180, 0.4) !important;
  border-radius: 12px;
  padding: var(--spacing-lg);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4),
              0 0 15px rgba(70, 130, 180, 0.15) !important;
  backdrop-filter: blur(25px);
  -webkit-backdrop-filter: blur(25px);
}

/* 表格样式 */
.safety-drill-page.theme-blue :deep(.el-table) {
  background: transparent !important;
  color: #ffffff !important;
}

.safety-drill-page.theme-blue :deep(.el-table::before) {
  display: none;
}

.safety-drill-page.theme-blue :deep(.el-table__header-wrapper) {
  background: linear-gradient(135deg,
    rgba(70, 130, 180, 0.25) 0%,
    rgba(30, 144, 255, 0.2) 100%) !important;
  border-radius: 8px 8px 0 0;
}

.safety-drill-page.theme-blue :deep(.el-table thead) {
  color: #ffffff !important;
}

.safety-drill-page.theme-blue :deep(.el-table th.el-table__cell) {
  background: transparent !important;
  color: #ffffff !important;
  border-bottom: 2px solid rgba(70, 130, 180, 0.4) !important;
  font-weight: 600;
  text-shadow: 0 0 8px rgba(0, 212, 255, 0.2) !important;
}

.safety-drill-page.theme-blue :deep(.el-table tr) {
  background: transparent !important;
}

.safety-drill-page.theme-blue :deep(.el-table tbody tr) {
  background: rgba(20, 30, 50, 0.3) !important;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1) !important;
}

.safety-drill-page.theme-blue :deep(.el-table tbody tr:hover) {
  background: rgba(70, 130, 180, 0.25) !important;
  transform: translateX(4px);
}

.safety-drill-page.theme-blue :deep(.el-table td.el-table__cell) {
  background: transparent !important;
  color: rgba(255, 255, 255, 0.9) !important;
  border-bottom: 1px solid rgba(70, 130, 180, 0.2) !important;
}

.safety-drill-page.theme-blue :deep(.el-table--enable-row-hover .el-table__body tr:hover > td) {
  background: transparent !important;
}

/* 按钮样式 */
.safety-drill-page.theme-blue :deep(.el-button--primary) {
  background: linear-gradient(135deg, #4682b4 0%, #1e90ff 100%) !important;
  border: 1px solid rgba(70, 130, 180, 0.6) !important;
  color: #ffffff !important;
  box-shadow: 0 4px 15px rgba(70, 130, 180, 0.2),
              inset 0 1px 0 rgba(255, 255, 255, 0.1) !important;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1) !important;
  font-weight: 500;
}

.safety-drill-page.theme-blue :deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, #5a9fd4 0%, #3ea8ff 100%) !important;
  box-shadow: 0 6px 20px rgba(70, 130, 180, 0.35),
              inset 0 1px 0 rgba(255, 255, 255, 0.15) !important;
  transform: translateY(-2px);
  border-color: rgba(70, 130, 180, 0.8) !important;
}

.safety-drill-page.theme-blue :deep(.el-button--primary:active) {
  transform: translateY(0);
}

.safety-drill-page.theme-blue :deep(.el-button--success) {
  background: linear-gradient(135deg, #34c759 0%, #30d158 100%) !important;
  border: 1px solid rgba(52, 199, 89, 0.6) !important;
  color: #ffffff !important;
  box-shadow: 0 4px 15px rgba(52, 199, 89, 0.2),
              inset 0 1px 0 rgba(255, 255, 255, 0.1) !important;
}

.safety-drill-page.theme-blue :deep(.el-button--success:hover) {
  background: linear-gradient(135deg, #4ad766 0%, #46e165 100%) !important;
  box-shadow: 0 6px 20px rgba(52, 199, 89, 0.35),
              inset 0 1px 0 rgba(255, 255, 255, 0.15) !important;
  transform: translateY(-2px);
}

.safety-drill-page.theme-blue :deep(.el-button--warning) {
  background: linear-gradient(135deg, #ff9500 0%, #ffaa00 100%) !important;
  border: 1px solid rgba(255, 149, 0, 0.6) !important;
  color: #ffffff !important;
  box-shadow: 0 4px 15px rgba(255, 149, 0, 0.2),
              inset 0 1px 0 rgba(255, 255, 255, 0.1) !important;
}

.safety-drill-page.theme-blue :deep(.el-button--warning:hover) {
  background: linear-gradient(135deg, #ffaa20 0%, #ffbb20 100%) !important;
  box-shadow: 0 6px 20px rgba(255, 149, 0, 0.35),
              inset 0 1px 0 rgba(255, 255, 255, 0.15) !important;
  transform: translateY(-2px);
}

.safety-drill-page.theme-blue :deep(.el-button--danger) {
  background: linear-gradient(135deg, #ff3b30 0%, #ff4545 100%) !important;
  border: 1px solid rgba(255, 59, 48, 0.6) !important;
  color: #ffffff !important;
  box-shadow: 0 4px 15px rgba(255, 59, 48, 0.2),
              inset 0 1px 0 rgba(255, 255, 255, 0.1) !important;
}

.safety-drill-page.theme-blue :deep(.el-button--danger:hover) {
  background: linear-gradient(135deg, #ff5245 0%, #ff5c5c 100%) !important;
  box-shadow: 0 6px 20px rgba(255, 59, 48, 0.35),
              inset 0 1px 0 rgba(255, 255, 255, 0.15) !important;
  transform: translateY(-2px);
}

.safety-drill-page.theme-blue :deep(.el-button.is-link) {
  color: #00d4ff !important;
  font-weight: 500;
}

.safety-drill-page.theme-blue :deep(.el-button.is-link:hover) {
  color: #4ddbff !important;
  text-shadow: 0 0 8px rgba(0, 212, 255, 0.4) !important;
}

/* 分页样式 */
.safety-drill-page.theme-blue :deep(.el-pagination) {
  padding: var(--spacing-lg) 0;
}

.safety-drill-page.theme-blue :deep(.el-pagination button),
.safety-drill-page.theme-blue :deep(.el-pager li) {
  background: rgba(20, 30, 50, 0.6) !important;
  color: rgba(255, 255, 255, 0.8) !important;
  border: 1px solid rgba(70, 130, 180, 0.3) !important;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1) !important;
}

.safety-drill-page.theme-blue :deep(.el-pagination button:hover),
.safety-drill-page.theme-blue :deep(.el-pager li:hover) {
  background: rgba(70, 130, 180, 0.4) !important;
  color: #ffffff !important;
  border-color: rgba(70, 130, 180, 0.6) !important;
}

.safety-drill-page.theme-blue :deep(.el-pager li.is-active) {
  background: linear-gradient(135deg, #4682b4 0%, #1e90ff 100%) !important;
  color: #ffffff !important;
  border-color: rgba(70, 130, 180, 0.8) !important;
  box-shadow: 0 4px 12px rgba(70, 130, 180, 0.3) !important;
}

/* Tag 样式 */
.safety-drill-page.theme-blue :deep(.el-tag) {
  border: 1px solid rgba(70, 130, 180, 0.4) !important;
  background: rgba(20, 30, 50, 0.6) !important;
  color: #ffffff !important;
  font-weight: 500;
}

.safety-drill-page.theme-blue :deep(.el-tag--success) {
  background: rgba(52, 199, 89, 0.2) !important;
  border-color: rgba(52, 199, 89, 0.5) !important;
  color: #34c759 !important;
}

.safety-drill-page.theme-blue :deep(.el-tag--warning) {
  background: rgba(255, 149, 0, 0.2) !important;
  border-color: rgba(255, 149, 0, 0.5) !important;
  color: #ff9500 !important;
}

.safety-drill-page.theme-blue :deep(.el-tag--danger) {
  background: rgba(255, 59, 48, 0.2) !important;
  border-color: rgba(255, 59, 48, 0.5) !important;
  color: #ff3b30 !important;
}

.safety-drill-page.theme-blue :deep(.el-tag--info) {
  background: rgba(70, 130, 180, 0.2) !important;
  border-color: rgba(70, 130, 180, 0.5) !important;
  color: #4682b4 !important;
}

/* 输入框样式 */
.safety-drill-page.theme-blue :deep(.el-input__wrapper) {
  background: rgba(20, 30, 50, 0.6) !important;
  border: 1px solid rgba(70, 130, 180, 0.35) !important;
  box-shadow: 0 0 0 1px transparent inset !important;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1) !important;
}

.safety-drill-page.theme-blue :deep(.el-input__wrapper:hover) {
  border-color: rgba(70, 130, 180, 0.5) !important;
}

.safety-drill-page.theme-blue :deep(.el-input__wrapper.is-focus) {
  border-color: #4682b4 !important;
  box-shadow: 0 0 0 1px #4682b4 inset,
              0 0 12px rgba(70, 130, 180, 0.3) !important;
}

.safety-drill-page.theme-blue :deep(.el-input__inner) {
  color: #ffffff !important;
}

.safety-drill-page.theme-blue :deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.4) !important;
}

/* 选择器样式 */
.safety-drill-page.theme-blue :deep(.el-select .el-input__wrapper) {
  background: rgba(20, 30, 50, 0.6) !important;
  border: 1px solid rgba(70, 130, 180, 0.35) !important;
}

/* Empty 空状态样式 */
.safety-drill-page.theme-blue :deep(.el-empty) {
  padding: var(--spacing-xl) 0;
}

.safety-drill-page.theme-blue :deep(.el-empty__description p) {
  color: rgba(255, 255, 255, 0.6) !important;
  font-size: 16px;
}

.safety-drill-page.theme-blue :deep(.el-empty__image svg) {
  fill: rgba(255, 255, 255, 0.3) !important;
}

/* Alert 警告样式 */
.safety-drill-page.theme-blue :deep(.el-alert) {
  background: rgba(255, 59, 48, 0.15) !important;
  border: 1px solid rgba(255, 59, 48, 0.4) !important;
  border-radius: 8px;
}

.safety-drill-page.theme-blue :deep(.el-alert__title) {
  color: #ff3b30 !important;
  font-weight: 600;
}

.safety-drill-page.theme-blue :deep(.el-alert__description) {
  color: rgba(255, 255, 255, 0.8) !important;
}

/* Loading 加载样式 */
.safety-drill-page.theme-blue :deep(.el-loading-mask) {
  background: rgba(10, 20, 40, 0.85) !important;
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

.safety-drill-page.theme-blue :deep(.el-loading-spinner .circular) {
  stroke: #00d4ff !important;
}

.safety-drill-page.theme-blue :deep(.el-loading-text) {
  color: #ffffff !important;
  text-shadow: 0 0 10px rgba(0, 212, 255, 0.3) !important;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .safety-drill-page.theme-blue {
    padding: var(--spacing-lg);
  }

  .safety-drill-page.theme-blue .dashboard-header {
    padding: var(--spacing-lg);
    margin-bottom: var(--spacing-lg);
  }

  .safety-drill-page.theme-blue :deep(.el-card) {
    margin-bottom: var(--spacing-md);
  }
}
</style>
