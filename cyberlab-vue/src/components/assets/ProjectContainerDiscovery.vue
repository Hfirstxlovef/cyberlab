<template>
  <div class="project-container-discovery" :class="themeClass">
    <div class="discovery-header">
      <h3>项目容器发现与管理</h3>
      <div class="header-actions">
        <el-button @click="startDiscovery(false, true)" type="primary" size="small" :loading="discovering">
          <el-icon><Search /></el-icon> {{ discovering ? '发现中...' : '开始发现' }}
        </el-button>
      </div>
    </div>

    <!-- 项目状态概览 -->
    <div class="project-overview" v-if="!discovering">
      <div class="overview-card">
        <div class="overview-content">
          <div class="overview-item">
            <span class="overview-label">📦 项目资产:</span>
            <span class="overview-value">{{ projectAssetCount }} 个</span>
          </div>
          <div class="overview-item">
            <span class="overview-label">🎨 发现镜像:</span>
            <span class="overview-value">{{ discoveredImages.length }} 个</span>
          </div>
          <div class="overview-item">
            <span class="overview-label">🖥️ Docker节点:</span>
            <span class="overview-value">{{ discoveryNodes.length }} 个</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 筛选区域 -->
    <div class="filter-bar" v-if="discoveredImages.length > 0">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索镜像名称..."
        clearable
        class="search-input"
      />
      <el-select v-model="filterNode" placeholder="发现节点" clearable class="filter-select" :popper-class="role === 'blue' ? 'blue-team-dropdown' : ''">
        <el-option label="全部节点" value="" />
        <el-option
          v-for="node in discoveryNodes"
          :key="node.id"
          :label="node.displayName"
          :value="node.id"
        />
      </el-select>
      <el-button type="primary" @click="applyFilters">查询</el-button>
    </div>

    <!-- 镜像列表 -->
    <div class="containers-list" v-if="!discovering && filteredImages.length > 0">
      <div class="containers-grid">
        <div
          v-for="image in filteredImages"
          :key="image.imageId"
          class="container-card"
        >
          <div class="container-header">
            <div class="container-title">
              <span class="container-name">{{ image.repository }}:{{ image.tag }}</span>
            </div>
          </div>

          <div class="container-info">
            <div class="info-row">
              <span class="label">镜像ID:</span>
              <span class="value">{{ image.imageId.substring(0, 12) }}</span>
            </div>
            <div class="info-row">
              <span class="label">大小:</span>
              <span class="value">{{ image.size }}</span>
            </div>
            <div class="info-row">
              <span class="label">创建时间:</span>
              <span class="value">{{ image.created }}</span>
            </div>
            <div class="info-row deployment-info" v-if="image.hostNodeName">
              <span class="label">节点信息:</span>
              <div class="deployment-details">
                <div class="main-info">
                  <div class="asset-info" v-if="image.hostNodeIp">
                    <el-icon><Monitor /></el-icon>
                    <span class="asset-ip">{{ image.hostNodeIp }}</span>
                    <el-tag type="success" size="small">节点IP</el-tag>
                  </div>
                  <div class="node-info">
                    <span class="node-name">{{ image.hostNodeName }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else-if="!discovering && discoveredImages.length === 0" class="empty-state">
      <div class="empty-icon">🔍</div>
      <h3>暂无发现的镜像</h3>
      <div class="empty-guidance">
        <p>要发现镜像，请确保：</p>
        <ul class="guidance-list">
          <li>✅ 项目中已添加资产</li>
          <li>⚡ 资产已配置Docker平台</li>
          <li>🖥️ Docker平台状态正常且可访问</li>
          <li>🐳 Docker平台上有可用的镜像</li>
        </ul>
        <div class="action-buttons">
          <el-button type="primary" @click="startDiscovery" :loading="discovering">
            开始发现镜像
          </el-button>
          <el-button @click="goToAssetManagement">
            管理资产和节点
          </el-button>
        </div>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="discovering" class="loading-state">
      <el-icon class="is-loading"><Loading /></el-icon>
      <p>正在发现镜像，请稍候...</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Loading, Monitor } from '@element-plus/icons-vue'
import { containerCache, performanceMonitor } from '@/utils/containerCache'
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

const emit = defineEmits(['switch-to-assets'])

// 主题支持
const role = getUserRole() || ''
const themeClass = computed(() => role === 'blue' ? 'theme-blue' : 'theme-admin')

const discovering = ref(false)
const discoveredImages = ref([])  // 改为镜像
const filteredImages = ref([])     // 改为镜像
const discoveryNodes = ref([])
const projectAssets = ref([]) // 添加项目资产数据

// 筛选相关
const searchKeyword = ref('')
const filterNode = ref('')

// 统计信息 - 镜像没有运行状态
const totalImageCount = computed(() => discoveredImages.value.length)

// 项目概览统计
const projectAssetCount = ref(0)
const assetsWithNodesCount = ref(0)

// 跳转到资产管理页面
const goToAssetManagement = () => {
  // 触发事件让父组件切换到项目资产标签页
  emit('switch-to-assets')
}

const startDiscovery = async (useCache = true, showMessage = true) => {
  discovering.value = true

  // 清空之前的结果
  discoveredImages.value = []  // 改为镜像
  discoveryNodes.value = []
  projectAssets.value = [] // 清空资产数据

  console.log('[镜像发现] 开始发现镜像，项目ID:', props.projectId)

  try {
    // 显示开始发现的提示（只在不使用缓存且 showMessage 为 true 时显示）
    if (!useCache && showMessage) {
      ElMessage.info('开始发现镜像，请稍候...')
    }

    // 使用性能监控 - 调用镜像API
    const result = await performanceMonitor.measure('image-discovery', async () => {
      return await containerCache.getImages(props.projectId, useCache)
    })

    console.log('[镜像发现] 收到响应:', result)
    
    // 检查返回的数据结构
    if (!result) {
      throw new Error('服务器返回空数据')
    }

    // 处理镜像数据
    const images = Array.isArray(result.images) ? result.images : []
    console.log('[镜像发现] 获取到镜像数组:', images.length, '个')

    // 镜像去重（基于imageId）
    const uniqueImages = []
    const seenIds = new Set()

    for (const image of images) {
      const imageId = image.imageId
      if (imageId && !seenIds.has(imageId)) {
        seenIds.add(imageId)
        uniqueImages.push(image)
        console.log('[镜像发现] 镜像详情:', {
          repository: image.repository,
          tag: image.tag,
          imageId: imageId.substring(0, 12),
          size: image.size,
          created: image.created,
          node: image.hostNodeName
        })
      }
    }

    discoveredImages.value = uniqueImages
    discoveryNodes.value = Array.isArray(result.nodes) ? result.nodes : []

    console.log('[镜像发现] 最终显示', uniqueImages.length, '个镜像')
    console.log('[镜像发现] 节点信息:', discoveryNodes.value)
    
    // 从redistribution结果中提取资产数据
    if (result.redistributionResult && result.redistributionResult.redistributions) {
      projectAssets.value = result.redistributionResult.redistributions.map(redistribution => ({
        id: redistribution.assetId,
        name: redistribution.assetName,
        ip: redistribution.assetIp,
        preferredHostNodeId: redistribution.newNodeId,
        deploymentStrategy: redistribution.deploymentStrategy
      }))
    }
    
    // 更新项目统计信息
    projectAssetCount.value = result.totalAssets || 0
    assetsWithNodesCount.value = result.assetsWithNodes || 0
    
    // 处理新的统计信息
    if (result.explicitNodeAssets !== undefined) {
    }
    
    applyFilters()
    
    // 根据发现结果显示不同的消息
    if (discoveredImages.value.length > 0) {
      // Only show message if showMessage is true
      if (showMessage) {
        const message = useCache ?
          `📋 显示缓存的 ${discoveredImages.value.length} 个镜像` :
          `🎉 成功发现 ${discoveredImages.value.length} 个镜像`
        ElMessage.success(message)
      }
    } else if (discoveryNodes.value.length === 0) {
      // 🔧 改进：区分不同的错误情况
      if (result.error === 'invalid_nodes') {
        // 情况1：资产配置的节点已被删除
        const invalidNodeCount = result.invalidNodeIds?.length || 0
        const affectedAssets = result.assetsWithInvalidNodes || 0
        ElMessage.error(`❌ 发现失败：项目中有 ${affectedAssets} 个资产配置的节点已被删除（${invalidNodeCount} 个无效节点），请重新为这些资产配置部署节点`)
      } else if (result.totalAssets === 0) {
        // 情况2：项目中没有资产
        ElMessage.warning('📦 项目中还没有任何资产，请先添加资产')
      } else if (result.assetsWithNodes === 0) {
        // 情况3：资产未配置节点
        ElMessage.warning(`⚡ 项目中有 ${result.totalAssets} 个资产，但没有资产关联到主机节点。请在资产管理中为资产选择部署节点`)
      } else {
        // 情况4：通用错误
        ElMessage.warning('⚠️ 项目未关联任何主机节点，请先配置资产的部署节点')
      }
    } else {
      // 有节点但没发现容器
      if (result.message && result.anyStrategyAssets > 0) {
        ElMessage.info(`📊 ${result.message}`)
        ElMessage.warning('🔍 未发现任何容器，请检查节点连接状态和Docker服务')
      } else {
        ElMessage.warning('🔍 未发现任何容器，请检查节点连接状态和Docker服务')
      }
    }
  } catch (error) {
    console.log('容器发现请求异常:', error)

    // 区分网络错误和其他错误
    if (error.name === 'TypeError' && error.message.includes('fetch')) {
      console.log('网络连接失败')
    } else if (error.message.includes('timeout')) {
      console.log('请求超时')
    } else {
      ElMessage.error(`❌ 容器发现异常: ${error.message}`)
    }
  } finally {
    discovering.value = false
  }
}

const applyFilters = () => {
  let filtered = [...discoveredImages.value]

  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    filtered = filtered.filter(image => {
      const fullName = `${image.repository}:${image.tag}`.toLowerCase()
      return fullName.includes(keyword) ||
             image.repository.toLowerCase().includes(keyword) ||
             image.tag.toLowerCase().includes(keyword)
    })
  }

  if (filterNode.value) {
    filtered = filtered.filter(image => image.hostNodeId === filterNode.value)
  }

  filteredImages.value = filtered
}

const getNodeName = (nodeId) => {
  const node = discoveryNodes.value.find(n => n.id === nodeId)
  return node ? node.displayName : `节点-${nodeId}`
}

const getNodeIpPort = (nodeId) => {
  const node = discoveryNodes.value.find(n => n.id === nodeId)
  if (node) {
    // 尝试从节点关联的资产中获取资产IP，如果没有则显示物理IP
    const associatedAsset = findAssetByNodeId(nodeId)
    if (associatedAsset && associatedAsset.ip) {
      // 优先显示资产的业务IP
      return `${associatedAsset.ip} (业务IP)`
    } else {
      // 如果没有关联资产，显示物理主机IP
      return `${node.hostIp}:${node.dockerPort || 2375} (物理IP)`
    }
  }
  return '未知地址'
}

// 辅助函数：根据节点ID查找关联的资产
const findAssetByNodeId = (nodeId) => {
  // 使用从后端获取的资产数据
  if (projectAssets.value && Array.isArray(projectAssets.value)) {
    return projectAssets.value.find(asset => 
      asset.preferredHostNodeId === nodeId || 
      (asset.deploymentStrategy === 'any' && discoveryNodes.value.some(node => 
        node.id === nodeId && node.anyStrategyAssets > 0
      ))
    )
  }
  return null
}

const getNodeDeploymentStrategy = (nodeId) => {
  const node = discoveryNodes.value.find(n => n.id === nodeId)
  if (node) {
    const strategies = []
    if (node.nodeType === 'explicit') {
      strategies.push('固定节点')
    } else if (node.nodeType === 'anyStrategy') {
      strategies.push('智能分配')
    }
    
    if (node.explicitAssets && node.explicitAssets > 0) {
      strategies.push(`${node.explicitAssets}个固定资产`)
    }
    
    if (node.anyStrategyAssets && node.anyStrategyAssets > 0) {
      strategies.push(`${node.anyStrategyAssets}个灵活资产`)
    }
    
    const loadInfo = node.containerCount !== undefined ? ` (负载: ${node.containerCount})` : ''
    return strategies.join(' + ') + loadInfo
  }
  return null
}

// 新增方法：获取容器对应的资产IP
const getAssetIpForContainer = (container) => {
  const associatedAsset = findAssetByNodeId(container.nodeId)
  return associatedAsset?.ip || container.nodeId // 如果找不到资产IP，显示节点ID作为后备
}

// 新增方法：获取节点环境
const getNodeEnvironment = (nodeId) => {
  const node = discoveryNodes.value.find(n => n.id === nodeId)
  return node?.environment
}

// 新增方法：获取物理节点信息
const getPhysicalNodeInfo = (nodeId) => {
  const node = discoveryNodes.value.find(n => n.id === nodeId)
  if (node) {
    return `${node.hostIp}:${node.dockerPort || 2375}`
  }
  return '未知'
}

// 新增方法：获取节点负载信息
const getNodeLoadInfo = (nodeId) => {
  const node = discoveryNodes.value.find(n => n.id === nodeId)
  if (node && node.containerCount !== undefined) {
    const loadRatio = node.loadRatio ? Math.round(node.loadRatio * 100) : 0
    return `${node.containerCount}个容器 (${loadRatio}%)`
  }
  return null
}

const formatTime = (timestamp) => {
  return new Date(timestamp).toLocaleString()
}

// 仅加载本地缓存数据（不发起API请求）
const loadCachedData = () => {
  const cacheKey = containerCache.generateKey('images', { projectId: props.projectId })
  const cached = containerCache.get(cacheKey)

  if (cached) {
    // 有缓存数据，直接显示
    discoveredImages.value = Array.isArray(cached.images) ? cached.images : []
    discoveryNodes.value = Array.isArray(cached.nodes) ? cached.nodes : []

    // 更新项目统计信息
    projectAssetCount.value = cached.totalAssets || 0
    assetsWithNodesCount.value = cached.assetsWithNodes || 0

    applyFilters()
  }
}

onMounted(() => {
  // 不自动探测，等待用户点击"开始发现"按钮
})

onUnmounted(() => {
  // 组件卸载时清理性能监控数据
  performanceMonitor.clear()
})
</script>

<style scoped>
/* ============================================
   Apple Elegant White Style - 容器管理页面
   ============================================ */

/* CSS Variables - 复用全局变量 */
:root {
  --font-apple: -apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", "Helvetica Neue", sans-serif;
}

.project-container-discovery {
  background: transparent;
  border-radius: 0;
  padding: 0;
  font-family: var(--font-apple);
}

/* ============================================
   Discovery Header - 页面标题
   ============================================ */
.discovery-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-lg, 24px);
  padding: var(--spacing-lg, 24px);
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.95) 0%,
    rgba(248, 248, 248, 0.85) 100%);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 0.5px solid var(--apple-border, rgba(0, 0, 0, 0.04));
  border-radius: var(--radius-lg, 20px);
  box-shadow: var(--shadow-card, 0 4px 16px rgba(0, 0, 0, 0.06));
}

.discovery-header h3 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: var(--apple-text-primary, #1d1d1f);
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
  border-radius: var(--radius-sm, 12px);
  font-weight: 600;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
  border: none;
}

.header-actions :deep(.el-button--primary) {
  background: linear-gradient(135deg, var(--apple-blue, #007aff) 0%, #0051d5 100%);
  color: white;
}

.header-actions :deep(.el-button--primary:hover) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 122, 255, 0.4);
}

.header-actions :deep(.el-button--success) {
  background: linear-gradient(135deg, var(--apple-green, #34c759) 0%, #28a745 100%);
  color: white;
}

.header-actions :deep(.el-button--success:hover) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(52, 199, 89, 0.4);
}

.header-actions :deep(.el-button:not(.el-button--primary):not(.el-button--success)) {
  background: rgba(120, 120, 128, 0.12);
  color: var(--apple-text-primary, #1d1d1f);
}

.header-actions :deep(.el-button:not(.el-button--primary):not(.el-button--success):hover) {
  background: rgba(120, 120, 128, 0.18);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

/* ============================================
   Project Overview - 项目概览
   ============================================ */
.project-overview {
  margin-bottom: var(--spacing-lg, 24px);
}

.overview-card {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.98) 0%,
    rgba(248, 248, 248, 0.95) 100%);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: var(--radius-md, 16px);
  padding: var(--spacing-lg, 24px);
  box-shadow: var(--shadow-card, 0 4px 16px rgba(0, 0, 0, 0.06));
  border: 0.5px solid var(--apple-border, rgba(0, 0, 0, 0.04));
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

.overview-card:hover {
  box-shadow: var(--shadow-card-hover, 0 8px 32px rgba(0, 0, 0, 0.12));
  transform: translateY(-2px);
}

.overview-content {
  display: flex;
  gap: 32px;
  flex-wrap: wrap;
}

.overview-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.overview-label {
  font-size: 14px;
  color: var(--apple-text-secondary, #6e6e73);
  font-weight: 500;
}

.overview-value {
  font-size: 18px;
  font-weight: 700;
  color: var(--apple-text-primary, #1d1d1f);
  letter-spacing: -0.3px;
}

/* ============================================
   Discovery Stats - 发现统计
   ============================================ */
.discovery-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: var(--spacing-md, 16px);
  margin-bottom: var(--spacing-lg, 24px);
}

.stat-item {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.98) 0%,
    rgba(248, 248, 248, 0.95) 100%);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: var(--radius-md, 16px);
  padding: var(--spacing-md, 16px);
  box-shadow: var(--shadow-card, 0 4px 16px rgba(0, 0, 0, 0.06));
  border: 0.5px solid var(--apple-border, rgba(0, 0, 0, 0.04));
  border-left: 4px solid #ddd;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.stat-item:hover {
  box-shadow: var(--shadow-card-hover, 0 8px 32px rgba(0, 0, 0, 0.12));
  transform: translateY(-2px);
}

.stat-item:nth-child(1) {
  border-left-color: var(--apple-blue, #007aff);
}

.stat-item:nth-child(2) {
  border-left-color: var(--apple-green, #34c759);
}

.stat-item:nth-child(3) {
  border-left-color: var(--apple-orange, #ff9500);
}

.stat-label {
  font-size: 13px;
  color: var(--apple-text-secondary, #6e6e73);
  font-weight: 600;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--apple-text-primary, #1d1d1f);
  letter-spacing: -1px;
  line-height: 1;
}

.stat-value.running {
  color: var(--apple-green, #34c759);
}

.stat-value.stopped {
  color: var(--apple-orange, #ff9500);
}

/* ============================================
   Filter Bar - 筛选区域
   ============================================ */
.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: var(--spacing-lg, 24px);
  padding: var(--spacing-md, 16px);
  background: linear-gradient(135deg,
    rgba(245, 245, 247, 0.6) 0%,
    rgba(255, 255, 255, 0.4) 100%);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 0.5px solid var(--apple-border, rgba(0, 0, 0, 0.04));
  border-radius: var(--radius-md, 16px);
  flex-wrap: wrap;
}

.search-input {
  width: 250px;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: var(--radius-sm, 12px);
  transition: all 0.3s ease;
}

.search-input :deep(.el-input__wrapper:hover) {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.search-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 3px rgba(0, 122, 255, 0.1);
}

.filter-select {
  width: 140px;
}

.filter-select :deep(.el-input__wrapper) {
  border-radius: var(--radius-sm, 12px);
}

.filter-bar :deep(.el-button) {
  height: 40px;
  min-width: 100px;
  font-size: 14px;
  padding: 0 20px;
  border-radius: var(--radius-sm, 12px);
  font-weight: 600;
}

.filter-bar :deep(.el-button--primary) {
  background: var(--apple-blue, #007aff);
  border-color: var(--apple-blue, #007aff);
}

.filter-bar :deep(.el-button--primary:hover) {
  background: #0051d5;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.3);
}

/* ============================================
   Container List - 容器列表
   ============================================ */
.containers-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(380px, 1fr));
  gap: var(--spacing-md, 16px);
}

.container-card {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.98) 0%,
    rgba(248, 248, 248, 0.95) 100%);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: var(--radius-md, 16px);
  padding: var(--spacing-md, 16px);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
  border: 0.5px solid var(--apple-border, rgba(0, 0, 0, 0.04));
  border-left: 4px solid #ddd;
  box-shadow: var(--shadow-card, 0 4px 16px rgba(0, 0, 0, 0.06));
  position: relative;
  overflow: hidden;
}

.container-card::before {
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

.container-card:hover {
  box-shadow: var(--shadow-card-hover, 0 8px 32px rgba(0, 0, 0, 0.12));
  transform: translateY(-4px);
}

.container-card:hover::before {
  opacity: 1;
}

.container-card.status-running {
  border-left-color: var(--apple-green, #34c759);
}

.container-card.status-exited {
  border-left-color: var(--apple-orange, #ff9500);
}

.container-card.status-paused {
  border-left-color: var(--apple-blue, #007aff);
}

.container-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--spacing-sm, 16px);
}

.container-title {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
}

.container-name {
  font-weight: 700;
  color: var(--apple-text-primary, #1d1d1f);
  font-size: 16px;
  letter-spacing: -0.3px;
}

.container-title :deep(.el-tag) {
  border-radius: 8px;
  font-weight: 600;
  padding: 4px 10px;
  border: none;
  font-size: 12px;
}

.container-title :deep(.el-tag--success) {
  background: linear-gradient(135deg, var(--apple-green, #34c759), #28a745);
  color: white;
}

.container-title :deep(.el-tag--warning) {
  background: linear-gradient(135deg, var(--apple-orange, #ff9500), #D97706);
  color: white;
}

.container-title :deep(.el-tag--info) {
  background: linear-gradient(135deg, #3B82F6, #2563EB);
  color: white;
}

.container-info {
  margin-bottom: var(--spacing-sm, 16px);
}

.info-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 13px;
  padding: 6px 0;
  border-bottom: 0.5px solid rgba(0, 0, 0, 0.04);
}

.info-row:last-child {
  border-bottom: none;
}

.info-row .label {
  color: var(--apple-text-secondary, #6e6e73);
  font-weight: 600;
  min-width: 80px;
  font-size: 12px;
}

.info-row .value {
  color: var(--apple-text-primary, #1d1d1f);
  text-align: right;
  word-break: break-all;
  font-weight: 500;
}

.docker-image {
  font-family: 'SF Mono', 'Monaco', 'Consolas', monospace;
  background: linear-gradient(135deg, rgba(0, 122, 255, 0.08), rgba(0, 122, 255, 0.05));
  padding: 4px 8px;
  border-radius: 6px;
  color: var(--apple-blue, #007aff);
  font-size: 11px;
  border: 0.5px solid rgba(0, 122, 255, 0.15);
}

.container-footer {
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  padding-top: var(--spacing-sm, 16px);
}

.labels {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.label-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.label-tag {
  font-size: 11px;
  border-radius: 6px;
}

/* ============================================
   Deployment Info Details - 部署信息详情
   ============================================ */
.deployment-info {
  flex-direction: column !important;
  align-items: flex-start !important;
}

.deployment-details {
  width: 100%;
  margin-top: 6px;
}

.main-info {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 10px;
}

.asset-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  background: linear-gradient(135deg,
    rgba(0, 122, 255, 0.08) 0%,
    rgba(0, 122, 255, 0.05) 100%);
  border-radius: 8px;
  border: 1px solid rgba(0, 122, 255, 0.2);
}

.asset-ip {
  font-weight: 700;
  color: var(--apple-blue, #007aff);
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Roboto Mono', monospace;
  font-size: 13px;
  letter-spacing: -0.3px;
}

.node-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.node-name {
  font-weight: 700;
  color: var(--apple-text-primary, #1d1d1f);
  font-size: 14px;
}

.node-info :deep(.el-tag) {
  border-radius: 6px;
  font-weight: 600;
  border: none;
}

/* 技术详情 */
.technical-details {
  margin-top: 10px;
  padding: 10px 14px;
  background: linear-gradient(135deg,
    rgba(245, 245, 247, 0.6) 0%,
    rgba(255, 255, 255, 0.4) 100%);
  border-radius: 8px;
  border: 0.5px solid rgba(0, 0, 0, 0.06);
}

.tech-detail-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
  font-size: 12px;
}

.tech-detail-item:last-child {
  margin-bottom: 0;
}

.tech-label {
  color: var(--apple-text-secondary, #6e6e73);
  font-weight: 600;
}

.tech-value {
  color: var(--apple-text-primary, #1d1d1f);
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Roboto Mono', monospace;
  font-weight: 500;
}

/* ============================================
   Empty & Loading States - 空状态和加载状态
   ============================================ */
.empty-state, .loading-state {
  text-align: center;
  padding: 60px 20px;
  color: var(--apple-text-secondary, #6e6e73);
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.95) 0%,
    rgba(248, 248, 248, 0.85) 100%);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: var(--radius-lg, 20px);
  border: 0.5px solid var(--apple-border, rgba(0, 0, 0, 0.04));
  box-shadow: var(--shadow-card, 0 4px 16px rgba(0, 0, 0, 0.06));
}

.empty-icon {
  font-size: 64px;
  margin-bottom: var(--spacing-lg, 24px);
  filter: drop-shadow(0 4px 8px rgba(0, 0, 0, 0.1));
}

.empty-state h3 {
  font-size: 20px;
  font-weight: 700;
  color: var(--apple-text-primary, #1d1d1f);
  margin: 0 0 var(--spacing-sm, 16px) 0;
  letter-spacing: -0.5px;
}

.empty-guidance {
  max-width: 560px;
  margin: 0 auto;
}

.empty-guidance > p {
  font-size: 15px;
  color: var(--apple-text-secondary, #6e6e73);
  margin-bottom: var(--spacing-md, 16px);
  font-weight: 600;
}

.guidance-list {
  text-align: left;
  list-style: none;
  padding: 0;
  margin: var(--spacing-md, 16px) 0;
  background: rgba(245, 245, 247, 0.5);
  border-radius: var(--radius-sm, 12px);
  padding: var(--spacing-md, 16px);
}

.guidance-list li {
  padding: 10px 0;
  font-size: 14px;
  color: var(--apple-text-secondary, #6e6e73);
  font-weight: 500;
  line-height: 1.6;
}

.action-buttons {
  margin-top: var(--spacing-lg, 24px);
  display: flex;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
}

.action-buttons :deep(.el-button) {
  height: 44px;
  min-width: 150px;
  font-size: 15px;
  padding: 0 24px;
  border-radius: var(--radius-md, 16px);
  font-weight: 600;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
  border: none;
}

.action-buttons :deep(.el-button--primary) {
  background: linear-gradient(135deg, var(--apple-blue, #007aff) 0%, #0051d5 100%);
  color: white;
}

.action-buttons :deep(.el-button--primary:hover) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 122, 255, 0.4);
}

.action-buttons :deep(.el-button:not(.el-button--primary)) {
  background: rgba(120, 120, 128, 0.12);
  color: var(--apple-text-primary, #1d1d1f);
}

.action-buttons :deep(.el-button:not(.el-button--primary):hover) {
  background: rgba(120, 120, 128, 0.18);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

/* ============================================
   Responsive Design - 响应式设计
   ============================================ */
@media (max-width: 1024px) {
  .containers-grid {
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  }

  .discovery-stats {
    grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  }
}

@media (max-width: 768px) {
  .discovery-header {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--spacing-md, 16px);
  }

  .header-actions {
    width: 100%;
    flex-wrap: wrap;
  }

  .header-actions :deep(.el-button) {
    flex: 1;
    min-width: 100px;
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

  .containers-grid {
    grid-template-columns: 1fr;
  }

  .discovery-stats {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 576px) {
  .discovery-header {
    padding: var(--spacing-md, 16px);
  }

  .discovery-header h3 {
    font-size: 20px;
  }

  .overview-card {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--spacing-md, 16px);
  }

  .overview-content {
    flex-direction: column;
    gap: var(--spacing-sm, 16px);
  }

  .container-card {
    padding: var(--spacing-sm, 16px);
  }
}

/* ============================================
   蓝队主题 - Blue Team Theme
   ============================================ */

/* 页面标题区域 */
.project-container-discovery.theme-blue .discovery-header {
  background: linear-gradient(135deg,
    rgba(20, 30, 50, 0.85) 0%,
    rgba(13, 26, 45, 0.95) 100%) !important;
  border: 1px solid rgba(70, 130, 180, 0.35) !important;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.45),
              0 0 20px rgba(70, 130, 180, 0.08) !important;
}

.project-container-discovery.theme-blue .discovery-header h3 {
  color: #ffffff !important;
  font-weight: 700 !important;
  text-shadow: 0 2px 8px rgba(70, 130, 180, 0.3) !important;
}

.project-container-discovery.theme-blue .header-actions :deep(.el-button--primary) {
  background: linear-gradient(135deg, #4682b4 0%, #1e90ff 100%) !important;
  border-color: rgba(70, 130, 180, 0.6) !important;
  color: #ffffff !important;
  box-shadow: 0 4px 15px rgba(70, 130, 180, 0.2),
              inset 0 1px 0 rgba(255, 255, 255, 0.1) !important;
}

.project-container-discovery.theme-blue .header-actions :deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, #5fa3d4 0%, #00d4ff 100%) !important;
  border-color: rgba(70, 130, 180, 0.8) !important;
  box-shadow: 0 8px 25px rgba(70, 130, 180, 0.4),
              0 0 20px rgba(30, 144, 255, 0.3),
              inset 0 1px 0 rgba(255, 255, 255, 0.15) !important;
  transform: translateY(-2px) !important;
}

/* 项目概览卡片 */
.project-container-discovery.theme-blue .overview-card {
  background: linear-gradient(135deg,
    rgba(20, 30, 50, 0.85) 0%,
    rgba(13, 26, 45, 0.95) 100%) !important;
  border: 1px solid rgba(70, 130, 180, 0.35) !important;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3),
              0 0 10px rgba(70, 130, 180, 0.1) !important;
}

.project-container-discovery.theme-blue .overview-label {
  color: rgba(255, 255, 255, 0.7) !important;
}

.project-container-discovery.theme-blue .overview-value {
  color: #ffffff !important;
  font-weight: 600 !important;
}

/* 发现统计 */
.project-container-discovery.theme-blue .discovery-stats {
  background: rgba(20, 30, 50, 0.5) !important;
  border: 1px solid rgba(70, 130, 180, 0.25) !important;
}

.project-container-discovery.theme-blue .stat-label {
  color: rgba(255, 255, 255, 0.7) !important;
}

.project-container-discovery.theme-blue .stat-value {
  color: #ffffff !important;
  font-weight: 600 !important;
}

.project-container-discovery.theme-blue .stat-value.running {
  color: #34c759 !important;
  text-shadow: 0 0 10px rgba(52, 199, 89, 0.4) !important;
}

.project-container-discovery.theme-blue .stat-value.stopped {
  color: #ff9500 !important;
  text-shadow: 0 0 10px rgba(255, 149, 0, 0.4) !important;
}

/* 发现统计卡片 */
.project-container-discovery.theme-blue .stat-item {
  background: linear-gradient(135deg,
    rgba(20, 30, 50, 0.85) 0%,
    rgba(13, 26, 45, 0.95) 100%) !important;
  border: 1px solid rgba(70, 130, 180, 0.35) !important;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3),
              0 0 10px rgba(70, 130, 180, 0.1) !important;
}

.project-container-discovery.theme-blue .stat-item:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4),
              0 0 20px rgba(70, 130, 180, 0.2) !important;
  transform: translateY(-2px) !important;
}

.project-container-discovery.theme-blue .stat-item:nth-child(1) {
  border-left-color: #00d4ff !important;
  border-left-width: 4px !important;
}

.project-container-discovery.theme-blue .stat-item:nth-child(2) {
  border-left-color: #34c759 !important;
  border-left-width: 4px !important;
}

.project-container-discovery.theme-blue .stat-item:nth-child(3) {
  border-left-color: #ff9500 !important;
  border-left-width: 4px !important;
}

/* 筛选栏 */
.project-container-discovery.theme-blue .filter-bar {
  background: rgba(20, 30, 50, 0.5) !important;
  border: 1px solid rgba(70, 130, 180, 0.25) !important;
  box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.2) !important;
}

.project-container-discovery.theme-blue .filter-bar :deep(.el-input__wrapper) {
  background: rgba(20, 30, 50, 0.6) !important;
  border: 1px solid rgba(70, 130, 180, 0.3) !important;
  box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.2) !important;
}

.project-container-discovery.theme-blue .filter-bar :deep(.el-input__inner) {
  color: #ffffff !important;
}

.project-container-discovery.theme-blue .filter-bar :deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.5) !important;
}

.project-container-discovery.theme-blue .filter-bar :deep(.el-input__wrapper.is-focus) {
  border-color: rgba(70, 130, 180, 0.6) !important;
  box-shadow: 0 0 0 2px rgba(70, 130, 180, 0.15),
              inset 0 1px 3px rgba(0, 0, 0, 0.2) !important;
}

.project-container-discovery.theme-blue .filter-bar :deep(.el-input__wrapper:hover) {
  border-color: rgba(70, 130, 180, 0.5) !important;
}

.project-container-discovery.theme-blue .filter-bar :deep(.el-select .el-input__wrapper) {
  background: rgba(20, 30, 50, 0.6) !important;
  border: 1px solid rgba(70, 130, 180, 0.3) !important;
  box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.2) !important;
}

.project-container-discovery.theme-blue .filter-bar :deep(.el-select .el-input__wrapper:hover) {
  border-color: rgba(70, 130, 180, 0.5) !important;
  background: rgba(30, 40, 60, 0.7) !important;
}

.project-container-discovery.theme-blue .filter-bar :deep(.el-select .el-input__wrapper.is-focus) {
  border-color: rgba(70, 130, 180, 0.6) !important;
  background: rgba(30, 40, 60, 0.8) !important;
}

.project-container-discovery.theme-blue .filter-bar :deep(.el-button--primary) {
  background: linear-gradient(135deg, #4682b4 0%, #1e90ff 100%) !important;
  border-color: rgba(70, 130, 180, 0.6) !important;
  color: #ffffff !important;
  box-shadow: 0 4px 15px rgba(70, 130, 180, 0.2) !important;
}

.project-container-discovery.theme-blue .filter-bar :deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, #5fa3d4 0%, #00d4ff 100%) !important;
  box-shadow: 0 8px 25px rgba(70, 130, 180, 0.4) !important;
  transform: translateY(-2px) !important;
}

/* 容器卡片 */
.project-container-discovery.theme-blue .container-card {
  background: linear-gradient(135deg,
    rgba(20, 30, 50, 0.85) 0%,
    rgba(13, 26, 45, 0.95) 100%) !important;
  border: 1px solid rgba(70, 130, 180, 0.35) !important;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3),
              0 0 10px rgba(70, 130, 180, 0.1) !important;
}

.project-container-discovery.theme-blue .container-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4),
              0 0 20px rgba(70, 130, 180, 0.2) !important;
  transform: translateY(-4px) !important;
}

.project-container-discovery.theme-blue .container-card.status-running {
  border-left-color: #34c759 !important;
  border-left-width: 4px !important;
}

.project-container-discovery.theme-blue .container-card.status-exited {
  border-left-color: #ff3b30 !important;
  border-left-width: 4px !important;
}

.project-container-discovery.theme-blue .container-card.status-paused {
  border-left-color: #ff9500 !important;
  border-left-width: 4px !important;
}

.project-container-discovery.theme-blue .container-name {
  color: #ffffff !important;
  font-weight: 700 !important;
}

.project-container-discovery.theme-blue .info-row .label {
  color: rgba(255, 255, 255, 0.7) !important;
}

.project-container-discovery.theme-blue .info-row .value {
  color: #ffffff !important;
}

.project-container-discovery.theme-blue .docker-image {
  color: #00d4ff !important;
  font-weight: 500 !important;
}

.project-container-discovery.theme-blue .deployment-details {
  background: rgba(10, 20, 40, 0.6) !important;
  border: 1px solid rgba(70, 130, 180, 0.25) !important;
}

.project-container-discovery.theme-blue .asset-ip {
  color: #ffffff !important;
}

.project-container-discovery.theme-blue .node-name {
  color: #ffffff !important;
}

.project-container-discovery.theme-blue .technical-details {
  background: rgba(10, 15, 30, 0.8) !important;
  border: 1px solid rgba(70, 130, 180, 0.2) !important;
}

.project-container-discovery.theme-blue .tech-label {
  color: rgba(255, 255, 255, 0.7) !important;
}

.project-container-discovery.theme-blue .tech-value {
  color: #ffffff !important;
}

/* Element Plus 标签 */
.project-container-discovery.theme-blue :deep(.el-tag) {
  border: none !important;
  color: #ffffff !important;
}

.project-container-discovery.theme-blue :deep(.el-tag--success) {
  background: linear-gradient(135deg, #34c759, #28a745) !important;
}

.project-container-discovery.theme-blue :deep(.el-tag--info) {
  background: linear-gradient(135deg, #3B82F6, #2563EB) !important;
}

.project-container-discovery.theme-blue :deep(.el-tag--warning) {
  background: linear-gradient(135deg, #ff9500, #D97706) !important;
}

.project-container-discovery.theme-blue :deep(.el-tag--danger) {
  background: linear-gradient(135deg, #ff3b30, #DC2626) !important;
}

/* 标签标签 */
.project-container-discovery.theme-blue .label-tag {
  background: linear-gradient(135deg, rgba(70, 130, 180, 0.3), rgba(70, 130, 180, 0.4)) !important;
  border: 0.5px solid rgba(70, 130, 180, 0.5) !important;
  color: #ffffff !important;
}

/* 空状态 */
.project-container-discovery.theme-blue .empty-state {
  color: rgba(255, 255, 255, 0.7) !important;
}

.project-container-discovery.theme-blue .empty-state h3 {
  color: #ffffff !important;
}

.project-container-discovery.theme-blue .empty-icon {
  filter: drop-shadow(0 4px 12px rgba(70, 130, 180, 0.3)) !important;
}

.project-container-discovery.theme-blue .guidance-list {
  color: rgba(255, 255, 255, 0.8) !important;
}

.project-container-discovery.theme-blue .action-buttons :deep(.el-button--primary) {
  background: linear-gradient(135deg, #4682b4 0%, #1e90ff 100%) !important;
  border-color: rgba(70, 130, 180, 0.6) !important;
  color: #ffffff !important;
}

.project-container-discovery.theme-blue .action-buttons :deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, #5fa3d4 0%, #00d4ff 100%) !important;
  box-shadow: 0 8px 25px rgba(70, 130, 180, 0.4) !important;
  transform: translateY(-2px) !important;
}

.project-container-discovery.theme-blue .action-buttons :deep(.el-button:not(.el-button--primary)) {
  background: rgba(20, 30, 50, 0.6) !important;
  border: 1px solid rgba(70, 130, 180, 0.3) !important;
  color: #a8d8ea !important;
}

.project-container-discovery.theme-blue .action-buttons :deep(.el-button:not(.el-button--primary):hover) {
  background: rgba(70, 130, 180, 0.2) !important;
  border-color: rgba(70, 130, 180, 0.5) !important;
  transform: translateY(-2px) !important;
}

/* 加载状态 */
.project-container-discovery.theme-blue .loading-state {
  color: rgba(255, 255, 255, 0.7) !important;
}

.project-container-discovery.theme-blue .loading-state :deep(.el-icon) {
  color: #00d4ff !important;
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