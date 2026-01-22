<template>
  <div class="user-mgmt-content">
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

    <el-card v-else class="user-card">
      <template #header>
        <div class="card-header">
          <div class="card-title">👤 用户管理</div>
          <el-button type="primary" @click="showCreateDialog = true">
            新建{{ getRoleDisplayName(activeRole) }}
          </el-button>
        </div>
      </template>

      <!-- 角色标签页 -->
      <el-tabs v-model="activeRole" @tab-change="handleTabChange" class="role-tabs">
        <el-tab-pane label="红队用户" name="red">
          <component 
            v-if="componentLoaded && UserTable" 
            :is="UserTable" 
            :users="users" 
            :loading="loading" 
            @edit="handleEdit" 
            @delete="handleDelete"
            @toggle="handleToggle" 
          />
          <el-skeleton v-else :rows="5" animated />
        </el-tab-pane>
        <el-tab-pane label="蓝队用户" name="blue">
          <component 
            v-if="componentLoaded && UserTable" 
            :is="UserTable" 
            :users="users" 
            :loading="loading" 
            @edit="handleEdit" 
            @delete="handleDelete"
            @toggle="handleToggle" 
          />
          <el-skeleton v-else :rows="5" animated />
        </el-tab-pane>
        <el-tab-pane label="裁判用户" name="judge">
          <component 
            v-if="componentLoaded && UserTable" 
            :is="UserTable" 
            :users="users" 
            :loading="loading" 
            @edit="handleEdit" 
            @delete="handleDelete"
            @toggle="handleToggle" 
          />
          <el-skeleton v-else :rows="5" animated />
        </el-tab-pane>
        <el-tab-pane label="管理员用户" name="admin">
          <component 
            v-if="componentLoaded && UserTable" 
            :is="UserTable" 
            :users="users" 
            :loading="loading" 
            @edit="handleEdit" 
            @delete="handleDelete"
            @toggle="handleToggle" 
          />
          <el-skeleton v-else :rows="5" animated />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 创建用户对话框 -->
    <el-dialog v-model="showCreateDialog" :title="'新建' + getRoleDisplayName(activeRole)" width="500px">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="createForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="createForm.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="createForm.role" disabled>
            <el-option :label="getRoleDisplayName(activeRole)" :value="activeRole" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreate" :loading="creating">创建</el-button>
      </template>
    </el-dialog>

    <!-- 编辑用户对话框 -->
    <el-dialog v-model="showEditDialog" title="编辑用户" width="500px">
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="editForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="editForm.role" placeholder="请选择角色" style="width: 100%" @change="handleRoleChange">
            <el-option label="红队用户" value="red">
              <el-tag type="danger">红队用户</el-tag>
            </el-option>
            <el-option label="蓝队用户" value="blue">
              <el-tag type="primary">蓝队用户</el-tag>
            </el-option>
            <el-option label="裁判用户" value="judge">
              <el-tag type="warning">裁判用户</el-tag>
            </el-option>
            <el-option label="管理员用户" value="admin">
              <el-tag type="success">管理员用户</el-tag>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="所属战队" v-if="editForm.role === 'red' || editForm.role === 'blue'">
          <el-select v-model="editForm.teamId" placeholder="请选择战队（可选）" clearable style="width: 100%">
            <el-option v-for="team in availableTeams" :key="team.id" :label="team.name" :value="team.id">
              <span>{{ team.name }}</span>
              <el-tag :type="team.teamType === 'red' ? 'danger' : 'primary'" size="small" style="margin-left: 8px">
                {{ team.teamType === 'red' ? '红队' : '蓝队' }}
              </el-tag>
            </el-option>
          </el-select>
          <div style="color: #8492a6; font-size: 12px; margin-top: 4px">
            提示：只能选择与用户角色匹配的战队类型
          </div>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="editForm.enabled" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="handleUpdate" :loading="updating">更新</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onErrorCaptured, reactive, markRaw, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserList, createUser, updateUser, deleteUser, toggleUserStatus } from '@/api/user'
import { getTeamList } from '@/api/team'
import axios from 'axios'

// 错误处理
const componentError = ref(null)

// 添加错误捕获
onErrorCaptured((error, instance, info) => {
  componentError.value = error
  ElMessage.error('页面加载出错，请刷新重试')
  return false
})

// 动态导入UserTable组件
const UserTable = ref(null)
const componentLoaded = ref(false)

const loadUserTableComponent = async () => {
  try {
    const module = await import('@/components/UserTable.vue')
    UserTable.value = markRaw(module.default)
    componentLoaded.value = true
  } catch (error) {
    ElMessage.error('用户表格组件加载失败，请刷新页面重试')
    throw error
  }
}

const users = ref([])
const loading = ref(false)
const activeRole = ref('red') // 默认显示红队用户

// 创建用户相关
const showCreateDialog = ref(false)
const creating = ref(false)
const createFormRef = ref()
const createForm = reactive({
  username: '',
  password: '',
  role: 'red'
})

// 编辑用户相关
const showEditDialog = ref(false)
const updating = ref(false)
const editFormRef = ref()
const editForm = reactive({
  id: null,
  username: '',
  role: '',
  teamId: null,
  enabled: true
})

// 可用战队列表
const availableTeams = ref([])

// 表单验证规则
const createRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ]
}

const editRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  role: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ]
}

// 角色显示名称映射
const getRoleDisplayName = (role) => {
  const roleMap = {
    red: '红队用户',
    blue: '蓝队用户',
    judge: '裁判用户',
    admin: '管理员用户'
  }
  return roleMap[role] || '用户'
}

// 获取战队列表（根据战队类型）
const fetchTeams = async (teamType) => {
  try {
    const response = await getTeamList({ teamType })
    availableTeams.value = Array.isArray(response) ? response : []
  } catch (error) {
    console.error('[UserManagementPage] Failed to fetch teams:', error)
    availableTeams.value = []
  }
}

// 角色变化时处理战队选择
const handleRoleChange = async (newRole) => {
  // 切换角色时清空战队选择
  editForm.teamId = null

  // 如果是红队或蓝队，加载对应类型的战队
  if (newRole === 'red' || newRole === 'blue') {
    await fetchTeams(newRole)
  } else {
    availableTeams.value = []
  }
}

// 获取用户列表
const fetchUsers = async (role = activeRole.value) => {
  loading.value = true
  try {
    const res = await getUserList({ role })
    users.value = Array.isArray(res) ? res : []
  } catch (error) {
    console.error('[UserManagementPage] Failed to fetch users:', error)
    ElMessage.error('获取用户失败')
    users.value = []
  } finally {
    loading.value = false
  }
}

// 标签页切换
const handleTabChange = (role) => {
  activeRole.value = role
  createForm.role = role
  fetchUsers(role)
}

// 创建用户
const handleCreate = async () => {
  if (!createFormRef.value) return

  try {
    await createFormRef.value.validate()
    creating.value = true

    await createUser(createForm)

    ElMessage.success('用户创建成功')
    showCreateDialog.value = false

    // 重置表单
    createForm.username = ''
    createForm.password = ''
    createForm.role = activeRole.value

    // 刷新当前标签页
    fetchUsers()
  } catch (error) {
    if (error.response?.data) {
      ElMessage.error(error.response.data)
    } else {
      ElMessage.error('创建用户失败')
    }
  } finally {
    creating.value = false
  }
}

// 编辑用户
const handleEdit = async (user) => {
  editForm.id = user.id
  editForm.username = user.username
  editForm.role = user.role
  editForm.teamId = user.teamId
  editForm.enabled = user.enabled

  // 如果用户是红队或蓝队，加载对应类型的战队列表
  if (user.role === 'red' || user.role === 'blue') {
    await fetchTeams(user.role)
  }

  showEditDialog.value = true
}

// 更新用户
const handleUpdate = async () => {
  if (!editFormRef.value) return

  try {
    await editFormRef.value.validate()
    updating.value = true

    await updateUser(editForm.id, {
      username: editForm.username,
      role: editForm.role,
      teamId: editForm.teamId,
      enabled: editForm.enabled
    })

    ElMessage.success('用户更新成功')
    showEditDialog.value = false

    // 如果角色发生变化，需要刷新所有标签页的数据
    if (editForm.role !== activeRole.value) {
      // 刷新原角色标签页
      const originalRole = activeRole.value
      fetchUsers(originalRole)

      // 切换到新角色标签页并刷新
      activeRole.value = editForm.role
      fetchUsers(editForm.role)
    } else {
      // 角色未变化，只刷新当前标签页
      fetchUsers()
    }
  } catch (error) {
    if (error.response?.data) {
      ElMessage.error(error.response.data)
    } else {
      ElMessage.error('更新用户失败')
    }
  } finally {
    updating.value = false
  }
}

// 删除用户
const handleDelete = async (user) => {
  try {
    await ElMessageBox.confirm(
      `确认删除用户 "${user.username}" 吗？`,
      '提示',
      { type: 'warning' }
    )

    await deleteUser(user.id)

    ElMessage.success('删除成功')
    fetchUsers()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 切换用户状态
const handleToggle = async (user) => {
  try {
    await toggleUserStatus(user.id)

    ElMessage.success('状态切换成功')
    fetchUsers()
  } catch {
    ElMessage.error('切换状态失败')
  }
}

onMounted(async () => {
  try {
    await loadUserTableComponent()
    createForm.role = activeRole.value
    await fetchUsers()
  } catch (error) {
    componentError.value = error
  }
})
</script>

<style scoped>
/* ============================================
   Apple Elegant White Style - 用户管理页
   User Management Page
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

.user-mgmt-content {
  background: transparent;
  padding: var(--spacing-lg);
  font-family: var(--font-apple);
  min-height: calc(100vh - 120px);
}

/* ============================================
   Error Alert
   ============================================ */
.error-alert {
  margin-bottom: var(--spacing-lg);
  border-radius: var(--radius-md);
  border: 1px solid rgba(255, 59, 48, 0.2);
  background: rgba(255, 59, 48, 0.05);
}

/* ============================================
   Main Card
   ============================================ */
:deep(.user-card) {
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

:deep(.user-card:hover) {
  box-shadow: var(--shadow-card-hover);
  transform: translateY(-2px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--apple-text);
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
}

.card-header .el-button {
  border-radius: var(--radius-sm);
  font-weight: 600;
  font-family: var(--font-apple);
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
  padding: 10px 20px;
  background: linear-gradient(135deg, var(--apple-blue) 0%, #0051d5 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.25);
}

.card-header .el-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 122, 255, 0.35);
}

/* ============================================
   Tabs Styling
   ============================================ */
.role-tabs {
  margin-top: var(--spacing-lg);
}

:deep(.el-tabs__header) {
  margin-bottom: var(--spacing-lg);
  border-bottom: 2px solid rgba(0, 0, 0, 0.06);
}

:deep(.el-tabs__nav-wrap::after) {
  display: none;
}

:deep(.el-tabs__item) {
  font-size: 16px;
  font-weight: 500;
  font-family: var(--font-apple);
  padding: 0 var(--spacing-lg);
  height: 48px;
  line-height: 48px;
  color: var(--apple-text-secondary);
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

:deep(.el-tabs__item:hover) {
  color: var(--apple-blue);
}

:deep(.el-tabs__item.is-active) {
  color: var(--apple-blue);
  font-weight: 600;
}

:deep(.el-tabs__active-bar) {
  height: 3px;
  background: linear-gradient(90deg, var(--apple-blue) 0%, #0051d5 100%);
  border-radius: 3px 3px 0 0;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
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

:deep(.el-select) {
  width: 100%;
}

:deep(.el-select .el-input__wrapper) {
  border-radius: var(--radius-sm);
}

/* ============================================
   Switch Styling
   ============================================ */
:deep(.el-switch) {
  --el-switch-on-color: var(--apple-green);
  --el-switch-off-color: var(--apple-text-secondary);
}

:deep(.el-switch__core) {
  border-radius: 20px;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

/* ============================================
   Button Styling
   ============================================ */
:deep(.el-button) {
  border-radius: var(--radius-sm);
  font-weight: 600;
  font-family: var(--font-apple);
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
  padding: 10px 20px;
}

:deep(.el-button--primary) {
  background: linear-gradient(135deg, var(--apple-blue) 0%, #0051d5 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.25);
}

:deep(.el-button--primary:hover) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 122, 255, 0.35);
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

/* ============================================
   Select Option Tags
   ============================================ */
:deep(.el-select-dropdown__item .el-tag) {
  margin: 0;
}

/* ============================================
   Skeleton Styling
   ============================================ */
:deep(.el-skeleton) {
  padding: var(--spacing-lg);
}

/* ============================================
   Card Body Padding
   ============================================ */
:deep(.el-card__body) {
  padding: var(--spacing-lg);
}

/* ============================================
   Responsive Design
   ============================================ */
@media (max-width: 1200px) {
  .user-mgmt-content {
    padding: var(--spacing-md);
  }
}

@media (max-width: 768px) {
  .user-mgmt-content {
    padding: var(--spacing-sm);
  }

  .card-header {
    flex-direction: column;
    gap: var(--spacing-md);
    align-items: stretch;
  }

  .card-header .el-button {
    width: 100%;
  }

  .card-title {
    font-size: 18px;
  }

  :deep(.el-tabs__item) {
    padding: 0 var(--spacing-md);
    font-size: 14px;
  }
}

@media (max-width: 576px) {
  .card-title {
    font-size: 16px;
  }

  :deep(.el-dialog) {
    width: 90% !important;
  }

  :deep(.el-tabs__item) {
    padding: 0 var(--spacing-sm);
    font-size: 13px;
  }
}
</style>