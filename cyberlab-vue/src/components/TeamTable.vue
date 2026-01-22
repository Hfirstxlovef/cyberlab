<template>
  <el-table
    :data="teams"
    :loading="loading"
    style="width: 100%"
    empty-text="暂无战队数据"
    :default-sort="{prop: 'id', order: 'ascending'}"
    :header-cell-style="{ background: 'transparent', color: '#1d1d1f', fontWeight: '600', borderBottom: '1px solid rgba(0,0,0,0.06)' }"
    :row-style="{ height: '60px' }"
    :cell-style="{ padding: '16px 12px', borderBottom: '1px solid rgba(0,0,0,0.04)' }"
  >
    <el-table-column prop="id" label="ID" width="80" sortable align="center" />
    <el-table-column prop="name" label="战队名称" min-width="180" sortable show-overflow-tooltip />
    <el-table-column label="类型" width="100" sortable prop="teamType" align="center">
      <template #default="scope">
        <el-tag :type="getTeamTypeTagType(scope.row.teamType)" size="default">
          {{ getTeamTypeDisplayName(scope.row.teamType) }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column prop="leaderName" label="队长" width="140" show-overflow-tooltip />
    <el-table-column label="成员数" width="100" align="center">
      <template #default="scope">
        <span>{{ scope.row.memberCount || 0 }}</span>
      </template>
    </el-table-column>
    <el-table-column label="状态" width="100" sortable prop="status" align="center">
      <template #default="scope">
        <el-tag :type="scope.row.status === 'active' ? 'success' : 'danger'" size="default">
          {{ scope.row.status === 'active' ? '活跃' : '已解散' }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column label="操作" width="320" fixed="right" align="center">
      <template #default="scope">
        <div class="action-buttons">
          <el-button
            size="small"
            type="primary"
            @click="$emit('view', scope.row)"
          >
            <el-icon><View /></el-icon>
            查看
          </el-button>
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
            type="warning"
            @click="$emit('delete', scope.row)"
          >
            <el-icon><Delete /></el-icon>
            解散
          </el-button>
        </div>
      </template>
    </el-table-column>
  </el-table>
</template>

<script setup>
import { Edit, Delete, View } from '@element-plus/icons-vue'

defineProps({
  teams: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  }
})

defineEmits(['view', 'edit', 'delete'])

// 战队类型显示名称映射
const getTeamTypeDisplayName = (teamType) => {
  const typeMap = {
    red: '红队',
    blue: '蓝队'
  }
  return typeMap[teamType] || '未知'
}

// 战队类型标签类型映射
const getTeamTypeTagType = (teamType) => {
  const typeMap = {
    red: 'danger',
    blue: 'primary'
  }
  return typeMap[teamType] || 'info'
}
</script>

<style scoped>
/* ============================================
   Apple Elegant White Style - 战队表格组件
   Team Table Component
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
   Button Icon Sizing
   ============================================ */
.action-buttons .el-button :deep(.el-icon) {
  font-size: 14px;
  margin-right: 4px;
}

/* ============================================
   Hover Effects Enhancement
   ============================================ */
.action-buttons .el-button:active {
  transform: translateY(0);
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
