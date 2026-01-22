<template>
  <div class="overview-cards">
    <div class="card" v-for="(item, index) in cards" :key="index">
      <div class="card-icon">
        {{ item.icon }}
      </div>
      <div class="card-content">
        <div class="card-title">{{ item.title }}</div>
        <div class="card-value">
          <CountUp :end-val="item.value" :duration="2" />
          <span class="unit">{{ item.unit }}</span>
        </div>
        <div class="card-trend" :class="item.trend">
          {{ item.trendIcon }}
          {{ item.trendText }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import CountUp from '@/components/common/CountUp.vue'

const props = defineProps({
  data: {
    type: Object,
    default: () => ({})
  }
})

const cards = computed(() => [
  {
    title: '运行演练数',
    value: props.data.runningDrills ?? 0,  // 使用 ?? 运算符，只在 null/undefined 时fallback
    unit: '个',
    icon: '🎯',
    trend: 'up',
    trendIcon: '↗',
    trendText: '+12%'
  },
  {
    title: '累计攻击次数',
    value: props.data.totalAttacks ?? 0,  // 允许显示真实的0值
    unit: '次',
    icon: '⚡',
    trend: 'up',
    trendIcon: '↗',
    trendText: '+8%'
  },
  {
    title: '当前在线人数',
    value: props.data.onlineUsers ?? 0,  // 允许显示真实的0值
    unit: '人',
    icon: '👥',
    trend: 'stable',
    trendIcon: '→',
    trendText: '0%'
  },
  {
    title: '活跃容器数',
    value: props.data.activeContainers ?? 0,  // 允许显示真实的0值
    unit: '个',
    icon: '🐳',
    trend: 'up',
    trendIcon: '↗',
    trendText: '+5%'
  }
])
</script>

<style scoped>
/* ============================================
   Apple Elegant White Style - Overview Cards
   ============================================ */

/* CSS Variables */
:root {
  --apple-white: #fbfbfd;
  --apple-gray: #f5f5f7;
  --apple-border: rgba(0, 0, 0, 0.04);
  --apple-text: #1d1d1f;
  --apple-text-secondary: #86868b;
  --apple-blue: #007aff;
  --apple-green: #34c759;
  --apple-orange: #ff9500;
  --apple-red: #ff3b30;
  --radius-sm: 12px;
  --radius-md: 16px;
  --shadow-card: 0 4px 16px rgba(0, 0, 0, 0.06);
  --shadow-card-hover: 0 8px 32px rgba(0, 0, 0, 0.12);
  --spacing-md: 16px;
  --spacing-lg: 24px;
  --font-apple: -apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", "Helvetica Neue", sans-serif;
}

.overview-cards {
  display: flex;
  gap: var(--spacing-lg);
  height: 100%;
  font-family: var(--font-apple);
}

.card {
  flex: 1;
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.98) 0%,
    rgba(250, 250, 250, 0.95) 100%);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: var(--radius-md);
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 15px;
  border: 0.5px solid var(--apple-border);
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
  position: relative;
  overflow: hidden;
  box-shadow: var(--shadow-card);
}

.card::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(0, 122, 255, 0.05), transparent);
  transition: left 0.5s ease;
}

.card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-card-hover);
  border-color: rgba(0, 122, 255, 0.15);
}

.card:hover::before {
  left: 100%;
}

.card-icon {
  width: 50px;
  height: 50px;
  border-radius: var(--radius-sm);
  background: linear-gradient(135deg, var(--apple-blue) 0%, #0051d5 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
  box-shadow: 0 4px 15px rgba(0, 122, 255, 0.25);
}

.card-content {
  flex: 1;
}

.card-title {
  font-size: 14px;
  color: var(--apple-text-secondary);
  margin-bottom: 5px;
  font-weight: 500;
}

.card-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--apple-text);
  margin-bottom: 5px;
  display: flex;
  align-items: baseline;
  gap: 5px;
  letter-spacing: -0.5px;
}

.unit {
  font-size: 14px;
  color: var(--apple-text-secondary);
  font-weight: 500;
}

.card-trend {
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 3px;
  font-weight: 600;
}

.card-trend.up {
  color: var(--apple-green);
}

.card-trend.down {
  color: var(--apple-red);
}

.card-trend.stable {
  color: var(--apple-orange);
}

/* Responsive Design */
@media (max-width: 768px) {
  .overview-cards {
    flex-direction: column;
    gap: var(--spacing-md);
  }

  .card {
    padding: 16px;
  }

  .card-value {
    font-size: 24px;
  }
}
</style>