import { ref, onMounted, onUnmounted } from 'vue'
import { createBigScreenWebSocket } from '@/utils/websocket'

/**
 * 大屏WebSocket数据推送Composable
 * 使用方法:
 * const { data, connected, error } = useBigScreenWebSocket()
 */
export function useBigScreenWebSocket() {
  const data = ref(null)
  const connected = ref(false)
  const error = ref(null)
  let wsClient = null

  const handleMessage = (message) => {
    data.value = message
    error.value = null
  }

  const handleError = (err) => {
    error.value = err
    connected.value = false
  }

  const handleOpen = () => {
    connected.value = true
    error.value = null
  }

  const handleClose = () => {
    connected.value = false
  }

  onMounted(() => {
    wsClient = createBigScreenWebSocket(handleMessage, handleError)
    wsClient.options.onOpen = handleOpen
    wsClient.options.onClose = handleClose
  })

  onUnmounted(() => {
    if (wsClient) {
      wsClient.close()
    }
  })

  return {
    data,
    connected,
    error
  }
}
