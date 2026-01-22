<template>
  <div class="recording-window">
    <!-- 动态光晕背景层 -->
    <div class="dynamic-glow-layer">
      <div class="glow-spot glow-1"></div>
      <div class="glow-spot glow-2"></div>
      <div class="glow-spot glow-3"></div>
    </div>

    <!-- 头部信息 -->
    <div class="recording-header">
      <div class="logo-section">
        <span class="logo-icon">🔴</span>
        <span class="app-name">CyberLab 录屏窗口</span>
      </div>
      <div class="status-section" v-if="isRecording">
        <span class="recording-indicator">● REC</span>
        <span class="duration">{{ formatDuration(recordingDuration) }}</span>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="recording-content">
      <!-- 未开始录制状态 -->
      <div v-if="!isRecording && !error" class="init-state">
        <div class="icon-wrapper">
          <el-icon :size="80" color="#409EFF">
            <VideoCamera />
          </el-icon>
        </div>
        <h2>准备开始录屏</h2>
        <p class="instruction">点击下方按钮，浏览器将弹出权限请求</p>
        <p class="instruction secondary">请选择要共享的屏幕或窗口</p>
        <el-button type="primary" size="large" @click="startRecording" :loading="isStarting">
          <el-icon><VideoCameraFilled /></el-icon>
          开始录屏
        </el-button>
      </div>

      <!-- 录制中状态 -->
      <div v-if="isRecording" class="recording-state">
        <div class="preview-container">
          <video ref="previewVideo" autoplay muted playsinline class="preview-video"></video>
          <div class="preview-overlay">
            <div class="recording-badge">
              <span class="pulse-dot"></span>
              正在录制
            </div>
          </div>
        </div>

        <div class="recording-info">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="录制时长">
              {{ formatDuration(recordingDuration) }}
            </el-descriptions-item>
            <el-descriptions-item label="会话ID">
              {{ sessionId || '未知' }}
            </el-descriptions-item>
            <el-descriptions-item label="用户">
              {{ username }}
            </el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag type="danger" effect="dark">录制中</el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="control-buttons">
          <el-button type="danger" size="large" @click="stopRecording" :loading="isStopping">
            <el-icon><VideoPause /></el-icon>
            停止录屏
          </el-button>
          <el-button size="large" @click="closeWindow">
            <el-icon><Close /></el-icon>
            关闭窗口（自动停止）
          </el-button>
        </div>
      </div>

      <!-- 错误状态 -->
      <div v-if="error" class="error-state">
        <div class="icon-wrapper">
          <el-icon :size="80" color="#F56C6C">
            <CircleClose />
          </el-icon>
        </div>
        <h2>录屏失败</h2>
        <p class="error-message">{{ error }}</p>
        <div class="error-actions">
          <el-button type="primary" @click="retryRecording">
            <el-icon><RefreshRight /></el-icon>
            重试
          </el-button>
          <el-button @click="closeWindow">
            <el-icon><Close /></el-icon>
            关闭窗口
          </el-button>
        </div>
      </div>
    </div>

    <!-- 底部提示 -->
    <div class="recording-footer">
      <el-alert
        v-if="!isRecording"
        type="info"
        :closable="false"
        show-icon
      >
        <template #title>
          <span class="footer-tip">此窗口用于录制屏幕，录制开始后您可以切换到主窗口进行操作</span>
        </template>
      </el-alert>
      <el-alert
        v-else
        type="warning"
        :closable="false"
        show-icon
      >
        <template #title>
          <span class="footer-tip">录屏进行中，关闭此窗口将自动停止录制</span>
        </template>
      </el-alert>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { VideoCamera, VideoCameraFilled, VideoPause, Close, CircleClose, RefreshRight } from '@element-plus/icons-vue'
import { useRecordingChannel } from '@/composables/useRecordingChannel'
import { getUserId, getUsername, getUserRole } from '@/utils/auth'
import axios from '@/api/axios'

const {
  notifyRecordingStarted,
  notifyRecordingStopped,
  notifyRecordingDuration,
  notifyRecordingError,
  onMessage
} = useRecordingChannel()

const isRecording = ref(false)
const isStarting = ref(false)
const isStopping = ref(false)
const sessionId = ref(null)
const username = ref('')
const error = ref(null)
const recordingDuration = ref(0)

const previewVideo = ref(null)

let mediaRecorder = null
let mediaStream = null
let chunkIndex = 0
let durationTimer = null

// 获取当前用户信息
const getCurrentUser = () => {
  const id = getUserId()
  const name = getUsername()
  const role = getUserRole()

  if (!id) {
    return null
  }

  return { id, username: name, role }
}

// 格式化时长
const formatDuration = (seconds) => {
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const secs = seconds % 60

  if (hours > 0) {
    return `${hours}:${String(minutes).padStart(2, '0')}:${String(secs).padStart(2, '0')}`
  }
  return `${minutes}:${String(secs).padStart(2, '0')}`
}

// 开始录屏
const startRecording = async () => {
  try {
    isStarting.value = true
    error.value = null

    const user = getCurrentUser()
    if (!user) {
      throw new Error('未登录，无法开始录屏')
    }

    username.value = user.username

    // 1. 检查浏览器支持
    if (!navigator.mediaDevices || !navigator.mediaDevices.getDisplayMedia) {
      throw new Error('您的浏览器不支持屏幕录制功能')
    }

    // 2. 请求屏幕录制权限
    ElMessage.info('请选择要共享的屏幕或窗口')

    mediaStream = await navigator.mediaDevices.getDisplayMedia({
      video: {
        width: { ideal: 1920 },
        height: { ideal: 1080 },
        frameRate: { ideal: 30 }
      },
      audio: {
        echoCancellation: true,
        noiseSuppression: true
      }
    })

    // 3. 先设置录制状态为 true，让 video 元素渲染到 DOM
    isRecording.value = true
    console.log('📹 设置录制状态: isRecording = true')

    // 4. 等待 DOM 更新完成
    await nextTick()
    console.log('⏳ DOM 更新完成，准备设置视频预览')

    // 5. 显示预览（此时 video 元素已经在 DOM 中）
    if (previewVideo.value) {
      console.log('✅ video 元素已找到，开始设置 srcObject')
      previewVideo.value.srcObject = mediaStream

      // 手动调用 play() 确保视频播放
      try {
        await previewVideo.value.play()
        console.log('✅ 视频预览已启动并播放')
      } catch (playError) {
        console.warn('⚠️ 视频预览播放失败:', playError)
        // 不影响录制功能，仅记录警告
      }
    } else {
      console.error('❌ video 元素未找到，previewVideo.value 是 null')
    }

    // 6. 通知后端开始录屏
    const formData = new URLSearchParams()
    formData.append('userId', user.id)
    formData.append('username', user.username)

    const response = await axios.post('/screen-recording/start', formData, {
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
    })

    if (!response.success) {
      throw new Error(response.message || '启动录屏失败')
    }

    sessionId.value = response.sessionId

    // 5. 创建MediaRecorder
    const options = {
      mimeType: 'video/webm;codecs=vp9',
      videoBitsPerSecond: 2500000
    }

    if (!MediaRecorder.isTypeSupported(options.mimeType)) {
      options.mimeType = 'video/webm;codecs=vp8'
      if (!MediaRecorder.isTypeSupported(options.mimeType)) {
        options.mimeType = 'video/webm'
      }
    }

    mediaRecorder = new MediaRecorder(mediaStream, options)

    // 6. 处理录制数据
    mediaRecorder.ondataavailable = async (event) => {
      if (event.data && event.data.size > 0) {
        await uploadChunk(event.data, chunkIndex++)
      }
    }

    // 7. 监听用户通过浏览器停止共享
    mediaStream.getVideoTracks()[0].onended = () => {
      console.log('用户通过浏览器停止了屏幕共享')
      stopRecording()
    }

    // 8. 开始录制
    mediaRecorder.start(20000) // 每20秒上传一次

    // 9. 启动计时器
    startDurationTimer()

    // 10. 通知主窗口
    notifyRecordingStarted(sessionId.value, new Date().toISOString())

    ElMessage.success('录屏已开始，您可以切换到主窗口进行操作')

  } catch (err) {
    console.error('录屏启动失败:', err)

    if (err.name === 'NotAllowedError') {
      error.value = '您拒绝了屏幕共享权限'
      ElMessage.warning('您拒绝了屏幕共享权限')
    } else if (err.name === 'NotFoundError') {
      error.value = '未找到可用的屏幕录制设备'
      ElMessage.error('未找到可用的屏幕录制设备')
    } else {
      error.value = err.message || '录屏启动失败'
      ElMessage.error('录屏启动失败: ' + (err.message || '未知错误'))
    }

    notifyRecordingError(err)

    // 清理资源
    if (mediaStream) {
      mediaStream.getTracks().forEach(track => track.stop())
    }
  } finally {
    isStarting.value = false
  }
}

// 停止录屏
const stopRecording = async () => {
  if (!isRecording.value) return

  try {
    isStopping.value = true

    // 1. 停止MediaRecorder
    if (mediaRecorder && mediaRecorder.state !== 'inactive') {
      mediaRecorder.stop()
      await new Promise(resolve => setTimeout(resolve, 1000))
    }

    // 2. 停止媒体流
    if (mediaStream) {
      mediaStream.getTracks().forEach(track => track.stop())
    }

    // 3. 通知后端停止
    if (sessionId.value) {
      const formData = new URLSearchParams()
      formData.append('sessionId', sessionId.value)
      formData.append('endTime', new Date().toISOString())

      await axios.post('/screen-recording/stop', formData, {
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
      })
    }

    // 4. 停止计时器
    stopDurationTimer()

    // 5. 通知主窗口
    notifyRecordingStopped(sessionId.value)

    // 6. 重置状态
    isRecording.value = false

    ElMessage.success('录屏已停止并保存，您可以手动关闭此窗口')

    // 不再自动关闭窗口，允许用户查看录制信息后手动关闭
    // setTimeout(() => {
    //   window.close()
    // }, 3000)

  } catch (err) {
    console.error('停止录屏失败:', err)
    ElMessage.error('停止录屏失败: ' + (err.message || '未知错误'))
  } finally {
    isStopping.value = false
  }
}

// 上传分片
const uploadChunk = async (blob, index) => {
  try {
    // 创建新的Blob以确保MIME类型正确
    const videoBlob = new Blob([blob], { type: 'video/webm' })

    const formData = new FormData()
    // 注意：参数顺序必须与后端Controller一致
    // 后端Controller参数顺序：sessionId, chunkIndex, file, timestamp
    formData.append('sessionId', sessionId.value)
    formData.append('chunkIndex', index)
    formData.append('file', videoBlob, `chunk_${index}.webm`)
    formData.append('timestamp', Date.now().toString())

    console.log(`📤 上传分片 ${index}:`, {
      sessionId: sessionId.value,
      chunkIndex: index,
      blobSize: blob.size,
      timestamp: Date.now()
    })

    await axios.post('/screen-recording/upload-chunk', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 60000
    })

    console.log(`✅ 分片 ${index} 上传成功`)
  } catch (err) {
    console.error(`❌ 分片 ${index} 上传失败:`, err)
    console.error('错误详情:', {
      message: err.message,
      response: err.response?.data,
      status: err.response?.status
    })
    // 不显示过多的错误提示，避免打扰用户
    if (index === 0) {
      ElMessage.error(`分片上传失败，请检查网络连接`)
    }
  }
}

// 启动计时器
const startDurationTimer = () => {
  recordingDuration.value = 0
  durationTimer = setInterval(() => {
    recordingDuration.value++
    // 每秒通知主窗口更新时长
    notifyRecordingDuration(recordingDuration.value)
  }, 1000)
}

// 停止计时器
const stopDurationTimer = () => {
  if (durationTimer) {
    clearInterval(durationTimer)
    durationTimer = null
    recordingDuration.value = 0
  }
}

// 重试录屏
const retryRecording = () => {
  error.value = null
  startRecording()
}

// 关闭窗口
const closeWindow = async () => {
  if (isRecording.value) {
    await stopRecording()
  }
  window.close()
}

// 监听主窗口的停止请求
onMounted(() => {
  // 监听主窗口发送的停止请求
  const cleanup = onMessage((message) => {
    if (message.type === 'stop-recording-request') {
      stopRecording()
    }
  })

  // 窗口关闭前自动停止录屏
  window.addEventListener('beforeunload', async (event) => {
    if (isRecording.value) {
      event.preventDefault()
      await stopRecording()
    }
  })

  // 组件卸载时清理
  onUnmounted(() => {
    if (cleanup) cleanup()
    stopDurationTimer()
    if (mediaStream) {
      mediaStream.getTracks().forEach(track => track.stop())
    }
  })
})
</script>

<style scoped>
/* ============================================
   录屏窗口 - 深色黑客科技风 + Apple优雅
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

.recording-window {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: linear-gradient(135deg, #0a0a0a 0%, #1a0d0d 50%, #0f0f0f 100%);
  color: #fff;
  font-family: var(--font-apple);
  position: relative;
  overflow: hidden;
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

/* 头部区域 */
.recording-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 30px;
  background: var(--hacker-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(255, 59, 48, 0.2);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
  position: relative;
  z-index: 10;
}

.logo-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-icon {
  font-size: 28px;
  animation: pulse-red 2s cubic-bezier(0.19, 1, 0.22, 1) infinite;
  filter: drop-shadow(0 0 15px #ff3b30);
}

@keyframes pulse-red {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.7;
    transform: scale(1.1);
  }
}

.app-name {
  font-size: 22px;
  font-weight: 700;
  color: #ffffff;
  letter-spacing: -0.5px;
  text-shadow: 0 0 20px var(--hacker-red-glow);
}

.status-section {
  display: flex;
  align-items: center;
  gap: 20px;
}

.recording-indicator {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: rgba(255, 59, 48, 0.2);
  border: 0.5px solid rgba(255, 59, 48, 0.5);
  border-radius: 25px;
  font-weight: 600;
  font-size: 14px;
  color: #ff3b30;
  animation: pulse-indicator 2s cubic-bezier(0.19, 1, 0.22, 1) infinite;
  box-shadow: 0 0 20px rgba(255, 59, 48, 0.4);
}

@keyframes pulse-indicator {
  0%, 100% {
    box-shadow: 0 0 20px rgba(255, 59, 48, 0.4);
    border-color: rgba(255, 59, 48, 0.5);
  }
  50% {
    box-shadow: 0 0 30px rgba(255, 59, 48, 0.6);
    border-color: rgba(255, 59, 48, 0.7);
  }
}

.duration {
  font-size: 22px;
  font-weight: 700;
  font-family: var(--font-mono);
  color: #ffffff;
  letter-spacing: 1px;
  text-shadow: 0 0 15px rgba(255, 59, 48, 0.5);
}

/* 主内容区 */
.recording-content {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  position: relative;
  z-index: 1;
}

/* 未开始状态 */
.init-state,
.error-state {
  text-align: center;
  max-width: 600px;
}

.icon-wrapper {
  margin-bottom: 40px;
}

.icon-wrapper :deep(.el-icon) {
  filter: drop-shadow(0 0 25px currentColor);
}

h2 {
  font-size: 36px;
  margin-bottom: 20px;
  font-weight: 700;
  color: #ffffff;
  letter-spacing: -0.5px;
}

.instruction {
  font-size: 17px;
  margin-bottom: 12px;
  color: rgba(255, 255, 255, 0.9);
  font-weight: 500;
}

.instruction.secondary {
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 40px;
  font-size: 15px;
}

.init-state :deep(.el-button),
.error-actions :deep(.el-button) {
  font-size: 18px;
  padding: 18px 45px;
  border-radius: 12px;
  font-weight: 600;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

.init-state :deep(.el-button--primary) {
  background: linear-gradient(135deg, rgba(255, 59, 48, 0.2) 0%, rgba(255, 59, 48, 0.35) 100%);
  border: 0.5px solid rgba(255, 59, 48, 0.4);
  color: #ffffff;
  box-shadow: 0 6px 20px rgba(255, 59, 48, 0.3);
}

.init-state :deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, rgba(255, 59, 48, 0.3) 0%, rgba(255, 59, 48, 0.45) 100%);
  border-color: rgba(255, 59, 48, 0.6);
  transform: translateY(-3px);
  box-shadow: 0 10px 30px rgba(255, 59, 48, 0.4);
}

.init-state :deep(.el-button--primary .el-icon) {
  margin-right: 8px;
}

/* 错误状态 */
.error-message {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: 35px;
  padding: 20px;
  background: rgba(255, 59, 48, 0.15);
  border-radius: 12px;
  border: 0.5px solid rgba(255, 59, 48, 0.3);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  box-shadow: 0 4px 16px rgba(255, 59, 48, 0.2);
}

.error-actions {
  display: flex;
  gap: 20px;
  justify-content: center;
}

.error-actions :deep(.el-button--primary) {
  background: linear-gradient(135deg, rgba(255, 59, 48, 0.2) 0%, rgba(255, 59, 48, 0.35) 100%);
  border: 0.5px solid rgba(255, 59, 48, 0.4);
  color: #ffffff;
  box-shadow: 0 6px 20px rgba(255, 59, 48, 0.3);
}

.error-actions :deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, rgba(255, 59, 48, 0.3) 0%, rgba(255, 59, 48, 0.45) 100%);
  border-color: rgba(255, 59, 48, 0.6);
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(255, 59, 48, 0.4);
}

.error-actions :deep(.el-button:not(.el-button--primary)) {
  background: linear-gradient(135deg, rgba(142, 142, 147, 0.15) 0%, rgba(142, 142, 147, 0.25) 100%);
  border: 0.5px solid rgba(142, 142, 147, 0.3);
  color: #ffffff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.error-actions :deep(.el-button:not(.el-button--primary):hover) {
  background: linear-gradient(135deg, rgba(142, 142, 147, 0.25) 0%, rgba(142, 142, 147, 0.35) 100%);
  border-color: rgba(142, 142, 147, 0.5);
  transform: translateY(-2px);
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.3);
}

/* 录制中状态 */
.recording-state {
  width: 100%;
  max-width: 1000px;
}

.preview-container {
  position: relative;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.5), 0 0 0 1px rgba(255, 59, 48, 0.2);
  margin-bottom: 35px;
  border: 1px solid rgba(255, 59, 48, 0.15);
}

.preview-video {
  width: 100%;
  aspect-ratio: 16/9;
  display: block;
  background: #000;
  object-fit: contain;
}

.preview-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
}

.recording-badge {
  position: absolute;
  top: 24px;
  left: 24px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 24px;
  background: rgba(255, 59, 48, 0.9);
  border-radius: 30px;
  font-weight: 700;
  font-size: 15px;
  backdrop-filter: blur(15px);
  -webkit-backdrop-filter: blur(15px);
  box-shadow: 0 4px 20px rgba(255, 59, 48, 0.5);
  border: 0.5px solid rgba(255, 255, 255, 0.2);
}

.pulse-dot {
  width: 12px;
  height: 12px;
  background: #fff;
  border-radius: 50%;
  animation: pulse-dot 1.5s cubic-bezier(0.19, 1, 0.22, 1) infinite;
  box-shadow: 0 0 10px rgba(255, 255, 255, 0.8);
}

@keyframes pulse-dot {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.5;
    transform: scale(1.3);
  }
}

/* 录制信息 */
.recording-info {
  margin-bottom: 35px;
}

.recording-info :deep(.el-descriptions) {
  background: var(--hacker-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 12px;
  overflow: hidden;
  border: 0.5px solid rgba(255, 59, 48, 0.2);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
}

.recording-info :deep(.el-descriptions__header) {
  background: rgba(30, 30, 30, 0.6);
  color: #ffffff;
}

.recording-info :deep(.el-descriptions__label) {
  background: rgba(30, 30, 30, 0.6);
  color: rgba(255, 255, 255, 0.9);
  font-weight: 600;
}

.recording-info :deep(.el-descriptions__content) {
  background: rgba(20, 20, 20, 0.5);
  color: rgba(255, 255, 255, 0.85);
  font-family: var(--font-mono);
}

.recording-info :deep(.el-descriptions__cell) {
  border-color: rgba(255, 255, 255, 0.08);
}

.recording-info :deep(.el-tag) {
  background: rgba(255, 59, 48, 0.2);
  border-color: rgba(255, 59, 48, 0.4);
  color: #ff3b30;
  font-weight: 600;
}

/* 控制按钮 */
.control-buttons {
  display: flex;
  gap: 20px;
  justify-content: center;
}

.control-buttons :deep(.el-button) {
  font-size: 17px;
  padding: 16px 40px;
  border-radius: 12px;
  font-weight: 600;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

.control-buttons :deep(.el-button--danger) {
  background: linear-gradient(135deg, rgba(255, 59, 48, 0.2) 0%, rgba(255, 59, 48, 0.35) 100%);
  border: 0.5px solid rgba(255, 59, 48, 0.4);
  color: #ffffff;
  box-shadow: 0 6px 20px rgba(255, 59, 48, 0.3);
}

.control-buttons :deep(.el-button--danger:hover) {
  background: linear-gradient(135deg, rgba(255, 59, 48, 0.3) 0%, rgba(255, 59, 48, 0.45) 100%);
  border-color: rgba(255, 59, 48, 0.6);
  transform: translateY(-2px);
  box-shadow: 0 8px 28px rgba(255, 59, 48, 0.4);
}

.control-buttons :deep(.el-button:not(.el-button--danger)) {
  background: linear-gradient(135deg, rgba(142, 142, 147, 0.15) 0%, rgba(142, 142, 147, 0.25) 100%);
  border: 0.5px solid rgba(142, 142, 147, 0.3);
  color: #ffffff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.control-buttons :deep(.el-button:not(.el-button--danger):hover) {
  background: linear-gradient(135deg, rgba(142, 142, 147, 0.25) 0%, rgba(142, 142, 147, 0.35) 100%);
  border-color: rgba(142, 142, 147, 0.5);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.3);
}

.control-buttons :deep(.el-button .el-icon) {
  margin-right: 6px;
}

/* 底部提示 */
.recording-footer {
  padding: 20px 30px;
  background: var(--hacker-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-top: 1px solid rgba(255, 59, 48, 0.2);
  box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.3);
  position: relative;
  z-index: 10;
}

.recording-footer :deep(.el-alert) {
  background: rgba(0, 122, 255, 0.12);
  border: 0.5px solid rgba(0, 122, 255, 0.25);
  color: rgba(255, 255, 255, 0.9);
}

.recording-footer :deep(.el-alert--warning) {
  background: rgba(255, 149, 0, 0.12);
  border-color: rgba(255, 149, 0, 0.25);
}

.recording-footer :deep(.el-alert__title) {
  color: #ffffff;
}

.recording-footer :deep(.el-alert__icon) {
  color: inherit;
}

.footer-tip {
  color: #fff;
  font-size: 14px;
  font-weight: 500;
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
  .recording-header {
    padding: 15px 20px;
  }

  .app-name {
    font-size: 18px;
  }

  .recording-content {
    padding: 30px 20px;
  }

  h2 {
    font-size: 28px;
  }

  .control-buttons {
    flex-direction: column;
    gap: 15px;
  }

  .control-buttons :deep(.el-button) {
    width: 100%;
  }
}
</style>
