<template>
  <div class="icon-panel">
    <div
      v-for="item in deviceTypes"
      :key="item.type"
      class="icon-item"
      draggable
      @dragstart="(event) => onDragStart(item, event)"
    >
      <img
        :src="iconMap[item.iconName]"
        class="device-icon"
        @error="onIconLoadError"
        @load="onIconLoadSuccess"
      />
      <div class="icon-label">{{ item.label }}</div>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { iconMap, defaultIcon, preloadIcons } from '@/assets/icons/iconMap'

const emit = defineEmits(['drag-start'])

const deviceTypes = [
  { label: '笔记本', type: 'laptop', iconName: 'laptop' },
  { label: 'PC', type: 'pc', iconName: 'pc' },
  { label: 'Web服务器', type: 'webserver', iconName: 'webserver' },
  { label: '存储服务器', type: 'storage_server', iconName: 'storage_server' },
  { label: '数据库', type: 'database', iconName: 'database' },
  { label: 'DNS', type: 'dns', iconName: 'dns' },
  { label: '防火墙', type: 'firewall', iconName: 'firewall' },
  { label: '邮件服务器', type: 'mail_server', iconName: 'mail_server' },
  { label: '主交换机', type: 'main_switch', iconName: 'main_switch' },
  { label: '光纤交换机', type: 'fiber_switch', iconName: 'fiber_switch' },
  { label: '以太网交换机', type: 'ethernet_switch', iconName: 'ethernet_switch' },
  { label: '路由器', type: 'router', iconName: 'router' }
]

const onDragStart = (item, event) => {
  const iconPath = iconMap[item.iconName] || defaultIcon
  const payload = {
    ...item,
    icon: iconPath,
    iconName: item.iconName
  }
  
  event.dataTransfer.setData('application/json', JSON.stringify(payload))
  event.dataTransfer.effectAllowed = 'copy'
  emit('drag-start', item)
}

// 图标加载成功事件
function onIconLoadSuccess(event) {
}

// 改进的图标加载失败处理
function onIconLoadError(event) {
  const originalSrc = event.target.src
  
  // 尝试使用默认图标
  if (event.target.src !== defaultIcon && !event.target.src.includes(defaultIcon)) {
    event.target.src = defaultIcon
  } else {
    // 如果连默认图标都失败，显示一个简单的占位符
    event.target.style.display = 'none'
    const parent = event.target.parentElement
    if (parent && !parent.querySelector('.fallback-icon')) {
      const fallback = document.createElement('div')
      fallback.className = 'fallback-icon'
      fallback.textContent = '📦'
      fallback.style.cssText = 'font-size: 32px; text-align: center; line-height: 40px; width: 40px; height: 40px; display: flex; align-items: center; justify-content: center; background: #f5f5f5; border-radius: 4px;'
      parent.insertBefore(fallback, event.target)
    }
  }
}

// 组件挂载时预加载图标
onMounted(async () => {
  try {
    await preloadIcons()
  } catch (error) {
  }
})
</script>

<style scoped>
/* Apple 高雅白风格 - 图标面板 */
.icon-panel {
  width: 160px;
  border-right: 1px solid rgba(142, 142, 147, 0.12);
  padding: 14px 10px;
  overflow-y: auto;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  height: 100%;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 10px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
}

/* 自定义滚动条样式 */
.icon-panel::-webkit-scrollbar {
  width: 6px;
}

.icon-panel::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.02);
  border-radius: 3px;
}

.icon-panel::-webkit-scrollbar-thumb {
  background: rgba(142, 142, 147, 0.3);
  border-radius: 3px;
  transition: background 0.3s ease;
}

.icon-panel::-webkit-scrollbar-thumb:hover {
  background: rgba(142, 142, 147, 0.5);
}

.icon-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: grab;
  padding: 10px;
  border-radius: 10px;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  background: rgba(255, 255, 255, 0.98);
  border: 1px solid rgba(142, 142, 147, 0.12);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.04);
}

.icon-item:hover {
  background: rgba(248, 249, 250, 0.98);
  border-color: #007AFF;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.15);
}

.icon-item:active {
  transform: scale(0.96);
  cursor: grabbing;
}

.device-icon {
  width: 48px;
  height: 48px;
  object-fit: contain;
  margin-bottom: 6px;
  filter: drop-shadow(0 1px 2px rgba(0, 0, 0, 0.08));
}

.icon-label {
  font-size: 12px;
  font-weight: 500;
  text-align: center;
  color: #1d1d1f;
  line-height: 1.3;
  letter-spacing: -0.01em;
}
</style>