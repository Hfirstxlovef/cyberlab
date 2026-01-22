<template>
  <div class="asset-container" :class="themeClass">

    <el-card class="asset-card">
      <template #header>
        <div class="header-bar">
          <span>📦 资产管理</span>
          <div style="float: right">
            <el-button type="primary" @click="fetchAssets" size="small" :loading="loading">🔄 刷新</el-button>
            <el-button v-if="canExportAssets" type="success" @click="exportCSV" size="small">📥 导出 CSV</el-button>
            <el-button v-if="canManageAssets" type="primary" size="small" @click="openProjectDialog()">➕ 添加项目资产</el-button>
          </div>
        </div>
      </template>

      <!-- 筛选区域 -->
      <div class="filter-bar">
        <el-input v-model="searchKeyword" placeholder="🔍 搜索名称 / IP / 负责人" clearable class="search-input" />
        <el-select v-if="canViewAllAssets" v-model="filterVisibility" placeholder="👁️ 可见性" clearable class="filter-select" :popper-class="role === 'blue' ? 'blue-team-dropdown' : ''">
          <el-option label="全部" :value="''" />
          <el-option label="Red" value="red" />
          <el-option label="Blue" value="blue" />
          <el-option label="Both" value="both" />
        </el-select>
        <el-select v-model="filterAssetType" placeholder="📁 资产类型" clearable class="filter-select" :popper-class="role === 'blue' ? 'blue-team-dropdown' : ''">
          <el-option label="全部" :value="''" />
          <el-option label="服务器" value="server" />
          <el-option label="容器" value="container" />
          <el-option label="服务" value="service" />
          <el-option label="网络设备" value="network" />
        </el-select>
        <el-select v-model="filterTarget" placeholder="🎯 靶场" clearable class="filter-select" :popper-class="role === 'blue' ? 'blue-team-dropdown' : ''">
          <el-option label="全部" :value="''" />
          <el-option label="是" :value="true" />
          <el-option label="否" :value="false" />
        </el-select>
        <el-select v-model="filterEnabled" placeholder="⚡ 状态" clearable class="filter-select" :popper-class="role === 'blue' ? 'blue-team-dropdown' : ''">
          <el-option label="全部" :value="''" />
          <el-option label="启用" :value="true" />
          <el-option label="禁用" :value="false" />
        </el-select>
        <el-button type="primary" @click="applyFilters">🔍 查询</el-button>
      </div>

      <!-- 分组表格 -->
      <el-table :data="groupedAssets" row-key="groupKey" border>
        <el-table-column type="expand">
          <template #default="props">
            <el-table :data="props.row.servers" size="small">
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
              <el-table-column label="操作" width="200" v-if="canManageAssets">
                <template #default="scope">
                  <div class="action-buttons">
                    <el-button size="small" @click="openFormDialog(scope.row)">✏️ 编辑</el-button>
                    <el-button size="small" type="danger" @click="deleteAsset(scope.row.id)">🗑️ 删除</el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </template>
        </el-table-column>
        <el-table-column prop="groupKey" label="企业 / 项目" />
        <el-table-column label="项目管理">
          <template #default="scope">
            <el-button type="primary" @click="goToProjectDetail(scope.row.groupKey)">🚀 进入项目</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <AssetForm v-model="showDialog" :data="selectedAsset" @submit="handleSubmit" />

    <!-- 项目创建对话框 -->
    <ProjectForm v-model="showProjectDialog" @submit="handleProjectSubmit" />

  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AssetForm from '@/views/assets/AssetForm.vue'
import ProjectForm from '@/components/assets/ProjectForm.vue'
import { getUsername, getUserRole } from '@/utils/auth'
import { getAssetList, createAsset, updateAsset, deleteAsset as deleteAssetAPI, getRoleAssets } from '@/api/asset'

const router = useRouter()
const assets = ref([])
const showDialog = ref(false)
const showProjectDialog = ref(false)
const selectedAsset = ref(null)
const loading = ref(false)

const username = getUsername() || ''
const role = getUserRole() || ''
const isAdmin = computed(() => role === 'admin' || username === 'admin' || username === '管理员')

// 主题切换：根据角色动态设置主题class
const themeClass = computed(() => {
  return role === 'blue' ? 'theme-blue' : 'theme-admin'
})

// 权限控制
const canManageAssets = computed(() => {
  // 只有管理员可以管理资产（创建、编辑、删除）
  return role === 'admin'
})

const canViewAllAssets = computed(() => {
  // 管理员和裁判可以查看所有资产
  return role === 'admin' || role === 'judge'
})

const canExportAssets = computed(() => {
  // 管理员和裁判可以导出资产
  return role === 'admin' || role === 'judge'
})

const searchKeyword = ref('')
const filterVisibility = ref('')
const filterTarget = ref('')
const filterEnabled = ref('')
const filterAssetType = ref('')
const filteredAssets = ref([])

const applyFilters = () => {
  const keyword = searchKeyword.value.toLowerCase()
  filteredAssets.value = assets.value.filter(asset => {
    const matchKeyword =
      !keyword ||
      asset.name?.toLowerCase().includes(keyword) ||
      asset.ip?.toLowerCase().includes(keyword) ||
      asset.owner?.toLowerCase().includes(keyword)
    const matchVisibility = !filterVisibility.value || asset.visibility === filterVisibility.value
    const matchTarget = filterTarget.value === '' || asset.isTarget === filterTarget.value
    const matchEnabled = filterEnabled.value === '' || asset.enabled === filterEnabled.value
    const matchAssetType = !filterAssetType.value || asset.assetType === filterAssetType.value
    return matchKeyword && matchVisibility && matchTarget && matchEnabled && matchAssetType
  })
}

const groupedAssets = computed(() => {
  const map = {}

  // 先收集所有项目（包括项目占位资产），确保空项目也能显示
  for (const asset of filteredAssets.value) {
    const key = `${asset.company || '未知企业'}｜${asset.project || '未分组'}`

    // 初始化项目分组
    if (!map[key]) {
      map[key] = []
    }

    // 只添加非项目类型的资产到列表中（过滤掉项目占位资产）
    if (asset.assetType !== 'project') {
      map[key].push(asset)
    }
  }

  return Object.entries(map).map(([groupKey, servers]) => ({ groupKey, servers }))
})

const fetchAssets = async () => {
  loading.value = true
  try {
    let res
    const userRole = getUserRole()

    // 根据用户角色调用不同的API接口
    switch (userRole) {
      case 'red':
        res = await getRoleAssets('red')
        break
      case 'blue':
        res = await getRoleAssets('blue')
        break
      case 'judge':
      case 'admin':
        res = await getAssetList()
        break
      default:
        res = []
    }

    assets.value = Array.isArray(res) ? res : (res.data || [])
    applyFilters()
    ElMessage.success(`刷新成功！共 ${assets.value.length} 个资产`)
  } catch (error) {
    ElMessage.error('获取资产失败')
  } finally {
    loading.value = false
  }
}

const exportCSV = () => {
  // 使用相对路径，通过 Vite 代理访问后端
  window.open('/api/assets/export', '_blank')
}

const openFormDialog = (asset = null) => {
  selectedAsset.value = asset
  showDialog.value = true
}

const handleSubmit = async (form) => {
  try {
    if (form.id) {
      await updateAsset(form.id, form)
    } else {
      await createAsset(form)
    }
    ElMessage.success('保存成功')
    showDialog.value = false
    fetchAssets()
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
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const goToProjectDetail = (groupKey) => {
  // 导航到项目详情页面
  router.push({
    path: `/assets/project/${encodeURIComponent(groupKey)}`,
    query: {
      projectId: groupKey,
      projectName: groupKey
    }
  })
}

// 打开项目创建对话框
const openProjectDialog = () => {
  showProjectDialog.value = true
}

// 处理项目创建提交
const handleProjectSubmit = async (projectData) => {
  try {
    // 验证项目是否已存在
    const existingProject = assets.value.find(asset => {
      const assetProjectId = `${asset.company || '未知企业'}｜${asset.project || '未分组'}`
      return assetProjectId === projectData.projectId
    })

    if (existingProject) {
      ElMessage.warning(`项目 "${projectData.projectId}" 已存在`)
      return
    }

    // 创建项目占位资产
    const projectAsset = {
      name: projectData.project,
      ip: '0.0.0.0', // 项目资产使用占位IP
      company: projectData.company,
      project: projectData.project,
      owner: username,
      visibility: 'both',
      isTarget: false,
      enabled: true,
      notes: projectData.description || '项目资产（自动创建）',
      assetType: 'project', // 标记为项目类型
      topologyProjectId: projectData.projectId
    }

    await createAsset(projectAsset)
    ElMessage.success(`项目 "${projectData.projectId}" 创建成功`)
    showProjectDialog.value = false

    // 刷新资产列表
    await fetchAssets()
  } catch (error) {
    ElMessage.error('创建项目失败: ' + (error.message || '未知错误'))
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
  fetchAssets()
})
</script>

<style scoped>
/* ============================================
   Apple Elegant White Style - 资产列表组件
   以复用现有为荣: 使用全局 apple-theme.css 的 CSS 变量
   以遵循规范为荣: 保持与其他组件一致的样式
   ============================================ */

.asset-container {
  padding: 0;
  background: transparent;
  min-height: auto;
  font-family: var(--font-apple);
}

/* ============================================
   Asset Card - 主卡片
   ============================================ */
.asset-card {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.95) 0%,
    rgba(248, 248, 248, 0.85) 100%);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 0.5px solid var(--apple-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  transition: all 0.3s ease;
  overflow: hidden;
}

.asset-card :deep(.el-card__header) {
  background: var(--apple-gray-1);
  border-bottom: 0.5px solid rgba(0, 0, 0, 0.04);
  padding: var(--spacing-lg);
}

.asset-card :deep(.el-card__body) {
  padding: 10px var(--spacing-lg) var(--spacing-lg) var(--spacing-lg);
}

.header-bar {
  font-size: 18px;
  font-weight: 600;
  color: var(--apple-text-primary);
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--spacing-md);
}

.header-bar > div {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.header-bar .el-button {
  height: 40px;
  min-width: 120px;
  font-size: 14px;
  padding: 0 20px;
  border-radius: var(--radius-sm);
  font-weight: 500;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

.header-bar .el-button--primary {
  background: var(--apple-blue);
  border-color: var(--apple-blue);
}

.header-bar .el-button--primary:hover {
  background: #0051d5;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.3);
}

.header-bar .el-button--success {
  background: var(--apple-green);
  border-color: var(--apple-green);
}

.header-bar .el-button--success:hover {
  background: #28a745;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(52, 199, 89, 0.3);
}

/* ============================================
   Filter Bar - 筛选区域
   ============================================ */
.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin: 0 0 10px 0;
  padding: var(--spacing-md);
  background: rgba(245, 245, 247, 0.5);
  border-radius: var(--radius-md);
  border: 0.5px solid var(--apple-border);
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

.filter-bar .el-button {
  height: 40px;
  min-width: 100px;
  font-size: 14px;
  padding: 0 20px;
  border-radius: var(--radius-sm);
  font-weight: 500;
}

.filter-bar .el-button--primary {
  background: var(--apple-blue);
  border-color: var(--apple-blue);
}

.filter-bar .el-button--primary:hover {
  background: #0051d5;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.3);
}

/* ============================================
   Table Styling - 表格样式
   ============================================ */
.asset-card :deep(.el-table) {
  border-radius: var(--radius-md);
  overflow: hidden;
  font-family: var(--font-apple);
}

.asset-card :deep(.el-table__header-wrapper) {
  background: var(--apple-gray-1);
}

.asset-card :deep(.el-table__header th) {
  background: transparent;
  color: var(--apple-text-primary);
  font-weight: 600;
  font-size: 13px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  border-bottom: 0.5px solid rgba(0, 0, 0, 0.04);
}

.asset-card :deep(.el-table__row) {
  transition: all 0.2s ease;
}

.asset-card :deep(.el-table__row:hover) {
  background: rgba(0, 122, 255, 0.03);
}

.asset-card :deep(.el-table__row td) {
  border-bottom: 0.5px solid rgba(0, 0, 0, 0.04);
}

.asset-card :deep(.el-table__expanded-cell) {
  background: rgba(245, 245, 247, 0.5);
  padding: var(--spacing-md);
}

.asset-card :deep(.el-table__expand-icon) {
  color: var(--apple-blue);
}

.asset-card :deep(.el-button) {
  border-radius: var(--radius-sm);
  font-weight: 500;
  transition: all 0.2s ease;
}

.asset-card :deep(.el-button--small) {
  height: 32px;
  padding: 0 16px;
  font-size: 13px;
  min-width: 80px;
}

/* 默认按钮 - Apple 中性风格（编辑按钮） */
.asset-card :deep(.el-button--small:not(.el-button--primary):not(.el-button--danger):not(.el-button--success):not(.el-button--warning)) {
  background: linear-gradient(135deg,
    rgba(120, 120, 128, 0.12) 0%,
    rgba(120, 120, 128, 0.08) 100%) !important;
  color: var(--apple-text-primary) !important;
  border: 1px solid rgba(0, 0, 0, 0.08) !important;
  font-weight: 600 !important;
  letter-spacing: -0.2px !important;
}

.asset-card :deep(.el-button--small:not(.el-button--primary):not(.el-button--danger):not(.el-button--success):not(.el-button--warning):hover) {
  background: linear-gradient(135deg,
    rgba(0, 122, 255, 0.12) 0%,
    rgba(0, 122, 255, 0.08) 100%) !important;
  color: var(--apple-blue) !important;
  border-color: rgba(0, 122, 255, 0.3) !important;
  transform: translateY(-2px) !important;
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.15) !important;
}

.asset-card :deep(.el-button--primary) {
  background: var(--apple-blue);
  border-color: var(--apple-blue);
}

.asset-card :deep(.el-button--primary:hover) {
  background: #0051d5;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.3);
}

.asset-card :deep(.el-button--danger) {
  background: var(--apple-red);
  border-color: var(--apple-red);
}

.asset-card :deep(.el-button--danger:hover) {
  background: #dc143c;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255, 59, 48, 0.3);
}

/* 表格内的正常尺寸按钮（进入项目） */
.asset-card :deep(.el-table .el-button:not(.el-button--small)) {
  height: 36px;
  min-width: 110px;
  font-size: 14px;
  padding: 0 18px;
}

/* ============================================
   Action Buttons - 操作按钮容器
   ============================================ */
.action-buttons {
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: flex-start;
  flex-wrap: nowrap;
  white-space: nowrap;
}

.action-buttons .el-button {
  flex-shrink: 0;
}

/* ============================================
   Tags Styling - 标签样式
   ============================================ */
.asset-card :deep(.el-tag) {
  border-radius: 6px;
  font-weight: 500;
  padding: 4px 10px;
  border: none;
  font-size: 12px;
}

.asset-card :deep(.el-tag--success) {
  background: linear-gradient(135deg, var(--apple-green), #28a745);
  color: white;
}

.asset-card :deep(.el-tag--info) {
  background: linear-gradient(135deg, #3B82F6, #2563EB);
  color: white;
}

.asset-card :deep(.el-tag--warning) {
  background: linear-gradient(135deg, var(--apple-orange), #D97706);
  color: white;
}

.asset-card :deep(.el-tag--danger) {
  background: linear-gradient(135deg, var(--apple-red), #DC2626);
  color: white;
}

/* ============================================
   Responsive Design - 响应式设计
   ============================================ */
@media (max-width: 768px) {
  .header-bar {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-bar > div {
    width: 100%;
    justify-content: flex-start;
  }

  .filter-bar {
    flex-direction: column;
  }

  .search-input,
  .filter-select {
    width: 100%;
  }

  .filter-bar .el-button {
    width: 100%;
  }
}

@media (max-width: 576px) {
  .asset-card :deep(.el-card__header),
  .asset-card :deep(.el-card__body) {
    padding: var(--spacing-md);
  }

  .header-bar {
    font-size: 16px;
  }
}

/* ============================================
   蓝队主题 - Blue Team Theme
   仅在蓝队用户登录时生效
   ============================================ */
.asset-container.theme-blue {
  background: transparent;
  padding: 0;
  min-height: auto;
}

.asset-container.theme-blue .asset-card {
  background: linear-gradient(135deg,
    rgba(20, 30, 50, 0.85) 0%,
    rgba(13, 26, 45, 0.95) 100%);
  backdrop-filter: blur(25px);
  -webkit-backdrop-filter: blur(25px);
  border: 1px solid rgba(70, 130, 180, 0.35);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.45),
              0 0 20px rgba(70, 130, 180, 0.08),
              inset 0 1px 1px rgba(255, 255, 255, 0.08);
}

.asset-container.theme-blue .asset-card :deep(.el-card__header) {
  background: rgba(20, 30, 50, 0.7);
  border-bottom: 1px solid rgba(70, 130, 180, 0.35);
}

.asset-container.theme-blue .header-bar {
  color: #ffffff;
}

.asset-container.theme-blue .header-bar span {
  color: #ffffff;
  text-shadow: 0 2px 8px rgba(70, 130, 180, 0.3);
}

.asset-container.theme-blue .filter-bar {
  background: rgba(20, 30, 50, 0.5);
  border: 1px solid rgba(70, 130, 180, 0.25);
  box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.2);
}

/* 蓝队表格样式 */
.asset-container.theme-blue :deep(.el-table) {
  background: linear-gradient(135deg,
    rgba(15, 25, 45, 0.95) 0%,
    rgba(20, 35, 60, 0.9) 100%);
  color: #ffffff;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
}

.asset-container.theme-blue :deep(.el-table th.el-table__cell) {
  background: linear-gradient(135deg,
    rgba(70, 130, 180, 0.35) 0%,
    rgba(70, 130, 180, 0.25) 100%) !important;
  color: #000000 !important;
  border-color: rgba(70, 130, 180, 0.4) !important;
  font-weight: 700 !important;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.1),
              0 1px 3px rgba(0, 0, 0, 0.2) !important;
}

.asset-container.theme-blue :deep(.el-table td.el-table__cell) {
  background: linear-gradient(135deg,
    rgba(20, 35, 55, 0.8) 0%,
    rgba(15, 30, 50, 0.85) 100%) !important;
  color: #ffffff !important;
  border-color: rgba(70, 130, 180, 0.4) !important;
  font-weight: 500 !important;
}

.asset-container.theme-blue :deep(.el-table tr:hover > td) {
  background: linear-gradient(135deg,
    rgba(70, 130, 180, 0.3) 0%,
    rgba(70, 130, 180, 0.25) 100%) !important;
  color: #ffffff !important;
  box-shadow: inset 0 0 10px rgba(70, 130, 180, 0.2),
              0 0 8px rgba(70, 130, 180, 0.15) !important;
}

.asset-container.theme-blue :deep(.el-table__empty-text) {
  color: rgba(255, 255, 255, 0.6);
}

.asset-container.theme-blue :deep(.el-table__expanded-cell) {
  background: linear-gradient(135deg,
    rgba(10, 20, 40, 0.95) 0%,
    rgba(13, 26, 45, 0.9) 100%);
  border-color: rgba(70, 130, 180, 0.4);
  box-shadow: inset 0 2px 8px rgba(0, 0, 0, 0.3);
}

/* 展开区域内嵌套表格样式增强 */
.asset-container.theme-blue :deep(.el-table__expanded-cell .el-table) {
  background: linear-gradient(135deg,
    rgba(13, 26, 45, 0.8) 0%,
    rgba(10, 20, 40, 0.85) 100%);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.25);
}

.asset-container.theme-blue :deep(.el-table__expanded-cell .el-table th.el-table__cell) {
  background: linear-gradient(135deg,
    rgba(70, 130, 180, 0.4) 0%,
    rgba(70, 130, 180, 0.3) 100%) !important;
  color: #000000 !important;
  font-weight: 700 !important;
  border-color: rgba(70, 130, 180, 0.5) !important;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.15) !important;
}

.asset-container.theme-blue :deep(.el-table__expanded-cell .el-table td.el-table__cell) {
  background: linear-gradient(135deg,
    rgba(20, 35, 55, 0.85) 0%,
    rgba(15, 30, 50, 0.9) 100%) !important;
  color: #ffffff !important;
  border-color: rgba(70, 130, 180, 0.4) !important;
  font-weight: 500 !important;
}

.asset-container.theme-blue :deep(.el-table__expanded-cell .el-table tr:hover > td) {
  background: linear-gradient(135deg,
    rgba(70, 130, 180, 0.35) 0%,
    rgba(70, 130, 180, 0.3) 100%) !important;
  color: #ffffff !important;
  box-shadow: inset 0 0 8px rgba(70, 130, 180, 0.25) !important;
}

.asset-container.theme-blue :deep(.el-table--border) {
  border-color: rgba(70, 130, 180, 0.4);
}

.asset-container.theme-blue :deep(.el-table--border::after),
.asset-container.theme-blue :deep(.el-table--border::before) {
  background-color: rgba(70, 130, 180, 0.4);
}

/* 蓝队按钮样式 */
.asset-container.theme-blue :deep(.el-button--primary) {
  background: linear-gradient(135deg, #4682b4 0%, #1e90ff 100%);
  border-color: rgba(70, 130, 180, 0.6);
  color: #ffffff;
  box-shadow: 0 4px 15px rgba(70, 130, 180, 0.2),
              inset 0 1px 0 rgba(255, 255, 255, 0.1);
}

.asset-container.theme-blue :deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, #5fa3d4 0%, #00d4ff 100%);
  border-color: rgba(70, 130, 180, 0.8);
  box-shadow: 0 8px 25px rgba(70, 130, 180, 0.4),
              0 0 20px rgba(30, 144, 255, 0.3),
              inset 0 1px 0 rgba(255, 255, 255, 0.15);
  transform: translateY(-2px);
}

.asset-container.theme-blue :deep(.el-button--success) {
  background: linear-gradient(135deg, #34c759 0%, #28a745 100%);
  border-color: rgba(52, 199, 89, 0.6);
  color: #ffffff;
  box-shadow: 0 4px 15px rgba(52, 199, 89, 0.2);
}

.asset-container.theme-blue :deep(.el-button--success:hover) {
  background: linear-gradient(135deg, #4caf50 0%, #66bb6a 100%);
  box-shadow: 0 8px 25px rgba(52, 199, 89, 0.4);
  transform: translateY(-2px);
}

.asset-container.theme-blue :deep(.el-button) {
  border: 1px solid rgba(70, 130, 180, 0.3);
  background: rgba(20, 30, 50, 0.6);
  color: #a8d8ea;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

.asset-container.theme-blue :deep(.el-button:hover) {
  background: rgba(70, 130, 180, 0.2);
  border-color: rgba(70, 130, 180, 0.5);
  transform: translateY(-2px);
}

/* 蓝队输入框样式 */
.asset-container.theme-blue :deep(.el-input__wrapper) {
  background: rgba(20, 30, 50, 0.6);
  border: 1px solid rgba(70, 130, 180, 0.3);
  box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.2);
}

.asset-container.theme-blue :deep(.el-input__inner) {
  color: #ffffff;
}

.asset-container.theme-blue :deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.5);
}

.asset-container.theme-blue :deep(.el-input__wrapper.is-focus) {
  border-color: rgba(70, 130, 180, 0.6);
  box-shadow: 0 0 0 2px rgba(70, 130, 180, 0.15),
              inset 0 1px 3px rgba(0, 0, 0, 0.2);
}

.asset-container.theme-blue :deep(.el-input__wrapper:hover) {
  border-color: rgba(70, 130, 180, 0.5);
}

.asset-container.theme-blue :deep(.el-input__prefix),
.asset-container.theme-blue :deep(.el-input__suffix) {
  color: rgba(255, 255, 255, 0.6);
}

/* 蓝队下拉框样式 */
.asset-container.theme-blue :deep(.el-select .el-input__wrapper) {
  background: rgba(20, 30, 50, 0.6);
  border: 1px solid rgba(70, 130, 180, 0.3);
  box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.2);
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

.asset-container.theme-blue :deep(.el-select .el-input__wrapper:hover) {
  border-color: rgba(70, 130, 180, 0.5);
  background: rgba(30, 40, 60, 0.7);
  box-shadow: 0 2px 8px rgba(70, 130, 180, 0.15),
              inset 0 1px 3px rgba(0, 0, 0, 0.2);
}

.asset-container.theme-blue :deep(.el-select .el-input__wrapper.is-focus) {
  border-color: rgba(70, 130, 180, 0.6);
  background: rgba(30, 40, 60, 0.8);
  box-shadow: 0 0 0 2px rgba(70, 130, 180, 0.15),
              inset 0 1px 3px rgba(0, 0, 0, 0.2);
}

.asset-container.theme-blue :deep(.el-select .el-input__inner) {
  color: #ffffff;
}

.asset-container.theme-blue :deep(.el-select__caret) {
  color: rgba(255, 255, 255, 0.6);
  transition: color 0.3s ease;
}

.asset-container.theme-blue :deep(.el-select:hover .el-select__caret) {
  color: #00d4ff;
}

.asset-container.theme-blue :deep(.el-select__suffix) {
  color: rgba(255, 255, 255, 0.6);
}

/* 蓝队标签样式 */
.asset-container.theme-blue :deep(.el-tag) {
  background: rgba(70, 130, 180, 0.2);
  border-color: rgba(70, 130, 180, 0.4);
  color: #a8d8ea;
}

.asset-container.theme-blue :deep(.el-tag--success) {
  background: rgba(76, 175, 80, 0.2);
  border-color: rgba(76, 175, 80, 0.4);
  color: #4caf50;
}

.asset-container.theme-blue :deep(.el-tag--danger) {
  background: rgba(244, 67, 54, 0.2);
  border-color: rgba(244, 67, 54, 0.4);
  color: #f44336;
}

.asset-container.theme-blue :deep(.el-tag--info) {
  background: rgba(70, 130, 180, 0.2);
  border-color: rgba(70, 130, 180, 0.4);
  color: #a8d8ea;
}

.asset-container.theme-blue :deep(.el-tag--warning) {
  background: rgba(255, 152, 0, 0.2);
  border-color: rgba(255, 152, 0, 0.4);
  color: #ff9800;
}
</style>

<!-- 非 scoped 样式：蓝队下拉菜单主题（因为 Element Plus 下拉菜单会 teleport 到 body） -->
<style>
/* ============================================
   Blue Team Select Dropdown Theme
   仅应用于带有 blue-team-dropdown 类名的下拉菜单
   不会影响 admin 用户
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
  font-size: 13px;
  padding: 8px 16px;
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
  cursor: not-allowed;
  background: transparent;
}

.blue-team-dropdown .el-select-dropdown__item.is-disabled:hover {
  background: transparent;
}

.blue-team-dropdown .el-select-dropdown__empty {
  color: rgba(255, 255, 255, 0.5);
  padding: 16px;
  text-align: center;
}

/* 滚动条样式（如果选项过多） */
.blue-team-dropdown .el-scrollbar__wrap {
  background: transparent;
}

.blue-team-dropdown .el-scrollbar__bar {
  opacity: 0.6;
}

.blue-team-dropdown .el-scrollbar__thumb {
  background: rgba(70, 130, 180, 0.4);
}

.blue-team-dropdown .el-scrollbar__thumb:hover {
  background: rgba(70, 130, 180, 0.6);
}
</style>
