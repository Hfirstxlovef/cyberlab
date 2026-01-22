<template>
  <div class="blueteam-bigscreen-container">
    <!-- 四角装饰框架 -->
    <div class="page-corner-frame corner-frame-tl"></div>
    <div class="page-corner-frame corner-frame-tr"></div>
    <div class="page-corner-frame corner-frame-bl"></div>
    <div class="page-corner-frame corner-frame-br"></div>

    <!-- 动态光晕背景层 -->
    <div class="dynamic-glow-layer">
      <div class="glow-spot glow-blue-1"></div>
      <div class="glow-spot glow-blue-2"></div>
      <div class="glow-spot glow-blue-3"></div>
    </div>

    <!-- 标题区域 -->
    <div class="bigscreen-header">
      <!-- 左上角装饰 -->
      <div class="corner-decoration corner-top-left"></div>
      <div class="corner-decoration corner-top-right"></div>

      <!-- 左侧装饰线 -->
      <div class="header-decoration-left"></div>

      <div class="header-content">
        <div class="title-section">
          <h1 class="title">🛡️ CyberLab 蓝队防护态势大屏展示</h1>
          <div class="title-underline"></div>
          <p class="subtitle">Blue Team Defense Posture - Professional Security Operations Center</p>
        </div>

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

      <!-- 右侧装饰线 -->
      <div class="header-decoration-right"></div>
    </div>

    <!-- 主要内容区域 -->
    <div class="bigscreen-content">
      <!-- 第一行：总览指标卡片 -->
      <div class="row row-1">
        <OverviewCards :data="overviewData" />
      </div>

      <!-- 第二行：防护趋势图 + 威胁检测 -->
      <div class="row row-2">
        <div class="chart-container">
          <DefenseTrendChart :data="trendData" />
        </div>
        <div class="chart-container">
          <ThreatDetectionChart :data="threatData" />
        </div>
      </div>

      <!-- 第三行：系统健康状态 + 资源监控 -->
      <div class="row row-3">
        <div class="chart-container">
          <SystemHealthStatus :data="healthData" />
        </div>
        <div class="chart-container">
          <SystemResourceMonitor :data="resourceData" />
        </div>
      </div>

      <!-- 第四行：安全告警 + 防护效果 -->
      <div class="row row-4">
        <div class="chart-container">
          <SecurityAlertPanel :data="alertData" />
        </div>
        <div class="chart-container">
          <DefenseEffectiveness :data="defenseData" />
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
import { ref, onMounted, onUnmounted } from 'vue'
import OverviewCards from '@/components/bigscreen/blue/BlueTeamOverviewCards.vue'
import DefenseTrendChart from '@/components/bigscreen/blue/DefenseTrendChart.vue'
import SystemResourceMonitor from '@/components/bigscreen/SystemResourceMonitor.vue'
import ThreatDetectionChart from '@/components/bigscreen/blue/ThreatDetectionChart.vue'
import SystemHealthStatus from '@/components/bigscreen/blue/SystemHealthStatus.vue'
import SecurityAlertPanel from '@/components/bigscreen/blue/SecurityAlertPanel.vue'
import DefenseEffectiveness from '@/components/bigscreen/blue/DefenseEffectiveness.vue'
import TeamRankingBoard from '@/components/TeamRankingBoard.vue'
import { getBlueTeamBigScreenData } from '@/api/blueTeam'
import { setBigScreenPageMeta } from '@/utils/pageTitle'
import { getToken } from '@/utils/auth'

const currentTime = ref('')
const overviewData = ref({})
const trendData = ref([])
const threatData = ref([])
const healthData = ref([])
const resourceData = ref({})
const alertData = ref([])
const defenseData = ref([])
const isFullscreen = ref(false)

let timer = null

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

// 获取蓝队大屏数据
const fetchData = async () => {
  try {
    const response = await getBlueTeamBigScreenData()
    
    // 处理不同的响应格式
    let data
    if (response && response.data) {
      data = response.data
    } else if (response) {
      data = response
    } else {
      throw new Error('响应数据为空')
    }
    
    // 安全地设置数据，提供默认值
    overviewData.value = data.overview || {}
    trendData.value = data.trend || []
    threatData.value = data.threats || []
    healthData.value = data.health || []
    resourceData.value = data.resources || {}
    alertData.value = data.alerts || []
    defenseData.value = data.defense || []
    
  } catch (error) {
    // 使用默认数据确保页面正常显示
    setDefaultData()
    
    // 检查是否是认证问题
    if (error.response && (error.response.status === 401 || error.response.status === 403)) {
    }
  }
}

// 设置默认数据
const setDefaultData = () => {
  overviewData.value = {
    runningDrills: 3,
    totalDefenses: 856,
    onlineUsers: 12,
    activeServices: 8
  }
  trendData.value = []
  threatData.value = []
  healthData.value = []
  resourceData.value = {
    cpu: 35,
    memory: 58,
    disk: 42,
    network: 18
  }
  alertData.value = []
  defenseData.value = []
}

// 启动定时器
const startTimer = () => {
  updateTime()
  fetchData()
  
  timer = setInterval(() => {
    updateTime()
    fetchData()
  }, 5000) // 每5秒刷新一次
}

// 停止定时器
const stopTimer = () => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

// 进入真正的全屏模式（隐藏浏览器UI）
const enterTrueFullscreen = async () => {
  try {
    const element = document.documentElement
    
    // 尝试使用不同的全屏API
    if (element.requestFullscreen) {
      // 标准API - 支持隐藏浏览器UI
      await element.requestFullscreen({ navigationUI: 'hide' })
    } else if (element.webkitRequestFullscreen) {
      // WebKit - Safari/Chrome
      await element.webkitRequestFullscreen(Element.ALLOW_KEYBOARD_INPUT)
    } else if (element.mozRequestFullScreen) {
      // Firefox
      await element.mozRequestFullScreen()
    } else if (element.msRequestFullscreen) {
      // IE/Edge
      await element.msRequestFullscreen()
    }
    
  } catch (error) {
    // 如果全屏失败，尝试其他方法
    tryAlternativeFullscreen()
  }
}

// 备用全屏方法
const tryAlternativeFullscreen = () => {
  try {
    // 隐藏浏览器滚动条
    document.body.style.overflow = 'hidden'
    document.documentElement.style.overflow = 'hidden'
    
    // 设置页面样式以模拟全屏
    const container = document.querySelector('.blueteam-bigscreen-container')
    if (container) {
      container.style.position = 'fixed'
      container.style.top = '0'
      container.style.left = '0'
      container.style.width = '100vw'
      container.style.height = '100vh'
      container.style.zIndex = '9999'
    }
    
  } catch (error) {
  }
}

// 退出全屏模式
const exitFullscreen = async () => {
  try {
    // 退出浏览器全屏
    if (document.exitFullscreen) {
      await document.exitFullscreen()
    } else if (document.webkitExitFullscreen) {
      await document.webkitExitFullscreen()
    } else if (document.mozCancelFullScreen) {
      await document.mozCancelFullScreen()
    } else if (document.msExitFullscreen) {
      await document.msExitFullscreen()
    }
    
    // 恢复页面样式
    document.body.style.overflow = ''
    document.documentElement.style.overflow = ''
    
    const container = document.querySelector('.blueteam-bigscreen-container')
    if (container) {
      container.style.position = ''
      container.style.top = ''
      container.style.left = ''
      container.style.width = ''
      container.style.height = ''
      container.style.zIndex = ''
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

// 监听键盘事件（ESC退出全屏）
const handleKeydown = (event) => {
  if (event.key === 'Escape' || event.keyCode === 27) {
    exitFullscreen()
  }
  // F11 切换全屏
  if (event.key === 'F11' || event.keyCode === 122) {
    event.preventDefault()
    const isFullscreen = !!(
      document.fullscreenElement ||
      document.webkitFullscreenElement ||
      document.mozFullScreenElement ||
      document.msFullscreenElement
    )
    
    if (isFullscreen) {
      exitFullscreen()
    } else {
      enterTrueFullscreen()
    }
  }
}

// 监听来自父窗口的消息
const handleMessage = (event) => {
  // 确保消息来自同源
  if (event.origin !== window.location.origin) {
    return
  }
  
  if (event.data && event.data.action === 'enterFullscreen') {
    // 延迟一点时间确保页面完全渲染
    setTimeout(() => {
      enterTrueFullscreen()
    }, 100)
  }
}

onMounted(() => {
  // 设置蓝队大屏页面的标题和图标
  setBigScreenPageMeta('blue')
  
  // 检查认证状态和角色权限
  const token = getToken()
  if (!token) {
    // 重定向到登录页面，并保存当前页面作为返回地址
    const returnUrl = encodeURIComponent(window.location.href)
    window.location.href = `/login?redirect=${returnUrl}`
    return
  }
  
  // TODO: 添加角色权限检查
  // const userRole = getUserRole()
  // if (userRole !== 'blue') {
  //   window.location.href = '/dashboard'
  //   return
  // }
  
  
  startTimer()
  
  // 添加全屏状态监听
  document.addEventListener('fullscreenchange', handleFullscreenChange)
  document.addEventListener('webkitfullscreenchange', handleFullscreenChange)
  document.addEventListener('mozfullscreenchange', handleFullscreenChange)
  document.addEventListener('MSFullscreenChange', handleFullscreenChange)
  
  // 添加键盘监听
  document.addEventListener('keydown', handleKeydown)
  
  // 添加消息监听，用于接收来自父窗口的全屏请求
  window.addEventListener('message', handleMessage)
  
  // 初始化全屏状态
  handleFullscreenChange()
  
  // 检查是否在新窗口中打开，如果是则自动尝试进入全屏
  if (window.opener) {
    setTimeout(() => {
      enterTrueFullscreen()
    }, 1000) // 延迟1秒确保页面完全加载
  }
  
})

onUnmounted(() => {
  stopTimer()
  
  // 移除事件监听
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
  document.removeEventListener('webkitfullscreenchange', handleFullscreenChange)
  document.removeEventListener('mozfullscreenchange', handleFullscreenChange)
  document.removeEventListener('MSFullscreenChange', handleFullscreenChange)
  document.removeEventListener('keydown', handleKeydown)
  
  // 移除消息监听
  window.removeEventListener('message', handleMessage)
  
  // 退出全屏模式
  exitFullscreen()
})
</script>

<style scoped>
/* ============================================
   蓝队网络空间安全驾驶舱 - Blue Team Security Cockpit
   专业防御者主题 - Professional Defender Theme
   ============================================ */

/* CSS Variables - 蓝队防御主题 */
:root {
  --blue-defender-bg: #0a1428;
  --blue-defender-bg-secondary: #0d1a2d;
  --blue-steel: #4682b4;
  --blue-electric: #1e90ff;
  --blue-cyan: #00d4ff;
  --blue-ice: #a8d8ea;
  --blue-glow-primary: rgba(70, 130, 180, 0.3);
  --blue-glow-accent: rgba(30, 144, 255, 0.25);
  --blue-glow-cyan: rgba(0, 212, 255, 0.2);
  --radius-sm: 12px;
  --radius-md: 16px;
  --radius-lg: 20px;
  --spacing-xs: 8px;
  --spacing-sm: 12px;
  --spacing-md: 16px;
  --spacing-lg: 24px;
  --spacing-xl: 32px;
  --font-apple: -apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", "Helvetica Neue", sans-serif;
  --font-mono: "SF Mono", Consolas, Monaco, monospace;
}

.blueteam-bigscreen-container {
  width: 100vw;
  height: 100vh;
  background:
    /* 微妙的网格纹理 */
    repeating-linear-gradient(
      0deg,
      rgba(70, 130, 180, 0.02) 0px,
      transparent 1px,
      transparent 40px,
      rgba(70, 130, 180, 0.02) 41px
    ),
    repeating-linear-gradient(
      90deg,
      rgba(70, 130, 180, 0.02) 0px,
      transparent 1px,
      transparent 40px,
      rgba(70, 130, 180, 0.02) 41px
    ),
    /* 主背景渐变 */
    linear-gradient(135deg, #0a1428 0%, #0d1a2d 50%, #0f1620 100%);
  color: #ffffff;
  font-family: var(--font-apple);
  overflow-y: auto;
  overflow-x: hidden;
  position: relative;
  /* 确保全屏时覆盖所有内容 */
  z-index: 9999;
  /* 美化滚动条 */
  scrollbar-width: thin;
  scrollbar-color: rgba(70, 130, 180, 0.3) transparent;
}

/* 四角装饰框架 */
.page-corner-frame {
  position: fixed;
  width: 80px;
  height: 80px;
  z-index: 100;
  pointer-events: none;
}

.corner-frame-tl {
  top: 20px;
  left: 20px;
  border-top: 3px solid rgba(70, 130, 180, 0.5);
  border-left: 3px solid rgba(70, 130, 180, 0.5);
  border-top-left-radius: 8px;
  box-shadow: 0 0 20px rgba(70, 130, 180, 0.3),
              inset 0 0 20px rgba(70, 130, 180, 0.1);
  animation: corner-pulse 4s ease-in-out infinite;
}

.corner-frame-tr {
  top: 20px;
  right: 20px;
  border-top: 3px solid rgba(70, 130, 180, 0.5);
  border-right: 3px solid rgba(70, 130, 180, 0.5);
  border-top-right-radius: 8px;
  box-shadow: 0 0 20px rgba(70, 130, 180, 0.3),
              inset 0 0 20px rgba(70, 130, 180, 0.1);
  animation: corner-pulse 4s ease-in-out infinite;
  animation-delay: 1s;
}

.corner-frame-bl {
  bottom: 20px;
  left: 20px;
  border-bottom: 3px solid rgba(70, 130, 180, 0.5);
  border-left: 3px solid rgba(70, 130, 180, 0.5);
  border-bottom-left-radius: 8px;
  box-shadow: 0 0 20px rgba(70, 130, 180, 0.3),
              inset 0 0 20px rgba(70, 130, 180, 0.1);
  animation: corner-pulse 4s ease-in-out infinite;
  animation-delay: 2s;
}

.corner-frame-br {
  bottom: 20px;
  right: 20px;
  border-bottom: 3px solid rgba(70, 130, 180, 0.5);
  border-right: 3px solid rgba(70, 130, 180, 0.5);
  border-bottom-right-radius: 8px;
  box-shadow: 0 0 20px rgba(70, 130, 180, 0.3),
              inset 0 0 20px rgba(70, 130, 180, 0.1);
  animation: corner-pulse 4s ease-in-out infinite;
  animation-delay: 3s;
}

@keyframes corner-pulse {
  0%, 100% {
    opacity: 0.6;
    box-shadow: 0 0 20px rgba(70, 130, 180, 0.3),
                inset 0 0 20px rgba(70, 130, 180, 0.1);
  }
  50% {
    opacity: 1;
    box-shadow: 0 0 30px rgba(70, 130, 180, 0.5),
                inset 0 0 30px rgba(70, 130, 180, 0.2);
  }
}

/* Webkit浏览器滚动条样式 */
.blueteam-bigscreen-container::-webkit-scrollbar {
  width: 8px;
}

.blueteam-bigscreen-container::-webkit-scrollbar-track {
  background: transparent;
}

.blueteam-bigscreen-container::-webkit-scrollbar-thumb {
  background: rgba(70, 130, 180, 0.3);
  border-radius: 4px;
}

.blueteam-bigscreen-container::-webkit-scrollbar-thumb:hover {
  background: rgba(70, 130, 180, 0.5);
}

/* 全屏状态下的样式 */
.blueteam-bigscreen-container:fullscreen {
  width: 100vw;
  height: 100vh;
  margin: 0;
  padding: 0;
}

.blueteam-bigscreen-container:-webkit-full-screen {
  width: 100vw;
  height: 100vh;
  margin: 0;
  padding: 0;
}

.blueteam-bigscreen-container:-moz-full-screen {
  width: 100vw;
  height: 100vh;
  margin: 0;
  padding: 0;
}

.blueteam-bigscreen-container:-ms-fullscreen {
  width: 100vw;
  height: 100vh;
  margin: 0;
  padding: 0;
}

/* 动态光晕背景层 - Dynamic Glow Layer */
.dynamic-glow-layer {
  position: fixed;
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
  filter: blur(80px);
  opacity: 0.3;
}

.glow-blue-1 {
  top: 15%;
  left: 20%;
  width: 450px;
  height: 450px;
  background: radial-gradient(circle, rgba(70, 130, 180, 0.25) 0%, transparent 70%);
  animation: glow-breath-blue-1 9s cubic-bezier(0.45, 0.05, 0.55, 0.95) infinite;
}

.glow-blue-2 {
  bottom: 15%;
  right: 20%;
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, rgba(30, 144, 255, 0.2) 0%, transparent 70%);
  animation: glow-breath-blue-2 11s cubic-bezier(0.45, 0.05, 0.55, 0.95) infinite;
  animation-delay: 2s;
}

.glow-blue-3 {
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(0, 212, 255, 0.15) 0%, transparent 70%);
  animation: glow-breath-blue-3 7s cubic-bezier(0.45, 0.05, 0.55, 0.95) infinite;
  animation-delay: 1s;
}

@keyframes glow-breath-blue-1 {
  0%, 100% {
    opacity: 0.25;
    transform: scale(1);
  }
  50% {
    opacity: 0.45;
    transform: scale(1.18);
  }
}

@keyframes glow-breath-blue-2 {
  0%, 100% {
    opacity: 0.2;
    transform: scale(1);
  }
  50% {
    opacity: 0.4;
    transform: scale(1.22);
  }
}

@keyframes glow-breath-blue-3 {
  0%, 100% {
    opacity: 0.18;
    transform: translate(-50%, -50%) scale(1);
  }
  50% {
    opacity: 0.38;
    transform: translate(-50%, -50%) scale(1.28);
  }
}

/* 防御护盾波纹扩散动画 - 从中心向外的防御波 */
@keyframes shield-ripple {
  0% {
    transform: translate(-50%, -50%) scale(0.65);
    opacity: 0.7;
  }
  40% {
    opacity: 0.4;
  }
  100% {
    transform: translate(-50%, -50%) scale(1.4);
    opacity: 0;
  }
}

/* 防御网格呼吸动画 - 稳定的防护系统 */
@keyframes defense-breath {
  0%, 100% {
    opacity: 0.35;
    transform: scale(1);
  }
  50% {
    opacity: 0.75;
    transform: scale(1.08);
  }
}

/* 防御护盾波纹 - 同心圆扩散效果（对比红队的旋转雷达） */
.blueteam-bigscreen-container::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 800px;
  height: 800px;
  background:
    radial-gradient(circle, transparent 30%, rgba(70, 130, 180, 0.15) 31%, transparent 32%),
    radial-gradient(circle, transparent 45%, rgba(30, 144, 255, 0.12) 46%, transparent 47%),
    radial-gradient(circle, transparent 60%, rgba(70, 130, 180, 0.1) 61%, transparent 62%);
  border-radius: 50%;
  pointer-events: none;
  animation: shield-ripple 6s cubic-bezier(0.4, 0, 0.2, 1) infinite;
  z-index: 0;
  filter: blur(1px);
}

/* 六边形防御网格呼吸 - 系统化防护效果 */
.blueteam-bigscreen-container::after {
  content: '';
  position: absolute;
  top: 20%;
  right: 15%;
  width: 500px;
  height: 500px;
  background:
    radial-gradient(circle, rgba(30, 144, 255, 0.08) 0%, transparent 60%),
    repeating-conic-gradient(
      from 0deg at 50% 50%,
      rgba(70, 130, 180, 0.15) 0deg,
      transparent 60deg,
      rgba(70, 130, 180, 0.15) 120deg
    );
  border-radius: 50%;
  pointer-events: none;
  animation: defense-breath 7s cubic-bezier(0.45, 0.05, 0.55, 0.95) infinite;
  z-index: 0;
  filter: blur(2px);
}

.bigscreen-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 40px;
  background: rgba(20, 30, 50, 0.7);
  backdrop-filter: blur(25px);
  -webkit-backdrop-filter: blur(25px);
  border-bottom: 1px solid rgba(70, 130, 180, 0.35);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.4),
              inset 0 1px 0 rgba(255, 255, 255, 0.08),
              0 0 40px rgba(70, 130, 180, 0.1);
  position: relative;
  z-index: 10;
}

/* 四角装饰 */
.corner-decoration {
  position: absolute;
  width: 30px;
  height: 30px;
  z-index: 5;
}

.corner-top-left {
  top: 0;
  left: 0;
  border-top: 2px solid rgba(70, 130, 180, 0.6);
  border-left: 2px solid rgba(70, 130, 180, 0.6);
  border-top-left-radius: 4px;
  box-shadow: 0 0 15px rgba(70, 130, 180, 0.4);
}

.corner-top-right {
  top: 0;
  right: 0;
  border-top: 2px solid rgba(70, 130, 180, 0.6);
  border-right: 2px solid rgba(70, 130, 180, 0.6);
  border-top-right-radius: 4px;
  box-shadow: 0 0 15px rgba(70, 130, 180, 0.4);
}

/* 左右装饰线 */
.header-decoration-left {
  position: absolute;
  left: 50px;
  top: 50%;
  transform: translateY(-50%);
  width: 2px;
  height: 60%;
  background: linear-gradient(180deg,
    transparent 0%,
    rgba(70, 130, 180, 0.6) 50%,
    transparent 100%);
  box-shadow: 0 0 10px rgba(70, 130, 180, 0.4);
  animation: pulse-line 3s ease-in-out infinite;
}

.header-decoration-right {
  position: absolute;
  right: 50px;
  top: 50%;
  transform: translateY(-50%);
  width: 2px;
  height: 60%;
  background: linear-gradient(180deg,
    transparent 0%,
    rgba(70, 130, 180, 0.6) 50%,
    transparent 100%);
  box-shadow: 0 0 10px rgba(70, 130, 180, 0.4);
  animation: pulse-line 3s ease-in-out infinite;
  animation-delay: 1.5s;
}

@keyframes pulse-line {
  0%, 100% {
    opacity: 0.5;
  }
  50% {
    opacity: 1;
  }
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding: 0 70px;
}

.title-section {
  flex: 1;
  text-align: center;
}

.bigscreen-header::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  width: 4px;
  height: 100%;
  background: linear-gradient(180deg,
    rgba(70, 130, 180, 0.9) 0%,
    rgba(30, 144, 255, 0.6) 50%,
    rgba(70, 130, 180, 0.9) 100%);
  border-radius: 0 4px 4px 0;
  box-shadow: 0 0 15px rgba(70, 130, 180, 0.5);
}

.title {
  font-size: 36px;
  font-weight: 700;
  margin: 0;
  background: linear-gradient(135deg,
    #4682b4 0%,
    #5fa3d4 25%,
    #1e90ff 50%,
    #6bb6ff 75%,
    #00d4ff 100%);
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  text-shadow: 0 0 35px rgba(70, 130, 180, 0.5);
  filter: drop-shadow(0 2px 15px rgba(70, 130, 180, 0.4))
          drop-shadow(0 0 25px rgba(30, 144, 255, 0.3));
  letter-spacing: 0.5px;
  animation: title-shimmer 8s ease-in-out infinite;
}

@keyframes title-shimmer {
  0%, 100% {
    filter: drop-shadow(0 2px 15px rgba(70, 130, 180, 0.4))
            drop-shadow(0 0 25px rgba(30, 144, 255, 0.3));
  }
  50% {
    filter: drop-shadow(0 2px 20px rgba(70, 130, 180, 0.6))
            drop-shadow(0 0 35px rgba(30, 144, 255, 0.5));
  }
}

.title-underline {
  width: 200px;
  height: 2px;
  margin: 12px auto;
  background: linear-gradient(90deg,
    transparent 0%,
    rgba(70, 130, 180, 0.3) 20%,
    rgba(30, 144, 255, 0.6) 50%,
    rgba(70, 130, 180, 0.3) 80%,
    transparent 100%);
  box-shadow: 0 0 10px rgba(30, 144, 255, 0.4);
  position: relative;
}

.title-underline::before,
.title-underline::after {
  content: '';
  position: absolute;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: rgba(30, 144, 255, 0.8);
  box-shadow: 0 0 8px rgba(30, 144, 255, 0.6);
  top: 50%;
  transform: translateY(-50%);
}

.title-underline::before {
  left: -10px;
}

.title-underline::after {
  right: -10px;
}

.subtitle {
  margin: 8px 0 0;
  font-size: 13px;
  font-weight: 500;
  color: rgba(168, 216, 234, 0.8);
  letter-spacing: 1px;
  text-transform: uppercase;
  font-family: var(--font-mono);
}

.header-info {
  display: flex;
  gap: 20px;
  font-size: 15px;
  color: rgba(255, 255, 255, 0.85);
  align-items: center;
}

.time {
  font-family: var(--font-mono);
  font-weight: 600;
  color: rgba(168, 216, 234, 0.9);
  padding: 6px 14px;
  background: rgba(30, 144, 255, 0.08);
  border-radius: var(--radius-sm);
  border: 1px solid rgba(70, 130, 180, 0.25);
  box-shadow: 0 2px 8px rgba(70, 130, 180, 0.1),
              inset 0 1px 0 rgba(255, 255, 255, 0.05);
  letter-spacing: 0.5px;
}

.version {
  font-family: var(--font-mono);
  font-weight: 600;
  color: rgba(0, 212, 255, 0.8);
  padding: 6px 14px;
  background: rgba(0, 212, 255, 0.05);
  border-radius: var(--radius-sm);
  border: 1px solid rgba(0, 212, 255, 0.2);
  box-shadow: 0 2px 8px rgba(0, 212, 255, 0.1);
  letter-spacing: 0.5px;
}

.fullscreen-controls {
  display: flex;
  align-items: center;
  gap: 10px;
}

.fullscreen-tip {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  animation: pulse 2s infinite;
}

.fullscreen-btn {
  background: linear-gradient(135deg,
    rgba(70, 130, 180, 0.28) 0%,
    rgba(30, 144, 255, 0.35) 100%);
  border: 1.5px solid rgba(70, 130, 180, 0.6);
  color: #ffffff;
  padding: 10px 20px;
  border-radius: var(--radius-md);
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  transition: all 0.4s cubic-bezier(0.19, 1, 0.22, 1);
  white-space: nowrap;
  box-shadow: 0 4px 15px rgba(70, 130, 180, 0.2),
              inset 0 1px 0 rgba(255, 255, 255, 0.1),
              inset 0 -1px 0 rgba(0, 0, 0, 0.1);
  position: relative;
  overflow: hidden;
  font-family: var(--font-apple);
  letter-spacing: 0.3px;
}

.fullscreen-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg,
    transparent 0%,
    rgba(255, 255, 255, 0.1) 50%,
    transparent 100%);
  transition: left 0.6s ease;
}

.fullscreen-btn:hover::before {
  left: 100%;
}

.fullscreen-btn:hover {
  background: linear-gradient(135deg,
    rgba(70, 130, 180, 0.4) 0%,
    rgba(30, 144, 255, 0.5) 100%);
  border-color: rgba(70, 130, 180, 0.8);
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(70, 130, 180, 0.4),
              0 0 20px rgba(30, 144, 255, 0.3),
              inset 0 1px 0 rgba(255, 255, 255, 0.15);
}

.fullscreen-btn:active {
  transform: translateY(0);
  box-shadow: 0 4px 12px rgba(70, 130, 180, 0.3);
}

@keyframes pulse {
  0%, 100% {
    opacity: 0.6;
  }
  50% {
    opacity: 1;
  }
}

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

.chart-container {
  flex: 1;
  background: rgba(20, 30, 50, 0.65);
  backdrop-filter: blur(25px);
  -webkit-backdrop-filter: blur(25px);
  border-radius: var(--radius-lg);
  border: 1px solid rgba(70, 130, 180, 0.35);
  padding: 20px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.45),
              0 0 20px rgba(70, 130, 180, 0.08),
              inset 0 1px 1px rgba(255, 255, 255, 0.08),
              inset 0 -1px 1px rgba(0, 0, 0, 0.1);
  transition: all 0.4s cubic-bezier(0.19, 1, 0.22, 1);
  position: relative;
  overflow: hidden;
}

/* 卡片边角装饰 */
.chart-container::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: var(--radius-lg);
  padding: 1px;
  background: linear-gradient(135deg,
    rgba(70, 130, 180, 0.3) 0%,
    transparent 20%,
    transparent 80%,
    rgba(30, 144, 255, 0.3) 100%);
  -webkit-mask: linear-gradient(#fff 0 0) content-box,
                linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.4s ease;
}

/* 卡片内发光 */
.chart-container::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  width: 3px;
  height: 100%;
  background: linear-gradient(180deg,
    rgba(70, 130, 180, 0.9) 0%,
    rgba(30, 144, 255, 0.6) 50%,
    rgba(70, 130, 180, 0.9) 100%);
  border-radius: 16px 0 0 16px;
  box-shadow: 0 0 15px rgba(70, 130, 180, 0.6),
              inset 0 0 10px rgba(30, 144, 255, 0.4);
  z-index: 2;
}

/* 光边扫过效果 */
.chart-container .shimmer {
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: linear-gradient(
    45deg,
    transparent 30%,
    rgba(70, 130, 180, 0.1) 50%,
    transparent 70%
  );
  transform: rotate(45deg);
  animation: shimmer-sweep 8s ease-in-out infinite;
  pointer-events: none;
}

@keyframes shimmer-sweep {
  0%, 100% {
    transform: translateX(-100%) rotate(45deg);
    opacity: 0;
  }
  10%, 90% {
    opacity: 1;
  }
  50% {
    transform: translateX(100%) rotate(45deg);
  }
}

.chart-container:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 48px rgba(70, 130, 180, 0.3),
              0 0 40px rgba(70, 130, 180, 0.2),
              inset 0 1px 2px rgba(255, 255, 255, 0.12);
  border-color: rgba(70, 130, 180, 0.5);
}

.chart-container:hover::after {
  opacity: 1;
}
</style>