<template>
  <div class="project-asset-list" :class="themeClass">
    <div class="list-header">
      <h3>📦 项目资产列表</h3>
      <div class="header-actions">
        <el-button @click="fetchAssets(true)" :loading="loading">🔄 刷新</el-button>
        <el-button type="success" @click="exportProjectCSV">📥 导出 CSV</el-button>
        <el-button v-if="isAdmin" type="primary" @click="openFormDialog()">➕ 添加资产</el-button>
      </div>
    </div>

    <!-- 筛选区域 -->
    <div class="filter-bar">
      <el-input v-model="searchKeyword" placeholder="🔍 搜索名称 / IP / 负责人" clearable class="search-input" />
      <el-select v-model="filterAssetType" placeholder="📁 资产类型" clearable class="filter-select" :popper-class="role === 'blue' ? 'blue-team-dropdown' : ''">
        <el-option label="全部" :value="''" />
        <el-option label="服务器" value="server" />
        <el-option label="容器" value="container" />
        <el-option label="服务" value="service" />
        <el-option label="网络设备" value="network" />
      </el-select>
      <el-select v-model="filterEnabled" placeholder="⚡ 状态" clearable class="filter-select" :popper-class="role === 'blue' ? 'blue-team-dropdown' : ''">
        <el-option label="全部" :value="''" />
        <el-option label="启用" :value="true" />
        <el-option label="禁用" :value="false" />
      </el-select>
      <el-button type="primary" @click="applyFilters">🔍 查询</el-button>
    </div>

    <!-- 资产表格 -->
    <el-table :data="filteredAssets" class="assets-table">
      <el-table-column prop="id" label="ID" width="50" />
      <el-table-column prop="name" label="名称" />
      <el-table-column label="类型" width="80">
        <template #default="scope">
          <el-tag :type="getAssetTypeTagType(scope.row.assetType)" size="small">
            {{ getAssetTypeLabel(scope.row.assetType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="ip" label="IP / 域名" />
      <el-table-column prop="owner" label="负责人" />
      <el-table-column prop="visibility" label="可见性" />
      <el-table-column label="靶场">
        <template #default="scope">
          <el-tag :type="scope.row.isTarget ? 'success' : 'info'">
            {{ scope.row.isTarget ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态">
        <template #default="scope">
          <el-tag :type="scope.row.enabled ? 'success' : 'danger'">
            {{ scope.row.enabled ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="notes" label="备注" />
      <el-table-column label="操作" width="200" v-if="isAdmin">
        <template #default="scope">
          <el-button size="small" @click="openFormDialog(scope.row)">✏️ 编辑</el-button>
          <el-button size="small" type="danger" @click="deleteAsset(scope.row.id)">🗑️ 删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 资产表单对话框 -->
    <AssetForm v-model="showDialog" :data="selectedAsset" @submit="handleSubmit" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, defineEmits } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AssetForm from '@/views/assets/AssetForm.vue'
import { getUsername, getUserRole } from '@/utils/auth'
import { getAssetList, createAsset, updateAsset, deleteAsset as deleteAssetAPI } from '@/api/asset'

const props = defineProps({
  projectId: {
    type: String,
    required: true
  },
  projectName: {
    type: String,
    required: true
  }
})

const emit = defineEmits(['asset-updated'])

const assets = ref([])
const showDialog = ref(false)
const selectedAsset = ref(null)
const loading = ref(false)

const username = getUsername() || ''
const role = getUserRole() || ''
const isAdmin = computed(() => role === 'admin' || username === 'admin' || username === '管理员')
const themeClass = computed(() => role === 'blue' ? 'theme-blue' : 'theme-admin')

const searchKeyword = ref('')
const filterAssetType = ref('')
const filterEnabled = ref('')
const filteredAssets = ref([])

const applyFilters = () => {
  const keyword = searchKeyword.value.toLowerCase()
  filteredAssets.value = assets.value.filter(asset => {
    const matchKeyword =
      !keyword ||
      asset.name?.toLowerCase().includes(keyword) ||
      asset.ip?.toLowerCase().includes(keyword) ||
      asset.owner?.toLowerCase().includes(keyword)
    const matchAssetType = !filterAssetType.value || asset.assetType === filterAssetType.value
    const matchEnabled = filterEnabled.value === '' || asset.enabled === filterEnabled.value
    return matchKeyword && matchAssetType && matchEnabled
  })
}

// 从缓存加载资产
const loadCachedAssets = () => {
  const cacheKey = `project_assets_${props.projectId}`
  const cached = localStorage.getItem(cacheKey)
  if (cached) {
    try {
      const { data } = JSON.parse(cached)
      assets.value = data || []
      applyFilters()
    } catch (error) {
      console.error('加载缓存失败:', error)
    }
  }
}

const fetchAssets = async (showMessage = true) => {
  loading.value = true
  try {
    const res = await getAssetList()
    const allAssets = Array.isArray(res) ? res : (res.data || [])

    // 按项目ID筛选资产
    assets.value = allAssets.filter(asset => {
      const assetProjectId = `${asset.company || '未知企业'}｜${asset.project || '未分组'}`
      return assetProjectId === props.projectId
    })

    // 保存到缓存
    const cacheKey = `project_assets_${props.projectId}`
    localStorage.setItem(cacheKey, JSON.stringify({
      data: assets.value,
      timestamp: Date.now()
    }))

    applyFilters()

    // 只在 showMessage 为 true 时显示提示
    if (showMessage) {
      ElMessage.success(`刷新成功！共 ${assets.value.length} 个资产`)
    }
  } catch (error) {
    ElMessage.error('获取项目资产失败')
  } finally {
    loading.value = false
  }
}

const exportProjectCSV = () => {
  // 使用相对路径，通过 Vite 代理访问后端
  window.open(`/api/assets/export?projectId=${encodeURIComponent(props.projectId)}`, '_blank')
}

const openFormDialog = (asset = null) => {
  selectedAsset.value = asset ? { ...asset } : {
    id: null,
    company: props.projectId.split('｜')[0] || '未知企业',
    project: props.projectId.split('｜')[1] || '未分组'
  }
  showDialog.value = true
}

const handleSubmit = async (form) => {
  try {
    // 确保新资产关联到当前项目
    const projectParts = props.projectId.split('｜')
    form.company = projectParts[0] || '未知企业'
    form.project = projectParts[1] || '未分组'

    if (form.id) {
      await updateAsset(form.id, form)
    } else {
      await createAsset(form)
    }
    ElMessage.success('保存成功')
    showDialog.value = false
    fetchAssets()
    emit('asset-updated')
  } catch (error) {
    ElMessage.error('提交失败')
  }
}

const deleteAsset = async (id) => {
  try {
    await ElMessageBox.confirm('确认删除该资产？', '警告', { type: 'warning' })
    await deleteAssetAPI(id)
    ElMessage.success('删除成功')
    fetchAssets()
    emit('asset-updated')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 资产类型相关方法
const getAssetTypeLabel = (assetType) => {
  const typeMap = {
    server: '服务器',
    container: '容器',
    service: '服务',
    network: '网络设备'
  }
  return typeMap[assetType] || '未知'
}

const getAssetTypeTagType = (assetType) => {
  const typeMap = {
    server: 'info',
    container: 'success',
    service: 'warning',
    network: 'danger'
  }
  return typeMap[assetType] || 'info'
}


onMounted(() => {
  fetchAssets(false)  // 静默加载，不显示提示
})
</script>

<style scoped>
/* ============================================
   Apple Elegant White Style - 苹果高雅白风格
   ============================================ */

/* CSS Variables */
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
  --shadow-card: 0 4px 16px rgba(0, 0, 0, 0.06);
  --shadow-card-hover: 0 8px 32px rgba(0, 0, 0, 0.12);
  --spacing-md: 16px;
  --spacing-lg: 24px;
  --font-apple: -apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", "Helvetica Neue", sans-serif;
}

.project-asset-list {
  background: transparent;
  border-radius: 0;
  padding: 0;
  font-family: var(--font-apple);
}

/* ============================================
   Header Styling - 标题区域
   ============================================ */
.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-lg);
}

.list-header h3 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: var(--apple-text);
  letter-spacing: -0.5px;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.header-actions :deep(.el-button) {
  height: 40px;
  min-width: 110px;
  font-size: 14px;
  padding: 0 20px;
  border-radius: var(--radius-sm);
  font-weight: 500;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

.header-actions :deep(.el-button:hover) {
  transform: translateY(-2px);
}

.header-actions :deep(.el-button--primary) {
  background: var(--apple-blue);
  border-color: var(--apple-blue);
}

.header-actions :deep(.el-button--primary:hover) {
  background: #0051d5;
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.3);
}

.header-actions :deep(.el-button--success) {
  background: var(--apple-green);
  border-color: var(--apple-green);
}

.header-actions :deep(.el-button--success:hover) {
  background: #28a745;
  box-shadow: 0 4px 12px rgba(52, 199, 89, 0.3);
}

/* ============================================
   Filter Bar - 筛选栏
   ============================================ */
.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin: 10px 0;
  padding: var(--spacing-md);
  background: linear-gradient(135deg,
    rgba(245, 245, 247, 0.6) 0%,
    rgba(255, 255, 255, 0.4) 100%);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 0.5px solid var(--apple-border);
  border-radius: var(--radius-md);
}

.search-input {
  width: 250px;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: var(--radius-sm);
  transition: all 0.3s ease;
}

.search-input :deep(.el-input__wrapper:hover) {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.search-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 3px rgba(0, 122, 255, 0.1);
}

.filter-select {
  width: 120px;
}

.filter-select :deep(.el-input__wrapper) {
  border-radius: var(--radius-sm);
  transition: all 0.3s ease;
}

.filter-bar :deep(.el-button) {
  height: 40px;
  min-width: 100px;
  font-size: 14px;
  padding: 0 20px;
  border-radius: var(--radius-sm);
  font-weight: 500;
}

.filter-bar :deep(.el-button--primary) {
  background: var(--apple-blue);
  border-color: var(--apple-blue);
}

.filter-bar :deep(.el-button--primary:hover) {
  background: #0051d5;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.3);
}

/* ============================================
   Table Styling - 表格样式
   ============================================ */
.assets-table {
  margin-top: var(--spacing-md);
}

.assets-table :deep(.el-table) {
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: var(--radius-md);
  overflow: hidden;
  font-family: var(--font-apple);
}

.assets-table :deep(.el-table__header-wrapper) {
  background: var(--apple-gray);
}

.assets-table :deep(.el-table__header th) {
  background: transparent;
  color: var(--apple-text);
  font-weight: 600;
  font-size: 13px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
  border-right: 1px solid rgba(0, 0, 0, 0.08);
}

.assets-table :deep(.el-table__header th:last-child) {
  border-right: none;
}

.assets-table :deep(.el-table__row) {
  transition: all 0.2s ease;
}

.assets-table :deep(.el-table__row:hover) {
  background: rgba(0, 122, 255, 0.03);
}

.assets-table :deep(.el-table__row td) {
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
  border-right: 1px solid rgba(0, 0, 0, 0.08);
}

.assets-table :deep(.el-table__row td:last-child) {
  border-right: none;
}

.assets-table :deep(.el-table__body-wrapper) {
  background: white;
}

/* ============================================
   Buttons in Table - 表格内按钮
   ============================================ */
.assets-table :deep(.el-button) {
  border-radius: var(--radius-sm);
  font-weight: 500;
  transition: all 0.2s ease;
}

.assets-table :deep(.el-button--small) {
  height: 32px;
  padding: 0 16px;
  font-size: 13px;
  min-width: 70px;
}

.assets-table :deep(.el-button:hover) {
  transform: translateY(-1px);
}

.assets-table :deep(.el-button--danger) {
  background: var(--apple-red);
  border-color: var(--apple-red);
}

.assets-table :deep(.el-button--danger:hover) {
  background: #dc143c;
  box-shadow: 0 4px 12px rgba(255, 59, 48, 0.3);
}

/* ============================================
   Tags Styling - 标签样式
   ============================================ */
.assets-table :deep(.el-tag) {
  border-radius: 6px;
  font-weight: 500;
  padding: 4px 10px;
  border: none;
  font-size: 12px;
}

.assets-table :deep(.el-tag--success) {
  background: linear-gradient(135deg, var(--apple-green), #28a745);
  color: white;
}

.assets-table :deep(.el-tag--info) {
  background: linear-gradient(135deg, #3B82F6, #2563EB);
  color: white;
}

.assets-table :deep(.el-tag--warning) {
  background: linear-gradient(135deg, var(--apple-orange), #D97706);
  color: white;
}

.assets-table :deep(.el-tag--danger) {
  background: linear-gradient(135deg, var(--apple-red), #DC2626);
  color: white;
}

/* ============================================
   Responsive Design - 响应式设计
   ============================================ */
@media (max-width: 768px) {
  .list-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .header-actions {
    width: 100%;
    flex-direction: column;
  }

  .header-actions :deep(.el-button) {
    width: 100%;
  }

  .filter-bar {
    flex-direction: column;
  }

  .search-input,
  .filter-select {
    width: 100%;
  }

  .filter-bar :deep(.el-button) {
    width: 100%;
  }
}

/* ============================================
   蓝队主题 - Blue Team Theme
   ============================================ */
.project-asset-list.theme-blue .list-header h3 {
  color: #ffffff !important;
  font-weight: 700 !important;
}

/* 筛选栏样式 */
.project-asset-list.theme-blue .filter-bar {
  background: rgba(20, 30, 50, 0.5) !important;
  border: 1px solid rgba(70, 130, 180, 0.25) !important;
  box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.2) !important;
}

/* 蓝队表格样式 */
.project-asset-list.theme-blue :deep(.el-table) {
  background: linear-gradient(135deg,
    rgba(15, 25, 45, 0.95) 0%,
    rgba(20, 35, 60, 0.9) 100%) !important;
  color: #ffffff !important;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3) !important;
}

.project-asset-list.theme-blue :deep(.el-table th.el-table__cell) {
  background: linear-gradient(135deg,
    rgba(70, 130, 180, 0.35) 0%,
    rgba(70, 130, 180, 0.25) 100%) !important;
  color: #000000 !important;
  border-color: rgba(70, 130, 180, 0.4) !important;
  font-weight: 700 !important;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.1),
              0 1px 3px rgba(0, 0, 0, 0.2) !important;
}

.project-asset-list.theme-blue :deep(.el-table td.el-table__cell) {
  background: linear-gradient(135deg,
    rgba(20, 35, 55, 0.8) 0%,
    rgba(15, 30, 50, 0.85) 100%) !important;
  color: #ffffff !important;
  border-color: rgba(70, 130, 180, 0.4) !important;
  font-weight: 500 !important;
}

.project-asset-list.theme-blue :deep(.el-table tr:hover > td) {
  background: linear-gradient(135deg,
    rgba(70, 130, 180, 0.3) 0%,
    rgba(70, 130, 180, 0.25) 100%) !important;
  color: #ffffff !important;
  box-shadow: inset 0 0 10px rgba(70, 130, 180, 0.2),
              0 0 8px rgba(70, 130, 180, 0.15) !important;
}

.project-asset-list.theme-blue :deep(.el-table__empty-text) {
  color: rgba(255, 255, 255, 0.6) !important;
}

/* 蓝队按钮样式 */
.project-asset-list.theme-blue :deep(.el-button--primary) {
  background: linear-gradient(135deg, #4682b4 0%, #1e90ff 100%) !important;
  border-color: rgba(70, 130, 180, 0.6) !important;
  color: #ffffff !important;
  box-shadow: 0 4px 15px rgba(70, 130, 180, 0.2),
              inset 0 1px 0 rgba(255, 255, 255, 0.1) !important;
}

.project-asset-list.theme-blue :deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, #5fa3d4 0%, #00d4ff 100%) !important;
  border-color: rgba(70, 130, 180, 0.8) !important;
  box-shadow: 0 8px 25px rgba(70, 130, 180, 0.4),
              0 0 20px rgba(30, 144, 255, 0.3),
              inset 0 1px 0 rgba(255, 255, 255, 0.15) !important;
  transform: translateY(-2px) !important;
}

.project-asset-list.theme-blue :deep(.el-button--success) {
  background: linear-gradient(135deg, #34c759 0%, #28a745 100%) !important;
  border-color: rgba(52, 199, 89, 0.6) !important;
  color: #ffffff !important;
  box-shadow: 0 4px 15px rgba(52, 199, 89, 0.2) !important;
}

.project-asset-list.theme-blue :deep(.el-button--success:hover) {
  background: linear-gradient(135deg, #4caf50 0%, #66bb6a 100%) !important;
  box-shadow: 0 8px 25px rgba(52, 199, 89, 0.4) !important;
  transform: translateY(-2px) !important;
}

.project-asset-list.theme-blue :deep(.el-button) {
  border: 1px solid rgba(70, 130, 180, 0.3) !important;
  background: rgba(20, 30, 50, 0.6) !important;
  color: #a8d8ea !important;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1) !important;
}

.project-asset-list.theme-blue :deep(.el-button:hover) {
  background: rgba(70, 130, 180, 0.2) !important;
  border-color: rgba(70, 130, 180, 0.5) !important;
  transform: translateY(-2px) !important;
}

.project-asset-list.theme-blue :deep(.el-button--danger) {
  background: linear-gradient(135deg, #ff3b30 0%, #dc143c 100%) !important;
  border-color: rgba(255, 59, 48, 0.6) !important;
  color: #ffffff !important;
}

.project-asset-list.theme-blue :deep(.el-button--danger:hover) {
  background: linear-gradient(135deg, #ff6b61 0%, #ff4757 100%) !important;
  box-shadow: 0 8px 25px rgba(255, 59, 48, 0.4) !important;
  transform: translateY(-2px) !important;
}

/* 蓝队输入框样式 */
.project-asset-list.theme-blue :deep(.el-input__wrapper) {
  background: rgba(20, 30, 50, 0.6) !important;
  border: 1px solid rgba(70, 130, 180, 0.3) !important;
  box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.2) !important;
}

.project-asset-list.theme-blue :deep(.el-input__inner) {
  color: #ffffff !important;
}

.project-asset-list.theme-blue :deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.5) !important;
}

.project-asset-list.theme-blue :deep(.el-input__wrapper.is-focus) {
  border-color: rgba(70, 130, 180, 0.6) !important;
  box-shadow: 0 0 0 2px rgba(70, 130, 180, 0.15),
              inset 0 1px 3px rgba(0, 0, 0, 0.2) !important;
}

.project-asset-list.theme-blue :deep(.el-input__wrapper:hover) {
  border-color: rgba(70, 130, 180, 0.5) !important;
}

.project-asset-list.theme-blue :deep(.el-input__prefix),
.project-asset-list.theme-blue :deep(.el-input__suffix) {
  color: rgba(255, 255, 255, 0.6) !important;
}

/* 蓝队下拉框样式 */
.project-asset-list.theme-blue :deep(.el-select .el-input__wrapper) {
  background: rgba(20, 30, 50, 0.6) !important;
  border: 1px solid rgba(70, 130, 180, 0.3) !important;
  box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.2) !important;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1) !important;
}

.project-asset-list.theme-blue :deep(.el-select .el-input__wrapper:hover) {
  border-color: rgba(70, 130, 180, 0.5) !important;
  background: rgba(30, 40, 60, 0.7) !important;
  box-shadow: 0 2px 8px rgba(70, 130, 180, 0.15),
              inset 0 1px 3px rgba(0, 0, 0, 0.2) !important;
}

.project-asset-list.theme-blue :deep(.el-select .el-input__wrapper.is-focus) {
  border-color: rgba(70, 130, 180, 0.6) !important;
  background: rgba(30, 40, 60, 0.8) !important;
  box-shadow: 0 0 0 2px rgba(70, 130, 180, 0.15),
              inset 0 1px 3px rgba(0, 0, 0, 0.2) !important;
}

.project-asset-list.theme-blue :deep(.el-select .el-input__inner) {
  color: #ffffff !important;
}

.project-asset-list.theme-blue :deep(.el-select .el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.5) !important;
}

/* 蓝队标签样式 */
.project-asset-list.theme-blue :deep(.el-tag) {
  border: none !important;
  color: #ffffff !important;
}

.project-asset-list.theme-blue :deep(.el-tag--success) {
  background: linear-gradient(135deg, #34c759, #28a745) !important;
}

.project-asset-list.theme-blue :deep(.el-tag--info) {
  background: linear-gradient(135deg, #3B82F6, #2563EB) !important;
}

.project-asset-list.theme-blue :deep(.el-tag--warning) {
  background: linear-gradient(135deg, #ff9500, #D97706) !important;
}

.project-asset-list.theme-blue :deep(.el-tag--danger) {
  background: linear-gradient(135deg, #ff3b30, #DC2626) !important;
}
</style>

<style>
/* ============================================
   蓝队下拉菜单样式 - 非scoped，适用于teleport到body的下拉框
   ============================================ */
.blue-team-dropdown.el-select-dropdown {
  background: rgba(20, 30, 50, 0.95);
  border: 1px solid rgba(70, 130, 180, 0.4);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5),
              0 0 20px rgba(70, 130, 180, 0.15);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
}

.blue-team-dropdown .el-select-dropdown__item {
  color: rgba(255, 255, 255, 0.9);
  background: transparent;
  transition: all 0.2s ease;
}

.blue-team-dropdown .el-select-dropdown__item:hover {
  background: rgba(70, 130, 180, 0.25);
  color: #00d4ff;
}

.blue-team-dropdown .el-select-dropdown__item.selected {
  background: rgba(70, 130, 180, 0.35);
  color: #00d4ff;
  font-weight: 600;
}

.blue-team-dropdown .el-select-dropdown__item.is-disabled {
  color: rgba(255, 255, 255, 0.3);
}

.blue-team-dropdown .el-popper__arrow::before {
  background: rgba(20, 30, 50, 0.95);
  border: 1px solid rgba(70, 130, 180, 0.4);
}
</style>
