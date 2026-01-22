<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from '@/api/axios'
import { getUserRole } from '@/utils/auth'

const router = useRouter()

const filters = ref({
  username: '',
  operation: '',
  businessModule: '',
  operationType: '',
  operationStatus: '',
  description: '',
  ip: '',
  timeRange: []
})

const businessModules = [
  '容器管理', '用户管理', '成果管理', '场景管理', '日志管理',
  '系统配置', '大屏展示', '主机节点管理', '资产管理', '认证授权',
  '队伍管理', '权限管理', '角色管理', '文件管理', '监控告警',
  '演练管理', '录屏管理', '网络拓扑', '数据备份', '通知消息', '其他'
]

const operationTypes = [
  '创建', '查询', '更新', '删除', '执行', '批量操作',
  '导出', '导入', '审计', '审核通过', '驳回', '配置', '登录', '登出'
]

const operationStatuses = [
  { label: '成功', value: 'SUCCESS' },
  { label: '失败', value: 'FAILED' },
  { label: '处理中', value: 'PENDING' }
]

const logs = ref([])
const totalCount = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const sortProp = ref('timestamp')
const sortOrder = ref('descending')

const fetchLogs = async () => {
  try {
    const response = await axios.get('/logs/page', {
      params: {
        username: filters.value.username,
        operation: filters.value.operation,
        businessModule: filters.value.businessModule,
        operationType: filters.value.operationType,
        operationStatus: filters.value.operationStatus,
        page: currentPage.value - 1,
        size: pageSize.value,
        sortField: sortProp.value || 'timestamp',
        sortOrder: sortOrder.value === 'ascending' ? 'asc' : 'desc'
      }
    })
    logs.value = response.content || []
    totalCount.value = response.totalElements || 0
  } catch {
    ElMessage.error('获取日志失败')
  }
}

const resetFilters = () => {
  filters.value = {
    username: '',
    operation: '',
    businessModule: '',
    operationType: '',
    operationStatus: '',
    description: '',
    ip: '',
    timeRange: []
  }
  fetchLogs()
}

const handleSort = ({ prop, order }) => {
  sortProp.value = prop
  sortOrder.value = order
  fetchLogs()
}
const handlePageChange = (page) => {
  currentPage.value = page
  fetchLogs()
}
const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  fetchLogs()
}

const highlightText = (text, keyword) => {
  if (!text || !keyword) return text || ''
  const escaped = keyword.replace(/[-/\\^$*+?.()|[\]{}]/g, '\\$&')
  const regex = new RegExp(`(${escaped})`, 'gi')
  return text.replace(regex, '<span class="highlight">$1</span>')
}

// 英文枚举 -> 中文映射
const moduleNameMap = {
  'CONTAINER': '容器管理',
  'USER': '用户管理',
  'ACHIEVEMENT': '成果管理',
  'DRILL': '演练管理',
  'SCENARIO': '场景管理',
  'PERMISSION': '权限管理',
  'ROLE': '角色管理',
  'TEAM': '队伍管理',
  'HOST_NODE': '主机节点管理',
  'ASSET': '资产管理',
  'SYSTEM_CONFIG': '系统配置',
  'LOG': '日志管理',
  'AUTH': '认证授权',
  'MONITOR': '监控告警',
  'BIG_SCREEN': '大屏展示',
  'FILE': '文件管理',
  'RECORDING': '录屏管理',
  'TOPOLOGY': '网络拓扑',
  'BACKUP': '数据备份',
  'NOTIFICATION': '通知消息',
  'OTHER': '其他'
}

const operationNameMap = {
  'CREATE': '创建',
  'READ': '查询',
  'UPDATE': '更新',
  'DELETE': '删除',
  'EXECUTE': '执行',
  'BATCH': '批量操作',
  'EXPORT': '导出',
  'IMPORT': '导入',
  'AUDIT': '审计',
  'APPROVE': '审核通过',
  'REJECT': '驳回',
  'CONFIGURE': '配置',
  'LOGIN': '登录',
  'LOGOUT': '登出'
}

const getModuleName = (module) => {
  return moduleNameMap[module] || module || '-'
}

const getOperationName = (operation) => {
  return operationNameMap[operation] || operation || '-'
}

// ✅ 添加：IP 地址格式化函数
const formatIpAddress = (ip) => {
  if (!ip || ip === 'unknown') return '未知'

  // IPv6 本地回环地址
  if (ip === '0:0:0:0:0:0:0:1' || ip === '::1') {
    return '本地连接'
  }

  // IPv4 本地回环地址
  if (ip === '127.0.0.1') {
    return '本地连接'
  }

  // 其他地址保持原样
  return ip
}

// ✅ 添加：时间格式化函数
const formatDateTime = (datetime) => {
  if (!datetime) return '-'

  // 处理 ISO 8601 格式: 2025-10-29T15:08:05.699857
  // 移除 T 分隔符和毫秒部分
  return datetime
    .replace('T', ' ')      // T → 空格
    .split('.')[0]          // 移除毫秒
}

const getModuleTagType = (module) => {
  const chineseName = getModuleName(module)
  const moduleMap = {
    '容器管理': 'primary',
    '用户管理': 'success',
    '成果管理': 'warning',
    '场景管理': 'info',
    '日志管理': 'info',
    '系统配置': 'danger',
    '大屏展示': 'primary',
    '主机节点管理': 'warning',
    '资产管理': 'success',
    '认证授权': 'danger',
    '队伍管理': 'info',
    '权限管理': 'danger',
    '角色管理': 'warning',
    '文件管理': 'info',
    '监控告警': 'danger',
    '演练管理': 'primary',
    '录屏管理': 'success',
    '网络拓扑': 'info',
    '数据备份': 'warning',
    '通知消息': 'success'
  }
  return moduleMap[chineseName]
}

const getOperationTagType = (operation) => {
  const chineseName = getOperationName(operation)
  const operationMap = {
    '创建': 'success',
    '查询': 'info',
    '更新': 'warning',
    '删除': 'danger',
    '执行': 'primary',
    '批量操作': 'warning',
    '导出': 'info',
    '导入': 'success',
    '审计': 'info',
    '审核通过': 'success',
    '驳回': 'danger',
    '配置': 'warning',
    '登录': 'success',
    '登出': 'info'
  }
  return operationMap[chineseName]
}

const exportToCSV = () => {
  const headers = ['ID', '用户名', '业务模块', '操作类型', '状态', '详情', 'IP地址', '时间']
  const rows = logs.value.map(log => [
    log.id,
    log.username,
    getModuleName(log.businessModule),
    getOperationName(log.operationType),
    log.operationStatus === 'SUCCESS' ? '成功' : log.operationStatus === 'FAILED' ? '失败' : log.operationStatus === 'PENDING' ? '处理中' : '-',
    log.description || '',
    formatIpAddress(log.ip),  // ✅ 使用格式化函数
    formatDateTime(log.timestamp)  // ✅ 使用格式化函数
  ])
  const csvContent = headers.join(',') + '\n' + rows.map(r => r.join(',')).join('\n')
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.setAttribute('download', '系统日志.csv')
  link.click()
}

onMounted(() => {
  const role = getUserRole()
  if (!['admin', 'judge'].includes(role)) {
    ElMessage.error('您无权限访问此页面')
    router.push('/dashboard')
  } else {
    fetchLogs()
  }
})
</script>

<template>
  <div class="system-log-page">
    <el-card class="log-card">
      <template #header>
        <div class="card-header">
          <div class="card-title">📝 系统日志</div>
          <el-button type="success" @click="exportToCSV">
            导出 CSV
          </el-button>
        </div>
      </template>

      <!-- 筛选表单 -->
      <el-form :inline="true" :model="filters" class="filter-form">
        <el-form-item label="用户名">
          <el-input v-model="filters.username" placeholder="用户名" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item label="业务模块">
          <el-select v-model="filters.businessModule" placeholder="业务模块" clearable style="width: 130px">
            <el-option v-for="module in businessModules" :key="module" :label="module" :value="module" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作类型">
          <el-select v-model="filters.operationType" placeholder="操作类型" clearable style="width: 120px">
            <el-option v-for="type in operationTypes" :key="type" :label="type" :value="type" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.operationStatus" placeholder="状态" clearable style="width: 100px">
            <el-option v-for="status in operationStatuses" :key="status.value" :label="status.label" :value="status.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="详情">
          <el-input v-model="filters.description" placeholder="详情关键词" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item label="IP地址">
          <el-input v-model="filters.ip" placeholder="IP关键词" clearable style="width: 130px" />
        </el-form-item>
        <el-form-item label="时间区间">
          <el-date-picker
            v-model="filters.timeRange"
            type="datetimerange"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 350px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchLogs">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 日志表格 -->
      <el-table
        :data="logs"
        style="width: 100%"
        :header-cell-style="{ background: 'transparent', color: '#1d1d1f', fontWeight: '600', borderBottom: '1px solid rgba(0,0,0,0.06)' }"
        :row-style="{ height: '60px' }"
        :cell-style="{ padding: '16px 12px', borderBottom: '1px solid rgba(0,0,0,0.04)' }"
        empty-text="暂无日志数据"
        @sort-change="handleSort"
      >
        <el-table-column prop="id" label="ID" width="80" sortable="custom" align="center" />
        <el-table-column label="用户名" width="120" sortable="custom">
          <template #default="{ row }">
            <span v-html="highlightText(row.username, filters.username)"></span>
          </template>
        </el-table-column>
        <el-table-column label="业务模块" width="140" sortable="custom">
          <template #default="{ row }">
            <el-tag
              v-if="row.businessModule"
              :type="getModuleTagType(row.businessModule) || undefined"
              size="small"
            >
              {{ getModuleName(row.businessModule) }}
            </el-tag>
            <span v-else class="text-gray">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作类型" width="120" sortable="custom">
          <template #default="{ row }">
            <el-tag
              v-if="row.operationType"
              :type="getOperationTagType(row.operationType) || undefined"
              size="small"
            >
              {{ getOperationName(row.operationType) }}
            </el-tag>
            <span v-else class="text-gray">-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.operationStatus === 'SUCCESS' || row.operationStatus === '成功'" type="success" size="small">
              成功
            </el-tag>
            <el-tag v-else-if="row.operationStatus === 'FAILED' || row.operationStatus === '失败'" type="danger" size="small">
              失败
            </el-tag>
            <el-tag v-else-if="row.operationStatus === 'PENDING' || row.operationStatus === '处理中'" type="warning" size="small">
              处理中
            </el-tag>
            <span v-else class="text-gray">-</span>
          </template>
        </el-table-column>
        <el-table-column label="详情" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-html="highlightText(row.description, filters.description)"></span>
          </template>
        </el-table-column>
        <el-table-column label="IP地址" width="160">
          <template #default="{ row }">
            <el-tooltip :content="`原始IP: ${row.ip || '未知'}`" placement="top">
              <span v-html="highlightText(formatIpAddress(row.ip), filters.ip)"></span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="180" sortable="custom">
          <template #default="{ row }">
            <el-tooltip :content="`原始时间: ${row.timestamp || '-'}`" placement="top">
              <span>{{ formatDateTime(row.timestamp) }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-if="logs.length > 0"
        class="pagination"
        layout="total, sizes, prev, pager, next"
        :total="totalCount"
        :page-size="pageSize"
        :current-page="currentPage"
        :page-sizes="[10, 20, 50, 100]"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </el-card>
  </div>
</template>

<style scoped>
/* ============================================
   Apple Elegant White Style - 系统日志页
   System Log Page
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

.system-log-page {
  background: transparent;
  padding: var(--spacing-lg);
  font-family: var(--font-apple);
  min-height: calc(100vh - 120px);
}

/* ============================================
   Main Card
   ============================================ */
:deep(.log-card) {
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

:deep(.log-card:hover) {
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
  background: linear-gradient(135deg, var(--apple-green) 0%, #28a745 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(52, 199, 89, 0.25);
}

.card-header .el-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(52, 199, 89, 0.35);
}

/* ============================================
   Filter Form Styling
   ============================================ */
.filter-form {
  margin-bottom: var(--spacing-lg);
  padding: var(--spacing-md);
  background: rgba(0, 122, 255, 0.02);
  border-radius: var(--radius-md);
  border: 1px solid rgba(0, 122, 255, 0.08);
}

.filter-form :deep(.el-form-item__label) {
  font-weight: 600;
  color: var(--apple-text);
  font-family: var(--font-apple);
}

.filter-form :deep(.el-input__wrapper) {
  border-radius: var(--radius-sm);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
  border: 1px solid var(--apple-border);
}

.filter-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.08);
  border-color: var(--apple-blue);
}

.filter-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 4px 16px rgba(0, 122, 255, 0.15);
  border-color: var(--apple-blue);
}

.filter-form :deep(.el-date-editor) {
  border-radius: var(--radius-sm);
}

.filter-form :deep(.el-date-editor .el-input__wrapper) {
  border-radius: var(--radius-sm);
}

.filter-form :deep(.el-button) {
  border-radius: var(--radius-sm);
  font-weight: 600;
  font-family: var(--font-apple);
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
  padding: 10px 20px;
}

.filter-form :deep(.el-button--primary) {
  background: linear-gradient(135deg, var(--apple-blue) 0%, #0051d5 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.25);
}

.filter-form :deep(.el-button--primary:hover) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 122, 255, 0.35);
}

.filter-form :deep(.el-button--default) {
  background: rgba(0, 0, 0, 0.02);
  border: 1px solid var(--apple-border);
  color: var(--apple-text);
}

.filter-form :deep(.el-button--default:hover) {
  background: rgba(0, 0, 0, 0.04);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

/* ============================================
   Table Styling
   ============================================ */
:deep(.el-table) {
  background: transparent;
  border-radius: var(--radius-md);
  overflow: hidden;
  font-family: var(--font-apple);
  margin-bottom: var(--spacing-lg);
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
   Highlight Text
   ============================================ */
:deep(.highlight) {
  color: var(--apple-orange);
  font-weight: 600;
  background-color: rgba(255, 149, 0, 0.1);
  padding: 2px 6px;
  border-radius: 4px;
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

:deep(.el-pagination__sizes .el-select .el-input__wrapper) {
  border-radius: var(--radius-sm);
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
  .system-log-page {
    padding: var(--spacing-md);
  }
}

@media (max-width: 768px) {
  .system-log-page {
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

  .filter-form {
    padding: var(--spacing-sm);
  }

  .filter-form :deep(.el-form-item) {
    margin-bottom: var(--spacing-sm);
  }

  .filter-form :deep(.el-input),
  .filter-form :deep(.el-date-editor) {
    width: 100% !important;
  }

  .filter-form :deep(.el-button) {
    width: 100%;
    margin-bottom: var(--spacing-xs);
  }
}

@media (max-width: 576px) {
  .card-title {
    font-size: 16px;
  }

  :deep(.el-table) {
    font-size: 13px;
  }

  .pagination {
    text-align: center;
  }

  :deep(.el-pagination) {
    justify-content: center;
  }

  .text-gray {
    color: var(--apple-text-secondary);
  }
}
</style>