<template>
  <el-table
    :data="users"
    :loading="loading"
    style="width: 100%"
    empty-text="暂无用户数据"
    :default-sort="{prop: 'id', order: 'ascending'}"
    :header-cell-style="{ background: 'transparent', color: '#1d1d1f', fontWeight: '600', borderBottom: '1px solid rgba(0,0,0,0.06)' }"
    :row-style="{ height: '60px' }"
    :cell-style="{ padding: '16px 12px', borderBottom: '1px solid rgba(0,0,0,0.04)' }"
  >
    <el-table-column prop="id" label="ID" width="100" sortable align="center" />
    <el-table-column prop="username" label="用户名" min-width="150" sortable show-overflow-tooltip />
    <el-table-column label="角色" width="140" sortable :sort-method="sortByRole" align="center">
      <template #default="scope">
        <el-tag :type="getRoleTagType(scope.row.role)" size="default">
          {{ getRoleDisplayName(scope.row.role) }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column label="启用状态" width="120" sortable prop="enabled" align="center">
      <template #default="scope">
        <el-tag :type="scope.row.enabled ? 'success' : 'danger'" size="default">
          {{ scope.row.enabled ? '启用' : '禁用' }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column label="操作" width="440" fixed="right" align="center">
      <template #default="scope">
        <div class="action-buttons">
          <el-button
            size="small"
            type="primary"
            @click="$emit('edit', scope.row)"
          >
            <el-icon><Edit /></el-icon>
            编辑
          </el-button>
          <el-button
            size="small"
            :type="scope.row.enabled ? 'warning' : 'success'"
            @click="$emit('toggle', scope.row)"
          >
            <el-icon><Switch /></el-icon>
            {{ scope.row.enabled ? '禁用' : '启用' }}
          </el-button>
          <el-button
            size="small"
            type="danger"
            @click="$emit('delete', scope.row)"
          >
            <el-icon><Delete /></el-icon>
            删除
          </el-button>
        </div>
      </template>
    </el-table-column>
  </el-table>
</template>

<script setup>
import { Edit, Delete, Switch } from '@element-plus/icons-vue'

defineProps({
  users: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  }
})

defineEmits(['edit', 'delete', 'toggle'])

// 角色显示名称映射
const getRoleDisplayName = (role) => {
  const roleMap = {
    red: '红队',
    blue: '蓝队', 
    judge: '裁判',
    admin: '管理员'
  }
  return roleMap[role] || '未知'
}

// 角色标签类型映射
const getRoleTagType = (role) => {
  const typeMap = {
    red: 'danger',
    blue: 'primary',
    judge: 'warning', 
    admin: 'success'
  }
  return typeMap[role] || 'info'
}

// 角色排序方法
const sortByRole = (a, b) => {
  const roleOrder = { 'admin': 1, 'judge': 2, 'red': 3, 'blue': 4 }
  return (roleOrder[a.role] || 999) - (roleOrder[b.role] || 999)
}
</script>

<style scoped>
/* ============================================
   Apple Elegant White Style - 用户表格组件
   User Table Component
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
  --spacing-xs: 8px;
  --spacing-sm: 12px;
  --spacing-md: 16px;
  --font-apple: -apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", "Helvetica Neue", sans-serif;
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

/* ============================================
   Tags and Badges
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
   Action Buttons
   ============================================ */
.action-buttons {
  display: flex;
  gap: var(--spacing-xs);
  justify-content: center;
  flex-wrap: wrap;
}

.action-buttons .el-button {
  margin: 0;
  min-width: 70px;
  border-radius: var(--radius-sm);
  font-weight: 600;
  font-size: 13px;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

.action-buttons .el-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.action-buttons .el-button--primary {
  background: linear-gradient(135deg, var(--apple-blue) 0%, #0051d5 100%);
  border: none;
  box-shadow: 0 2px 8px rgba(0, 122, 255, 0.2);
}

.action-buttons .el-button--primary:hover {
  box-shadow: 0 6px 16px rgba(0, 122, 255, 0.3);
}

.action-buttons .el-button--success {
  background: linear-gradient(135deg, var(--apple-green) 0%, #28a745 100%);
  border: none;
  box-shadow: 0 2px 8px rgba(52, 199, 89, 0.2);
}

.action-buttons .el-button--success:hover {
  box-shadow: 0 6px 16px rgba(52, 199, 89, 0.3);
}

.action-buttons .el-button--warning {
  background: linear-gradient(135deg, var(--apple-orange) 0%, #e68900 100%);
  border: none;
  box-shadow: 0 2px 8px rgba(255, 149, 0, 0.2);
}

.action-buttons .el-button--warning:hover {
  box-shadow: 0 6px 16px rgba(255, 149, 0, 0.3);
}

.action-buttons .el-button--danger {
  background: linear-gradient(135deg, var(--apple-red) 0%, #d32f2f 100%);
  border: none;
  box-shadow: 0 2px 8px rgba(255, 59, 48, 0.2);
}

.action-buttons .el-button--danger:hover {
  box-shadow: 0 6px 16px rgba(255, 59, 48, 0.3);
}

/* ============================================
   Responsive Design
   ============================================ */
@media (max-width: 768px) {
  .action-buttons {
    flex-direction: column;
    gap: var(--spacing-xs);
  }

  .action-buttons .el-button {
    width: 100%;
  }
}
</style>