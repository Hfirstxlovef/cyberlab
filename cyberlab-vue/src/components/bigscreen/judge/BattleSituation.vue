<template>
  <div class="battle-situation">
    <h3 class="chart-title">实时对抗态势</h3>
    <div class="battle-content">
      <div class="team-status red-team">
        <h4>🔴 红队</h4>
        <div class="status-item">
          <span class="label">当前得分:</span>
          <span class="value">{{ redTeamScore }}</span>
        </div>
        <div class="status-item">
          <span class="label">攻击次数:</span>
          <span class="value">{{ redTeamAttacks }}</span>
        </div>
        <div class="status-item">
          <span class="label">成功率:</span>
          <span class="value">{{ redTeamSuccessRate }}%</span>
        </div>
        <div class="progress-bar">
          <div class="progress-fill red" :style="{width: `${redTeamProgress}%`}"></div>
        </div>
      </div>
      
      <div class="vs-divider">
        <span class="vs-text">VS</span>
        <div class="battle-time">{{ battleTime }}</div>
      </div>
      
      <div class="team-status blue-team">
        <h4>🔵 蓝队</h4>
        <div class="status-item">
          <span class="label">当前得分:</span>
          <span class="value">{{ blueTeamScore }}</span>
        </div>
        <div class="status-item">
          <span class="label">防御次数:</span>
          <span class="value">{{ blueTeamDefenses }}</span>
        </div>
        <div class="status-item">
          <span class="label">防御率:</span>
          <span class="value">{{ blueTeamDefenseRate }}%</span>
        </div>
        <div class="progress-bar">
          <div class="progress-fill blue" :style="{width: `${blueTeamProgress}%`}"></div>
        </div>
      </div>
    </div>
    
    <div class="battle-stats">
      <div class="stat-card">
        <span class="stat-label">总攻防次数</span>
        <span class="stat-value">{{ totalEvents }}</span>
      </div>
      <div class="stat-card">
        <span class="stat-label">演练时长</span>
        <span class="stat-value">{{ drillDuration }}</span>
      </div>
      <div class="stat-card">
        <span class="stat-label">当前阶段</span>
        <span class="stat-value">{{ currentPhase }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  data: {
    type: Object,
    default: () => ({})
  }
})

const redTeamScore = ref(850)
const blueTeamScore = ref(720)
const redTeamAttacks = ref(156)
const blueTeamDefenses = ref(132)
const battleTime = ref('02:45:30')
const totalEvents = ref(288)
const drillDuration = ref('2h 45m')
const currentPhase = ref('激烈对抗')

const redTeamSuccessRate = computed(() => 78)
const blueTeamDefenseRate = computed(() => 65)
const redTeamProgress = computed(() => (redTeamScore.value / 1000) * 100)
const blueTeamProgress = computed(() => (blueTeamScore.value / 1000) * 100)

let timer = null

const updateTime = () => {
  const now = new Date()
  const hours = String(now.getHours()).padStart(2, '0')
  const minutes = String(now.getMinutes()).padStart(2, '0')
  const seconds = String(now.getSeconds()).padStart(2, '0')
  battleTime.value = `${hours}:${minutes}:${seconds}`
}

onMounted(() => {
  updateTime()
  timer = setInterval(updateTime, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.battle-situation {
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

.battle-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  margin-bottom: 20px;
}

.team-status {
  flex: 1;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 10px;
  padding: 15px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.red-team {
  border-color: rgba(244, 67, 54, 0.3);
  background: rgba(244, 67, 54, 0.05);
}

.blue-team {
  border-color: rgba(33, 150, 243, 0.3);
  background: rgba(33, 150, 243, 0.05);
}

.team-status h4 {
  margin: 0 0 12px 0;
  font-size: 16px;
  color: #fff;
}

.status-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 13px;
}

.label {
  color: rgba(255, 255, 255, 0.6);
}

.value {
  color: #fff;
  font-weight: bold;
}

.progress-bar {
  height: 8px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 4px;
  overflow: hidden;
  margin-top: 10px;
}

.progress-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.3s ease;
}

.progress-fill.red {
  background: linear-gradient(90deg, #f44336, #e91e63);
}

.progress-fill.blue {
  background: linear-gradient(90deg, #2196f3, #03a9f4);
}

.vs-divider {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 0 20px;
}

.vs-text {
  font-size: 24px;
  font-weight: bold;
  color: #9c27b0;
  text-shadow: 0 0 10px rgba(156, 39, 176, 0.5);
  margin-bottom: 8px;
}

.battle-time {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
  font-family: monospace;
}

.battle-stats {
  display: flex;
  gap: 15px;
}

.stat-card {
  flex: 1;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  padding: 12px;
  text-align: center;
  border: 1px solid rgba(156, 39, 176, 0.2);
}

.stat-label {
  display: block;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  margin-bottom: 6px;
}

.stat-value {
  display: block;
  font-size: 18px;
  font-weight: bold;
  color: #9c27b0;
}
</style>