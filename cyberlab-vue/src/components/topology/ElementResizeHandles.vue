<template>
  <div v-if="selectedElement && (selectedElement.type === 'rect' || selectedElement.type === 'circle')" 
       class="resize-handles-overlay">
    <!-- 矩形控制点 -->
    <template v-if="selectedElement.type === 'rect'">
      <div
        v-for="(handle, index) in getRectHandles(selectedElement)"
        :key="`handle-${index}`"
        class="resize-handle rect-handle"
        :class="`handle-${handle.direction}`"
        :style="{
          left: Math.max(0, handle.x) + 'px',
          top: Math.max(0, handle.y) + 'px',
          cursor: handle.cursor,
          zIndex: 10002,
          backgroundColor: '#007aff',
          border: '2px solid #ffffff',
          opacity: 1,
          display: 'block'
        }"
        @mousedown="startResize($event, selectedElement, handle)"
      ></div>
    </template>
    
    <!-- 圆形控制点 -->
    <template v-if="selectedElement.type === 'circle'">
      <div
        v-for="(handle, index) in getCircleHandles(selectedElement)"
        :key="`handle-${index}`"
        class="resize-handle circle-handle"
        :style="{
          left: handle.x + 'px',
          top: handle.y + 'px'
        }"
        @mousedown="startResize($event, selectedElement, handle)"
      ></div>
    </template>
  </div>
</template>

<script setup>
import { nextTick, watch } from 'vue'

const props = defineProps({
  selectedElement: Object,
  canvasRef: Object,
  chartRef: Object
})

const emit = defineEmits(['update:customElements', 'render'])

// 监听选中元素变化，用于调试和确保响应性
watch(() => props.selectedElement, (newElement, oldElement) => {
  if (process.env.NODE_ENV === 'development') {
  }
}, { deep: true })

// 生成矩形控制点位置
const getRectHandles = (element) => {
  if (!element || element.type !== 'rect') return []

  // 统一获取位置和尺寸数据
  const x = element.x || element.chartX || element.left || 0
  const y = element.y || element.chartY || element.top || 0
  const width = element.width || element.shape?.width || 100
  const height = element.height || element.shape?.height || 60

  const handleSize = 8
  const half = handleSize / 2

  // 计算8个控制点位置（四个角 + 四个边中点）
  const handles = [
    // 四个角
    { x: x - half, y: y - half, direction: 'nw', cursor: 'nw-resize' },
    { x: x + width - half, y: y - half, direction: 'ne', cursor: 'ne-resize' },
    { x: x + width - half, y: y + height - half, direction: 'se', cursor: 'se-resize' },
    { x: x - half, y: y + height - half, direction: 'sw', cursor: 'sw-resize' },
    // 四个边中点
    { x: x + width / 2 - half, y: y - half, direction: 'n', cursor: 'n-resize' },
    { x: x + width - half, y: y + height / 2 - half, direction: 'e', cursor: 'e-resize' },
    { x: x + width / 2 - half, y: y + height - half, direction: 's', cursor: 's-resize' },
    { x: x - half, y: y + height / 2 - half, direction: 'w', cursor: 'w-resize' }
  ]

  return handles
}

// 生成圆形控制点位置
const getCircleHandles = (element) => {
  if (!element || element.type !== 'circle') return []

  // 统一获取位置和半径数据
  const x = element.x || element.chartX || element.left || 0
  const y = element.y || element.chartY || element.top || 0
  const radius = element.radius || element.shape?.r || 30

  // 圆心坐标
  const centerX = x + radius
  const centerY = y + radius

  const handleSize = 8
  const half = handleSize / 2

  // 四个方向的控制点
  return [
    { 
      x: centerX - half, 
      y: centerY - radius - half, 
      direction: 'n',
      cursor: 'n-resize'
    },
    { 
      x: centerX + radius - half, 
      y: centerY - half, 
      direction: 'e',
      cursor: 'e-resize'
    },
    { 
      x: centerX - half, 
      y: centerY + radius - half, 
      direction: 's',
      cursor: 's-resize'
    },
    { 
      x: centerX - radius - half, 
      y: centerY - half, 
      direction: 'w',
      cursor: 'w-resize'
    }
  ]
}

// 开始调整大小
const startResize = (event, element, handle) => {
  event.preventDefault()
  event.stopPropagation()

  const startX = event.clientX
  const startY = event.clientY
  const startElement = { ...element }

  const handleMouseMove = (e) => {
    const deltaX = e.clientX - startX
    const deltaY = e.clientY - startY
    
    const updatedElement = { ...startElement }
    
    if (element.type === 'rect') {
      const originalX = startElement.x || startElement.chartX || startElement.left || 0
      const originalY = startElement.y || startElement.chartY || startElement.top || 0
      const originalWidth = startElement.width || startElement.shape?.width || 100
      const originalHeight = startElement.height || startElement.shape?.height || 60
      
      let newX = originalX
      let newY = originalY  
      let newWidth = originalWidth
      let newHeight = originalHeight
      
      // 根据控制点方向调整尺寸和位置
      switch (handle.direction) {
        case 'nw': // 左上角
          newX = originalX + deltaX
          newY = originalY + deltaY
          newWidth = Math.max(20, originalWidth - deltaX)
          newHeight = Math.max(20, originalHeight - deltaY)
          break
        case 'ne': // 右上角
          newY = originalY + deltaY
          newWidth = Math.max(20, originalWidth + deltaX)
          newHeight = Math.max(20, originalHeight - deltaY)
          break
        case 'se': // 右下角
          newWidth = Math.max(20, originalWidth + deltaX)
          newHeight = Math.max(20, originalHeight + deltaY)
          break
        case 'sw': // 左下角
          newX = originalX + deltaX
          newWidth = Math.max(20, originalWidth - deltaX)
          newHeight = Math.max(20, originalHeight + deltaY)
          break
        case 'n': // 上边中点
          newY = originalY + deltaY
          newHeight = Math.max(20, originalHeight - deltaY)
          break
        case 'e': // 右边中点
          newWidth = Math.max(20, originalWidth + deltaX)
          break
        case 's': // 下边中点
          newHeight = Math.max(20, originalHeight + deltaY)
          break
        case 'w': // 左边中点
          newX = originalX + deltaX
          newWidth = Math.max(20, originalWidth - deltaX)
          break
      }
      
      // 更新元素的所有坐标和尺寸字段
      updatedElement.x = newX
      updatedElement.y = newY
      updatedElement.chartX = newX
      updatedElement.chartY = newY
      updatedElement.left = newX
      updatedElement.top = newY
      updatedElement.width = newWidth
      updatedElement.height = newHeight
      
      // 如果元素有shape对象，也更新它
      if (updatedElement.shape) {
        updatedElement.shape = {
          ...updatedElement.shape,
          width: newWidth,
          height: newHeight
        }
      } else {
        updatedElement.shape = { width: newWidth, height: newHeight }
      }
      
    } else if (element.type === 'circle') {
      const originalX = startElement.x || startElement.chartX || startElement.left || 0
      const originalY = startElement.y || startElement.chartY || startElement.top || 0
      const originalRadius = startElement.radius || startElement.shape?.r || 30
      
      // 圆心坐标
      const centerX = originalX + originalRadius
      const centerY = originalY + originalRadius
      
      // 计算鼠标当前位置
      const canvasRect = props.canvasRef?.getBoundingClientRect()
      if (!canvasRect) return
      
      const mouseX = e.clientX - canvasRect.left
      const mouseY = e.clientY - canvasRect.top
      
      // 计算从圆心到鼠标的距离作为新半径
      const newRadius = Math.max(10, Math.sqrt(
        Math.pow(mouseX - centerX, 2) + Math.pow(mouseY - centerY, 2)
      ))
      
      // 根据新半径调整位置，保持圆心不变
      const newX = centerX - newRadius
      const newY = centerY - newRadius
      
      // 更新元素的所有坐标和半径字段
      updatedElement.x = newX
      updatedElement.y = newY
      updatedElement.chartX = newX
      updatedElement.chartY = newY
      updatedElement.left = newX
      updatedElement.top = newY
      updatedElement.radius = newRadius
      
      // 如果元素有shape对象，也更新它
      if (updatedElement.shape) {
        updatedElement.shape = {
          ...updatedElement.shape,
          r: newRadius
        }
      } else {
        updatedElement.shape = { r: newRadius }
      }
    }
    
    // 实时更新元素
    emit('update:customElements', updatedElement)
  }

  const handleMouseUp = () => {
    document.removeEventListener('mousemove', handleMouseMove)
    document.removeEventListener('mouseup', handleMouseUp)
    
    // 强制重新渲染
    nextTick(() => {
      emit('render')
    })
  }

  document.addEventListener('mousemove', handleMouseMove)
  document.addEventListener('mouseup', handleMouseUp)
}
</script>

<style scoped>
.resize-handles-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 10000;
}

.resize-handle {
  position: absolute;
  width: 12px;
  height: 12px;
  background: #007aff;
  border: 2px solid #ffffff;
  border-radius: 2px;
  pointer-events: auto;
  z-index: 10001;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  transition: all 0.15s ease;
  transform-origin: center;
}

.resize-handle:hover {
  background: #0056b3;
  transform: scale(1.2);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);
}

.resize-handle:active {
  background: #003d82;
  transform: scale(1.1);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.5);
}

.resize-handle.circle-handle {
  border-radius: 50%;
}

/* 矩形控制点样式 */
.resize-handle.handle-nw { cursor: nw-resize; }
.resize-handle.handle-ne { cursor: ne-resize; }
.resize-handle.handle-se { cursor: se-resize; }
.resize-handle.handle-sw { cursor: sw-resize; }
.resize-handle.handle-n { cursor: n-resize; }
.resize-handle.handle-e { cursor: e-resize; }
.resize-handle.handle-s { cursor: s-resize; }
.resize-handle.handle-w { cursor: w-resize; }

/* 拖拽时的全局样式 */
.resize-handle.dragging {
  background: #003d82;
  transform: scale(1.1);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.6);
  transition: none;
}
</style>