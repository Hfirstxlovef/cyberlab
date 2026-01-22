<template>
  <div class="auth-debugger">
    <h3>认证状态调试</h3>
    <div class="debug-info">
      <div class="info-item">
        <label>Token存在:</label>
        <span :class="{ success: hasToken, error: !hasToken }">
          {{ hasToken ? '是' : '否' }}
        </span>
      </div>
      <div class="info-item" v-if="hasToken">
        <label>Token前缀:</label>
        <span>{{ tokenPreview }}</span>
      </div>
      <div class="info-item">
        <label>用户角色:</label>
        <span>{{ userRole || '未设置' }}</span>
      </div>
      <div class="info-item">
        <label>用户名:</label>
        <span>{{ username || '未设置' }}</span>
      </div>
      <div class="info-item">
        <label>已登录:</label>
        <span :class="{ success: isAuthenticated, error: !isAuthenticated }">
          {{ isAuthenticated ? '是' : '否' }}
        </span>
      </div>
    </div>
    <div class="actions">
      <el-button @click="testAchievementApi" type="primary" size="small">
        测试成果API
      </el-button>
      <el-button @click="clearAuth" type="danger" size="small">
        清除认证信息
      </el-button>
      <el-button @click="mockLogin" type="success" size="small">
        模拟管理员登录
      </el-button>
    </div>
    <div v-if="apiTestResult" class="api-result">
      <h4>API测试结果：</h4>
      <pre>{{ apiTestResult }}</pre>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getToken, getUserRole, getUsername, isLoggedIn, clearUserInfo, setToken, setUserRole, setUsername } from '@/utils/auth'
import axios from '@/api/axios'
import { ElMessage } from 'element-plus'

const apiTestResult = ref('')

const hasToken = computed(() => !!getToken())
const tokenPreview = computed(() => {
  const token = getToken()
  return token ? token.substring(0, 20) + '...' : ''
})
const userRole = computed(() => getUserRole())
const username = computed(() => getUsername())
const isAuthenticated = computed(() => isLoggedIn())

const testAchievementApi = async () => {
  try {
    const response = await axios.get('/achievements/4')
    apiTestResult.value = JSON.stringify(response, null, 2)
    ElMessage.success('API调用成功')
  } catch (error) {
    apiTestResult.value = JSON.stringify({
      error: error.message,
      status: error.response?.status,
      statusText: error.response?.statusText,
      data: error.response?.data
    }, null, 2)
    ElMessage.error('API调用失败: ' + error.message)
  }
}

const clearAuth = () => {
  clearUserInfo()
  apiTestResult.value = ''
  ElMessage.info('认证信息已清除')
}

const mockLogin = () => {
  // 模拟一个管理员token（实际使用时应该通过登录接口获取）
  const mockToken = 'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsInJvbGVzIjpbIlJPTEVfYWRtaW4iXSwiaWF0IjoxNzMzNDE2MzMxLCJleHAiOjE3MzM1MDI3MzF9.mockTokenForTesting'
  setToken(mockToken)
  setUserRole('admin')
  setUsername('admin')
  ElMessage.success('已模拟管理员登录')
}

onMounted(() => {
    hasToken: hasToken.value,
    userRole: userRole.value,
    username: username.value,
    isAuthenticated: isAuthenticated.value
  })
})
</script>

<style scoped>
.auth-debugger {
  border: 1px solid #ddd;
  border-radius: 4px;
  padding: 16px;
  margin: 16px;
  background: #f9f9f9;
}

.debug-info {
  margin: 12px 0;
}

.info-item {
  display: flex;
  margin: 8px 0;
}

.info-item label {
  width: 100px;
  font-weight: bold;
}

.success {
  color: #67c23a;
}

.error {
  color: #f56c6c;
}

.actions {
  margin: 16px 0;
  display: flex;
  gap: 8px;
}

.api-result {
  margin-top: 16px;
  padding: 12px;
  background: #f5f5f5;
  border-radius: 4px;
}

.api-result pre {
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
