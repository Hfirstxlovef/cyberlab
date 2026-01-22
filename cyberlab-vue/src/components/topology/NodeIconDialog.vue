<template>
  <div v-if="visible" class="dialog-overlay" @click="closeDialog">
    <div class="dialog-content" @click.stop>
      <div class="dialog-header">
        <h3 class="dialog-title">
          <span class="title-icon">🎨</span>
          修改节点图标
        </h3>
        <button class="close-button" @click="closeDialog">×</button>
      </div>
      
      <div class="dialog-body">
        <div class="current-preview">
          <div class="preview-card">
            <div class="preview-icon">
              <img v-if="currentIcon && currentIcon.includes('image:')" 
                   :src="getIconUrl(currentIcon)" 
                   alt="当前图标" 
                   class="icon-image" />
              <span v-else class="icon-fallback">🔘</span>
            </div>
            <div class="preview-info">
              <h4>{{ node?.name || '未命名节点' }}</h4>
              <p>当前图标</p>
            </div>
          </div>
        </div>
        
        <div class="icon-categories">
          <div class="category-tabs">
            <button 
              v-for="category in iconCategories" 
              :key="category.id"
              class="tab-button"
              :class="{ active: activeCategory === category.id }"
              @click="activeCategory = category.id"
            >
              <span class="tab-icon">{{ category.icon }}</span>
              <span class="tab-text">{{ category.name }}</span>
            </button>
          </div>
          
          <div class="icon-grid">
            <div 
              v-for="icon in getCurrentCategoryIcons()" 
              :key="icon.id"
              class="icon-item"
              :class="{ active: selectedIcon === icon.path }"
              @click="selectIcon(icon.path)"
              :title="icon.name"
            >
              <img 
                :src="icon.path" 
                :alt="icon.name"
                class="icon-preview"
                @error="handleImageError"
              />
              <span class="icon-name">{{ icon.name }}</span>
            </div>
          </div>
        </div>
        
        <div v-if="selectedIcon" class="selection-preview">
          <h5 class="section-title">预览效果</h5>
          <div class="preview-comparison">
            <div class="comparison-item">
              <div class="comparison-icon">
                <img v-if="currentIcon && currentIcon.includes('image:')" 
                     :src="getIconUrl(currentIcon)" 
                     alt="原图标" />
                <span v-else class="icon-fallback">🔘</span>
              </div>
              <span class="comparison-label">原图标</span>
            </div>
            <div class="comparison-arrow">→</div>
            <div class="comparison-item">
              <div class="comparison-icon">
                <img :src="selectedIcon" alt="新图标" />
              </div>
              <span class="comparison-label">新图标</span>
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
          @click="confirmChangeIcon"
          :disabled="!selectedIcon || selectedIcon === currentIcon"
        >
          <span>✅</span>
          确认更换
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

const activeCategory = ref('network')
const selectedIcon = ref('')
const currentIcon = ref('')

// 图标分类定义
const iconCategories = [
  { id: 'network', name: '网络设备', icon: '🌐' },
  { id: 'server', name: '服务器', icon: '🖥️' },
  { id: 'security', name: '安全设备', icon: '🛡️' },
  { id: 'terminal', name: '终端设备', icon: '💻' },
  { id: 'mobile', name: '移动设备', icon: '📱' }
]

// 图标库定义（使用实际存在的PNG图标文件）
const iconLibrary = {
  network: [
    { id: 'router', name: '路由器', path: 'icons/router.png' },
    { id: 'ethernet_switch', name: '以太网交换机', path: 'icons/ethernet_switch.png' },
    { id: 'fiber_switch', name: '光纤交换机', path: 'icons/fiber_switch.png' },
    { id: 'main_switch', name: '主交换机', path: 'icons/main_switch.png' }
  ],
  server: [
    { id: 'PcServer', name: 'PC服务器', path: 'icons/PcServer.png' },
    { id: 'webserver', name: 'Web服务器', path: 'icons/webserver.png' },
    { id: 'mail_server', name: '邮件服务器', path: 'icons/mail_server.png' },
    { id: 'storage_server', name: '存储服务器', path: 'icons/storage_server.png' },
    { id: 'database', name: '数据库服务器', path: 'icons/database.png' }
  ],
  security: [
    { id: 'firewall', name: '防火墙', path: 'icons/firewall.png' }
  ],
  terminal: [
    { id: 'pc', name: '个人电脑', path: 'icons/pc.png' },
    { id: 'laptop', name: '笔记本电脑', path: 'icons/laptop.png' }
  ],
  mobile: [
    { id: 'DNS', name: 'DNS服务器', path: 'icons/DNS.png' }
  ]
}

// 监听对话框显示状态
watch(() => props.visible, (newVisible) => {
  if (newVisible && props.node) {
    currentIcon.value = props.node.symbol || ''
    selectedIcon.value = ''
  }
})

const getCurrentCategoryIcons = () => {
  return iconLibrary[activeCategory.value] || []
}

const getIconUrl = (symbol) => {
  if (symbol && symbol.includes('image:')) {
    // 🔥 修复：同时处理两个斜杠和三个斜杠的情况
    return symbol.replace('image:///', '').replace('image://', '')
  }
  return symbol
}

const selectIcon = (iconPath) => {
  selectedIcon.value = iconPath
}

const handleImageError = (event) => {
  // 如果图标加载失败，显示备用图标
  event.target.style.display = 'none'
  event.target.nextElementSibling.style.display = 'flex'
}

const closeDialog = () => {
  emit('close')
}

const confirmChangeIcon = () => {
  if (selectedIcon.value) {
    // 🔥 修复路径格式：确保生成正确的image://格式
    const iconPath = selectedIcon.value.startsWith('/') 
      ? selectedIcon.value.substring(1)  // 移除开头的斜杠
      : selectedIcon.value
    
    const newIconUrl = `image://${iconPath}`
    
    emit('confirm', {
      nodeId: props.node.id,
      newIcon: newIconUrl
    })
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
  background: linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%);
  color: #744210;
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
  color: #744210;
  font-size: 24px;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.close-button:hover {
  background: rgba(116, 66, 16, 0.1);
}

.dialog-body {
  padding: 24px;
  max-height: 65vh;
  overflow-y: auto;
}

.current-preview {
  margin-bottom: 24px;
}

.preview-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 12px;
  border: 1px solid #e1e4e8;
}

.preview-icon {
  width: 64px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.icon-image {
  width: 48px;
  height: 48px;
  object-fit: contain;
}

.icon-fallback {
  font-size: 32px;
}

.preview-info h4 {
  margin: 0 0 4px;
  font-size: 18px;
  font-weight: 600;
  color: #2d3748;
}

.preview-info p {
  margin: 0;
  color: #4a5568;
  font-size: 14px;
}

.category-tabs {
  display: flex;
  gap: 4px;
  margin-bottom: 20px;
  background: #f1f3f4;
  border-radius: 8px;
  padding: 4px;
}

.tab-button {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 12px 16px;
  background: none;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 14px;
  font-weight: 500;
  color: #4a5568;
}

.tab-button:hover {
  background: rgba(255, 255, 255, 0.5);
}

.tab-button.active {
  background: white;
  color: #2d3748;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.tab-icon {
  font-size: 16px;
}

.icon-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.icon-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 12px;
  border: 2px solid #e1e4e8;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  background: white;
}

.icon-item:hover {
  border-color: #ffecd2;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.icon-item.active {
  border-color: #fcb69f;
  background: linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%);
}

.icon-preview {
  width: 48px;
  height: 48px;
  object-fit: contain;
}

.icon-name {
  font-size: 12px;
  text-align: center;
  color: #4a5568;
  font-weight: 500;
}

.icon-item.active .icon-name {
  color: #744210;
}

.section-title {
  margin: 0 0 16px;
  font-size: 16px;
  font-weight: 600;
  color: #2d3748;
}

.selection-preview {
  border-top: 1px solid #e1e4e8;
  padding-top: 20px;
}

.preview-comparison {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 32px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
}

.comparison-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.comparison-icon {
  width: 64px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: white;
  border-radius: 8px;
  border: 2px solid #e1e4e8;
}

.comparison-icon img {
  width: 48px;
  height: 48px;
  object-fit: contain;
}

.comparison-label {
  font-size: 12px;
  font-weight: 500;
  color: #4a5568;
}

.comparison-arrow {
  font-size: 24px;
  color: #4a5568;
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
  background: linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%);
  color: #744210;
}

.btn-primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(252, 182, 159, 0.3);
}

.btn-primary:disabled {
  background: #a0aec0;
  color: #718096;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}
</style>