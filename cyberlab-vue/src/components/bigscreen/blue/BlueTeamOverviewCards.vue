<template>
  <div class="overview-cards">
    <div class="card" v-for="(item, index) in cards" :key="index">
      <div class="card-icon">
        <i :class="item.icon"></i>
      </div>
      <div class="card-content">
        <div class="card-value">{{ formatValue(item.value) }}</div>
        <div class="card-label">{{ item.label }}</div>
        <div class="card-trend" :class="getTrendClass(item.trend)">
          <span class="trend-icon">{{ getTrendIcon(item.trend) }}</span>
          <span class="trend-value">{{ Math.abs(item.trend) }}%</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  data: {
    type: Object,
    default: () => ({})
  }
})

const cards = computed(() => [
  {
    icon: 'shield-icon',
    label: '防护成功率',
    value: props.data.defenseRate || 95.8,
    trend: 2.3
  },
  {
    icon: 'alert-icon',
    label: '活跃威胁',
    value: props.data.activeThreats || 12,
    trend: -15.2
  },
  {
    icon: 'server-icon',
    label: '系统健康度',
    value: props.data.systemHealth || 98.5,
    trend: 1.8
  },
  {
    icon: 'firewall-icon',
    label: '防火墙拦截',
    value: props.data.blockedAttacks || 3567,
    trend: 28.4
  }
])

const formatValue = (value) => {
  if (typeof value === 'number') {
    if (value >= 1000) {
      return (value / 1000).toFixed(1) + 'K'
    }
    if (value < 100 && value.toString().includes('.')) {
      return value.toFixed(1) + '%'
    }
    return value.toLocaleString()
  }
  return value
}

const getTrendClass = (trend) => {
  if (trend > 0) return 'trend-up'
  if (trend < 0) return 'trend-down'
  return 'trend-neutral'
}

const getTrendIcon = (trend) => {
  if (trend > 0) return '↑'
  if (trend < 0) return '↓'
  return '→'
}
</script>

<style scoped>
.overview-cards {
  display: flex;
  gap: 20px;
  width: 100%;
  height: 100%;
}

.card {
  flex: 1;
  background: linear-gradient(135deg, rgba(70, 130, 180, 0.1) 0%, rgba(30, 144, 255, 0.05) 100%);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  border: 1px solid rgba(70, 130, 180, 0.2);
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, #4682b4, transparent);
  animation: slideIn 3s ease-in-out infinite;
}

@keyframes slideIn {
  0% {
    transform: translateX(-100%);
  }
  100% {
    transform: translateX(100%);
  }
}

.card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(70, 130, 180, 0.2);
  border-color: rgba(70, 130, 180, 0.4);
}

.card-icon {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #4682b4 0%, #1e90ff 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
  box-shadow: 0 4px 12px rgba(70, 130, 180, 0.3);
}

.shield-icon::before {
  content: '🛡️';
}

.alert-icon::before {
  content: '⚠️';
}

.server-icon::before {
  content: '🖥️';
}

.firewall-icon::before {
  content: '🔥';
}

.card-content {
  flex: 1;
}

.card-value {
  font-size: 28px;
  font-weight: 700;
  color: #ffffff;
  margin-bottom: 4px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.card-label {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 8px;
}

.card-trend {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
}

.trend-up {
  background: rgba(76, 175, 80, 0.2);
  color: #4caf50;
}

.trend-down {
  background: rgba(244, 67, 54, 0.2);
  color: #f44336;
}

.trend-neutral {
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.6);
}

.trend-icon {
  font-size: 14px;
}
</style>