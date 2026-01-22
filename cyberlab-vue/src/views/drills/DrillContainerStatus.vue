<template>
  <div class="drill-container-status" :class="themeClass">

    <!-- 操作栏 - 仅管理员可见 -->
    <div class="action-bar" v-if="role !== 'blue'">
      <div>
        <el-button
          type="primary"
          :icon="Plus"
          @click="showAssetSelector"
          :disabled="loading"
        >
          选择资产
        </el-button>
        <el-button
          :icon="Refresh"
          @click="refreshAssets"
          :loading="loading"
        >
          刷新状态
        </el-button>
      </div>

      <div>
        <!-- 新增：节点筛选器 -->
        <el-select
          v-model="selectedNodeFilter"
          @change="filterByNode"
          placeholder="筛选节点"
          clearable
          style="width: 200px; margin-right: 12px;"
        >
          <el-option value="" label="所有节点"></el-option>
          <el-option
            v-for="node in availableNodes"
            :key="node?.id || `node-${Math.random()}`"
            :value="node?.id"
            :label="node?.displayName || node?.name || '未知节点'"
          >
            <span>{{ node?.displayName || node?.name || '未知节点' }}</span>
            <span style="color: #8492a6; font-size: 13px; margin-left: 8px;" v-if="node?.hostIp">
              {{ node.hostIp }}
            </span>
          </el-option>
        </el-select>
      </div>
    </div>

    <!-- 项目概览信息 -->
    <div class="project-overview" v-if="projectInfo">
      <el-card class="overview-card">
        <template #header>
          <div class="overview-header">
            <span class="overview-title">项目概览</span>
          </div>
        </template>
        <el-row :gutter="16">
          <el-col :span="6">
            <div class="overview-item">
              <div class="overview-label">项目ID</div>
              <div class="overview-value">{{ projectInfo.projectId || '未知' }}</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="overview-item">
              <div class="overview-label">关联资产</div>
              <div class="overview-value">{{ projectInfo.totalAssets || 0 }} 个</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="overview-item">
              <div class="overview-label">部署节点</div>
              <div class="overview-value">{{ projectInfo.deploymentNodes || 0 }} 个</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="overview-item">
              <div class="overview-label">负载均衡度</div>
              <div class="overview-value">
                <el-tag :type="getBalanceScoreType(projectInfo.balanceScore)">
                  {{ Math.round(projectInfo.balanceScore || 0) }}%
                </el-tag>
              </div>
            </div>
          </el-col>
        </el-row>
      </el-card>
    </div>

    <!-- 资产状态统计 -->
    <div class="status-summary" v-if="selectedAssets.length > 0">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-card class="status-card">
            <div class="status-content">
              <div class="status-number">{{ selectedAssets.length }}</div>
              <div class="status-label">总资产</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="status-card">
            <div class="status-content">
              <div class="status-number success">{{ deployedCount }}</div>
              <div class="status-label">已部署</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="status-card">
            <div class="status-content">
              <div class="status-number warning">{{ deployingCount }}</div>
              <div class="status-label">部署中</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="status-card">
            <div class="status-content">
              <div class="status-number danger">{{ failedCount }}</div>
              <div class="status-label">失败</div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 资产列表 -->
    <div class="assets-grid">
      <el-card 
        v-for="asset in selectedAssets" 
        :key="asset.id"
        class="asset-card"
        shadow="hover"
      >
        <!-- 资产头部信息 -->
        <template #header>
          <div class="asset-header">
            <div class="asset-title">
              <div class="asset-icon">
                <img :src="getAssetIcon(asset.asset?.type)" :alt="asset.asset?.name" />
              </div>
              <div class="asset-info">
                <strong>{{ asset.containerName }}</strong>
                <el-tag 
                  :type="getStatusTagType(asset.deploymentStatus)" 
                  size="small"
                  class="status-tag"
                >
                  {{ getStatusIcon(asset.deploymentStatus) }} {{ getStatusText(asset.deploymentStatus) }}
                </el-tag>
              </div>
            </div>
          </div>
        </template>

        <!-- 资产详细信息 -->
        <div class="asset-details">
          <div class="info-row">
            <span class="label">资产名称：</span>
            <span class="value">{{ asset.asset?.name }}</span>
          </div>
          <div class="info-row">
            <span class="label">资产类型：</span>
            <span class="value">{{ asset.asset?.type }}</span>
          </div>
          <!-- 新增：部署节点信息 -->
          <div class="info-row" v-if="asset.targetHostNodeId">
            <span class="label">部署节点：</span>
            <span class="value">
              <el-tag size="small" :type="getNodeStatusType(asset.targetHostNodeId)">
                {{ getNodeDisplayName(asset.targetHostNodeId) }}
              </el-tag>
            </span>
          </div>
          <div class="info-row" v-if="getNodeInfo(asset.targetHostNodeId)">
            <span class="label">节点地址：</span>
            <span class="value">{{ getNodeInfo(asset.targetHostNodeId)?.hostIp }}</span>
          </div>
          <div class="info-row">
            <span class="label">Docker镜像：</span>
            <span class="value docker-image-value" :title="asset.asset?.dockerImage">{{ asset.asset?.dockerImage || '未设置' }}</span>
          </div>
          <div class="info-row" v-if="asset.containerId">
            <span class="label">容器ID：</span>
            <span class="value">{{ asset.containerId }}</span>
          </div>
          <!-- 新增：完整容器名称显示 -->
          <div class="info-row" v-if="asset.containerFullName">
            <span class="label">完整名称：</span>
            <span class="value">{{ asset.containerFullName }}</span>
          </div>
          <div class="info-row" v-if="asset.accessUrl">
            <span class="label">访问地址：</span>
            <el-link 
              :href="asset.accessUrl" 
              target="_blank" 
              type="primary"
              class="access-link"
              @click="openContainerUrl(asset.accessUrl)"
            >
              {{ asset.accessUrl }}
            </el-link>
          </div>
          <div class="info-row" v-if="asset.hostPort">
            <span class="label">主机端口：</span>
            <span class="value">{{ asset.hostPort }}</span>
          </div>
          <div class="info-row" v-if="asset.ipAddress">
            <span class="label">容器地址：</span>
            <span class="value">{{ asset.ipAddress }}</span>
          </div>
          <div class="info-row" v-if="asset.deployedAt">
            <span class="label">部署时间：</span>
            <span class="value">{{ formatTime(asset.deployedAt) }}</span>
          </div>
          <div class="info-row error-row" v-if="asset.deploymentStatus === 'FAILED' && asset.deployError">
            <span class="label">错误信息：</span>
            <span class="value error-text">{{ asset.deployError }}</span>
          </div>
        </div>

        <!-- 操作按钮区 -->
        <div class="asset-actions">
          <template v-if="asset.deploymentStatus === 'PENDING'">
            <el-button 
              type="primary" 
              @click="deployAsset(asset)"
              :loading="asset.deploying"
            >
              ▶️ 部署
            </el-button>
          </template>
          
          <template v-else-if="asset.deploymentStatus === 'DEPLOYING'">
            <el-button disabled loading>
              🟡 部署中...
            </el-button>
            <el-button @click="viewLogs(asset)">
              📄 查看日志
            </el-button>
          </template>
          
          <template v-else-if="asset.deploymentStatus === 'DEPLOYED'">
            <el-button 
              type="warning" 
              @click="stopAsset(asset)"
              :loading="asset.stopping"
            >
              ⏹ 停止
            </el-button>
            <el-button @click="viewLogs(asset)">
              📄 查看日志
            </el-button>
          </template>
          
          <template v-else-if="asset.deploymentStatus === 'FAILED'">
            <el-button 
              type="primary" 
              @click="retryDeploy(asset)"
              :loading="asset.retrying"
            >
              🔄 重试部署
            </el-button>
            <el-button @click="viewLogs(asset)">
              📄 查看日志
            </el-button>
          </template>
          
          <!-- 删除按钮 -->
          <el-button 
            type="danger" 
            @click="removeAsset(asset)"
            :disabled="asset.deploymentStatus === 'DEPLOYING'"
          >
            🗑️ 移除
          </el-button>
        </div>
      </el-card>
    </div>

    <!-- 空状态 -->
    <el-empty 
      v-if="selectedAssets.length === 0 && !loading" 
      description="暂未选择资产"
      class="empty-state"
    >
      <el-button type="primary" @click="showAssetSelector">
        🎯 选择资产
      </el-button>
    </el-empty>

    <!-- 资产选择器弹窗 -->
    <el-dialog
      v-model="assetSelectorVisible"
      width="90%"
      top="5vh"
      :close-on-click-modal="false"
      :lock-scroll="false"
      class="asset-selector-dialog"
    >
      <AssetSelector 
        :range-id="Number(drillId)"
        :pre-selected-assets="selectedAssets"
        @confirm="handleAssetSelection"
        @cancel="assetSelectorVisible = false"
      />
    </el-dialog>

    <!-- 日志查看弹窗 -->
    <el-dialog
      v-model="logDialogVisible"
      title="资产部署日志"
      width="80%"
      top="5vh"
      :lock-scroll="false"
    >
      <div class="log-content">
        <div class="log-header">
          <span>资产：{{ currentAsset?.containerName }}</span>
        </div>
        <el-input
          v-model="assetLogs"
          type="textarea"
          :rows="15"
          readonly
          class="log-textarea"
          placeholder="加载日志中..."
        />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import { getAvailableHostNodes } from '@/api/hostNodes'
import { 
  Refresh, 
  Plus
} from '@element-plus/icons-vue'
import AssetSelector from '@/components/drills/AssetSelector.vue'
import { getUserRole } from '@/utils/auth'

// 主题支持
const role = getUserRole() || ''
const themeClass = computed(() => role === 'blue' ? 'theme-blue' : 'theme-admin')

// 为body添加主题class，以便全局组件应用主题
onMounted(() => {
  if (role === 'blue') {
    document.body.classList.add('blue-theme-active')
  }
})

// 组件卸载时移除body的主题class
onUnmounted(() => {
  if (role === 'blue') {
    document.body.classList.remove('blue-theme-active')
  }
})

// Props
const props = defineProps({
  drillId: {
    type: [Number, String],
    required: true
  }
})

// API配置 - 使用相对路径通过Vite代理，避免CORS问题
const apiBaseUrl = ref('/api')

// 响应式数据
const selectedAssets = ref([])
const availableNodes = ref([]) // 新增：可用节点列表
const selectedNodeFilter = ref('') // 新增：节点筛选
const projectInfo = ref(null) // 新增：项目信息
const loading = ref(false)
const refreshing = ref(false)

// 弹窗相关
const assetSelectorVisible = ref(false)
const logDialogVisible = ref(false)
const assetLogs = ref('')
const currentAsset = ref(null)

// 计算属性
const deployedCount = computed(() => 
  selectedAssets.value.filter(asset => asset.deploymentStatus === 'DEPLOYED').length
)

const deployingCount = computed(() => 
  selectedAssets.value.filter(asset => asset.deploymentStatus === 'DEPLOYING').length
)

const failedCount = computed(() => 
  selectedAssets.value.filter(asset => asset.deploymentStatus === 'FAILED').length
)

// 状态相关方法
const getStatusTagType = (status) => {
  const statusMap = {
    'PENDING': 'info',
    'DEPLOYING': 'warning', 
    'DEPLOYED': 'success',
    'FAILED': 'danger'
  }
  return statusMap[status] || 'info'
}

const getStatusIcon = (status) => {
  const iconMap = {
    'PENDING': '⚪',
    'DEPLOYING': '🟡',
    'DEPLOYED': '🟢', 
    'FAILED': '❌'
  }
  return iconMap[status] || '⚪'
}

const getStatusText = (status) => {
  const textMap = {
    'PENDING': '待部署',
    'DEPLOYING': '部署中',
    'DEPLOYED': '已部署',
    'FAILED': '部署失败'
  }
  return textMap[status] || '未知'
}

const getAssetIcon = (type) => {
  const iconMap = {
    server: '/icons/webserver.png',
    network: '/icons/router.png',
    security: '/icons/firewall.png',
    workstation: '/icons/pc.png',
    database: '/icons/database.png'
  }
  return iconMap[type] || '/icons/pc.png'
}

// 负载均衡度标签类型
const getBalanceScoreType = (score) => {
  if (!score) return 'info'
  if (score >= 80) return 'success'
  if (score >= 60) return 'warning'
  return 'danger'
}

// 时间格式化
const formatTime = (timeStr) => {
  if (!timeStr) return '-'
  return new Date(timeStr).toLocaleString('zh-CN')
}

// 新增：节点相关方法
const getNodeInfo = (nodeId) => {
  if (!nodeId || !availableNodes.value) return null
  return availableNodes.value.find(node => node && node.id === nodeId)
}

const getNodeDisplayName = (nodeId) => {
  const node = getNodeInfo(nodeId)
  return node ? (node.displayName || node.name || '未知节点') : '未知节点'
}

const getNodeStatusType = (nodeId) => {
  const node = getNodeInfo(nodeId)
  if (!node || !node.status) return 'info'
  
  const statusMap = {
    'active': 'success',
    'inactive': 'danger',
    'maintenance': 'warning',
    'error': 'danger'
  }
  return statusMap[node.status] || 'info'
}

const loadAvailableNodes = async () => {
  try {
    const nodes = await getAvailableHostNodes()
    availableNodes.value = Array.isArray(nodes) ? nodes : []
  } catch {
    availableNodes.value = []
  }
}

const filterByNode = () => {
  // 节点筛选逻辑
  if (selectedNodeFilter.value) {
    // 这里可以添加按节点筛选容器的逻辑
  }
}

// 新增：获取项目信息
const fetchProjectInfo = async () => {
  try {
    const response = await fetch(`${apiBaseUrl.value}/drills/${props.drillId}/project-info`, {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    })

    if (response.ok) {
      const info = await response.json()
      projectInfo.value = info
    }
  } catch {
    // 静默失败，项目信息不是必需的
  }
}

// API 调用方法
const fetchDrillAssets = async () => {
  if (!props.drillId) return

  try {
    loading.value = true
    const response = await fetch(`${apiBaseUrl.value}/drills/${props.drillId}/assets`, {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    })

    if (!response.ok) throw new Error('获取资产列表失败')

    const assetsData = await response.json()

    selectedAssets.value = Array.isArray(assetsData) ? assetsData : []
  } catch {
    ElMessage.error('获取资产列表失败')
    selectedAssets.value = []
  } finally {
    loading.value = false
  }
}

const refreshAssets = async () => {
  refreshing.value = true
  await fetchDrillAssets()
  refreshing.value = false
  ElMessage.success('刷新成功')
}

// 资产选择相关
const showAssetSelector = () => {
  assetSelectorVisible.value = true
}

const handleAssetSelection = async (assets) => {
  try {
    let successCount = 0
    let conflictCount = 0

    // 不再预先清空，而是逐个添加，遇到冲突时询问用户
    for (const asset of assets) {
      try {
        await addAssetToDrill(asset)
        successCount++
      } catch (error) {
        // 检查是否是冲突错误（409）
        if (error.isConflict) {
          // 弹出确认对话框
          const conflictImages = error.conflictImages || []
          const imageNames = conflictImages.map(img =>
            `${img.imageName}:${img.tag}`
          ).join(', ')

          try {
            await ElMessageBox.confirm(
              `资产 "${asset.name || asset.assetId}" 的镜像已存在（${imageNames}），是否删除并重新添加？`,
              '资产冲突',
              {
                confirmButtonText: '删除并重新添加',
                cancelButtonText: '跳过',
                type: 'warning',
                distinguishCancelAndClose: true
              }
            )

            // 用户确认：完全删除后重新添加
            await completelyRemoveAsset(asset.assetId)
            await addAssetToDrill(asset)
            successCount++
            ElNotification.success({
              title: '替换成功',
              message: `资产 "${asset.name || asset.assetId}" 已替换`
            })
          } catch (cancelError) {
            // 用户取消或关闭对话框
            if (cancelError === 'cancel' || cancelError === 'close') {
              conflictCount++
              ElNotification.info({
                title: '已跳过',
                message: `已跳过资产 "${asset.name || asset.assetId}"`
              })
            } else {
              // 删除或重新添加失败
              throw cancelError
            }
          }
        } else {
          // 其他错误直接抛出
          throw error
        }
      }
    }

    assetSelectorVisible.value = false

    if (successCount > 0) {
      ElMessage.success(`成功添加 ${successCount} 个资产${conflictCount > 0 ? `，跳过 ${conflictCount} 个` : ''}`)
    } else if (conflictCount > 0) {
      ElMessage.info(`已跳过 ${conflictCount} 个冲突资产`)
    }

    await fetchDrillAssets()
  } catch (error) {
    ElNotification.error({
      title: '资产选择失败',
      message: error.message || '未知错误',
      duration: 5000
    })
  }
}

// clearExistingAssets 函数已被移除，新逻辑中不再预先清空资产
// 而是在添加时检测冲突并提示用户确认

/**
 * 完全删除资产（容器+映射+配置）
 */
const completelyRemoveAsset = async (assetId) => {
  const response = await fetch(`${apiBaseUrl.value}/drills/${props.drillId}/assets/${assetId}/complete`, {
    method: 'DELETE',
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('token')}`
    }
  })

  if (!response.ok) {
    const data = await response.json()
    throw new Error(data.message || '完全删除失败')
  }

  return await response.json()
}

const addAssetToDrill = async (asset) => {
  const response = await fetch(`${apiBaseUrl.value}/drills/${props.drillId}/assets`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('token')}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      assetId: asset.assetId,
      containerName: asset.containerName,
      networkConfig: asset.networkConfig
    })
  })

  if (!response.ok) {
    const data = await response.json()

    // 检查是否是 409 冲突错误
    if (response.status === 409) {
      const error = new Error(data.message || '资产已存在')
      error.isConflict = true
      error.conflictImages = data.conflictImages || []
      error.assetId = data.assetId
      throw error
    }

    throw new Error(data.message || '添加资产失败')
  }

  return await response.json()
}

// 资产操作方法
const deployAsset = async (asset) => {
  try {
    asset.deploying = true

    // 🆕 构建请求体：如果asset有imageId，则为镜像模式部署
    const requestBody = asset.imageId ? { imageId: asset.imageId } : {}

    const response = await fetch(`${apiBaseUrl.value}/drills/${props.drillId}/assets/${asset.assetId}/deploy`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(requestBody)
    })

    if (!response.ok) throw new Error('部署失败')

    ElMessage.success(`资产 ${asset.containerName} 开始部署`)
    
    // 立即刷新状态
    await fetchDrillAssets()
    
    // 设置轮询检查部署状态
    const checkDeploymentStatus = setInterval(async () => {
      try {
        await fetchDrillAssets()
        const updatedAsset = selectedAssets.value.find(a => a.assetId === asset.assetId)
        if (updatedAsset && updatedAsset.deploymentStatus !== 'DEPLOYING') {
          clearInterval(checkDeploymentStatus)
          if (updatedAsset.deploymentStatus === 'DEPLOYED') {
            ElMessage.success(`资产 ${asset.containerName} 部署成功`)
          } else if (updatedAsset.deploymentStatus === 'FAILED') {
            ElMessage.error(`资产 ${asset.containerName} 部署失败`)
          }
        }
      } catch {
        clearInterval(checkDeploymentStatus)
      }
    }, 2000) // 每2秒检查一次
    
    // 30秒后停止轮询
    setTimeout(() => clearInterval(checkDeploymentStatus), 30000)
    
  } catch (error) {
    ElMessage.error(`部署失败: ${error.message}`)
  } finally {
    asset.deploying = false
  }
}

const stopAsset = async (asset) => {
  try {
    asset.stopping = true

    // ✅ 修复：构建URL时添加imageId参数（如果存在）
    let stopUrl = `${apiBaseUrl.value}/drills/${props.drillId}/assets/${asset.assetId}/stop`
    if (asset.imageId) {
      stopUrl += `?imageId=${encodeURIComponent(asset.imageId)}`
    }

    const response = await fetch(stopUrl, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    })

    if (!response.ok) {
      const errorData = await response.json()
      throw new Error(errorData.message || '停止失败')
    }

    ElMessage.success(`资产 ${asset.containerName} 停止成功`)
    await fetchDrillAssets()
  } catch (error) {
    ElMessage.error(`停止失败: ${error.message}`)
  } finally {
    asset.stopping = false
  }
}

const retryDeploy = async (asset) => {
  try {
    asset.retrying = true
    await deployAsset(asset)
  } finally {
    asset.retrying = false
  }
}

const removeAsset = async (asset) => {
  try {
    await ElMessageBox.confirm(
      `确定要移除资产 "${asset.containerName}" 吗？`,
      '确认移除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    // 🆕 如果asset有imageId，则为单镜像删除模式，传递imageId参数
    let deleteUrl = `${apiBaseUrl.value}/drills/${props.drillId}/assets/${asset.assetId}`
    if (asset.imageId) {
      deleteUrl += `?imageId=${encodeURIComponent(asset.imageId)}`
    }

    const response = await fetch(deleteUrl, {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    })

    if (!response.ok) throw new Error('移除失败')

    ElMessage.success(`资产 ${asset.containerName} 移除成功`)
    await fetchDrillAssets()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(`移除失败: ${error.message}`)
    }
  }
}

// 日志相关方法
const viewLogs = async (asset) => {
  currentAsset.value = asset
  logDialogVisible.value = true
  await fetchLogs(asset)
}

const fetchLogs = async (asset) => {
  try {
    const response = await fetch(`${apiBaseUrl.value}/drills/${props.drillId}/assets/${asset.assetId}/logs`, {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    })

    if (!response.ok) throw new Error('获取日志失败')

    assetLogs.value = await response.text()
  } catch (error) {
    assetLogs.value = `获取日志失败: ${error.message}`
  }
}

// 打开容器访问URL
const openContainerUrl = (url) => {
  if (url) {
    window.open(url, '_blank')
    ElMessage.info('正在打开容器访问地址...')
  }
}

// 生命周期
onMounted(async () => {
  await loadAvailableNodes() // 加载节点列表
  await fetchDrillAssets()
  await fetchProjectInfo() // 加载项目信息
})

// 监听drillId变化
watch(() => props.drillId, () => {
  fetchDrillAssets()
})
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

.drill-container-status {
  padding: var(--spacing-xl);
  background: var(--apple-white);
  min-height: 100vh;
  font-family: var(--font-apple);
}

/* ============================================
   Action Bar - 操作栏
   ============================================ */
.action-bar {
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
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

.action-bar:hover {
  box-shadow: var(--shadow-card-hover);
}

.action-bar .el-button {
  margin-right: 10px;
  border-radius: var(--radius-sm);
  font-weight: 500;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

.action-bar .el-button:hover {
  transform: translateY(-2px);
}

.action-bar .el-button--primary {
  background: var(--apple-blue);
  border-color: var(--apple-blue);
}

.action-bar .el-button--primary:hover {
  background: #0051d5;
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.3);
}

/* ============================================
   Project Overview - 项目概览
   ============================================ */
.project-overview {
  margin-bottom: var(--spacing-lg);
}

.overview-card {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.95) 0%,
    rgba(248, 248, 248, 0.85) 100%);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 0.5px solid var(--apple-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  transition: all 0.3s ease;
}

.overview-card:hover {
  box-shadow: var(--shadow-card-hover);
}

.overview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.overview-title {
  font-weight: 600;
  color: var(--apple-text);
  font-size: 17px;
  letter-spacing: -0.3px;
}

.overview-item {
  text-align: center;
  padding: 10px;
}

.overview-label {
  font-size: 13px;
  color: var(--apple-text-secondary);
  margin-bottom: 5px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  font-weight: 500;
}

.overview-value {
  font-size: 20px;
  font-weight: 600;
  color: var(--apple-text);
  font-variant-numeric: tabular-nums;
}

/* ============================================
   Status Summary - 状态统计
   ============================================ */
.status-summary {
  margin-bottom: var(--spacing-lg);
}

.status-card {
  text-align: center;
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.95) 0%,
    rgba(248, 248, 248, 0.85) 100%);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 0.5px solid var(--apple-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  transition: all 0.4s cubic-bezier(0.19, 1, 0.22, 1);
}

.status-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--shadow-card-hover);
}

.status-content {
  padding: var(--spacing-lg);
}

.status-number {
  font-size: 36px;
  font-weight: 700;
  margin-bottom: 5px;
  color: var(--apple-text);
  font-variant-numeric: tabular-nums;
  line-height: 1;
}

.status-number.success {
  color: var(--apple-green);
}

.status-number.warning {
  color: var(--apple-orange);
}

.status-number.danger {
  color: var(--apple-red);
}

.status-label {
  color: var(--apple-text-secondary);
  font-size: 14px;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

/* ============================================
   Assets Grid - 资产网格
   ============================================ */
.assets-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: var(--spacing-lg);
  margin-bottom: var(--spacing-lg);
}

.asset-card {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.95) 0%,
    rgba(248, 248, 248, 0.85) 100%);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 0.5px solid var(--apple-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  transition: all 0.4s cubic-bezier(0.19, 1, 0.22, 1);
  overflow: hidden;
}

.asset-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--shadow-card-hover);
}

.asset-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.asset-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.asset-icon {
  width: 40px;
  height: 40px;
  filter: drop-shadow(0 2px 8px rgba(0, 0, 0, 0.1));
}

.asset-icon img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.asset-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.asset-info strong {
  font-weight: 600;
  color: var(--apple-text);
  font-size: 15px;
  letter-spacing: -0.2px;
}

.status-tag {
  font-weight: 600;
  align-self: flex-start;
  border-radius: 8px;
}

.asset-details {
  margin-bottom: var(--spacing-lg);
  padding: var(--spacing-md);
  background: rgba(255, 255, 255, 0.5);
  border-radius: var(--radius-sm);
}

.info-row {
  display: flex;
  margin-bottom: 10px;
  align-items: center;
  line-height: 1.6;
}

.label {
  font-weight: 500;
  color: var(--apple-text-secondary);
  min-width: 90px;
  font-size: 13px;
}

.value {
  color: var(--apple-text);
  font-size: 14px;
}

.docker-image-value {
  font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
  font-size: 12px;
  background: rgba(0, 122, 255, 0.08);
  padding: 4px 8px;
  border-radius: 6px;
  color: var(--apple-blue);
  max-width: 280px;
  display: inline-block;
  word-break: break-all;
  overflow-wrap: anywhere;
  line-height: 1.4;
  vertical-align: middle;
  cursor: help;
  transition: all 0.2s ease;
}

.docker-image-value:hover {
  background: rgba(0, 122, 255, 0.12);
}

.error-row {
  padding: var(--spacing-sm);
  background: rgba(255, 59, 48, 0.05);
  border-radius: 8px;
  border-left: 3px solid var(--apple-red);
}

.error-text {
  color: var(--apple-red);
  font-size: 12px;
  word-break: break-all;
}

.access-link {
  font-family: 'Monaco', 'Menlo', monospace;
  font-size: 13px;
  color: var(--apple-blue);
  text-decoration: none;
  transition: all 0.2s ease;
}

.access-link:hover {
  color: #0051d5;
}

.asset-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  padding: var(--spacing-md);
  background: rgba(255, 255, 255, 0.5);
  border-radius: 0 0 var(--radius-lg) var(--radius-lg);
}

.asset-actions .el-button {
  flex: 1;
  min-width: 80px;
  border-radius: var(--radius-sm);
  font-weight: 500;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

.asset-actions .el-button:hover {
  transform: translateY(-2px);
}

.asset-actions .el-button--primary {
  background: var(--apple-blue);
  border-color: var(--apple-blue);
}

.asset-actions .el-button--primary:hover {
  background: #0051d5;
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.3);
}

.asset-actions .el-button--warning {
  background: var(--apple-orange);
  border-color: var(--apple-orange);
}

.asset-actions .el-button--warning:hover {
  background: #e69500;
  box-shadow: 0 4px 12px rgba(230, 165, 0, 0.3);
}

.asset-actions .el-button--danger {
  background: var(--apple-red);
  border-color: var(--apple-red);
}

.asset-actions .el-button--danger:hover {
  background: #e03020;
  box-shadow: 0 4px 12px rgba(255, 59, 48, 0.3);
}

/* ============================================
   Empty State - 空状态
   ============================================ */
.empty-state {
  margin: 60px 0;
  padding: var(--spacing-xl) 0;
}

.empty-state .el-button {
  min-width: 180px;
  height: 44px;
  font-size: 15px;
  font-weight: 500;
  border-radius: var(--radius-sm);
  background: var(--apple-blue);
  border-color: var(--apple-blue);
}

.empty-state .el-button:hover {
  background: #0051d5;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.3);
}

/* ============================================
   Dialog - 弹窗
   ============================================ */
.asset-selector-dialog {
  border-radius: var(--radius-xl);
}

.log-content {
  max-height: 60vh;
  overflow-y: auto;
}

.log-header {
  margin-bottom: 10px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--apple-border);
  font-weight: 500;
  color: var(--apple-text);
}

.log-textarea {
  font-family: 'Monaco', 'Menlo', monospace;
  font-size: 12px;
}

.log-textarea :deep(.el-textarea__inner) {
  background: #1e1e1e;
  color: #d4d4d4;
  border: none;
  border-radius: var(--radius-sm);
  max-height: 50vh;
  overflow-y: auto;
}

/* ============================================
   Responsive Design - 响应式设计
   ============================================ */
@media (max-width: 768px) {
  .drill-container-status {
    padding: var(--spacing-md);
  }

  .assets-grid {
    grid-template-columns: 1fr;
  }

  .action-bar {
    flex-direction: column;
    gap: 10px;
  }

  .action-bar > div {
    width: 100%;
  }

  .action-bar .el-button {
    width: 100%;
    margin-right: 0;
    margin-bottom: 8px;
  }

  .action-bar .el-select {
    width: 100% !important;
    margin-right: 0 !important;
  }

  .asset-actions {
    flex-direction: column;
  }

  .asset-actions .el-button {
    flex: none;
    width: 100%;
  }

  .status-number {
    font-size: 28px;
  }

  .overview-value {
    font-size: 18px;
  }
}

@media (max-width: 576px) {
  .action-bar {
    padding: var(--spacing-md);
  }

  .info-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }

  .label {
    min-width: auto;
  }
}

/* ==================== 蓝队主题样式 ==================== */
/* 主容器 */
.drill-container-status.theme-blue {
  background: transparent;
}

/* 操作栏 */
.drill-container-status.theme-blue .action-bar {
  background: linear-gradient(135deg,
    rgba(20, 30, 50, 0.85) 0%,
    rgba(13, 26, 45, 0.95) 100%) !important;
  border: 1px solid rgba(70, 130, 180, 0.35) !important;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3) !important;
  backdrop-filter: blur(15px);
  -webkit-backdrop-filter: blur(15px);
}

/* 按钮样式 */
.drill-container-status.theme-blue :deep(.el-button--primary) {
  background: linear-gradient(135deg, #4682b4 0%, #1e90ff 100%) !important;
  border: 1px solid rgba(70, 130, 180, 0.6) !important;
  color: #ffffff !important;
  box-shadow: 0 4px 15px rgba(70, 130, 180, 0.2) !important;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1) !important;
}

.drill-container-status.theme-blue :deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, #5a9fd4 0%, #3ea8ff 100%) !important;
  box-shadow: 0 6px 20px rgba(70, 130, 180, 0.35) !important;
  transform: translateY(-2px);
}

.drill-container-status.theme-blue :deep(.el-button--default) {
  background: rgba(20, 30, 50, 0.6) !important;
  border: 1px solid rgba(70, 130, 180, 0.4) !important;
  color: rgba(255, 255, 255, 0.85) !important;
}

.drill-container-status.theme-blue :deep(.el-button--default:hover) {
  background: rgba(70, 130, 180, 0.3) !important;
  color: #ffffff !important;
}

.drill-container-status.theme-blue :deep(.el-button--success) {
  background: linear-gradient(135deg, #34c759 0%, #30d158 100%) !important;
  border: 1px solid rgba(52, 199, 89, 0.6) !important;
}

.drill-container-status.theme-blue :deep(.el-button--success:hover) {
  background: linear-gradient(135deg, #4ad766 0%, #46e165 100%) !important;
  transform: translateY(-2px);
}

.drill-container-status.theme-blue :deep(.el-button--warning) {
  background: linear-gradient(135deg, #ff9500 0%, #ffaa00 100%) !important;
  border: 1px solid rgba(255, 149, 0, 0.6) !important;
}

.drill-container-status.theme-blue :deep(.el-button--warning:hover) {
  background: linear-gradient(135deg, #ffaa20 0%, #ffbb20 100%) !important;
  transform: translateY(-2px);
}

.drill-container-status.theme-blue :deep(.el-button--danger) {
  background: linear-gradient(135deg, #ff3b30 0%, #ff4545 100%) !important;
  border: 1px solid rgba(255, 59, 48, 0.6) !important;
}

.drill-container-status.theme-blue :deep(.el-button--danger:hover) {
  background: linear-gradient(135deg, #ff5245 0%, #ff5c5c 100%) !important;
  transform: translateY(-2px);
}

/* 选择器 */
.drill-container-status.theme-blue :deep(.el-select .el-input__wrapper) {
  background: rgba(20, 30, 50, 0.6) !important;
  border: 1px solid rgba(70, 130, 180, 0.35) !important;
}

.drill-container-status.theme-blue :deep(.el-select .el-input__inner) {
  color: #ffffff !important;
}

/* 项目概览卡片 */
.drill-container-status.theme-blue .project-overview {
  margin-bottom: var(--spacing-lg);
}

.drill-container-status.theme-blue .overview-card {
  background: linear-gradient(135deg,
    rgba(20, 30, 50, 0.85) 0%,
    rgba(13, 26, 45, 0.95) 100%) !important;
  border: 1px solid rgba(70, 130, 180, 0.35) !important;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.45),
              0 0 20px rgba(70, 130, 180, 0.08) !important;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
}

.drill-container-status.theme-blue .overview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.drill-container-status.theme-blue .overview-title {
  color: #ffffff !important;
  font-weight: 600;
  text-shadow: 0 0 10px rgba(0, 212, 255, 0.2);
}

.drill-container-status.theme-blue .overview-item {
  text-align: center;
  padding: var(--spacing-md);
}

.drill-container-status.theme-blue .overview-label {
  color: rgba(255, 255, 255, 0.7) !important;
  font-size: 13px;
  margin-bottom: 8px;
}

.drill-container-status.theme-blue .overview-value {
  color: #00d4ff !important;
  font-size: 24px;
  font-weight: 700;
  text-shadow: 0 0 10px rgba(0, 212, 255, 0.3);
}

/* 状态统计卡片 */
.drill-container-status.theme-blue .status-summary {
  margin-bottom: var(--spacing-lg);
}

.drill-container-status.theme-blue .status-card {
  background: linear-gradient(135deg,
    rgba(20, 30, 50, 0.85) 0%,
    rgba(13, 26, 45, 0.95) 100%) !important;
  border: 1px solid rgba(70, 130, 180, 0.35) !important;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3) !important;
  backdrop-filter: blur(15px);
  -webkit-backdrop-filter: blur(15px);
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

.drill-container-status.theme-blue .status-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4),
              0 0 20px rgba(70, 130, 180, 0.12) !important;
}

.drill-container-status.theme-blue .status-number {
  color: #00d4ff !important;
  font-weight: 700;
  text-shadow: 0 0 15px rgba(0, 212, 255, 0.4);
}

.drill-container-status.theme-blue .status-label {
  color: rgba(255, 255, 255, 0.75) !important;
}

/* 资产列表 */
.drill-container-status.theme-blue .asset-list {
  background: linear-gradient(135deg,
    rgba(13, 26, 45, 0.9) 0%,
    rgba(10, 20, 40, 0.95) 100%) !important;
  border: 1px solid rgba(70, 130, 180, 0.4) !important;
  border-radius: 12px;
  padding: var(--spacing-lg);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4) !important;
}

.drill-container-status.theme-blue .asset-item {
  background: rgba(20, 30, 50, 0.5) !important;
  border: 1px solid rgba(70, 130, 180, 0.3) !important;
  border-radius: 10px;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

.drill-container-status.theme-blue .asset-item:hover {
  background: rgba(70, 130, 180, 0.15) !important;
  border-color: rgba(70, 130, 180, 0.5) !important;
  transform: translateX(4px);
}

.drill-container-status.theme-blue .asset-name {
  color: #ffffff !important;
  font-weight: 600;
}

.drill-container-status.theme-blue .asset-info {
  color: rgba(255, 255, 255, 0.75) !important;
}

/* 表格样式 */
.drill-container-status.theme-blue :deep(.el-table) {
  background: transparent !important;
  color: #ffffff !important;
}

.drill-container-status.theme-blue :deep(.el-table::before) {
  display: none;
}

.drill-container-status.theme-blue :deep(.el-table__header-wrapper) {
  background: linear-gradient(135deg,
    rgba(70, 130, 180, 0.25) 0%,
    rgba(30, 144, 255, 0.2) 100%) !important;
  border-radius: 8px 8px 0 0;
}

.drill-container-status.theme-blue :deep(.el-table thead) {
  color: #ffffff !important;
}

.drill-container-status.theme-blue :deep(.el-table th.el-table__cell) {
  background: transparent !important;
  color: #ffffff !important;
  border-bottom: 2px solid rgba(70, 130, 180, 0.4) !important;
  font-weight: 600;
  text-shadow: 0 0 8px rgba(0, 212, 255, 0.2) !important;
}

.drill-container-status.theme-blue :deep(.el-table tr) {
  background: transparent !important;
}

.drill-container-status.theme-blue :deep(.el-table tbody tr) {
  background: rgba(20, 30, 50, 0.3) !important;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1) !important;
}

.drill-container-status.theme-blue :deep(.el-table tbody tr:hover) {
  background: rgba(70, 130, 180, 0.25) !important;
}

.drill-container-status.theme-blue :deep(.el-table td.el-table__cell) {
  background: transparent !important;
  color: rgba(255, 255, 255, 0.9) !important;
  border-bottom: 1px solid rgba(70, 130, 180, 0.2) !important;
}

.drill-container-status.theme-blue :deep(.el-table--enable-row-hover .el-table__body tr:hover > td) {
  background: transparent !important;
}

/* Tags */
.drill-container-status.theme-blue :deep(.el-tag) {
  border: 1px solid rgba(70, 130, 180, 0.4) !important;
  background: rgba(20, 30, 50, 0.6) !important;
  color: #ffffff !important;
  font-weight: 500;
}

.drill-container-status.theme-blue :deep(.el-tag--success) {
  background: rgba(52, 199, 89, 0.2) !important;
  border-color: rgba(52, 199, 89, 0.5) !important;
  color: #34c759 !important;
}

.drill-container-status.theme-blue :deep(.el-tag--warning) {
  background: rgba(255, 149, 0, 0.2) !important;
  border-color: rgba(255, 149, 0, 0.5) !important;
  color: #ff9500 !important;
}

.drill-container-status.theme-blue :deep(.el-tag--danger) {
  background: rgba(255, 59, 48, 0.2) !important;
  border-color: rgba(255, 59, 48, 0.5) !important;
  color: #ff3b30 !important;
}

.drill-container-status.theme-blue :deep(.el-tag--info) {
  background: rgba(70, 130, 180, 0.2) !important;
  border-color: rgba(70, 130, 180, 0.5) !important;
  color: #4682b4 !important;
}

/* Empty state */
.drill-container-status.theme-blue :deep(.el-empty__description p) {
  color: rgba(255, 255, 255, 0.6) !important;
}

.drill-container-status.theme-blue :deep(.el-empty__image svg) {
  fill: rgba(255, 255, 255, 0.3) !important;
}

/* 卡片 */
.drill-container-status.theme-blue :deep(.el-card) {
  background: linear-gradient(135deg,
    rgba(20, 30, 50, 0.85) 0%,
    rgba(13, 26, 45, 0.95) 100%) !important;
  border: 1px solid rgba(70, 130, 180, 0.35) !important;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.45),
              0 0 20px rgba(70, 130, 180, 0.08) !important;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
}

.drill-container-status.theme-blue :deep(.el-card__header) {
  background: linear-gradient(135deg,
    rgba(70, 130, 180, 0.2) 0%,
    rgba(30, 144, 255, 0.15) 100()) !important;
  border-bottom: 1px solid rgba(70, 130, 180, 0.3) !important;
}

/* Link buttons */
.drill-container-status.theme-blue :deep(.el-button.is-link) {
  color: #00d4ff !important;
}

.drill-container-status.theme-blue :deep(.el-button.is-link:hover) {
  color: #4ddbff !important;
  text-shadow: 0 0 8px rgba(0, 212, 255, 0.4) !important;
}

/* Loading */
.drill-container-status.theme-blue :deep(.el-loading-mask) {
  background: rgba(10, 20, 40, 0.85) !important;
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

.drill-container-status.theme-blue :deep(.el-loading-spinner .circular) {
  stroke: #00d4ff !important;
}

.drill-container-status.theme-blue :deep(.el-loading-text) {
  color: #ffffff !important;
}

/* Progress */
.drill-container-status.theme-blue :deep(.el-progress__text) {
  color: #ffffff !important;
}

.drill-container-status.theme-blue :deep(.el-progress-bar__outer) {
  background: rgba(70, 130, 180, 0.2) !important;
}

.drill-container-status.theme-blue :deep(.el-progress-bar__inner) {
  background: linear-gradient(135deg, #4682b4 0%, #1e90ff 100%) !important;
}

/* Dialog */
.drill-container-status.theme-blue :deep(.el-dialog) {
  background: linear-gradient(135deg,
    rgba(13, 26, 45, 0.98) 0%,
    rgba(10, 20, 40, 0.99) 100%) !important;
  border: 1px solid rgba(70, 130, 180, 0.4) !important;
  border-radius: 16px;
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.6),
              0 0 30px rgba(70, 130, 180, 0.15) !important;
  backdrop-filter: blur(30px);
  -webkit-backdrop-filter: blur(30px);
}

.drill-container-status.theme-blue :deep(.el-dialog__header) {
  background: linear-gradient(135deg,
    rgba(70, 130, 180, 0.25) 0%,
    rgba(30, 144, 255, 0.2) 100%) !important;
  border-bottom: 1px solid rgba(70, 130, 180, 0.35) !important;
  border-radius: 16px 16px 0 0;
}

.drill-container-status.theme-blue :deep(.el-dialog__title) {
  color: #ffffff !important;
  font-weight: 600;
  text-shadow: 0 0 15px rgba(0, 212, 255, 0.3) !important;
}

.drill-container-status.theme-blue :deep(.el-dialog__close) {
  color: rgba(255, 255, 255, 0.7) !important;
}

.drill-container-status.theme-blue :deep(.el-dialog__close:hover) {
  color: #ffffff !important;
}

.drill-container-status.theme-blue :deep(.el-dialog__body) {
  background: transparent;
  color: rgba(255, 255, 255, 0.85) !important;
}

.drill-container-status.theme-blue :deep(.el-dialog__footer) {
  border-top: 1px solid rgba(70, 130, 180, 0.25) !important;
  background: rgba(20, 30, 50, 0.5);
  border-radius: 0 0 16px 16px;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .drill-container-status.theme-blue .overview-value {
    font-size: 20px;
  }

  .drill-container-status.theme-blue .status-number {
    font-size: 32px;
  }
}
</style>

<!-- 全局样式 - 处理 teleport 到 body 的组件 -->
<style>
/* 注意：由于父组件 DrillDetailPage 已经添加了 blue-theme-active 的全局样式，
   这里可以不重复添加，避免样式冲突 */
</style>