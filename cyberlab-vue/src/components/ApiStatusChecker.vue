<template>
  <div class="api-status-checker" v-if="showStatus">
    <el-alert
      :title="statusMessage"
      :type="statusType"
      :closable="false"
      show-icon
      class="status-alert">
      <template #default>
        <div class="status-details">
          <p>{{ statusMessage }}</p>
          <div class="status-actions" v-if="!isConnected">
            <el-button size="small" @click="retryConnection">重试连接</el-button>
            <el-button size="small" type="info" @click="useMockData">使用模拟数据</el-button>
          </div>
        </div>
      </template>
    </el-alert>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElAlert, ElButton, ElMessage } from 'element-plus'
import axios from 'axios'

const showStatus = ref(false)
const isConnected = ref(true)
const statusMessage = ref('')
const statusType = ref('success')

const checkApiStatus = async () => {
  try {
    // 检查后端API连接
    await axios.get('/health', { timeout: 3000 })
    isConnected.value = true
    statusMessage.value = 'API连接正常'
    statusType.value = 'success'
    showStatus.value = false
  } catch (error) {
    isConnected.value = false
    
    if (error.response?.status === 403) {
      statusMessage.value = 'API权限验证失败，已自动切换到模拟数据模式'
      statusType.value = 'warning'
    } else if (error.code === 'ECONNREFUSED' || !error.response) {
      statusMessage.value = 'API服务器连接失败，请检查后端服务是否启动'
      statusType.value = 'error'
    } else {
      statusMessage.value = `API连接异常: ${error.message}`
      statusType.value = 'error'
    }
    
    showStatus.value = true
  }
}

const retryConnection = async () => {
  ElMessage.info('正在重试连接...')
  await checkApiStatus()
}

const useMockData = () => {
  showStatus.value = false
  ElMessage.success('已切换到模拟数据模式')
}

onMounted(() => {
  checkApiStatus()
})

defineExpose({
  checkApiStatus
})
</script>

<style scoped>
.api-status-checker {
  margin-bottom: 16px;
}

.status-alert {
  border-radius: 6px;
}

.status-details {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.status-actions {
  display: flex;
  gap: 8px;
}
</style>