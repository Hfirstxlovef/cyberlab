<template>
  <div class="my-team-page" :class="themeClass">
    <!-- 红队动态光晕背景层 -->
    <div v-if="currentRole === 'red'" class="dynamic-glow-layer">
      <div class="glow-spot glow-1"></div>
      <div class="glow-spot glow-2"></div>
      <div class="glow-spot glow-3"></div>
    </div>

    <!-- 蓝队动态光晕背景层 -->
    <div v-else-if="currentRole === 'blue'" class="dynamic-glow-layer">
      <div class="glow-spot glow-blue-1"></div>
      <div class="glow-spot glow-blue-2"></div>
      <div class="glow-spot glow-blue-3"></div>
    </div>

    <el-card class="team-card">
      <template #header>
        <div class="card-header">
          <span>👥 我的战队</span>
        </div>
      </template>

      <!-- 已加入战队 -->
      <div v-if="myTeam && !loading" class="team-content">
        <el-descriptions :column="2" border class="team-info">
          <el-descriptions-item label="战队ID">
            <span class="team-id-value">{{ myTeam.id }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="战队名称">
            <span class="team-name-value">{{ myTeam.name }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="战队类型">
            <el-tag :type="getTeamTypeTagType(myTeam.teamType)">
              {{ getTeamTypeDisplayName(myTeam.teamType) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="myTeam.status === 'active' ? 'success' : 'danger'">
              {{ myTeam.status === 'active' ? '活跃' : '已解散' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="队长">
            <span class="leader-name-value">{{ leaderName || '未知' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="成员数量">
            <span class="member-count-value">{{ teamMembers.length || 0 }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间" :span="2">
            <span class="create-time-value">{{ formatDateTime(myTeam.createdAt) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="战队描述" :span="2">
            <span class="team-desc-value">{{ myTeam.description || '无描述' }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <!-- 成员列表 -->
        <div class="members-section">
          <div class="section-header">
            <h4>战队成员</h4>
          </div>
          <el-table :data="teamMembers" style="width: 100%">
            <el-table-column prop="userId" label="用户ID" width="100" align="center">
              <template #default="scope">
                <span class="user-id-cell">{{ scope.row.userId }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="username" label="用户名" min-width="150">
              <template #default="scope">
                <span class="username-cell">{{ scope.row.username }}</span>
              </template>
            </el-table-column>
            <el-table-column label="角色" width="120" align="center">
              <template #default="scope">
                <el-tag :type="scope.row.userId === myTeam.leaderId ? 'warning' : 'info'">
                  {{ scope.row.userId === myTeam.leaderId ? '队长' : '成员' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="加入时间" width="200" align="center">
              <template #default="scope">
                <span class="join-time-cell">{{ formatDateTime(scope.row.joinedAt) }}</span>
              </template>
            </el-table-column>
            <el-table-column v-if="isLeader" label="操作" width="100" align="center">
              <template #default="scope">
                <el-button
                  v-if="scope.row.userId !== myTeam.leaderId"
                  type="danger"
                  size="small"
                  :icon="Delete"
                  @click="handleRemoveMember(scope.row)"
                >
                  移除
                </el-button>
                <span v-else style="color: rgba(255, 255, 255, 0.3)">-</span>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 队长视图：申请管理 -->
        <div v-if="isLeader" class="applications-section">
          <div class="section-header">
            <h4>申请管理</h4>
            <el-badge :value="pendingApplications.length" :hidden="pendingApplications.length === 0">
              <el-button
                size="small"
                @click="fetchPendingApplications"
                :loading="refreshingApplications"
                :icon="Refresh"
              >
                刷新申请
              </el-button>
            </el-badge>
          </div>

          <div v-if="pendingApplications.length > 0">
            <el-table :data="pendingApplications" style="width: 100%">
              <el-table-column prop="userId" label="申请人ID" width="120" align="center" />
              <el-table-column label="申请人" min-width="150">
                <template #default="scope">
                  {{ getUsernameById(scope.row.userId) }}
                </template>
              </el-table-column>
              <el-table-column label="申请留言" min-width="200">
                <template #default="scope">
                  {{ scope.row.message || '无留言' }}
                </template>
              </el-table-column>
              <el-table-column label="申请时间" width="200" align="center">
                <template #default="scope">
                  {{ formatDateTime(scope.row.createdAt) }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="200" align="center">
                <template #default="scope">
                  <el-button size="small" type="success" @click="handleApprove(scope.row)">
                    批准
                  </el-button>
                  <el-button size="small" type="danger" @click="openRejectDialog(scope.row)">
                    拒绝
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
          <el-empty v-else description="暂无待处理的申请" :image-size="80" />
        </div>
      </div>

      <!-- 未加入战队 -->
      <div v-else-if="!myTeam && !loading" class="no-team-content">
        <!-- 我的申请状态 -->
        <div v-if="myApplications.length > 0" class="my-applications">
          <h3>我的申请</h3>
          <el-table :data="myApplications" style="width: 100%; margin-bottom: 24px">
            <el-table-column label="战队名称" min-width="150">
              <template #default="scope">
                {{ getTeamNameById(scope.row.teamId) }}
              </template>
            </el-table-column>
            <el-table-column label="申请状态" width="120" align="center">
              <template #default="scope">
                <el-tag :type="getApplicationStatusTagType(scope.row.status)">
                  {{ getApplicationStatusText(scope.row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="申请时间" width="200" align="center">
              <template #default="scope">
                {{ formatDateTime(scope.row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column label="拒绝理由" min-width="150">
              <template #default="scope">
                {{ scope.row.rejectReason || '-' }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" align="center">
              <template #default="scope">
                <el-button
                  v-if="scope.row.status === 'pending'"
                  size="small"
                  type="warning"
                  @click="handleWithdraw(scope.row)"
                >
                  撤回
                </el-button>
                <span v-else>-</span>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <el-empty v-else description="您还未加入任何战队">
          <template #image>
            <div class="empty-icon">👥</div>
          </template>
        </el-empty>

        <!-- 可用战队列表 -->
        <div v-if="availableTeams.length > 0" class="available-teams">
          <h3>可加入的战队</h3>
          <el-table :data="availableTeams" style="width: 100%">
            <el-table-column prop="id" label="战队ID" width="100" align="center" />
            <el-table-column prop="name" label="战队名称" min-width="180" />
            <el-table-column label="类型" width="120" align="center">
              <template #default="scope">
                <el-tag :type="getTeamTypeTagType(scope.row.teamType)">
                  {{ getTeamTypeDisplayName(scope.row.teamType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="memberCount" label="成员数" width="100" align="center" />
            <el-table-column label="状态" width="100" align="center">
              <template #default="scope">
                <el-tag :type="scope.row.status === 'active' ? 'success' : 'info'">
                  {{ scope.row.status === 'active' ? '活跃' : '已解散' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" align="center">
              <template #default="scope">
                <el-button
                  type="primary"
                  size="small"
                  @click="openApplicationDialog(scope.row)"
                  :disabled="hasAppliedToTeam(scope.row.id)"
                >
                  {{ hasAppliedToTeam(scope.row.id) ? '已申请' : '申请加入' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <div v-else class="no-available-teams">
          <el-alert
            title="暂无可加入的战队"
            type="info"
            :closable="false"
          >
            请联系管理员创建战队或将您加入现有战队。
          </el-alert>
        </div>
      </div>

      <!-- 加载中 -->
      <div v-else class="loading-content">
        <el-skeleton :rows="5" animated />
      </div>
    </el-card>

    <!-- 申请加入对话框 -->
    <el-dialog v-model="applicationDialogVisible" title="申请加入战队" width="500px">
      <el-form :model="applicationForm" ref="applicationFormRef" label-width="80px">
        <el-form-item label="战队名称">
          <el-input v-model="selectedTeam.name" disabled />
        </el-form-item>
        <el-form-item label="申请留言">
          <el-input
            v-model="applicationForm.message"
            type="textarea"
            :rows="4"
            placeholder="请输入申请留言（选填）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applicationDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitApplication" :loading="submitting">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 拒绝申请对话框 -->
    <el-dialog v-model="rejectDialogVisible" title="拒绝申请" width="500px">
      <el-form :model="rejectForm" ref="rejectFormRef" label-width="80px">
        <el-form-item label="申请人">
          <el-input :value="getUsernameById(selectedApplication?.userId)" disabled />
        </el-form-item>
        <el-form-item label="拒绝理由" prop="reason">
          <el-input
            v-model="rejectForm.reason"
            type="textarea"
            :rows="4"
            placeholder="请输入拒绝理由"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="handleReject" :loading="submitting">确认拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Delete } from '@element-plus/icons-vue'
import {
  getTeamList,
  getTeamMembers,
  submitApplication,
  getMyApplications,
  getPendingApplicationsByTeam,
  approveApplication,
  rejectApplication,
  withdrawApplication,
  removeTeamMember
} from '@/api/team'
import { getUsers } from '@/api/user'
import { getUserRole, getUserId } from '@/utils/auth'

// 角色判断和主题切换
const currentRole = computed(() => getUserRole())
const themeClass = computed(() => currentRole.value === 'blue' ? 'theme-blue' : 'theme-red')

const loading = ref(false)
const myTeam = ref(null)
const teamMembers = ref([])
const availableTeams = ref([])
const myApplications = ref([])
const pendingApplications = ref([])
const allUsers = ref([])
const currentUserId = ref(null)
const currentUserRole = ref(null)
const submitting = ref(false)
const refreshingApplications = ref(false)

// 对话框状态
const applicationDialogVisible = ref(false)
const rejectDialogVisible = ref(false)

// 表单数据
const selectedTeam = reactive({ id: null, name: '' })
const selectedApplication = ref(null)
const applicationForm = reactive({
  message: ''
})
const rejectForm = reactive({
  reason: ''
})

// 队长名称
const leaderName = computed(() => {
  if (!myTeam.value || !myTeam.value.leaderId) return '未知'
  const leader = allUsers.value.find(u => u.id === myTeam.value.leaderId)
  return leader?.username || '未知'
})

// 判断当前用户是否为队长
const isLeader = computed(() => {
  return myTeam.value && currentUserId.value === myTeam.value.leaderId
})

// 获取我的战队信息
const fetchMyTeam = async () => {
  loading.value = true
  try {
    currentUserId.value = getUserId()
    currentUserRole.value = getUserRole()

    if (!currentUserId.value) {
      ElMessage.error('无法获取用户信息')
      return
    }

    // 获取所有用户（基本信息）
    const users = await getUsers()
    allUsers.value = users
    const currentUser = users.find(u => u.id === currentUserId.value)

    if (!currentUser) {
      ElMessage.error('无法找到当前用户信息')
      return
    }

    // 如果用户有战队ID，获取战队信息
    if (currentUser.teamId) {
      const teams = await getTeamList({ teamType: currentUserRole.value })
      myTeam.value = teams.find(t => t.id === currentUser.teamId)

      if (myTeam.value) {
        // 获取战队成员
        await fetchTeamMembers(myTeam.value.id)

        // 如果是队长，获取待处理的申请
        if (isLeader.value) {
          await fetchPendingApplications()
        }
      }
    } else {
      // 没有战队，获取可加入的战队列表和我的申请
      const teams = await getTeamList({ teamType: currentUserRole.value })
      availableTeams.value = await Promise.all(
        teams.map(async (team) => {
          try {
            const members = await getTeamMembers(team.id)
            return {
              ...team,
              memberCount: members.length
            }
          } catch {
            return {
              ...team,
              memberCount: 0
            }
          }
        })
      )

      // 获取我的申请
      await fetchMyApplications()
    }
  } catch (error) {
    console.error('获取战队信息失败:', error)
    ElMessage.error('获取战队信息失败')
  } finally {
    loading.value = false
  }
}

// 获取战队成员列表
const fetchTeamMembers = async (teamId) => {
  try {
    const members = await getTeamMembers(teamId)
    teamMembers.value = await Promise.all(
      members.map(async (member) => {
        const user = allUsers.value.find(u => Number(u.id) === Number(member.userId))
        return {
          ...member,
          username: user?.username || '未知用户'
        }
      })
    )
  } catch (error) {
    console.error('获取战队成员失败:', error)
    teamMembers.value = []
  }
}

// 获取我的申请
const fetchMyApplications = async () => {
  try {
    myApplications.value = await getMyApplications()
  } catch (error) {
    console.error('获取申请列表失败:', error)
    myApplications.value = []
  }
}

// 获取待处理的申请
const fetchPendingApplications = async () => {
  if (!myTeam.value) return

  refreshingApplications.value = true
  try {
    pendingApplications.value = await getPendingApplicationsByTeam(myTeam.value.id)
    const count = pendingApplications.value.length
    if (count > 0) {
      ElMessage.success(`已刷新，当前有 ${count} 条待处理申请`)
    } else {
      ElMessage.success('已刷新，暂无待处理的申请')
    }
  } catch (error) {
    console.error('获取待处理申请失败:', error)
    ElMessage.error('刷新失败，请稍后重试')
    pendingApplications.value = []
  } finally {
    refreshingApplications.value = false
  }
}

// 打开申请对话框
const openApplicationDialog = (team) => {
  selectedTeam.id = team.id
  selectedTeam.name = team.name
  applicationForm.message = ''
  applicationDialogVisible.value = true
}

// 提交申请
const handleSubmitApplication = async () => {
  try {
    submitting.value = true
    await submitApplication({
      teamId: selectedTeam.id,
      message: applicationForm.message
    })
    ElMessage.success('申请已提交，请等待队长审批')
    applicationDialogVisible.value = false
    await fetchMyApplications()
  } catch (error) {
    ElMessage.error(error.response?.data || '提交申请失败')
  } finally {
    submitting.value = false
  }
}

// 撤回申请
const handleWithdraw = async (application) => {
  try {
    await ElMessageBox.confirm(
      '确认撤回该申请吗？',
      '提示',
      { type: 'warning' }
    )
    await withdrawApplication(application.id)
    ElMessage.success('申请已撤回')
    await fetchMyApplications()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('撤回申请失败')
    }
  }
}

// 打开拒绝对话框
const openRejectDialog = (application) => {
  selectedApplication.value = application
  rejectForm.reason = ''
  rejectDialogVisible.value = true
}

// 批准申请
const handleApprove = async (application) => {
  try {
    const isBlue = currentRole.value === 'blue'
    await ElMessageBox.confirm(
      `确认批准 "${getUsernameById(application.userId)}" 加入战队吗？`,
      '✅ 批准申请',
      {
        type: 'success',
        confirmButtonText: '确认批准',
        cancelButtonText: '取消',
        customClass: isBlue ? 'blue-team-confirm-box' : 'red-team-confirm-box',
        confirmButtonClass: isBlue ? 'blue-team-success-btn' : 'red-team-success-btn',
        cancelButtonClass: isBlue ? 'blue-team-cancel-btn' : 'red-team-cancel-btn'
      }
    )
    await approveApplication(application.id)
    ElMessage.success('已批准申请')
    await fetchPendingApplications()
    await fetchTeamMembers(myTeam.value.id)
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data || '批准申请失败')
    }
  }
}

// 拒绝申请
const handleReject = async () => {
  if (!rejectForm.reason.trim()) {
    ElMessage.warning('请输入拒绝理由')
    return
  }
  try {
    submitting.value = true
    await rejectApplication(selectedApplication.value.id, {
      reason: rejectForm.reason
    })
    ElMessage.success('已拒绝申请')
    rejectDialogVisible.value = false
    await fetchPendingApplications()
  } catch (error) {
    ElMessage.error(error.response?.data || '拒绝申请失败')
  } finally {
    submitting.value = false
  }
}

// 移除队员
const handleRemoveMember = async (member) => {
  try {
    const isBlue = currentRole.value === 'blue'
    await ElMessageBox.confirm(
      `确认将 "${member.username}" 移出战队吗？此操作不可撤销。`,
      '⚠️ 移除队员',
      {
        type: 'warning',
        confirmButtonText: '确认移除',
        cancelButtonText: '取消',
        customClass: isBlue ? 'blue-team-confirm-box' : 'red-team-confirm-box',
        confirmButtonClass: isBlue ? 'blue-team-danger-btn' : 'red-team-danger-btn',
        cancelButtonClass: isBlue ? 'blue-team-cancel-btn' : 'red-team-cancel-btn'
      }
    )

    await removeTeamMember(myTeam.value.id, member.userId)
    ElMessage.success(`已将 ${member.username} 移出战队`)

    // 刷新成员列表
    await fetchTeamMembers(myTeam.value.id)
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data || '移除队员失败')
    }
  }
}

// 工具方法
const formatDateTime = (dateTime) => {
  if (!dateTime) return '-'
  return new Date(dateTime).toLocaleString('zh-CN')
}

const getTeamTypeDisplayName = (teamType) => {
  return teamType === 'red' ? '红队' : '蓝队'
}

const getTeamTypeTagType = (teamType) => {
  return teamType === 'red' ? 'danger' : 'primary'
}

const getUsernameById = (userId) => {
  const user = allUsers.value.find(u => u.id === userId)
  return user?.username || '未知用户'
}

const getTeamNameById = (teamId) => {
  const team = availableTeams.value.find(t => t.id === teamId)
  return team?.name || '未知战队'
}

const hasAppliedToTeam = (teamId) => {
  return myApplications.value.some(
    app => app.teamId === teamId && app.status === 'pending'
  )
}

const getApplicationStatusText = (status) => {
  const map = {
    pending: '待审批',
    approved: '已批准',
    rejected: '已拒绝'
  }
  return map[status] || '未知'
}

const getApplicationStatusTagType = (status) => {
  const map = {
    pending: 'warning',
    approved: 'success',
    rejected: 'danger'
  }
  return map[status] || 'info'
}

onMounted(() => {
  fetchMyTeam()
})
</script>

<style scoped>
/* ============================================
   红队"我的战队"页面 - Apple优雅 × 深色科技风
   Red Team - My Team Page - Dark Hacker Style
   ============================================ */

/* CSS Variables - 红队深色主题 */
:root {
  --hacker-bg: #0a0a0a;
  --hacker-bg-secondary: #1a0d0d;
  --hacker-red: #ff3b30;
  --hacker-red-glow: rgba(255, 59, 48, 0.3);
  --hacker-text: #ffffff;
  --hacker-text-secondary: rgba(255, 255, 255, 0.7);
  --hacker-glass: rgba(20, 20, 20, 0.6);
  --apple-green: #34c759;
  --apple-orange: #ff9500;
  --apple-blue: #007aff;
  --radius-sm: 12px;
  --radius-md: 16px;
  --radius-lg: 20px;
  --spacing-xs: 8px;
  --spacing-sm: 12px;
  --spacing-md: 16px;
  --spacing-lg: 24px;
  --spacing-xl: 32px;
  --font-apple: -apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", "Helvetica Neue", sans-serif;
  --font-mono: "SF Mono", Consolas, Monaco, monospace;
}

.my-team-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #0a0a0a 0%, #1a0d0d 50%, #0f0f0f 100%);
  padding: var(--spacing-lg);
  font-family: var(--font-apple);
  color: var(--hacker-text);
  position: relative;
  overflow-x: hidden;
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

/* 蓝队动态光晕动画 - Blue Team Glow Animations */
.glow-blue-1 {
  top: 15%;
  left: 25%;
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(0, 212, 255, 0.2) 0%, transparent 70%);
  animation: glow-blue-breath-1 8s ease-in-out infinite;
}

.glow-blue-2 {
  bottom: 20%;
  right: 15%;
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, rgba(0, 150, 255, 0.15) 0%, transparent 70%);
  animation: glow-blue-breath-2 10s ease-in-out infinite;
  animation-delay: 2s;
}

.glow-blue-3 {
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 350px;
  height: 350px;
  background: radial-gradient(circle, rgba(0, 212, 255, 0.12) 0%, transparent 70%);
  animation: glow-blue-breath-3 6s ease-in-out infinite;
  animation-delay: 1s;
}

@keyframes glow-blue-breath-1 {
  0%, 100% {
    opacity: 0.25;
    transform: scale(1);
  }
  50% {
    opacity: 0.4;
    transform: scale(1.15);
  }
}

@keyframes glow-blue-breath-2 {
  0%, 100% {
    opacity: 0.2;
    transform: scale(1);
  }
  50% {
    opacity: 0.35;
    transform: scale(1.2);
  }
}

@keyframes glow-blue-breath-3 {
  0%, 100% {
    opacity: 0.18;
    transform: translate(-50%, -50%) scale(1);
  }
  50% {
    opacity: 0.32;
    transform: translate(-50%, -50%) scale(1.25);
  }
}

/* ============================================
   Team Card - 深色玻璃态卡片
   ============================================ */
.team-card {
  background: var(--hacker-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: var(--radius-lg);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4), inset 0 0 0 0.5px rgba(255, 255, 255, 0.05);
  border: 0.5px solid rgba(255, 59, 48, 0.2);
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
  position: relative;
  z-index: 1;
  overflow: hidden;
}

.team-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  width: 3px;
  height: 100%;
  background: linear-gradient(180deg, rgba(255, 59, 48, 0.8) 0%, rgba(255, 59, 48, 0.3) 100%);
  border-radius: 16px 0 0 16px;
}

.team-card:hover {
  box-shadow: 0 12px 48px rgba(255, 59, 48, 0.2), 0 0 30px rgba(255, 59, 48, 0.1);
  border-color: rgba(255, 59, 48, 0.3);
}

/* Card Header - 红色渐变标题 */
.card-header {
  font-size: 20px;
  font-weight: 700;
  background: linear-gradient(135deg, #ff3b30 0%, #ff6b59 100%);
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  text-shadow: 0 0 30px var(--hacker-red-glow);
  filter: drop-shadow(0 0 20px var(--hacker-red-glow));
}

/* Team Content */
.team-content {
  padding: var(--spacing-md);
}

.team-info {
  margin-bottom: var(--spacing-xl);
}

/* ============================================
   Members Section - 深色成员区域
   ============================================ */
.members-section,
.applications-section {
  margin-top: var(--spacing-xl);
  padding: var(--spacing-lg);
  background: rgba(30, 30, 30, 0.5);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-radius: var(--radius-md);
  border: 0.5px solid rgba(255, 59, 48, 0.2);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-lg);
}

.section-header h4 {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.9);
}

/* ============================================
   No Team Content - 未加入战队区域
   ============================================ */
.no-team-content {
  padding: var(--spacing-xl);
  text-align: center;
}

.empty-icon {
  font-size: 72px;
  margin-bottom: var(--spacing-md);
  filter: drop-shadow(0 0 15px rgba(255, 59, 48, 0.3));
}

.my-applications {
  margin-bottom: var(--spacing-xl);
  text-align: left;
}

.my-applications h3 {
  font-size: 18px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: var(--spacing-md);
}

.available-teams {
  margin-top: var(--spacing-xl);
  text-align: left;
}

.available-teams h3 {
  font-size: 18px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: var(--spacing-md);
}

.no-available-teams {
  margin-top: var(--spacing-xl);
}

/* ============================================
   Loading Content
   ============================================ */
.loading-content {
  padding: var(--spacing-xl);
}

/* ============================================
   Descriptions Styling - 深色描述列表 + 赛博朋克配色
   ============================================ */
:deep(.el-descriptions) {
  border-radius: var(--radius-md);
  overflow: hidden;
  background: rgba(30, 30, 30, 0.5);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 2px solid rgba(255, 59, 48, 0.5);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4),
              0 0 30px rgba(255, 59, 48, 0.3),
              inset 0 1px 0 rgba(255, 255, 255, 0.05),
              inset 0 0 1px rgba(255, 59, 48, 0.3);
}

:deep(.el-descriptions__label) {
  font-weight: 700;
  color: rgba(255, 255, 255, 0.95);
  background: rgba(30, 30, 30, 0.9) !important;
  text-shadow: 0 0 3px rgba(0, 0, 0, 0.9);
  letter-spacing: 0.3px;
}

:deep(.el-descriptions__content) {
  color: rgba(255, 255, 255, 0.85);
  background: rgba(20, 20, 20, 0.7) !important;
}

:deep(.el-descriptions__table) {
  border-color: rgba(255, 59, 48, 0.35) !important;
}

:deep(.el-descriptions__cell) {
  border-color: rgba(255, 59, 48, 0.35) !important;
  border-width: 1px !important;
}

/* 赛博朋克描述列表配色 - Cyberpunk Descriptions Colors */
/* 简化版：纯色 + 单层衬底 + 单层柔和辉光 */
:deep(.team-id-value) {
  color: #00ffff !important;
  font-weight: 800;
  text-shadow:
    0 0 3px rgba(0, 0, 0, 0.9),
    0 0 8px rgba(0, 255, 255, 0.4);
  font-family: var(--font-mono);
  letter-spacing: 0.8px;
  font-size: 16px;
}

:deep(.team-name-value) {
  color: #ffffff !important;
  font-weight: 700;
  text-shadow:
    0 0 3px rgba(0, 0, 0, 0.9),
    0 0 8px rgba(255, 255, 255, 0.3);
  font-size: 16px;
}

:deep(.leader-name-value) {
  color: #ffffff !important;
  font-weight: 700;
  text-shadow:
    0 0 3px rgba(0, 0, 0, 0.9),
    0 0 8px rgba(255, 255, 255, 0.3);
}

:deep(.member-count-value) {
  color: #ff9500 !important;
  font-weight: 800;
  text-shadow:
    0 0 3px rgba(0, 0, 0, 0.9),
    0 0 8px rgba(255, 149, 0, 0.4);
  font-family: var(--font-mono);
  font-size: 18px;
}

:deep(.create-time-value) {
  color: #c5a8e8 !important;
  font-weight: 600;
  text-shadow:
    0 0 3px rgba(0, 0, 0, 0.9),
    0 0 8px rgba(197, 168, 232, 0.4);
}

:deep(.team-desc-value) {
  color: rgba(255, 255, 255, 0.95) !important;
  font-weight: 500;
  line-height: 1.6;
  text-shadow: 0 0 3px rgba(0, 0, 0, 0.9);
}

/* ============================================
   Table Styling - 深色表格 + 赛博朋克配色
   ============================================ */
:deep(.el-table) {
  background: transparent;
  border-radius: var(--radius-md);
  overflow: hidden;
  font-family: var(--font-apple);
  color: rgba(255, 255, 255, 0.9);
}

:deep(.el-table__inner-wrapper::before) {
  display: none;
}

:deep(.el-table thead) {
  background: rgba(30, 30, 30, 0.8);
}

:deep(.el-table th.el-table__cell) {
  background: rgba(30, 30, 30, 0.8) !important;
  color: #ff3b30 !important;
  font-weight: 700;
  text-shadow: 0 0 15px rgba(255, 59, 48, 0.6);
  border-bottom: 2px solid rgba(255, 59, 48, 0.4);
}

:deep(.el-table__row) {
  transition: all 0.2s ease;
  background: transparent;
}

:deep(.el-table td.el-table__cell) {
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.8);
}

:deep(.el-table__row:hover) {
  background: rgba(255, 59, 48, 0.08) !important;
}

/* 赛博朋克表格配色 - Cyberpunk Table Colors */
:deep(.user-id-cell) {
  color: #00ffff !important;
  font-weight: 700;
  text-shadow: 0 0 10px rgba(0, 255, 255, 0.5);
  font-family: var(--font-mono);
  letter-spacing: 0.5px;
}

:deep(.username-cell) {
  color: #ffffff !important;
  font-weight: 600;
  text-shadow: 0 0 8px rgba(255, 255, 255, 0.3);
}

:deep(.join-time-cell) {
  color: #b19cd9 !important;
  font-weight: 500;
  text-shadow: 0 0 8px rgba(177, 156, 217, 0.4);
}

/* ============================================
   Tags Styling - 深色标签
   ============================================ */
:deep(.el-tag) {
  border-radius: var(--radius-sm);
  border: none;
  font-weight: 600;
  padding: 4px 12px;
  font-size: 12px;
}

:deep(.el-tag--warning) {
  background: rgba(255, 149, 0, 0.2);
  border-color: rgba(255, 149, 0, 0.4);
  color: var(--apple-orange);
}

:deep(.el-tag--success) {
  background: rgba(52, 199, 89, 0.2);
  border-color: rgba(52, 199, 89, 0.4);
  color: var(--apple-green);
}

:deep(.el-tag--danger) {
  background: rgba(255, 59, 48, 0.2);
  border-color: rgba(255, 59, 48, 0.4);
  color: var(--hacker-red);
}

:deep(.el-tag--primary) {
  background: rgba(0, 122, 255, 0.2);
  border-color: rgba(0, 122, 255, 0.4);
  color: var(--apple-blue);
}

:deep(.el-tag--info) {
  background: rgba(142, 142, 147, 0.2);
  border-color: rgba(142, 142, 147, 0.4);
  color: rgba(255, 255, 255, 0.6);
}

/* ============================================
   Button Styling - 红队风格按钮
   ============================================ */
:deep(.el-button) {
  border-radius: var(--radius-sm);
  font-weight: 600;
  font-family: var(--font-apple);
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

:deep(.el-button--primary) {
  background: linear-gradient(135deg, rgba(255, 59, 48, 0.2) 0%, rgba(255, 59, 48, 0.3) 100%);
  border: 0.5px solid rgba(255, 59, 48, 0.3);
  color: #ffffff;
  box-shadow: 0 4px 12px rgba(255, 59, 48, 0.15);
}

:deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, rgba(255, 59, 48, 0.3) 0%, rgba(255, 59, 48, 0.4) 100%);
  border-color: rgba(255, 59, 48, 0.5);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(255, 59, 48, 0.35);
}

:deep(.el-button--primary:disabled) {
  background: rgba(255, 255, 255, 0.05);
  border-color: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.3);
  box-shadow: none;
  transform: none;
}

:deep(.el-button--success) {
  background: linear-gradient(135deg, rgba(52, 199, 89, 0.15) 0%, rgba(52, 199, 89, 0.25) 100%);
  border: 0.5px solid rgba(52, 199, 89, 0.3);
  color: #ffffff;
  box-shadow: 0 4px 12px rgba(52, 199, 89, 0.15);
}

:deep(.el-button--success:hover) {
  background: linear-gradient(135deg, rgba(52, 199, 89, 0.25) 0%, rgba(52, 199, 89, 0.35) 100%);
  border-color: rgba(52, 199, 89, 0.5);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(52, 199, 89, 0.35);
}

:deep(.el-button--danger) {
  background: linear-gradient(135deg, rgba(255, 59, 48, 0.2) 0%, rgba(255, 59, 48, 0.3) 100%);
  border: 0.5px solid rgba(255, 59, 48, 0.3);
  color: #ffffff;
  box-shadow: 0 4px 12px rgba(255, 59, 48, 0.15);
}

:deep(.el-button--danger:hover) {
  background: linear-gradient(135deg, rgba(255, 59, 48, 0.3) 0%, rgba(255, 59, 48, 0.4) 100%);
  border-color: rgba(255, 59, 48, 0.5);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(255, 59, 48, 0.35);
}

:deep(.el-button--warning) {
  background: linear-gradient(135deg, rgba(255, 149, 0, 0.15) 0%, rgba(255, 149, 0, 0.25) 100%);
  border: 0.5px solid rgba(255, 149, 0, 0.3);
  color: #ffffff;
  box-shadow: 0 4px 12px rgba(255, 149, 0, 0.15);
}

:deep(.el-button--warning:hover) {
  background: linear-gradient(135deg, rgba(255, 149, 0, 0.25) 0%, rgba(255, 149, 0, 0.35) 100%);
  border-color: rgba(255, 149, 0, 0.5);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(255, 149, 0, 0.35);
}

/* ============================================
   Empty Styling - 深色空状态
   ============================================ */
:deep(.el-empty) {
  padding: var(--spacing-xl) 0;
}

:deep(.el-empty__description) {
  font-size: 16px;
  color: var(--hacker-text-secondary);
  font-family: var(--font-apple);
}

/* ============================================
   Alert Styling - 深色警告框
   ============================================ */
:deep(.el-alert) {
  border-radius: var(--radius-md);
  border: 0.5px solid rgba(255, 59, 48, 0.2);
  font-family: var(--font-apple);
  background: rgba(30, 30, 30, 0.5);
  backdrop-filter: blur(10px);
}

:deep(.el-alert--info) {
  background: rgba(0, 122, 255, 0.1);
  border-color: rgba(0, 122, 255, 0.3);
}

:deep(.el-alert__title) {
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
}

:deep(.el-alert__description) {
  color: rgba(255, 255, 255, 0.7);
}

/* ============================================
   Dialog Styling - 深色对话框
   ============================================ */
:deep(.el-dialog) {
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.6);
  font-family: var(--font-apple);
  background: var(--hacker-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 0.5px solid rgba(255, 59, 48, 0.2);
}

:deep(.el-dialog__header) {
  background: rgba(30, 30, 30, 0.8);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(255, 59, 48, 0.2);
  padding: var(--spacing-lg);
}

:deep(.el-dialog__title) {
  font-weight: 700;
  color: rgba(255, 255, 255, 0.95);
  font-size: 18px;
}

:deep(.el-dialog__body) {
  padding: var(--spacing-lg);
  background: transparent;
}

:deep(.el-dialog__footer) {
  padding: var(--spacing-md) var(--spacing-lg);
  border-top: 1px solid rgba(255, 59, 48, 0.15);
  background: rgba(20, 20, 20, 0.5);
}

/* ============================================
   Form Styling - 深色表单
   ============================================ */
:deep(.el-form-item__label) {
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  font-family: var(--font-apple);
}

:deep(.el-input__wrapper) {
  border-radius: var(--radius-sm);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  transition: all 0.3s ease;
  border: 1px solid rgba(255, 59, 48, 0.2);
  background: rgba(30, 30, 30, 0.5);
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 4px 12px rgba(255, 59, 48, 0.15);
  border-color: rgba(255, 59, 48, 0.4);
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 4px 16px rgba(255, 59, 48, 0.25);
  border-color: var(--hacker-red);
}

:deep(.el-input__inner) {
  color: rgba(255, 255, 255, 0.9);
}

:deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.3);
}

:deep(.el-input.is-disabled .el-input__wrapper) {
  background: rgba(20, 20, 20, 0.3);
  border-color: rgba(255, 255, 255, 0.1);
}

:deep(.el-input.is-disabled .el-input__inner) {
  color: rgba(255, 255, 255, 0.4);
}

:deep(.el-textarea__inner) {
  border-radius: var(--radius-sm);
  border: 1px solid rgba(255, 59, 48, 0.2);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  transition: all 0.3s ease;
  font-family: var(--font-apple);
  line-height: 1.6;
  background: rgba(30, 30, 30, 0.5);
  color: rgba(255, 255, 255, 0.9);
}

:deep(.el-textarea__inner::placeholder) {
  color: rgba(255, 255, 255, 0.3);
}

:deep(.el-textarea__inner:hover) {
  box-shadow: 0 4px 12px rgba(255, 59, 48, 0.15);
  border-color: rgba(255, 59, 48, 0.4);
}

:deep(.el-textarea__inner:focus) {
  box-shadow: 0 4px 16px rgba(255, 59, 48, 0.25);
  border-color: var(--hacker-red);
}

/* ============================================
   Badge Styling - 深色徽章
   ============================================ */
:deep(.el-badge__content) {
  background-color: var(--hacker-red);
  border-radius: 10px;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(255, 59, 48, 0.3);
}

/* ============================================
   Skeleton Styling - 深色骨架屏
   ============================================ */
:deep(.el-skeleton) {
  padding: var(--spacing-lg);
}

:deep(.el-skeleton__item) {
  background: linear-gradient(90deg,
    rgba(255, 255, 255, 0.03) 25%,
    rgba(255, 255, 255, 0.05) 37%,
    rgba(255, 255, 255, 0.03) 63%);
  background-size: 400% 100%;
  animation: skeleton-loading 1.4s ease infinite;
}

@keyframes skeleton-loading {
  0% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0 50%;
  }
}

/* ============================================
   Responsive Design - 响应式设计
   ============================================ */
@media (max-width: 768px) {
  .my-team-page {
    padding: var(--spacing-md);
  }

  .team-content,
  .no-team-content {
    padding: var(--spacing-md);
  }

  .members-section,
  .applications-section {
    padding: var(--spacing-md);
  }
}

/* ============================================
   蓝队"我的战队"页面主题 - Blue Team Theme
   ============================================ */

/* 蓝队页面背景 */
.my-team-page.theme-blue {
  background: linear-gradient(135deg, #0a1428 0%, #0d1a2d 50%, #0f1520 100%);
}

/* 蓝队卡片样式 */
.my-team-page.theme-blue .team-card {
  border: 0.5px solid rgba(0, 212, 255, 0.2);
}

.my-team-page.theme-blue .team-card::before {
  background: linear-gradient(180deg, rgba(0, 212, 255, 0.8) 0%, rgba(0, 212, 255, 0.3) 100%);
}

.my-team-page.theme-blue .team-card:hover {
  box-shadow: 0 12px 48px rgba(0, 212, 255, 0.2), 0 0 30px rgba(0, 212, 255, 0.1);
  border-color: rgba(0, 212, 255, 0.3);
}

/* 蓝队卡片标题 */
.my-team-page.theme-blue .card-header {
  background: linear-gradient(135deg, #00d4ff 0%, #4dd0ff 100%);
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  text-shadow: 0 0 30px rgba(0, 212, 255, 0.3);
  filter: drop-shadow(0 0 20px rgba(0, 212, 255, 0.3));
}

/* 蓝队成员区域 */
.my-team-page.theme-blue .members-section,
.my-team-page.theme-blue .applications-section {
  border: 0.5px solid rgba(0, 212, 255, 0.2);
}

/* 蓝队描述列表样式 */
.my-team-page.theme-blue :deep(.el-descriptions) {
  border: 2px solid rgba(0, 212, 255, 0.5);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4),
              0 0 30px rgba(0, 212, 255, 0.3),
              inset 0 1px 0 rgba(255, 255, 255, 0.05),
              inset 0 0 1px rgba(0, 212, 255, 0.3);
}

.my-team-page.theme-blue :deep(.el-descriptions__table) {
  border-color: rgba(0, 212, 255, 0.35) !important;
}

.my-team-page.theme-blue :deep(.el-descriptions__cell) {
  border-color: rgba(0, 212, 255, 0.35) !important;
}

/* 蓝队表格样式 */
.my-team-page.theme-blue :deep(.el-table th.el-table__cell) {
  color: #00d4ff !important;
  text-shadow: 0 0 15px rgba(0, 212, 255, 0.6);
  border-bottom: 2px solid rgba(0, 212, 255, 0.4);
}

.my-team-page.theme-blue :deep(.el-table__row:hover) {
  background: rgba(0, 212, 255, 0.08) !important;
}

/* 蓝队按钮样式 */
.my-team-page.theme-blue :deep(.el-button--primary) {
  background: linear-gradient(135deg, rgba(0, 212, 255, 0.2) 0%, rgba(0, 212, 255, 0.3) 100%);
  border: 0.5px solid rgba(0, 212, 255, 0.3);
  box-shadow: 0 4px 12px rgba(0, 212, 255, 0.15);
}

.my-team-page.theme-blue :deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, rgba(0, 212, 255, 0.3) 0%, rgba(0, 212, 255, 0.4) 100%);
  border-color: rgba(0, 212, 255, 0.5);
  box-shadow: 0 6px 20px rgba(0, 212, 255, 0.35);
}

.my-team-page.theme-blue :deep(.el-button--danger) {
  background: linear-gradient(135deg, rgba(0, 212, 255, 0.2) 0%, rgba(0, 212, 255, 0.3) 100%);
  border: 0.5px solid rgba(0, 212, 255, 0.3);
  box-shadow: 0 4px 12px rgba(0, 212, 255, 0.15);
}

.my-team-page.theme-blue :deep(.el-button--danger:hover) {
  background: linear-gradient(135deg, rgba(0, 212, 255, 0.3) 0%, rgba(0, 212, 255, 0.4) 100%);
  border-color: rgba(0, 212, 255, 0.5);
  box-shadow: 0 6px 20px rgba(0, 212, 255, 0.35);
}

/* 蓝队表单输入样式 */
.my-team-page.theme-blue :deep(.el-input__wrapper) {
  border: 1px solid rgba(0, 212, 255, 0.2);
}

.my-team-page.theme-blue :deep(.el-input__wrapper:hover) {
  box-shadow: 0 4px 12px rgba(0, 212, 255, 0.15);
  border-color: rgba(0, 212, 255, 0.4);
}

.my-team-page.theme-blue :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 4px 16px rgba(0, 212, 255, 0.25);
  border-color: #00d4ff;
}

.my-team-page.theme-blue :deep(.el-textarea__inner) {
  border: 1px solid rgba(0, 212, 255, 0.2);
}

.my-team-page.theme-blue :deep(.el-textarea__inner:hover) {
  box-shadow: 0 4px 12px rgba(0, 212, 255, 0.15);
  border-color: rgba(0, 212, 255, 0.4);
}

.my-team-page.theme-blue :deep(.el-textarea__inner:focus) {
  box-shadow: 0 4px 16px rgba(0, 212, 255, 0.25);
  border-color: #00d4ff;
}

/* 蓝队对话框样式 */
.my-team-page.theme-blue :deep(.el-dialog) {
  border: 0.5px solid rgba(0, 212, 255, 0.2);
}

.my-team-page.theme-blue :deep(.el-dialog__header) {
  border-bottom: 1px solid rgba(0, 212, 255, 0.2);
}

.my-team-page.theme-blue :deep(.el-dialog__footer) {
  border-top: 1px solid rgba(0, 212, 255, 0.15);
}

/* 蓝队警告框样式 */
.my-team-page.theme-blue :deep(.el-alert) {
  border: 0.5px solid rgba(0, 212, 255, 0.2);
}

/* 蓝队徽章样式 */
.my-team-page.theme-blue :deep(.el-badge__content) {
  background-color: #00d4ff;
  box-shadow: 0 2px 8px rgba(0, 212, 255, 0.3);
}

/* 蓝队空状态图标 */
.my-team-page.theme-blue .empty-icon {
  filter: drop-shadow(0 0 15px rgba(0, 212, 255, 0.3));
}
</style>

<style>
/* ============================================
   全局 MessageBox 样式 - 红队风格确认框
   Global MessageBox Styling - Red Team Confirm Box
   注意：此样式不使用 scoped，因为 MessageBox 是全局挂载组件
   ============================================ */

/* MessageBox 容器 */
.red-team-confirm-box {
  background: rgba(20, 20, 20, 0.95) !important;
  backdrop-filter: blur(30px);
  -webkit-backdrop-filter: blur(30px);
  border: 2px solid rgba(255, 59, 48, 0.5) !important;
  border-radius: 20px;
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.6),
              0 0 40px rgba(255, 59, 48, 0.3),
              inset 0 1px 0 rgba(255, 255, 255, 0.05) !important;
}

/* MessageBox 标题栏 */
.red-team-confirm-box .el-message-box__header {
  background: rgba(30, 30, 30, 0.9);
  border-bottom: 1px solid rgba(255, 59, 48, 0.3);
  padding: 24px;
}

/* MessageBox 标题文字 */
.red-team-confirm-box .el-message-box__title {
  color: rgba(255, 255, 255, 0.95) !important;
  font-weight: 700;
  font-size: 18px;
  text-shadow: 0 0 8px rgba(255, 59, 48, 0.4);
}

/* MessageBox 内容区域 */
.red-team-confirm-box .el-message-box__content {
  color: rgba(255, 255, 255, 0.85) !important;
  font-size: 15px;
  padding: 24px;
}

/* MessageBox 消息文本 */
.red-team-confirm-box .el-message-box__message {
  color: rgba(255, 255, 255, 0.85) !important;
}

/* MessageBox 按钮栏 */
.red-team-confirm-box .el-message-box__btns {
  padding: 16px 24px;
  border-top: 1px solid rgba(255, 59, 48, 0.2);
  background: rgba(15, 15, 15, 0.6);
}

/* 确认按钮（危险操作） */
.red-team-danger-btn {
  background: linear-gradient(135deg, rgba(255, 59, 48, 0.25) 0%, rgba(255, 59, 48, 0.35) 100%) !important;
  border: 1px solid rgba(255, 59, 48, 0.5) !important;
  color: #ffffff !important;
  font-weight: 600;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(255, 59, 48, 0.2);
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

.red-team-danger-btn:hover {
  background: linear-gradient(135deg, rgba(255, 59, 48, 0.35) 0%, rgba(255, 59, 48, 0.45) 100%) !important;
  border-color: rgba(255, 59, 48, 0.7) !important;
  transform: translateY(-2px);
  box-shadow: 0 6px 24px rgba(255, 59, 48, 0.4);
}

/* 确认按钮（批准操作） - 绿色主题 */
.red-team-success-btn {
  background: linear-gradient(135deg, rgba(52, 199, 89, 0.2) 0%, rgba(52, 199, 89, 0.3) 100%) !important;
  border: 1px solid rgba(52, 199, 89, 0.5) !important;
  color: #ffffff !important;
  font-weight: 600;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(52, 199, 89, 0.2);
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

.red-team-success-btn:hover {
  background: linear-gradient(135deg, rgba(52, 199, 89, 0.3) 0%, rgba(52, 199, 89, 0.4) 100%) !important;
  border-color: rgba(52, 199, 89, 0.7) !important;
  transform: translateY(-2px);
  box-shadow: 0 6px 24px rgba(52, 199, 89, 0.4);
}

/* 取消按钮 */
.red-team-cancel-btn {
  background: rgba(142, 142, 147, 0.15) !important;
  border: 1px solid rgba(142, 142, 147, 0.3) !important;
  color: rgba(255, 255, 255, 0.8) !important;
  font-weight: 600;
  border-radius: 12px;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

.red-team-cancel-btn:hover {
  background: rgba(142, 142, 147, 0.25) !important;
  border-color: rgba(142, 142, 147, 0.5) !important;
  color: #ffffff !important;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(142, 142, 147, 0.3);
}

/* ============================================
   全局 MessageBox 样式 - 蓝队风格确认框
   Global MessageBox Styling - Blue Team Confirm Box
   注意：此样式不使用 scoped，因为 MessageBox 是全局挂载组件
   ============================================ */

/* MessageBox 容器 */
.blue-team-confirm-box {
  background: rgba(20, 20, 20, 0.95) !important;
  backdrop-filter: blur(30px);
  -webkit-backdrop-filter: blur(30px);
  border: 2px solid rgba(0, 212, 255, 0.5) !important;
  border-radius: 20px;
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.6),
              0 0 40px rgba(0, 212, 255, 0.3),
              inset 0 1px 0 rgba(255, 255, 255, 0.05) !important;
}

/* MessageBox 标题栏 */
.blue-team-confirm-box .el-message-box__header {
  background: rgba(30, 30, 30, 0.9);
  border-bottom: 1px solid rgba(0, 212, 255, 0.3);
  padding: 24px;
}

/* MessageBox 标题文字 */
.blue-team-confirm-box .el-message-box__title {
  color: rgba(255, 255, 255, 0.95) !important;
  font-weight: 700;
  font-size: 18px;
  text-shadow: 0 0 8px rgba(0, 212, 255, 0.4);
}

/* MessageBox 内容区域 */
.blue-team-confirm-box .el-message-box__content {
  color: rgba(255, 255, 255, 0.85) !important;
  font-size: 15px;
  padding: 24px;
}

/* MessageBox 消息文本 */
.blue-team-confirm-box .el-message-box__message {
  color: rgba(255, 255, 255, 0.85) !important;
}

/* MessageBox 按钮栏 */
.blue-team-confirm-box .el-message-box__btns {
  padding: 16px 24px;
  border-top: 1px solid rgba(0, 212, 255, 0.2);
  background: rgba(15, 15, 15, 0.6);
}

/* 确认按钮（危险操作） */
.blue-team-danger-btn {
  background: linear-gradient(135deg, rgba(0, 212, 255, 0.25) 0%, rgba(0, 212, 255, 0.35) 100%) !important;
  border: 1px solid rgba(0, 212, 255, 0.5) !important;
  color: #ffffff !important;
  font-weight: 600;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(0, 212, 255, 0.2);
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

.blue-team-danger-btn:hover {
  background: linear-gradient(135deg, rgba(0, 212, 255, 0.35) 0%, rgba(0, 212, 255, 0.45) 100%) !important;
  border-color: rgba(0, 212, 255, 0.7) !important;
  transform: translateY(-2px);
  box-shadow: 0 6px 24px rgba(0, 212, 255, 0.4);
}

/* 确认按钮（批准操作） - 绿色主题 */
.blue-team-success-btn {
  background: linear-gradient(135deg, rgba(52, 199, 89, 0.2) 0%, rgba(52, 199, 89, 0.3) 100%) !important;
  border: 1px solid rgba(52, 199, 89, 0.5) !important;
  color: #ffffff !important;
  font-weight: 600;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(52, 199, 89, 0.2);
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

.blue-team-success-btn:hover {
  background: linear-gradient(135deg, rgba(52, 199, 89, 0.3) 0%, rgba(52, 199, 89, 0.4) 100%) !important;
  border-color: rgba(52, 199, 89, 0.7) !important;
  transform: translateY(-2px);
  box-shadow: 0 6px 24px rgba(52, 199, 89, 0.4);
}

/* 取消按钮 */
.blue-team-cancel-btn {
  background: rgba(142, 142, 147, 0.15) !important;
  border: 1px solid rgba(142, 142, 147, 0.3) !important;
  color: rgba(255, 255, 255, 0.8) !important;
  font-weight: 600;
  border-radius: 12px;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

.blue-team-cancel-btn:hover {
  background: rgba(142, 142, 147, 0.25) !important;
  border-color: rgba(142, 142, 147, 0.5) !important;
  color: #ffffff !important;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(142, 142, 147, 0.3);
}
</style>
