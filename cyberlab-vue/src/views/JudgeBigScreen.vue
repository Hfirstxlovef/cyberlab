<template>
  <div class="judge-bigscreen-container">
    <!-- 标题区域 -->
    <div class="bigscreen-header">
      <h1 class="title">⚖️ CyberLab 裁判对抗态势大屏展示</h1>
      <div class="header-info">
        <span class="time">{{ currentTime }}</span>
        <span class="match-status">
          <span class="status-dot" :class="matchStatusClass"></span>
          {{ matchStatusText }}
        </span>
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
      <!-- 第一行：比赛总览 -->
      <div class="row row-1">
        <div class="match-overview">
          <div class="team-score red-team">
            <div class="team-name">红队</div>
            <div class="team-points">{{ redTeamScore }}</div>
            <div class="team-trend">{{ redTeamTrend }}</div>
          </div>
          <div class="match-timer">
            <div class="timer-label">比赛时间</div>
            <div class="timer-value">{{ matchTime }}</div>
            <div class="timer-progress">
              <div class="progress-fill" :style="{ width: matchProgress + '%' }"></div>
            </div>
          </div>
          <div class="team-score blue-team">
            <div class="team-name">蓝队</div>
            <div class="team-points">{{ blueTeamScore }}</div>
            <div class="team-trend">{{ blueTeamTrend }}</div>
          </div>
        </div>
      </div>

      <!-- 第二行：对抗态势图 + 得分趋势 -->
      <div class="row row-2">
        <div class="chart-container">
          <BattleSituation :data="battleData" />
        </div>
        <div class="chart-container">
          <ScoreTrend :redData="redScoreTrend" :blueData="blueScoreTrend" />
        </div>
      </div>

      <!-- 第三行：攻防事件时间线 + 系统状态监控 -->
      <div class="row row-3">
        <div class="chart-container">
          <EventTimeline :events="eventData" />
        </div>
        <div class="chart-container">
          <SystemMonitor :systems="systemData" />
        </div>
      </div>

      <!-- 第四行：队伍表现统计 + 已通过报告展示 -->
      <div class="row row-4">
        <div class="chart-container">
          <TeamPerformance :redStats="redPerformance" :blueStats="bluePerformance" />
        </div>
        <div class="chart-container approved-reports-container">
          <h3>📋 最新通过的报告</h3>
          <div class="reports-list">
            <div v-for="report in approvedReports" :key="report.id" class="report-card">
              <div class="report-header">
                <span class="team-badge" :class="report.type">{{ report.teamName }}</span>
                <span class="approve-time">{{ formatTime(report.reviewTime) }}</span>
              </div>
              <div class="report-content">
                <p class="report-target"><strong>目标：</strong>{{ report.targetName }}</p>
                <p class="report-method"><strong>方法：</strong>{{ report.attackMethod }}</p>
                <!-- 录屏缩略图 -->
                <div v-if="report.recordings && report.recordings.length > 0" class="video-thumbnails">
                  <div v-for="(video, index) in report.recordings.slice(0, 2)" :key="index" class="video-thumb" @click="playReportVideo(video)">
                    <el-icon size="24"><VideoPlay /></el-icon>
                    <span class="video-time">{{ video.startTime }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
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
import { VideoPlay } from '@element-plus/icons-vue'
import BattleSituation from '@/components/bigscreen/judge/BattleSituation.vue'
import ScoreTrend from '@/components/bigscreen/judge/ScoreTrend.vue'
import EventTimeline from '@/components/bigscreen/judge/EventTimeline.vue'
import SystemMonitor from '@/components/bigscreen/judge/SystemMonitor.vue'
import TeamPerformance from '@/components/bigscreen/judge/TeamPerformance.vue'
import ViolationRecords from '@/components/bigscreen/judge/ViolationRecords.vue'
import TeamRankingBoard from '@/components/TeamRankingBoard.vue'
import { getJudgeBigScreenData } from '@/api/judge'
import { setBigScreenPageMeta } from '@/utils/pageTitle'
import axios from '@/api/axios'

const currentTime = ref('')
const matchTime = ref('01:23:45')
const matchProgress = ref(55)
const redTeamScore = ref(1250)
const blueTeamScore = ref(1180)
const redTeamTrend = ref('↑ +120')
const blueTeamTrend = ref('↑ +85')
const battleData = ref([])
const redScoreTrend = ref([])
const blueScoreTrend = ref([])
const eventData = ref([])
const systemData = ref([])
const redPerformance = ref({})
const bluePerformance = ref({})
const violationData = ref([])
const approvedReports = ref([])
const isFullscreen = ref(false)
const matchStatus = ref('ongoing') // ongoing, paused, finished

let timer = null
let reportTimer = null

const matchStatusClass = computed(() => {
  const statusMap = {
    ongoing: 'status-ongoing',
    paused: 'status-paused',
    finished: 'status-finished'
  }
  return statusMap[matchStatus.value] || 'status-ongoing'
})

const matchStatusText = computed(() => {
  const statusMap = {
    ongoing: '比赛进行中',
    paused: '比赛暂停',
    finished: '比赛结束'
  }
  return statusMap[matchStatus.value] || '比赛进行中'
})

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

// 更新比赛时间
const updateMatchTime = () => {
  // 模拟比赛时间增长
  const parts = matchTime.value.split(':')
  let hours = parseInt(parts[0])
  let minutes = parseInt(parts[1])
  let seconds = parseInt(parts[2])
  
  seconds++
  if (seconds >= 60) {
    seconds = 0
    minutes++
    if (minutes >= 60) {
      minutes = 0
      hours++
    }
  }
  
  matchTime.value = `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`
  
  // 更新进度条
  const totalSeconds = hours * 3600 + minutes * 60 + seconds
  matchProgress.value = Math.min((totalSeconds / 7200) * 100, 100) // 假设比赛时长2小时
}

// 获取裁判大屏数据
const fetchData = async () => {
  try {
    const response = await getJudgeBigScreenData()
    
    let data
    if (response && response.data) {
      data = response.data
    } else if (response) {
      data = response
    } else {
      throw new Error('响应数据为空')
    }
    
    // 更新数据
    if (data.scores) {
      redTeamScore.value = data.scores.red || 1250
      blueTeamScore.value = data.scores.blue || 1180
    }
    
    battleData.value = data.battle || []
    redScoreTrend.value = data.redTrend || []
    blueScoreTrend.value = data.blueTrend || []
    eventData.value = data.events || []
    systemData.value = data.systems || []
    redPerformance.value = data.redPerformance || {}
    bluePerformance.value = data.bluePerformance || {}
    violationData.value = data.violations || []
    
  } catch (error) {
    setDefaultData()
  }
}

// 设置默认数据
const setDefaultData = () => {
  battleData.value = []
  redScoreTrend.value = [800, 900, 1000, 1100, 1150, 1250]
  blueScoreTrend.value = [850, 920, 980, 1050, 1120, 1180]
  
  eventData.value = [
    { time: '00:15:23', team: 'red', event: '成功获取Web服务权限', score: '+50' },
    { time: '00:18:45', team: 'blue', event: '检测并阻止SQL注入攻击', score: '+30' },
    { time: '00:22:10', team: 'red', event: '发现并利用文件上传漏洞', score: '+80' },
    { time: '00:25:30', team: 'blue', event: '修复关键系统漏洞', score: '+60' }
  ]
  
  systemData.value = [
    { name: 'Web服务器', status: 'compromised', owner: 'red' },
    { name: '数据库服务器', status: 'defended', owner: 'blue' },
    { name: '应用服务器', status: 'contested', owner: 'neutral' },
    { name: '文件服务器', status: 'normal', owner: 'blue' }
  ]
  
  redPerformance.value = {
    attacks: 45,
    successes: 28,
    rate: 62
  }
  
  bluePerformance.value = {
    defenses: 38,
    blocks: 30,
    rate: 79
  }
  
  violationData.value = []
}

// 启动定时器
const startTimer = () => {
  updateTime()
  fetchData()
  fetchApprovedReports()

  timer = setInterval(() => {
    updateTime()
    if (matchStatus.value === 'ongoing') {
      updateMatchTime()
    }
    fetchData()
  }, 1000)

  // 每30秒更新一次已通过的报告
  reportTimer = setInterval(() => {
    fetchApprovedReports()
  }, 30000)
}

// 停止定时器
const stopTimer = () => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
  if (reportTimer) {
    clearInterval(reportTimer)
    reportTimer = null
  }
}

// 获取已通过的报告
const fetchApprovedReports = async () => {
  try {
    const { data } = await axios.get('/api/achievements/approved')

    // 解析报告数据并关联录屏
    const reports = await Promise.all(data.slice(0, 5).map(async (achievement) => {
      let reportData = {}
      try {
        reportData = achievement.attackReportJson ? JSON.parse(achievement.attackReportJson) : {}
      } catch (e) {
        console.error('解析报告JSON失败:', e)
      }

      // 获取关联的录屏
      let recordings = []
      if (achievement.relatedRecordingIds) {
        try {
          const recordingIds = JSON.parse(achievement.relatedRecordingIds)
          if (recordingIds && recordingIds.length > 0) {
            const recordingPromises = recordingIds.map(id =>
              axios.get(`/api/screen-recording/${id}`).catch(() => null)
            )
            const recordingResults = await Promise.all(recordingPromises)
            recordings = recordingResults
              .filter(res => res && res.data)
              .map(res => res.data)
          }
        } catch (e) {
          console.error('获取关联录屏失败:', e)
        }
      }

      return {
        id: achievement.id,
        teamName: achievement.teamName,
        targetName: achievement.targetName || reportData.targetName || '-',
        attackMethod: achievement.attackMethod || reportData.attackMethod || '-',
        reviewTime: achievement.reviewTime,
        type: 'red', // 目前只有攻击报告
        recordings: recordings
      }
    }))

    approvedReports.value = reports
  } catch (error) {
    console.error('获取已通过报告失败:', error)
  }
}

// 播放报告相关的录屏
const playReportVideo = (video) => {
  if (!video || !video.id) return
  window.open(`/admin/screen-recordings?videoId=${video.id}`, '_blank')
}

// 格式化时间
const formatTime = (dateTime) => {
  if (!dateTime) return '-'
  const date = new Date(dateTime)
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
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
  // 设置裁判大屏页面的标题和图标
  setBigScreenPageMeta('judge')
  
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
   Apple Elegant White Style - 裁判大屏幕
   Judge BigScreen Dashboard
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
  --radius-lg: 20px;
  --shadow-card: 0 4px 16px rgba(0, 0, 0, 0.06);
  --shadow-card-hover: 0 8px 32px rgba(0, 0, 0, 0.12);
  --spacing-xs: 8px;
  --spacing-sm: 12px;
  --spacing-md: 16px;
  --spacing-lg: 24px;
  --spacing-xl: 32px;
  --font-apple: -apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", "Helvetica Neue", sans-serif;
}

.judge-bigscreen-container {
  width: 100vw;
  min-height: 100vh;
  background: linear-gradient(135deg,
    rgba(251, 251, 253, 1) 0%,
    rgba(245, 245, 247, 0.98) 50%,
    rgba(248, 248, 250, 1) 100%);
  color: var(--apple-text);
  font-family: var(--font-apple);
  overflow-y: auto !important;
  overflow-x: hidden !important;
  position: relative;
  z-index: 9999;
  will-change: transform, opacity;
  contain: layout style paint;
}

/* 全屏状态下的样式 */
.judge-bigscreen-container:fullscreen,
.judge-bigscreen-container:-webkit-full-screen,
.judge-bigscreen-container:-moz-full-screen,
.judge-bigscreen-container:-ms-fullscreen {
  width: 100vw;
  height: 100vh;
  margin: 0;
  padding: 0;
}

/* 精致的背景纹理 */
.judge-bigscreen-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background:
    radial-gradient(circle at 20% 30%, rgba(0, 122, 255, 0.03) 0%, transparent 50%),
    radial-gradient(circle at 80% 60%, rgba(52, 199, 89, 0.03) 0%, transparent 50%),
    radial-gradient(circle at 50% 80%, rgba(175, 82, 222, 0.02) 0%, transparent 50%);
  pointer-events: none;
}

/* ============================================
   Header Styling
   ============================================ */
.bigscreen-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-lg) var(--spacing-xl);
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.95) 0%,
    rgba(250, 250, 250, 0.9) 100%);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid var(--apple-border);
  box-shadow: 0 2px 16px rgba(0, 0, 0, 0.04);
}

.title {
  font-size: 32px;
  font-weight: 700;
  margin: 0;
  background: linear-gradient(135deg, var(--apple-blue) 0%, #0051d5 100%);
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  letter-spacing: -0.5px;
}

.header-info {
  display: flex;
  gap: var(--spacing-lg);
  font-size: 15px;
  color: var(--apple-text-secondary);
  align-items: center;
  font-weight: 500;
}

.time {
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.3px;
}

.match-status {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  animation: blink 2s infinite;
}

.status-dot.status-ongoing {
  background: var(--apple-green);
}

.status-dot.status-paused {
  background: var(--apple-orange);
}

.status-dot.status-finished {
  background: var(--apple-text-secondary);
  animation: none;
}

@keyframes blink {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.3;
  }
}

.version {
  padding: 4px 12px;
  background: rgba(0, 122, 255, 0.08);
  color: var(--apple-blue);
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
}

.fullscreen-controls {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.fullscreen-tip {
  font-size: 12px;
  color: var(--apple-text-secondary);
  animation: pulse 2s ease-in-out infinite;
}

.fullscreen-btn {
  background: linear-gradient(135deg, var(--apple-blue) 0%, #0051d5 100%);
  border: none;
  color: #ffffff;
  padding: 10px 20px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  font-family: var(--font-apple);
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
  white-space: nowrap;
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.25);
}

.fullscreen-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 122, 255, 0.35);
}

@keyframes pulse {
  0%, 100% {
    opacity: 0.5;
  }
  50% {
    opacity: 1;
  }
}

/* ============================================
   Content Area
   ============================================ */
.bigscreen-content {
  padding: var(--spacing-lg);
  min-height: calc(100vh - 100px);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.row {
  display: flex;
  gap: var(--spacing-lg);
}

.row-1 {
  min-height: 120px;
}

.row-2, .row-3, .row-4 {
  min-height: 300px;
  flex: 0 0 auto;
}

.row-5 {
  min-height: 400px;
  flex: 0 0 auto;
}

.ranking-container {
  width: 100%;
}

.achievement-feed-container {
  width: 100%;
}

/* ============================================
   Match Overview Section
   ============================================ */
.match-overview {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding: 20px;
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.98) 0%,
    rgba(250, 250, 250, 0.95) 100%);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: var(--radius-lg);
  border: 0.5px solid var(--apple-border);
  box-shadow: var(--shadow-card);
}

.team-score {
  flex: 1;
  text-align: center;
  padding: 20px;
}

.team-score.red-team {
  border-right: 1px solid rgba(0, 0, 0, 0.06);
}

.team-score.blue-team {
  border-left: 1px solid rgba(0, 0, 0, 0.06);
}

.team-name {
  font-size: 18px;
  margin-bottom: 10px;
  font-weight: 500;
}

.red-team .team-name {
  color: #ff4444;
}

.blue-team .team-name {
  color: #4682b4;
}

.team-points {
  font-size: 48px;
  font-weight: bold;
  margin-bottom: 5px;
}

.red-team .team-points {
  color: #ff4444;
}

.blue-team .team-points {
  color: #4682b4;
}

.team-trend {
  font-size: 14px;
  color: var(--apple-text-secondary);
}

.match-timer {
  flex: 1.5;
  text-align: center;
  padding: 20px;
}

.timer-label {
  font-size: 14px;
  color: var(--apple-text-secondary);
  margin-bottom: 10px;
}

.timer-value {
  font-size: 36px;
  font-weight: bold;
  color: var(--apple-text);
  margin-bottom: 15px;
}

.timer-progress {
  width: 100%;
  height: 8px;
  background: rgba(0, 0, 0, 0.06);
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #ff4444, #4682b4);
  border-radius: 4px;
  transition: width 1s ease;
}

/* ============================================
   Chart Container Cards
   ============================================ */
.chart-container {
  flex: 1;
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.98) 0%,
    rgba(250, 250, 250, 0.95) 100%);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: var(--radius-lg);
  border: 0.5px solid var(--apple-border);
  padding: var(--spacing-lg);
  box-shadow: var(--shadow-card);
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

.chart-container:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-card-hover);
  border-color: rgba(0, 122, 255, 0.15);
}

/* 已通过报告样式 */
.approved-reports-container h3 {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 20px 0;
  color: var(--apple-text);
}

.reports-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
  max-height: 400px;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: rgba(0, 122, 255, 0.2) transparent;
}

.reports-list::-webkit-scrollbar {
  width: 6px;
}

.reports-list::-webkit-scrollbar-track {
  background: transparent;
}

.reports-list::-webkit-scrollbar-thumb {
  background: rgba(0, 122, 255, 0.2);
  border-radius: 3px;
}

.report-card {
  background: rgba(255, 255, 255, 0.6);
  border-radius: 12px;
  padding: 15px;
  border: 0.5px solid var(--apple-border);
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.report-card:hover {
  background: rgba(255, 255, 255, 0.8);
  border-color: rgba(0, 122, 255, 0.2);
  transform: translateX(5px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.report-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.team-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
}

.team-badge.red {
  background: rgba(255, 68, 68, 0.1);
  color: #ff4444;
  border: 1px solid rgba(255, 68, 68, 0.3);
}

.team-badge.blue {
  background: rgba(70, 130, 180, 0.1);
  color: #4682b4;
  border: 1px solid rgba(70, 130, 180, 0.3);
}

.approve-time {
  font-size: 12px;
  color: var(--apple-text-secondary);
}

.report-content p {
  margin: 8px 0;
  font-size: 14px;
  color: var(--apple-text);
}

.report-content strong {
  color: var(--apple-text);
  margin-right: 8px;
}

.video-thumbnails {
  display: flex;
  gap: 10px;
  margin-top: 10px;
}

.video-thumb {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 80px;
  height: 60px;
  background: rgba(0, 122, 255, 0.08);
  border-radius: 8px;
  border: 1px solid rgba(0, 122, 255, 0.2);
  cursor: pointer;
  transition: all 0.3s ease;
}

.video-thumb:hover {
  background: rgba(0, 122, 255, 0.15);
  border-color: var(--apple-blue);
  transform: scale(1.1);
}

.video-thumb .el-icon {
  color: var(--apple-blue);
  margin-bottom: 4px;
}

.video-time {
  font-size: 10px;
  color: var(--apple-text-secondary);
}

/* ========== Apple 风格优化结束 ========== */
</style>