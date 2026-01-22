<template>
  <div class="mission-progress">
    <h3 class="chart-title">任务进度</h3>
    <div class="missions-list">
      <div class="mission-item" v-for="mission in missions" :key="mission.id">
        <div class="mission-header">
          <span class="mission-name">{{ mission.name }}</span>
          <span class="mission-points">{{ mission.points }} pts</span>
        </div>
        <div class="mission-progress-bar">
          <div 
            class="progress-fill" 
            :style="{width: `${mission.progress}%`}"
            :class="`progress-${getProgressColor(mission.progress)}`"
          ></div>
        </div>
        <div class="mission-footer">
          <span class="mission-status">{{ getStatusText(mission.status) }}</span>
          <span class="mission-percentage">{{ mission.progress }}%</span>
        </div>
      </div>
    </div>
    <div class="overall-progress">
      <h4>总体完成度</h4>
      <div class="overall-bar">
        <div class="overall-fill" :style="{width: `${overallProgress}%`}"></div>
      </div>
      <div class="overall-stats">
        <span>{{ completedMissions }}/{{ totalMissions }} 任务</span>
        <span>{{ overallProgress }}%</span>
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

const missions = computed(() => props.data.length > 0 ? props.data : [
  {
    id: 1,
    name: '信息收集',
    status: 'completed',
    progress: 100,
    points: 50
  },
  {
    id: 2,
    name: '漏洞扫描',
    status: 'completed',
    progress: 100,
    points: 100
  },
  {
    id: 3,
    name: '获取Web权限',
    status: 'completed',
    progress: 100,
    points: 200
  },
  {
    id: 4,
    name: '权限提升',
    status: 'in_progress',
    progress: 65,
    points: 300
  },
  {
    id: 5,
    name: '横向移动',
    status: 'in_progress',
    progress: 30,
    points: 400
  },
  {
    id: 6,
    name: '获取核心数据',
    status: 'pending',
    progress: 0,
    points: 500
  }
])

const completedMissions = computed(() => 
  missions.value.filter(m => m.status === 'completed').length
)

const totalMissions = computed(() => missions.value.length)

const overallProgress = computed(() => {
  if (missions.value.length === 0) return 0
  const total = missions.value.reduce((sum, m) => sum + m.progress, 0)
  return Math.round(total / missions.value.length)
})

const getProgressColor = (progress) => {
  if (progress === 100) return 'success'
  if (progress >= 60) return 'warning'
  if (progress > 0) return 'info'
  return 'default'
}

const getStatusText = (status) => {
  const statusMap = {
    'completed': '已完成',
    'in_progress': '进行中',
    'pending': '待开始',
    'failed': '失败'
  }
  return statusMap[status] || '未知'
}
</script>

<style scoped>
/* ============================================
   任务进度 - 深色科技风 + Apple优雅
   ============================================ */

.mission-progress {
  height: 100%;
  display: flex;
  flex-direction: column;
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", "Helvetica Neue", sans-serif;
}

.chart-title {
  margin: 0 0 20px 0;
  font-size: 18px;
  font-weight: 700;
  color: #007aff;
  letter-spacing: -0.3px;
  text-shadow: 0 0 20px rgba(0, 122, 255, 0.4);
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
  background: linear-gradient(90deg, #007aff 0%, transparent 100%);
  box-shadow: 0 0 10px rgba(0, 122, 255, 0.5);
}

.missions-list {
  flex: 1;
  overflow-y: auto;
  margin-bottom: 15px;
  padding-right: 5px;
}

.missions-list::-webkit-scrollbar {
  width: 4px;
}

.missions-list::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 2px;
}

.missions-list::-webkit-scrollbar-thumb {
  background: rgba(0, 122, 255, 0.3);
  border-radius: 2px;
  transition: background 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

.missions-list::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 122, 255, 0.5);
}

.mission-item {
  background: rgba(20, 20, 20, 0.5);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-radius: 12px;
  padding: 14px;
  margin-bottom: 10px;
  border: 0.5px solid rgba(255, 255, 255, 0.08);
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  position: relative;
}

.mission-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 3px;
  height: 100%;
  background: linear-gradient(180deg, rgba(0, 122, 255, 0.6) 0%, rgba(0, 122, 255, 0.2) 100%);
  opacity: 0;
  transition: opacity 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

.mission-item:hover {
  transform: translateX(6px);
  border-color: rgba(0, 122, 255, 0.3);
  background: rgba(30, 30, 30, 0.6);
  box-shadow: 0 4px 16px rgba(0, 122, 255, 0.15);
}

.mission-item:hover::before {
  opacity: 1;
}

.mission-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.mission-name {
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;
  letter-spacing: -0.2px;
}

.mission-points {
  font-size: 11px;
  font-weight: 700;
  color: #ffcc00;
  font-family: "SF Mono", Consolas, monospace;
  letter-spacing: 0.5px;
  text-shadow: 0 0 10px rgba(255, 204, 0, 0.3);
}

.mission-progress-bar {
  height: 6px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 10px;
  box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.3);
}

.progress-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.5s cubic-bezier(0.19, 1, 0.22, 1);
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

.progress-success {
  background: linear-gradient(90deg, #34c759 0%, #65d872 100%);
  box-shadow: 0 0 10px rgba(52, 199, 89, 0.5);
}

.progress-warning {
  background: linear-gradient(90deg, #ff9500 0%, #ffad33 100%);
  box-shadow: 0 0 10px rgba(255, 149, 0, 0.5);
}

.progress-info {
  background: linear-gradient(90deg, #007aff 0%, #3395ff 100%);
  box-shadow: 0 0 10px rgba(0, 122, 255, 0.5);
}

.progress-default {
  background: linear-gradient(90deg, #8e8e93 0%, #aeaeb2 100%);
  box-shadow: 0 0 10px rgba(142, 142, 147, 0.3);
}

.mission-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 11px;
}

.mission-status {
  color: rgba(255, 255, 255, 0.6);
  font-weight: 500;
}

.mission-percentage {
  color: rgba(255, 255, 255, 0.8);
  font-weight: 600;
  font-family: "SF Mono", Consolas, monospace;
}

.overall-progress {
  background: rgba(20, 20, 20, 0.6);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-radius: 12px;
  padding: 14px;
  border: 0.5px solid rgba(0, 122, 255, 0.3);
  box-shadow: 0 4px 16px rgba(0, 122, 255, 0.15);
}

.overall-progress h4 {
  margin: 0 0 10px 0;
  font-size: 13px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  letter-spacing: -0.2px;
}

.overall-bar {
  height: 10px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 5px;
  overflow: hidden;
  margin-bottom: 10px;
  box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.3);
}

.overall-fill {
  height: 100%;
  background: linear-gradient(90deg, #007aff 0%, #5ac8fa 100%);
  border-radius: 5px;
  transition: width 0.5s cubic-bezier(0.19, 1, 0.22, 1);
  box-shadow: 0 0 15px rgba(0, 122, 255, 0.6);
  position: relative;
}

.overall-fill::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(90deg, transparent 0%, rgba(255, 255, 255, 0.3) 50%, transparent 100%);
  animation: shimmer 2s infinite;
}

.overall-stats {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.8);
  font-family: "SF Mono", Consolas, monospace;
}
</style>