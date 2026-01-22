<template>
  <div class="bigscreen-container">
    <!-- 标题区域 -->
    <div class="bigscreen-header">
      <h1 class="title">🔐 CyberLab 安全演练态势大屏展示</h1>
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
        <OverviewCards :data="overviewData" />
      </div>

      <!-- 第二行：攻击趋势图 + 热门漏洞类型 -->
      <div class="row row-2">
        <div class="chart-container">
          <AttackTrendChart :data="trendData" />
        </div>
        <div class="chart-container">
          <VulnerabilityChart :data="vulnData" />
        </div>
      </div>

      <!-- 第三行：容器动态 + 资源监控 -->
      <div class="row row-3">
        <div class="chart-container">
          <ContainerStatus :data="containerData" />
        </div>
        <div class="chart-container">
          <SystemResourceMonitor :data="resourceData" />
        </div>
      </div>

      <!-- 第四行：告警信息 + 战队排行 -->
      <div class="row row-4">
        <div class="chart-container">
          <AlertPanel :data="alertData" />
        </div>
        <div class="chart-container">
          <TeamRanking :data="rankingData" />
        </div>
      </div>

      <!-- 第五行：实时成果流 -->
      <div class="row row-5">
        <div class="chart-container achievement-feed-container">
          <AchievementFeed />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import OverviewCards from '@/components/bigscreen/OverviewCards.vue'
import AttackTrendChart from '@/components/bigscreen/AttackTrendChart.vue'
import SystemResourceMonitor from '@/components/bigscreen/SystemResourceMonitor.vue'
import VulnerabilityChart from '@/components/bigscreen/VulnerabilityChart.vue'
import ContainerStatus from '@/components/bigscreen/ContainerStatus.vue'
import AlertPanel from '@/components/bigscreen/AlertPanel.vue'
import TeamRanking from '@/components/bigscreen/TeamRanking.vue'
import AchievementFeed from '@/components/AchievementFeed.vue'
import { getBigScreenData } from '@/api/bigscreen'
import { setBigScreenPageMeta } from '@/utils/pageTitle'
import { getToken } from '@/utils/auth'

const currentTime = ref('')
const overviewData = ref({})
const trendData = ref([])
const vulnData = ref([])
const containerData = ref([])
const resourceData = ref({})
const alertData = ref([])
const rankingData = ref([])
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

// 获取大屏数据
const fetchData = async () => {
  try {
    const response = await getBigScreenData()

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
    vulnData.value = data.vulnerability || []
    containerData.value = data.containers || []
    resourceData.value = data.resources || {}
    alertData.value = data.alerts || []
    rankingData.value = data.ranking || []
  } catch (error) {
    // 使用默认数据确保页面正常显示
    setDefaultData()
    
    // 检查是否是认证问题
    if (error.response && (error.response.status === 401 || error.response.status === 403)) {
      // 可选：显示重新登录提示
    }
  }
}

// 设置默认数据（当API失败时使用空数据，避免显示误导性的示例数据）
const setDefaultData = () => {
  overviewData.value = {
    runningDrills: 0,
    totalAttacks: 0,
    onlineUsers: 0,
    activeContainers: 0
  }
  trendData.value = []
  vulnData.value = []
  containerData.value = []
  resourceData.value = {
    cpu: 0,
    memory: 0,
    disk: 0,
    network: 0
  }
  alertData.value = []
  rankingData.value = []
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

    // 使用 requestAnimationFrame 确保在浏览器准备好时执行，减少卡顿
    await new Promise(resolve => requestAnimationFrame(resolve))

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
    // 八耻八荣：静默处理全屏API错误，防止控制台刷屏
    // 如果全屏失败，尝试其他方法
    tryAlternativeFullscreen()
  }
}

// 备用全屏方法
const tryAlternativeFullscreen = () => {
  try {
    // 设置页面样式以模拟全屏
    const container = document.querySelector('.bigscreen-container')
    if (container) {
      container.style.position = 'fixed'
      container.style.top = '0'
      container.style.left = '0'
      container.style.width = '100vw'
      container.style.height = '100vh'
      container.style.zIndex = '9999'
      // 即使在全屏模式下也保持可滚动，以便查看所有内容
      container.style.overflowY = 'auto'
      container.style.overflowX = 'hidden'
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

    const container = document.querySelector('.bigscreen-container')
    if (container) {
      container.style.position = ''
      container.style.top = ''
      container.style.left = ''
      container.style.width = ''
      container.style.height = ''
      container.style.zIndex = ''
      // 恢复滚动：确保非全屏状态下可滚动
      container.style.overflowY = 'auto'
      container.style.overflowX = 'hidden'
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
  // 设置管理员大屏页面的标题和图标
  setBigScreenPageMeta('admin')
  
  // 检查认证状态
  const token = getToken()
  if (!token) {
    // 重定向到登录页面，并保存当前页面作为返回地址
    const returnUrl = encodeURIComponent(window.location.href)
    window.location.href = `/login?redirect=${returnUrl}`
    return
  }
  
  
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

<style>
/* 全局样式 - 确保页面可以滚动 */
html, body {
  overflow-y: auto !important;
  height: auto !important;
  min-height: 100vh;
}
</style>

<style scoped>
/* ============================================
   Apple Elegant White Style - 管理员大屏幕
   Admin BigScreen Dashboard
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

.bigscreen-container {
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
.bigscreen-container:fullscreen,
.bigscreen-container:-webkit-full-screen,
.bigscreen-container:-moz-full-screen,
.bigscreen-container:-ms-fullscreen {
  width: 100vw;
  height: 100vh;
  margin: 0;
  padding: 0;
}

/* 精致的背景纹理 */
.bigscreen-container::before {
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

.achievement-feed-container {
  width: 100%;
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

/* ============================================
   Responsive Design
   ============================================ */
@media (max-width: 1200px) {
  .bigscreen-content {
    padding: var(--spacing-md);
  }

  .row {
    gap: var(--spacing-md);
  }
}

@media (max-width: 768px) {
  .bigscreen-header {
    flex-direction: column;
    gap: var(--spacing-md);
    padding: var(--spacing-md);
  }

  .title {
    font-size: 24px;
  }

  .header-info {
    flex-wrap: wrap;
    justify-content: center;
    gap: var(--spacing-sm);
  }

  .fullscreen-tip {
    display: none;
  }

  .bigscreen-content {
    padding: var(--spacing-sm);
  }

  .row {
    flex-direction: column;
    gap: var(--spacing-sm);
  }

  .chart-container {
    padding: var(--spacing-md);
  }
}

@media (max-width: 576px) {
  .title {
    font-size: 20px;
  }

  .header-info {
    font-size: 13px;
  }

  .fullscreen-btn {
    padding: 8px 16px;
    font-size: 13px;
  }
}
</style>