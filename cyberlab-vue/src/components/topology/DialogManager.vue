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
import IconDialog from './dialogs/IconDialog.vue'
import AddCommentDialog from './dialogs/AddCommentDialog.vue'
import NodeDetailDialog from './dialogs/NodeDetailDialog.vue'
import SetTypeDialog from './dialogs/SetTypeDialog.vue'

const props = defineProps({
  nodes: Array
})

const emit = defineEmits(['update-node', 'save-topology'])

// 对话框显示状态
const iconDialogVisible = ref(false)
const commentDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const setTypeDialogVisible = ref(false)
const currentNode = ref(null)

// 显示图标选择对话框
const showIconDialog = (node) => {
  currentNode.value = node
  iconDialogVisible.value = true
}

// 显示备注对话框
const showCommentDialog = (node) => {
  currentNode.value = node
  commentDialogVisible.value = true
}

// 显示节点详情对话框
const showDetailDialog = (node) => {
  currentNode.value = node
  detailDialogVisible.value = true
}

// 显示设备类型设置对话框
const showSetTypeDialog = (node) => {
  currentNode.value = node
  setTypeDialogVisible.value = true
}

// 处理图标选择
const handleIconSelect = (data) => {
  const { nodeId, iconName } = data
  const nodeIndex = props.nodes.findIndex(n => n.id === nodeId)
  if (nodeIndex !== -1) {
    const updatedNode = {
      ...props.nodes[nodeIndex],
      iconName: iconName
    }
    emit('update-node', updatedNode)
    nextTick(() => {
      iconDialogVisible.value = false
      setTimeout(() => {
        currentNode.value = null
      }, 100)
    })
  }
}

// 处理备注保存
const handleCommentSave = (comment) => {
  if (!currentNode.value) return
  const nodeIndex = props.nodes.findIndex(n => n.id === currentNode.value.id)
  if (nodeIndex !== -1) {
    const updatedNode = {
      ...props.nodes[nodeIndex],
      comment: comment
    }
    emit('update-node', updatedNode)
    emit('save-topology')
  }
}

// 处理节点详情保存
const handleNodeDetailSave = (updatedNode) => {
  if (!updatedNode || !updatedNode.id) return
  const nodeIndex = props.nodes.findIndex(n => n.id === updatedNode.id)
  if (nodeIndex !== -1) {
    const newNode = { ...props.nodes[nodeIndex], ...updatedNode }
    emit('update-node', newNode)
    emit('save-topology')
  }
}

// 处理设备类型保存
const handleTypeSave = (typeData) => {
  if (!currentNode.value) return
  
  const nodeIndex = props.nodes.findIndex(n => n.id === currentNode.value.id)
  if (nodeIndex !== -1) {
    const updatedNode = {
      ...props.nodes[nodeIndex],
      iconName: typeData.iconName,
      type: typeData.type,
      tags: typeData.tags,
      category: typeData.category
    }
    
    emit('update-node', updatedNode)
    emit('save-topology')
  }
  
  setTypeDialogVisible.value = false
  currentNode.value = null
}

// 暴露方法给父组件
defineExpose({
  showIconDialog,
  showCommentDialog,
  showDetailDialog,
  showSetTypeDialog
})
</script>