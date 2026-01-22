<template>
  <div class="system-health">
    <h3 class="chart-title">系统健康状态</h3>
    <div class="systems-grid">
      <div 
        class="system-card" 
        v-for="system in systems" 
        :key="system.id"
        :class="`status-${system.status}`"
      >
        <div class="system-header">
          <span class="system-icon">{{ system.icon }}</span>
          <span class="system-name">{{ system.name }}</span>
        </div>
        <div class="system-metrics">
          <div class="metric-item">
            <span class="metric-label">CPU</span>
            <div class="metric-bar">
              <div class="metric-fill" :style="{width: `${system.cpu}%`, background: getMetricColor(system.cpu)}"></div>
            </div>
            <span class="metric-value">{{ system.cpu }}%</span>
          </div>
          <div class="metric-item">
            <span class="metric-label">内存</span>
            <div class="metric-bar">
              <div class="metric-fill" :style="{width: `${system.memory}%`, background: getMetricColor(system.memory)}"></div>
            </div>
            <span class="metric-value">{{ system.memory }}%</span>
          </div>
          <div class="metric-item">
            <span class="metric-label">网络</span>
            <div class="metric-bar">
              <div class="metric-fill" :style="{width: `${system.network}%`, background: getMetricColor(system.network)}"></div>
            </div>
            <span class="metric-value">{{ system.network }}%</span>
          </div>
        </div>
        <div class="system-status">
          <span class="status-dot"></span>
          <span class="status-text">{{ getStatusText(system.status) }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  data: {
    type: Array,
    default: () => []
  }
})

const systems = ref([
  {
    id: 1,
    icon: '🌐',
    name: 'Web服务器',
    cpu: 45,
    memory: 62,
    network: 38,
    status: 'normal'
  },
  {
    id: 2,
    icon: '🗄️',
    name: '数据库服务',
    cpu: 72,
    memory: 85,
    network: 55,
    status: 'warning'
  },
  {
    id: 3,
    icon: '📁',
    name: '文件服务器',
    cpu: 28,
    memory: 45,
    network: 22,
    status: 'normal'
  },
  {
    id: 4,
    icon: '🔌',
    name: 'API网关',
    cpu: 58,
    memory: 68,
    network: 78,
    status: 'normal'
  },
  {
    id: 5,
    icon: '📧',
    name: '邮件服务器',
    cpu: 92,
    memory: 88,
    network: 45,
    status: 'critical'
  },
  {
    id: 6,
    icon: '🔍',
    name: '监控系统',
    cpu: 35,
    memory: 42,
    network: 30,
    status: 'normal'
  }
])

const getMetricColor = (value) => {
  if (value >= 80) return '#f44336'
  if (value >= 60) return '#ff9800'
  return '#4caf50'
}

const getStatusText = (status) => {
  const statusMap = {
    'normal': '正常运行',
    'warning': '性能警告',
    'critical': '严重告警',
    'offline': '离线'
  }
  return statusMap[status] || '未知'
}

const updateMetrics = () => {
  systems.value.forEach(system => {
    system.cpu = Math.min(100, Math.max(10, system.cpu + (Math.random() - 0.5) * 20))
    system.memory = Math.min(100, Math.max(10, system.memory + (Math.random() - 0.5) * 15))
    system.network = Math.min(100, Math.max(10, system.network + (Math.random() - 0.5) * 25))
    
    if (system.cpu > 80 || system.memory > 80) {
      system.status = 'critical'
    } else if (system.cpu > 60 || system.memory > 60) {
      system.status = 'warning'
    } else {
      system.status = 'normal'
    }
  })
}

let timer = null

onMounted(() => {
  timer = setInterval(updateMetrics, 5000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.system-health {
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

.systems-grid {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  overflow-y: auto;
}

.system-card {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  padding: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: all 0.3s ease;
}

.system-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.3);
}

.status-normal {
  border-color: rgba(76, 175, 80, 0.3);
}

.status-warning {
  border-color: rgba(255, 152, 0, 0.3);
  background: rgba(255, 152, 0, 0.05);
}

.status-critical {
  border-color: rgba(244, 67, 54, 0.3);
  background: rgba(244, 67, 54, 0.05);
  animation: criticalPulse 2s ease infinite;
}

@keyframes criticalPulse {
  0%, 100% {
    box-shadow: 0 0 0 0 rgba(244, 67, 54, 0.4);
  }
  50% {
    box-shadow: 0 0 10px 5px rgba(244, 67, 54, 0.2);
  }
}

.system-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.system-icon {
  font-size: 20px;
}

.system-name {
  font-size: 13px;
  font-weight: 500;
  color: #fff;
}

.system-metrics {
  margin-bottom: 10px;
}

.metric-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.metric-label {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.6);
  width: 30px;
}

.metric-bar {
  flex: 1;
  height: 4px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 2px;
  overflow: hidden;
}

.metric-fill {
  height: 100%;
  border-radius: 2px;
  transition: all 0.3s ease;
}

.metric-value {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.8);
  width: 35px;
  text-align: right;
}

.system-status {
  display: flex;
  align-items: center;
  gap: 6px;
  padding-top: 8px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  animation: statusPulse 2s ease infinite;
}

.status-normal .status-dot {
  background: #4caf50;
}

.status-warning .status-dot {
  background: #ff9800;
}

.status-critical .status-dot {
  background: #f44336;
}

@keyframes statusPulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

.status-text {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.8);
}
</style>