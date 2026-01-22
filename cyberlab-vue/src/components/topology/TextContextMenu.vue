<template>
  <BaseContextMenu
    :visible="visible"
    :x="x"
    :y="y"
    title="文字选项"
    title-icon="📝"
    @close="$emit('close')"
  >
    <div class="menu-item" @click="editContent">
      <span class="menu-icon">✏️</span>
      <span class="menu-text">编辑内容</span>
      <span class="menu-shortcut">⏎</span>
    </div>
    
    <div class="menu-item" @click="modifyFontSize">
      <span class="menu-icon">🔠</span>
      <span class="menu-text">修改字体大小</span>
      <span class="menu-shortcut">⌘+</span>
    </div>
    
    <div class="menu-item submenu" @click="toggleColorMenu">
      <span class="menu-icon">🎨</span>
      <span class="menu-text">修改颜色</span>
      <span class="menu-shortcut">▶</span>
    </div>
    
    <!-- 颜色选择器 -->
    <div v-if="showColorMenu" class="color-menu">
      <div class="color-options">
        <div 
          v-for="color in textColors" 
          :key="'text-' + color.value"
          class="color-option"
          :style="{ backgroundColor: color.value }"
          @click="selectColor(color.value)"
          :title="color.name"
        ></div>
      </div>
    </div>
    
    <div class="menu-divider"></div>
    
    <div class="menu-item" @click="togglePin">
      <span class="menu-icon">📌</span>
      <span class="menu-text">{{ textElement?.pinned ? '取消固定' : '固定位置' }}</span>
      <span class="menu-shortcut">⌘L</span>
    </div>
    
    <div class="menu-divider"></div>
    
    <div class="menu-item destructive" @click="deleteText">
      <span class="menu-icon">🗑️</span>
      <span class="menu-text">删除文字</span>
      <span class="menu-shortcut">⌫</span>
    </div>
  </BaseContextMenu>
</template>

<script setup>
import { ref } from 'vue'
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
  textElement: {
    type: Object,
    default: null
  }
})

const emit = defineEmits([
  'edit-content',
  'modify-font-size', 
  'modify-color',
  'delete-text',
  'toggle-pin',
  'close'
])

// 颜色菜单显示状态
const showColorMenu = ref(false)

// 文字颜色选项
const textColors = [
  { name: '黑色', value: '#000000' },
  { name: '深灰', value: '#333333' },
  { name: '灰色', value: '#666666' },
  { name: '浅灰', value: '#999999' },
  { name: '白色', value: '#FFFFFF' },
  { name: '红色', value: '#FF0000' },
  { name: '橙色', value: '#FF9500' },
  { name: '黄色', value: '#FFCC00' },
  { name: '绿色', value: '#00CC00' },
  { name: '蓝色', value: '#007AFF' },
  { name: '紫色', value: '#9900CC' },
  { name: '粉色', value: '#FF3B82' }
]

const editContent = () => {
  emit('edit-content', props.textElement)
  emit('close')
}

const modifyFontSize = () => {
  emit('modify-font-size', props.textElement)
  emit('close')
}

const toggleColorMenu = () => {
  showColorMenu.value = !showColorMenu.value
}

const selectColor = (color) => {
  emit('modify-color', props.textElement, color)
  showColorMenu.value = false
  emit('close')
}

const deleteText = () => {
  emit('delete-text', props.textElement)
  emit('close')
}

const togglePin = () => {
  emit('toggle-pin', props.textElement)
  emit('close')
}
</script>

<style scoped>
.color-menu {
  background: #ffffff;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  padding: 8px;
  margin: 2px 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.color-options {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 6px;
  min-width: 120px;
}

.color-option {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  border: 2px solid #e0e0e0;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
}

.color-option:hover {
  transform: scale(1.1);
  border-color: #007aff;
  box-shadow: 0 2px 6px rgba(0, 122, 255, 0.3);
}

.menu-item.submenu .menu-shortcut {
  color: #007aff;
  font-weight: bold;
}

.menu-item.submenu:hover .menu-shortcut {
  color: #ffffff;
}
</style>