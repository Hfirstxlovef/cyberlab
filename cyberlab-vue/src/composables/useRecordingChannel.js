import { ref, onUnmounted } from 'vue'

/**
 * 录屏窗口间通信组件
 * 使用BroadcastChannel API实现主窗口与录屏窗口的双向通信
 */
export function useRecordingChannel() {
  const isRecording = ref(false)
  const sessionId = ref(null)
  const recordingDuration = ref(0)

  // 创建广播频道
  let channel = null
  try {
    channel = new BroadcastChannel('cyberlab-recording-channel')
  } catch (error) {
    console.error('BroadcastChannel不可用，使用localStorage作为备选方案')
  }

  /**
   * 发送消息到其他窗口
   * @param {string} type - 消息类型
   * @param {object} data - 消息数据
   */
  const sendMessage = (type, data = {}) => {
    const message = {
      type,
      data,
      timestamp: Date.now()
    }

    if (channel) {
      channel.postMessage(message)
    } else {
      // 备选方案：使用localStorage + storage事件
      localStorage.setItem('recording-message', JSON.stringify(message))
      localStorage.removeItem('recording-message') // 立即清除，触发事件
    }
  }

  /**
   * 监听来自其他窗口的消息
   * @param {function} callback - 消息处理回调
   */
  const onMessage = (callback) => {
    if (channel) {
      channel.onmessage = (event) => {
        callback(event.data)
      }
    } else {
      // 备选方案：监听storage事件
      const storageHandler = (event) => {
        if (event.key === 'recording-message' && event.newValue) {
          try {
            const message = JSON.parse(event.newValue)
            callback(message)
          } catch (error) {
            console.error('解析录屏消息失败:', error)
          }
        }
      }
      window.addEventListener('storage', storageHandler)

      // 返回清理函数
      return () => {
        window.removeEventListener('storage', storageHandler)
      }
    }
  }

  /**
   * 主窗口：打开录屏窗口
   * @returns {Window} 录屏窗口引用
   */
  const openRecordingWindow = () => {
    const width = 800
    const height = 600
    const left = (screen.width - width) / 2
    const top = (screen.height - height) / 2

    const features = `
      width=${width},
      height=${height},
      left=${left},
      top=${top},
      resizable=yes,
      scrollbars=yes,
      status=yes
    `.replace(/\s+/g, '')

    const recordingWindow = window.open(
      '/recording-window',
      'CyberLabRecording',
      features
    )

    if (!recordingWindow) {
      throw new Error('无法打开录屏窗口，请检查浏览器弹窗拦截设置')
    }

    return recordingWindow
  }

  /**
   * 处理录屏状态更新
   */
  const handleRecordingStatus = (message) => {
    switch (message.type) {
      case 'recording-started':
        isRecording.value = true
        sessionId.value = message.data.sessionId
        console.log('✅ 录屏已开始:', message.data)
        break

      case 'recording-stopped':
        isRecording.value = false
        sessionId.value = null
        recordingDuration.value = 0
        console.log('⏹️ 录屏已停止')
        break

      case 'recording-duration':
        recordingDuration.value = message.data.duration
        break

      case 'recording-error':
        isRecording.value = false
        sessionId.value = null
        console.error('❌ 录屏错误:', message.data.error)
        break

      default:
        console.log('未知消息类型:', message.type)
    }
  }

  /**
   * 主窗口：启动录屏流程
   */
  const startRecording = async () => {
    try {
      // 打开录屏窗口
      const recordingWindow = openRecordingWindow()

      // 监听录屏窗口的状态消息
      const cleanup = onMessage(handleRecordingStatus)

      // 保存清理函数以便后续使用
      if (cleanup) {
        onUnmounted(cleanup)
      }

      return recordingWindow
    } catch (error) {
      console.error('启动录屏失败:', error)
      throw error
    }
  }

  /**
   * 主窗口：请求停止录屏
   */
  const stopRecording = () => {
    sendMessage('stop-recording-request')
  }

  /**
   * 录屏窗口：通知已开始录屏
   */
  const notifyRecordingStarted = (sessionId, startTime) => {
    sendMessage('recording-started', { sessionId, startTime })
  }

  /**
   * 录屏窗口：通知已停止录屏
   */
  const notifyRecordingStopped = (sessionId) => {
    sendMessage('recording-stopped', { sessionId })
  }

  /**
   * 录屏窗口：更新录屏时长
   */
  const notifyRecordingDuration = (duration) => {
    sendMessage('recording-duration', { duration })
  }

  /**
   * 录屏窗口：通知录屏错误
   */
  const notifyRecordingError = (error) => {
    sendMessage('recording-error', { error: error.message || String(error) })
  }

  // 组件卸载时关闭频道
  onUnmounted(() => {
    if (channel) {
      channel.close()
    }
  })

  return {
    // 状态
    isRecording,
    sessionId,
    recordingDuration,

    // 主窗口方法
    startRecording,
    stopRecording,
    openRecordingWindow,

    // 录屏窗口方法
    notifyRecordingStarted,
    notifyRecordingStopped,
    notifyRecordingDuration,
    notifyRecordingError,

    // 通用方法
    sendMessage,
    onMessage,
    handleRecordingStatus
  }
}
