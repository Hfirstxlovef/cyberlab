<template>
  <div v-if="visible" class="dialog-overlay" @click="closeDialog">
    <div class="dialog-content" @click.stop>
      <div class="dialog-header">
        <h3 class="dialog-title">
          <span class="title-icon">✏️</span>
          重命名节点
        </h3>
        <button class="close-button" @click="closeDialog">×</button>
      </div>
      
      <div class="dialog-body">
        <div class="form-group">
          <label for="nodeName">节点名称</label>
          <input
            id="nodeName"
            ref="nameInput"
            v-model="nodeName"
            type="text"
            class="form-input"
            placeholder="请输入节点名称"
            @keyup.enter="confirmRename"
            @keyup.escape="closeDialog"
          />
        </div>
        
        <div class="node-info">
          <div class="info-item">
            <span class="info-label">节点ID:</span>
            <span class="info-value">{{ node?.id }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">当前名称:</span>
            <span class="info-value">{{ node?.name || '未命名' }}</span>
          </div>
        </div>
      </div>
      
      <div class="dialog-footer">
        <button class="btn btn-secondary" @click="closeDialog">
          取消
        </button>
        <button 
          class="btn btn-primary" 
          @click="confirmRename"
          :disabled="!nodeName.trim() || nodeName.trim() === (node?.name || '')"
        >
          确认重命名
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  node: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['confirm', 'close'])

const nodeName = ref('')
const nameInput = ref(null)

// 监听对话框显示状态
watch(() => props.visible, (newVisible) => {
  if (newVisible && props.node) {
    nodeName.value = props.node.name || ''
    nextTick(() => {
      nameInput.value?.focus()
      nameInput.value?.select()
    })
  }
})

const closeDialog = () => {
  emit('close')
}

const confirmRename = () => {
  const newName = nodeName.value.trim()
  if (newName && newName !== (props.node?.name || '')) {
    emit('confirm', {
      nodeId: props.node.id,
      newName: newName
    })
  }
  closeDialog()
}
</script>

<style scoped>
.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10000;
  backdrop-filter: blur(4px);
}

.dialog-content {
  background: white;
  border-radius: 12px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
  width: 90%;
  max-width: 480px;
  max-height: 90vh;
  overflow: hidden;
  animation: dialogSlideIn 0.3s ease-out;
}

@keyframes dialogSlideIn {
  from {
    opacity: 0;
    transform: scale(0.9) translateY(-20px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid #e1e4e8;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.dialog-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-icon {
  font-size: 20px;
}

.close-button {
  background: none;
  border: none;
  color: white;
  font-size: 24px;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.close-button:hover {
  background: rgba(255, 255, 255, 0.2);
}

.dialog-body {
  padding: 24px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #2d3748;
}

.form-input {
  width: 100%;
  padding: 12px 16px;
  border: 2px solid #e1e4e8;
  border-radius: 8px;
  font-size: 16px;
  transition: all 0.2s;
  box-sizing: border-box;
}

.form-input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.node-info {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 16px;
  margin-top: 16px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.info-item:last-child {
  margin-bottom: 0;
}

.info-label {
  font-weight: 500;
  color: #4a5568;
}

.info-value {
  color: #2d3748;
  font-family: 'SF Mono', Monaco, Consolas, monospace;
  background: white;
  padding: 4px 8px;
  border-radius: 4px;
  border: 1px solid #e1e4e8;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 20px 24px;
  background: #f8f9fa;
  border-top: 1px solid #e1e4e8;
}

.btn {
  padding: 10px 20px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 6px;
}

.btn-secondary {
  background: #e1e4e8;
  color: #4a5568;
}

.btn-secondary:hover {
  background: #cbd5e0;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.btn-primary:disabled {
  background: #a0aec0;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}
</style>