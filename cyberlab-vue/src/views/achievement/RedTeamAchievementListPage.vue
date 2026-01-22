<template>
  <div class="red-team-achievement-list-page">
    <!-- 动态光晕背景层 -->
    <div class="dynamic-glow-layer">
      <div class="glow-spot glow-1"></div>
      <div class="glow-spot glow-2"></div>
      <div class="glow-spot glow-3"></div>
    </div>

    <el-card class="page-card">
      <template #header>
        <div class="card-header">
          <span>🏆 我的成果提交</span>
          <div class="header-filter">
            <el-button @click="() => fetchMyAchievements(true)" :icon="Refresh" round size="small">刷新</el-button>
            <select v-model="statusFilter" @change="handleStatusChange" class="custom-select status-filter-select">
              <option value="">全部</option>
              <option value="pending">待审批</option>
              <option value="approved">已通过</option>
              <option value="rejected">已驳回</option>
            </select>
          </div>
        </div>
      </template>

      <!-- 成果列表表格 -->
      <el-table
        :data="achievementList"
        v-loading="loading"
        style="width: 100%"
        :header-cell-style="{ background: 'rgba(30, 30, 30, 0.5)', color: 'rgba(255, 255, 255, 0.9)', fontWeight: '600', borderBottom: '1px solid rgba(255, 59, 48, 0.2)' }"
        :row-style="{ height: '60px' }"
        :cell-style="{ padding: '16px 12px', borderBottom: '1px solid rgba(255, 59, 48, 0.1)' }"
        empty-text="暂无提交记录">
        <el-table-column prop="id" label="ID" width="80" align="center"></el-table-column>
        <el-table-column prop="targetName" label="攻击目标" width="200" show-overflow-tooltip></el-table-column>
        <el-table-column prop="submitTime" label="提交时间" width="180" align="center">
          <template #default="scope">
            {{ formatDateTime(scope.row.submitTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120" align="center">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)" size="small">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="得分" width="120" align="center">
          <template #default="scope">
            <span v-if="scope.row.status === 'approved' && scope.row.finalScore !== null && scope.row.finalScore !== undefined" class="score-badge">
              {{ scope.row.finalScore }}
            </span>
            <span v-else class="no-score">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" fixed="right">
          <template #default="scope">
            <div class="action-buttons">
              <el-button
                size="small"
                type="primary"
                @click="viewDetail(scope.row)">
                查看详情
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        :current-page="currentPage"
        :page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        class="pagination">
      </el-pagination>
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="成果详情" width="70%">
      <div v-if="currentAchievement" class="achievement-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="攻击队名称">{{ currentAchievement.teamName }}</el-descriptions-item>
          <el-descriptions-item label="攻击目标">{{ currentAchievement.targetName }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ formatDateTime(currentAchievement.submitTime) }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(currentAchievement.status)">{{ getStatusText(currentAchievement.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="攻击工具/方法" :span="2">{{ currentAchievement.attackMethod || '未填写' }}</el-descriptions-item>
          <el-descriptions-item label="攻击描述" :span="2">
            <div class="description-text">{{ currentAchievement.description }}</div>
          </el-descriptions-item>
        </el-descriptions>

        <!-- 截图展示 -->
        <div v-if="currentAchievement.screenshots" class="file-section">
          <h4>漏洞截图</h4>
          <div class="screenshot-gallery">
            <el-image
              v-for="(screenshot, index) in getScreenshots(currentAchievement.screenshots)"
              :key="index"
              :src="screenshot"
              :preview-src-list="getScreenshots(currentAchievement.screenshots)"
              class="screenshot-item">
            </el-image>
          </div>
        </div>

        <!-- 证明文件 -->
        <div v-if="currentAchievement.proofFiles" class="file-section">
          <h4>证明文件</h4>
          <div class="proof-files">
            <el-link
              v-for="(file, index) in getProofFiles(currentAchievement.proofFiles)"
              :key="index"
              :href="file"
              target="_blank"
              class="file-link">
              {{ getFileName(file) }}
            </el-link>
          </div>
        </div>

        <!-- 打分信息 (已通过的成果) -->
        <div v-if="currentAchievement.status === 'approved'" class="score-info">
          <h4>🏆 打分信息</h4>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="成果类型">
              {{ getAchievementTypeName(currentAchievement.achievementType) }}
            </el-descriptions-item>
            <el-descriptions-item label="最终得分">
              <span class="final-score-display">{{ currentAchievement.finalScore }} 分</span>
            </el-descriptions-item>
            <el-descriptions-item label="基础分值">
              {{ currentAchievement.baseScore }} 分
            </el-descriptions-item>
            <el-descriptions-item label="审批时间">
              {{ formatDateTime(currentAchievement.reviewTime) }}
            </el-descriptions-item>
            <el-descriptions-item v-if="currentAchievement.scoreReason" label="打分说明" :span="2">
              <div class="score-reason-text">{{ currentAchievement.scoreReason }}</div>
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 驳回理由 -->
        <div v-if="currentAchievement.status === 'rejected' && currentAchievement.rejectReason" class="reject-reason">
          <h4>驳回理由</h4>
          <p>{{ currentAchievement.rejectReason }}</p>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import axios from '@/api/axios'
import { getUserId, getUsername } from '@/utils/auth'
import { getTeamByUserId } from '@/api/team'

// 响应式数据
const loading = ref(false)
const achievementList = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const statusFilter = ref('')
const detailDialogVisible = ref(false)
const currentAchievement = ref(null)

// 用户战队信息
const userTeam = ref(null)

// 获取我的成果列表
const fetchMyAchievements = async (showMessage = false) => {
  try {
    loading.value = true

    // 如果没有战队信息，先获取战队
    if (!userTeam.value) {
      try {
        const userId = getUserId()
        if (userId) {
          userTeam.value = await getTeamByUserId(userId)
        }
      } catch (error) {
      }
    }

    // 尝试调用后端API
    try {
      // 有战队：使用战队ID过滤
      if (userTeam.value) {
        // 动态构建参数对象
        const params = {
          teamId: userTeam.value.id,
          page: currentPage.value - 1,
          size: pageSize.value
        }

        // 只在 statusFilter 有值时才添加 status 参数
        if (statusFilter.value && statusFilter.value !== '') {
          params.status = statusFilter.value
        }

        const response = await axios.get('/achievements/team/submissions', { params })

        if (response && response.content) {
          achievementList.value = response.content
          total.value = response.totalElements
          if (showMessage) {
            ElMessage.success(`刷新成功，共 ${response.totalElements} 条记录`)
          }
        } else {
          achievementList.value = []
          total.value = 0
          if (showMessage) {
            ElMessage.success('刷新成功')
          }
        }
      } else {
        // 无战队：查询个人提交（从本地存储或全部成果中过滤）
        const personalTeamName = `个人 - ${getUsername()}`
        // 尝试使用admin接口获取全部成果，然后过滤
        const response = await axios.get('/achievements/admin/list', {
          params: {
            page: currentPage.value - 1,
            size: 100, // 获取足够多的数据用于前端过滤
            status: statusFilter.value || undefined
          }
        })

        if (response && response.content) {
          // 前端过滤出个人提交的成果
          const personalAchievements = response.content.filter(item => item.teamName === personalTeamName)

          // 手动分页
          const start = (currentPage.value - 1) * pageSize.value
          const end = start + pageSize.value
          achievementList.value = personalAchievements.slice(start, end)
          total.value = personalAchievements.length
          if (showMessage) {
            ElMessage.success(`刷新成功，共 ${personalAchievements.length} 条记录`)
          }
        } else {
          achievementList.value = []
          total.value = 0
          if (showMessage) {
            ElMessage.success('刷新成功')
          }
        }
      }
    } catch (apiError) {
      // API未就绪，使用本地存储的提交记录
      const allSubmissions = JSON.parse(localStorage.getItem('redTeamSubmissions') || '[]')

      // 确定筛选条件
      let filteredSubmissions = allSubmissions
      if (userTeam.value) {
        // 有战队：按战队名称过滤
        filteredSubmissions = allSubmissions.filter(item => item.teamName === userTeam.value.name)
      } else {
        // 无战队：按个人身份过滤
        const personalTeamName = `个人 - ${getUsername()}`
        filteredSubmissions = allSubmissions.filter(item => item.teamName === personalTeamName)
      }

      // 根据状态筛选
      if (statusFilter.value && statusFilter.value !== '') {
        filteredSubmissions = filteredSubmissions.filter(item => item.status === statusFilter.value)
      }

      // 分页处理
      const start = (currentPage.value - 1) * pageSize.value
      const end = start + pageSize.value
      achievementList.value = filteredSubmissions.slice(start, end)
      total.value = filteredSubmissions.length

      if (showMessage) {
        if (filteredSubmissions.length === 0) {
          ElMessage.info('刷新成功，暂无提交记录')
        } else {
          ElMessage.success(`刷新成功，共 ${filteredSubmissions.length} 条记录`)
        }
      } else if (filteredSubmissions.length === 0) {
        ElMessage.info('暂无提交记录')
      }
    }
  } catch (error) {
    ElMessage.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

// 查看详情
const viewDetail = async (achievement) => {
  try {
    // 尝试从API获取详情
    try {
      const response = await axios.get(`/achievements/${achievement.id}`)
      currentAchievement.value = response
    } catch {
      // API未就绪，使用本地数据
      currentAchievement.value = achievement
    }
    detailDialogVisible.value = true
  } catch (error) {
    ElMessage.error('获取详情失败')
  }
}

// 状态筛选变化
const handleStatusChange = () => {
  currentPage.value = 1
  fetchMyAchievements()
}

// 分页大小变化
const handleSizeChange = (newSize) => {
  pageSize.value = newSize
  currentPage.value = 1
  fetchMyAchievements()
}

// 当前页变化
const handleCurrentChange = (newPage) => {
  currentPage.value = newPage
  fetchMyAchievements()
}

// 辅助方法
const formatDateTime = (dateTime) => {
  if (!dateTime) return '-'
  return new Date(dateTime).toLocaleString('zh-CN')
}

const getStatusType = (status) => {
  const types = {
    pending: 'warning',
    approved: 'success',
    rejected: 'danger'
  }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = {
    pending: '待审批',
    approved: '已通过',
    rejected: '已驳回'
  }
  return texts[status] || '未知'
}

const getScreenshots = (screenshots) => {
  return screenshots ? screenshots.split(',') : []
}

const getProofFiles = (proofFiles) => {
  return proofFiles ? proofFiles.split(',') : []
}

const getFileName = (filePath) => {
  return filePath.split('/').pop()
}

// 根据成果类型value获取显示名称
const getAchievementTypeName = (typeValue) => {
  if (!typeValue) return '未知类型'

  // 类型名称映射（从AchievementType枚举）
  const typeNames = {
    'red_vulnerability_exploit': '漏洞发现与利用',
    'red_privilege_escalation': '权限提升',
    'red_lateral_movement': '横向移动',
    'red_data_exfiltration': '数据窃取',
    'red_social_engineering': '社会工程学',
    'red_backdoor_implant': '后门植入',
    'red_reconnaissance': '信息收集',
    'red_zero_day': '0day漏洞发现',
    'blue_intrusion_detection': '入侵检测与响应',
    'blue_threat_intelligence': '威胁情报分析',
    'blue_log_analysis': '日志分析与关联',
    'blue_incident_response': '应急响应处置',
    'blue_vulnerability_remediation': '漏洞修复加固',
    'blue_forensics': '取证分析',
    'blue_security_policy': '安全策略优化',
    'blue_apt_attribution': 'APT攻击溯源'
  }

  return typeNames[typeValue] || typeValue
}

// 页面加载时获取数据
onMounted(() => {
  fetchMyAchievements()
})
</script>

<style scoped>
/* ============================================
   红队深色主题 - Red Team Dark Theme
   我的成果查看页
   ============================================ */

/* CSS Variables */
:root {
  --red-team-primary: #ff3b30;
  --red-team-secondary: #ff6b59;
  --red-team-dark-bg: rgba(20, 20, 20, 0.6);
  --red-team-darker-bg: rgba(30, 30, 30, 0.6);
  --red-team-border: rgba(255, 59, 48, 0.2);
  --radius-sm: 12px;
  --radius-md: 16px;
  --radius-lg: 20px;
  --spacing-xs: 8px;
  --spacing-sm: 12px;
  --spacing-md: 16px;
  --spacing-lg: 24px;
  --spacing-xl: 32px;
  --font-apple: -apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", "Helvetica Neue", sans-serif;
}

.red-team-achievement-list-page {
  background: linear-gradient(135deg, #0a0a0a 0%, #1a0d0d 50%, #0f0f0f 100%);
  min-height: 100vh;
  padding: var(--spacing-lg);
  font-family: var(--font-apple);
  position: relative;
  overflow: hidden;
}

/* 动态光晕背景层 */
.dynamic-glow-layer {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
}

.glow-spot {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.3;
}

.glow-1 {
  top: 15%;
  left: 25%;
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(255, 59, 48, 0.2) 0%, transparent 70%);
  animation: glow-breath-1 8s ease-in-out infinite;
}

.glow-2 {
  bottom: 20%;
  right: 15%;
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, rgba(204, 0, 0, 0.15) 0%, transparent 70%);
  animation: glow-breath-2 10s ease-in-out infinite;
  animation-delay: 2s;
}

.glow-3 {
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 350px;
  height: 350px;
  background: radial-gradient(circle, rgba(255, 59, 48, 0.12) 0%, transparent 70%);
  animation: glow-breath-3 6s ease-in-out infinite;
  animation-delay: 1s;
}

@keyframes glow-breath-1 {
  0%, 100% {
    opacity: 0.25;
    transform: scale(1);
  }
  50% {
    opacity: 0.4;
    transform: scale(1.15);
  }
}

@keyframes glow-breath-2 {
  0%, 100% {
    opacity: 0.2;
    transform: scale(1);
  }
  50% {
    opacity: 0.35;
    transform: scale(1.2);
  }
}

@keyframes glow-breath-3 {
  0%, 100% {
    opacity: 0.18;
    transform: translate(-50%, -50%) scale(1);
  }
  50% {
    opacity: 0.32;
    transform: translate(-50%, -50%) scale(1.25);
  }
}

/* 主卡片 - 深色毛玻璃 */
.page-card {
  background: var(--red-team-dark-bg);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 0.5px solid var(--red-team-border);
  border-left: 4px solid var(--red-team-primary);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5), inset 0 0 0 0.5px rgba(255, 255, 255, 0.05);
  border-radius: var(--radius-lg);
  position: relative;
  z-index: 1;
  max-width: 1400px;
  margin: 0 auto;
}

.page-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, var(--red-team-primary), var(--red-team-secondary));
  opacity: 1;
}

.page-card:hover {
  box-shadow: 0 12px 48px rgba(255, 59, 48, 0.3), inset 0 0 0 0.5px rgba(255, 255, 255, 0.05);
  border-color: rgba(255, 59, 48, 0.3);
}

/* 卡片头部 */
.page-card :deep(.el-card__header) {
  background: rgba(30, 30, 30, 0.5);
  border-bottom: 1px solid var(--red-team-border);
  padding: var(--spacing-lg);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 18px;
  font-weight: 700;
  color: #ffffff;
  text-shadow: 0 0 20px rgba(255, 59, 48, 0.4);
}

.header-filter {
  display: flex;
  gap: var(--spacing-sm);
  align-items: center;
}

/* 原生select样式 */
.custom-select.status-filter-select {
  background: var(--red-team-darker-bg);
  border: 0.5px solid var(--red-team-border);
  color: rgba(255, 255, 255, 0.9);
  padding: 8px 16px;
  border-radius: var(--radius-sm);
  font-family: var(--font-apple);
  font-size: 14px;
  transition: all 0.3s ease;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  min-width: 120px;
}

.custom-select.status-filter-select:hover {
  background: rgba(30, 30, 30, 0.7);
  border-color: rgba(255, 59, 48, 0.4);
  box-shadow: 0 4px 12px rgba(255, 59, 48, 0.2);
}

.custom-select.status-filter-select:focus {
  outline: none;
  background: rgba(30, 30, 30, 0.8);
  border-color: rgba(255, 59, 48, 0.6);
  box-shadow: 0 4px 16px rgba(255, 59, 48, 0.3);
}

.custom-select.status-filter-select option {
  background: rgba(20, 20, 20, 0.95);
  color: rgba(255, 255, 255, 0.9);
  padding: 8px 12px;
}

/* 刷新按钮 */
.header-filter :deep(.el-button) {
  background: linear-gradient(135deg, rgba(255, 59, 48, 0.25) 0%, rgba(255, 59, 48, 0.4) 100%);
  border: 0.5px solid rgba(255, 59, 48, 0.4);
  color: #ffffff;
  border-radius: var(--radius-sm);
}

.header-filter :deep(.el-button:hover) {
  background: linear-gradient(135deg, rgba(255, 59, 48, 0.35) 0%, rgba(255, 59, 48, 0.5) 100%);
  border-color: rgba(255, 59, 48, 0.6);
  transform: translateY(-2px);
}

/* 卡片主体 */
.page-card :deep(.el-card__body) {
  background: transparent;
  padding: var(--spacing-lg);
}

/* 表格样式 */
:deep(.el-table) {
  background: transparent;
  color: rgba(255, 255, 255, 0.9);
  border-radius: var(--radius-md);
  overflow: hidden;
}

:deep(.el-table__inner-wrapper::before) {
  display: none;
}

:deep(.el-table__row) {
  background: transparent;
  transition: all 0.2s ease;
}

:deep(.el-table__row:hover) {
  background: rgba(255, 59, 48, 0.08) !important;
}

:deep(.el-table__empty-text) {
  color: rgba(255, 255, 255, 0.5);
}

:deep(.el-table td.el-table__cell) {
  color: rgba(255, 255, 255, 0.85);
  border-bottom: 1px solid rgba(255, 59, 48, 0.1);
}

/* Loading样式 */
:deep(.el-loading-mask) {
  background-color: rgba(10, 10, 10, 0.8);
  backdrop-filter: blur(10px);
}

:deep(.el-loading-spinner .circular) {
  stroke: var(--red-team-primary);
}

/* 标签样式 */
:deep(.el-tag) {
  border-radius: var(--radius-sm);
  border: none;
  font-weight: 600;
  padding: 4px 12px;
  font-size: 12px;
}

:deep(.el-tag--warning) {
  background: rgba(255, 149, 0, 0.2);
  color: #ff9500;
}

:deep(.el-tag--success) {
  background: rgba(52, 199, 89, 0.2);
  color: #34c759;
}

:deep(.el-tag--danger) {
  background: rgba(255, 59, 48, 0.2);
  color: #ff6b59;
}

/* 操作按钮 */
.action-buttons {
  display: flex;
  gap: var(--spacing-xs);
  justify-content: center;
}

.action-buttons :deep(.el-button) {
  background: linear-gradient(135deg, rgba(255, 59, 48, 0.25) 0%, rgba(255, 59, 48, 0.4) 100%);
  border: 0.5px solid rgba(255, 59, 48, 0.4);
  color: #ffffff;
  border-radius: var(--radius-sm);
  font-weight: 600;
  transition: all 0.3s ease;
}

.action-buttons :deep(.el-button:hover) {
  background: linear-gradient(135deg, rgba(255, 59, 48, 0.35) 0%, rgba(255, 59, 48, 0.5) 100%);
  border-color: rgba(255, 59, 48, 0.6);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255, 59, 48, 0.3);
}

/* 分页样式 */
.pagination {
  margin-top: var(--spacing-lg);
  text-align: center;
  padding: var(--spacing-md) 0;
}

:deep(.el-pagination) {
  font-family: var(--font-apple);
  font-weight: 500;
}

:deep(.el-pagination button),
:deep(.el-pager li) {
  border-radius: var(--spacing-xs);
  background: rgba(30, 30, 30, 0.5);
  border: 0.5px solid rgba(255, 255, 255, 0.15);
  color: rgba(255, 255, 255, 0.85);
  transition: all 0.2s ease;
}

:deep(.el-pagination button:hover),
:deep(.el-pager li:hover) {
  background: rgba(255, 59, 48, 0.15);
  border-color: rgba(255, 59, 48, 0.4);
  color: #ffffff;
}

:deep(.el-pager li.is-active) {
  background: var(--red-team-primary);
  color: white;
  border-color: var(--red-team-primary);
}

:deep(.el-pagination__sizes .el-select .el-input__wrapper) {
  background: rgba(30, 30, 30, 0.6);
  border: 0.5px solid rgba(255, 59, 48, 0.2);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

:deep(.el-pagination__jump) {
  color: rgba(255, 255, 255, 0.85);
}

:deep(.el-pagination__total) {
  color: rgba(255, 255, 255, 0.85);
}

/* 详情对话框 */
:deep(.el-dialog) {
  background: rgba(20, 20, 20, 0.95);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 0.5px solid var(--red-team-border);
  border-radius: var(--radius-lg);
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.6);
}

:deep(.el-dialog__header) {
  background: rgba(30, 30, 30, 0.8);
  border-bottom: 1px solid var(--red-team-border);
  padding: var(--spacing-lg);
}

:deep(.el-dialog__title) {
  font-weight: 700;
  color: #ffffff;
  font-size: 18px;
  text-shadow: 0 0 15px rgba(255, 59, 48, 0.4);
}

:deep(.el-dialog__body) {
  padding: var(--spacing-lg);
  background: transparent;
}

:deep(.el-dialog__close) {
  color: rgba(255, 255, 255, 0.7);
}

:deep(.el-dialog__close:hover) {
  color: var(--red-team-primary);
}

/* 详情内容 */
.achievement-detail {
  max-height: 600px;
  overflow-y: auto;
  padding: var(--spacing-sm);
}

.achievement-detail::-webkit-scrollbar {
  width: 8px;
}

.achievement-detail::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 4px;
}

.achievement-detail::-webkit-scrollbar-thumb {
  background: rgba(255, 59, 48, 0.3);
  border-radius: 4px;
}

.achievement-detail::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 59, 48, 0.5);
}

:deep(.el-descriptions) {
  border-radius: var(--radius-md);
  overflow: hidden;
}

:deep(.el-descriptions__label) {
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  background: rgba(30, 30, 30, 0.5) !important;
}

:deep(.el-descriptions__content) {
  color: rgba(255, 255, 255, 0.85);
  background: rgba(20, 20, 20, 0.3) !important;
}

:deep(.el-descriptions__table) {
  border-color: rgba(255, 59, 48, 0.2) !important;
}

:deep(.el-descriptions__cell) {
  border-color: rgba(255, 59, 48, 0.2) !important;
}

.description-text {
  white-space: pre-wrap;
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.85);
  padding: var(--spacing-sm);
  background: rgba(30, 30, 30, 0.3);
  border-radius: var(--radius-sm);
}

/* 文件区域 */
.file-section {
  margin-top: var(--spacing-lg);
  padding: var(--spacing-md);
  background: rgba(255, 59, 48, 0.08);
  border-radius: var(--radius-md);
  border: 0.5px solid rgba(255, 59, 48, 0.2);
}

.file-section h4 {
  margin: 0 0 var(--spacing-md) 0;
  color: var(--red-team-secondary);
  font-weight: 700;
  font-size: 15px;
  text-shadow: 0 0 10px rgba(255, 59, 48, 0.3);
}

.screenshot-gallery {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-sm);
}

.screenshot-item {
  width: 120px;
  height: 120px;
  border-radius: var(--radius-md);
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  transition: all 0.3s ease;
  cursor: pointer;
  border: 1px solid rgba(255, 59, 48, 0.2);
}

.screenshot-item:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 16px rgba(255, 59, 48, 0.4);
}

.proof-files {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.file-link {
  display: inline-flex;
  align-items: center;
  padding: var(--spacing-xs) var(--spacing-sm);
  background: rgba(255, 59, 48, 0.12);
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 500;
  transition: all 0.2s ease;
  width: fit-content;
  color: rgba(255, 255, 255, 0.85);
  text-decoration: none;
}

.file-link:hover {
  background: rgba(255, 59, 48, 0.2);
  transform: translateX(4px);
  color: var(--red-team-secondary);
}

/* 驳回理由区域 */
.reject-reason {
  margin-top: var(--spacing-lg);
  padding: var(--spacing-md);
  background: rgba(255, 59, 48, 0.12);
  border-radius: var(--radius-md);
  border-left: 4px solid var(--red-team-primary);
  border: 0.5px solid rgba(255, 59, 48, 0.3);
}

.reject-reason h4 {
  color: var(--red-team-primary);
  margin: 0 0 var(--spacing-sm) 0;
  font-weight: 700;
  font-size: 15px;
  text-shadow: 0 0 15px rgba(255, 59, 48, 0.5);
}

.reject-reason p {
  margin: 0;
  color: rgba(255, 255, 255, 0.85);
  line-height: 1.6;
}

/* ============================================
   Score Display - 得分显示
   ============================================ */

/* 表格中的得分徽章 */
.score-badge {
  display: inline-block;
  padding: 4px 12px;
  background: linear-gradient(135deg, rgba(255, 59, 48, 0.2) 0%, rgba(255, 59, 48, 0.3) 100%);
  border: 1px solid rgba(255, 59, 48, 0.4);
  border-radius: var(--radius-sm);
  color: var(--red-team-secondary);
  font-weight: 700;
  font-size: 14px;
  text-shadow: 0 0 10px rgba(255, 59, 48, 0.3);
}

.no-score {
  color: rgba(255, 255, 255, 0.3);
  font-size: 14px;
}

/* 打分信息区域 (已通过的成果) */
.score-info {
  margin-top: var(--spacing-lg);
  padding: var(--spacing-md);
  background: rgba(255, 59, 48, 0.08);
  border-radius: var(--radius-md);
  border-left: 4px solid var(--red-team-primary);
  border: 0.5px solid rgba(255, 59, 48, 0.3);
}

.score-info h4 {
  color: var(--red-team-secondary);
  margin: 0 0 var(--spacing-md) 0;
  font-weight: 700;
  font-size: 15px;
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  text-shadow: 0 0 15px rgba(255, 59, 48, 0.5);
}

.final-score-display {
  font-size: 18px;
  font-weight: 700;
  color: var(--red-team-secondary);
  text-shadow: 0 0 10px rgba(255, 59, 48, 0.4);
}

.score-reason-text {
  white-space: pre-wrap;
  line-height: 1.6;
  color: rgba(255, 255, 255, 0.85);
  padding: var(--spacing-sm);
  background: rgba(30, 30, 30, 0.3);
  border-radius: var(--radius-sm);
}

/* ============================================
   Responsive Design
   ============================================ */
@media (max-width: 768px) {
  .red-team-achievement-list-page {
    padding: var(--spacing-md);
  }

  .card-header {
    flex-direction: column;
    gap: var(--spacing-sm);
    align-items: flex-start;
  }

  .header-filter {
    width: 100%;
    flex-direction: column;
  }

  .custom-select.status-filter-select {
    width: 100%;
  }

  .screenshot-item {
    width: 100px;
    height: 100px;
  }
}
</style>
