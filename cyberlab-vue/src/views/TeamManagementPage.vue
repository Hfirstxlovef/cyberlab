<template>
  <div class="team-management-page">
    <el-card class="page-card">
      <template #header>
        <div class="card-header">
          <span>👥 战队管理</span>
          <el-button type="primary" @click="openCreateDialog" size="default" class="create-team-btn">
            创建战队
          </el-button>
        </div>
      </template>

      <!-- 战队类型标签页 -->
      <el-tabs v-model="activeTab" @tab-change="handleTabChange" class="team-tabs">
        <el-tab-pane label="🔴 红队" name="red">
          <component :is="TableComponent" v-if="TableComponent" :teams="currentTeams" :loading="loading"
            @view="viewTeam" @edit="editTeam" @delete="confirmDeleteTeam" />
        </el-tab-pane>
        <el-tab-pane label="🔵 蓝队" name="blue">
          <component :is="TableComponent" v-if="TableComponent" :teams="currentTeams" :loading="loading"
            @view="viewTeam" @edit="editTeam" @delete="confirmDeleteTeam" />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 创建战队对话框 -->
    <el-dialog v-model="createDialogVisible" title="创建战队" width="600px">
      <el-form :model="createForm" :rules="createRules" ref="createFormRef" label-width="100px">
        <el-form-item label="战队名称" prop="name">
          <el-input v-model="createForm.name" placeholder="请输入战队名称"></el-input>
        </el-form-item>
        <el-form-item label="战队类型" prop="teamType">
          <el-select v-model="createForm.teamType" placeholder="请选择战队类型" style="width: 100%">
            <el-option label="红队" value="red"></el-option>
            <el-option label="蓝队" value="blue"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="队长" prop="leaderId">
          <el-select v-model="createForm.leaderId" placeholder="请选择队长" filterable style="width: 100%">
            <el-option v-for="user in filteredAvailableUsersForCreate" :key="user.id" :label="user.username" :value="user.id">
              <span>{{ user.username }}</span>
              <span style="color: #8492a6; font-size: 13px; margin-left: 8px">({{ getRoleDisplayName(user.role)
                  }})</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="战队描述" prop="description">
          <el-input v-model="createForm.description" type="textarea" :rows="4" placeholder="请输入战队描述"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreate" :loading="submitting">创建</el-button>
      </template>
    </el-dialog>

    <!-- 编辑战队对话框 -->
    <el-dialog v-model="editDialogVisible" title="编辑战队" width="600px">
      <el-form :model="editForm" :rules="editRules" ref="editFormRef" label-width="100px">
        <el-form-item label="战队名称" prop="name">
          <el-input v-model="editForm.name" placeholder="请输入战队名称"></el-input>
        </el-form-item>
        <el-form-item label="队长" prop="leaderId">
          <el-select v-model="editForm.leaderId" placeholder="请选择队长" filterable style="width: 100%">
            <el-option v-for="member in currentTeamMembers" :key="member.userId" :label="member.username"
              :value="member.userId">
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="战队描述" prop="description">
          <el-input v-model="editForm.description" type="textarea" :rows="4" placeholder="请输入战队描述"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleEdit" :loading="submitting">保存</el-button>
      </template>
    </el-dialog>

    <!-- 查看战队详情对话框 -->
    <el-dialog v-model="viewDialogVisible" title="战队详情" width="700px">
      <div v-if="currentTeam" class="team-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="战队ID">{{ currentTeam.id }}</el-descriptions-item>
          <el-descriptions-item label="战队名称">{{ currentTeam.name }}</el-descriptions-item>
          <el-descriptions-item label="战队类型">
            <el-tag :type="getTeamTypeTagType(currentTeam.teamType)">
              {{ getTeamTypeDisplayName(currentTeam.teamType) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="currentTeam.status === 'active' ? 'success' : 'danger'">
              {{ currentTeam.status === 'active' ? '活跃' : '已解散' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="队长">{{ currentTeam.leaderName || '未知' }}</el-descriptions-item>
          <el-descriptions-item label="成员数量">{{ currentTeam.memberCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="创建时间" :span="2">
            {{ formatDateTime(currentTeam.createdAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="战队描述" :span="2">
            {{ currentTeam.description || '无描述' }}
          </el-descriptions-item>
        </el-descriptions>

        <!-- 成员列表 -->
        <div class="members-section">
          <div class="section-header">
            <h4>战队成员</h4>
            <el-button size="small" type="primary" @click="openAddMemberDialog">
              <el-icon><Plus /></el-icon>
              添加成员
            </el-button>
          </div>
          <el-table :data="currentTeamMembers" style="width: 100%" max-height="300">
            <el-table-column prop="userId" label="用户ID" width="80" align="center" />
            <el-table-column prop="username" label="用户名" min-width="120" />
            <el-table-column label="角色" width="100" align="center">
              <template #default="scope">
                <el-tag :type="scope.row.memberRole === 'leader' ? 'warning' : 'info'" size="small">
                  {{ scope.row.memberRole === 'leader' ? '队长' : '成员' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="加入时间" width="180" align="center">
              <template #default="scope">
                {{ formatDateTime(scope.row.joinedAt) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" align="center">
              <template #default="scope">
                <el-button v-if="scope.row.memberRole !== 'leader'" size="small" type="danger"
                  @click="confirmRemoveMember(scope.row)">
                  移除
                </el-button>
                <span v-else style="color: #8492a6; font-size: 12px">不可移除</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </el-dialog>

    <!-- 添加成员对话框 -->
    <el-dialog v-model="addMemberDialogVisible" title="添加成员" width="500px">
      <el-form :model="addMemberForm" :rules="addMemberRules" ref="addMemberFormRef" label-width="80px">
        <el-form-item label="选择用户" prop="userId">
          <el-select v-model="addMemberForm.userId" placeholder="请选择用户" filterable style="width: 100%">
            <el-option v-for="user in filteredAvailableUsersForAddMember" :key="user.id" :label="user.username" :value="user.id">
              <span>{{ user.username }}</span>
              <span style="color: #8492a6; font-size: 13px; margin-left: 8px">({{ getRoleDisplayName(user.role)
                  }})</span>
            </el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addMemberDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAddMember" :loading="submitting">添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, markRaw, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  getTeamList,
  createTeam,
  updateTeam,
  deleteTeam,
  getTeamMembers,
  addTeamMember,
  removeTeamMember
} from '@/api/team'
import { getUserList } from '@/api/user'

// 响应式数据
const activeTab = ref('red')
const loading = ref(false)
const submitting = ref(false)
const redTeams = ref([])
const blueTeams = ref([])
const TableComponent = ref(null)
const availableUsers = ref([])
const currentTeam = ref(null)
const currentTeamMembers = ref([])

// 对话框显示状态
const createDialogVisible = ref(false)
const editDialogVisible = ref(false)
const viewDialogVisible = ref(false)
const addMemberDialogVisible = ref(false)

// 表单引用
const createFormRef = ref()
const editFormRef = ref()
const addMemberFormRef = ref()

// 创建表单
const createForm = reactive({
  name: '',
  leaderId: null,
  description: '',
  teamType: 'red'
})

// 编辑表单
const editForm = reactive({
  id: null,
  name: '',
  leaderId: null,
  description: ''
})

// 添加成员表单
const addMemberForm = reactive({
  userId: null
})

// 表单验证规则
const createRules = {
  name: [{ required: true, message: '请输入战队名称', trigger: 'blur' }],
  teamType: [{ required: true, message: '请选择战队类型', trigger: 'change' }],
  leaderId: [{ required: true, message: '请选择队长', trigger: 'change' }]
}

const editRules = {
  name: [{ required: true, message: '请输入战队名称', trigger: 'blur' }],
  leaderId: [{ required: true, message: '请选择队长', trigger: 'change' }]
}

const addMemberRules = {
  userId: [{ required: true, message: '请选择用户', trigger: 'change' }]
}

// 当前显示的战队列表
const currentTeams = computed(() => {
  return activeTab.value === 'red' ? redTeams.value : blueTeams.value
})

// 创建战队时的队长候选人（根据teamType过滤，只显示对应角色的用户）
const filteredAvailableUsersForCreate = computed(() => {
  if (!createForm.teamType) return []
  return availableUsers.value.filter(user => user.role === createForm.teamType)
})

// 添加成员时的候选人（根据当前战队类型过滤，只显示对应角色的用户）
const filteredAvailableUsersForAddMember = computed(() => {
  if (!currentTeam.value) return []
  return availableUsers.value.filter(user => user.role === currentTeam.value.teamType)
})

// 监听战队类型变化，自动清空队长选择（避免选择了红队用户后切换到蓝队仍保留）
watch(() => createForm.teamType, (newType, oldType) => {
  if (newType !== oldType && oldType) {
    createForm.leaderId = null
  }
})

// 动态加载TeamTable组件
onMounted(async () => {
  try {
    const module = await import('@/components/TeamTable.vue')
    TableComponent.value = markRaw(module.default)
  } catch (error) {
    console.error('Failed to load TeamTable component:', error)
  }

  // 加载战队数据
  await Promise.all([fetchTeams('red'), fetchTeams('blue')])

  // 加载可用用户列表
  await fetchAvailableUsers()
})

// 获取战队列表
const fetchTeams = async (teamType) => {
  try {
    loading.value = true

    // 先获取所有用户列表（一次性获取，提高效率）
    let allUsers = []
    try {
      allUsers = await getUserList()
    } catch (error) {
      console.error('获取用户列表失败:', error)
    }

    const response = await getTeamList({ teamType })

    // 为每个战队添加额外信息
    const teamsWithInfo = await Promise.all(
      response.map(async (team) => {
        try {
          // 获取成员数量
          const members = await getTeamMembers(team.id)

          // 使用 team.leaderId 从用户列表中查找队长名称
          const leader = allUsers.find(u => Number(u.id) === Number(team.leaderId))

          return {
            ...team,
            memberCount: members.length,
            leaderName: leader?.username || '未知'
          }
        } catch {
          return {
            ...team,
            memberCount: 0,
            leaderName: '未知'
          }
        }
      })
    )

    if (teamType === 'red') {
      redTeams.value = teamsWithInfo
    } else {
      blueTeams.value = teamsWithInfo
    }
  } catch (error) {
    ElMessage.error('获取战队列表失败')
  } finally {
    loading.value = false
  }
}

// 获取可用用户列表（未加入任何战队的用户）
const fetchAvailableUsers = async () => {
  try {
    const response = await getUserList()
    // 过滤出未加入战队的用户
    availableUsers.value = response.filter(user => !user.teamId)
  } catch (error) {
    console.error('Failed to fetch available users:', error)
  }
}

// 标签页切换
const handleTabChange = (tabName) => {
  activeTab.value = tabName
}

// 打开创建对话框
const openCreateDialog = () => {
  // 重置表单
  Object.assign(createForm, {
    name: '',
    leaderId: null,
    description: '',
    teamType: activeTab.value
  })
  createDialogVisible.value = true
}

// 创建战队
const handleCreate = async () => {
  try {
    await createFormRef.value.validate()
    submitting.value = true

    await createTeam(createForm)
    ElMessage.success('战队创建成功')

    createDialogVisible.value = false
    await fetchTeams(createForm.teamType)
    await fetchAvailableUsers()
  } catch (error) {
    if (error !== false) { // 不是验证错误
      ElMessage.error(error.response?.data || '创建战队失败')
    }
  } finally {
    submitting.value = false
  }
}

// 查看战队详情
const viewTeam = async (team) => {
  currentTeam.value = team
  await fetchTeamMembers(team.id)

  // 从成员列表中找到队长并更新 leaderName
  const leader = currentTeamMembers.value.find(m => m.memberRole === 'leader')
  if (leader) {
    currentTeam.value = {
      ...currentTeam.value,
      leaderName: leader.username
    }
  }

  viewDialogVisible.value = true
}

// 编辑战队
const editTeam = async (team) => {
  currentTeam.value = team
  await fetchTeamMembers(team.id)

  Object.assign(editForm, {
    id: team.id,
    name: team.name,
    leaderId: team.leaderId,
    description: team.description
  })
  editDialogVisible.value = true
}

// 保存编辑
const handleEdit = async () => {
  try {
    await editFormRef.value.validate()
    submitting.value = true

    await updateTeam(editForm.id, {
      name: editForm.name,
      leaderId: editForm.leaderId,
      description: editForm.description
    })

    ElMessage.success('战队信息更新成功')
    editDialogVisible.value = false

    await fetchTeams(activeTab.value)
  } catch (error) {
    if (error !== false) {
      ElMessage.error(error.response?.data || '更新战队失败')
    }
  } finally {
    submitting.value = false
  }
}

// 确认解散战队
const confirmDeleteTeam = (team) => {
  ElMessageBox.confirm(
    `确定要解散战队 "${team.name}" 吗？解散后将移除所有成员，且数据无法恢复。`,
    '确认解散',
    {
      confirmButtonText: '解散',
      cancelButtonText: '取消',
      type: 'warning',
      customClass: 'apple-elegant-message-box',
      confirmButtonClass: 'apple-confirm-button',
      cancelButtonClass: 'apple-cancel-button'
    }
  ).then(async () => {
    try {
      await deleteTeam(team.id)
      ElMessage.success('战队解散成功')
      await fetchTeams(activeTab.value)
      await fetchAvailableUsers()
    } catch (error) {
      ElMessage.error(error.response?.data || '解散战队失败')
    }
  }).catch(() => {
    // 用户取消
  })
}

// 获取战队成员列表
const fetchTeamMembers = async (teamId) => {
  try {
    const members = await getTeamMembers(teamId)
    // 为每个成员添加用户名（从availableUsers或其他来源获取）
    currentTeamMembers.value = await Promise.all(
      members.map(async (member) => {
        try {
          const allUsers = await getUserList()
          const user = allUsers.find(u => Number(u.id) === Number(member.userId))
          return {
            ...member,
            username: user?.username || '未知用户'
          }
        } catch {
          return {
            ...member,
            username: '未知用户'
          }
        }
      })
    )
  } catch (error) {
    console.error('Failed to fetch team members:', error)
    currentTeamMembers.value = []
  }
}

// 打开添加成员对话框
const openAddMemberDialog = () => {
  addMemberForm.userId = null
  addMemberDialogVisible.value = true
}

// 添加成员
const handleAddMember = async () => {
  try {
    await addMemberFormRef.value.validate()
    submitting.value = true

    await addTeamMember(currentTeam.value.id, { userId: addMemberForm.userId })
    ElMessage.success('成员添加成功')

    addMemberDialogVisible.value = false
    await fetchTeamMembers(currentTeam.value.id)
    await fetchAvailableUsers()
    await fetchTeams(activeTab.value)
  } catch (error) {
    if (error !== false) {
      ElMessage.error(error.response?.data || '添加成员失败')
    }
  } finally {
    submitting.value = false
  }
}

// 确认移除成员
const confirmRemoveMember = (member) => {
  ElMessageBox.confirm(
    `确定要移除成员 "${member.username}" 吗？`,
    '确认移除',
    {
      confirmButtonText: '移除',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await removeTeamMember(currentTeam.value.id, member.userId)
      ElMessage.success('成员移除成功')
      await fetchTeamMembers(currentTeam.value.id)
      await fetchAvailableUsers()
      await fetchTeams(activeTab.value)
    } catch (error) {
      ElMessage.error(error.response?.data || '移除成员失败')
    }
  }).catch(() => {
    // 用户取消
  })
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

const getRoleDisplayName = (role) => {
  const roleMap = {
    red: '红队',
    blue: '蓝队',
    judge: '裁判',
    admin: '管理员'
  }
  return roleMap[role] || '未知'
}
</script>

<style scoped>
/* ============================================
   Apple Elegant White Style - 战队管理页面
   Team Management Page
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

.team-management-page {
  padding: var(--spacing-lg);
  font-family: var(--font-apple);
}

/* ============================================
   Page Card
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

.page-card:hover {
  box-shadow: var(--shadow-card-hover);
}

/* ============================================
   Card Header
   ============================================ */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 20px;
  font-weight: 700;
  color: var(--apple-text);
}

/* ============================================
   Tabs
   ============================================ */
.team-tabs {
  margin-top: var(--spacing-md);
}

.team-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.team-tabs :deep(.el-tabs__item) {
  font-weight: 600;
  font-size: 15px;
  color: var(--apple-text-secondary);
}

.team-tabs :deep(.el-tabs__item.is-active) {
  color: var(--apple-blue);
}

.team-tabs :deep(.el-tabs__active-bar) {
  background-color: var(--apple-blue);
}

/* ============================================
   Team Detail
   ============================================ */
.team-detail {
  padding: var(--spacing-md);
}

.team-detail :deep(.el-descriptions) {
  margin-bottom: var(--spacing-lg);
}

.members-section {
  margin-top: var(--spacing-lg);
  padding: var(--spacing-md);
  background: rgba(0, 0, 0, 0.02);
  border-radius: var(--radius-md);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-md);
}

.section-header h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: var(--apple-text);
}

/* ============================================
   Buttons Styling
   ============================================ */
:deep(.el-button) {
  border-radius: var(--radius-sm);
  font-weight: 600;
  font-family: var(--font-apple);
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
  padding: 10px 20px;
  font-size: 14px;
}

/* 创建战队按钮 - 高雅白色风格 */
.card-header :deep(.create-team-btn) {
  background: linear-gradient(135deg, #ffffff 0%, #f8f9fa 100%);
  border: 1.5px solid rgba(0, 0, 0, 0.08);
  color: var(--apple-text);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04),
              0 0 0 1px rgba(255, 255, 255, 0.5) inset;
  font-weight: 600;
  letter-spacing: 0.3px;
}

.card-header :deep(.create-team-btn:hover) {
  background: linear-gradient(135deg, #ffffff 0%, #f0f1f3 100%);
  border-color: rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08),
              0 0 0 1px rgba(255, 255, 255, 0.6) inset;
  color: var(--apple-blue);
}

.card-header :deep(.create-team-btn:active) {
  transform: translateY(0);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.06);
}

:deep(.el-button--default) {
  background: rgba(0, 0, 0, 0.02);
  border: 1px solid var(--apple-border);
  color: var(--apple-text);
}

:deep(.el-button--default:hover) {
  background: rgba(0, 0, 0, 0.04);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

:deep(.el-button--danger) {
  background: linear-gradient(135deg, var(--apple-red) 0%, #d32f2f 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(255, 59, 48, 0.25);
}

:deep(.el-button--danger:hover) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(255, 59, 48, 0.35);
}

:deep(.el-button--warning) {
  background: linear-gradient(135deg, var(--apple-orange) 0%, #e68900 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(255, 149, 0, 0.25);
}

:deep(.el-button--warning:hover) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(255, 149, 0, 0.35);
}

:deep(.el-button--success) {
  background: linear-gradient(135deg, var(--apple-green) 0%, #28a745 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(52, 199, 89, 0.25);
}

:deep(.el-button--success:hover) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(52, 199, 89, 0.35);
}

/* ============================================
   Dialog Styling
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

:deep(.el-dialog__footer) {
  padding: var(--spacing-md) var(--spacing-lg);
  border-top: 1px solid var(--apple-border);
  background: rgba(250, 250, 250, 0.5);
}

:deep(.el-dialog__close) {
  color: var(--apple-text-secondary);
  transition: color 0.3s ease;
}

:deep(.el-dialog__close:hover) {
  color: var(--apple-blue);
}

/* ============================================
   Form Styling
   ============================================ */
:deep(.el-form-item__label) {
  font-weight: 600;
  color: var(--apple-text);
  font-family: var(--font-apple);
}

:deep(.el-input__wrapper) {
  border-radius: var(--radius-sm);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
  border: 1px solid var(--apple-border);
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.08);
  border-color: var(--apple-blue);
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 4px 16px rgba(0, 122, 255, 0.15);
  border-color: var(--apple-blue);
}

:deep(.el-textarea__inner) {
  border-radius: var(--radius-sm);
  border: 1px solid var(--apple-border);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
  font-family: var(--font-apple);
  line-height: 1.6;
}

:deep(.el-textarea__inner:hover) {
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.08);
  border-color: var(--apple-blue);
}

:deep(.el-textarea__inner:focus) {
  box-shadow: 0 4px 16px rgba(0, 122, 255, 0.15);
  border-color: var(--apple-blue);
}

:deep(.el-select) {
  width: 100%;
}

:deep(.el-select .el-input__wrapper) {
  border-radius: var(--radius-sm);
}

:deep(.el-select:hover .el-input__wrapper) {
  border-color: var(--apple-blue);
}

:deep(.el-select .el-input.is-focus .el-input__wrapper) {
  border-color: var(--apple-blue);
  box-shadow: 0 4px 16px rgba(0, 122, 255, 0.15);
}

/* ============================================
   Descriptions Styling
   ============================================ */
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
  background: rgba(255, 255, 255, 0.5) !important;
}

:deep(.el-descriptions__table) {
  border-color: var(--apple-border) !important;
}

:deep(.el-descriptions__cell) {
  border-color: var(--apple-border) !important;
}

/* ============================================
   Members Section Styling
   ============================================ */
.members-section {
  margin-top: var(--spacing-lg);
  padding: var(--spacing-lg);
  background: rgba(0, 0, 0, 0.02);
  border-radius: var(--radius-md);
  border: 0.5px solid var(--apple-border);
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
  color: var(--apple-text);
}

.members-section :deep(.el-table) {
  background: transparent;
  border-radius: var(--radius-sm);
}

.members-section :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.members-section :deep(.el-table thead) {
  background: rgba(0, 0, 0, 0.02);
}

.members-section :deep(.el-table__row) {
  transition: all 0.2s ease;
}

.members-section :deep(.el-table__row:hover) {
  background: rgba(0, 122, 255, 0.02) !important;
}

/* ============================================
   Loading State
   ============================================ */
:deep(.el-loading-mask) {
  background-color: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

:deep(.el-loading-spinner .circular) {
  stroke: var(--apple-blue);
}

:deep(.el-loading-spinner .el-loading-text) {
  color: var(--apple-text);
  font-weight: 500;
}

/* ============================================
   Tags Styling
   ============================================ */
:deep(.el-tag) {
  border-radius: var(--radius-sm);
  border: none;
  font-weight: 600;
  padding: 4px 12px;
  font-size: 12px;
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

:deep(.el-tag--primary) {
  background: rgba(0, 122, 255, 0.1);
  color: var(--apple-blue);
}

:deep(.el-tag--info) {
  background: rgba(0, 0, 0, 0.05);
  color: var(--apple-text-secondary);
}

/* ============================================
   Responsive Design
   ============================================ */
@media (max-width: 768px) {
  .team-management-page {
    padding: var(--spacing-md);
  }

  .card-header {
    flex-direction: column;
    gap: var(--spacing-sm);
    align-items: flex-start;
  }

  .card-header :deep(.el-button) {
    width: 100%;
  }

  .section-header {
    flex-direction: column;
    gap: var(--spacing-sm);
    align-items: stretch;
  }

  .section-header :deep(.el-button) {
    width: 100%;
  }
}

@media (max-width: 576px) {
  :deep(.el-dialog) {
    width: 90% !important;
  }

  :deep(.el-form-item__label) {
    font-size: 14px;
  }
}
</style>

<style>
/* ============================================
   Apple Elegant Message Box - Global Styles
   全局样式（不使用scoped，以覆盖Element Plus teleport组件）
   ============================================ */

/* 确认对话框容器 */
.apple-elegant-message-box {
  border-radius: 20px !important;
  border: 0.5px solid rgba(0, 0, 0, 0.06) !important;
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.12),
              0 0 0 0.5px rgba(255, 255, 255, 0.5) inset !important;
  backdrop-filter: blur(30px);
  -webkit-backdrop-filter: blur(30px);
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.98) 0%,
    rgba(252, 252, 252, 0.95) 100%) !important;
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", "Helvetica Neue", sans-serif !important;
  overflow: hidden;
}

/* 对话框头部 */
.apple-elegant-message-box .el-message-box__header {
  padding: 24px 24px 16px 24px !important;
  border-bottom: none !important;
}

.apple-elegant-message-box .el-message-box__title {
  font-size: 20px !important;
  font-weight: 700 !important;
  color: #1d1d1f !important;
  letter-spacing: -0.3px;
}

/* 警告图标 */
.apple-elegant-message-box .el-message-box__status {
  font-size: 28px !important;
  margin-right: 12px !important;
}

.apple-elegant-message-box .el-message-box__status.el-icon {
  color: #ff9500 !important;
  filter: drop-shadow(0 2px 8px rgba(255, 149, 0, 0.15));
}

/* 对话框内容 */
.apple-elegant-message-box .el-message-box__content {
  padding: 0 24px 24px 24px !important;
  color: #1d1d1f !important;
}

.apple-elegant-message-box .el-message-box__message {
  font-size: 15px !important;
  line-height: 1.6 !important;
  color: #1d1d1f !important;
  font-weight: 400 !important;
}

.apple-elegant-message-box .el-message-box__message p {
  margin: 0 !important;
  color: #1d1d1f !important;
}

/* 底部按钮区域 */
.apple-elegant-message-box .el-message-box__btns {
  padding: 16px 24px 24px 24px !important;
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

/* 取消按钮 - 高雅白 */
.apple-elegant-message-box .apple-cancel-button {
  background: linear-gradient(135deg, #ffffff 0%, #f8f9fa 100%) !important;
  border: 1.5px solid rgba(0, 0, 0, 0.08) !important;
  color: #1d1d1f !important;
  border-radius: 12px !important;
  padding: 10px 24px !important;
  font-size: 15px !important;
  font-weight: 600 !important;
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Text", "Helvetica Neue", sans-serif !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04),
              0 0 0 1px rgba(255, 255, 255, 0.5) inset !important;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1) !important;
  letter-spacing: 0.2px;
  min-width: 80px;
}

.apple-elegant-message-box .apple-cancel-button:hover {
  background: linear-gradient(135deg, #ffffff 0%, #f0f1f3 100%) !important;
  border-color: rgba(0, 0, 0, 0.12) !important;
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08),
              0 0 0 1px rgba(255, 255, 255, 0.6) inset !important;
  color: #007aff !important;
}

.apple-elegant-message-box .apple-cancel-button:active {
  transform: translateY(0) !important;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.06) !important;
}

/* 确认按钮 - 警告橙色 */
.apple-elegant-message-box .apple-confirm-button {
  background: linear-gradient(135deg, #ff9500 0%, #e68900 100%) !important;
  border: none !important;
  color: #ffffff !important;
  border-radius: 12px !important;
  padding: 10px 24px !important;
  font-size: 15px !important;
  font-weight: 600 !important;
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Text", "Helvetica Neue", sans-serif !important;
  box-shadow: 0 4px 16px rgba(255, 149, 0, 0.3) !important;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1) !important;
  letter-spacing: 0.2px;
  min-width: 80px;
}

.apple-elegant-message-box .apple-confirm-button:hover {
  background: linear-gradient(135deg, #ffa51e 0%, #f09500 100%) !important;
  transform: translateY(-1px);
  box-shadow: 0 6px 24px rgba(255, 149, 0, 0.4) !important;
}

.apple-elegant-message-box .apple-confirm-button:active {
  transform: translateY(0) !important;
  box-shadow: 0 3px 12px rgba(255, 149, 0, 0.3) !important;
}

/* 关闭按钮 */
.apple-elegant-message-box .el-message-box__close {
  color: #86868b !important;
  font-size: 20px !important;
  transition: all 0.2s ease !important;
}

.apple-elegant-message-box .el-message-box__close:hover {
  color: #ff9500 !important;
  transform: rotate(90deg);
}
</style>
