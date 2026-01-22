<template>
  <div class="system-health-status">
    <div class="health-header">
      <h3 class="health-title">💻 系统健康状态</h3>
      <div class="health-score">
        <span class="score-label">健康度</span>
        <span class="score-value" :class="getHealthClass(overallHealth)">
          {{ overallHealth }}%
        </span>
      </div>
    </div>
    
    <div class="services-grid">
      <div 
        v-for="service in services" 
        :key="service.name"
        class="service-card"
        :class="getServiceClass(service.status)"
      >
        <div class="service-icon">
          <i :class="getServiceIcon(service.type)"></i>
        </div>
        <div class="service-info">
          <div class="service-name">{{ service.name }}</div>
          <div class="service-status">
            <span class="status-dot" :class="service.status"></span>
            <span class="status-text">{{ getStatusText(service.status) }}</span>
          </div>
          <div class="service-metrics">
            <div class="metric">
              <span class="metric-label">CPU</span>
              <span class="metric-value">{{ service.cpu }}%</span>
            </div>
            <div class="metric">
              <span class="metric-label">内存</span>
              <span class="metric-value">{{ service.memory }}%</span>
            </div>
          </div>
        </div>
        <div class="service-uptime">
          <span class="uptime-label">运行时间</span>
          <span class="uptime-value">{{ service.uptime }}</span>
        </div>
      </div>
    </div>

    <div class="health-indicators">
      <div class="indicator" v-for="indicator in healthIndicators" :key="indicator.name">
        <div class="indicator-header">
          <span class="indicator-name">{{ indicator.name }}</span>
          <span class="indicator-value" :class="getIndicatorClass(indicator.value)">
            {{ indicator.value }}{{ indicator.unit }}
          </span>
        </div>
        <div class="indicator-bar">
          <div 
            class="indicator-fill" 
            :style="{ 
              width: `${indicator.percentage}%`,
              background: getIndicatorColor(indicator.percentage)
            }"
          ></div>
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

const services = computed(() => {
  if (props.data && props.data.length > 0) {
    return props.data
  }
  
  return [
    {
      name: 'Web服务器',
      type: 'web',
      status: 'healthy',
      cpu: 23,
      memory: 45,
      uptime: '15天3小时'
    },
    {
      name: '数据库',
      type: 'database',
      status: 'healthy',
      cpu: 35,
      memory: 67,
      uptime: '30天12小时'
    },
    {
      name: '防火墙',
      type: 'firewall',
      status: 'healthy',
      cpu: 15,
      memory: 28,
      uptime: '45天8小时'
    },
    {
      name: 'IDS/IPS',
      type: 'security',
      status: 'warning',
      cpu: 78,
      memory: 82,
      uptime: '7天19小时'
    },
    {
      name: '邮件服务',
      type: 'mail',
      status: 'healthy',
      cpu: 12,
      memory: 34,
      uptime: '22天5小时'
    },
    {
      name: 'VPN网关',
      type: 'vpn',
      status: 'healthy',
      cpu: 28,
      memory: 41,
      uptime: '60天2小时'
    }
  ]
})

const overallHealth = computed(() => {
  const healthyCount = services.value.filter(s => s.status === 'healthy').length
  return Math.round((healthyCount / services.value.length) * 100)
})

const healthIndicators = computed(() => [
  {
    name: '响应时间',
    value: 125,
    unit: 'ms',
    percentage: 75,
    threshold: 200
  },
  {
    name: '可用性',
    value: 99.9,
    unit: '%',
    percentage: 99.9,
    threshold: 95
  },
  {
    name: '错误率',
    value: 0.3,
    unit: '%',
    percentage: 97,
    threshold: 5
  },
  {
    name: '吞吐量',
    value: 8500,
    unit: 'req/s',
    percentage: 85,
    threshold: 10000
  }
])

const getHealthClass = (health) => {
  if (health >= 90) return 'excellent'
  if (health >= 70) return 'good'
  if (health >= 50) return 'warning'
  return 'critical'
}

const getServiceClass = (status) => {
  return `service-${status}`
}

const getServiceIcon = (type) => {
  const icons = {
    web: 'web-icon',
    database: 'db-icon',
    firewall: 'fw-icon',
    security: 'sec-icon',
    mail: 'mail-icon',
    vpn: 'vpn-icon'
  }
  return icons[type] || 'default-icon'
}

const getStatusText = (status) => {
  const texts = {
    healthy: '正常',
    warning: '警告',
    error: '异常',
    offline: '离线'
  }
  return texts[status] || '未知'
}

const getIndicatorClass = (value) => {
  return value > 90 ? 'good' : value > 70 ? 'warning' : 'critical'
}

const getIndicatorColor = (percentage) => {
  if (percentage >= 90) return 'linear-gradient(90deg, #4caf50, #66bb6a)'
  if (percentage >= 70) return 'linear-gradient(90deg, #ff9800, #ffb347)'
  if (percentage >= 50) return 'linear-gradient(90deg, #ffc107, #ffd54f)'
  return 'linear-gradient(90deg, #f44336, #ff6b6b)'
}
</script>

<style scoped>
.system-health-status {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.health-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.health-title {
  font-size: 18px;
  font-weight: 600;
  color: #ffffff;
  margin: 0;
}

.health-score {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: rgba(70, 130, 180, 0.1);
  border-radius: 8px;
}

.score-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.score-value {
  font-size: 24px;
  font-weight: 700;
}

.score-value.excellent {
  color: #4caf50;
}

.score-value.good {
  color: #8bc34a;
}

.score-value.warning {
  color: #ff9800;
}

.score-value.critical {
  color: #f44336;
}

.services-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

.service-card {
  background: rgba(70, 130, 180, 0.05);
  border: 1px solid rgba(70, 130, 180, 0.2);
  border-radius: 8px;
  padding: 12px;
  transition: all 0.3s ease;
}

.service-card.service-healthy {
  border-color: rgba(76, 175, 80, 0.3);
}

.service-card.service-warning {
  border-color: rgba(255, 152, 0, 0.3);
  background: rgba(255, 152, 0, 0.05);
}

.service-card.service-error {
  border-color: rgba(244, 67, 54, 0.3);
  background: rgba(244, 67, 54, 0.05);
}

.service-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.service-icon {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #4682b4, #1e90ff);
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 8px;
  font-size: 16px;
}

.web-icon::before { content: '🌐'; }
.db-icon::before { content: '🗄️'; }
.fw-icon::before { content: '🔥'; }
.sec-icon::before { content: '🔒'; }
.mail-icon::before { content: '📧'; }
.vpn-icon::before { content: '🔐'; }

.service-info {
  margin-bottom: 8px;
}

.service-name {
  font-size: 13px;
  font-weight: 600;
  color: #ffffff;
  margin-bottom: 4px;
}

.service-status {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 8px;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  animation: pulse 2s infinite;
}

.status-dot.healthy {
  background: #4caf50;
}

.status-dot.warning {
  background: #ff9800;
}

.status-dot.error {
  background: #f44336;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.6;
    transform: scale(1.2);
  }
}

.status-text {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.6);
}

.service-metrics {
  display: flex;
  gap: 12px;
}

.metric {
  display: flex;
  flex-direction: column;
}

.metric-label {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.5);
}

.metric-value {
  font-size: 12px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.8);
}

.service-uptime {
  padding-top: 8px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.uptime-label {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.5);
}

.uptime-value {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.7);
}

.health-indicators {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.indicator {
  background: rgba(70, 130, 180, 0.05);
  padding: 8px 12px;
  border-radius: 6px;
}

.indicator-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.indicator-name {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}

.indicator-value {
  font-size: 14px;
  font-weight: 600;
}

.indicator-value.good {
  color: #4caf50;
}

.indicator-value.warning {
  color: #ff9800;
}

.indicator-value.critical {
  color: #f44336;
}

.indicator-bar {
  width: 100%;
  height: 4px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 2px;
  overflow: hidden;
}

.indicator-fill {
  height: 100%;
  border-radius: 2px;
  transition: width 0.3s ease;
}
</style>