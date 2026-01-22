<template>
  <div v-if="visible" class="dialog-overlay" @click="closeDialog">
    <div class="dialog-content" @click.stop>
      <div class="dialog-header">
        <h3 class="dialog-title">
          <span class="title-icon">📄</span>
          节点详情
        </h3>
        <button class="close-button" @click="closeDialog">×</button>
      </div>
      
      <div class="dialog-body">
        <div class="node-preview">
          <div class="node-icon">
            <img v-if="node?.symbol && node.symbol.includes('image:')" 
                 :src="getNodeIconUrl(node.symbol)" 
                 alt="节点图标" 
                 class="icon-image" />
            <span v-else class="icon-fallback">🔘</span>
          </div>
          <div class="node-basic-info">
            <h4 class="node-name">{{ node?.name || '未命名节点' }}</h4>
            <p class="node-id">ID: {{ node?.id }}</p>
          </div>
        </div>
        
        <div class="details-grid">
          <div class="detail-section">
            <h5 class="section-title">基本信息</h5>
            <div class="detail-list">
              <div class="detail-item">
                <span class="detail-label">节点名称</span>
                <span class="detail-value">{{ node?.name || '未命名' }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">节点ID</span>
                <span class="detail-value code">{{ node?.id }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">节点类型</span>
                <span class="detail-value">{{ getNodeType(node) }}</span>
              </div>
              <div class="detail-item" v-if="node?.typeIcon">
                <span class="detail-label">类型图标</span>
                <span class="detail-value">{{ node.typeIcon }} {{ node?.typeName || '未设置' }}</span>
              </div>
              <div class="detail-item" v-if="node?.description">
                <span class="detail-label">描述信息</span>
                <span class="detail-value">{{ node.description }}</span>
              </div>
              <div class="detail-item" v-if="node?.iconName">
                <span class="detail-label">图标名称</span>
                <span class="detail-value code">{{ node.iconName }}</span>
              </div>
            </div>
          </div>
          
          <div class="detail-section">
            <h5 class="section-title">位置信息</h5>
            <div class="detail-list">
              <div class="detail-item">
                <span class="detail-label">X 坐标</span>
                <span class="detail-value code">{{ node?.x || 0 }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">Y 坐标</span>
                <span class="detail-value code">{{ node?.y || 0 }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">是否可拖拽</span>
                <span class="detail-value">{{ node?.draggable !== false ? '是' : '否' }}</span>
              </div>
            </div>
          </div>
          
          <div class="detail-section">
            <h5 class="section-title">显示设置</h5>
            <div class="detail-list">
              <div class="detail-item">
                <span class="detail-label">图标大小</span>
                <span class="detail-value">{{ getSymbolSize(node) }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">标签显示</span>
                <span class="detail-value">{{ node?.label?.show !== false ? '显示' : '隐藏' }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">标签位置</span>
                <span class="detail-value">{{ node?.label?.position || 'bottom' }}</span>
              </div>
            </div>
          </div>
          
          <div class="detail-section">
            <h5 class="section-title">连接信息</h5>
            <div class="detail-list">
              <div class="detail-item">
                <span class="detail-label">连接数量</span>
                <span class="detail-value">{{ getConnectionCount(node) }} 个</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">入度</span>
                <span class="detail-value">{{ getInDegree(node) }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">出度</span>
                <span class="detail-value">{{ getOutDegree(node) }}</span>
              </div>
            </div>
          </div>
        </div>
        
        <div v-if="getConnectedNodes(node).length > 0" class="connected-nodes">
          <h5 class="section-title">连接的节点</h5>
          <div class="connected-list">
            <div v-for="connectedNode in getConnectedNodes(node)" :key="connectedNode.id" class="connected-item">
              <span class="connected-name">{{ connectedNode.name || connectedNode.id }}</span>
              <span class="connected-type">{{ getConnectionDirection(node, connectedNode) }}</span>
            </div>
          </div>
        </div>
      </div>
      
      <div class="dialog-footer">
        <button class="btn btn-secondary" @click="closeDialog">
          关闭
        </button>
        <button class="btn btn-primary" @click="editNode">
          <span>✏️</span>
          编辑节点
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  node: {
    type: Object,
    default: null
  },
  links: {
    type: Array,
    default: () => []
  },
  nodes: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['close', 'edit'])

const closeDialog = () => {
  emit('close')
}

const editNode = () => {
  emit('edit', props.node)
  closeDialog()
}

const getNodeIconUrl = (symbol) => {
  if (symbol && symbol.includes('image:')) {
    return symbol.replace('image://', '')
  }
  return ''
}

const getNodeType = (node) => {
  if (!node) return '未知'
  
  // 🔥 优先使用节点的 typeName 属性（用户设置的类型名称）
  if (node.typeName) {
    return node.typeName
  }
  
  // 其次使用 type 属性
  if (node.type) {
    return node.type
  }
  
  // 最后根据图标推断节点类型
  const symbol = node.symbol || ''
  if (symbol.includes('router')) return '路由器'
  if (symbol.includes('switch')) return '交换机'
  if (symbol.includes('server')) return '服务器'
  if (symbol.includes('pc') || symbol.includes('computer')) return '计算机'
  if (symbol.includes('firewall')) return '防火墙'
  
  return '设备'
}

const getSymbolSize = (node) => {
  if (!node?.symbolSize) return '默认'
  if (Array.isArray(node.symbolSize)) {
    return `${node.symbolSize[0]} × ${node.symbolSize[1]}`
  }
  return node.symbolSize.toString()
}

const getConnectionCount = (node) => {
  if (!node || !props.links) return 0
  return props.links.filter(link => 
    link.source === node.id || link.target === node.id
  ).length
}

const getInDegree = (node) => {
  if (!node || !props.links) return 0
  return props.links.filter(link => link.target === node.id).length
}

const getOutDegree = (node) => {
  if (!node || !props.links) return 0
  return props.links.filter(link => link.source === node.id).length
}

const getConnectedNodes = (node) => {
  if (!node || !props.links || !props.nodes) return []
  
  const connectedNodeIds = new Set()
  props.links.forEach(link => {
    if (link.source === node.id) {
      connectedNodeIds.add(link.target)
    }
    if (link.target === node.id) {
      connectedNodeIds.add(link.source)
    }
  })
  
  return props.nodes.filter(n => connectedNodeIds.has(n.id))
}

const getConnectionDirection = (currentNode, connectedNode) => {
  if (!currentNode || !connectedNode || !props.links) return ''
  
  const hasOutgoing = props.links.some(link => 
    link.source === currentNode.id && link.target === connectedNode.id
  )
  const hasIncoming = props.links.some(link => 
    link.source === connectedNode.id && link.target === currentNode.id
  )
  
  if (hasOutgoing && hasIncoming) return '双向连接'
  if (hasOutgoing) return '出站连接'
  if (hasIncoming) return '入站连接'
  return '未知连接'
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
  max-width: 720px;
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
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
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
  max-height: 60vh;
  overflow-y: auto;
}

.node-preview {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: linear-gradient(135deg, #f8f9fa, #e9ecef);
  border-radius: 12px;
  margin-bottom: 24px;
}

.node-icon {
  width: 64px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.icon-image {
  width: 48px;
  height: 48px;
  object-fit: contain;
}

.icon-fallback {
  font-size: 32px;
}

.node-basic-info h4 {
  margin: 0 0 4px;
  font-size: 20px;
  font-weight: 600;
  color: #2d3748;
}

.node-id {
  margin: 0;
  color: #4a5568;
  font-family: 'SF Mono', Monaco, Consolas, monospace;
  font-size: 14px;
}

.details-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.detail-section {
  background: white;
  border: 1px solid #e1e4e8;
  border-radius: 8px;
  padding: 16px;
}

.section-title {
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 600;
  color: #4a5568;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.detail-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f1f3f4;
}

.detail-item:last-child {
  border-bottom: none;
}

.detail-label {
  font-weight: 500;
  color: #4a5568;
  font-size: 14px;
}

.detail-value {
  color: #2d3748;
  font-size: 14px;
}

.detail-value.code {
  font-family: 'SF Mono', Monaco, Consolas, monospace;
  background: #f8f9fa;
  padding: 2px 6px;
  border-radius: 4px;
  border: 1px solid #e1e4e8;
}

.connected-nodes {
  margin-top: 20px;
}

.connected-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
}

.connected-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e1e4e8;
}

.connected-name {
  font-weight: 500;
  color: #2d3748;
}

.connected-type {
  font-size: 12px;
  color: #4a5568;
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
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  color: white;
}

.btn-primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(79, 172, 254, 0.3);
}
</style>