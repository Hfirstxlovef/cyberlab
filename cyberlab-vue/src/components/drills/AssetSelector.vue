<template>
  <div class="asset-selector">
    <div class="selector-header">
      <h3>选择演练资产</h3>
      <div class="filter-controls">
        <select v-model="selectedVisibility" @change="filterAssets" class="type-filter">
          <option value="">所有可见性</option>
          <option value="red">Red</option>
          <option value="blue">Blue</option>
          <option value="both">Both</option>
        </select>
        <select v-model="selectedTarget" @change="filterAssets" class="type-filter">
          <option value="">所有资产</option>
          <option value="true">靶场资产</option>
          <option value="false">普通资产</option>
        </select>
        <select v-model="selectedHostNode" @change="filterAssets" class="type-filter">
          <option value="">所有节点</option>
          <template v-for="node in availableNodes" :key="node?.id || `node-${Math.random()}`">
            <option v-if="node && node.id" :value="node.id">
              {{ node?.displayName || node?.name || '未知节点' }}
            </option>
          </template>
        </select>
        <input 
          v-model="searchQuery" 
          @input="filterAssets" 
          type="text" 
          placeholder="搜索资产..." 
          class="search-input"
        />
      </div>
    </div>

    <div class="assets-grid">
      <div 
        v-for="asset in filteredAssets" 
        :key="asset.id"
        class="asset-card"
        :class="{ 'selected': isSelected(asset.id) }"
        @click="toggleAsset(asset)"
      >
        <div class="asset-info">
          <h4>{{ asset.name }}</h4>
          <p class="asset-type">{{ getVisibilityLabel(asset.visibility) }}</p>
          <p class="asset-description">{{ asset.ip }} | {{ asset.company || '未分组' }}</p>
          <p class="asset-notes">{{ asset.notes || '无备注' }}</p>
          
          <!-- 新增：容器信息预览 -->
          <div v-if="asset.discoveredImages?.length > 0 || asset.assetType === 'container'" class="container-info">
            <!-- 显示发现的镜像列表 -->
            <div v-if="asset.discoveredImages?.length > 0" class="discovered-images">
              <div class="container-info-item">
                <span class="info-icon">🐳</span>
                <span class="info-label">可用镜像:</span>
              </div>
              <div v-for="image in asset.discoveredImages" :key="image.imageId" class="image-item">
                <span class="docker-image-name">{{ image.repository }}:{{ image.tag }}</span>
                <span class="image-size">{{ image.size }}</span>
              </div>
            </div>
            <!-- 如果没有发现镜像，显示静态配置 -->
            <div v-else-if="asset.dockerImage" class="container-info-item">
              <span class="info-icon">🐳</span>
              <span class="info-label">镜像:</span>
              <span class="docker-image-name">{{ asset.dockerImage }}</span>
            </div>
            <div class="container-info-item" v-if="asset.containerPorts">
              <span class="info-icon">🔌</span>
              <span class="info-label">端口:</span>
              <span class="port-config">{{ formatPortConfig(asset.containerPorts) }}</span>
            </div>
            <div class="container-info-item" v-if="asset.resourceLimitCpu || asset.resourceLimitMemory">
              <span class="info-icon">📊</span>
              <span class="info-label">资源:</span>
              <span class="resource-limit">
                {{ asset.resourceLimitCpu ? `${asset.resourceLimitCpu}m CPU` : '' }}
                {{ asset.resourceLimitMemory ? `${asset.resourceLimitMemory}MB RAM` : '' }}
              </span>
            </div>
          </div>
          
          <!-- 显示首选部署节点信息 -->
          <p class="asset-deploy-info" v-if="asset.preferredHostNodeName">
            <span class="deploy-label">首选节点:</span>
            <span class="node-name" :class="getNodeStatusClass(asset.preferredHostNodeId)">
              {{ asset.preferredHostNodeName }}
            </span>
          </p>
          <div class="asset-status">
            <span :class="['status-badge', getStatusClass(asset)]">
              {{ getStatusText(asset) }}
            </span>
            <span v-if="asset.isTarget" class="target-badge">
              🎯 靶场
            </span>
          </div>
        </div>
      </div>
    </div>

    <div class="selector-footer">
      <div class="selected-summary">
        <span>已选择 {{ Object.keys(selectedAssets).length }} 个资产</span>
      </div>
      <div class="action-buttons">
        <button @click="$emit('cancel')" class="btn-cancel">取消</button>
        <button @click="confirmSelection" class="btn-confirm" :disabled="Object.keys(selectedAssets).length === 0">
          确认选择
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, watch } from 'vue'
import { getAssetList } from '@/api/asset'
import { getAvailableHostNodes } from '@/api/hostNodes'
import { discoverAssetImages } from '@/api/containers'

export default {
  name: 'AssetSelector',
  props: {
    rangeId: {
      type: Number,
      required: true
    },
    preSelectedAssets: {
      type: Array,
      default: () => []
    }
  },
  emits: ['confirm', 'cancel'],
  setup(props, { emit }) {
    const assets = ref([])
    const availableNodes = ref([]) // 新增：可用节点列表
    const selectedAssets = ref({})
    const selectedVisibility = ref('')
    const selectedTarget = ref('')
    const selectedHostNode = ref('') // 新增：选中的主机节点筛选
    const searchQuery = ref('')
    const loading = ref(false)

    const filteredAssets = computed(() => {
      let result = assets.value || []
      
      // 按可见性筛选
      if (selectedVisibility.value) {
        result = result.filter(asset => asset.visibility === selectedVisibility.value)
      }
      
      // 按靶场类型筛选
      if (selectedTarget.value !== '') {
        const isTarget = selectedTarget.value === 'true'
        result = result.filter(asset => asset.isTarget === isTarget)
      }
      
      // 按主机节点筛选
      if (selectedHostNode.value) {
        result = result.filter(asset => 
          asset.preferredHostNodeId === parseInt(selectedHostNode.value)
        )
      }
      
      // 按关键词搜索
      if (searchQuery.value) {
        const keyword = searchQuery.value.toLowerCase()
        result = result.filter(asset => 
          (asset.name && asset.name.toLowerCase().includes(keyword)) ||
          (asset.ip && asset.ip.toLowerCase().includes(keyword)) ||
          (asset.company && asset.company.toLowerCase().includes(keyword)) ||
          (asset.owner && asset.owner.toLowerCase().includes(keyword)) ||
          (asset.notes && asset.notes.toLowerCase().includes(keyword)) ||
          (asset.preferredHostNodeName && asset.preferredHostNodeName.toLowerCase().includes(keyword))
        )
      }
      
      return result
    })

    const loadAssets = async () => {
      loading.value = true
      try {
        // 首先获取演练信息，确定关联的项目
        let drillInfo = null
        if (props.rangeId) {
          try {
            const drillResponse = await fetch(`/api/range/${props.rangeId}`, {
              headers: {
                'Authorization': `Bearer ${localStorage.getItem('token')}`
              }
            })
            if (drillResponse.ok) {
              drillInfo = await drillResponse.json()
            }
          } catch (error) {
          }
        }

        // 获取所有资产
        const response = await getAssetList()
        let allAssets = Array.isArray(response) ? response : (response.data || [])

        // 如果演练关联了特定项目，只显示该项目的资产
        if (drillInfo && drillInfo.topologyProjectId) {
          allAssets = allAssets.filter(asset => {
            // 根据项目名称匹配（topologyProjectId 通常是项目名称）
            const assetProject = `${asset.company || '未知企业'}｜${asset.project || '未分组'}`
            return assetProject === drillInfo.topologyProjectId ||
                   asset.project === drillInfo.topologyProjectId ||
                   asset.topologyProjectId === drillInfo.topologyProjectId
          })
        }

        // 🔧 新增：为每个资产加载镜像信息
        console.log('开始为资产加载Docker镜像...', allAssets.length, '个资产')

        // 并发加载所有资产的镜像，提高性能
        await Promise.all(
          allAssets.map(async (asset) => {
            // 检查资产是否配置了Docker平台
            // 检查dockerApiEnabled字段或者assetPlatform字段来判断是否配置了Docker
            const hasDockerConfig = asset.dockerApiEnabled === true ||
                                   asset.assetPlatform === 'docker' ||
                                   asset.assetPlatform === 'both' ||
                                   asset.dockerPort !== null

            if (hasDockerConfig) {
              try {
                console.log(`正在为资产 ${asset.name} (ID: ${asset.id}) 加载镜像...`)
                const imageResponse = await discoverAssetImages(asset.id)

                // 将镜像数据添加到资产对象
                if (imageResponse && imageResponse.images) {
                  asset.discoveredImages = imageResponse.images
                  console.log(`资产 ${asset.name} 发现 ${imageResponse.images.length} 个镜像`)
                } else {
                  asset.discoveredImages = []
                }
              } catch (error) {
                console.error(`加载资产 ${asset.name} 的镜像失败:`, error)
                asset.discoveredImages = []
              }
            } else {
              // 没有Docker配置的资产，设置空镜像列表
              asset.discoveredImages = []
            }
          })
        )

        console.log('所有资产镜像加载完成')
        assets.value = allAssets
      } catch (error) {
        console.error('加载资产列表失败:', error)
        assets.value = []
      } finally {
        loading.value = false
      }
    }
    
    // 新增：加载可用节点列表
    const loadAvailableNodes = async () => {
      try {
        const nodes = await getAvailableHostNodes()
        availableNodes.value = Array.isArray(nodes) ? nodes : []
      } catch (error) {
        availableNodes.value = []
      }
    }

    const toggleAsset = (asset) => {
      if (isSelected(asset.id)) {
        delete selectedAssets.value[asset.id]
      } else {
        // 智能节点推荐
        let defaultTargetNodeId = null
        if (asset.preferredHostNodeId) {
          // 使用资产的首选节点
          const preferredNode = availableNodes.value.find(node => node.id === asset.preferredHostNodeId)
          if (preferredNode && preferredNode.status === 'active') {
            defaultTargetNodeId = asset.preferredHostNodeId
          }
        }
        
        // 如果首选节点不可用，进行智能推荐
        if (!defaultTargetNodeId && Array.isArray(availableNodes.value) && availableNodes.value.length > 0) {
          const recommendedNode = getRecommendedNode(asset)
          if (recommendedNode) {
            defaultTargetNodeId = recommendedNode.id
          }
        }
        
        selectedAssets.value[asset.id] = {
          assetId: asset.id,
          containerName: `${asset.name}-${Date.now()}`,
          networkConfig: asset.notes || '', // 使用备注作为网络配置
          targetHostNodeId: defaultTargetNodeId, // 新增：目标部署节点
          asset: asset
        }
      }
    }

    const isSelected = (assetId) => {
      return selectedAssets.value[assetId] !== undefined
    }

    const getAssetIcon = (visibility) => {
      const iconMap = {
        red: '/icons/red-team.png',
        blue: '/icons/blue-team.png',
        both: '/icons/shared.png'
      }
      return iconMap[visibility] || '/icons/pc.png'
    }
    
    const getVisibilityLabel = (visibility) => {
      const labelMap = {
        red: 'Red Team',
        blue: 'Blue Team', 
        both: '共享资产'
      }
      return labelMap[visibility] || '未设置'
    }
    
    const getStatusClass = (asset) => {
      if (!asset.enabled) return 'disabled'
      if (asset.isTarget) return 'target'
      return 'normal'
    }
    
    const getStatusText = (asset) => {
      if (!asset.enabled) return '已禁用'
      if (asset.isTarget) return '靶场资产'
      return '正常'
    }

    // 新增：获取节点状态类
    const getNodeStatusClass = (nodeId) => {
      const node = availableNodes.value.find(n => n.id === nodeId)
      if (!node) return 'node-unknown'
      
      switch (node.status) {
        case 'active': return 'node-active'
        case 'inactive': return 'node-inactive'
        case 'maintenance': return 'node-maintenance'
        default: return 'node-unknown'
      }
    }
    
    // 新增：智能节点推荐算法
    const getRecommendedNode = (asset) => {
      const activeNodes = availableNodes.value.filter(node => node.status === 'active')
      if (activeNodes.length === 0) return null
      
      // 推荐策略：
      // 1. 优先选择同环境的节点
      // 2. 考虑节点负载（优先级高的节点）
      // 3. 考虑资源充足度
      
      let scoredNodes = activeNodes.map(node => {
        let score = 0
        
        // 环境匹配加分
        if (asset.environment && node.environment === asset.environment) {
          score += 50
        }
        
        // 节点优先级加分
        score += (node.priority || 1) * 10
        
        // 资源充足度加分（假设资源使用率越低越好）
        const maxContainers = node.maxContainers || 50
        const currentLoad = node.currentContainers || 0
        const loadRatio = currentLoad / maxContainers
        score += Math.max(0, (1 - loadRatio) * 30)
        
        // 节点类型加分（本地节点优先）
        if (node.nodeType === 'local') {
          score += 20
        }
        
        return { node, score }
      })
      
      // 按分数排序，返回最高分的节点
      scoredNodes.sort((a, b) => b.score - a.score)
      return scoredNodes[0]?.node || null
    }

    const confirmSelection = () => {
      const selected = Object.values(selectedAssets.value)
      emit('confirm', selected)
    }

    const filterAssets = () => {
      // 过滤逻辑由 computed 属性处理
    }

    // 新增：格式化端口配置
    const formatPortConfig = (portConfig) => {
      if (!portConfig) return '未配置'

      // 如果是字符串，尝试解析为JSON
      let ports = portConfig
      if (typeof portConfig === 'string') {
        try {
          ports = JSON.parse(portConfig)
        } catch {
          return portConfig
        }
      }

      // 格式化端口映射: {"80/tcp":"8080"} → "8080→80/tcp"
      if (typeof ports === 'object' && ports !== null) {
        return Object.entries(ports)
          .map(([container, host]) => `${host}→${container}`)
          .join(', ')
      }

      return String(portConfig)
    }

    // 监听预选资产变化
    watch(() => props.preSelectedAssets, (newVal) => {
      selectedAssets.value = {}
      newVal.forEach(asset => {
        selectedAssets.value[asset.assetId] = asset
      })
    }, { immediate: true })

    onMounted(() => {
      loadAssets()
      loadAvailableNodes() // 新增：加载节点列表
    })

    return {
      assets,
      availableNodes, // 新增
      selectedAssets,
      selectedVisibility,
      selectedTarget,
      selectedHostNode, // 新增
      searchQuery,
      filteredAssets,
      loading,
      toggleAsset,
      isSelected,
      getAssetIcon,
      getVisibilityLabel,
      getStatusClass,
      getStatusText,
      getNodeStatusClass, // 新增
      getRecommendedNode, // 新增
      confirmSelection,
      filterAssets,
      formatPortConfig // 新增：端口格式化方法
    }
  }
}
</script>

<style scoped>
/* ============================================
   Apple Elegant White Style - 苹果高雅白风格
   ============================================ */

/* CSS Variables for consistency */
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
  --apple-yellow: #ffcc00;
  --radius-sm: 12px;
  --radius-md: 16px;
  --radius-lg: 20px;
  --radius-xl: 24px;
  --shadow-card: 0 4px 16px rgba(0, 0, 0, 0.06);
  --shadow-card-hover: 0 8px 32px rgba(0, 0, 0, 0.12);
  --spacing-xs: 8px;
  --spacing-sm: 12px;
  --spacing-md: 16px;
  --spacing-lg: 24px;
  --spacing-xl: 32px;
  --font-apple: -apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", "Helvetica Neue", sans-serif;
}

.asset-selector {
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  background: var(--apple-white);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-card-hover);
  max-height: calc(90vh - 120px);
  display: flex;
  flex-direction: column;
  font-family: var(--font-apple);
  overflow: hidden;
}

/* ============================================
   Selector Header - 头部区域
   ============================================ */
.selector-header {
  padding: var(--spacing-lg);
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.95) 0%,
    rgba(248, 248, 248, 0.85) 100%);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 0.5px solid var(--apple-border);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.selector-header h3 {
  margin: 0;
  color: var(--apple-text);
  font-size: 24px;
  font-weight: 600;
  letter-spacing: -0.5px;
}

.filter-controls {
  display: flex;
  gap: var(--spacing-sm);
  align-items: center;
}

.type-filter, .search-input {
  padding: 10px 14px;
  border: 0.5px solid var(--apple-border);
  background: rgba(255, 255, 255, 0.9);
  border-radius: var(--radius-sm);
  font-size: 14px;
  font-family: var(--font-apple);
  color: var(--apple-text);
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

.type-filter:hover, .search-input:hover {
  border-color: rgba(0, 122, 255, 0.3);
  background: rgba(255, 255, 255, 1);
}

.type-filter:focus, .search-input:focus {
  outline: none;
  border-color: var(--apple-blue);
  background: rgba(255, 255, 255, 1);
  box-shadow: 0 0 0 3px rgba(0, 122, 255, 0.1);
}

.search-input {
  width: 220px;
}

.search-input::placeholder {
  color: var(--apple-text-secondary);
}

/* ============================================
   Assets Grid - 资产网格
   ============================================ */
.assets-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: var(--spacing-lg);
  padding: var(--spacing-lg);
  flex: 1;
  overflow-y: auto;
  min-height: 300px;
  background: var(--apple-white);
}

.asset-card {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.95) 0%,
    rgba(248, 248, 248, 0.85) 100%);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 2px solid var(--apple-border);
  border-radius: var(--radius-lg);
  padding: var(--spacing-md);
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.19, 1, 0.22, 1);
  box-shadow: var(--shadow-card);
}

.asset-card:hover {
  border-color: var(--apple-blue);
  box-shadow: var(--shadow-card-hover);
  transform: translateY(-6px);
}

.asset-card.selected {
  border-color: var(--apple-blue);
  background: linear-gradient(135deg,
    rgba(0, 122, 255, 0.05) 0%,
    rgba(0, 122, 255, 0.02) 100%);
  box-shadow: 0 4px 16px rgba(0, 122, 255, 0.2);
}

.asset-info h4 {
  margin: 0 0 6px 0;
  color: var(--apple-text);
  font-size: 17px;
  font-weight: 600;
  letter-spacing: -0.3px;
}

.asset-type {
  margin: 0 0 var(--spacing-sm) 0;
  color: var(--apple-text-secondary);
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.8px;
  font-weight: 500;
}

.asset-description {
  margin: 0 0 8px 0;
  color: var(--apple-text);
  font-size: 13px;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.asset-notes {
  margin: 0 0 var(--spacing-sm) 0;
  color: var(--apple-text-secondary);
  font-size: 12px;
  font-style: italic;
  line-height: 1.4;
  max-height: 34px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

/* ============================================
   Container Info - 容器信息
   ============================================ */
.container-info {
  margin: var(--spacing-sm) 0;
  padding: var(--spacing-sm);
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(10px);
  border-radius: var(--radius-sm);
  border-left: 3px solid var(--apple-green);
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-sm);
}

.container-info-item {
  display: flex;
  align-items: center;
  margin-bottom: 0;
  font-size: 11px;
  white-space: nowrap;
}

.info-icon {
  margin-right: 4px;
  font-size: 13px;
}

.info-label {
  font-weight: 600;
  color: var(--apple-text-secondary);
  margin-right: 4px;
  min-width: 35px;
}

.docker-image-name {
  font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
  font-size: 11px;
  background: rgba(0, 122, 255, 0.08);
  padding: 3px 7px;
  border-radius: 6px;
  color: var(--apple-blue);
  max-width: 220px;
  display: inline-block;
  word-break: break-all;
  overflow-wrap: anywhere;
  line-height: 1.4;
  vertical-align: middle;
  transition: all 0.2s ease;
}

.docker-image-name:hover {
  background: rgba(0, 122, 255, 0.12);
}

.discovered-images {
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
}

.image-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 8px;
  background: rgba(0, 122, 255, 0.04);
  border-radius: 6px;
  transition: all 0.2s ease;
}

.image-item:hover {
  background: rgba(0, 122, 255, 0.08);
}

.image-item .docker-image-name {
  flex: 1;
  margin-right: 8px;
}

.image-size {
  font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
  font-size: 10px;
  color: var(--apple-text-secondary);
  background: rgba(0, 0, 0, 0.04);
  padding: 2px 6px;
  border-radius: 4px;
  white-space: nowrap;
}

.port-config {
  font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
  background: rgba(156, 39, 176, 0.08);
  padding: 3px 7px;
  border-radius: 6px;
  color: #7b1fa2;
  font-size: 11px;
  max-width: 180px;
  display: inline-block;
  word-break: break-all;
  overflow-wrap: anywhere;
  line-height: 1.4;
  vertical-align: middle;
  transition: all 0.2s ease;
}

.port-config:hover {
  background: rgba(156, 39, 176, 0.12);
}

.resource-limit {
  font-size: 10px;
  color: var(--apple-orange);
  font-weight: 600;
  padding: 2px 6px;
  background: rgba(255, 149, 0, 0.08);
  border-radius: 4px;
}

/* ============================================
   Deploy Info - 部署信息
   ============================================ */
.asset-deploy-info, .asset-strategy {
  margin: 0 0 6px 0;
  color: var(--apple-text);
  font-size: 12px;
  line-height: 1.4;
}

.deploy-label, .strategy-label {
  font-weight: 600;
  color: var(--apple-green);
}

.asset-strategy .strategy-label {
  color: var(--apple-blue);
}

/* Node Status - 节点状态 */
.node-name {
  font-weight: 600;
  padding: 2px 6px;
  border-radius: 6px;
  font-size: 11px;
}

.node-active {
  background: rgba(52, 199, 89, 0.1);
  color: var(--apple-green);
}

.node-inactive {
  background: rgba(255, 59, 48, 0.1);
  color: var(--apple-red);
}

.node-maintenance {
  background: rgba(255, 149, 0, 0.1);
  color: var(--apple-orange);
}

.node-unknown {
  background: rgba(0, 0, 0, 0.05);
  color: var(--apple-text-secondary);
}

/* ============================================
   Asset Status - 资产状态
   ============================================ */
.asset-status {
  margin-top: var(--spacing-sm);
  display: flex;
  gap: var(--spacing-xs);
  align-items: center;
}

.status-badge {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.status-badge.normal {
  background: rgba(52, 199, 89, 0.1);
  color: var(--apple-green);
}

.status-badge.target {
  background: rgba(255, 204, 0, 0.15);
  color: #b8860b;
}

.status-badge.disabled {
  background: rgba(255, 59, 48, 0.1);
  color: var(--apple-red);
}

.target-badge {
  padding: 4px 8px;
  background: var(--apple-yellow);
  color: #000;
  border-radius: 10px;
  font-size: 10px;
  font-weight: 600;
}

/* ============================================
   Selector Footer - 底部操作栏
   ============================================ */
.selector-footer {
  padding: var(--spacing-lg);
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.95) 0%,
    rgba(248, 248, 248, 0.85) 100%);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-top: 0.5px solid var(--apple-border);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.selected-summary {
  color: var(--apple-text);
  font-size: 15px;
  font-weight: 500;
}

.action-buttons {
  display: flex;
  gap: var(--spacing-sm);
}

.btn-cancel, .btn-confirm {
  padding: 12px 24px;
  border: none;
  border-radius: var(--radius-sm);
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  font-family: var(--font-apple);
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
  min-width: 100px;
}

.btn-cancel {
  background: rgba(0, 0, 0, 0.05);
  color: var(--apple-text);
}

.btn-cancel:hover {
  background: rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.btn-confirm {
  background: var(--apple-blue);
  color: white;
}

.btn-confirm:hover:not(:disabled) {
  background: #0051d5;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.3);
}

.btn-confirm:disabled {
  background: rgba(0, 0, 0, 0.1);
  color: rgba(0, 0, 0, 0.3);
  cursor: not-allowed;
  transform: none;
}

/* ============================================
   Scrollbar - 滚动条美化
   ============================================ */
.assets-grid::-webkit-scrollbar {
  width: 8px;
}

.assets-grid::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.02);
  border-radius: 4px;
}

.assets-grid::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.15);
  border-radius: 4px;
  transition: background 0.2s ease;
}

.assets-grid::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.25);
}

/* ============================================
   Responsive Design - 响应式设计
   ============================================ */
@media (max-width: 768px) {
  .asset-selector {
    max-height: calc(100vh - 80px);
  }

  .selector-header {
    flex-direction: column;
    gap: var(--spacing-md);
    align-items: flex-start;
  }

  .selector-header h3 {
    font-size: 20px;
  }

  .filter-controls {
    width: 100%;
    flex-direction: column;
  }

  .type-filter, .search-input {
    width: 100%;
  }

  .assets-grid {
    grid-template-columns: 1fr;
    padding: var(--spacing-md);
  }

  .selector-footer {
    flex-direction: column;
    gap: var(--spacing-md);
    align-items: stretch;
  }

  .selected-summary {
    text-align: center;
  }

  .action-buttons {
    width: 100%;
  }

  .btn-cancel, .btn-confirm {
    flex: 1;
  }
}

@media (max-width: 576px) {
  .selector-header {
    padding: var(--spacing-md);
  }

  .selector-header h3 {
    font-size: 18px;
  }

  .assets-grid {
    padding: var(--spacing-sm);
    gap: var(--spacing-md);
  }

  .action-buttons {
    flex-direction: column;
  }

  .btn-cancel, .btn-confirm {
    width: 100%;
  }
}
</style>