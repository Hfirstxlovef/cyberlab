<template>
  <div v-if="visible" class="dialog-overlay" @click="closeDialog">
    <div class="dialog-content" @click.stop>
      <div class="dialog-header">
        <h3 class="dialog-title">
          <span class="title-icon">🏷️</span>
          设置节点类型
        </h3>
        <button class="close-button" @click="closeDialog">×</button>
      </div>
      
      <div class="dialog-body">
        <div class="current-node">
          <div class="node-preview">
            <span class="node-icon">🔘</span>
            <div class="node-info">
              <h4>{{ node?.name || '未命名节点' }}</h4>
              <p>ID: {{ node?.id }}</p>
            </div>
          </div>
        </div>
        
        <div class="type-selection">
          <h5 class="section-title">选择设备类型</h5>
          <div class="type-grid">
            <div 
              v-for="deviceType in deviceTypes" 
              :key="deviceType.id"
              class="type-card"
              :class="{ active: selectedType === deviceType.id }"
              @click="selectType(deviceType.id)"
            >
              <div class="type-icon">{{ deviceType.icon }}</div>
              <div class="type-info">
                <h6 class="type-name">{{ deviceType.name }}</h6>
                <p class="type-description">{{ deviceType.description }}</p>
              </div>
            </div>
          </div>
        </div>
        
        <div v-if="selectedType" class="type-properties">
          <h5 class="section-title">类型属性</h5>
          <div class="property-list">
            <div class="property-item">
              <label for="nodeType">设备类型</label>
              <input 
                id="nodeType"
                v-model="selectedType" 
                type="text" 
                class="form-input"
                readonly 
              />
            </div>
            <div class="property-item">
              <label for="nodeDescription">描述信息</label>
              <textarea 
                id="nodeDescription"
                v-model="nodeDescription" 
                class="form-textarea"
                placeholder="请输入设备描述信息"
                rows="3"
              ></textarea>
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
          @click="confirmSetType"
          :disabled="!selectedType"
        >
          <span>✅</span>
          确认设置
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

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

const selectedType = ref('')
const nodeDescription = ref('')

// 设备类型定义
const deviceTypes = [
  {
    id: 'router',
    name: '路由器',
    icon: '🔀',
    description: '网络路由设备，负责数据包转发'
  },
  {
    id: 'switch',
    name: '交换机',
    icon: '🔗',
    description: '网络交换设备，连接多个网络段'
  },
  {
    id: 'server',
    name: '服务器',
    icon: '🖥️',
    description: '提供网络服务的计算机系统'
  },
  {
    id: 'pc',
    name: '个人电脑',
    icon: '💻',
    description: '终端用户计算机设备'
  },
  {
    id: 'firewall',
    name: '防火墙',
    icon: '🛡️',
    description: '网络安全防护设备'
  },
  {
    id: 'hub',
    name: '集线器',
    icon: '🕸️',
    description: '网络集线设备，共享带宽'
  },
  {
    id: 'printer',
    name: '打印机',
    icon: '🖨️',
    description: '网络打印设备'
  },
  {
    id: 'storage',
    name: '存储设备',
    icon: '💾',
    description: '网络存储系统'
  },
  {
    id: 'mobile',
    name: '移动设备',
    icon: '📱',
    description: '移动终端设备'
  },
  {
    id: 'iot',
    name: '物联网设备',
    icon: '🌐',
    description: '智能物联网终端设备'
  },
  {
    id: 'camera',
    name: '网络摄像头',
    icon: '📹',
    description: '网络监控摄像设备'
  },
  {
    id: 'other',
    name: '其他设备',
    icon: '❓',
    description: '未分类的网络设备'
  }
]

// 监听对话框显示状态
watch(() => props.visible, (newVisible) => {
  if (newVisible && props.node) {
    // 🔥 修复：优先使用节点已设置的type属性，其次推断类型
    selectedType.value = props.node.type || inferNodeType(props.node)
    nodeDescription.value = props.node.description || ''
  }
})

const inferNodeType = (node) => {
  if (!node) return 'other'
  
  const symbol = node.symbol || ''
  const name = (node.name || '').toLowerCase()
  
  if (symbol.includes('router') || name.includes('router')) return 'router'
  if (symbol.includes('switch') || name.includes('switch')) return 'switch'
  if (symbol.includes('server') || name.includes('server')) return 'server'
  if (symbol.includes('pc') || name.includes('computer')) return 'pc'
  if (symbol.includes('firewall') || name.includes('firewall')) return 'firewall'
  
  return 'other'
}

const selectType = (typeId) => {
  selectedType.value = typeId
}

const closeDialog = () => {
  emit('close')
}

const confirmSetType = () => {
  if (selectedType.value) {
    const selectedTypeInfo = deviceTypes.find(t => t.id === selectedType.value)
    const confirmData = {
      nodeId: props.node.id,
      type: selectedType.value,
      typeName: selectedTypeInfo?.name,
      typeIcon: selectedTypeInfo?.icon,
      description: nodeDescription.value.trim()
    }
    
    emit('confirm', confirmData)
  } else {
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
  max-width: 800px;
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
  background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%);
  color: #2d3748;
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
  color: #2d3748;
  font-size: 24px;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.close-button:hover {
  background: rgba(45, 55, 72, 0.1);
}

.dialog-body {
  padding: 24px;
  max-height: 60vh;
  overflow-y: auto;
}

.current-node {
  margin-bottom: 24px;
}

.node-preview {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e1e4e8;
}

.node-icon {
  font-size: 24px;
}

.node-info h4 {
  margin: 0 0 4px;
  font-size: 16px;
  font-weight: 600;
  color: #2d3748;
}

.node-info p {
  margin: 0;
  font-size: 14px;
  color: #4a5568;
  font-family: 'SF Mono', Monaco, Consolas, monospace;
}

.section-title {
  margin: 0 0 16px;
  font-size: 16px;
  font-weight: 600;
  color: #2d3748;
}

.type-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
  margin-bottom: 24px;
}

.type-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border: 2px solid #e1e4e8;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  background: white;
}

.type-card:hover {
  border-color: #a8edea;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.type-card.active {
  border-color: #667eea;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.type-icon {
  font-size: 24px;
  flex-shrink: 0;
}

.type-info {
  flex: 1;
  min-width: 0;
}

.type-name {
  margin: 0 0 4px;
  font-size: 14px;
  font-weight: 600;
}

.type-description {
  margin: 0;
  font-size: 12px;
  opacity: 0.8;
  line-height: 1.4;
}

.type-properties {
  border-top: 1px solid #e1e4e8;
  padding-top: 24px;
}

.property-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.property-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.property-item label {
  font-weight: 500;
  color: #2d3748;
  font-size: 14px;
}

.form-input,
.form-textarea {
  padding: 12px 16px;
  border: 2px solid #e1e4e8;
  border-radius: 8px;
  font-size: 14px;
  transition: border-color 0.2s;
  font-family: inherit;
}

.form-input:focus,
.form-textarea:focus {
  outline: none;
  border-color: #667eea;
}

.form-input[readonly] {
  background: #f8f9fa;
  color: #4a5568;
}

.form-textarea {
  resize: vertical;
  min-height: 80px;
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