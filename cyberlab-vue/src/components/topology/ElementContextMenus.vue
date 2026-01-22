<template>
  <!-- 文字右键菜单 -->
  <Teleport to="body">
    <TextContextMenu 
      v-if="textContextMenu.visible && !isUnmounted" 
      :visible="textContextMenu.visible"
      :x="textContextMenu.x" 
      :y="textContextMenu.y" 
      :text-element="textContextMenu.element"
      @edit-content="handleEditTextContent" 
      @modify-font-size="handleModifyFontSize"
      @modify-color="handleModifyColor" 
      @delete-text="handleDeleteText" 
      @toggle-pin="handleTogglePin"
      @close="closeTextContextMenu" 
    />
  </Teleport>

  <!-- 文字编辑对话框 -->
  <Teleport to="body">
    <TextEditDialog 
      v-if="textEditDialog.visible && !isUnmounted" 
      :visible="textEditDialog.visible"
      :text-element="textEditDialog.element" 
      @save="handleSaveTextEdit" 
      @close="closeTextEditDialog" 
    />
  </Teleport>

  <!-- 矩形右键菜单 -->
  <Teleport to="body">
    <RectContextMenu 
      v-if="rectContextMenu.visible && !isUnmounted" 
      :visible="rectContextMenu.visible"
      :x="rectContextMenu.x" 
      :y="rectContextMenu.y" 
      :rect-element="rectContextMenu.element"
      @edit-label="handleRectEditLabel"
      @modify-border-color="handleRectModifyBorderColor"
      @modify-fill-color="handleRectModifyFillColor" 
      @adjust-size="handleRectAdjustSize"
      @toggle-pin="handleRectTogglePin" 
      @delete-rect="handleRectDelete"
      @close="closeRectContextMenu" 
    />
  </Teleport>

  <!-- 圆形右键菜单 -->
  <Teleport to="body">
    <CircleContextMenu 
      v-if="circleContextMenu.visible && !isUnmounted" 
      :visible="circleContextMenu.visible"
      :x="circleContextMenu.x" 
      :y="circleContextMenu.y" 
      :circle-element="circleContextMenu.element"
      @edit-label="handleCircleEditLabel"
      @modify-border-color="handleCircleModifyBorderColor"
      @modify-fill-color="handleCircleModifyFillColor" 
      @adjust-radius="handleCircleAdjustRadius"
      @toggle-pin="handleCircleTogglePin"
      @delete-circle="handleCircleDelete" 
      @close="closeCircleContextMenu" 
    />
  </Teleport>
</template>

<script setup>
import { ref } from 'vue'
import TextContextMenu from './TextContextMenu.vue'
import TextEditDialog from './TextEditDialog.vue'
import RectContextMenu from './RectContextMenu.vue'
import CircleContextMenu from './CircleContextMenu.vue'

defineProps({
  isUnmounted: Boolean
})

const emit = defineEmits(['update-element', 'delete-element', 'render'])

// 文字右键菜单状态
const textContextMenu = ref({
  visible: false,
  x: 0,
  y: 0,
  element: null
})

// 矩形右键菜单状态
const rectContextMenu = ref({
  visible: false,
  x: 0,
  y: 0,
  element: null
})

// 圆形右键菜单状态
const circleContextMenu = ref({
  visible: false,
  x: 0,
  y: 0,
  element: null
})

// 文字编辑对话框状态
const textEditDialog = ref({
  visible: false,
  element: null
})

// 显示文字右键菜单
const showTextContextMenu = (data) => {
  if (data && data.element && typeof data.x === 'number' && typeof data.y === 'number') {
    textContextMenu.value = {
      visible: true,
      x: Math.max(0, Math.min(window.innerWidth - 200, data.x)),
      y: Math.max(0, Math.min(window.innerHeight - 300, data.y)),
      element: data.element
    }
  }
}

// 显示矩形右键菜单
const showRectContextMenu = (data) => {
  if (data && data.element && typeof data.x === 'number' && typeof data.y === 'number') {
    rectContextMenu.value = {
      visible: true,
      x: Math.max(0, Math.min(window.innerWidth - 200, data.x)),
      y: Math.max(0, Math.min(window.innerHeight - 350, data.y)),
      element: data.element
    }
  }
}

// 显示圆形右键菜单
const showCircleContextMenu = (data) => {
  if (data && data.element && typeof data.x === 'number' && typeof data.y === 'number') {
    circleContextMenu.value = {
      visible: true,
      x: Math.max(0, Math.min(window.innerWidth - 200, data.x)),
      y: Math.max(0, Math.min(window.innerHeight - 350, data.y)),
      element: data.element
    }
  }
}

// 文字菜单事件处理
const closeTextContextMenu = () => {
  textContextMenu.value = {
    visible: false,
    x: 0,
    y: 0,
    element: null
  }
}

const handleEditTextContent = (textElement) => {
  textEditDialog.value = {
    visible: true,
    element: textElement
  }
}

const handleModifyFontSize = (textElement) => {
  const currentSize = textElement.style?.fontSize || 16
  const newSize = prompt('请输入字体大小 (12-48):', currentSize)

  if (newSize && !isNaN(newSize)) {
    const size = Math.max(12, Math.min(48, parseInt(newSize)))
    const styleUpdate = {
      style: {
        ...textElement.style,
        fontSize: size
      }
    }
    emit('update-element', textElement.id, styleUpdate)
  }
}

const handleModifyColor = (textElement, color) => {
  const newColor = color || textElement.style?.fill || '#333333'
  const styleUpdate = {
    style: {
      ...textElement.style,
      fill: newColor
    }
  }
  emit('update-element', textElement.id, styleUpdate)
}

const handleDeleteText = (textElement) => {
  if (confirm('确定要删除这个文字元素吗？')) {
    emit('delete-element', textElement.id)
    closeTextContextMenu()
  }
}

const handleTogglePin = (textElement) => {
  const newPinned = !textElement.pinned
  emit('update-element', textElement.id, {
    pinned: newPinned,
    draggable: !newPinned
  })
}

const closeTextEditDialog = () => {
  textEditDialog.value.visible = false
}

const handleSaveTextEdit = (updatedElement) => {
  emit('update-element', updatedElement.id, updatedElement)
}

// 矩形菜单事件处理
const closeRectContextMenu = () => {
  rectContextMenu.value = {
    visible: false,
    x: 0,
    y: 0,
    element: null
  }
}

const handleRectEditLabel = (rectElement) => {
  const newLabel = prompt('请输入矩形标签:', rectElement.label || '')
  if (newLabel !== null) {
    emit('update-element', rectElement.id, { label: newLabel })
  }
}

const handleRectModifyBorderColor = (rectElement, color) => {
  const newColor = color || rectElement.style?.stroke || '#007aff'
  emit('update-element', rectElement.id, {
    style: {
      ...rectElement.style,
      stroke: newColor
    }
  })
}

const handleRectModifyFillColor = (rectElement, color) => {
  const newColor = color || rectElement.style?.fill || 'rgba(0, 122, 255, 0.3)'
  emit('update-element', rectElement.id, {
    style: {
      ...rectElement.style,
      fill: newColor
    }
  })
}

const handleRectAdjustSize = (rectElement) => {
  const currentWidth = rectElement.shape?.width || 100
  const currentHeight = rectElement.shape?.height || 60

  const newWidth = prompt('请输入矩形宽度:', currentWidth)
  if (newWidth && !isNaN(newWidth)) {
    const newHeight = prompt('请输入矩形高度:', currentHeight)
    if (newHeight && !isNaN(newHeight)) {
      emit('update-element', rectElement.id, {
        shape: {
          ...rectElement.shape,
          width: parseInt(newWidth),
          height: parseInt(newHeight)
        }
      })
    }
  }
}

const handleRectTogglePin = (rectElement) => {
  emit('update-element', rectElement.id, {
    pinned: !rectElement.pinned
  })
}

const handleRectDelete = (rectElement) => {
  if (confirm('确定要删除这个矩形吗？')) {
    emit('delete-element', rectElement.id)
    closeRectContextMenu()
  }
}

// 圆形菜单事件处理
const closeCircleContextMenu = () => {
  circleContextMenu.value = {
    visible: false,
    x: 0,
    y: 0,
    element: null
  }
}

const handleCircleEditLabel = (circleElement) => {
  const newLabel = prompt('请输入圆形标签:', circleElement.label || '')
  if (newLabel !== null) {
    emit('update-element', circleElement.id, { label: newLabel })
  }
}

const handleCircleModifyBorderColor = (circleElement, color) => {
  const newColor = color || circleElement.style?.stroke || '#ff9500'
  emit('update-element', circleElement.id, {
    style: {
      ...circleElement.style,
      stroke: newColor
    }
  })
}

const handleCircleModifyFillColor = (circleElement, color) => {
  const newColor = color || circleElement.style?.fill || 'rgba(255, 149, 0, 0.3)'
  emit('update-element', circleElement.id, {
    style: {
      ...circleElement.style,
      fill: newColor
    }
  })
}

const handleCircleAdjustRadius = (circleElement) => {
  const currentRadius = circleElement.shape?.r || 30
  const newRadius = prompt('请输入圆形半径:', currentRadius)
  if (newRadius && !isNaN(newRadius)) {
    emit('update-element', circleElement.id, {
      shape: {
        ...circleElement.shape,
        r: parseInt(newRadius)
      }
    })
  }
}

const handleCircleTogglePin = (circleElement) => {
  emit('update-element', circleElement.id, {
    pinned: !circleElement.pinned
  })
}

const handleCircleDelete = (circleElement) => {
  if (confirm('确定要删除这个圆形吗？')) {
    emit('delete-element', circleElement.id)
    closeCircleContextMenu()
  }
}

// 隐藏所有菜单
const hideAllMenus = () => {
  closeTextContextMenu()
  closeRectContextMenu()
  closeCircleContextMenu()
  closeTextEditDialog()
}

// 暴露方法给父组件
defineExpose({
  showTextContextMenu,
  showRectContextMenu,
  showCircleContextMenu,
  hideAllMenus
})
</script>