<template>
  <BaseContextMenu
    :visible="visible"
    :x="x"
    :y="y"
    title="节点选项"
    title-icon="🔘"
    @close="$emit('close')"
  >
    <div class="menu-item" @click="rename">
      <span class="menu-icon">✏️</span>
      <span class="menu-text">编辑名称</span>
      <span class="menu-shortcut">⏎</span>
    </div>
    
    <div class="menu-item" @click="setType">
      <span class="menu-icon">🏷️</span>
      <span class="menu-text">设置类型</span>
      <span class="menu-shortcut">⌘T</span>
    </div>
    
    <div class="menu-item" @click="changeIcon">
      <span class="menu-icon">🎨</span>
      <span class="menu-text">修改图标</span>
      <span class="menu-shortcut">⌘I</span>
    </div>
    
    <div class="menu-divider"></div>
    
    <div class="menu-item" @click="addConnection">
      <span class="menu-icon">🔗</span>
      <span class="menu-text">添加连接</span>
      <span class="menu-shortcut">⌘L</span>
    </div>
    
    <div class="menu-item" @click="centerNode">
      <span class="menu-icon">🧭</span>
      <span class="menu-text">定位中心</span>
      <span class="menu-shortcut">⌘G</span>
    </div>
    
    <div class="menu-divider"></div>
    
    <div class="menu-item" @click="viewDetail">
      <span class="menu-icon">📄</span>
      <span class="menu-text">查看详情</span>
      <span class="menu-shortcut">Space</span>
    </div>
    
    <div class="menu-divider"></div>
    
    <div class="menu-item destructive" @click="deleteNode">
      <span class="menu-icon">🗑️</span>
      <span class="menu-text">删除节点</span>
      <span class="menu-shortcut">⌫</span>
    </div>
  </BaseContextMenu>
</template>

<script setup>
import BaseContextMenu from './BaseContextMenu.vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  x: {
    type: Number,
    default: 0
  },
  y: {
    type: Number,
    default: 0
  },
  element: {
    type: Object,
    default: null
  }
})

const emit = defineEmits([
  'edit-name',
  'set-type',
  'change-icon',
  'add-connection',
  'center-node',
  'view-detail',
  'delete-node',
  'close'
])

const rename = () => {
  emit('edit-name', props.element)
  emit('close')
}

const setType = () => {
  emit('set-type', props.element)
  emit('close')
}

const changeIcon = () => {
  emit('change-icon', props.element)
  emit('close')
}

const addConnection = () => {
  emit('add-connection', props.element)
  emit('close')
}

const centerNode = () => {
  emit('center-node', props.element)
  emit('close')
}

const viewDetail = () => {
  emit('view-detail', props.element)
  emit('close')
}

const deleteNode = () => {
  emit('delete-node', props.element)
  emit('close')
}
</script>

