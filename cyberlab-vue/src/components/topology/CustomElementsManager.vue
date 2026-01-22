<template>
  <div class="custom-elements-manager">
    <!-- SVG 覆盖层用于自定义元素 -->
    <svg class="svg-overlay">
      <g v-for="element in customElements" :key="element.id">
        <!-- 文字元素 -->
        <text 
          v-if="element.type === 'text'"
          :x="element.x || element.chartX || element.left || 100"
          :y="(element.y || element.chartY || element.top || 100) + 16"
          :font-size="(element.style?.fontSize || '16px').toString().replace('px', '')"
          :fill="element.style?.fill || '#333333'"
          :font-weight="element.style?.fontWeight || 'normal'"
          @mousedown="startDrag($event, element)"
          @contextmenu.prevent="handleElementContextMenu($event, element)"
          :style="{ cursor: isDragging && dragElement?.id === element.id ? 'grabbing' : 'grab' }"
        >
          {{ element.style?.text || '新文本' }}
        </text>
        
        <!-- 矩形元素 -->
        <rect 
          v-else-if="element.type === 'rect'"
          :x="element.x || element.chartX || element.left || 100"
          :y="element.y || element.chartY || element.top || 100"
          :width="element.width || element.shape?.width || 100"
          :height="element.height || element.shape?.height || 60"
          :fill="element.style?.fill || 'rgba(0, 122, 255, 0.1)'"
          :stroke="selectedElement?.id === element.id ? '#ff6b35' : (element.style?.stroke || element.style?.strokeWidth || '#007aff')"
          :stroke-width="selectedElement?.id === element.id ? '3px' : (element.style?.strokeWidth || element.style?.lineWidth || '2px')"
          :stroke-dasharray="selectedElement?.id === element.id ? '5,5' : 'none'"
          @mousedown="startDrag($event, element)"
          @contextmenu.prevent="handleElementContextMenu($event, element)"
          :style="{ 
            cursor: isDragging && dragElement?.id === element.id ? 'grabbing' : 'grab',
            filter: selectedElement?.id === element.id ? 'drop-shadow(0 0 8px rgba(255, 107, 53, 0.6))' : 'none'
          }"
        />
        
        <!-- 圆形元素 -->
        <circle 
          v-else-if="element.type === 'circle'"
          :cx="(element.x || element.chartX || element.left || 100) + (element.radius || element.shape?.r || 30)"
          :cy="(element.y || element.chartY || element.top || 100) + (element.radius || element.shape?.r || 30)"
          :r="element.radius || element.shape?.r || 30"
          :fill="element.style?.fill || 'rgba(255, 149, 0, 0.1)'"
          :stroke="selectedElement?.id === element.id ? '#ff6b35' : (element.style?.stroke || '#ff9500')"
          :stroke-width="selectedElement?.id === element.id ? '3px' : (element.style?.strokeWidth || element.style?.lineWidth || '2px')"
          :stroke-dasharray="selectedElement?.id === element.id ? '5,5' : 'none'"
          @mousedown="startDrag($event, element)"
          @contextmenu.prevent="handleElementContextMenu($event, element)"
          :style="{ 
            cursor: isDragging && dragElement?.id === element.id ? 'grabbing' : 'grab',
            filter: selectedElement?.id === element.id ? 'drop-shadow(0 0 8px rgba(255, 107, 53, 0.6))' : 'none'
          }"
        />
      </g>
    </svg>
    
    <!-- 缩放控制手柄 -->
    <ElementResizeHandles
      :selected-element="selectedElement"
      :canvas-ref="canvasRef"
      :chart-ref="chartRef"
      @update:customElements="handleElementUpdate"
      @render="$emit('render')"
    />
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import ElementResizeHandles from './ElementResizeHandles.vue'

const props = defineProps({
  customElements: {
    type: Array,
    default: () => []
  },
  canvasRef: {
    type: Object,
    default: null
  },
  chartRef: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['update:customElements', 'element-contextmenu', 'element-selected', 'render'])

// 本地状态管理
const customElements = ref([...props.customElements])
const selectedElement = ref(null)

// 拖拽相关状态
const isDragging = ref(false)
const dragElement = ref(null)
const dragOffset = ref({ x: 0, y: 0 })

// 右键菜单事件处理
const handleElementContextMenu = (event, element) => {
  // 元素右键事件
  
  emit('element-contextmenu', {
    element,
    x: event.clientX,
    y: event.clientY
  })
}

// 元素更新处理
const handleElementUpdate = (updatedElement) => {
  const elementIndex = customElements.value.findIndex(el => el.id === updatedElement.id)
  if (elementIndex !== -1) {
    customElements.value[elementIndex] = updatedElement
    emit('update:customElements', [...customElements.value])
    
    // 同步更新选中元素的引用
    if (selectedElement.value && selectedElement.value.id === updatedElement.id) {
      selectedElement.value = updatedElement
    }
  }
}

// 拖拽事件处理
const startDrag = (e, element) => {
  e.preventDefault()
  e.stopPropagation()
  
  // 设置选中的元素
  selectedElement.value = element
  emit('element-selected', element)
  
  isDragging.value = true
  dragElement.value = element
  
  const rect = props.canvasRef?.getBoundingClientRect() || { left: 0, top: 0 }
  const elementX = element.x || element.chartX || element.left || 0
  const elementY = element.y || element.chartY || element.top || 0
  
  dragOffset.value = {
    x: e.clientX - rect.left - elementX,
    y: e.clientY - rect.top - elementY
  }
  
  // 添加全局鼠标事件监听
  document.addEventListener('mousemove', handleDragMove)
  document.addEventListener('mouseup', stopDrag)
  
  // 设置拖拽时的鼠标样式
  document.body.style.cursor = 'grabbing'
}

const handleDragMove = (e) => {
  if (!isDragging.value || !dragElement.value || !props.canvasRef) return
  
  const rect = props.canvasRef.getBoundingClientRect()
  const newX = e.clientX - rect.left - dragOffset.value.x
  const newY = e.clientY - rect.top - dragOffset.value.y
  
  // 限制在画布范围内
  const maxX = rect.width - 50
  const maxY = rect.height - 50
  const constrainedX = Math.max(0, Math.min(maxX, newX))
  const constrainedY = Math.max(0, Math.min(maxY, newY))
  
  // 找到并更新元素位置
  const elementIndex = customElements.value.findIndex(el => el.id === dragElement.value.id)
  if (elementIndex !== -1) {
    const updatedElement = { ...customElements.value[elementIndex] }
    
    // 更新所有可能的坐标字段
    updatedElement.x = constrainedX
    updatedElement.y = constrainedY
    updatedElement.chartX = constrainedX
    updatedElement.chartY = constrainedY
    updatedElement.left = constrainedX
    updatedElement.top = constrainedY
    
    customElements.value[elementIndex] = updatedElement
    
    // 同步更新选中元素的引用
    if (selectedElement.value && selectedElement.value.id === updatedElement.id) {
      selectedElement.value = updatedElement
    }
  }
}

const stopDrag = () => {
  if (isDragging.value) {
    isDragging.value = false
    dragElement.value = null
    
    // 移除全局事件监听
    document.removeEventListener('mousemove', handleDragMove)
    document.removeEventListener('mouseup', stopDrag)
    
    // 恢复鼠标样式
    document.body.style.cursor = 'default'
    
    // 发出更新事件
    emit('update:customElements', [...customElements.value])
  }
}

// 添加新元素
const addElement = (element) => {
  customElements.value.push(element)
  emit('update:customElements', [...customElements.value])
}

// 删除元素
const deleteElement = (elementId) => {
  const elementIndex = customElements.value.findIndex(el => el.id === elementId)
  if (elementIndex !== -1) {
    customElements.value.splice(elementIndex, 1)
    
    // 如果删除的是选中元素，清除选中状态
    if (selectedElement.value && selectedElement.value.id === elementId) {
      selectedElement.value = null
      emit('element-selected', null)
    }
    
    emit('update:customElements', [...customElements.value])
  }
}

// 更新特定元素
const updateElement = (elementId, updates) => {
  const elementIndex = customElements.value.findIndex(el => el.id === elementId)
  if (elementIndex !== -1) {
    const updatedElement = {
      ...customElements.value[elementIndex],
      ...updates
    }
    
    customElements.value[elementIndex] = updatedElement
    
    // 同步更新选中元素的引用
    if (selectedElement.value && selectedElement.value.id === elementId) {
      selectedElement.value = updatedElement
    }
    
    emit('update:customElements', [...customElements.value])
  }
}

// 清空所有元素
const clearAll = () => {
  customElements.value = []
  selectedElement.value = null
  emit('element-selected', null)
  emit('update:customElements', [])
}

// 取消选中
const clearSelection = () => {
  selectedElement.value = null
  emit('element-selected', null)
}

// 监听props变化，同步到本地状态
watch(() => props.customElements, (newCustomElements) => {
  if (newCustomElements && Array.isArray(newCustomElements)) {
    customElements.value = [...newCustomElements]
  }
}, { deep: true, immediate: true })

// 暴露给父组件的方法
defineExpose({
  addElement,
  deleteElement,
  updateElement,
  clearAll,
  clearSelection,
  selectedElement
})
</script>

<style scoped>
.custom-elements-manager {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 100;
}

.svg-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 100;
}

.svg-overlay text,
.svg-overlay rect,
.svg-overlay circle {
  pointer-events: all;
  user-select: none;
  transition: filter 0.2s ease;
}

.svg-overlay text:hover,
.svg-overlay rect:hover,
.svg-overlay circle:hover {
  filter: drop-shadow(0 0 4px rgba(64, 158, 255, 0.6));
}

.svg-overlay text:active,
.svg-overlay rect:active,
.svg-overlay circle:active {
  filter: drop-shadow(0 0 6px rgba(64, 158, 255, 0.8));
  opacity: 0.8;
}
</style>