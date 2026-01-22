<template>
  <div class="project-host-node-management" :class="themeClass">
    <div class="page-header">
      <h3>资产节点分布</h3>
      <div class="header-actions">
        <el-button @click="loadNodeDistribution(true)" size="small" :loading="loading">
          <el-icon><Refresh /></el-icon> 刷新
        </el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-cards">
      <div class="stat-card nodes">
        <div class="stat-icon">🖥️</div>
        <div class="stat-content">
          <div class="stat-number">{{ totalNodes }}</div>
          <div class="stat-label">总节点数</div>
        </div>
      </div>
      <div class="stat-card assets">
        <div class="stat-icon">📦</div>
        <div class="stat-content">
          <div class="stat-number">{{ totalAssetsWithNodes }}</div>
          <div class="stat-label">已关联资产</div>
        </div>
      </div>
      <div class="stat-card containers">
        <div class="stat-icon">🚀</div>
        <div class="stat-content">
          <div class="stat-number">{{ totalContainers }}</div>
          <div class="stat-label">运行容器</div>
        </div>
      </div>
    </div>

    <!-- 资产节点列表 -->
    <div class="asset-nodes-list" v-if="!loading && assetNodes.length > 0">
      <div 
        v-for="assetNode in assetNodes" 
        :key="assetNode.id"
        class="asset-node-card"
        :class="[
          assetNode.isAssigned ? `status-${assetNode.assignedNode.status}` : 'status-unassigned',
          `strategy-${assetNode.deploymentStrategy}`
        ]"
      >
        <div class="asset-node-header">
          <div class="asset-title">
            <h4>{{ assetNode.asset.name }}</h4>
            <div class="asset-badges">
              <span class="asset-type-badge">{{ getAssetTypeLabel(assetNode.asset.assetType) }}</span>
              <span :class="['strategy-badge', `strategy-${assetNode.deploymentStrategy}`]">
                {{ getDeploymentStrategyText(assetNode.deploymentStrategy) }}
              </span>
            </div>
          </div>
        </div>

        <div class="asset-info">
          <div class="info-row">
            <span class="label">资产IP:</span>
            <span class="value">{{ assetNode.asset.ip || '未配置' }}</span>
          </div>
          <div class="info-row" v-if="assetNode.assignedNode">
            <span class="label">部署节点:</span>
            <span class="value" :class="['node-status', `status-${assetNode.assignedNode.status}`]">
              {{ assetNode.assignedNode.displayName }}
              <span class="node-address">({{ assetNode.assignedNode.hostIp }}:{{ assetNode.assignedNode.dockerPort }})</span>
            </span>
          </div>
          <div class="info-row" v-else>
            <span class="label">部署节点:</span>
            <span class="value status-unassigned">未分配</span>
          </div>
          <div class="info-row" v-if="assetNode.assignedNode">
            <span class="label">节点环境:</span>
            <span class="value">{{ getEnvironmentLabel(assetNode.assignedNode.environment) }}</span>
          </div>
          <div class="info-row" v-if="assetNode.assignedNode">
            <span class="label">容器状态:</span>
            <span class="value" :class="getContainerStatusClass(assetNode.containerInfo)">
              {{ assetNode.runningContainers || 0 }}/{{ assetNode.containers || 0 }} 个
              <span v-if="assetNode.containerInfo?.loadRatio > 0" class="load-indicator">
                ({{ Math.round((assetNode.containerInfo.loadRatio || 0) * 100) }}%)
              </span>
            </span>
          </div>
          <div class="info-row" v-if="assetNode.nodeScore !== undefined && assetNode.nodeScore > 0">
            <span class="label">节点评分:</span>
            <span class="value" :class="getNodeScoreClass(assetNode.nodeScore)">
              {{ assetNode.nodeScore }}/100
            </span>
          </div>
        </div>

        <!-- 资产详细信息 -->
        <div class="asset-details" v-if="assetNode.asset.notes">
          <div class="details-header">
            <span>📋 资产详情</span>
          </div>
          <div class="details-content">
            <div class="detail-item" v-if="assetNode.asset.company">
              <span class="detail-label">所属企业:</span>
              <span class="detail-value">{{ assetNode.asset.company }}</span>
            </div>
            <div class="detail-item" v-if="assetNode.asset.project">
              <span class="detail-label">所属项目:</span>
              <span class="detail-value">{{ assetNode.asset.project }}</span>
            </div>
            <div class="detail-item" v-if="assetNode.asset.notes">
              <span class="detail-label">备注信息:</span>
              <span class="detail-value">{{ assetNode.asset.notes }}</span>
            </div>
          </div>
        </div>
        
        <!-- 部署状态指示器 -->
        <div class="deployment-status">
          <div class="status-indicator" :class="assetNode.isAssigned ? 'status-deployed' : 'status-pending'">
            <span class="status-icon">{{ assetNode.isAssigned ? '✅' : '⏳' }}</span>
            <span class="status-text">{{ assetNode.isAssigned ? '已部署' : '待部署' }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else-if="!loading && assetNodes.length === 0" class="empty-state">
      <div class="empty-icon">📦</div>
      <h3>暂无节点分布数据</h3>
      <p>节点配置功能正在维护中，或暂无可用节点</p>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <el-icon class="is-loading"><Loading /></el-icon>
      <p>加载资产节点分布中...</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Loading, Warning } from '@element-plus/icons-vue'
import { getHostNodes, getHostNodesLoadInfo } from '@/api/hostNodes'
import { getAssets } from '@/api/asset'
import { getUserRole } from '@/utils/auth'

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

const emit = defineEmits(['node-updated'])

const assetNodes = ref([]) // 改为资产节点列表
const allNodes = ref([])
const projectAssets = ref([])
const nodeContainers = ref({})
const loading = ref(false)

// 主题支持
const role = getUserRole() || ''
const themeClass = computed(() => role === 'blue' ? 'theme-blue' : 'theme-admin')

// 计算属性
const totalNodes = computed(() => assetNodes.value.length)

const totalAssetsWithNodes = computed(() => {
  // 统计所有有关联节点的资产（包括固定部署和灵活部署）
  const fixedAssets = projectAssets.value.filter(asset => asset.preferredHostNodeId).length
  const flexibleAssets = projectAssets.value.filter(asset => 
    asset.deploymentStrategy === 'any' && !asset.preferredHostNodeId
  ).length
  return fixedAssets + flexibleAssets
})

const totalContainers = computed(() => {
  // 按节点去重统计，避免重复计算同一节点的容器
  const nodeContainerMap = {}
  assetNodes.value.forEach(assetNode => {
    if (assetNode.assignedNode?.id) {
      const nodeId = assetNode.assignedNode.id
      // 每个节点只统计一次运行中的容器数量
      if (!nodeContainerMap[nodeId]) {
        nodeContainerMap[nodeId] = assetNode.containerInfo?.running || 0
      }
    }
  })
  return Object.values(nodeContainerMap).reduce((sum, count) => sum + count, 0)
})

// 新增：分布均衡度指标（基于资产在物理节点上的分布）
const distributionBalance = computed(() => {
  if (assetNodes.value.length === 0) return 100
  
  // 统计每个物理节点上的资产数量
  const nodeAssetCounts = {}
  assetNodes.value.forEach(assetNode => {
    const nodeId = assetNode.assignedNode?.id || 'unassigned'
    nodeAssetCounts[nodeId] = (nodeAssetCounts[nodeId] || 0) + 1
  })
  
  const counts = Object.values(nodeAssetCounts)
  if (counts.length <= 1) return 100
  
  const avgAssets = counts.reduce((sum, count) => sum + count, 0) / counts.length
  const variance = counts.reduce((sum, count) => sum + Math.pow(count - avgAssets, 2), 0) / counts.length
  const standardDeviation = Math.sqrt(variance)
  
  // 转换为均衡度百分比
  const balanceScore = Math.max(0, 100 - (standardDeviation / avgAssets) * 100)
  return Math.round(balanceScore)
})

// 从缓存加载节点分布
const loadCachedNodeDistribution = () => {
  const cacheKey = `project_nodes_${props.projectId}`
  const cached = localStorage.getItem(cacheKey)
  if (cached) {
    try {
      const { data } = JSON.parse(cached)
      allNodes.value = data.allNodes || []
      projectAssets.value = data.projectAssets || []
      assetNodes.value = data.assetNodes || []
      nodeContainers.value = data.nodeContainers || {}
    } catch (error) {
      console.error('加载缓存失败:', error)
    }
  }
}

// 加载节点分布数据
const loadNodeDistribution = async (showMessage = true) => {
  loading.value = true
  try {
    // 并发加载所有需要的数据
    await Promise.all([
      loadAllNodes(),
      loadProjectAssets(),
      loadContainerCounts()
    ])

    // 计算资产节点
    calculateAssetNodes()

    // 保存到缓存
    const cacheKey = `project_nodes_${props.projectId}`
    localStorage.setItem(cacheKey, JSON.stringify({
      data: {
        allNodes: allNodes.value,
        projectAssets: projectAssets.value,
        assetNodes: assetNodes.value,
        nodeContainers: nodeContainers.value
      },
      timestamp: Date.now()
    }))

    // 只在 showMessage 为 true 时显示提示
    if (showMessage) {
      ElMessage.success(`刷新成功！${totalNodes.value} 个节点，${totalAssetsWithNodes.value} 个资产`)
    }
  } catch (error) {
    // 静默失败 - 不显示错误提示，避免用户困扰
    console.warn('节点分布数据加载失败，可能节点配置已被删除:', error)
  } finally {
    loading.value = false
  }
}

// 加载所有节点
const loadAllNodes = async () => {
  try {
    allNodes.value = await getHostNodes()
  } catch (error) {
    console.error('Failed to load host nodes:', error)
    allNodes.value = []
  }
}

// 加载项目资产
const loadProjectAssets = async () => {
  try {
    const allAssets = await getAssets()
    // 按项目ID筛选资产
    projectAssets.value = (allAssets || []).filter(asset => {
      const assetProjectId = `${asset.company || '未知企业'}｜${asset.project || '未分组'}`
      return assetProjectId === props.projectId
    })
  } catch (error) {
    console.error('Failed to load project assets:', error)
    projectAssets.value = []
  }
}

// 加载容器数量
const loadContainerCounts = async () => {
  try {
    const loadInfoList = await getHostNodesLoadInfo()
    // 将容器数量更新到节点分布数据中
    nodeContainers.value = loadInfoList.reduce((acc, info) => {
      acc[info.nodeId] = {
        total: info.totalContainers || 0,
        running: info.runningContainers || 0,
        stopped: info.stoppedContainers || 0,
        failed: info.failedContainers || 0,
        loadRatio: info.loadRatio || 0,
        availableSlots: info.availableSlots || 0
      }
      return acc
    }, {})
  } catch (error) {
    nodeContainers.value = {}
  }
}

// 计算资产节点（每个资产作为一个独立的节点显示）
const calculateAssetNodes = () => {
  const assetNodesList = []
  // 临时负载跟踪：记录每个节点已分配的资产数量
  const tempNodeLoads = {}
  
  // 为每个项目资产创建一个独立的资产节点
  for (const asset of projectAssets.value) {
    let assignedNode = null
    let assignmentReason = ''
    
    // 确定资产分配的物理节点
    if (asset.preferredHostNodeId) {
      // 有明确指定的节点
      assignedNode = allNodes.value.find(node => node.id === asset.preferredHostNodeId)
      assignmentReason = asset.deploymentStrategy === 'fixed' ? '固定部署' : '首选节点'
    } else if (asset.deploymentStrategy === 'any') {
      // 使用"任意节点"策略，智能分配给负载最低的活跃节点
      assignedNode = selectOptimalNodeForAsset(asset, tempNodeLoads)
      // 计算当前分配后的负载情况，生成详细的分配理由
      const currentLoad = tempNodeLoads[assignedNode?.id] || 0
      assignmentReason = `轮询分配 (负载: ${currentLoad})`
    } else if (asset.deploymentStrategy === 'load_balanced') {
      // 负载均衡策略
      assignedNode = selectLoadBalancedNodeForAsset(asset, tempNodeLoads)
      const currentLoad = tempNodeLoads[assignedNode?.id] || 0
      assignmentReason = `负载均衡 (负载: ${currentLoad})`
    }
    
    // 如果没有分配到节点，尝试分配到第一个可用节点
    if (!assignedNode && allNodes.value.length > 0) {
      assignedNode = allNodes.value.find(node => node.status === 'active') || allNodes.value[0]
      assignmentReason = '默认分配'
    }
    
    // 更新临时负载跟踪
    if (assignedNode) {
      tempNodeLoads[assignedNode.id] = (tempNodeLoads[assignedNode.id] || 0) + 1
    }
    
    // 获取分配节点的容器信息
    const containerInfo = assignedNode ? 
      (nodeContainers.value[assignedNode.id] || { total: 0, running: 0, stopped: 0, failed: 0 }) : 
      { total: 0, running: 0, stopped: 0, failed: 0 }
    
    // 创建资产节点对象
    assetNodesList.push({
      id: `asset-${asset.id}`,
      asset: asset,
      assignedNode: assignedNode,
      assignmentReason: assignmentReason,
      deploymentStrategy: asset.deploymentStrategy || 'any',
      containers: assignedNode ? (containerInfo.total || 0) : 0,
      runningContainers: assignedNode ? (containerInfo.running || 0) : 0,
      containerInfo: containerInfo,
      nodeScore: assignedNode ? (containerInfo.nodeScore || 0) : 0,
      lastHealthCheck: assignedNode ? containerInfo.lastHealthCheck : null,
      isAssigned: !!assignedNode,
      canRedeploy: asset.deploymentStrategy !== 'fixed' // 固定部署的资产不能重新部署
    })
  }
  
  // 按资产名称排序
  assetNodesList.sort((a, b) => a.asset.name.localeCompare(b.asset.name))
  
  assetNodes.value = assetNodesList
}

// 轮询分配策略：确保资产尽可能均匀分配到所有活跃节点
const selectRoundRobinNode = (activeNodes, tempNodeLoads = {}) => {
  if (activeNodes.length === 0) return null
  if (activeNodes.length === 1) return activeNodes[0]
  
  // 计算每个节点的总负载（现有+临时）
  const nodeLoads = {}
  activeNodes.forEach(node => {
    const existingLoad = assetNodes.value.filter(an => an.assignedNode?.id === node.id).length
    const tempLoad = tempNodeLoads[node.id] || 0
    nodeLoads[node.id] = existingLoad + tempLoad
  })
  
  // 找到负载最低的节点
  const minLoad = Math.min(...Object.values(nodeLoads))
  const candidateNodes = activeNodes.filter(node => nodeLoads[node.id] === minLoad)
  
  // 改进轮询逻辑：基于资产名称的哈希值来确保相同资产始终分配到同一节点，但不同资产分配到不同节点
  const totalAssetsProcessed = Object.values(tempNodeLoads).reduce((sum, load) => sum + load, 0)
  
  // 使用时间戳确保每次分配都有一定的随机性，避免所有资产都分配到同一节点
  const randomFactor = (Date.now() + totalAssetsProcessed) % candidateNodes.length
  return candidateNodes[randomFactor]
}

// 为资产选择最优节点（任意策略）
const selectOptimalNodeForAsset = (asset, tempNodeLoads = {}) => {
  const activeNodes = allNodes.value.filter(node => node.status === 'active')
  if (activeNodes.length === 0) return null
  
  // 如果只有一个节点，直接返回
  if (activeNodes.length === 1) return activeNodes[0]
  
  // 基于资产名称的哈希值来确保同名资产分配到同一节点，不同资产分配到不同节点
  const assetHash = asset.name.split('').reduce((hash, char) => {
    return ((hash << 5) - hash) + char.charCodeAt(0)
  }, 0)
  
  // 计算每个节点当前的资产负载（结合临时分配记录）
  const nodeLoads = {}
  activeNodes.forEach(node => {
    const existingLoad = assetNodes.value.filter(an => an.assignedNode?.id === node.id).length
    const tempLoad = tempNodeLoads[node.id] || 0
    nodeLoads[node.id] = existingLoad + tempLoad
  })
  
  // 找到负载最低的值
  const minLoad = Math.min(...Object.values(nodeLoads))
  
  // 获取所有负载最低的节点
  const lightestNodes = activeNodes.filter(node => nodeLoads[node.id] === minLoad)
  
  // 如果有多个负载相同的节点，基于资产哈希值和节点优先级选择
  return lightestNodes.reduce((best, current) => {
    // 优先选择优先级高的节点
    const bestPriority = best.priority || 1
    const currentPriority = current.priority || 1
    
    if (currentPriority > bestPriority) return current
    if (currentPriority < bestPriority) return best
    
    // 优先级相同时，基于资产哈希值选择节点，确保不同资产分配到不同节点
    const bestScore = (Math.abs(assetHash) + best.id) % 1000
    const currentScore = (Math.abs(assetHash) + current.id) % 1000
    
    return currentScore < bestScore ? current : best
  })
}

// 为资产选择负载均衡节点
const selectLoadBalancedNodeForAsset = (asset, tempNodeLoads = {}) => {
  const activeNodes = allNodes.value.filter(node => node.status === 'active')
  if (activeNodes.length === 0) return null
  
  // 如果只有一个节点，直接返回
  if (activeNodes.length === 1) return activeNodes[0]
  
  // 基于资产名称哈希值确保分配的一致性但又有差异性
  const assetHash = asset.name.split('').reduce((hash, char) => {
    return ((hash << 5) - hash) + char.charCodeAt(0)
  }, 0)
  
  // 基于节点优先级、当前负载和容器负载率综合选择
  return activeNodes.sort((a, b) => {
    const aInfo = nodeContainers.value[a.id] || { loadRatio: 0 }
    const bInfo = nodeContainers.value[b.id] || { loadRatio: 0 }
    
    // 计算资产负载（包含临时分配）
    const aAssetLoad = (assetNodes.value.filter(an => an.assignedNode?.id === a.id).length + (tempNodeLoads[a.id] || 0))
    const bAssetLoad = (assetNodes.value.filter(an => an.assignedNode?.id === b.id).length + (tempNodeLoads[b.id] || 0))
    
    // 先按资产负载均衡
    const assetLoadDiff = aAssetLoad - bAssetLoad
    if (assetLoadDiff !== 0) return assetLoadDiff
    
    // 然后按优先级（优先级高的排在前面）
    const priorityDiff = (b.priority || 1) - (a.priority || 1)
    if (priorityDiff !== 0) return priorityDiff
    
    // 最后按容器负载率
    const loadRatioDiff = (aInfo.loadRatio || 0) - (bInfo.loadRatio || 0)
    if (loadRatioDiff !== 0) return loadRatioDiff
    
    // 如果所有条件都相等，使用资产哈希值确保一致的分配
    const aScore = (Math.abs(assetHash) + a.id) % 1000
    const bScore = (Math.abs(assetHash) + b.id) % 1000
    return aScore - bScore
  })[0]
}

// 工具方法
const getStatusLabel = (status) => {
  const labels = {
    active: '活跃',
    inactive: '离线',
    maintenance: '维护中',
    error: '异常'
  }
  return labels[status] || status
}

const getEnvironmentLabel = (environment) => {
  const labels = {
    development: '开发环境',
    testing: '测试环境',
    staging: '预发布环境',
    production: '生产环境'
  }
  return labels[environment] || environment
}

const getAssetTypeTagType = (assetType) => {
  const types = {
    server: 'info',
    container: 'success',
    service: 'warning',
    network: 'danger'
  }
  return types[assetType] || 'info'
}

const getAssetTooltip = (asset) => {
  let tooltip = `名称：${asset.name}\n类型：${getAssetTypeLabel(asset.assetType)}`
  if (asset.ip) {
    tooltip += `\nIP：${asset.ip}`
  }
  if (asset.deploymentStrategy) {
    tooltip += `\n部署策略：${getDeploymentStrategyText(asset.deploymentStrategy)}`
  }
  return tooltip
}

const getAssetTypeLabel = (assetType) => {
  const labels = {
    server: '服务器',
    container: '容器',
    service: '服务',
    network: '网络设备'
  }
  return labels[assetType] || '未知'
}

const getDeploymentStrategyText = (strategy) => {
  const labels = {
    fixed: '固定节点',
    any: '任意节点',
    load_balanced: '负载均衡'
  }
  return labels[strategy] || '任意节点'
}

const getContainerStatusClass = (containerInfo) => {
  if (!containerInfo) return ''
  const loadRatio = containerInfo.loadRatio || 0
  if (loadRatio >= 0.9) return 'status-critical'
  if (loadRatio >= 0.7) return 'status-warning'
  return 'status-normal'
}

const getNodeScoreClass = (score) => {
  if (score >= 90) return 'score-excellent'
  if (score >= 75) return 'score-good'
  if (score >= 60) return 'score-fair'
  if (score >= 40) return 'score-poor'
  return 'score-critical'
}

onMounted(() => {
  loadNodeDistribution(false)  // 静默加载，不显示提示
})
</script>

<style scoped>
/* ============================================
   Apple Elegant White Style - Host Node Management
   ============================================ */

/* CSS Variables - Apple Design System */
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
  --apple-purple: #af52de;
  --radius-sm: 12px;
  --radius-md: 16px;
  --radius-lg: 20px;
  --shadow-card: 0 4px 16px rgba(0, 0, 0, 0.06);
  --shadow-card-hover: 0 8px 32px rgba(0, 0, 0, 0.12);
  --shadow-elevated: 0 12px 48px rgba(0, 0, 0, 0.18);
  --spacing-sm: 8px;
  --spacing-md: 16px;
  --spacing-lg: 24px;
  --spacing-xl: 32px;
  --font-apple: -apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", "Helvetica Neue", sans-serif;
}

/* ============================================
   Main Container - 主容器
   ============================================ */
.project-host-node-management {
  background: transparent;
  border-radius: 0;
  padding: 0;
  font-family: var(--font-apple);
}

/* ============================================
   Page Header - 页面标题
   ============================================ */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-lg);
  padding: var(--spacing-lg);
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.95) 0%,
    rgba(248, 248, 248, 0.85) 100%);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 0.5px solid var(--apple-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
}

.page-header h3 {
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

/* Header Buttons */
.header-actions :deep(.el-button) {
  height: 40px;
  min-width: 100px;
  font-size: 14px;
  padding: 0 20px;
  border-radius: var(--radius-sm);
  font-weight: 600;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
  border: none;
}

.header-actions :deep(.el-button--info) {
  background: linear-gradient(135deg, var(--apple-blue), #0051d5);
  color: white;
}

.header-actions :deep(.el-button--info:hover) {
  background: linear-gradient(135deg, #0051d5, #003db3);
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 122, 255, 0.4);
}

.header-actions :deep(.el-button:not(.el-button--info)) {
  background: rgba(0, 0, 0, 0.04);
  color: var(--apple-text);
}

.header-actions :deep(.el-button:not(.el-button--info):hover) {
  background: rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

/* ============================================
   Statistics Cards - 统计卡片
   ============================================ */
.stats-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-lg);
}

.stat-card {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.98) 0%,
    rgba(248, 248, 248, 0.95) 100%);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: var(--radius-md);
  padding: var(--spacing-lg);
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  box-shadow: var(--shadow-card);
  border: 0.5px solid var(--apple-border);
  border-left: 4px solid #ddd;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
  position: relative;
  overflow: hidden;
}

.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg,
    transparent 0%,
    rgba(0, 122, 255, 0.02) 100%);
  opacity: 0;
  transition: opacity 0.3s ease;
  pointer-events: none;
}

.stat-card:hover {
  box-shadow: var(--shadow-card-hover);
  transform: translateY(-4px);
}

.stat-card:hover::before {
  opacity: 1;
}

.stat-card.nodes {
  border-left-color: var(--apple-blue);
}

.stat-card.assets {
  border-left-color: var(--apple-green);
}

.stat-card.containers {
  border-left-color: var(--apple-purple);
}

.stat-card.balance {
  border-left-color: var(--apple-orange);
}

.stat-icon {
  font-size: 32px;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.1));
}

.stat-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-number {
  font-size: 28px;
  font-weight: 700;
  color: var(--apple-text);
  letter-spacing: -1px;
  line-height: 1;
}

.stat-label {
  font-size: 13px;
  color: var(--apple-text-secondary);
  font-weight: 500;
}

/* ============================================
   Asset Nodes List - 资产节点列表
   ============================================ */
.asset-nodes-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(380px, 1fr));
  gap: var(--spacing-md);
}

.asset-node-card {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.98) 0%,
    rgba(248, 248, 248, 0.95) 100%);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: var(--radius-md);
  padding: var(--spacing-lg);
  box-shadow: var(--shadow-card);
  border: 0.5px solid var(--apple-border);
  border-left: 4px solid #ddd;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
  position: relative;
  overflow: hidden;
}

.asset-node-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg,
    transparent 0%,
    rgba(0, 122, 255, 0.02) 100%);
  opacity: 0;
  transition: opacity 0.3s ease;
  pointer-events: none;
}

.asset-node-card:hover {
  box-shadow: var(--shadow-card-hover);
  transform: translateY(-4px);
}

.asset-node-card:hover::before {
  opacity: 1;
}

.asset-node-card.status-active {
  border-left-color: var(--apple-green);
}

.asset-node-card.status-inactive {
  border-left-color: var(--apple-red);
}

.asset-node-card.status-unassigned {
  border-left-color: var(--apple-orange);
}

/* ============================================
   Asset Node Header - 资产节点头部
   ============================================ */
.asset-node-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--spacing-md);
}

.asset-title h4 {
  margin: 0 0 var(--spacing-sm) 0;
  font-size: 18px;
  color: var(--apple-text);
  font-weight: 700;
  letter-spacing: -0.3px;
}

.asset-badges {
  display: flex;
  gap: var(--spacing-sm);
  flex-wrap: wrap;
  margin-top: var(--spacing-sm);
}

.asset-type-badge {
  background: linear-gradient(135deg, rgba(0, 122, 255, 0.1), rgba(0, 122, 255, 0.15));
  color: var(--apple-blue);
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
  border: 0.5px solid rgba(0, 122, 255, 0.2);
}

.strategy-badge {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
}

.strategy-badge.strategy-fixed {
  background: linear-gradient(135deg, #f0f9ff, #e0f2fe);
  color: #1e40af;
  border: 0.5px solid #bfdbfe;
}

.strategy-badge.strategy-any {
  background: linear-gradient(135deg, #f0fdf4, #dcfce7);
  color: #16a34a;
  border: 0.5px solid #bbf7d0;
}

.strategy-badge.strategy-load_balanced {
  background: linear-gradient(135deg, #fef3c7, #fde68a);
  color: #d97706;
  border: 0.5px solid #fcd34d;
}

.status-badge {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
}

.status-badge.status-active {
  background: linear-gradient(135deg, rgba(52, 199, 89, 0.15), rgba(52, 199, 89, 0.1));
  color: var(--apple-green);
  border: 0.5px solid rgba(52, 199, 89, 0.3);
}

.status-badge.status-inactive {
  background: linear-gradient(135deg, rgba(255, 59, 48, 0.15), rgba(255, 59, 48, 0.1));
  color: var(--apple-red);
  border: 0.5px solid rgba(255, 59, 48, 0.3);
}

/* ============================================
   Asset Actions - 资产操作按钮
   ============================================ */
.asset-actions {
  display: flex;
  gap: var(--spacing-sm);
  flex-wrap: wrap;
}

/* ============================================
   Asset Info - 资产信息
   ============================================ */
.asset-info {
  margin-bottom: var(--spacing-md);
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-sm);
  font-size: 13px;
  padding: 6px 0;
  border-bottom: 0.5px solid rgba(0, 0, 0, 0.04);
}

.info-row:last-child {
  border-bottom: none;
}

.info-row .label {
  color: var(--apple-text-secondary);
  font-weight: 600;
  font-size: 12px;
}

.info-row .value {
  color: var(--apple-text);
  font-weight: 500;
}

/* Node Status Display */
.node-status {
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.node-status.status-active {
  color: var(--apple-green);
}

.node-status.status-inactive {
  color: var(--apple-red);
}

.node-address {
  font-size: 11px;
  color: var(--apple-text-secondary);
  font-weight: 500;
  opacity: 0.7;
}

/* ============================================
   Assets List (Unused but kept for compatibility)
   ============================================ */
.assets-list {
  margin-top: var(--spacing-md);
  padding: var(--spacing-md);
  background: rgba(245, 245, 247, 0.5);
  border-radius: var(--radius-sm);
}

.assets-header {
  font-size: 12px;
  color: var(--apple-text-secondary);
  margin-bottom: var(--spacing-sm);
  font-weight: 600;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--spacing-sm);
}

.asset-strategy-info {
  display: flex;
  gap: var(--spacing-sm);
  flex-wrap: wrap;
}

.strategy-label {
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 8px;
  font-weight: 600;
}

.strategy-label.explicit {
  background: linear-gradient(135deg, rgba(0, 122, 255, 0.1), rgba(0, 122, 255, 0.15));
  color: var(--apple-blue);
}

.strategy-label.any {
  background: linear-gradient(135deg, #f0f9ff, #e0f2fe);
  color: #2563eb;
}

.asset-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.asset-tag {
  cursor: pointer;
  position: relative;
}

.asset-tag.any-strategy {
  border-style: dashed;
  background: linear-gradient(45deg, transparent 25%, rgba(37, 99, 235, 0.1) 25%, rgba(37, 99, 235, 0.1) 50%, transparent 50%);
}

.strategy-icon {
  font-size: 10px;
  margin-left: 2px;
  opacity: 0.7;
}

.no-assets {
  text-align: center;
  padding: var(--spacing-md);
  color: var(--apple-text-secondary);
  font-size: 12px;
  background: rgba(245, 245, 247, 0.5);
  border-radius: var(--radius-sm);
  margin-top: var(--spacing-md);
}

/* ============================================
   Empty & Loading States - 空状态和加载状态
   ============================================ */
.empty-state, .loading-state {
  text-align: center;
  padding: 60px 20px;
  color: var(--apple-text-secondary);
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.95) 0%,
    rgba(248, 248, 248, 0.85) 100%);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: var(--radius-lg);
  border: 0.5px solid var(--apple-border);
  box-shadow: var(--shadow-card);
}

.empty-icon {
  font-size: 64px;
  margin-bottom: var(--spacing-lg);
  filter: drop-shadow(0 4px 8px rgba(0, 0, 0, 0.1));
}

.empty-state h3 {
  font-size: 20px;
  font-weight: 700;
  color: var(--apple-text);
  margin: 0 0 var(--spacing-sm) 0;
  letter-spacing: -0.5px;
}

.empty-state p {
  font-size: 14px;
  color: var(--apple-text-secondary);
  margin: 0;
  line-height: 1.6;
}

/* ============================================
   Container Status Styles - 容器状态样式
   ============================================ */
.status-normal {
  color: var(--apple-green);
  font-weight: 600;
}

.status-warning {
  color: var(--apple-orange);
  font-weight: 700;
}

.status-critical {
  color: var(--apple-red);
  font-weight: 700;
}

.load-indicator {
  font-size: 10px;
  opacity: 0.8;
}

/* ============================================
   Load Balance Dialog (Unused but kept)
   ============================================ */
.load-balance-content {
  font-size: 14px;
  font-family: var(--font-apple);
}

.plan-summary {
  margin-bottom: var(--spacing-lg);
  padding: var(--spacing-md);
  background: rgba(245, 245, 247, 0.5);
  border-radius: var(--radius-sm);
}

.plan-summary h4 {
  margin: 0 0 var(--spacing-md) 0;
  color: var(--apple-text);
  font-size: 16px;
  font-weight: 700;
}

.summary-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: var(--spacing-md);
}

.stat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-item .label {
  color: var(--apple-text-secondary);
  font-size: 12px;
  font-weight: 600;
}

.stat-item .value {
  font-weight: 700;
  color: var(--apple-text);
}

.redistribution-list h5 {
  margin: 0 0 var(--spacing-md) 0;
  color: var(--apple-text);
  font-size: 14px;
  font-weight: 700;
}

.redistribution-items {
  max-height: 300px;
  overflow-y: auto;
}

.redistribution-item {
  display: flex;
  align-items: center;
  padding: var(--spacing-sm) var(--spacing-md);
  margin-bottom: var(--spacing-sm);
  background: white;
  border: 0.5px solid rgba(0, 0, 0, 0.08);
  border-radius: var(--radius-sm);
  gap: var(--spacing-md);
}

.asset-name {
  font-weight: 600;
  color: var(--apple-text);
  font-size: 13px;
}

.deployment-strategy {
  font-size: 10px;
  color: var(--apple-text-secondary);
  background: rgba(0, 0, 0, 0.04);
  padding: 2px 6px;
  border-radius: 8px;
  align-self: flex-start;
}

.arrow {
  color: var(--apple-blue);
  font-weight: 700;
  font-size: 16px;
}

.node-info {
  flex: 1;
  text-align: right;
}

.node-name {
  color: var(--apple-green);
  font-weight: 600;
  font-size: 13px;
}

.more-items {
  text-align: center;
  color: var(--apple-text-secondary);
  font-size: 12px;
  padding: var(--spacing-sm);
  font-style: italic;
}

.no-redistribution {
  text-align: center;
  padding: var(--spacing-lg);
  color: var(--apple-green);
}

/* ============================================
   Balance Level Styles - 均衡度等级样式
   ============================================ */
.balance-excellent {
  color: var(--apple-green);
  font-weight: 700;
}

.balance-good {
  color: var(--apple-blue);
  font-weight: 700;
}

.balance-fair {
  color: var(--apple-orange);
  font-weight: 700;
}

.balance-poor {
  color: var(--apple-red);
  font-weight: 700;
}

.balance-critical {
  color: var(--apple-red);
  font-weight: 700;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% { opacity: 1; }
  50% { opacity: 0.7; }
  100% { opacity: 1; }
}

/* ============================================
   Node Score Styles - 节点评分样式
   ============================================ */
.score-excellent {
  color: var(--apple-green);
  font-weight: 700;
}

.score-good {
  color: var(--apple-blue);
  font-weight: 700;
}

.score-fair {
  color: var(--apple-orange);
  font-weight: 700;
}

.score-poor {
  color: var(--apple-red);
  font-weight: 700;
}

.score-critical {
  color: var(--apple-red);
  font-weight: 700;
  animation: pulse 2s infinite;
}

/* ============================================
   Asset Details - 资产详情样式
   ============================================ */
.asset-details {
  margin-top: var(--spacing-md);
  padding: var(--spacing-md);
  background: linear-gradient(135deg,
    rgba(245, 245, 247, 0.5) 0%,
    rgba(255, 255, 255, 0.3) 100%);
  border-radius: var(--radius-sm);
  border: 0.5px solid rgba(0, 0, 0, 0.06);
}

.details-header {
  font-size: 13px;
  color: var(--apple-text);
  margin-bottom: var(--spacing-sm);
  font-weight: 700;
}

.details-content {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  padding: 4px 0;
}

.detail-label {
  color: var(--apple-text-secondary);
  font-weight: 600;
  min-width: 80px;
}

.detail-value {
  color: var(--apple-text);
  text-align: right;
  max-width: 220px;
  word-break: break-all;
  font-weight: 500;
}

/* ============================================
   Deployment Status - 部署状态指示器
   ============================================ */
.deployment-status {
  margin-top: var(--spacing-md);
  display: flex;
  justify-content: center;
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: 8px 16px;
  border-radius: 24px;
  font-size: 13px;
  font-weight: 600;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

.status-indicator.status-deployed {
  background: linear-gradient(135deg,
    rgba(52, 199, 89, 0.15) 0%,
    rgba(52, 199, 89, 0.08) 100%);
  color: var(--apple-green);
  border: 1px solid rgba(52, 199, 89, 0.3);
}

.status-indicator.status-deployed:hover {
  background: linear-gradient(135deg,
    rgba(52, 199, 89, 0.2) 0%,
    rgba(52, 199, 89, 0.12) 100%);
  box-shadow: 0 4px 12px rgba(52, 199, 89, 0.2);
}

.status-indicator.status-pending {
  background: linear-gradient(135deg,
    rgba(255, 149, 0, 0.15) 0%,
    rgba(255, 149, 0, 0.08) 100%);
  color: var(--apple-orange);
  border: 1px solid rgba(255, 149, 0, 0.3);
}

.status-indicator.status-pending:hover {
  background: linear-gradient(135deg,
    rgba(255, 149, 0, 0.2) 0%,
    rgba(255, 149, 0, 0.12) 100%);
  box-shadow: 0 4px 12px rgba(255, 149, 0, 0.2);
}

.status-icon {
  font-size: 16px;
  filter: drop-shadow(0 1px 2px rgba(0, 0, 0, 0.1));
}

.status-text {
  font-weight: 700;
  letter-spacing: -0.2px;
}

/* 未分配状态 */
.status-unassigned {
  color: var(--apple-orange);
  font-weight: 700;
}

/* ============================================
   Responsive Design - 响应式设计
   ============================================ */
@media (max-width: 1024px) {
  .asset-nodes-list {
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  }

  .stats-cards {
    grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  }
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--spacing-md);
  }

  .header-actions {
    width: 100%;
    flex-wrap: wrap;
  }

  .header-actions :deep(.el-button) {
    flex: 1;
    min-width: 120px;
  }

  .asset-nodes-list {
    grid-template-columns: 1fr;
  }

  .stats-cards {
    grid-template-columns: 1fr;
  }

  .stat-card {
    padding: var(--spacing-md);
  }

  .asset-node-card {
    padding: var(--spacing-md);
  }
}

@media (max-width: 576px) {
  .page-header {
    padding: var(--spacing-md);
  }

  .page-header h3 {
    font-size: 20px;
  }

  .stat-icon {
    font-size: 24px;
  }

  .stat-number {
    font-size: 22px;
  }

  .stat-label {
    font-size: 11px;
  }

  .asset-title h4 {
    font-size: 16px;
  }

  .detail-value {
    max-width: 160px;
  }
}

/* ============================================
   蓝队主题 - Blue Team Theme
   ============================================ */

/* 页面标题区域 */
.project-host-node-management.theme-blue .page-header {
  background: linear-gradient(135deg,
    rgba(20, 30, 50, 0.85) 0%,
    rgba(13, 26, 45, 0.95) 100%) !important;
  border: 1px solid rgba(70, 130, 180, 0.35) !important;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.45),
              0 0 20px rgba(70, 130, 180, 0.08) !important;
}

.project-host-node-management.theme-blue .page-header h3 {
  color: #ffffff !important;
  font-weight: 700 !important;
  text-shadow: 0 2px 8px rgba(70, 130, 180, 0.3) !important;
}

.project-host-node-management.theme-blue .header-actions :deep(.el-button) {
  background: linear-gradient(135deg, #4682b4 0%, #1e90ff 100%) !important;
  border-color: rgba(70, 130, 180, 0.6) !important;
  color: #ffffff !important;
  box-shadow: 0 4px 15px rgba(70, 130, 180, 0.2),
              inset 0 1px 0 rgba(255, 255, 255, 0.1) !important;
}

.project-host-node-management.theme-blue .header-actions :deep(.el-button:hover) {
  background: linear-gradient(135deg, #5fa3d4 0%, #00d4ff 100%) !important;
  border-color: rgba(70, 130, 180, 0.8) !important;
  box-shadow: 0 8px 25px rgba(70, 130, 180, 0.4),
              0 0 20px rgba(30, 144, 255, 0.3),
              inset 0 1px 0 rgba(255, 255, 255, 0.15) !important;
  transform: translateY(-2px) !important;
}

/* 统计卡片 */
.project-host-node-management.theme-blue .stat-card {
  background: linear-gradient(135deg,
    rgba(20, 30, 50, 0.85) 0%,
    rgba(13, 26, 45, 0.95) 100%) !important;
  border: 1px solid rgba(70, 130, 180, 0.35) !important;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3),
              0 0 10px rgba(70, 130, 180, 0.1) !important;
}

.project-host-node-management.theme-blue .stat-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4),
              0 0 20px rgba(70, 130, 180, 0.2) !important;
  transform: translateY(-4px) !important;
}

.project-host-node-management.theme-blue .stat-card.nodes {
  border-left-color: #00d4ff !important;
  border-left-width: 4px !important;
}

.project-host-node-management.theme-blue .stat-card.assets {
  border-left-color: #34c759 !important;
  border-left-width: 4px !important;
}

.project-host-node-management.theme-blue .stat-card.containers {
  border-left-color: #af52de !important;
  border-left-width: 4px !important;
}

.project-host-node-management.theme-blue .stat-number {
  color: #ffffff !important;
  text-shadow: 0 2px 8px rgba(255, 255, 255, 0.2) !important;
}

.project-host-node-management.theme-blue .stat-label {
  color: rgba(255, 255, 255, 0.7) !important;
}

/* 资产节点卡片 */
.project-host-node-management.theme-blue .asset-node-card {
  background: linear-gradient(135deg,
    rgba(20, 30, 50, 0.85) 0%,
    rgba(13, 26, 45, 0.95) 100%) !important;
  border: 1px solid rgba(70, 130, 180, 0.35) !important;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3),
              0 0 10px rgba(70, 130, 180, 0.1) !important;
}

.project-host-node-management.theme-blue .asset-node-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4),
              0 0 20px rgba(70, 130, 180, 0.2) !important;
  transform: translateY(-4px) !important;
}

.project-host-node-management.theme-blue .asset-node-card.status-active {
  border-left-color: #34c759 !important;
}

.project-host-node-management.theme-blue .asset-node-card.status-inactive {
  border-left-color: #ff3b30 !important;
}

.project-host-node-management.theme-blue .asset-node-card.status-unassigned {
  border-left-color: #ff9500 !important;
}

.project-host-node-management.theme-blue .asset-title h4 {
  color: #ffffff !important;
  font-weight: 700 !important;
}

.project-host-node-management.theme-blue .asset-type-badge {
  background: linear-gradient(135deg, rgba(0, 212, 255, 0.2), rgba(0, 212, 255, 0.3)) !important;
  color: #00d4ff !important;
  border: 0.5px solid rgba(0, 212, 255, 0.4) !important;
}

.project-host-node-management.theme-blue .strategy-badge {
  color: #ffffff !important;
  border: none !important;
}

.project-host-node-management.theme-blue .strategy-badge.strategy-fixed {
  background: linear-gradient(135deg, #4682b4, #1e90ff) !important;
}

.project-host-node-management.theme-blue .strategy-badge.strategy-any {
  background: linear-gradient(135deg, #34c759, #28a745) !important;
}

.project-host-node-management.theme-blue .strategy-badge.strategy-balanced {
  background: linear-gradient(135deg, #ff9500, #D97706) !important;
}

/* 资产信息行 */
.project-host-node-management.theme-blue .info-row .label {
  color: rgba(255, 255, 255, 0.7) !important;
}

.project-host-node-management.theme-blue .info-row .value {
  color: #ffffff !important;
}

.project-host-node-management.theme-blue .node-status.status-active {
  color: #34c759 !important;
  text-shadow: 0 0 10px rgba(52, 199, 89, 0.4) !important;
}

.project-host-node-management.theme-blue .node-status.status-inactive {
  color: #ff3b30 !important;
  text-shadow: 0 0 10px rgba(255, 59, 48, 0.4) !important;
}

.project-host-node-management.theme-blue .status-unassigned {
  color: #ff9500 !important;
  text-shadow: 0 0 10px rgba(255, 149, 0, 0.4) !important;
}

.project-host-node-management.theme-blue .node-address {
  color: rgba(255, 255, 255, 0.6) !important;
}

.project-host-node-management.theme-blue .load-indicator {
  color: #00d4ff !important;
}

/* 容器状态颜色 */
.project-host-node-management.theme-blue .status-good {
  color: #34c759 !important;
}

.project-host-node-management.theme-blue .status-warning {
  color: #ff9500 !important;
}

.project-host-node-management.theme-blue .status-danger {
  color: #ff3b30 !important;
}

/* 节点评分颜色 */
.project-host-node-management.theme-blue .score-good {
  color: #34c759 !important;
  text-shadow: 0 0 10px rgba(52, 199, 89, 0.4) !important;
}

.project-host-node-management.theme-blue .score-medium {
  color: #ff9500 !important;
  text-shadow: 0 0 10px rgba(255, 149, 0, 0.4) !important;
}

.project-host-node-management.theme-blue .score-low {
  color: #ff3b30 !important;
  text-shadow: 0 0 10px rgba(255, 59, 48, 0.4) !important;
}

/* 资产详情区域 */
.project-host-node-management.theme-blue .asset-details {
  background: rgba(10, 20, 40, 0.6) !important;
  border: 1px solid rgba(70, 130, 180, 0.25) !important;
}

.project-host-node-management.theme-blue .details-header {
  color: #00d4ff !important;
  border-bottom: 1px solid rgba(70, 130, 180, 0.25) !important;
}

.project-host-node-management.theme-blue .detail-label {
  color: rgba(255, 255, 255, 0.7) !important;
}

.project-host-node-management.theme-blue .detail-value {
  color: #ffffff !important;
}

/* 部署状态指示器 */
.project-host-node-management.theme-blue .deployment-status {
  background: transparent !important;
}

.project-host-node-management.theme-blue .status-indicator.status-deployed {
  background: linear-gradient(135deg, rgba(52, 199, 89, 0.2), rgba(40, 167, 69, 0.3)) !important;
  border: 1px solid rgba(52, 199, 89, 0.4) !important;
  color: #34c759 !important;
}

.project-host-node-management.theme-blue .status-indicator.status-pending {
  background: linear-gradient(135deg, rgba(255, 149, 0, 0.2), rgba(217, 119, 6, 0.3)) !important;
  border: 1px solid rgba(255, 149, 0, 0.4) !important;
  color: #ff9500 !important;
}

/* 空状态 */
.project-host-node-management.theme-blue .empty-state {
  color: rgba(255, 255, 255, 0.7) !important;
}

.project-host-node-management.theme-blue .empty-state h3 {
  color: #ffffff !important;
}

.project-host-node-management.theme-blue .empty-icon {
  filter: drop-shadow(0 4px 12px rgba(70, 130, 180, 0.3)) !important;
}

/* 加载状态 */
.project-host-node-management.theme-blue .loading-state {
  color: rgba(255, 255, 255, 0.7) !important;
}

.project-host-node-management.theme-blue .loading-state :deep(.el-icon) {
  color: #00d4ff !important;
}
</style>