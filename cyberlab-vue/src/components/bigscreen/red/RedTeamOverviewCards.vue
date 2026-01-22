<template>
  <div class="red-team-overview-cards">
    <div class="overview-card" v-for="card in overviewCards" :key="card.id">
      <div class="card-icon" :style="{background: card.color}">
        {{ card.icon }}
      </div>
      <div class="card-content">
        <div class="card-value">{{ card.value }}</div>
        <div class="card-label">{{ card.label }}</div>
        <div class="card-trend" :class="card.trend > 0 ? 'trend-up' : 'trend-down'">
          <span>{{ card.trend > 0 ? '↑' : '↓' }}</span>
          {{ Math.abs(card.trend) }}%
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

const overviewCards = computed(() => [
  {
    id: 1,
    icon: '🎯',
    label: '已攻陷目标',
    value: props.data.compromisedTargets || 12,
    trend: 15,
    color: 'linear-gradient(135deg, #f44336, #e91e63)'
  },
  {
    id: 2,
    icon: '🔍',
    label: '发现漏洞',
    value: props.data.vulnerabilities || 58,
    trend: 8,
    color: 'linear-gradient(135deg, #ff9800, #ff5722)'
  },
  {
    id: 3,
    icon: '⚡',
    label: '攻击成功率',
    value: props.data.successRate || '78%',
    trend: -3,
    color: 'linear-gradient(135deg, #4caf50, #8bc34a)'
  },
  {
    id: 4,
    icon: '📊',
    label: '积分排名',
    value: props.data.ranking || '#2',
    trend: 10,
    color: 'linear-gradient(135deg, #2196f3, #03a9f4)'
  }
])
</script>

<style scoped>
.red-team-overview-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  height: 100%;
}

.overview-card {
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  padding: 20px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  align-items: center;
  gap: 15px;
  transition: all 0.3s ease;
}

.overview-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
  border-color: rgba(255, 107, 107, 0.3);
}

.card-icon {
  width: 50px;
  height: 50px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
}

.card-content {
  flex: 1;
  min-width: 0;
}

.card-value {
  font-size: 24px;
  font-weight: bold;
  color: #fff;
  margin-bottom: 4px;
}

.card-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 6px;
}

.card-trend {
  font-size: 12px;
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 2px 6px;
  border-radius: 4px;
}

.trend-up {
  color: #4caf50;
  background: rgba(76, 175, 80, 0.1);
}

.trend-down {
  color: #f44336;
  background: rgba(244, 67, 54, 0.1);
}
</style>