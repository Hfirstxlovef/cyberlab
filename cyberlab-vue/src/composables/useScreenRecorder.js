import { ref, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import axios from '@/api/axios'
import { getUserId, getUsername, getUserRole } from '@/utils/auth'
import { useRecordingChannel } from './useRecordingChannel'

/**
 * 屏幕录制组件 - 新窗口模式
 * 通过打开独立录屏窗口，使用浏览器权限（而非系统权限）
 */
export function useScreenRecorder() {
  const isRecording = ref(false)
  const canAccessProject = ref(false)
  const sessionId = ref(null)
  const recordingDuration = ref(0)

  // 录屏窗口引用
  let recordingWindow = null

  // 使用窗口通信组件
  const {
    startRecording: openRecordingWindow,
    stopRecording: requestStopRecording,
    onMessage,
    handleRecordingStatus
  } = useRecordingChannel()

  // 当前用户信息（从 auth 工具函数获取）
  const getCurrentUser = () => {
    const id = getUserId()
    const username = getUsername()
    const role = getUserRole()

    // 只有当 id 存在时才返回用户对象
    if (!id) {
      return null
    }

    return { id, username, role }
  }

  /**
   * 启动录屏（打开新窗口）
   */
  async function startRecording() {
    try {
      const user = getCurrentUser()
      if (!user) {
        ElMessage.error('未登录，无法开始录屏')
        return false
      }

      // 1. 检查浏览器是否支持必要的API
      if (!navigator.mediaDevices || !navigator.mediaDevices.getDisplayMedia) {
        ElMessage.error('您的浏览器不支持屏幕录制功能，请使用 Chrome、Edge 或 Firefox 浏览器')
        return false
      }

      // 2. 检查是否在安全上下文（HTTPS 或 localhost）
      const isSecureContext = window.isSecureContext || window.location.protocol === 'https:' ||
                              window.location.hostname === 'localhost'

      if (!isSecureContext) {
        ElMessage.error('屏幕录制需要在安全环境下运行，请使用 https:// 或 http://localhost 访问')
        console.warn('当前访问地址:', window.location.href, '不是安全上下文，无法使用屏幕录制')
        return false
      }

      // 3. 打开录屏窗口
      try {
        recordingWindow = await openRecordingWindow()
        ElMessage.success('录屏窗口已打开，请在新窗口中授权并开始录制')
      } catch (error) {
        ElMessage.error('无法打开录屏窗口，请检查浏览器弹窗拦截设置')
        console.error('打开录屏窗口失败:', error)
        return false
      }

      // 4. 监听录屏窗口的状态更新
      setupMessageListener()

      return true

    } catch (error) {
      console.error('录屏启动失败:', error)
      ElMessage.error('录屏启动失败: ' + (error.message || '未知错误'))
      return false
    }
  }

  /**
   * 设置消息监听器
   */
  function setupMessageListener() {
    onMessage((message) => {
      switch (message.type) {
        case 'recording-started':
          isRecording.value = true
          canAccessProject.value = true
          sessionId.value = message.data.sessionId
          ElMessage.success('🔴 录屏已开启，现在可以访问攻击项目')
          console.log('✅ 录屏已开始:', message.data)
          break

        case 'recording-stopped':
          isRecording.value = false
          canAccessProject.value = false
          sessionId.value = null
          recordingDuration.value = 0
          ElMessage.info('录屏已停止')
          console.log('⏹️ 录屏已停止')
          break

        case 'recording-duration':
          recordingDuration.value = message.data.duration
          break

        case 'recording-error':
          isRecording.value = false
          canAccessProject.value = false
          sessionId.value = null
          ElMessage.error('录屏错误: ' + message.data.error)
          console.error('❌ 录屏错误:', message.data.error)
          break

        default:
          console.log('未知消息类型:', message.type)
      }
    })
  }

  /**
   * 停止录屏
   */
  async function stopRecording() {
    if (!isRecording.value) {
      ElMessage.warning('当前没有正在进行的录屏')
      return
    }

    try {
      // 请求录屏窗口停止录制
      requestStopRecording()
      ElMessage.info('正在停止录屏...')
    } catch (error) {
      console.error('停止录屏失败:', error)
      ElMessage.error('停止录屏失败: ' + (error.message || '未知错误'))
    }
  }

  /**
   * 检查录屏状态（从服务器）
   */
  async function checkRecordingStatus() {
    try {
      const user = getCurrentUser()
      if (!user || !user.id) {
        console.warn('用户未登录或 user.id 为空，无法检查录屏状态')
        isRecording.value = false
        canAccessProject.value = false
        return false
      }

      // axios 拦截器已经返回 response.data，所以直接接收即可
      const response = await axios.get(`/screen-recording/status/${user.id}`)
      isRecording.value = response.isRecording
      canAccessProject.value = response.isRecording

      if (response.isRecording && response.sessionId) {
        sessionId.value = response.sessionId
      }

      return response.isRecording
    } catch (error) {
      console.error('检查录屏状态失败:', error)
      isRecording.value = false
      canAccessProject.value = false
      return false
    }
  }

  /**
   * 格式化时长显示
   */
  function formatDuration(seconds) {
    const hours = Math.floor(seconds / 3600)
    const minutes = Math.floor((seconds % 3600) / 60)
    const secs = seconds % 60

    if (hours > 0) {
      return `${hours}:${String(minutes).padStart(2, '0')}:${String(secs).padStart(2, '0')}`
    }
    return `${minutes}:${String(secs).padStart(2, '0')}`
  }

  // 页面卸载时清理
  onUnmounted(() => {
    // 如果录屏窗口还在，关闭它
    if (recordingWindow && !recordingWindow.closed) {
      recordingWindow.close()
    }
  })

  return {
    isRecording,
    canAccessProject,
    recordingDuration,
    sessionId,
    startRecording,
    stopRecording,
    checkRecordingStatus,
    formatDuration
  }
}
