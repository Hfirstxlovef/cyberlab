<template>
  <div class="security-alert-panel">
    <div class="panel-header">
      <h3 class="panel-title">🚨 安全告警监控</h3>
      <div class="alert-stats">
        <span class="stat-item critical">严重: {{ criticalCount }}</span>
        <span class="stat-item high">高危: {{ highCount }}</span>
        <span class="stat-item medium">中危: {{ mediumCount }}</span>
      </div>
    </div>
    <div class="panel-content">
      <div class="alert-list">
        <div 
          v-for="(alert, index) in displayAlerts" 
          :key="index"
          class="alert-item"
          :class="[`severity-${alert.severity}`, { 'new-alert': alert.isNew }]"
        >
          <div class="alert-indicator">
            <span class="severity-icon">{{ getSeverityIcon(alert.severity) }}</span>
            <span class="alert-time">{{ alert.time }}</span>
          </div>
          <div class="alert-info">
            <div class="alert-title">{{ alert.title }}</div>
            <div class="alert-details">
              <span class="alert-source">来源: {{ alert.source }}</span>
              <span class="alert-target">目标: {{ alert.target }}</span>
            </div>
            <div class="alert-description">{{ alert.description }}</div>
          </div>
          <div class="alert-action">
            <button class="action-btn" @click="handleAlert(alert)">
              {{ alert.handled ? '已处理' : '处理' }}
            </button>
          </div>
        </div>
      </div>
      <div v-if="displayAlerts.length === 0" class="no-alerts">
        <span class="no-alerts-icon">✅</span>
        <span class="no-alerts-text">当前无安全告警</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  data: {
    type: Array,
    default: () => []
  }
})

const alerts = ref([])
let alertInterval = null

const displayAlerts = computed(() => {
  if (props.data && props.data.length > 0) {
    return props.data.slice(0, 10)
  }
  return alerts.value.slice(0, 10)
})

const criticalCount = computed(() => {
  return displayAlerts.value.filter(a => a.severity === 'critical').length
})

const highCount = computed(() => {
  return displayAlerts.value.filter(a => a.severity === 'high').length
})

const mediumCount = computed(() => {
  return displayAlerts.value.filter(a => a.severity === 'medium').length
})

const getSeverityIcon = (severity) => {
  const icons = {
    critical: '🔴',
    high: '🟠',
    medium: '🟡',
    low: '🟢'
  }
  return icons[severity] || '⚪'
}

const handleAlert = (alert) => {
  alert.handled = !alert.handled
}

const generateMockAlert = () => {
  const severities = ['critical', 'high', 'medium', 'low']
  const sources = ['IDS', 'WAF', 'Firewall', 'SIEM', 'EDR']
  const titles = [
    'SQL注入攻击尝试',
    'DDoS攻击检测',
    '异常登录行为',
    '端口扫描活动',
    '恶意软件检测',
    '数据外泄尝试',
    'XSS攻击尝试',
    '暴力破解攻击'
  ]
  
  const now = new Date()
  const time = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}`
  
  return {
    severity: severities[Math.floor(Math.random() * severities.length)],
    time,
    title: titles[Math.floor(Math.random() * titles.length)],
    source: sources[Math.floor(Math.random() * sources.length)],
    target: `192.168.${Math.floor(Math.random() * 255)}.${Math.floor(Math.random() * 255)}`,
    description: '检测到可疑活动，已自动触发防护机制',
    isNew: true,
    handled: false
  }
}

const initMockData = () => {
  // 初始化一些模拟告警
  const initialAlerts = []
  for (let i = 0; i < 5; i++) {
    const alert = generateMockAlert()
    alert.isNew = false
    initialAlerts.push(alert)
  }
  alerts.value = initialAlerts
}

const startAlertSimulation = () => {
  alertInterval = setInterval(() => {
    // 随机生成新告警
    if (Math.random() > 0.7) {
      const newAlert = generateMockAlert()
      alerts.value.unshift(newAlert)
      
      // 保持列表长度
      if (alerts.value.length > 20) {
        alerts.value.pop()
      }
      
      // 移除新告警标记
      setTimeout(() => {
        newAlert.isNew = false
      }, 3000)
    }
  }, 5000)
}

onMounted(() => {
  if (!props.data || props.data.length === 0) {
    initMockData()
    startAlertSimulation()
  }
})

onUnmounted(() => {
  if (alertInterval) {
    clearInterval(alertInterval)
  }
})
</script>

<style scoped>
.security-alert-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.panel-title {
  font-size: 18px;
  font-weight: 600;
  color: #ff6b6b;
  margin: 0;
}

.alert-stats {
  display: flex;
  gap: 15px;
}

.stat-item {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 500;
}

.stat-item.critical {
  background: rgba(255, 77, 77, 0.2);
  color: #ff4d4d;
  border: 1px solid rgba(255, 77, 77, 0.3);
}

.stat-item.high {
  background: rgba(255, 152, 0, 0.2);
  color: #ff9800;
  border: 1px solid rgba(255, 152, 0, 0.3);
}

.stat-item.medium {
  background: rgba(255, 193, 7, 0.2);
  color: #ffc107;
  border: 1px solid rgba(255, 193, 7, 0.3);
}

.panel-content {
  flex: 1;
  overflow: hidden;
}

.alert-list {
  height: 100%;
  overflow-y: auto;
  padding-right: 5px;
}

.alert-list::-webkit-scrollbar {
  width: 6px;
}

.alert-list::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 3px;
}

.alert-list::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 3px;
}

.alert-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  margin-bottom: 10px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 8px;
  border-left: 3px solid transparent;
  transition: all 0.3s ease;
}

.alert-item:hover {
  background: rgba(255, 255, 255, 0.05);
  transform: translateX(2px);
}

.alert-item.new-alert {
  animation: newAlert 1s ease;
}

@keyframes newAlert {
  0% {
    background: rgba(255, 107, 107, 0.3);
    transform: scale(1.02);
  }
  100% {
    background: rgba(255, 255, 255, 0.03);
    transform: scale(1);
  }
}

.alert-item.severity-critical {
  border-left-color: #ff4d4d;
}

.alert-item.severity-high {
  border-left-color: #ff9800;
}

.alert-item.severity-medium {
  border-left-color: #ffc107;
}

.alert-item.severity-low {
  border-left-color: #4caf50;
}

.alert-indicator {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
  min-width: 50px;
}

.severity-icon {
  font-size: 20px;
}

.alert-time {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
}

.alert-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.alert-title {
  font-size: 14px;
  font-weight: 500;
  color: #ffffff;
}

.alert-details {
  display: flex;
  gap: 15px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.alert-description {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

.alert-action {
  display: flex;
  align-items: center;
}

.action-btn {
  padding: 6px 12px;
  background: rgba(70, 130, 180, 0.2);
  border: 1px solid rgba(70, 130, 180, 0.4);
  color: #4682b4;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.action-btn:hover {
  background: rgba(70, 130, 180, 0.3);
  border-color: rgba(70, 130, 180, 0.6);
  color: #ffffff;
}

.no-alerts {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  gap: 10px;
}

.no-alerts-icon {
  font-size: 48px;
}

.no-alerts-text {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.5);
}
</style>