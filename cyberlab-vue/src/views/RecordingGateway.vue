<template>
  <div class="recording-gateway">
    <!-- 动态光晕背景层 -->
    <div class="dynamic-glow-layer">
      <div class="glow-spot glow-1"></div>
      <div class="glow-spot glow-2"></div>
      <div class="glow-spot glow-3"></div>
    </div>

    <el-card class="gateway-card">
      <div class="gateway-header">
        <h1>🔴 红队攻击项目准入验证</h1>
        <p class="subtitle">为确保安全审计和学习记录，所有攻击操作必须全程录屏</p>
      </div>

      <!-- 未开启录屏状态 -->
      <div v-if="!canAccessProject" class="access-blocked">
        <el-alert
          type="warning"
          title="需要开启录屏才能访问攻击项目"
          :closable="false"
          show-icon>
          <template #default>
            <p>点击下方按钮开始录屏，浏览器将请求屏幕共享权限</p>
          </template>
        </el-alert>

        <div class="recording-status">
          <div class="status-icon">
            <el-icon v-if="!isRecording" :size="100" color="#909399">
              <VideoCamera />
            </el-icon>
            <el-icon v-else :size="100" color="#F56C6C" class="recording-pulse">
              <VideoCamera />
            </el-icon>
          </div>
          <p class="status-text">{{ isRecording ? '录屏进行中...' : '未开启录屏' }}</p>
        </div>

        <div class="action-area">
          <el-button
            type="danger"
            size="large"
            @click="handleStartRecording"
            :loading="starting"
            class="start-btn">
            <el-icon><VideoPlay /></el-icon>
            开始录屏并进入攻击项目
          </el-button>
        </div>

        <div class="tips-section">
          <h3>📋 使用说明</h3>
          <ol class="tips-list">
            <li>点击"开始录屏"按钮</li>
            <li>浏览器会弹窗请求屏幕录制权限</li>
            <li>选择要共享的<strong>整个屏幕</strong>或<strong>应用窗口</strong></li>
            <li>点击浏览器弹窗中的"共享"按钮</li>
            <li>录屏开启后即可访问攻击项目</li>
            <li>录屏会自动分片上传到服务器，无需手动操作</li>
            <li>完成攻击后可手动停止录屏，或关闭页面自动停止</li>
          </ol>

          <el-alert
            type="info"
            :closable="false"
            class="browser-tip">
            <template #default>
              <strong>浏览器兼容性：</strong>
              <ul>
                <li>✅ Chrome / Edge - 完全支持</li>
                <li>✅ Firefox - 支持（需确认权限）</li>
                <li>⚠️ Safari - 部分支持（建议使用Chrome）</li>
              </ul>
            </template>
          </el-alert>
        </div>
      </div>

      <!-- 已开启录屏状态 -->
      <div v-else class="access-granted">
        <el-result icon="success" title="✅ 录屏已开启" sub-title="您现在可以访问攻击项目了">
          <template #extra>
            <div class="recording-info">
              <div class="info-item">
                <span class="label">录屏状态：</span>
                <el-tag type="danger" size="large">
                  <el-icon class="recording-dot"><VideoCamera /></el-icon>
                  录制中
                </el-tag>
              </div>
              <div class="info-item">
                <span class="label">录制时长：</span>
                <span class="duration">{{ formattedDuration }}</span>
              </div>
            </div>

            <div class="action-buttons">
              <el-button
                type="primary"
                size="large"
                @click="enterProject"
                class="enter-btn">
                🚀 进入攻击项目
              </el-button>
              <el-button
                size="large"
                @click="handleStopRecording">
                ⏹️ 停止录屏
              </el-button>
            </div>
          </template>
        </el-result>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { VideoCamera, VideoPlay } from '@element-plus/icons-vue'
import { useScreenRecorder } from '@/composables/useScreenRecorder'
import { getUserId, isLoggedIn } from '@/utils/auth'

const router = useRouter()
const {
  isRecording,
  canAccessProject,
  recordingDuration,
  startRecording,
  stopRecording,
  checkRecordingStatus,
  formatDuration
} = useScreenRecorder()

const starting = ref(false)

const formattedDuration = computed(() => formatDuration(recordingDuration.value))

async function handleStartRecording() {
  starting.value = true
  const success = await startRecording()
  starting.value = false

  if (!success) {
    ElMessage.error('录屏启动失败，请重试或检查浏览器权限设置')
  }
}

async function handleStopRecording() {
  await stopRecording()
  ElMessage.info('录屏已停止，您需要重新开启录屏才能访问攻击项目')
}

function enterProject() {
  // 根据实际路由配置修改
  router.push('/red/attack-workspace')
}

// 页面加载时检查录屏状态
onMounted(async () => {
  // 检查用户是否已登录
  if (!isLoggedIn()) {
    ElMessage.warning('请先登录后再访问攻击项目')
    router.push('/login')
    return
  }

  // 获取用户ID
  const userId = getUserId()
  if (!userId) {
    ElMessage.warning('用户信息异常，请重新登录')
    router.push('/login')
    return
  }

  // 检查录屏状态
  await checkRecordingStatus()
})
</script>

<style scoped>
/* ============================================
   红队准入验证 - 深色黑客科技风 + Apple优雅
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

.recording-gateway {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0a0a0a 0%, #1a0d0d 50%, #0f0f0f 100%);
  padding: 20px;
  position: relative;
  overflow: hidden;
  font-family: var(--font-apple);
}

/* 动态光晕背景层 */
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

.glow-1 {
  top: 15%;
  left: 25%;
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(255, 59, 48, 0.2) 0%, transparent 70%);
  animation: glow-breath-1 8s ease-in-out infinite;
}

.glow-2 {
  bottom: 20%;
  right: 15%;
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, rgba(204, 0, 0, 0.15) 0%, transparent 70%);
  animation: glow-breath-2 10s ease-in-out infinite;
  animation-delay: 2s;
}

.glow-3 {
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 350px;
  height: 350px;
  background: radial-gradient(circle, rgba(255, 59, 48, 0.12) 0%, transparent 70%);
  animation: glow-breath-3 6s ease-in-out infinite;
  animation-delay: 1s;
}

@keyframes glow-breath-1 {
  0%, 100% {
    opacity: 0.25;
    transform: scale(1);
  }
  50% {
    opacity: 0.4;
    transform: scale(1.15);
  }
}

@keyframes glow-breath-2 {
  0%, 100% {
    opacity: 0.2;
    transform: scale(1);
  }
  50% {
    opacity: 0.35;
    transform: scale(1.2);
  }
}

@keyframes glow-breath-3 {
  0%, 100% {
    opacity: 0.18;
    transform: translate(-50%, -50%) scale(1);
  }
  50% {
    opacity: 0.32;
    transform: translate(-50%, -50%) scale(1.25);
  }
}

/* 主卡片 */
.gateway-card {
  width: 100%;
  max-width: 1000px;
  background: var(--hacker-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 20px;
  border: 0.5px solid rgba(255, 59, 48, 0.2);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5), inset 0 0 0 0.5px rgba(255, 255, 255, 0.05);
  position: relative;
  z-index: 1;
  color: var(--hacker-text);
}

.gateway-card :deep(.el-card__body) {
  padding: 40px;
}

/* 页面头部 */
.gateway-header {
  text-align: center;
  margin-bottom: 40px;
}

.gateway-header h1 {
  font-size: 36px;
  font-weight: 700;
  color: #ffffff;
  margin-bottom: 16px;
  letter-spacing: -0.5px;
  text-shadow: 0 0 30px var(--hacker-red-glow);
  background: linear-gradient(135deg, #ff3b30 0%, #ff6b59 100%);
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  filter: drop-shadow(0 0 20px var(--hacker-red-glow));
}

.subtitle {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.7);
  margin: 0;
  font-weight: 500;
  line-height: 1.6;
}

/* 未开启录屏状态 */
.access-blocked {
  padding: 20px 0;
}

/* Alert 提示 */
.access-blocked :deep(.el-alert) {
  background: rgba(255, 149, 0, 0.15);
  border: 0.5px solid rgba(255, 149, 0, 0.3);
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: 30px;
}

.access-blocked :deep(.el-alert__title) {
  color: #ffffff;
  font-weight: 600;
}

.access-blocked :deep(.el-alert__content) {
  color: rgba(255, 255, 255, 0.8);
}

.access-blocked :deep(.el-alert__icon) {
  color: #ff9500;
}

.access-blocked :deep(.el-alert p) {
  margin: 8px 0 0 0;
  color: rgba(255, 255, 255, 0.8);
}

/* 录屏状态区域 */
.recording-status {
  text-align: center;
  margin: 50px 0;
}

.status-icon {
  margin-bottom: 30px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.status-icon :deep(.el-icon) {
  filter: drop-shadow(0 0 20px currentColor);
}

.recording-pulse {
  animation: pulse-red 2s cubic-bezier(0.19, 1, 0.22, 1) infinite;
}

@keyframes pulse-red {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
    filter: drop-shadow(0 0 20px #ff3b30);
  }
  50% {
    opacity: 0.6;
    transform: scale(1.1);
    filter: drop-shadow(0 0 40px #ff3b30);
  }
}

.status-text {
  font-size: 20px;
  color: rgba(255, 255, 255, 0.9);
  font-weight: 600;
  letter-spacing: -0.3px;
}

/* 操作按钮区域 */
.action-area {
  text-align: center;
  margin: 50px 0;
}

.start-btn {
  font-size: 18px;
  padding: 20px 50px;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(255, 59, 48, 0.2) 0%, rgba(255, 59, 48, 0.35) 100%);
  border: 0.5px solid rgba(255, 59, 48, 0.4);
  color: #ffffff;
  font-weight: 600;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
  box-shadow: 0 6px 20px rgba(255, 59, 48, 0.3);
}

.start-btn:hover {
  background: linear-gradient(135deg, rgba(255, 59, 48, 0.3) 0%, rgba(255, 59, 48, 0.45) 100%);
  border-color: rgba(255, 59, 48, 0.6);
  transform: translateY(-3px);
  box-shadow: 0 10px 30px rgba(255, 59, 48, 0.4);
}

.start-btn:active {
  transform: translateY(-1px);
}

.start-btn :deep(.el-icon) {
  margin-right: 8px;
}

/* 使用说明区域 */
.tips-section {
  margin-top: 50px;
  padding: 30px;
  background: rgba(30, 30, 30, 0.5);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-radius: 16px;
  border: 0.5px solid rgba(255, 59, 48, 0.15);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
}

.tips-section h3 {
  color: #ffffff;
  margin-bottom: 20px;
  font-size: 20px;
  font-weight: 600;
  letter-spacing: -0.3px;
}

.tips-list {
  margin: 20px 0;
  padding-left: 28px;
  line-height: 2;
  color: rgba(255, 255, 255, 0.85);
  list-style-type: none;
}

.tips-list li {
  margin: 12px 0;
  position: relative;
  padding-left: 10px;
}

.tips-list li::before {
  content: '▸';
  position: absolute;
  left: -18px;
  color: #ff3b30;
  font-weight: 700;
  text-shadow: 0 0 10px rgba(255, 59, 48, 0.5);
}

.tips-list strong {
  color: #ff6b59;
  font-weight: 600;
}

/* 浏览器兼容提示 */
.browser-tip {
  margin-top: 25px;
  background: rgba(0, 122, 255, 0.12);
  border: 0.5px solid rgba(0, 122, 255, 0.25);
}

.browser-tip :deep(.el-alert__content) {
  color: rgba(255, 255, 255, 0.85);
}

.browser-tip :deep(.el-alert__icon) {
  color: #007aff;
}

.browser-tip strong {
  color: #ffffff;
  font-weight: 600;
}

.browser-tip ul {
  margin: 12px 0 0 0;
  padding-left: 24px;
  list-style: none;
}

.browser-tip li {
  margin: 8px 0;
  color: rgba(255, 255, 255, 0.85);
}

/* 已开启录屏状态 */
.access-granted {
  padding: 40px 20px;
}

.access-granted :deep(.el-result) {
  padding: 20px;
}

.access-granted :deep(.el-result__icon svg) {
  width: 80px;
  height: 80px;
  fill: #34c759;
  filter: drop-shadow(0 0 20px rgba(52, 199, 89, 0.5));
}

.access-granted :deep(.el-result__title) {
  margin-top: 20px;
}

.access-granted :deep(.el-result__title p) {
  font-size: 28px;
  font-weight: 700;
  color: #ffffff;
  letter-spacing: -0.3px;
}

.access-granted :deep(.el-result__subtitle) {
  margin-top: 12px;
}

.access-granted :deep(.el-result__subtitle p) {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.7);
}

/* 录制信息 */
.recording-info {
  display: flex;
  justify-content: center;
  gap: 50px;
  margin: 40px 0;
  padding: 30px;
  background: rgba(30, 30, 30, 0.5);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-radius: 16px;
  border: 0.5px solid rgba(255, 59, 48, 0.2);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
}

.info-item {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 16px;
}

.info-item .label {
  color: rgba(255, 255, 255, 0.7);
  font-weight: 600;
  font-size: 15px;
}

.info-item :deep(.el-tag) {
  background: rgba(255, 59, 48, 0.2);
  border-color: rgba(255, 59, 48, 0.4);
  color: #ff3b30;
  font-weight: 600;
  padding: 8px 16px;
  font-size: 15px;
}

.duration {
  font-size: 32px;
  font-weight: 700;
  color: #ff3b30;
  font-family: var(--font-mono);
  letter-spacing: 1px;
  text-shadow: 0 0 20px rgba(255, 59, 48, 0.5);
}

.recording-dot {
  margin-right: 6px;
  animation: blink-red 1.5s cubic-bezier(0.19, 1, 0.22, 1) infinite;
}

@keyframes blink-red {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.4;
    transform: scale(1.15);
  }
}

/* 操作按钮组 */
.action-buttons {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 40px;
}

.action-buttons :deep(.el-button) {
  font-size: 17px;
  padding: 16px 40px;
  border-radius: 12px;
  font-weight: 600;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

.enter-btn {
  background: linear-gradient(135deg, rgba(255, 59, 48, 0.2) 0%, rgba(255, 59, 48, 0.35) 100%);
  border: 0.5px solid rgba(255, 59, 48, 0.4);
  color: #ffffff;
  box-shadow: 0 6px 20px rgba(255, 59, 48, 0.3);
}

.enter-btn:hover {
  background: linear-gradient(135deg, rgba(255, 59, 48, 0.3) 0%, rgba(255, 59, 48, 0.45) 100%);
  border-color: rgba(255, 59, 48, 0.6);
  transform: translateY(-3px);
  box-shadow: 0 10px 30px rgba(255, 59, 48, 0.4);
}

.action-buttons :deep(.el-button:not(.enter-btn)) {
  background: linear-gradient(135deg, rgba(142, 142, 147, 0.15) 0%, rgba(142, 142, 147, 0.25) 100%);
  border: 0.5px solid rgba(142, 142, 147, 0.3);
  color: #ffffff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.action-buttons :deep(.el-button:not(.enter-btn):hover) {
  background: linear-gradient(135deg, rgba(142, 142, 147, 0.25) 0%, rgba(142, 142, 147, 0.35) 100%);
  border-color: rgba(142, 142, 147, 0.5);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.3);
}

/* Loading 状态 */
:deep(.el-loading-mask) {
  background-color: rgba(10, 10, 10, 0.8);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

:deep(.el-loading-spinner .circular) {
  stroke: #ff3b30;
}

:deep(.el-loading-spinner .el-loading-text) {
  color: rgba(255, 255, 255, 0.8);
}

/* 响应式调整 */
@media (max-width: 768px) {
  .gateway-card :deep(.el-card__body) {
    padding: 30px 20px;
  }

  .gateway-header h1 {
    font-size: 28px;
  }

  .subtitle {
    font-size: 14px;
  }

  .recording-info {
    flex-direction: column;
    gap: 20px;
  }

  .action-buttons {
    flex-direction: column;
    gap: 15px;
  }

  .action-buttons :deep(.el-button) {
    width: 100%;
  }

  .tips-section {
    padding: 20px;
  }
}
</style>
