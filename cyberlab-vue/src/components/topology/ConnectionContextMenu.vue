<template>
  <BaseContextMenu
    :visible="visible"
    :x="x"
    :y="y"
    title="连线选项"
    title-icon="🔗"
    @close="$emit('close')"
  >
    <div class="menu-item" @click="editLabel">
      <span class="menu-icon">✏️</span>
      <span class="menu-text">编辑标签</span>
      <span class="menu-shortcut">⌘E</span>
    </div>
    
    <div class="menu-item" @click="setBandwidth">
      <span class="menu-icon">📊</span>
      <span class="menu-text">设置带宽</span>
      <span class="menu-shortcut">⌘B</span>
    </div>
    
    <div class="menu-item submenu" @click="toggleColorMenu">
      <span class="menu-icon">🎨</span>
      <span class="menu-text">更改颜色</span>
      <span class="menu-shortcut">▶</span>
    </div>
    
    <!-- 颜色选择器 -->
    <div v-if="showColorMenu" class="color-menu">
      <div class="color-options">
        <div 
          v-for="color in lineColors" 
          :key="'line-' + color.value"
          class="color-option"
          :style="{ backgroundColor: color.value }"
          @click="selectColor(color.value)"
          :title="color.name"
        ></div>
      </div>
    </div>
    
    <div class="menu-item submenu" @click="toggleDirectionMenu">
      <span class="menu-icon">➡️</span>
      <span class="menu-text">设置方向</span>
      <span class="menu-shortcut">▶</span>
    </div>
    
    <!-- 方向选择器 -->
    <div v-if="showDirectionMenu" class="direction-menu">
      <div class="direction-options">
        <div 
          class="direction-option"
          @click="setDirection('forward')"
          :class="{ active: currentDirection === 'forward' }"
          title="单向：从源到目标"
        >
          <span class="direction-icon">→</span>
          <span class="direction-text">单向</span>
        </div>
        <div 
          class="direction-option"
          @click="setDirection('reverse')"
          :class="{ active: currentDirection === 'reverse' }"
          title="反向：从目标到源"
        >
          <span class="direction-icon">←</span>
          <span class="direction-text">反向</span>
        </div>
        <div 
          class="direction-option"
          @click="setDirection('bidirectional')"
          :class="{ active: currentDirection === 'bidirectional' }"
          title="双向连接"
        >
          <span class="direction-icon">↔</span>
          <span class="direction-text">双向</span>
        </div>
        <div 
          class="direction-option"
          @click="setDirection('none')"
          :class="{ active: currentDirection === 'none' }"
          title="无方向"
        >
          <span class="direction-icon">—</span>
          <span class="direction-text">无向</span>
        </div>
      </div>
    </div>
    
    <div class="menu-item" @click="toggleDirection">
      <span class="menu-icon">🔄</span>
      <span class="menu-text">切换方向</span>
      <span class="menu-shortcut">⌘D</span>
    </div>
    
    <div class="menu-divider"></div>
    
    <div class="menu-item destructive" @click="deleteLink">
      <span class="menu-icon">🗑️</span>
      <span class="menu-text">删除连接</span>
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
  element: {
    type: Object,
    default: null
  }
})

const emit = defineEmits([
  'edit-label',
  'set-bandwidth',
  'change-color',
  'set-direction',
  'toggle-direction',
  'delete-connection',
  'close'
])

// 颜色菜单显示状态
const showColorMenu = ref(false)
// 方向菜单显示状态
const showDirectionMenu = ref(false)

// 当前连线方向
const currentDirection = ref('forward') // forward, reverse, bidirectional, none

// 连线颜色选项
const lineColors = [
  { name: '深灰', value: '#333333' },
  { name: '灰色', value: '#666666' },
  { name: '浅灰', value: '#999999' },
  { name: '黑色', value: '#000000' },
  { name: '红色', value: '#FF0000' },
  { name: '橙色', value: '#FF9500' },
  { name: '黄色', value: '#FFCC00' },
  { name: '绿色', value: '#00CC00' },
  { name: '蓝色', value: '#007AFF' },
  { name: '紫色', value: '#9900CC' },
  { name: '粉色', value: '#FF3B82' },
  { name: '青色', value: '#00CCCC' }
]

const editLabel = () => {
  emit('edit-label', props.element)
  emit('close')
}

const setBandwidth = () => {
  emit('set-bandwidth', props.element)
  emit('close')
}

const toggleColorMenu = () => {
  showColorMenu.value = !showColorMenu.value
  // 关闭方向菜单
  showDirectionMenu.value = false
}

const toggleDirectionMenu = () => {
  showDirectionMenu.value = !showDirectionMenu.value
  // 关闭颜色菜单
  showColorMenu.value = false
  
  // 更新当前方向显示
  if (props.element?.direction) {
    currentDirection.value = props.element.direction
  } else {
    currentDirection.value = 'forward' // 默认单向
  }
}

const selectColor = (color) => {
  emit('change-color', props.element, color)
  showColorMenu.value = false
  emit('close')
}

const setDirection = (direction) => {
  currentDirection.value = direction
  emit('set-direction', props.element, direction)
  showDirectionMenu.value = false
  emit('close')
}

const toggleDirection = () => {
  emit('toggle-direction', props.element)
  emit('close')
}

const deleteLink = () => {
  emit('delete-connection', props.element)
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

/* 方向菜单样式 */
.direction-menu {
  background: #ffffff;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  padding: 8px;
  margin: 2px 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.direction-options {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 140px;
}

.direction-option {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid transparent;
}

.direction-option:hover {
  background-color: #f5f5f5;
  border-color: #007aff;
}

.direction-option.active {
  background-color: #e6f3ff;
  border-color: #007aff;
  color: #007aff;
}

.direction-icon {
  font-size: 16px;
  font-weight: bold;
  min-width: 20px;
  text-align: center;
}

.direction-text {
  font-size: 12px;
  font-weight: 500;
}

.menu-item.submenu .menu-shortcut {
  color: #007aff;
  font-weight: bold;
}

.menu-item.submenu:hover .menu-shortcut {
  color: #ffffff;
}
</style>