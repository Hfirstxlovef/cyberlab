<template>
  <div class="team-ranking">
    <div class="chart-header">
      <h3>🏆 战队排行榜</h3>
      <div class="ranking-type">
        <span>实时积分</span>
      </div>
    </div>
    
    <div class="ranking-list">
      <div 
        v-for="(team, index) in sortedTeams" 
        :key="team.id"
        class="ranking-item"
        :class="getRankingClass(index)">
        <div class="rank-number">
          <span v-if="index < 3" class="medal">{{ getMedal(index) }}</span>
          <span v-else class="rank">{{ index + 1 }}</span>
        </div>
        
        <div class="team-info">
          <div class="team-name">{{ team.name }}</div>
          <div class="team-type" :class="team.type">{{ getTeamTypeText(team.type) }}</div>
        </div>
        
        <div class="team-stats">
          <div class="score">{{ team.score }}</div>
          <div class="score-change" :class="team.change > 0 ? 'positive' : 'negative'">
            {{ team.change > 0 ? '+' : '' }}{{ team.change }}
          </div>
        </div>
        
        <div class="progress-indicator">
          <div class="progress-bar">
            <div 
              class="progress-fill" 
              :style="{ width: getProgressWidth(team.score) + '%' }"
              :class="team.type">
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <div class="ranking-footer">
      <div class="update-time">
        更新时间: {{ formatTime(lastUpdate) }}
      </div>
      <div class="total-teams">
        共 {{ data.length }} 支队伍
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  data: {
    type: Array,
    default: () => [
      { id: 1, name: '红龙战队', type: 'red', score: 2850, change: 120 },
      { id: 2, name: '蓝盾防护', type: 'blue', score: 2720, change: 85 },
      { id: 3, name: '暗影突击', type: 'red', score: 2680, change: -45 },
      { id: 4, name: '守护者联盟', type: 'blue', score: 2590, change: 200 },
      { id: 5, name: '网络幽灵', type: 'red', score: 2480, change: 60 },
      { id: 6, name: '安全堡垒', type: 'blue', score: 2350, change: -30 },
      { id: 7, name: '黑客军团', type: 'red', score: 2280, change: 90 },
      { id: 8, name: '防火墙', type: 'blue', score: 2150, change: 40 }
    ]
  }
})

const lastUpdate = ref(new Date())

const sortedTeams = computed(() => {
  return [...props.data].sort((a, b) => b.score - a.score)
})

const maxScore = computed(() => {
  return Math.max(...props.data.map(team => team.score))
})

const getRankingClass = (index) => {
  if (index === 0) return 'first'
  if (index === 1) return 'second'
  if (index === 2) return 'third'
  return ''
}

const getMedal = (index) => {
  const medals = ['🥇', '🥈', '🥉']
  return medals[index]
}

const getTeamTypeText = (type) => {
  return type === 'red' ? '红队' : '蓝队'
}

const getProgressWidth = (score) => {
  return (score / maxScore.value) * 100
}

const formatTime = (time) => {
  return time.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}
</script>

<style scoped>
/* ============================================
   Apple Elegant White Style - Team Ranking
   ============================================ */

:root {
  --apple-text: #1d1d1f;
  --apple-text-secondary: #86868b;
  --apple-border: rgba(0, 0, 0, 0.04);
  --apple-blue: #007aff;
  --apple-red: #ff3b30;
  --apple-green: #34c759;
  --font-apple: -apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", "Helvetica Neue", sans-serif;
}

.team-ranking {
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

.ranking-type {
  font-size: 12px;
  font-weight: 500;
  color: var(--apple-text-secondary);
  padding: 4px 8px;
  background: rgba(0, 122, 255, 0.08);
  border-radius: 8px;
}

.ranking-list {
  flex: 1;
  overflow-y: auto;
  padding-right: 5px;
}

.ranking-list::-webkit-scrollbar {
  width: 4px;
}

.ranking-list::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.04);
  border-radius: 2px;
}

.ranking-list::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.15);
  border-radius: 2px;
  transition: background 0.2s ease;
}

.ranking-list::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.25);
}

.ranking-item {
  display: flex;
  align-items: center;
  padding: 12px 8px;
  margin-bottom: 8px;
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.95) 0%,
    rgba(250, 250, 250, 0.9) 100%);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-radius: 10px;
  border-left: 3px solid transparent;
  border: 0.5px solid var(--apple-border);
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.ranking-item:hover {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 1) 0%,
    rgba(248, 248, 248, 0.95) 100%);
  transform: translateX(2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.ranking-item.first {
  border-left-color: #ffd700;
  background: linear-gradient(135deg,
    rgba(255, 215, 0, 0.08) 0%,
    rgba(255, 215, 0, 0.04) 100%);
}

.ranking-item.second {
  border-left-color: #c0c0c0;
  background: linear-gradient(135deg,
    rgba(192, 192, 192, 0.08) 0%,
    rgba(192, 192, 192, 0.04) 100%);
}

.ranking-item.third {
  border-left-color: #cd7f32;
  background: linear-gradient(135deg,
    rgba(205, 127, 50, 0.08) 0%,
    rgba(205, 127, 50, 0.04) 100%);
}

.rank-number {
  width: 40px;
  text-align: center;
}

.medal {
  font-size: 20px;
}

.rank {
  font-size: 16px;
  font-weight: 700;
  color: var(--apple-text-secondary);
  letter-spacing: -0.3px;
}

.team-info {
  flex: 1;
  margin-left: 12px;
}

.team-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--apple-text);
  margin-bottom: 2px;
  letter-spacing: -0.2px;
}

.team-type {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 6px;
  border-radius: 8px;
  display: inline-block;
}

.team-type.red {
  background: rgba(255, 59, 48, 0.1);
  color: var(--apple-red);
}

.team-type.blue {
  background: rgba(0, 122, 255, 0.1);
  color: var(--apple-blue);
}

.team-stats {
  text-align: right;
  margin-right: 12px;
}

.score {
  font-size: 16px;
  font-weight: 700;
  color: var(--apple-text);
  letter-spacing: -0.3px;
}

.score-change {
  font-size: 11px;
  font-weight: 600;
  margin-top: 2px;
}

.score-change.positive {
  color: var(--apple-green);
}

.score-change.negative {
  color: var(--apple-red);
}

.progress-indicator {
  width: 60px;
}

.progress-bar {
  width: 100%;
  height: 4px;
  background: rgba(0, 0, 0, 0.06);
  border-radius: 2px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 2px;
  transition: width 0.5s ease;
}

.progress-fill.red {
  background: linear-gradient(90deg, var(--apple-red) 0%, #ff6b59 100%);
}

.progress-fill.blue {
  background: linear-gradient(90deg, var(--apple-blue) 0%, #5ac8fa 100%);
}

.ranking-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px solid var(--apple-border);
  font-size: 11px;
  font-weight: 500;
  color: var(--apple-text-secondary);
}
</style>