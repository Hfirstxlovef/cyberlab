<template>
  <BaseContextMenu
    :visible="visible"
    :x="x"
    :y="y"
    title="节点选项"
    title-icon="🔘"
    @close="hide"
  >
    <div class="menu-item" @click="emitCommand('rename')">
      <span class="menu-icon">✏️</span>
      <span class="menu-text">编辑名称</span>
      <span class="menu-shortcut">⏎</span>
    </div>
    
    <div class="menu-item" @click="emitCommand('type')">
      <span class="menu-icon">🏷️</span>
      <span class="menu-text">设置类型</span>
      <span class="menu-shortcut">⌘T</span>
    </div>
    
    <div class="menu-item" @click="emitCommand('icon')">
      <span class="menu-icon">🎨</span>
      <span class="menu-text">修改图标</span>
      <span class="menu-shortcut">⌘I</span>
    </div>
    
    <div class="menu-divider"></div>
    
    <div class="menu-item" @click="emitCommand('connect')">
      <span class="menu-icon">🔗</span>
      <span class="menu-text">添加连接</span>
      <span class="menu-shortcut">⌘L</span>
    </div>
    
    <div class="menu-item" @click="emitCommand('center')">
      <span class="menu-icon">🧭</span>
      <span class="menu-text">定位中心</span>
      <span class="menu-shortcut">⌘G</span>
    </div>
    
    <div class="menu-divider"></div>
    
    <div class="menu-item" @click="emitCommand('detail')">
      <span class="menu-icon">📄</span>
      <span class="menu-text">查看详情</span>
      <span class="menu-shortcut">Space</span>
    </div>
    
    <div class="menu-divider"></div>
    
    <div class="menu-item destructive" @click="emitCommand('delete')">
      <span class="menu-icon">🗑️</span>
      <span class="menu-text">删除节点</span>
      <span class="menu-shortcut">⌫</span>
    </div>
  </BaseContextMenu>
</template>

<script setup>
import { ref } from 'vue'
import BaseContextMenu from './BaseContextMenu.vue'

const emit = defineEmits([
  'rename', 'set-type', 'change-icon', 'connect',
  'center', 'view-detail', 'delete'
])

const visible = ref(false)
const x = ref(0)
const y = ref(0)
let currentNode = null

function show({ node, x: posX, y: posY }) {
  x.value = posX
  y.value = posY
  currentNode = node
  visible.value = true

  setTimeout(() => {
    document.addEventListener('click', hide)
  }, 0)
}

function hide() {
  visible.value = false
  document.removeEventListener('click', hide)
}

function emitCommand(cmd) {
  switch (cmd) {
    case 'rename':
      emit('rename', currentNode.id)
      break
    case 'type':
      emit('set-type', currentNode.id)
      break
    case 'icon':
      emit('change-icon', currentNode.id)
      break
    case 'connect':
      emit('connect', currentNode.id)
      break
    case 'center':
      emit('center', currentNode.id)
      break
    case 'detail':
      emit('view-detail', currentNode.id)
      break
    case 'delete':
      emit('delete', currentNode.id)
      break
  }
  hide()
}

defineExpose({ show }) // 允许父组件通过 ref 调用 show()
</script>

