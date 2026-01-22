<template>
  <div class="container-status">
    <div class="chart-header">
      <h3>🐳 容器状态监控</h3>
      <div class="status-summary">
        <span class="status-item running">运行: {{ runningCount }}</span>
        <span class="status-item stopped">停止: {{ stoppedCount }}</span>
        <span class="status-refresh" @click="refreshData" :class="{ loading: isLoading }">
          <span class="refresh-icon">🔄</span> {{ lastUpdate }}
        </span>
      </div>
    </div>
    
    <div v-if="isLoading && containers.length === 0" class="loading-placeholder">
      <div class="loading-spinner">⟳</div>
      <p>正在获取容器信息...</p>
    </div>
    
    <div v-else-if="containers.length === 0" class="empty-state">
      <p>暂无容器运行</p>
      <button @click="refreshData" class="refresh-btn">刷新数据</button>
    </div>
    
    <div v-else class="container-grid">
      <div 
        v-for="container in containers" 
        :key="container.containerId"
        class="container-card"
        :class="getContainerStatusClass(container.status)">
        <div class="container-header">
          <div class="container-name">{{ container.name }}</div>
          <div class="container-status-badge" :class="getContainerStatusClass(container.status)">
            {{ getStatusText(container.status) }}
          </div>
        </div>
        
        <div class="container-info">
          <div class="info-item">
            <span class="info-label">镜像:</span>
            <span class="info-value">{{ container.image }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">容器ID:</span>
            <span class="info-value">{{ container.containerId.substring(0, 12) }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">CPU:</span>
            <span class="info-value">{{ container.cpuUsage || 'N/A' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">内存:</span>
            <span class="info-value">{{ container.memoryUsage || 'N/A' }}</span>
          </div>
        </div>
        
        <div class="container-actions">
          <button 
            class="action-btn"
            :class="getActionButtonClass(container.status)"
            @click="toggleContainer(container)"
            :disabled="isProcessing">
            {{ getActionButtonText(container.status) }}
          </button>
          <button 
            class="action-btn restart" 
            @click="restartContainer(container)"
            :disabled="isProcessing">
            重启
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import axios from 'axios'
import { getToken } from '@/utils/auth'

// 响应式数据
const containers = ref([])
const isLoading = ref(false)
const isProcessing = ref(false)
const lastUpdate = ref('未更新')
const refreshTimer = ref(null)
const errorMessage = ref('')

// 计算属性
const runningCount = computed(() => {
  return containers.value.filter(container => 
    container.status && container.status.toLowerCase().includes('running')
  ).length
})

const stoppedCount = computed(() => {
  return containers.value.filter(container => 
    container.status && !container.status.toLowerCase().includes('running')
  ).length
})

// API 基础URL
const API_BASE = '/api'

// 获取所有容器信息
const fetchContainers = async () => {
  try {
    isLoading.value = true
    errorMessage.value = ''
    
    // 调用BigScreenController的getContainerStatus API
    const response = await axios.get(`${API_BASE}/bigscreen/container-status`, {
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    
    if (response.data && Array.isArray(response.data)) {
      containers.value = response.data
      updateLastUpdateTime()
    } else {
      containers.value = []
    }
    
  } catch (error) {
    errorMessage.value = '获取容器信息失败'
    
    // 如果API调用失败，使用模拟数据作为后备
    containers.value = getSimulatedData()
    updateLastUpdateTime()
  } finally {
    isLoading.value = false
  }
}

// 刷新数据
const refreshData = async () => {
  await fetchContainers()
}

// 更新最后更新时间
const updateLastUpdateTime = () => {
  const now = new Date()
  lastUpdate.value = now.toLocaleTimeString('zh-CN')
}

// 获取容器状态类
const getContainerStatusClass = (status) => {
  if (!status) return 'unknown'
  const statusLower = status.toLowerCase()
  if (statusLower.includes('running')) return 'running'
  if (statusLower.includes('exited') || statusLower.includes('stopped')) return 'stopped'
  return 'unknown'
}

// 获取状态文本
const getStatusText = (status) => {
  if (!status) return '未知'
  const statusLower = status.toLowerCase()
  if (statusLower.includes('running')) return '运行中'
  if (statusLower.includes('exited')) return '已退出'
  if (statusLower.includes('stopped')) return '已停止'
  return status
}

// 获取操作按钮类
const getActionButtonClass = (status) => {
  if (!status) return 'start'
  const statusLower = status.toLowerCase()
  return statusLower.includes('running') ? 'stop' : 'start'
}

// 获取操作按钮文本
const getActionButtonText = (status) => {
  if (!status) return '启动'
  const statusLower = status.toLowerCase()
  return statusLower.includes('running') ? '停止' : '启动'
}

// 切换容器状态（启动/停止）
const toggleContainer = async (container) => {
  if (isProcessing.value) return
  
  try {
    isProcessing.value = true
    const isRunning = container.status && container.status.toLowerCase().includes('running')
    const action = isRunning ? 'stop' : 'start'
    
    
    // 这里可以调用真实的容器操作API
    // const response = await axios.post(`${API_BASE}/containers/${container.containerId}/${action}`)
    
    // 模拟操作延迟
    await new Promise(resolve => setTimeout(resolve, 1000))
    
    // 刷新容器状态
    await fetchContainers()
    
  } catch (error) {
  } finally {
    isProcessing.value = false
  }
}

// 重启容器
const restartContainer = async (container) => {
  if (isProcessing.value) return
  
  try {
    isProcessing.value = true
    
    // 这里可以调用真实的容器重启API
    // const response = await axios.post(`${API_BASE}/containers/${container.containerId}/restart`)
    
    // 模拟操作延迟
    await new Promise(resolve => setTimeout(resolve, 1500))
    
    // 刷新容器状态
    await fetchContainers()
    
  } catch (error) {
  } finally {
    isProcessing.value = false
  }
}

// 获取模拟数据（作为后备）
const getSimulatedData = () => {
  return [
    {
      containerId: '1a2b3c4d5e6f',
      name: 'cyberlab-mysql',
      image: 'mysql:8.0',
      status: 'Up 2 hours',
      cpuUsage: '3.2%',
      memoryUsage: '156MB / 2GB'
    },
    {
      containerId: '7g8h9i0j1k2l',
      name: 'cyberlab-web',
      image: 'nginx:latest',
      status: 'Up 1 hour',
      cpuUsage: '1.8%',
      memoryUsage: '45MB / 512MB'
    },
    {
      containerId: '3m4n5o6p7q8r',
      name: 'vuln-app',
      image: 'vulnerable/webapp:latest',
      status: 'Exited (0) 10 minutes ago',
      cpuUsage: '0%',
      memoryUsage: '0MB / 0MB'
    },
    {
      containerId: '9s0t1u2v3w4x',
      name: 'kali-tools',
      image: 'kalilinux/kali-rolling:latest',
      status: 'Up 30 minutes',
      cpuUsage: '12.5%',
      memoryUsage: '512MB / 1GB'
    }
  ]
}

// 生命周期钩子
onMounted(() => {
  fetchContainers()
  
  // 设置定时刷新（每30秒）
  refreshTimer.value = setInterval(() => {
    fetchContainers()
  }, 30000)
})

onUnmounted(() => {
  if (refreshTimer.value) {
    clearInterval(refreshTimer.value)
  }
})
</script>

<style scoped>
/* ============================================
   Apple Elegant White Style - Container Status
   ============================================ */

:root {
    --apple-text: #1d1d1f;
    --apple-text-secondary: #86868b;
    --apple-border: rgba(0, 0, 0, 0.04);
    --apple-blue: #007aff;
    --apple-green: #34c759;
    --apple-orange: #ff9500;
    --apple-red: #ff3b30;
    --font-apple: -apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", "Helvetica Neue", sans-serif;
}

.container-status {
    height: 100%;
    display: flex;
    flex-direction: column;
    font-family: var(--font-apple);
}

.chart-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
}

.chart-header h3 {
    margin: 0;
    font-size: 18px;
    font-weight: 700;
    color: var(--apple-text);
    letter-spacing: -0.3px;
}

.status-summary {
    display: flex;
    gap: 15px;
    font-size: 14px;
    align-items: center;
}

.status-refresh {
    cursor: pointer;
    padding: 4px 8px;
    border-radius: 8px;
    font-size: 12px;
    font-weight: 500;
    background: rgba(0, 122, 255, 0.08);
    color: var(--apple-blue);
    transition: all 0.3s ease;
}

.status-refresh:hover {
    background: rgba(0, 122, 255, 0.15);
}

.status-refresh.loading .refresh-icon {
    animation: spin 1s linear infinite;
}

@keyframes spin {
    from { transform: rotate(0deg); }
    to { transform: rotate(360deg); }
}

.loading-placeholder, .empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 200px;
    color: var(--apple-text-secondary);
}

.loading-spinner {
    font-size: 24px;
    animation: spin 1s linear infinite;
    margin-bottom: 10px;
}

.refresh-btn {
    padding: 8px 16px;
    background: linear-gradient(135deg, var(--apple-green) 0%, #28a745 100%);
    border: none;
    border-radius: 8px;
    color: #ffffff;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
    margin-top: 10px;
    box-shadow: 0 2px 8px rgba(52, 199, 89, 0.2);
}

.refresh-btn:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(52, 199, 89, 0.3);
}

.status-item {
    padding: 4px 8px;
    border-radius: 8px;
    font-size: 12px;
    font-weight: 600;
}

.status-item.running {
    background: rgba(52, 199, 89, 0.1);
    color: var(--apple-green);
}

.status-item.stopped {
    background: rgba(255, 59, 48, 0.1);
    color: var(--apple-red);
}

.container-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
    gap: 15px;
    flex: 1;
    overflow-y: auto;
}

.container-card {
    background: linear-gradient(135deg,
        rgba(255, 255, 255, 0.95) 0%,
        rgba(250, 250, 250, 0.9) 100%);
    backdrop-filter: blur(10px);
    -webkit-backdrop-filter: blur(10px);
    border-radius: 12px;
    padding: 15px;
    border: 0.5px solid var(--apple-border);
    transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.container-card:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.container-card.running {
    border-left: 3px solid var(--apple-green);
}

.container-card.stopped {
    border-left: 3px solid var(--apple-red);
}

.container-card.unknown {
    border-left: 3px solid var(--apple-orange);
}

.container-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
}

.container-name {
    font-size: 16px;
    font-weight: 600;
    color: var(--apple-text);
    letter-spacing: -0.3px;
}

.container-status-badge {
    padding: 2px 8px;
    border-radius: 10px;
    font-size: 12px;
    font-weight: 600;
}

.container-status-badge.running {
    background: rgba(52, 199, 89, 0.15);
    color: var(--apple-green);
}

.container-status-badge.stopped {
    background: rgba(255, 59, 48, 0.15);
    color: var(--apple-red);
}

.container-status-badge.unknown {
    background: rgba(255, 149, 0, 0.15);
    color: var(--apple-orange);
}

.container-info {
    margin-bottom: 15px;
}

.info-item {
    display: flex;
    justify-content: space-between;
    margin-bottom: 6px;
    font-size: 13px;
}

.info-label {
    color: var(--apple-text-secondary);
    font-weight: 500;
}

.info-value {
    color: var(--apple-text);
    font-weight: 600;
    letter-spacing: -0.2px;
}

.container-actions {
    display: flex;
    gap: 8px;
}

.action-btn {
    flex: 1;
    padding: 6px 12px;
    border: none;
    border-radius: 8px;
    font-size: 12px;
    font-weight: 600;
    font-family: var(--font-apple);
    cursor: pointer;
    transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

.action-btn.start {
    background: linear-gradient(135deg, var(--apple-green) 0%, #28a745 100%);
    color: #ffffff;
    box-shadow: 0 2px 8px rgba(52, 199, 89, 0.2);
}

.action-btn.start:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(52, 199, 89, 0.3);
}

.action-btn.stop {
    background: linear-gradient(135deg, var(--apple-red) 0%, #d32f2f 100%);
    color: #ffffff;
    box-shadow: 0 2px 8px rgba(255, 59, 48, 0.2);
}

.action-btn.stop:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(255, 59, 48, 0.3);
}

.action-btn.restart {
    background: linear-gradient(135deg, var(--apple-orange) 0%, #e68900 100%);
    color: #ffffff;
    box-shadow: 0 2px 8px rgba(255, 149, 0, 0.2);
}

.action-btn.restart:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(255, 149, 0, 0.3);
}

/* 滚动条样式 - Apple风格 */
.container-grid::-webkit-scrollbar {
    width: 4px;
}

.container-grid::-webkit-scrollbar-track {
    background: rgba(0, 0, 0, 0.04);
    border-radius: 2px;
}

.container-grid::-webkit-scrollbar-thumb {
    background: rgba(0, 0, 0, 0.15);
    border-radius: 2px;
    transition: background 0.2s ease;
}

.container-grid::-webkit-scrollbar-thumb:hover {
    background: rgba(0, 0, 0, 0.25);
}
</style>