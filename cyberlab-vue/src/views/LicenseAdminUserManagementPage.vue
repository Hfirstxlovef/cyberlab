<template>
  <div class="license-user-mgmt-page">
    <div class="page-header">
      <h2>👥 用户管理</h2>
      <p class="page-description">查看和管理系统用户信息</p>
    </div>

    <el-card class="apple-card">
      <template #header>
        <div class="card-header">
          <span class="header-icon">📋</span>
          <span class="header-title">用户列表</span>
          <el-input
            v-model="searchKeyword"
            placeholder="搜索用户名或角色"
            :prefix-icon="Search"
            clearable
            style="width: 300px;"
            @input="handleSearch" />
        </div>
      </template>

      <el-table
        :data="displayUsers"
        v-loading="loading"
        stripe
        style="width: 100%">

        <el-table-column prop="id" label="ID" width="80" />

        <el-table-column prop="username" label="用户名" min-width="150">
          <template #default="{ row }">
            <span class="username-text">{{ row.username }}</span>
          </template>
        </el-table-column>

        <el-table-column label="角色" width="150">
          <template #default="{ row }">
            <el-tag :type="getRoleTagType(row.role)" size="default">
              {{ getRoleDisplayName(row.role) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
              {{ row.enabled ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleViewDetail(row)">
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 用户详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="用户详情"
      width="500px">
      <div v-if="selectedUser" class="user-detail">
        <div class="detail-item">
          <span class="label">用户ID</span>
          <span class="value">{{ selectedUser.id }}</span>
        </div>
        <div class="detail-item">
          <span class="label">用户名</span>
          <span class="value">{{ selectedUser.username }}</span>
        </div>
        <div class="detail-item">
          <span class="label">角色</span>
          <el-tag :type="getRoleTagType(selectedUser.role)">
            {{ getRoleDisplayName(selectedUser.role) }}
          </el-tag>
        </div>
        <div class="detail-item">
          <span class="label">状态</span>
          <el-tag :type="selectedUser.enabled ? 'success' : 'info'">
            {{ selectedUser.enabled ? '启用' : '禁用' }}
          </el-tag>
        </div>
        <div class="detail-item">
          <span class="label">创建时间</span>
          <span class="value">{{ formatDate(selectedUser.createdAt) }}</span>
        </div>
      </div>

      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getUserList } from '@/api/user'

// 响应式数据
const users = ref([])
const loading = ref(false)
const searchKeyword = ref('')
const detailDialogVisible = ref(false)
const selectedUser = ref(null)

// 计算属性 - 过滤后的用户列表
const displayUsers = computed(() => {
  if (!searchKeyword.value.trim()) {
    return users.value
  }

  const keyword = searchKeyword.value.toLowerCase()
  return users.value.filter(user => {
    return (
      user.username.toLowerCase().includes(keyword) ||
      getRoleDisplayName(user.role).includes(keyword)
    )
  })
})

/**
 * 获取所有用户列表
 */
const fetchAllUsers = async () => {
  loading.value = true
  try {
    // 调用现有的 getUserList 接口，不传 role 参数获取所有用户
    const response = await getUserList({})
    if (Array.isArray(response)) {
      users.value = response
    } else if (response.data && Array.isArray(response.data)) {
      users.value = response.data
    } else {
      users.value = []
    }
  } catch (error) {
    console.error('获取用户列表失败:', error)
    ElMessage.error('获取用户列表失败')
    users.value = []
  } finally {
    loading.value = false
  }
}

/**
 * 搜索处理
 */
const handleSearch = () => {
  // 搜索由computed属性自动处理
}

/**
 * 查看用户详情
 */
const handleViewDetail = (user) => {
  selectedUser.value = user
  detailDialogVisible.value = true
}

/**
 * 获取角色显示名称
 */
const getRoleDisplayName = (role) => {
  const roleMap = {
    red: '红队',
    blue: '蓝队',
    judge: '裁判',
    admin: '管理员',
    license_admin: '红岸授权员'
  }
  return roleMap[role] || '未知角色'
}

/**
 * 获取角色标签类型
 */
const getRoleTagType = (role) => {
  const typeMap = {
    red: 'danger',
    blue: 'primary',
    judge: 'warning',
    admin: 'success',
    license_admin: 'warning'
  }
  return typeMap[role] || 'info'
}

/**
 * 格式化日期
 */
const formatDate = (dateString) => {
  if (!dateString) return '-'
  const date = new Date(dateString)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 页面加载时获取数据
onMounted(() => {
  fetchAllUsers()
})
</script>

<style scoped>
.license-user-mgmt-page {
  max-width: 1400px;
  margin: 0 auto;
}

/* 页面头部 */
.page-header {
  margin-bottom: var(--spacing-lg, 32px);
}

.page-header h2 {
  margin: 0 0 8px 0;
  font-size: var(--font-3xl, 32px);
  font-weight: var(--font-weight-bold, 700);
  color: var(--apple-text-primary, #1d1d1f);
}

.page-description {
  margin: 0;
  font-size: var(--font-md, 16px);
  color: var(--apple-text-secondary, #6e6e73);
}

/* Apple 风格卡片 */
.apple-card {
  margin-bottom: var(--spacing-lg, 32px);
  border-radius: var(--radius-xl, 20px);
  border: 0.5px solid rgba(0, 0, 0, 0.04);
  box-shadow: var(--shadow-card,
    0 2px 8px rgba(0, 0, 0, 0.04),
    0 8px 32px rgba(0, 0, 0, 0.03));
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}

.apple-card:hover {
  box-shadow: var(--shadow-card-hover,
    0 8px 24px rgba(0, 0, 0, 0.08),
    0 16px 48px rgba(0, 0, 0, 0.06));
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-icon {
  font-size: 24px;
}

.header-title {
  flex: 1;
  font-size: var(--font-lg, 18px);
  font-weight: var(--font-weight-semibold, 600);
  color: var(--apple-text-primary, #1d1d1f);
}

/* 用户名样式 */
.username-text {
  font-weight: var(--font-weight-medium, 500);
  color: var(--apple-text-primary, #1d1d1f);
}

/* 用户详情 */
.user-detail {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md, 24px);
}

.detail-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-sm, 16px);
  background: rgba(255, 255, 255, 0.6);
  border-radius: var(--radius-md, 12px);
  border: 0.5px solid rgba(0, 0, 0, 0.04);
}

.detail-item .label {
  font-size: var(--font-sm, 14px);
  font-weight: var(--font-weight-medium, 500);
  color: var(--apple-text-secondary, #6e6e73);
}

.detail-item .value {
  font-size: var(--font-md, 16px);
  font-weight: var(--font-weight-semibold, 600);
  color: var(--apple-text-primary, #1d1d1f);
}

/* 响应式 */
@media (max-width: 768px) {
  .license-user-mgmt-page {
    padding: var(--spacing-sm, 16px);
  }

  .detail-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}
</style>
