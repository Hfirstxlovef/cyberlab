<template>
  <div class="topology-layout-controls" :class="{ 'minimized': isMinimized }">
    <!-- 标题栏和最小化按钮 -->
    <div class="control-header">
      <span class="control-title">布局控制</span>
      <button 
        @click="toggleMinimized" 
        class="minimize-btn"
        :title="isMinimized ? '展开' : '最小化'"
      >
        {{ isMinimized ? '📐' : '➖' }}
      </button>
    </div>
    
    <!-- 控制面板内容 -->
    <div class="control-content" v-show="!isMinimized">
      <div class="control-group">
        <label>布局模式:</label>
        <select v-model="currentLayout" @change="onLayoutChange">
          <option value="none">自由布局</option>
          <option value="force">力导向布局</option>
          <option value="circular">环形布局</option>
        </select>
      </div>
      
      <div class="control-group">
        <label>连线弯曲度:</label>
        <input 
          type="range" 
          min="0" 
          max="0.5" 
          step="0.1" 
          v-model="curveness" 
          @input="onCurvenessChange"
        >
        <span>{{ curveness }}</span>
      </div>
      
      <div class="control-group">
        <label>连线样式:</label>
        <select v-model="linkStyle" @change="onLinkStyleChange">
          <option value="straight">直线</option>
          <option value="curve">曲线</option>
          <option value="polyline">折线</option>
        </select>
      </div>
      
      <div class="control-group">
        <label>默认方向:</label>
        <select v-model="defaultDirection" @change="onDefaultDirectionChange">
          <option value="forward">单向 →</option>
          <option value="reverse">反向 ←</option>
          <option value="bidirectional">双向 ↔</option>
          <option value="none">无向 —</option>
        </select>
      </div>
      
      <div class="control-group">
        <button @click="optimizeLayout" class="optimize-btn">
          智能优化布局
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  nodeCount: {
    type: Number,
    default: 0
  },
  linkCount: {
    type: Number,
    default: 0
  }
})

const emit = defineEmits(['layout-change', 'curveness-change', 'link-style-change', 'default-direction-change', 'optimize-layout'])

// 响应式数据
const isMinimized = ref(false)
const currentLayout = ref('none')
const curveness = ref(0.2)
const linkStyle = ref('straight')
const defaultDirection = ref('forward')

// 最小化切换功能
const toggleMinimized = () => {
  isMinimized.value = !isMinimized.value
}

// 根据节点数量智能建议布局
watch(() => props.nodeCount, (count) => {
  if (count > 20) {
    currentLayout.value = 'force'
    curveness.value = 0.3
  } else if (count > 10) {
    curveness.value = 0.2
  } else {
    curveness.value = 0
  }
})

// 事件处理
const onLayoutChange = () => {
  emit('layout-change', currentLayout.value)
}

const onCurvenessChange = () => {
  emit('curveness-change', parseFloat(curveness.value))
}

const onLinkStyleChange = () => {
  emit('link-style-change', linkStyle.value)
}

const onDefaultDirectionChange = () => {
  emit('default-direction-change', defaultDirection.value)
}

const optimizeLayout = () => {
  // 智能优化算法
  let recommendedLayout = 'none'
  let recommendedCurveness = 0
  
  if (props.nodeCount > 50) {
    recommendedLayout = 'force'
    recommendedCurveness = 0.4
  } else if (props.nodeCount > 20) {
    recommendedLayout = 'force'
    recommendedCurveness = 0.3
  } else if (props.nodeCount > 10) {
    recommendedCurveness = 0.2
  }
  
  currentLayout.value = recommendedLayout
  curveness.value = recommendedCurveness
  
  emit('optimize-layout', {
    layout: recommendedLayout,
    curveness: recommendedCurveness,
    nodeCount: props.nodeCount,
    linkCount: props.linkCount
  })
}

// 暴露方法
defineExpose({
  getCurrentSettings: () => ({
    layout: currentLayout.value,
    curveness: curveness.value,
    linkStyle: linkStyle.value
  })
})
</script>

<style scoped>
.topology-layout-controls {
  position: absolute;
  top: 10px;
  right: 10px;
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid #e1e4e8;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  z-index: 1000;
  min-width: 200px;
  transition: all 0.3s ease;
  overflow: hidden;
}

.topology-layout-controls.minimized {
  min-width: auto;
  width: auto;
}

.control-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  border-bottom: 1px solid #e1e4e8;
  background: #f6f8fa;
}

.control-title {
  font-weight: 600;
  font-size: 14px;
  color: #24292e;
}

.minimize-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 16px;
  padding: 4px;
  border-radius: 4px;
  transition: background-color 0.2s ease;
}

.minimize-btn:hover {
  background-color: rgba(0, 0, 0, 0.1);
}

.control-content {
  padding: 16px;
}

.control-group {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.control-group label {
  font-size: 12px;
  color: #666;
  min-width: 80px;
}

.control-group select,
.control-group input[type="range"] {
  flex: 1;
  padding: 4px 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 12px;
}

.optimize-btn {
  background: #409eff;
  color: white;
  border: none;
  padding: 6px 12px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  transition: background-color 0.2s;
}

.optimize-btn:hover {
  background: #337ecc;
}

.control-group span {
  font-size: 12px;
  color: #666;
  min-width: 30px;
}
</style>