<template>
  <div class="target-system-status">
    <h3 class="chart-title">目标系统状态</h3>
    <div class="systems-list">
      <div class="system-card" v-for="system in systems" :key="system.id">
        <div class="system-header">
          <span class="system-name">{{ system.name }}</span>
          <span class="system-status" :class="`status-${system.status}`">
            {{ getStatusText(system.status) }}
          </span>
        </div>
        <div class="system-info">
          <div class="info-row">
            <span class="info-label">IP:</span>
            <span class="info-value">{{ system.ip }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">端口:</span>
            <span class="info-value">{{ system.ports?.join(', ') || 'N/A' }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">进度:</span>
            <div class="progress-bar">
              <div class="progress-fill" :style="{width: `${system.progress}%`}"></div>
            </div>
            <span class="progress-text">{{ system.progress }}%</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  data: {
    type: Array,
    default: () => []
  }
})

const systems = computed(() => props.data.length > 0 ? props.data : [
  {
    id: 1,
    name: 'Web服务器',
    ip: '192.168.1.100',
    ports: [80, 443],
    status: 'compromised',
    progress: 85
  },
  {
    id: 2,
    name: 'API网关',
    ip: '192.168.1.101',
    ports: [8080, 8443],
    status: 'scanning',
    progress: 45
  },
  {
    id: 3,
    name: 'FTP服务器',
    ip: '192.168.1.102',
    ports: [21, 22],
    status: 'vulnerable',
    progress: 60
  },
  {
    id: 4,
    name: '数据库服务器',
    ip: '192.168.1.103',
    ports: [3306],
    status: 'protected',
    progress: 15
  }
])

const getStatusText = (status) => {
  const statusMap = {
    'compromised': '已攻陷',
    'scanning': '扫描中',
    'vulnerable': '存在漏洞',
    'protected': '防护中',
    'unknown': '未知'
  }
  return statusMap[status] || '未知'
}
</script>

<style scoped>
/* ============================================
   目标系统状态 - 深色科技风 + Apple优雅
   ============================================ */

.target-system-status {
  height: 100%;
  display: flex;
  flex-direction: column;
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", "Helvetica Neue", sans-serif;
}

.chart-title {
  margin: 0 0 20px 0;
  font-size: 18px;
  font-weight: 700;
  color: #34c759;
  letter-spacing: -0.3px;
  text-shadow: 0 0 20px rgba(52, 199, 89, 0.4);
  position: relative;
  padding-bottom: 12px;
}

.chart-title::after {
  content: '';
  position: absolute;
  left: 0;
  bottom: 0;
  width: 40px;
  height: 2px;
  background: linear-gradient(90deg, #34c759 0%, transparent 100%);
  box-shadow: 0 0 10px rgba(52, 199, 89, 0.5);
}

.systems-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-right: 5px;
}

.systems-list::-webkit-scrollbar {
  width: 4px;
}

.systems-list::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 2px;
}

.systems-list::-webkit-scrollbar-thumb {
  background: rgba(255, 59, 48, 0.3);
  border-radius: 2px;
  transition: background 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

.systems-list::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 59, 48, 0.5);
}

.system-card {
  background: rgba(20, 20, 20, 0.5);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-radius: 12px;
  padding: 16px;
  border: 0.5px solid rgba(255, 255, 255, 0.08);
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  position: relative;
  overflow: hidden;
}

.system-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 3px;
  height: 100%;
  background: linear-gradient(180deg, rgba(255, 59, 48, 0.6) 0%, rgba(255, 59, 48, 0.2) 100%);
  opacity: 0;
  transition: opacity 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

.system-card:hover {
  transform: translateX(6px);
  border-color: rgba(255, 59, 48, 0.3);
  background: rgba(30, 30, 30, 0.6);
  box-shadow: 0 4px 16px rgba(255, 59, 48, 0.15);
}

.system-card:hover::before {
  opacity: 1;
}

.system-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.system-name {
  font-size: 15px;
  font-weight: 600;
  color: #ffffff;
  letter-spacing: -0.2px;
}

.system-status {
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 10px;
  font-weight: 700;
  font-family: "SF Mono", Consolas, monospace;
  letter-spacing: 0.5px;
}

.status-compromised {
  background: linear-gradient(135deg, rgba(255, 59, 48, 0.25) 0%, rgba(255, 59, 48, 0.15) 100%);
  color: #ff3b30;
  text-shadow: 0 0 10px rgba(255, 59, 48, 0.5);
  box-shadow: 0 0 15px rgba(255, 59, 48, 0.3);
  animation: pulse-red 2s cubic-bezier(0.19, 1, 0.22, 1) infinite;
}

.status-scanning {
  background: linear-gradient(135deg, rgba(0, 122, 255, 0.25) 0%, rgba(0, 122, 255, 0.15) 100%);
  color: #007aff;
  text-shadow: 0 0 10px rgba(0, 122, 255, 0.5);
  animation: pulse-blue 2s cubic-bezier(0.19, 1, 0.22, 1) infinite;
}

.status-vulnerable {
  background: linear-gradient(135deg, rgba(255, 149, 0, 0.25) 0%, rgba(255, 149, 0, 0.15) 100%);
  color: #ff9500;
  text-shadow: 0 0 10px rgba(255, 149, 0, 0.5);
}

.status-protected {
  background: linear-gradient(135deg, rgba(52, 199, 89, 0.25) 0%, rgba(52, 199, 89, 0.15) 100%);
  color: #34c759;
  text-shadow: 0 0 10px rgba(52, 199, 89, 0.5);
}

@keyframes pulse-red {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

@keyframes pulse-blue {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

.system-info {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.info-row {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
}

.info-label {
  color: rgba(255, 255, 255, 0.5);
  min-width: 50px;
  font-weight: 500;
}

.info-value {
  color: rgba(255, 255, 255, 0.9);
  font-family: "SF Mono", Consolas, Monaco, monospace;
  font-weight: 500;
}

.progress-bar {
  flex: 1;
  height: 6px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 3px;
  overflow: hidden;
  box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.3);
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #ff3b30 0%, #ff6b59 100%);
  border-radius: 3px;
  transition: width 0.5s cubic-bezier(0.19, 1, 0.22, 1);
  box-shadow: 0 0 10px rgba(255, 59, 48, 0.5);
  position: relative;
}

.progress-fill::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(90deg, transparent 0%, rgba(255, 255, 255, 0.2) 50%, transparent 100%);
  animation: shimmer 2s infinite;
}

@keyframes shimmer {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}

.progress-text {
  color: rgba(255, 255, 255, 0.7);
  font-size: 11px;
  min-width: 40px;
  text-align: right;
  font-family: "SF Mono", Consolas, monospace;
  font-weight: 600;
}
</style>