<template>
  <div class="system-monitor">
    <h3 class="chart-title">系统监控</h3>
    <div class="systems-list">
      <div 
        class="system-item" 
        v-for="system in systems" 
        :key="system.id"
        :class="`system-${system.owner}`"
      >
        <div class="system-header">
          <span class="system-name">{{ system.name }}</span>
          <span class="system-status" :class="`status-${system.status}`">
            {{ getStatusText(system.status) }}
          </span>
        </div>
        <div class="system-owner">
          <span class="owner-label">控制方:</span>
          <span class="owner-value">{{ getOwnerText(system.owner) }}</span>
        </div>
        <div class="system-metrics">
          <div class="metric">
            <span class="metric-name">CPU</span>
            <span class="metric-value">{{ system.cpu }}%</span>
          </div>
          <div class="metric">
            <span class="metric-name">内存</span>
            <span class="metric-value">{{ system.memory }}%</span>
          </div>
          <div class="metric">
            <span class="metric-name">网络</span>
            <span class="metric-value">{{ system.network }} Mbps</span>
          </div>
        </div>
        <div class="system-progress">
          <div 
            class="progress-bar" 
            :style="{width: `${system.integrity}%`, background: getIntegrityColor(system.integrity)}"
          ></div>
        </div>
        <div class="integrity-label">系统完整性: {{ system.integrity }}%</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  systems: {
    type: Array,
    default: () => []
  }
})

const systems = ref([
  {
    id: 1,
    name: 'Web服务器',
    status: 'compromised',
    owner: 'red',
    cpu: 78,
    memory: 65,
    network: 120,
    integrity: 45
  },
  {
    id: 2,
    name: '数据库服务器',
    status: 'defended',
    owner: 'blue',
    cpu: 45,
    memory: 52,
    network: 80,
    integrity: 92
  },
  {
    id: 3,
    name: '应用服务器',
    status: 'contested',
    owner: 'neutral',
    cpu: 62,
    memory: 71,
    network: 95,
    integrity: 68
  },
  {
    id: 4,
    name: '文件服务器',
    status: 'normal',
    owner: 'blue',
    cpu: 32,
    memory: 41,
    network: 60,
    integrity: 100
  },
  {
    id: 5,
    name: 'API网关',
    status: 'contested',
    owner: 'neutral',
    cpu: 55,
    memory: 48,
    network: 150,
    integrity: 75
  },
  {
    id: 6,
    name: '邮件服务器',
    status: 'normal',
    owner: 'blue',
    cpu: 28,
    memory: 35,
    network: 40,
    integrity: 95
  }
])

const getStatusText = (status) => {
  const statusMap = {
    'normal': '正常',
    'compromised': '已沦陷',
    'defended': '防守中',
    'contested': '争夺中'
  }
  return statusMap[status] || '未知'
}

const getOwnerText = (owner) => {
  const ownerMap = {
    'red': '🔴 红队',
    'blue': '🔵 蓝队',
    'neutral': '⚪ 中立'
  }
  return ownerMap[owner] || '未知'
}

const getIntegrityColor = (integrity) => {
  if (integrity >= 80) return '#4caf50'
  if (integrity >= 50) return '#ff9800'
  return '#f44336'
}

const updateMetrics = () => {
  systems.value.forEach(system => {
    system.cpu = Math.min(100, Math.max(10, system.cpu + (Math.random() - 0.5) * 10))
    system.memory = Math.min(100, Math.max(10, system.memory + (Math.random() - 0.5) * 8))
    system.network = Math.min(200, Math.max(20, system.network + (Math.random() - 0.5) * 20))
    
    if (system.status === 'contested') {
      system.integrity = Math.min(100, Math.max(30, system.integrity + (Math.random() - 0.5) * 5))
    }
  })
}

let timer = null

onMounted(() => {
  if (props.systems.length > 0) {
    systems.value = props.systems
  }
  timer = setInterval(updateMetrics, 3000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.system-monitor {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.chart-title {
  margin: 0 0 15px 0;
  font-size: 18px;
  color: #9c27b0;
  font-weight: 500;
}

.systems-list {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  overflow-y: auto;
}

.systems-list::-webkit-scrollbar {
  width: 6px;
}

.systems-list::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 3px;
}

.systems-list::-webkit-scrollbar-thumb {
  background: rgba(156, 39, 176, 0.3);
  border-radius: 3px;
}

.system-item {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  padding: 10px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: all 0.3s ease;
}

.system-red {
  border-color: rgba(244, 67, 54, 0.3);
  background: rgba(244, 67, 54, 0.05);
}

.system-blue {
  border-color: rgba(33, 150, 243, 0.3);
  background: rgba(33, 150, 243, 0.05);
}

.system-neutral {
  border-color: rgba(158, 158, 158, 0.3);
  background: rgba(158, 158, 158, 0.05);
}

.system-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.3);
}

.system-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.system-name {
  font-size: 13px;
  font-weight: 500;
  color: #fff;
}

.system-status {
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 4px;
}

.status-normal {
  background: rgba(76, 175, 80, 0.2);
  color: #4caf50;
}

.status-compromised {
  background: rgba(244, 67, 54, 0.2);
  color: #f44336;
}

.status-defended {
  background: rgba(33, 150, 243, 0.2);
  color: #2196f3;
}

.status-contested {
  background: rgba(255, 152, 0, 0.2);
  color: #ff9800;
}

.system-owner {
  display: flex;
  gap: 5px;
  font-size: 11px;
  margin-bottom: 8px;
}

.owner-label {
  color: rgba(255, 255, 255, 0.6);
}

.owner-value {
  color: rgba(255, 255, 255, 0.9);
  font-weight: 500;
}

.system-metrics {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 10px;
}

.metric {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.metric-name {
  color: rgba(255, 255, 255, 0.6);
  margin-bottom: 2px;
}

.metric-value {
  color: rgba(255, 255, 255, 0.9);
  font-weight: 500;
}

.system-progress {
  height: 4px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 2px;
  overflow: hidden;
  margin-bottom: 6px;
}

.progress-bar {
  height: 100%;
  border-radius: 2px;
  transition: all 0.3s ease;
}

.integrity-label {
  font-size: 10px;
  text-align: center;
  color: rgba(255, 255, 255, 0.7);
}
</style>