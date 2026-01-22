<template>
  <div v-if="visible" class="dialog-overlay" @click="closeDialog">
    <div class="dialog-content" @click.stop>
      <div class="dialog-header">
        <h3 class="dialog-title">
          <span class="title-icon">🔗</span>
          创建节点连接
        </h3>
        <button class="close-button" @click="closeDialog">×</button>
      </div>
      
      <div class="dialog-body">
        <div class="source-node">
          <h5 class="section-title">源节点</h5>
          <div class="node-card source">
            <span class="node-icon">🔘</span>
            <div class="node-info">
              <h4>{{ node?.name || '未命名节点' }}</h4>
              <p>ID: {{ node?.id }}</p>
            </div>
          </div>
        </div>
        
        <div class="target-selection">
          <h5 class="section-title">选择目标节点</h5>
          <div class="search-box">
            <input 
              v-model="searchQuery"
              type="text" 
              placeholder="搜索节点..."
              class="search-input"
            />
          </div>
          
          <div class="nodes-list">
            <div 
              v-for="targetNode in filteredNodes" 
              :key="targetNode.id"
              class="node-item"
              :class="{ selected: selectedTargetId === targetNode.id }"
              @click="selectTarget(targetNode.id)"
            >
              <span class="node-icon">🔘</span>
              <div class="node-details">
                <h6 class="node-name">{{ targetNode.name || '未命名节点' }}</h6>
                <p class="node-id">{{ targetNode.id }}</p>
              </div>
              <div class="connection-status">
                <span v-if="isAlreadyConnected(targetNode.id)" class="status-badge connected">
                  已连接
                </span>
                <span v-else class="status-badge available">
                  可连接
                </span>
              </div>
            </div>
          </div>
        </div>
        
        <div v-if="selectedTargetId" class="connection-options">
          <h5 class="section-title">连接设置</h5>
          <div class="options-grid">
            <div class="option-group">
              <label for="connectionType">连接类型</label>
              <select id="connectionType" v-model="connectionType" class="form-select">
                <option value="bidirectional">双向连接</option>
                <option value="unidirectional">单向连接</option>
              </select>
            </div>
            
            <div class="option-group">
              <label for="connectionLabel">连接标签</label>
              <input 
                id="connectionLabel"
                v-model="connectionLabel" 
                type="text" 
                placeholder="可选：输入连接描述"
                class="form-input"
              />
            </div>
            
            <div class="option-group">
              <label for="lineStyle">线条样式</label>
              <select id="lineStyle" v-model="lineStyle" class="form-select">
                <option value="solid">实线</option>
                <option value="dashed">虚线</option>
                <option value="dotted">点线</option>
              </select>
            </div>
            
            <div class="option-group">
              <label for="lineColor">线条颜色</label>
              <div class="color-picker">
                <select v-model="lineColor" class="form-select">
                  <option value="#606266">默认灰色</option>
                  <option value="#409EFF">蓝色</option>
                  <option value="#67C23A">绿色</option>
                  <option value="#E6A23C">橙色</option>
                  <option value="#F56C6C">红色</option>
                  <option value="#909399">灰色</option>
                </select>
                <div class="color-preview" :style="{ backgroundColor: lineColor }"></div>
              </div>
            </div>
          </div>
        </div>
        
        <div v-if="selectedTargetId" class="connection-preview">
          <h5 class="section-title">连接预览</h5>
          <div class="preview-diagram">
            <div class="preview-node source">
              <span class="node-icon">🔘</span>
              <span class="node-label">{{ node?.name || 'Source' }}</span>
            </div>
            
            <div class="connection-line">
              <div 
                class="line" 
                :class="lineStyle"
                :style="{ backgroundColor: lineColor }"
              ></div>
              <div v-if="connectionType === 'bidirectional'" class="arrow left">←</div>
              <div class="arrow right">→</div>
              <div v-if="connectionLabel" class="line-label">{{ connectionLabel }}</div>
            </div>
            
            <div class="preview-node target">
              <span class="node-icon">🔘</span>
              <span class="node-label">{{ getTargetNodeName() }}</span>
            </div>
          </div>
        </div>
      </div>
      
      <div class="dialog-footer">
        <button class="btn btn-secondary" @click="closeDialog">
          取消
        </button>
        <button 
          class="btn btn-primary" 
          @click="confirmConnection"
          :disabled="!selectedTargetId"
        >
          <span>🔗</span>
          创建连接
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed } from 'vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  node: {
    type: Object,
    default: null
  },
  nodes: {
    type: Array,
    default: () => []
  },
  links: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['confirm', 'close'])

const searchQuery = ref('')
const selectedTargetId = ref('')
const connectionType = ref('unidirectional')
const connectionLabel = ref('')
const lineStyle = ref('solid')
const lineColor = ref('#606266')

// 监听对话框显示状态
watch(() => props.visible, (newVisible) => {
  if (newVisible) {
    resetForm()
  }
})

const resetForm = () => {
  searchQuery.value = ''
  selectedTargetId.value = ''
  connectionType.value = 'unidirectional'
  connectionLabel.value = ''
  lineStyle.value = 'solid'
  lineColor.value = '#606266'
}

// 过滤可用的目标节点
const filteredNodes = computed(() => {
  if (!props.nodes || !props.node) return []
  
  return props.nodes
    .filter(n => n.id !== props.node.id) // 排除自己
    .filter(n => {
      if (!searchQuery.value) return true
      const query = searchQuery.value.toLowerCase()
      return (n.name || '').toLowerCase().includes(query) || 
             n.id.toLowerCase().includes(query)
    })
})

// 检查是否已经连接
const isAlreadyConnected = (targetId) => {
  if (!props.links || !props.node) return false
  
  return props.links.some(link => 
    (link.source === props.node.id && link.target === targetId) ||
    (link.target === props.node.id && link.source === targetId)
  )
}

const selectTarget = (targetId) => {
  selectedTargetId.value = targetId
}

const getTargetNodeName = () => {
  if (!selectedTargetId.value) return ''
  const targetNode = props.nodes.find(n => n.id === selectedTargetId.value)
  return targetNode?.name || targetNode?.id || 'Target'
}

const closeDialog = () => {
  emit('close')
}

const confirmConnection = () => {
  if (!selectedTargetId.value) return
  
  const connections = []
  
  // 创建主连接
  connections.push({
    source: props.node.id,
    target: selectedTargetId.value,
    label: connectionLabel.value,
    lineStyle: {
      color: lineColor.value,
      type: lineStyle.value,
      width: 2
    }
  })
  
  // 如果是双向连接，创建反向连接
  if (connectionType.value === 'bidirectional') {
    connections.push({
      source: selectedTargetId.value,
      target: props.node.id,
      label: connectionLabel.value,
      lineStyle: {
        color: lineColor.value,
        type: lineStyle.value,
        width: 2
      }
    })
  }
  
  emit('confirm', {
    connections,
    sourceId: props.node.id,
    targetId: selectedTargetId.value,
    type: connectionType.value,
    label: connectionLabel.value
  })
  
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
  max-width: 900px;
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
  max-height: 65vh;
  overflow-y: auto;
}

.section-title {
  margin: 0 0 16px;
  font-size: 16px;
  font-weight: 600;
  color: #2d3748;
}

.source-node {
  margin-bottom: 24px;
}

.node-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
  border: 2px solid #e1e4e8;
}

.node-card.source {
  border-color: #667eea;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
}

.node-icon {
  font-size: 24px;
}

.node-info h4 {
  margin: 0 0 4px;
  font-size: 16px;
  font-weight: 600;
}

.node-info p {
  margin: 0;
  font-size: 14px;
  opacity: 0.8;
  font-family: 'SF Mono', Monaco, Consolas, monospace;
}

.search-box {
  margin-bottom: 16px;
}

.search-input {
  width: 100%;
  padding: 12px 16px;
  border: 2px solid #e1e4e8;
  border-radius: 8px;
  font-size: 14px;
  transition: border-color 0.2s;
}

.search-input:focus {
  outline: none;
  border-color: #667eea;
}

.nodes-list {
  max-height: 300px;
  overflow-y: auto;
  border: 1px solid #e1e4e8;
  border-radius: 8px;
  margin-bottom: 24px;
}

.node-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid #e1e4e8;
  cursor: pointer;
  transition: all 0.2s;
}

.node-item:last-child {
  border-bottom: none;
}

.node-item:hover {
  background: #f8f9fa;
}

.node-item.selected {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
}

.node-details {
  flex: 1;
}

.node-name {
  margin: 0 0 4px;
  font-size: 14px;
  font-weight: 600;
}

.node-id {
  margin: 0;
  font-size: 12px;
  opacity: 0.7;
  font-family: 'SF Mono', Monaco, Consolas, monospace;
}

.status-badge {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.status-badge.connected {
  background: #fef0e6;
  color: #d46b08;
}

.status-badge.available {
  background: #e6f7ff;
  color: #1890ff;
}

.node-item.selected .status-badge {
  background: rgba(255, 255, 255, 0.2);
  color: white;
}

.options-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.option-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.option-group label {
  font-weight: 500;
  color: #2d3748;
  font-size: 14px;
}

.form-input,
.form-select {
  padding: 10px 12px;
  border: 2px solid #e1e4e8;
  border-radius: 6px;
  font-size: 14px;
  transition: border-color 0.2s;
}

.form-input:focus,
.form-select:focus {
  outline: none;
  border-color: #667eea;
}

.color-picker {
  display: flex;
  align-items: center;
  gap: 8px;
}

.color-preview {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  border: 1px solid #e1e4e8;
}

.connection-preview {
  border-top: 1px solid #e1e4e8;
  padding-top: 20px;
}

.preview-diagram {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
  padding: 30px 20px;
  background: #f8f9fa;
  border-radius: 8px;
}

.preview-node {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  min-width: 120px;
}

.preview-node.source {
  border: 2px solid #667eea;
}

.preview-node.target {
  border: 2px solid #28a745;
}

.node-label {
  font-size: 12px;
  font-weight: 500;
  text-align: center;
  color: #2d3748;
}

.connection-line {
  display: flex;
  align-items: center;
  position: relative;
  flex: 1;
  max-width: 200px;
}

.line {
  height: 3px;
  flex: 1;
  position: relative;
}

.line.solid {
  background: currentColor;
}

.line.dashed {
  background: repeating-linear-gradient(
    to right,
    currentColor,
    currentColor 10px,
    transparent 10px,
    transparent 20px
  );
}

.line.dotted {
  background: repeating-linear-gradient(
    to right,
    currentColor,
    currentColor 3px,
    transparent 3px,
    transparent 8px
  );
}

.arrow {
  position: absolute;
  font-size: 16px;
  color: inherit;
  background: #f8f9fa;
  padding: 0 4px;
}

.arrow.left {
  left: 0;
  transform: translateX(-50%);
}

.arrow.right {
  right: 0;
  transform: translateX(50%);
}

.line-label {
  position: absolute;
  top: -24px;
  left: 50%;
  transform: translateX(-50%);
  background: #f8f9fa;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  color: #4a5568;
  white-space: nowrap;
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