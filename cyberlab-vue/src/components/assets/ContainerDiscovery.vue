<template>
  <div class="container-discovery">
    <div class="discovery-header">
      <h3>容器发现与导入</h3>
      <p class="description">从主机节点发现现有容器并将其导入为资产</p>
    </div>

    <!-- 节点选择区域 -->
    <el-card class="node-selection-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>选择节点</span>
          <el-button @click="loadNodes" size="small" :loading="loadingNodes">
            <el-icon><Refresh /></el-icon>
            刷新节点
          </el-button>
        </div>
      </template>

      <div class="nodes-grid" v-loading="loadingNodes">
        <div 
          v-for="node in availableNodes" 
          :key="node.id"
          class="node-card"
          :class="{ 'selected': selectedNodeId === node.id }"
          @click="selectNode(node)"
        >
          <div class="node-header">
            <div class="node-name">{{ node.displayName }}</div>
            <el-tag :type="getNodeStatusType(node.status)" size="small">
              {{ getNodeStatusLabel(node.status) }}
            </el-tag>
          </div>
          <div class="node-info">
            <div class="info-item">
              <span class="label">地址:</span>
              <span class="value">{{ node.hostIp }}:{{ node.dockerPort }}</span>
            </div>
            <div class="info-item">
              <span class="label">环境:</span>
              <span class="value">{{ getEnvironmentLabel(node.environment) }}</span>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 容器发现区域 -->
    <el-card class="discovery-card" shadow="never" v-if="selectedNodeId">
      <template #header>
        <div class="card-header">
          <span>发现的容器</span>
          <div>
            <el-button @click="discoverContainers" size="small" :loading="discovering">
              <el-icon><Search /></el-icon>
              扫描容器
            </el-button>
            <el-button 
              @click="importSelectedContainers" 
              type="primary" 
              size="small" 
              :disabled="selectedContainers.length === 0"
              :loading="importing"
            >
              <el-icon><Download /></el-icon>
              导入选中 ({{ selectedContainers.length }})
            </el-button>
          </div>
        </div>
      </template>

      <!-- 导入配置 -->
      <div class="import-config" v-if="discoveredContainers.length > 0">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="所属企业">
              <el-input v-model="importConfig.company" placeholder="请输入企业名称" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="项目分组">
              <el-input v-model="importConfig.project" placeholder="请输入项目名称" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="批量操作">
              <el-checkbox v-model="selectAll" @change="handleSelectAll">全选</el-checkbox>
              <el-checkbox v-model="onlySuitable" @change="filterContainers">仅显示适合的容器</el-checkbox>
            </el-form-item>
          </el-col>
        </el-row>
      </div>

      <!-- 容器列表 -->
      <div class="containers-list" v-loading="discovering">
        <el-empty v-if="!discovering && displayedContainers.length === 0" description="未发现容器" />
        
        <div v-else class="containers-grid">
          <div 
            v-for="container in displayedContainers" 
            :key="container.containerId"
            class="container-card"
            :class="{ 
              'selected': selectedContainers.includes(container.containerId),
              'suitable': isContainerSuitable(container),
              'unsuitable': !isContainerSuitable(container)
            }"
            @click="toggleContainer(container)"
          >
            <div class="container-header">
              <div class="container-name">{{ getContainerDisplayName(container) }}</div>
              <div class="container-status">
                <el-tag :type="getContainerStatusType(container.status)" size="small">
                  {{ container.status }}
                </el-tag>
                <el-tag v-if="isContainerSuitable(container)" type="success" size="small">
                  适合导入
                </el-tag>
                <el-tag v-else type="warning" size="small">
                  系统容器
                </el-tag>
              </div>
            </div>
            
            <div class="container-info">
              <div class="info-item">
                <span class="label">镜像:</span>
                <span class="value docker-image">{{ container.image }}</span>
              </div>
              <div class="info-item" v-if="container.containerId">
                <span class="label">ID:</span>
                <span class="value container-id">{{ container.containerId.substring(0, 12) }}...</span>
              </div>
              <div class="info-item" v-if="container.cpuUsage">
                <span class="label">CPU:</span>
                <span class="value">{{ container.cpuUsage }}</span>
              </div>
              <div class="info-item" v-if="container.memoryUsage">
                <span class="label">内存:</span>
                <span class="value">{{ container.memoryUsage }}</span>
              </div>
            </div>

            <div class="container-actions" v-if="isContainerSuitable(container)">
              <el-checkbox 
                :model-value="selectedContainers.includes(container.containerId)"
                @change="toggleContainer(container)"
                @click.stop
              >
                导入此容器
              </el-checkbox>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 导入结果 -->
    <el-alert
      v-if="importResult"
      :title="`导入完成：成功 ${importResult.importedCount} 个，失败 ${importResult.errors?.length || 0} 个`"
      :type="importResult.errors?.length > 0 ? 'warning' : 'success'"
      :closable="false"
      show-icon
      class="import-result"
    >
      <div v-if="importResult.errors?.length > 0">
        <p>失败原因：</p>
        <ul>
          <li v-for="error in importResult.errors" :key="error">{{ error }}</li>
        </ul>
      </div>
    </el-alert>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search, Download } from '@element-plus/icons-vue'

const emit = defineEmits(['close', 'imported'])

// 响应式数据
const availableNodes = ref([])
const selectedNodeId = ref(null)
const discoveredContainers = ref([])
const selectedContainers = ref([])
const loadingNodes = ref(false)
const discovering = ref(false)
const importing = ref(false)
const selectAll = ref(false)
const onlySuitable = ref(true)
const importResult = ref(null)

const importConfig = ref({
  company: '发现的容器',
  project: '容器导入'
})

// 计算属性
const displayedContainers = computed(() => {
  if (onlySuitable.value) {
    return discoveredContainers.value.filter(isContainerSuitable)
  }
  return discoveredContainers.value
})

// 方法
const loadNodes = async () => {
  loadingNodes.value = true
  try {
    const response = await fetch('/api/host-nodes/active', {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    })
    if (response.ok) {
      availableNodes.value = await response.json()
    }
  } catch (error) {
    ElMessage.error('加载节点失败')
  } finally {
    loadingNodes.value = false
  }
}

const selectNode = (node) => {
  selectedNodeId.value = node.id
  discoveredContainers.value = []
  selectedContainers.value = []
  importResult.value = null
}

const discoverContainers = async () => {
  if (!selectedNodeId.value) return
  
  discovering.value = true
  try {
    const response = await fetch(`/api/assets/discover-containers/${selectedNodeId.value}`, {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    })
    
    if (response.ok) {
      const result = await response.json()
      discoveredContainers.value = result.allContainers || []
      ElMessage.success(`发现 ${result.totalCount} 个容器，其中 ${result.suitableCount} 个适合导入`)
    } else {
      ElMessage.error('容器发现失败')
    }
  } catch (error) {
    ElMessage.error('容器发现失败')
  } finally {
    discovering.value = false
  }
}

const toggleContainer = (container) => {
  if (!isContainerSuitable(container)) return
  
  const index = selectedContainers.value.indexOf(container.containerId)
  if (index > -1) {
    selectedContainers.value.splice(index, 1)
  } else {
    selectedContainers.value.push(container.containerId)
  }
  
  // 更新全选状态
  const suitableContainers = displayedContainers.value.filter(isContainerSuitable)
  selectAll.value = suitableContainers.length > 0 && 
    suitableContainers.every(c => selectedContainers.value.includes(c.containerId))
}

const handleSelectAll = (checked) => {
  if (checked) {
    const suitableContainers = displayedContainers.value.filter(isContainerSuitable)
    selectedContainers.value = suitableContainers.map(c => c.containerId)
  } else {
    selectedContainers.value = []
  }
}

const filterContainers = () => {
  selectedContainers.value = []
  selectAll.value = false
}

const importSelectedContainers = async () => {
  if (selectedContainers.value.length === 0) {
    ElMessage.warning('请选择要导入的容器')
    return
  }

  importing.value = true
  try {
    const containersToImport = discoveredContainers.value.filter(c => 
      selectedContainers.value.includes(c.containerId)
    )

    const response = await fetch('/api/assets/import-containers', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      },
      body: JSON.stringify({
        nodeId: selectedNodeId.value,
        company: importConfig.value.company,
        project: importConfig.value.project,
        containers: containersToImport
      })
    })

    if (response.ok) {
      const result = await response.json()
      importResult.value = result
      
      if (result.importedCount > 0) {
        ElMessage.success(`成功导入 ${result.importedCount} 个容器`)
        emit('imported', result)
      }
      
      if (result.errors?.length > 0) {
        ElMessage.warning(`${result.errors.length} 个容器导入失败`)
      }
    } else {
      ElMessage.error('导入失败')
    }
  } catch (error) {
    ElMessage.error('导入容器失败')
  } finally {
    importing.value = false
  }
}

const isContainerSuitable = (container) => {
  // 基本的适合性检查
  const name = container.name?.toLowerCase() || ''
  const image = container.image?.toLowerCase() || ''
  
  // 排除系统容器
  if (name.includes('docker') && (name.includes('proxy') || name.includes('registry'))) {
    return false
  }
  if (name.startsWith('k8s_') || name.startsWith('kindnet-') || name.startsWith('coredns-')) {
    return false
  }
  if (image.includes('pause') || image.includes('kindnet') || image.includes('coredns')) {
    return false
  }
  
  return true
}

const getContainerDisplayName = (container) => {
  if (container.name) {
    return container.name.startsWith('/') ? container.name.substring(1) : container.name
  }
  return container.containerId?.substring(0, 12) || 'Unknown'
}

const getNodeStatusType = (status) => {
  const statusMap = {
    'active': 'success',
    'inactive': 'danger',
    'maintenance': 'warning'
  }
  return statusMap[status] || 'info'
}

const getNodeStatusLabel = (status) => {
  const statusMap = {
    'active': '活跃',
    'inactive': '离线',
    'maintenance': '维护中'
  }
  return statusMap[status] || status
}

const getEnvironmentLabel = (environment) => {
  const envMap = {
    'development': '开发',
    'testing': '测试',
    'staging': '预发布',
    'production': '生产'
  }
  return envMap[environment] || environment
}

const getContainerStatusType = (status) => {
  if (status.toLowerCase().includes('running')) return 'success'
  if (status.toLowerCase().includes('exited')) return 'info'
  if (status.toLowerCase().includes('error')) return 'danger'
  return 'warning'
}

onMounted(() => {
  loadNodes()
})
</script>

<style scoped>
.container-discovery {
  max-width: 1200px;
  margin: 0 auto;
}

.discovery-header {
  text-align: center;
  margin-bottom: 24px;
}

.discovery-header h3 {
  margin: 0 0 8px 0;
  color: #333;
  font-size: 20px;
}

.description {
  color: #666;
  margin: 0;
}

.node-selection-card, .discovery-card {
  margin-bottom: 24px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}

.nodes-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
  min-height: 120px;
}

.node-card {
  padding: 16px;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.node-card:hover {
  border-color: #3b82f6;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.1);
}

.node-card.selected {
  border-color: #3b82f6;
  background: #eff6ff;
}

.node-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.node-name {
  font-weight: 600;
  color: #333;
}

.node-info .info-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
  font-size: 14px;
}

.info-item .label {
  color: #666;
  font-weight: 500;
}

.info-item .value {
  color: #333;
}

.import-config {
  margin-bottom: 20px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 6px;
}

.containers-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.container-card {
  padding: 16px;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.container-card.suitable {
  cursor: pointer;
}

.container-card.suitable:hover {
  border-color: #10b981;
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.1);
}

.container-card.selected {
  border-color: #10b981;
  background: #ecfdf5;
}

.container-card.unsuitable {
  opacity: 0.6;
  border-color: #d1d5db;
}

.container-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.container-name {
  font-weight: 600;
  color: #333;
  flex: 1;
  margin-right: 8px;
}

.container-status {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: flex-end;
}

.container-info {
  margin-bottom: 12px;
}

.container-info .info-item {
  display: flex;
  margin-bottom: 6px;
  font-size: 13px;
}

.container-info .label {
  color: #666;
  font-weight: 500;
  min-width: 40px;
  margin-right: 8px;
}

.container-info .value {
  color: #333;
  flex: 1;
}

.docker-image {
  font-family: 'Monaco', 'Menlo', monospace;
  background: #f0f9ff;
  padding: 2px 4px;
  border-radius: 4px;
  color: #0369a1;
}

.container-id {
  font-family: 'Monaco', 'Menlo', monospace;
  color: #6b7280;
}

.container-actions {
  padding-top: 12px;
  border-top: 1px solid #e5e7eb;
}

.import-result {
  margin-top: 20px;
}

.import-result ul {
  margin: 8px 0 0 0;
  padding-left: 20px;
}
</style>