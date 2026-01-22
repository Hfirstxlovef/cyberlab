<template>
  <div class="drawing-toolbar" style="visibility: visible !important; display: flex !important;">
    <button @click.stop.prevent="setCurrentTool('text')" :class="{ active: currentTool === 'text' }" type="button">
      📝 文字
    </button>
    <button @click.stop.prevent="setCurrentTool('rect')" :class="{ active: currentTool === 'rect' }" type="button">
      ⬜ 矩形
    </button>
    <button @click.stop.prevent="setCurrentTool('circle')" :class="{ active: currentTool === 'circle' }" type="button">
      ⭕ 圆形
    </button>
    <button @click.stop.prevent="setCurrentTool('trapezoid')" :class="{ active: currentTool === 'trapezoid' }" type="button">
      🔷 梯形
    </button>
    <button @click.stop.prevent="setCurrentTool(null)" v-if="currentTool" type="button" class="cancel-btn">
      ❌ 取消
    </button>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'

defineProps({
  currentTool: String
})

const emit = defineEmits(['tool-changed'])

onMounted(() => {
  // 工具栏已挂载
})

const setCurrentTool = (tool) => {
  try {
    // 按钮点击，工具变更
    
    // 安全地发出事件
    if (emit) {
      emit('tool-changed', tool)
    } else {
      // emit函数不可用
      return
    }
    
    // 添加视觉反馈
    try {
      if (tool) {
        document.body.style.cursor = tool === 'text' ? 'text' : 'crosshair'
      } else {
        document.body.style.cursor = 'default'
      }
    } catch {
      // 设置鼠标样式时出错
    }
  } catch {
    // setCurrentTool出错
  }
}

</script>

<style scoped>
.drawing-toolbar {
  position: absolute !important;
  top: 20px !important;
  right: 20px !important;
  z-index: 99999 !important;
  display: flex !important;
  gap: 8px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
  border: 1px solid #e1e4e8;
  align-items: center;
  max-width: 400px;
  flex-wrap: wrap;
  pointer-events: auto !important;
  visibility: visible !important;
}

.drawing-toolbar button {
  padding: 10px 16px;
  border: 1px solid #d0d7de;
  border-radius: 8px;
  background: #f6f8fa;
  color: #24292f;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 14px;
  font-weight: 500;
  min-width: 80px;
  position: relative;
  overflow: hidden;
  opacity: 1;
  pointer-events: auto;
}

.drawing-toolbar button:hover {
  background: #f3f4f6;
  border-color: #afb8c1;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.drawing-toolbar button.active {
  background: linear-gradient(135deg, #0969da 0%, #0550ae 100%);
  color: white;
  border-color: #0969da;
  box-shadow: 0 4px 15px rgba(9, 105, 218, 0.4);
  transform: scale(1.02);
}

.drawing-toolbar button.active:hover {
  background: linear-gradient(135deg, #0550ae 0%, #033d8b 100%);
  box-shadow: 0 6px 20px rgba(9, 105, 218, 0.5);
}

.drawing-toolbar button:active {
  transform: scale(0.98);
  transition: transform 0.1s ease;
}

.drawing-toolbar button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  pointer-events: none;
}

.drawing-toolbar button.cancel-btn {
  background: linear-gradient(135deg, #da3633 0%, #cf222e 100%);
  color: white;
  border-color: #da3633;
}

.drawing-toolbar button.cancel-btn:hover {
  background: linear-gradient(135deg, #cf222e 0%, #a40e26 100%);
  box-shadow: 0 4px 15px rgba(218, 54, 51, 0.4);
}

</style>