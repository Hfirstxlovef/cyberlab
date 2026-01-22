<template>
  <BaseContextMenu
    :visible="visible"
    :x="x"
    :y="y"
    title="圆形选项"
    title-icon="⚪"
    @close="$emit('close')"
  >
    <div class="menu-item" @click="editLabel">
      <span class="menu-icon">✏️</span>
      <span class="menu-text">编辑标签</span>
      <span class="menu-shortcut">⏎</span>
    </div>
    
    <div class="menu-item submenu" @click="toggleBorderColorMenu">
      <span class="menu-icon">🎨</span>
      <span class="menu-text">修改边框颜色</span>
      <span class="menu-shortcut">▶</span>
    </div>
    
    <!-- 边框颜色选择器 -->
    <div v-if="showBorderColorMenu" class="color-menu">
      <div class="color-options">
        <div 
          v-for="color in basicColors" 
          :key="'border-' + color.value"
          class="color-option"
          :style="{ backgroundColor: color.value }"
          @click="selectBorderColor(color.value)"
          :title="color.name"
        ></div>
      </div>
    </div>
    
    <div class="menu-item submenu" @click="toggleFillColorMenu">
      <span class="menu-icon">🧱</span>
      <span class="menu-text">修改填充颜色</span>
      <span class="menu-shortcut">▶</span>
    </div>
    
    <!-- 填充颜色选择器 -->
    <div v-if="showFillColorMenu" class="color-menu">
      <div class="color-options">
        <div 
          v-for="color in basicFillColors" 
          :key="'fill-' + color.value"
          class="color-option"
          :style="{ backgroundColor: color.value }"
          @click="selectFillColor(color.value)"
          :title="color.name"
        ></div>
      </div>
    </div>
    
    <div class="menu-item" @click="adjustRadius">
      <span class="menu-icon">📐</span>
      <span class="menu-text">调整半径</span>
      <span class="menu-shortcut">⌘R</span>
    </div>
    
    <div class="menu-divider"></div>
    
    <div class="menu-item" @click="togglePin">
      <span class="menu-icon">📌</span>
      <span class="menu-text">{{ circleElement?.pinned ? '取消固定' : '固定位置' }}</span>
      <span class="menu-shortcut">⌘L</span>
    </div>
    
    <div class="menu-divider"></div>
    
    <div class="menu-item destructive" @click="deleteCircle">
      <span class="menu-icon">🗑️</span>
      <span class="menu-text">删除圆形</span>
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
  circleElement: {
    type: Object,
    default: null
  }
})

const emit = defineEmits([
  'edit-label',
  'modify-border-color',
  'modify-fill-color',
  'adjust-radius',
  'toggle-pin',
  'delete-circle',
  'close'
])

// 颜色菜单显示状态
const showBorderColorMenu = ref(false)
const showFillColorMenu = ref(false)

// 基本边框颜色选项
const basicColors = [
  { name: '黑色', value: '#000000' },
  { name: '深灰', value: '#333333' },
  { name: '灰色', value: '#666666' },
  { name: '浅灰', value: '#999999' },
  { name: '红色', value: '#FF0000' },
  { name: '橙色', value: '#FF9500' },
  { name: '黄色', value: '#FFCC00' },
  { name: '绿色', value: '#00CC00' },
  { name: '蓝色', value: '#007AFF' },
  { name: '紫色', value: '#9900CC' },
  { name: '粉色', value: '#FF3B82' },
  { name: '青色', value: '#00CCCC' }
]

// 基本填充颜色选项（带透明度）
const basicFillColors = [
  { name: '透明', value: 'transparent' },
  { name: '浅灰', value: 'rgba(128, 128, 128, 0.1)' },
  { name: '淡红', value: 'rgba(255, 0, 0, 0.1)' },
  { name: '淡橙', value: 'rgba(255, 149, 0, 0.1)' },
  { name: '淡黄', value: 'rgba(255, 204, 0, 0.1)' },
  { name: '淡绿', value: 'rgba(0, 204, 0, 0.1)' },
  { name: '淡蓝', value: 'rgba(0, 122, 255, 0.1)' },
  { name: '淡紫', value: 'rgba(153, 0, 204, 0.1)' },
  { name: '淡粉', value: 'rgba(255, 59, 130, 0.1)' },
  { name: '淡青', value: 'rgba(0, 204, 204, 0.1)' },
  { name: '深灰', value: 'rgba(128, 128, 128, 0.3)' },
  { name: '白色', value: 'rgba(255, 255, 255, 0.8)' }
]

const editLabel = () => {
  emit('edit-label', props.circleElement)
  emit('close')
}

const toggleBorderColorMenu = () => {
  showBorderColorMenu.value = !showBorderColorMenu.value
  showFillColorMenu.value = false
}

const toggleFillColorMenu = () => {
  showFillColorMenu.value = !showFillColorMenu.value
  showBorderColorMenu.value = false
}

const selectBorderColor = (color) => {
  emit('modify-border-color', props.circleElement, color)
  showBorderColorMenu.value = false
  emit('close')
}

const selectFillColor = (color) => {
  emit('modify-fill-color', props.circleElement, color)
  showFillColorMenu.value = false
  emit('close')
}

const adjustRadius = () => {
  emit('adjust-radius', props.circleElement)
  emit('close')
}

const togglePin = () => {
  emit('toggle-pin', props.circleElement)
  emit('close')
}

const deleteCircle = () => {
  emit('delete-circle', props.circleElement)
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