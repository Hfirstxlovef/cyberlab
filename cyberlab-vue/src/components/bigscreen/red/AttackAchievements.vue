<template>
  <div class="attack-achievements">
    <h3 class="chart-title">攻击成就</h3>
    <div class="achievements-grid">
      <div 
        class="achievement-card" 
        v-for="achievement in achievements" 
        :key="achievement.id"
        :class="{ 'achieved': achievement.achieved }"
      >
        <div class="achievement-icon">{{ achievement.icon }}</div>
        <div class="achievement-info">
          <div class="achievement-name">{{ achievement.name }}</div>
          <div class="achievement-desc">{{ achievement.description }}</div>
          <div class="achievement-points">+{{ achievement.points }} pts</div>
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

const achievements = computed(() => props.data.length > 0 ? props.data : [
  {
    id: 1,
    icon: '🎯',
    name: '首次突破',
    description: '成功获取第一个系统权限',
    points: 100,
    achieved: true
  },
  {
    id: 2,
    icon: '🔓',
    name: '密码破解专家',
    description: '破解10个以上账户密码',
    points: 200,
    achieved: true
  },
  {
    id: 3,
    icon: '🛡️',
    name: '防御绕过',
    description: '成功绕过WAF防护',
    points: 300,
    achieved: true
  },
  {
    id: 4,
    icon: '👑',
    name: '域控制权',
    description: '获取域管理员权限',
    points: 500,
    achieved: false
  },
  {
    id: 5,
    icon: '🏆',
    name: '完美渗透',
    description: '不触发任何告警完成任务',
    points: 1000,
    achieved: false
  },
  {
    id: 6,
    icon: '⚡',
    name: '速度之王',
    description: '30分钟内完成所有目标',
    points: 800,
    achieved: false
  }
])
</script>

<style scoped>
/* ============================================
   攻击成就 - 深色科技风 + Apple优雅
   ============================================ */

.attack-achievements {
  height: 100%;
  display: flex;
  flex-direction: column;
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", "Helvetica Neue", sans-serif;
}

.chart-title {
  margin: 0 0 20px 0;
  font-size: 18px;
  font-weight: 700;
  color: #ffcc00;
  letter-spacing: -0.3px;
  text-shadow: 0 0 20px rgba(255, 204, 0, 0.4);
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
  background: linear-gradient(90deg, #ffcc00 0%, transparent 100%);
  box-shadow: 0 0 10px rgba(255, 204, 0, 0.5);
}

.achievements-grid {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  overflow-y: auto;
  padding-right: 5px;
}

.achievements-grid::-webkit-scrollbar {
  width: 4px;
}

.achievements-grid::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 2px;
}

.achievements-grid::-webkit-scrollbar-thumb {
  background: rgba(255, 204, 0, 0.3);
  border-radius: 2px;
  transition: background 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

.achievements-grid::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 204, 0, 0.5);
}

.achievement-card {
  background: rgba(20, 20, 20, 0.3);
  backdrop-filter: blur(5px);
  -webkit-backdrop-filter: blur(5px);
  border-radius: 12px;
  padding: 14px;
  border: 0.5px solid rgba(255, 255, 255, 0.05);
  display: flex;
  gap: 12px;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
  opacity: 0.4;
  position: relative;
  overflow: hidden;
}

.achievement-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: radial-gradient(circle at center, rgba(255, 204, 0, 0.05) 0%, transparent 70%);
  opacity: 0;
  transition: opacity 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

.achievement-card.achieved {
  opacity: 1;
  background: rgba(30, 30, 20, 0.5);
  border-color: rgba(255, 204, 0, 0.3);
  box-shadow: 0 0 20px rgba(255, 204, 0, 0.15);
}

.achievement-card.achieved::before {
  opacity: 1;
}

.achievement-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.4);
}

.achievement-card.achieved:hover {
  box-shadow: 0 6px 25px rgba(255, 204, 0, 0.25);
  border-color: rgba(255, 204, 0, 0.5);
}

.achievement-icon {
  font-size: 32px;
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  position: relative;
  z-index: 1;
}

.achieved .achievement-icon {
  filter: drop-shadow(0 0 15px rgba(255, 204, 0, 0.5));
  animation: glow 2s cubic-bezier(0.19, 1, 0.22, 1) infinite;
}

@keyframes glow {
  0%, 100% {
    filter: drop-shadow(0 0 15px rgba(255, 204, 0, 0.5));
  }
  50% {
    filter: drop-shadow(0 0 25px rgba(255, 204, 0, 0.8));
  }
}

.achievement-info {
  flex: 1;
  min-width: 0;
  position: relative;
  z-index: 1;
}

.achievement-name {
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;
  margin-bottom: 4px;
  letter-spacing: -0.2px;
}

.achieved .achievement-name {
  color: #ffcc00;
  text-shadow: 0 0 10px rgba(255, 204, 0, 0.3);
}

.achievement-desc {
  font-size: 11px;
  font-weight: 400;
  color: rgba(255, 255, 255, 0.5);
  margin-bottom: 6px;
  line-height: 1.4;
}

.achieved .achievement-desc {
  color: rgba(255, 255, 255, 0.7);
}

.achievement-points {
  font-size: 11px;
  font-weight: 700;
  color: rgba(255, 204, 0, 0.5);
  font-family: "SF Mono", Consolas, monospace;
  letter-spacing: 0.5px;
}

.achieved .achievement-points {
  color: #34c759;
  text-shadow: 0 0 10px rgba(52, 199, 89, 0.5);
}
</style>