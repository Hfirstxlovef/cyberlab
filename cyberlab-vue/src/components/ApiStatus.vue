<template>
  <div class="api-status" v-if="showStatus">
    <div class="status-header" @click="toggleExpanded">
      <span class="status-indicator" :class="{ online: isOnline, offline: !isOnline }"></span>
      <span class="status-text">API状态</span>
      <span class="toggle-icon" :class="{ expanded }">▼</span>
    </div>

    <div class="status-details" v-show="expanded">
      <div class="status-item" v-for="(status, key) in apiStatus" :key="key">
        <span class="service-name">{{ getServiceName(key) }}</span>
        <span class="service-status" :class="{ online: status, offline: !status }">
          {{ status ? '正常' : '异常' }}
        </span>
      </div>

      <div class="status-actions">
        <button @click="testConnection" :disabled="testing" class="test-btn">
          {{ testing ? '测试中...' : '重新测试' }}
        </button>
        <button @click="hideStatus" class="hide-btn">隐藏</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ApiTester } from '../utils/apiTest'

const showStatus = ref(true)
const expanded = ref(false)
const testing = ref(false)
const apiStatus = ref({
  backend: false,
  auth: false,
  topology: false,
  assets: false,
  users: false,
  ranges: false
})

const isOnline = computed(() => {
  return Object.values(apiStatus.value).some(status => status)
})

const serviceNames = {
  backend: '后端服务',
  auth: '认证服务',
  topology: '拓扑服务',
  assets: '资产服务',
  users: '用户服务',
  ranges: '演练服务'
}

const getServiceName = (key) => serviceNames[key] || key

const toggleExpanded = () => {
  expanded.value = !expanded.value
}

const hideStatus = () => {
  showStatus.value = false
}

const testConnection = async () => {
  testing.value = true
  try {
    const results = await ApiTester.testConnection()
    apiStatus.value = results
  } catch {
    // API测试失败，保持当前状态
  } finally {
    testing.value = false
  }
}

onMounted(() => {
  testConnection()
})
</script>

<style scoped>
/* Apple风格的API状态组件 */
.api-status {
  position: fixed;
  top: 24px;
  right: 24px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08),
    0 1px 2px rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.2);
  z-index: 1000;
  min-width: 280px;
  font-size: 15px;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'Helvetica Neue', Arial, sans-serif;
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  overflow: hidden;
}

.api-status:hover {
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.12),
    0 2px 4px rgba(0, 0, 0, 0.06);
  transform: translateY(-1px);
}

.status-header {
  display: flex;
  align-items: center;
  padding: 18px 20px;
  cursor: pointer;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  transition: background-color 0.2s ease;
  position: relative;
}

.status-header:hover {
  background: rgba(0, 0, 0, 0.02);
}

.status-header:active {
  background: rgba(0, 0, 0, 0.04);
  transform: scale(0.995);
}

.status-indicator {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-right: 12px;
  position: relative;
  transition: all 0.3s ease;
}

.status-indicator::before {
  content: '';
  position: absolute;
  top: -2px;
  left: -2px;
  right: -2px;
  bottom: -2px;
  border-radius: 50%;
  opacity: 0.3;
  transition: all 0.3s ease;
}

.status-indicator.online {
  background: linear-gradient(135deg, #34C759, #30D158);
  box-shadow: 0 2px 8px rgba(52, 199, 89, 0.3);
}

.status-indicator.online::before {
  background: #34C759;
  animation: pulse 2s infinite;
}

.status-indicator.offline {
  background: linear-gradient(135deg, #FF3B30, #FF453A);
  box-shadow: 0 2px 8px rgba(255, 59, 48, 0.3);
}

@keyframes pulse {
  0% {
    transform: scale(1);
    opacity: 0.3;
  }

  50% {
    transform: scale(1.2);
    opacity: 0.1;
  }

  100% {
    transform: scale(1);
    opacity: 0.3;
  }
}

.status-text {
  flex: 1;
  font-weight: 600;
  font-size: 16px;
  color: #1d1d1f;
  letter-spacing: -0.01em;
}

.toggle-icon {
  font-size: 12px;
  color: #86868b;
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  transform-origin: center;
}

.toggle-icon.expanded {
  transform: rotate(180deg);
  color: #007AFF;
}

.status-details {
  padding: 20px;
  background: rgba(248, 248, 248, 0.5);
}

.status-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
  transition: all 0.2s ease;
}

.status-item:last-of-type {
  border-bottom: none;
}

.status-item:hover {
  background: rgba(0, 0, 0, 0.02);
  margin: 0 -12px;
  padding: 12px 12px;
  border-radius: 8px;
}

.service-name {
  color: #424245;
  font-weight: 500;
  font-size: 15px;
  letter-spacing: -0.01em;
}

.service-status {
  font-size: 13px;
  font-weight: 600;
  padding: 6px 12px;
  border-radius: 20px;
  letter-spacing: -0.01em;
  transition: all 0.2s ease;
}

.service-status.online {
  background: linear-gradient(135deg, rgba(52, 199, 89, 0.1), rgba(48, 209, 88, 0.1));
  color: #34C759;
  border: 1px solid rgba(52, 199, 89, 0.2);
}

.service-status.offline {
  background: linear-gradient(135deg, rgba(255, 59, 48, 0.1), rgba(255, 69, 58, 0.1));
  color: #FF3B30;
  border: 1px solid rgba(255, 59, 48, 0.2);
}

.status-actions {
  display: flex;
  gap: 12px;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}

.test-btn,
.hide-btn {
  flex: 1;
  padding: 12px 16px;
  border: none;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  font-family: inherit;
  letter-spacing: -0.01em;
  position: relative;
  overflow: hidden;
}

.test-btn {
  background: linear-gradient(135deg, #007AFF, #0051D5);
  color: white;
  box-shadow: 0 4px 16px rgba(0, 122, 255, 0.3);
}

.test-btn:hover {
  background: linear-gradient(135deg, #0056CC, #003D99);
  box-shadow: 0 6px 20px rgba(0, 122, 255, 0.4);
  transform: translateY(-1px);
}

.test-btn:active {
  transform: translateY(0);
  box-shadow: 0 2px 8px rgba(0, 122, 255, 0.3);
}

.hide-btn {
  background: rgba(142, 142, 147, 0.12);
  color: #1d1d1f;
  border: 1px solid rgba(0, 0, 0, 0.04);
}

.hide-btn:hover {
  background: rgba(142, 142, 147, 0.2);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.hide-btn:active {
  transform: translateY(0);
  background: rgba(142, 142, 147, 0.3);
}

.test-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
  box-shadow: 0 2px 8px rgba(0, 122, 255, 0.2);
}

.test-btn:disabled:hover {
  transform: none;
  box-shadow: 0 2px 8px rgba(0, 122, 255, 0.2);
}

/* 深色模式支持 */
@media (prefers-color-scheme: dark) {
  .api-status {
    background: rgba(28, 28, 30, 0.95);
    border: 1px solid rgba(255, 255, 255, 0.1);
  }

  .status-header {
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  }

  .status-header:hover {
    background: rgba(255, 255, 255, 0.05);
  }

  .status-text {
    color: #f2f2f7;
  }

  .status-details {
    background: rgba(44, 44, 46, 0.3);
  }

  .status-item {
    border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  }

  .status-item:hover {
    background: rgba(255, 255, 255, 0.05);
  }

  .service-name {
    color: #aeaeb2;
  }

  .status-actions {
    border-top: 1px solid rgba(255, 255, 255, 0.1);
  }

  .hide-btn {
    background: rgba(72, 72, 74, 0.6);
    color: #f2f2f7;
    border: 1px solid rgba(255, 255, 255, 0.1);
  }

  .hide-btn:hover {
    background: rgba(72, 72, 74, 0.8);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .api-status {
    top: 16px;
    right: 16px;
    min-width: 260px;
  }

  .status-header {
    padding: 16px 18px;
  }

  .status-details {
    padding: 18px;
  }

  .status-actions {
    flex-direction: column;
    gap: 8px;
  }
}
</style>