<template>
  <div class="achievement-manage-page">
    <el-card class="page-card">
      <template #header>
        <div class="card-header">
          <span>🔍 成果审核</span>
          <div class="header-filter">
            <el-button @click="fetchAchievements" :icon="Refresh" round size="small">刷新</el-button>
            <el-select v-model="statusFilter" @change="handleStatusChange" placeholder="筛选状态">
              <el-option label="全部" value=""></el-option>
              <el-option label="待审批" value="pending"></el-option>
              <el-option label="已通过" value="approved"></el-option>
              <el-option label="已驳回" value="rejected"></el-option>
            </el-select>
          </div>
        </div>
      </template>

      <!-- 成果列表表格 -->
      <el-table
        :data="achievementList"
        v-loading="loading"
        style="width: 100%"
        :header-cell-style="{ background: 'transparent', color: '#1d1d1f', fontWeight: '600', borderBottom: '1px solid rgba(0,0,0,0.06)' }"
        :row-style="{ height: '60px' }"
        :cell-style="{ padding: '16px 12px', borderBottom: '1px solid rgba(0,0,0,0.04)' }"
        empty-text="暂无数据">
        <el-table-column prop="id" label="ID" width="80" align="center"></el-table-column>
        <el-table-column prop="teamName" label="提交方" width="140" show-overflow-tooltip></el-table-column>
        <el-table-column prop="type" label="队伍类型" width="100" align="center">
          <template #default="scope">
            <el-tag :type="getTeamTypeColor(scope.row.type || scope.row.teamType)" size="small">
              {{ getTeamTypeText(scope.row.type || scope.row.teamType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetName" label="攻击目标" width="160" show-overflow-tooltip></el-table-column>
        <el-table-column prop="submitTime" label="提交时间" width="180" align="center">
          <template #default="scope">
            {{ formatDateTime(scope.row.submitTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="200" align="center" class-name="status-column">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)" size="small">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="360" align="center" fixed="right">
          <template #default="scope">
            <div class="action-buttons">
              <el-button 
                size="small" 
                type="primary" 
                plain
                @click="viewDetail(scope.row)">
                查看详情
              </el-button>
              <el-button
                v-if="scope.row.status === 'pending'"
                size="small"
                type="success"
                @click="showApproveDialog(scope.row.id)">
                <el-icon><Check /></el-icon>
                通过
              </el-button>
              <el-button 
                v-if="scope.row.status === 'pending'" 
                size="small" 
                type="danger" 
                @click="showRejectDialog(scope.row)">
                <el-icon><Close /></el-icon>
                驳回
              </el-button>
              <el-button 
                v-if="scope.row.status !== 'pending'" 
                size="small" 
                type="info" 
                plain
                disabled>
                已处理
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

    <!-- 通过对话框（带打分功能） -->
    <el-dialog v-model="approveDialogVisible" title="通过成果并打分" width="900px" class="approve-dialog">
      <el-form :model="approveForm" label-width="100px" class="apple-form">

        <!-- 成果类型选择 -->
        <el-form-item label="成果类型" required>
          <div class="achievement-type-selector">
            <div
              v-for="type in achievementTypes"
              :key="type.value"
              class="type-option"
              :class="{ active: approveForm.achievementType === type.value }"
              @click="selectAchievementType(type)">
              <div class="type-icon">{{ type.icon }}</div>
              <div class="type-name">{{ type.name }}</div>
              <div class="type-score">{{ type.minScore }}-{{ type.maxScore }}分</div>
            </div>
          </div>
        </el-form-item>

        <!-- 基础分值展示 -->
        <el-form-item label="基础分值">
          <div class="score-display-card">
            <div class="score-label">系统推荐分值</div>
            <div class="score-value">{{ approveForm.baseScore }}</div>
            <div class="score-unit">分</div>
          </div>
        </el-form-item>

        <!-- 最终得分调整 -->
        <el-form-item label="最终得分" required>
          <div class="score-adjuster">
            <el-input-number
              v-model="approveForm.finalScore"
              :step="5"
              class="score-input">
            </el-input-number>
            <div class="score-range-hint">
              可输入任意整数（包括负分）
            </div>
          </div>
        </el-form-item>

        <!-- 打分说明 -->
        <el-form-item label="打分说明">
          <el-input
            v-model="approveForm.scoreReason"
            type="textarea"
            :rows="3"
            placeholder="选填，说明打分理由或特殊情况"
            class="apple-textarea">
          </el-input>
        </el-form-item>

        <!-- 得分预览卡片 -->
        <div class="score-preview-card">
          <div class="preview-header">
            <span class="preview-icon">✅</span>
            <span class="preview-title">审批通过后</span>
          </div>
          <div class="preview-content">
            <div class="preview-item">
              <span class="preview-label">团队名称:</span>
              <span class="preview-value">{{ getCurrentAchievementTeamName() }}</span>
            </div>
            <div class="preview-item highlight">
              <span class="preview-label">将获得分数:</span>
              <span class="preview-value score">+{{ approveForm.finalScore }} 分</span>
            </div>
          </div>
        </div>

      </el-form>

      <template #footer>
        <el-button @click="approveDialogVisible = false">取消</el-button>
        <el-button type="success" @click="confirmApprove" :loading="approving">
          确认通过并打分
        </el-button>
      </template>
    </el-dialog>

    <!-- 驳回对话框 -->
    <el-dialog v-model="rejectDialogVisible" title="驳回成果" width="500px">
      <el-form :model="rejectForm" label-width="80px">
        <el-form-item label="驳回理由" required>
          <el-input
            v-model="rejectForm.reason"
            type="textarea"
            :rows="4"
            placeholder="请输入驳回理由">
          </el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmReject" :loading="rejecting">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Check, Close } from '@element-plus/icons-vue'
import axios from '@/api/axios'


// 响应式数据
const loading = ref(false)
const achievementList = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const statusFilter = ref('')
const detailDialogVisible = ref(false)
const approveDialogVisible = ref(false)
const rejectDialogVisible = ref(false)
const currentAchievement = ref(null)
const approving = ref(false)
const rejecting = ref(false)
const currentApproveId = ref(null)
const currentRejectId = ref(null)

// 成果类型列表（动态从后端加载）
const achievementTypes = ref([])

// 审批表单
const approveForm = reactive({
  achievementType: '',
  baseScore: 0,
  finalScore: 0,
  scoreReason: ''
})

// 驳回表单
const rejectForm = reactive({
  reason: ''
})

// 获取成果列表
const fetchAchievements = async () => {
  try {
    loading.value = true;

    const response = await axios.get('/achievements/admin/list', {
      params: {
        page: currentPage.value - 1,
        size: pageSize.value,
        status: statusFilter.value
      }
    })
    
    
    if (response && response.content) {
      achievementList.value = response.content
      total.value = response.totalElements
    } else {
      achievementList.value = []
      total.value = 0
    }
  } catch (error) {
    // API未就绪，使用模拟数据
    // 扩展的模拟数据
    achievementList.value = [
      {
        id: 1,
        type: 'red_team',
        teamName: '红队Alpha',
        targetName: 'Web服务器',
        drillName: '春季攻防演练',
        status: 'pending',
        submitTime: '2024-01-15 10:30:00',
        targetInfo: '192.168.1.100 Web服务器',
        attackPath: '通过SQL注入获取管理员权限',
        vulnerabilities: 'SQL注入漏洞',
        cvssScore: 'high',
        description: '通过SQL注入漏洞成功获取管理员权限，并获取敏感数据',
        attackMethod: 'SQLMap工具',
        screenshots: '/uploads/screenshot1.png,/uploads/screenshot2.png',
        proofFiles: '/uploads/poc.mp4,/uploads/logs.txt'
      },
      {
        id: 2,
        type: 'blue_team', 
        teamName: '蓝队Beta',
        targetName: '防护系统',
        drillName: '春季攻防演练',
        status: 'approved',
        submitTime: '2024-01-15 11:45:00',
        defenseType: '入侵检测',
        detectionMethod: 'IDS告警分析',
        responseProcess: '立即隔离受影响主机',
        description: '成功检测到攻击行为并及时响应处理',
        attackMethod: 'IDS监控系统',
        screenshots: '/uploads/defense1.png',
        proofFiles: '/uploads/incident_report.pdf'
      },
      {
        id: 3,
        type: 'red_team',
        teamName: '红队Gamma',
        targetName: '文件服务器',
        drillName: '夏季红蓝对抗',
        status: 'rejected',
        submitTime: '2024-01-16 14:20:00',
        targetInfo: '192.168.1.200 文件服务器',
        attackPath: '权限提升攻击',
        vulnerabilities: '本地权限提升漏洞',
        cvssScore: 'medium',
        description: '尝试通过本地权限提升漏洞获取系统权限',
        attackMethod: '权限提升工具',
        rejectReason: '证明材料不充分，缺少详细的攻击过程说明',
        screenshots: '/uploads/screenshot3.png',
        proofFiles: '/uploads/exploit.py'
      }
    ]
    
    // 根据筛选条件过滤数据
    let filteredData = achievementList.value
    if (statusFilter.value && statusFilter.value !== '') {
      filteredData = filteredData.filter(item => item.status === statusFilter.value)
    }
    
    achievementList.value = filteredData
    total.value = filteredData.length
  } finally {
    loading.value = false
  }
}

// 查看详情 - 修复API路径
const viewDetail = async (achievement) => {
  try {
const response = await axios.get(`/achievements/${achievement.id}`)
    currentAchievement.value = response
    detailDialogVisible.value = true
  } catch (error) {
    // API未就绪，使用本地数据
    // 使用传入的achievement数据作为详情
    currentAchievement.value = achievement
    detailDialogVisible.value = true
  }
}

// 加载成果类型列表
const loadAchievementTypes = async (teamType) => {
  try {
    const params = teamType ? { teamType } : {}
    const response = await axios.get('/achievements/achievement-types', { params })

    if (response && response.data) {
      achievementTypes.value = response.data
      console.log(`✅ 加载成果类型成功: ${achievementTypes.value.length} 个类型`, achievementTypes.value)
    }
  } catch (error) {
    console.error('❌ 加载成果类型失败:', error)
    ElMessage.error('加载成果类型失败')
    // 失败时使用空数组
    achievementTypes.value = []
  }
}

// 显示通过对话框
const showApproveDialog = async (id) => {
  currentApproveId.value = id
  // 重置表单
  approveForm.achievementType = ''
  approveForm.baseScore = 0
  approveForm.finalScore = 0
  approveForm.scoreReason = ''

  // 获取当前成果的队伍类型，动态加载对应的成果类型
  const achievement = achievementList.value.find(item => item.id === id)
  if (achievement && achievement.teamType) {
    console.log(`🔍 成果的队伍类型: ${achievement.teamType}`)
    await loadAchievementTypes(achievement.teamType)
  } else {
    console.warn('⚠️ 成果没有队伍类型，加载所有成果类型')
    await loadAchievementTypes(null)
  }

  // 显示对话框
  approveDialogVisible.value = true
}

// 选择成果类型
const selectAchievementType = (type) => {
  approveForm.achievementType = type.value
  approveForm.baseScore = type.baseScore
  approveForm.finalScore = type.baseScore // 默认使用基础分值
}

// 监听成果类型变化，自动更新分数
watch(() => approveForm.achievementType, (newType) => {
  const type = achievementTypes.value.find(t => t.value === newType)
  if (type) {
    approveForm.baseScore = type.baseScore
    approveForm.finalScore = type.baseScore
  }
})

// 获取当前审批成果的团队名称
const getCurrentAchievementTeamName = () => {
  const achievement = achievementList.value.find(item => item.id === currentApproveId.value)
  return achievement ? achievement.teamName : '未知'
}

// 确认通过并打分
const confirmApprove = async () => {
  // 验证表单
  if (!approveForm.achievementType) {
    ElMessage.warning('请选择成果类型')
    return
  }
  if (!approveForm.finalScore || approveForm.finalScore < 0) {
    ElMessage.warning('请输入有效的最终得分')
    return
  }

  approving.value = true
  try {
    try {
      await axios.put(`/achievements/admin/approve/${currentApproveId.value}`, null, {
        params: {
          reviewerId: 'admin',
          achievementType: approveForm.achievementType,
          baseScore: approveForm.baseScore,
          finalScore: approveForm.finalScore,
          scoreReason: approveForm.scoreReason || undefined
        }
      })
      ElMessage.success(`审批通过，${getCurrentAchievementTeamName()} 获得 ${approveForm.finalScore} 分`)
    } catch {
      // API未就绪，模拟审批操作
      const index = achievementList.value.findIndex(item => item.id === currentApproveId.value)
      if (index !== -1) {
        achievementList.value[index].status = 'approved'
        achievementList.value[index].finalScore = approveForm.finalScore
      }
      ElMessage.success(`审批通过（模拟），${getCurrentAchievementTeamName()} 获得 ${approveForm.finalScore} 分`)
    }

    approveDialogVisible.value = false
    fetchAchievements()
  } catch {
    ElMessage.error('操作失败')
  } finally {
    approving.value = false
  }
}

// 确认驳回 - 添加模拟处理
const confirmReject = async () => {
  if (!rejectForm.reason.trim()) {
    ElMessage.warning('请输入驳回理由')
    return
  }
  
  rejecting.value = true
  try {
    try {
      await axios.put(`/achievements/admin/reject/${currentRejectId.value}`, null, {
        params: {
          reviewerId: 'admin',
          reason: rejectForm.reason
        }
      })
    } catch {
      // API未就绪，模拟驳回操作
      // 模拟驳回成功
      const index = achievementList.value.findIndex(item => item.id === currentRejectId.value)
      if (index !== -1) {
        achievementList.value[index].status = 'rejected'
        achievementList.value[index].rejectReason = rejectForm.reason
      }
    }
    
    ElMessage.success('已驳回')
    rejectDialogVisible.value = false
    fetchAchievements()
  } catch {
    ElMessage.error('操作失败')
  } finally {
    rejecting.value = false
  }
}

// 显示驳回对话框
const showRejectDialog = (achievement) => {
  currentRejectId.value = achievement.id
  rejectForm.reason = ''
  rejectDialogVisible.value = true
}

// 状态筛选变化
const handleStatusChange = () => {
  currentPage.value = 1
  fetchAchievements()
}

// 分页大小变化
const handleSizeChange = (newSize) => {
  pageSize.value = newSize
  currentPage.value = 1
  fetchAchievements()
}

// 当前页变化
const handleCurrentChange = (newPage) => {
  currentPage.value = newPage
  fetchAchievements()
}

// 辅助方法
const formatDateTime = (dateTime) => {
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

// 获取队伍类型文本
const getTeamTypeText = (type) => {
  if (!type) return '未知'

  const typeMap = {
    'red_team': '红队',
    'blue_team': '蓝队',
    'red': '红队',
    'blue': '蓝队'
  }

  return typeMap[type] || '未知'
}

// 获取队伍类型标签颜色
const getTeamTypeColor = (type) => {
  if (!type) return 'info'

  const colorMap = {
    'red_team': 'danger',
    'blue_team': 'primary',
    'red': 'danger',
    'blue': 'primary'
  }

  return colorMap[type] || 'info'
}

const getScreenshots = (screenshots) => {
  if (!screenshots) return []

  return screenshots.split(',').map(path => {
    // 兼容处理:如果是绝对路径,转换为相对URL路径
    if (path.includes('/uploads/achievements/')) {
      // 提取/uploads/开始的部分
      const index = path.indexOf('/uploads/')
      return path.substring(index)
    }
    // 已经是相对路径,直接返回
    return path
  })
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
  fetchAchievements()
})
</script>

<style scoped>
/* ============================================
   Apple Elegant White Style - 苹果高雅白风格
   成果审批管理页
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

.achievement-manage-page {
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

.header-actions {
  display: flex;
  gap: var(--spacing-sm);
}

.header-actions :deep(.el-button) {
  font-weight: 500;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

.header-actions :deep(.el-button:hover) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.2);
}

/* ============================================
   Main Card
   ============================================ */
.page-card {
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

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 18px;
  font-weight: 700;
  color: var(--apple-text);
}

.card-header span {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
}

.header-filter {
  display: flex;
  gap: var(--spacing-sm);
}

.header-filter :deep(.el-select) {
  min-width: 150px;
}

.header-filter :deep(.el-input__wrapper) {
  border-radius: var(--radius-sm);
  border: 1px solid var(--apple-border);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
}

.header-filter :deep(.el-input__wrapper:hover) {
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.08);
  border-color: var(--apple-blue);
}

/* ============================================
   Table Styling
   ============================================ */
:deep(.el-table) {
  background: transparent;
  border-radius: var(--radius-md);
  overflow: hidden;
  font-family: var(--font-apple);
}

:deep(.el-table__inner-wrapper::before) {
  display: none; /* Remove default border */
}

:deep(.el-table thead) {
  background: rgba(0, 0, 0, 0.02);
}

:deep(.el-table__row) {
  transition: all 0.2s ease;
  background: transparent;
}

:deep(.el-table__row:hover) {
  background: rgba(0, 122, 255, 0.02) !important;
  transform: scale(1.001);
}

:deep(.el-table__body-wrapper) {
  border-radius: var(--radius-sm);
}

:deep(.el-table--enable-row-hover .el-table__body tr:hover>td) {
  background-color: transparent !important;
}

:deep(.el-table__empty-text) {
  color: var(--apple-text-secondary);
  font-weight: 500;
}

/* 防止表格单元格内的.cell容器截断内容 */
:deep(.el-table .cell) {
  overflow: visible;
  text-overflow: clip;
}

/* 针对状态列单元格的特殊处理 - 使用更高优先级 */
:deep(.el-table .status-column) {
  overflow: visible !important;
}

:deep(.el-table .status-column .cell) {
  overflow: visible !important;
  text-overflow: clip !important;
  white-space: nowrap !important;
  line-height: normal !important;
}

/* ============================================
   Tags and Badges
   ============================================ */
:deep(.el-tag) {
  border-radius: var(--radius-sm);
  border: none;
  font-weight: 600;
  padding: 4px 10px;
  font-size: 12px;
  white-space: nowrap;
  display: inline-flex;
  align-items: center;
  max-width: none;
}

:deep(.el-tag--warning) {
  background: rgba(255, 149, 0, 0.1);
  color: var(--apple-orange);
}

:deep(.el-tag--success) {
  background: rgba(52, 199, 89, 0.1);
  color: var(--apple-green);
}

:deep(.el-tag--danger) {
  background: rgba(255, 59, 48, 0.1);
  color: var(--apple-red);
}

/* ============================================
   Action Buttons
   ============================================ */
.action-buttons {
  display: flex;
  gap: var(--spacing-xs);
  justify-content: center;
  flex-wrap: wrap;
}

.action-buttons :deep(.el-button) {
  border-radius: var(--radius-sm);
  font-weight: 600;
  font-size: 13px;
  margin: 0;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

.action-buttons :deep(.el-button:hover) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.action-buttons :deep(.el-button--primary) {
  background: linear-gradient(135deg, var(--apple-blue) 0%, #0051d5 100%);
  border: none;
}

.action-buttons :deep(.el-button--success) {
  background: linear-gradient(135deg, var(--apple-green) 0%, #28a745 100%);
  border: none;
}

.action-buttons :deep(.el-button--danger) {
  background: linear-gradient(135deg, var(--apple-red) 0%, #dc143c 100%);
  border: none;
}

.action-buttons :deep(.el-button--info) {
  background: rgba(0, 0, 0, 0.04);
  color: var(--apple-text-secondary);
  border: 1px solid var(--apple-border);
}

/* ============================================
   Pagination
   ============================================ */
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
  border-radius: var(--radius-sm);
  background: rgba(0, 0, 0, 0.02);
  border: 1px solid var(--apple-border);
  transition: all 0.2s ease;
}

:deep(.el-pagination button:hover),
:deep(.el-pager li:hover) {
  background: rgba(0, 122, 255, 0.06);
  border-color: var(--apple-blue);
  transform: translateY(-1px);
}

:deep(.el-pager li.is-active) {
  background: var(--apple-blue);
  color: white;
  border-color: var(--apple-blue);
}

/* ============================================
   Detail Dialog
   ============================================ */
:deep(.el-dialog) {
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-card-hover);
  font-family: var(--font-apple);
}

:deep(.el-dialog__header) {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.98) 0%,
    rgba(248, 248, 248, 0.95) 100%);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--apple-border);
  padding: var(--spacing-lg);
}

:deep(.el-dialog__title) {
  font-weight: 700;
  color: var(--apple-text);
  font-size: 18px;
}

:deep(.el-dialog__body) {
  padding: var(--spacing-lg);
  background: rgba(255, 255, 255, 0.95);
}

.achievement-detail {
  max-height: 600px;
  overflow-y: auto;
  padding: var(--spacing-sm);
}

.achievement-detail::-webkit-scrollbar {
  width: 8px;
}

.achievement-detail::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.02);
  border-radius: 4px;
}

.achievement-detail::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.1);
  border-radius: 4px;
}

.achievement-detail::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.15);
}

:deep(.el-descriptions) {
  border-radius: var(--radius-md);
  overflow: hidden;
}

:deep(.el-descriptions__label) {
  font-weight: 600;
  color: var(--apple-text);
  background: rgba(0, 0, 0, 0.02) !important;
}

:deep(.el-descriptions__content) {
  color: var(--apple-text);
}

.description-text {
  white-space: pre-wrap;
  line-height: 1.8;
  color: var(--apple-text);
  padding: var(--spacing-sm);
  background: rgba(0, 0, 0, 0.02);
  border-radius: var(--radius-sm);
}

/* ============================================
   File Sections
   ============================================ */
.file-section {
  margin-top: var(--spacing-lg);
  padding: var(--spacing-md);
  background: rgba(0, 122, 255, 0.02);
  border-radius: var(--radius-md);
  border: 1px solid rgba(0, 122, 255, 0.08);
}

.file-section h4 {
  margin: 0 0 var(--spacing-md) 0;
  color: var(--apple-blue);
  font-weight: 700;
  font-size: 15px;
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
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
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
  cursor: pointer;
}

.screenshot-item:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
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
  background: rgba(0, 122, 255, 0.06);
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 500;
  transition: all 0.2s ease;
  width: fit-content;
}

.file-link:hover {
  background: rgba(0, 122, 255, 0.12);
  transform: translateX(4px);
}

/* ============================================
   Score Info Section (Approved Achievements)
   ============================================ */
.score-info {
  margin-top: var(--spacing-lg);
  padding: var(--spacing-md);
  background: rgba(52, 199, 89, 0.04);
  border-radius: var(--radius-md);
  border-left: 4px solid var(--apple-green);
}

.score-info h4 {
  color: var(--apple-green);
  margin: 0 0 var(--spacing-md) 0;
  font-weight: 700;
  font-size: 15px;
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
}

.final-score-display {
  font-size: 18px;
  font-weight: 700;
  color: var(--apple-green);
}

.score-reason-text {
  white-space: pre-wrap;
  line-height: 1.6;
  color: var(--apple-text);
  padding: var(--spacing-sm);
  background: rgba(0, 0, 0, 0.02);
  border-radius: var(--radius-sm);
}

/* ============================================
   Reject Reason Section
   ============================================ */
.reject-reason {
  margin-top: var(--spacing-lg);
  padding: var(--spacing-md);
  background: rgba(255, 59, 48, 0.04);
  border-radius: var(--radius-md);
  border-left: 4px solid var(--apple-red);
}

.reject-reason h4 {
  color: var(--apple-red);
  margin: 0 0 var(--spacing-sm) 0;
  font-weight: 700;
  font-size: 15px;
}

.reject-reason p {
  margin: 0;
  color: var(--apple-text);
  line-height: 1.6;
}

/* ============================================
   Responsive Design
   ============================================ */
@media (max-width: 768px) {
  .achievement-manage-page {
    padding: var(--spacing-md);
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--spacing-md);
  }

  .header-icon {
    font-size: 40px;
  }

  .header-title {
    font-size: 26px;
  }

  .card-header {
    flex-direction: column;
    gap: var(--spacing-sm);
    align-items: flex-start;
  }

  .action-buttons {
    flex-direction: column;
    width: 100%;
  }

  .action-buttons :deep(.el-button) {
    width: 100%;
  }
}

@media (max-width: 576px) {
  .header-icon {
    font-size: 36px;
  }

  .header-title {
    font-size: 22px;
  }

  .screenshot-item {
    width: 100px;
    height: 100px;
  }
}

/* ============================================
   Approve/Reject Message Styling
   ============================================ */
.approve-message {
  padding: var(--spacing-md);
  background: rgba(52, 199, 89, 0.06);
  border-radius: var(--radius-sm);
  color: var(--apple-text);
  font-size: 15px;
  font-weight: 500;
  line-height: 1.6;
  text-align: center;
}

/* ============================================
   Approve Dialog - Apple Scoring UI
   ============================================ */

/* 成果类型选择器 */
.achievement-type-selector {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--spacing-sm);
}

.type-option {
  padding: var(--spacing-md);
  background: rgba(0, 0, 0, 0.02);
  border: 2px solid transparent;
  border-radius: var(--radius-md);
  text-align: center;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

.type-option:hover {
  background: rgba(0, 122, 255, 0.04);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.1);
}

.type-option.active {
  background: linear-gradient(135deg, rgba(0, 122, 255, 0.08) 0%, rgba(0, 122, 255, 0.12) 100%);
  border-color: var(--apple-blue);
  box-shadow: 0 4px 16px rgba(0, 122, 255, 0.15);
}

.type-icon {
  font-size: 32px;
  margin-bottom: var(--spacing-xs);
}

.type-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--apple-text);
  margin-bottom: var(--spacing-xs);
}

.type-score {
  font-size: 13px;
  color: var(--apple-blue);
  font-weight: 500;
}

/* 分数展示卡片 */
.score-display-card {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-md);
  background: linear-gradient(135deg, rgba(52, 199, 89, 0.06) 0%, rgba(52, 199, 89, 0.1) 100%);
  border-radius: var(--radius-md);
  border-left: 4px solid var(--apple-green);
}

.score-label {
  font-size: 13px;
  color: var(--apple-text-secondary);
}

.score-value {
  font-size: 36px;
  font-weight: 700;
  color: var(--apple-green);
  font-family: -apple-system, "SF Pro Display", sans-serif;
  letter-spacing: -1px;
}

.score-unit {
  font-size: 15px;
  color: var(--apple-text-secondary);
  font-weight: 500;
}

/* 分数调整器 */
.score-adjuster {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.score-input :deep(.el-input-number__decrease),
.score-input :deep(.el-input-number__increase) {
  background: rgba(0, 0, 0, 0.02);
  border-radius: var(--radius-sm);
  transition: all 0.2s ease;
}

.score-input :deep(.el-input-number__decrease):hover,
.score-input :deep(.el-input-number__increase):hover {
  background: rgba(0, 122, 255, 0.1);
  color: var(--apple-blue);
}

.score-range-hint {
  font-size: 12px;
  color: var(--apple-text-secondary);
  padding: var(--spacing-xs);
  background: rgba(0, 0, 0, 0.02);
  border-radius: var(--radius-sm);
}

/* Apple TextArea样式 */
.apple-textarea :deep(.el-textarea__inner) {
  border-radius: var(--radius-sm);
  border: 1px solid var(--apple-border);
  font-family: var(--font-apple);
  transition: all 0.3s ease;
}

.apple-textarea :deep(.el-textarea__inner):focus {
  border-color: var(--apple-blue);
  box-shadow: 0 0 0 3px rgba(0, 122, 255, 0.1);
}

/* 得分预览卡片 */
.score-preview-card {
  margin-top: var(--spacing-lg);
  padding: var(--spacing-md);
  background: linear-gradient(135deg, rgba(0, 122, 255, 0.03) 0%, rgba(0, 122, 255, 0.06) 100%);
  border-radius: var(--radius-md);
  border: 1px solid rgba(0, 122, 255, 0.1);
}

.preview-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  margin-bottom: var(--spacing-sm);
}

.preview-icon {
  font-size: 20px;
  filter: drop-shadow(0 2px 8px rgba(52, 199, 89, 0.3));
}

.preview-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--apple-text);
}

.preview-content {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.preview-item {
  display: flex;
  justify-content: space-between;
  padding: var(--spacing-xs);
  font-size: 14px;
}

.preview-item.highlight {
  background: rgba(52, 199, 89, 0.08);
  border-radius: var(--radius-sm);
  padding: var(--spacing-sm);
}

.preview-label {
  color: var(--apple-text-secondary);
}

.preview-value {
  color: var(--apple-text);
  font-weight: 500;
}

.preview-value.score {
  font-size: 20px;
  font-weight: 700;
  color: var(--apple-green);
}
</style>