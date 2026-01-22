<template>
  <div class="redteam-bigscreen-container">
    <!-- 动态光晕背景层 -->
    <div class="dynamic-glow-layer">
      <div class="glow-spot glow-1"></div>
      <div class="glow-spot glow-2"></div>
      <div class="glow-spot glow-3"></div>
    </div>

    <!-- 标题区域 -->
    <div class="bigscreen-header">
      <h1 class="title">⚔️ CyberLab 红队攻击态势大屏展示</h1>
      <div class="header-info">
        <span class="time">{{ currentTime }}</span>
        <span class="version">v1.0.0</span>
        <div class="fullscreen-controls">
          <span v-if="!isFullscreen" class="fullscreen-tip">点击进入全屏</span>
          <button class="fullscreen-btn" @click="toggleFullscreen" title="切换全屏 (F11)">
            {{ isFullscreen ? '退出全屏' : '进入全屏' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="bigscreen-content">
      <!-- 第一行：总览指标卡片 -->
      <div class="row row-1">
        <div class="overview-cards">
          <div class="card" v-for="card in overviewCards" :key="card.label">
            <div class="card-icon">{{ card.icon }}</div>
            <div class="card-value">{{ card.value }}</div>
            <div class="card-label">{{ card.label }}</div>
          </div>
        </div>
      </div>

      <!-- 第二行：攻击成功率 + 漏洞利用统计 -->
      <div class="row row-2">
        <div class="chart-container">
          <AttackSuccessChart :data="attackSuccessData" />
        </div>
        <div class="chart-container">
          <ExploitStatistics :data="exploitData" />
        </div>
      </div>

      <!-- 第三行：目标系统状态 + 攻击路径分析 -->
      <div class="row row-3">
        <div class="chart-container">
          <TargetSystemStatus :data="targetSystemData" />
        </div>
        <div class="chart-container">
          <AttackPathAnalysis :data="attackPathData" />
        </div>
      </div>

      <!-- 第四行：战果统计 + 任务进度 -->
      <div class="row row-4">
        <div class="chart-container">
          <AttackAchievements :data="achievementsData" />
        </div>
        <div class="chart-container">
          <MissionProgress :data="missionData" />
        </div>
      </div>

      <!-- 第五行：实时排行榜 -->
      <div class="row row-5">
        <div class="chart-container ranking-container">
          <TeamRankingBoard :rangeId="1" :autoRefresh="true" :refreshInterval="30000" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import AttackSuccessChart from '@/components/bigscreen/red/AttackSuccessChart.vue'
import ExploitStatistics from '@/components/bigscreen/red/ExploitStatistics.vue'
import TargetSystemStatus from '@/components/bigscreen/red/TargetSystemStatus.vue'
import AttackPathAnalysis from '@/components/bigscreen/red/AttackPathAnalysis.vue'
import AttackAchievements from '@/components/bigscreen/red/AttackAchievements.vue'
import MissionProgress from '@/components/bigscreen/red/MissionProgress.vue'
import TeamRankingBoard from '@/components/TeamRankingBoard.vue'
import { getRedTeamBigScreenData } from '@/api/redTeam'
import { setBigScreenPageMeta } from '@/utils/pageTitle'

const currentTime = ref('')
const attackSuccessData = ref([])
const exploitData = ref([])
const targetSystemData = ref([])
const attackPathData = ref([])
const achievementsData = ref([])
const missionData = ref([])
const isFullscreen = ref(false)

let timer = null

const overviewCards = computed(() => [
  { icon: '🎯', label: '攻击目标', value: 12 },
  { icon: '✅', label: '成功渗透', value: 8 },
  { icon: '🔓', label: '获取权限', value: 6 },
  { icon: '📊', label: '成功率', value: '67%' },
  { icon: '⏱️', label: '平均用时', value: '12m' },
  { icon: '🏆', label: '得分', value: 2450 }
])

// 更新时间
const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

// 获取红队大屏数据
const fetchData = async () => {
  try {
    const response = await getRedTeamBigScreenData()
    
    let data
    if (response && response.data) {
      data = response.data
    } else if (response) {
      data = response
    } else {
      throw new Error('响应数据为空')
    }
    
    attackSuccessData.value = data.attackSuccess || []
    exploitData.value = data.exploits || []
    targetSystemData.value = data.targets || []
    attackPathData.value = data.paths || []
    achievementsData.value = data.achievements || []
    missionData.value = data.missions || []
    
  } catch (error) {
    setDefaultData()
  }
}

// 设置默认数据
const setDefaultData = () => {
  attackSuccessData.value = [
    { name: 'Web应用', success: 85, attempts: 100 },
    { name: '数据库', success: 60, attempts: 80 },
    { name: '主机系统', success: 45, attempts: 60 },
    { name: '网络设备', success: 70, attempts: 90 }
  ]
  
  exploitData.value = [
    { type: 'SQL注入', count: 45, success: 38 },
    { type: 'XSS', count: 32, success: 28 },
    { type: '文件上传', count: 18, success: 15 },
    { type: '命令执行', count: 12, success: 10 }
  ]
  
  targetSystemData.value = [
    { name: 'Web-01', status: 'compromised', services: 5 },
    { name: 'DB-01', status: 'partial', services: 3 },
    { name: 'App-01', status: 'scanning', services: 8 },
    { name: 'File-01', status: 'clean', services: 4 }
  ]
  
  attackPathData.value = []
  achievementsData.value = []
  missionData.value = []
}

// 启动定时器
const startTimer = () => {
  updateTime()
  fetchData()
  
  timer = setInterval(() => {
    updateTime()
    fetchData()
  }, 5000)
}

// 停止定时器
const stopTimer = () => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

// 进入全屏
const enterTrueFullscreen = async () => {
  try {
    const element = document.documentElement
    
    if (element.requestFullscreen) {
      await element.requestFullscreen({ navigationUI: 'hide' })
    } else if (element.webkitRequestFullscreen) {
      await element.webkitRequestFullscreen(Element.ALLOW_KEYBOARD_INPUT)
    } else if (element.mozRequestFullScreen) {
      await element.mozRequestFullScreen()
    } else if (element.msRequestFullscreen) {
      await element.msRequestFullscreen()
    }
    
  } catch (error) {
  }
}

// 退出全屏
const exitFullscreen = async () => {
  try {
    if (document.exitFullscreen) {
      await document.exitFullscreen()
    } else if (document.webkitExitFullscreen) {
      await document.webkitExitFullscreen()
    } else if (document.mozCancelFullScreen) {
      await document.mozCancelFullScreen()
    } else if (document.msExitFullscreen) {
      await document.msExitFullscreen()
    }
    
  } catch (error) {
  }
}

// 监听全屏状态变化
const handleFullscreenChange = () => {
  isFullscreen.value = !!(
    document.fullscreenElement ||
    document.webkitFullscreenElement ||
    document.mozFullScreenElement ||
    document.msFullscreenElement
  )
}

// 切换全屏状态
const toggleFullscreen = () => {
  if (isFullscreen.value) {
    exitFullscreen()
  } else {
    enterTrueFullscreen()
  }
}

// 监听键盘事件
const handleKeydown = (event) => {
  if (event.key === 'Escape' || event.keyCode === 27) {
    exitFullscreen()
  }
  if (event.key === 'F11' || event.keyCode === 122) {
    event.preventDefault()
    toggleFullscreen()
  }
}

onMounted(() => {
  // 设置红队大屏页面的标题和图标
  setBigScreenPageMeta('red')
  
  startTimer()
  
  // 添加全屏状态监听
  document.addEventListener('fullscreenchange', handleFullscreenChange)
  document.addEventListener('webkitfullscreenchange', handleFullscreenChange)
  document.addEventListener('mozfullscreenchange', handleFullscreenChange)
  document.addEventListener('MSFullscreenChange', handleFullscreenChange)
  
  // 添加键盘监听
  document.addEventListener('keydown', handleKeydown)
  
  // 初始化全屏状态
  handleFullscreenChange()
})

onUnmounted(() => {
  stopTimer()
  
  // 移除事件监听
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
  document.removeEventListener('webkitfullscreenchange', handleFullscreenChange)
  document.removeEventListener('mozfullscreenchange', handleFullscreenChange)
  document.removeEventListener('MSFullscreenChange', handleFullscreenChange)
  document.removeEventListener('keydown', handleKeydown)
  
  // 退出全屏模式
  exitFullscreen()
})
</script>

<style scoped>
/* ============================================
   红队驾驶舱 - 深色科技风 + Apple优雅
   ============================================ */

:root {
  --hacker-bg: #0a0a0a;
  --hacker-bg-secondary: #1a0d0d;
  --hacker-red: #ff3b30;
  --hacker-red-glow: rgba(255, 59, 48, 0.3);
  --hacker-text: #ffffff;
  --hacker-text-secondary: rgba(255, 255, 255, 0.7);
  --hacker-glass: rgba(20, 20, 20, 0.6);
  --font-apple: -apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", "Helvetica Neue", sans-serif;
  --font-mono: "SF Mono", Consolas, Monaco, monospace;
}

.redteam-bigscreen-container {
  width: 100vw;
  height: 100vh;
  background: linear-gradient(135deg, #0a0a0a 0%, #1a0d0d 50%, #0f0f0f 100%);
  color: var(--hacker-text);
  font-family: var(--font-apple);
  overflow-y: auto;
  overflow-x: hidden;
  position: relative;
  z-index: 9999;
  scrollbar-width: thin;
  scrollbar-color: var(--hacker-red-glow) transparent;
}

/* Apple风格滚动条 */
.redteam-bigscreen-container::-webkit-scrollbar {
  width: 6px;
}

.redteam-bigscreen-container::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 3px;
}

.redteam-bigscreen-container::-webkit-scrollbar-thumb {
  background: var(--hacker-red-glow);
  border-radius: 3px;
  transition: background 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

.redteam-bigscreen-container::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 59, 48, 0.5);
}

/* 旋转雷达扫描线 - ::before */
.redteam-bigscreen-container::before {
  content: '';
  position: absolute;
  top: 10%;
  right: 15%;
  width: 50vw;
  height: 50vw;
  max-width: 800px;
  max-height: 800px;
  background: conic-gradient(
    from 0deg at 50% 50%,
    transparent 0deg,
    transparent 60deg,
    rgba(255, 59, 48, 0.35) 90deg,
    rgba(255, 59, 48, 0.15) 120deg,
    transparent 150deg,
    transparent 360deg
  );
  border-radius: 50%;
  pointer-events: none;
  animation: radar-scan 6s linear infinite;
  z-index: 0;
  filter: blur(2px);
}

/* 脉冲波纹 - ::after */
.redteam-bigscreen-container::after {
  content: '';
  position: absolute;
  bottom: 10%;
  left: 10%;
  width: 40vw;
  height: 40vw;
  max-width: 600px;
  max-height: 600px;
  background: radial-gradient(
    circle at center,
    transparent 40%,
    rgba(204, 0, 0, 0.3) 45%,
    rgba(204, 0, 0, 0.15) 50%,
    transparent 55%
  );
  border-radius: 50%;
  pointer-events: none;
  animation: pulse-wave 4s ease-out infinite;
  z-index: 0;
}

/* 动态光晕背景层 */
.dynamic-glow-layer {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
}

.glow-spot {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0.4;
}

.glow-1 {
  top: 20%;
  left: 30%;
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(255, 59, 48, 0.25) 0%, transparent 70%);
  animation: glow-breath-1 7s ease-in-out infinite;
}

.glow-2 {
  bottom: 25%;
  right: 20%;
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(204, 0, 0, 0.2) 0%, transparent 70%);
  animation: glow-breath-2 9s ease-in-out infinite;
  animation-delay: 2s;
}

.glow-3 {
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 250px;
  height: 250px;
  background: radial-gradient(circle, rgba(255, 59, 48, 0.15) 0%, transparent 70%);
  animation: glow-breath-3 5s ease-in-out infinite;
  animation-delay: 1s;
}

/* 雷达扫描旋转动画 */
@keyframes radar-scan {
  0% {
    transform: rotate(0deg);
    opacity: 0.8;
  }
  50% {
    opacity: 1;
  }
  100% {
    transform: rotate(360deg);
    opacity: 0.8;
  }
}

/* 脉冲波纹扩散动画 */
@keyframes pulse-wave {
  0% {
    transform: scale(0.8);
    opacity: 0;
  }
  50% {
    opacity: 0.6;
  }
  100% {
    transform: scale(1.5);
    opacity: 0;
  }
}

/* 光晕呼吸动画 */
@keyframes glow-breath-1 {
  0%, 100% {
    opacity: 0.3;
    transform: scale(1);
  }
  50% {
    opacity: 0.6;
    transform: scale(1.2);
  }
}

@keyframes glow-breath-2 {
  0%, 100% {
    opacity: 0.25;
    transform: scale(1);
  }
  50% {
    opacity: 0.5;
    transform: scale(1.15);
  }
}

@keyframes glow-breath-3 {
  0%, 100% {
    opacity: 0.2;
    transform: translate(-50%, -50%) scale(1);
  }
  50% {
    opacity: 0.45;
    transform: translate(-50%, -50%) scale(1.3);
  }
}

/* Header - Apple优雅毛玻璃效果 */
.bigscreen-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 40px;
  background: var(--hacker-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(255, 59, 48, 0.1);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
  position: relative;
  z-index: 10;
}

.title {
  font-size: 32px;
  font-weight: 700;
  margin: 0;
  background: linear-gradient(135deg, #ff3b30 0%, #ff6b59 100%);
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  letter-spacing: -0.5px;
  text-shadow: 0 0 30px var(--hacker-red-glow);
  filter: drop-shadow(0 0 20px var(--hacker-red-glow));
}

.header-info {
  display: flex;
  gap: 30px;
  font-size: 14px;
  font-weight: 500;
  color: #ffffff;
  align-items: center;
  font-family: var(--font-mono);
}

/* 全屏控制 - Apple精致按钮 */
.fullscreen-controls {
  display: flex;
  align-items: center;
  gap: 10px;
}

.fullscreen-tip {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.9);
  animation: pulse 2s cubic-bezier(0.19, 1, 0.22, 1) infinite;
  font-family: var(--font-mono);
}

.fullscreen-btn {
  background: linear-gradient(135deg, rgba(255, 59, 48, 0.15) 0%, rgba(255, 59, 48, 0.25) 100%);
  border: 0.5px solid rgba(255, 59, 48, 0.3);
  color: #ffffff;
  padding: 8px 16px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  font-family: var(--font-apple);
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
  white-space: nowrap;
  box-shadow: 0 2px 8px rgba(255, 59, 48, 0.1);
}

.fullscreen-btn:hover {
  background: linear-gradient(135deg, rgba(255, 59, 48, 0.25) 0%, rgba(255, 59, 48, 0.35) 100%);
  border-color: rgba(255, 59, 48, 0.5);
  color: #ffffff;
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(255, 59, 48, 0.3);
}

.fullscreen-btn:active {
  transform: translateY(0);
}

@keyframes pulse {
  0%, 100% {
    opacity: 0.5;
  }
  50% {
    opacity: 1;
  }
}

/* 内容区域 */
.bigscreen-content {
  padding: 20px;
  min-height: calc(100vh - 100px);
  display: flex;
  flex-direction: column;
  gap: 20px;
  position: relative;
  z-index: 1;
}

.row {
  display: flex;
  gap: 20px;
}

.row-1 {
  min-height: 120px;
}

.row-2, .row-3, .row-4 {
  min-height: 300px;
  flex: 1;
}

.row-5 {
  min-height: 350px;
}

.ranking-container {
  width: 100%;
}

/* 总览卡片 - Apple毛玻璃+霓虹边框 */
.overview-cards {
  display: flex;
  gap: 20px;
  width: 100%;
}

.card {
  flex: 1;
  background: var(--hacker-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 14px;
  border: 0.5px solid rgba(255, 59, 48, 0.15);
  padding: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3), inset 0 0 0 0.5px rgba(255, 255, 255, 0.05);
  position: relative;
  overflow: hidden;
}

.card::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(255, 59, 48, 0.05) 0%, transparent 70%);
  opacity: 0;
  transition: opacity 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

.card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 32px rgba(255, 59, 48, 0.2), 0 0 20px rgba(255, 59, 48, 0.1);
  border-color: rgba(255, 59, 48, 0.3);
}

.card:hover::before {
  opacity: 1;
}

.card-icon {
  font-size: 32px;
  margin-bottom: 10px;
  filter: drop-shadow(0 0 10px rgba(255, 59, 48, 0.3));
}

.card-value {
  font-size: 28px;
  font-weight: 700;
  color: #ffffff;
  margin-bottom: 6px;
  font-family: var(--font-mono);
  letter-spacing: -0.5px;
  text-shadow: 0 0 20px var(--hacker-red-glow);
}

.card-label {
  font-size: 13px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.6);
  letter-spacing: 0.3px;
}

/* 图表容器 - 深色毛玻璃 */
.chart-container {
  flex: 1;
  background: var(--hacker-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 16px;
  border: 0.5px solid rgba(255, 255, 255, 0.08);
  padding: 20px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4), inset 0 0 0 0.5px rgba(255, 255, 255, 0.05);
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
  position: relative;
  overflow: hidden;
}

.chart-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 59, 48, 0.03), transparent);
  transition: left 0.6s cubic-bezier(0.19, 1, 0.22, 1);
}

.chart-container:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.5), 0 0 30px rgba(255, 59, 48, 0.1);
  border-color: rgba(255, 59, 48, 0.2);
}

.chart-container:hover::before {
  left: 100%;
}
</style>