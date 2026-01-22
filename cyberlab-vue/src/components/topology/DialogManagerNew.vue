<template>
  <div class="dialog-manager">
    <!-- 节点功能对话框 -->
    <NodeRenameDialog
      :visible="dialogStates.rename.visible"
      :node="dialogStates.rename.node"
      @confirm="handleRenameConfirm"
      @close="() => closeDialog('rename')"
    />
    
    <NodeDetailDialog
      :visible="dialogStates.detail.visible"
      :node="dialogStates.detail.node"
      :nodes="nodes"
      :links="links"
      @edit="handleDetailEdit"
      @close="() => closeDialog('detail')"
    />
    
    <NodeTypeDialog
      :visible="dialogStates.type.visible"
      :node="dialogStates.type.node"
      @confirm="handleTypeConfirm"
      @close="() => closeDialog('type')"
    />
    
    <NodeIconDialog
      :visible="dialogStates.icon.visible"
      :node="dialogStates.icon.node"
      @confirm="handleIconConfirm"
      @close="() => closeDialog('icon')"
    />
    
    <NodeConnectionDialog
      :visible="dialogStates.connection.visible"
      :node="dialogStates.connection.node"
      :nodes="nodes"
      :links="links"
      @confirm="handleConnectionConfirm"
      @close="() => closeDialog('connection')"
    />
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import NodeRenameDialog from './NodeRenameDialog.vue'
import NodeDetailDialog from './NodeDetailDialog.vue'
import NodeTypeDialog from './NodeTypeDialog.vue'
import NodeIconDialog from './NodeIconDialog.vue'
import NodeConnectionDialog from './NodeConnectionDialog.vue'

const props = defineProps({
  nodes: {
    type: Array,
    default: () => []
  },
  links: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits([
  'update-nodes',
  'update-links', 
  'update-data',
  'render-chart'
])

// 对话框状态管理
const dialogStates = ref({
  rename: { visible: false, node: null },
  detail: { visible: false, node: null },
  type: { visible: false, node: null },
  icon: { visible: false, node: null },
  connection: { visible: false, node: null }
})

// 辅助函数：从图标URL中提取iconName
const extractIconNameFromUrl = (iconUrl) => {
  if (!iconUrl || !iconUrl.includes('image://')) {
    return 'pc' // 默认图标名称
  }
  
  // 从 "image://icons/fiber_switch.png" 中提取 "fiber_switch"
  const path = iconUrl.replace('image:///', '').replace('image://', '')
  const filename = path.split('/').pop() || ''
  const iconName = filename.replace(/\.(png|svg|jpg|jpeg)$/i, '')
  
  // 提取图标名称
  
  return iconName || 'pc'
}

// 对话框显示方法
const showRenameDialog = (node) => {
  // 显示重命名对话框
  dialogStates.value.rename.visible = true
  dialogStates.value.rename.node = node
}

const showDetailDialog = (node) => {
  // 显示详情对话框
  dialogStates.value.detail.visible = true
  dialogStates.value.detail.node = node
}

const showTypeDialog = (node) => {
  // 显示类型设置对话框
  dialogStates.value.type.visible = true
  dialogStates.value.type.node = node
}

const showIconDialog = (node) => {
  // 显示图标对话框
  dialogStates.value.icon.visible = true
  dialogStates.value.icon.node = node
}

const showConnectionDialog = (node) => {
  // 显示连接对话框
  dialogStates.value.connection.visible = true
  dialogStates.value.connection.node = node
}

// 对话框确认处理函数
const handleRenameConfirm = (data) => {
  // 确认重命名
  const { nodeId, newName } = data
  
  const updatedNodes = [...props.nodes]
  const nodeIndex = updatedNodes.findIndex(n => n.id === nodeId)
  
  // 查找节点索引（重命名）
  if (nodeIndex !== -1) {
    updatedNodes[nodeIndex].name = newName
    
    emit('update-data', { nodes: updatedNodes, links: props.links })
    
    // 强制触发图表更新
    nextTick(() => {
      // 正在强制更新图表
      emit('render-chart')
    })
  } else {
    // 未找到要重命名的节点
  }
  
  closeDialog('rename')
}

const handleIconConfirm = (data) => {
  // 确认更换图标
  const { nodeId, newIcon } = data
  
  const updatedNodes = [...props.nodes]
  const nodeIndex = updatedNodes.findIndex(n => n.id === nodeId)
  
  // 查找节点索引（图标更换）
  if (nodeIndex !== -1) {
    updatedNodes[nodeIndex].symbol = newIcon
    
    emit('update-data', { nodes: updatedNodes, links: props.links })
    
    // 🔧 立即清理该节点的本地图标覆盖，避免下次加载时被覆盖
    try {
      const iconOverrides = JSON.parse(localStorage.getItem('topology-icon-overrides') || '{}')
      if (iconOverrides[nodeId]) {
        delete iconOverrides[nodeId]
        localStorage.setItem('topology-icon-overrides', JSON.stringify(iconOverrides))
        // 已清理节点本地图标覆盖
        
        // 如果没有任何覆盖了，清空整个localStorage项
        if (Object.keys(iconOverrides).length === 0) {
          localStorage.removeItem('topology-icon-overrides')
          // 已清空所有本地图标覆盖
        }
      }
    } catch {
      // 清理本地图标覆盖失败
    }
    
    // 强制触发图表更新
    nextTick(() => {
      // 正在强制更新图表（图标更换）
      emit('render-chart')
    })
  } else {
    // 未找到要更换图标的节点
  }
  
  closeDialog('icon')
}

const handleTypeConfirm = (data) => {
  // 确认设置类型
  const { nodeId, type, typeName, typeIcon, description } = data
  
  const updatedNodes = [...props.nodes]
  const nodeIndex = updatedNodes.findIndex(n => n.id === nodeId)
  
  // 查找节点索引（类型设置）
  if (nodeIndex !== -1) {
    updatedNodes[nodeIndex].category = type
    updatedNodes[nodeIndex].typeName = typeName
    updatedNodes[nodeIndex].typeIcon = typeIcon
    updatedNodes[nodeIndex].description = description
    
    emit('update-data', { nodes: updatedNodes, links: props.links })
    
    // 强制触发图表更新
    nextTick(() => {
      // 正在强制更新图表（类型设置）
      emit('render-chart')
    })
  } else {
    // 未找到要设置类型的节点
  }
  
  closeDialog('type')
}

const handleConnectionConfirm = (data) => {
  // 确认创建连接
  const { connections } = data
  
  const updatedLinks = [...props.links, ...connections]
  
  // 连接创建更新
  
  emit('update-data', { nodes: props.nodes, links: updatedLinks })
  
  // 强制触发图表更新
  nextTick(() => {
    // 正在强制更新图表（连接创建）
    emit('render-chart')
  })
  
  closeDialog('connection')
}

const handleDetailEdit = (node) => {
  // 编辑节点详情
  // 关闭详情对话框，打开重命名对话框
  dialogStates.value.detail.visible = false
  dialogStates.value.rename.visible = true
  dialogStates.value.rename.node = node
}

// 对话框关闭处理函数
const closeDialog = (dialogType) => {
  // 关闭对话框
  dialogStates.value[dialogType].visible = false
  dialogStates.value[dialogType].node = null
}

// 关闭所有对话框
const closeAllDialogs = () => {
  Object.keys(dialogStates.value).forEach(key => {
    dialogStates.value[key].visible = false
    dialogStates.value[key].node = null
  })
}

// 暴露给父组件的方法
defineExpose({
  showRenameDialog,
  showDetailDialog,
  showTypeDialog,
  showIconDialog,
  showConnectionDialog,
  closeDialog,
  closeAllDialogs,
  extractIconNameFromUrl  // 暴露工具函数
})
</script>

<style scoped>
.dialog-manager {
  /* 不需要额外样式，因为对话框都是独立的 */
}
</style>