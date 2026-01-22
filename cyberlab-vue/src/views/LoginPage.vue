<template>
  <div class="login-background">
    <div class="overlay">
      <el-card class="login-card" shadow="hover">
        <h2 class="title">{{ loginTitle }}</h2>
        <el-form :model="form" ref="loginFormRef" label-width="70px" class="form">
          <el-form-item label="用户名">
            <el-input v-model="form.username" placeholder="请输入用户名" clearable />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password clearable />
          </el-form-item>
          <el-form-item>
            <el-checkbox v-model="rememberMe">记住我</el-checkbox>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleLogin" :loading="loading" class="login-button">登录</el-button>
          </el-form-item>
          <el-alert
            v-if="error"
            title="登录失败，请检查用户名和密码"
            type="error"
            show-icon
            :closable="false"
          />
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '@/api/auth'
import { setToken, setUsername, setUserRole, setAuthorities, setRefreshToken, setUserId } from '@/utils/auth'
import { useAuth } from '@/composables/useAuth'
import { getSystemSettings } from '@/api/settings'

const router = useRouter()
const { setLoginInProgress, updateAuthState } = useAuth()
const form = ref({ username: '', password: '' })
const rememberMe = ref(false)
const error = ref(false)
const loading = ref(false)
const loginFormRef = ref(null)
const loginTitle = ref('红岸网络空间安全对抗平台')

const handleLogin = async () => {
  error.value = false
  loading.value = true
  
  
  // 设置登录进行中状态，防止其他组件提前加载
  setLoginInProgress(true)
  
  try {
    // 使用统一的API调用方式
    const response = await login(form.value)
    
    
    // 检查响应结构
    if (response && response.token) {

      // 保存登录信息（根据"记住我"选项决定存储方式）
      const shouldRemember = rememberMe.value

      setToken(response.token, shouldRemember)
      setRefreshToken(response.refreshToken, shouldRemember)
      setUsername(response.username || form.value.username, shouldRemember)
      setUserRole(response.role || 'user', shouldRemember)

      // 保存用户ID（录屏功能需要）
      if (response.userId !== undefined && response.userId !== null) {
        setUserId(response.userId, shouldRemember)
      }

      // 验证令牌是否正确保存
      const storageType = shouldRemember ? 'localStorage' : 'sessionStorage'
      const storage = shouldRemember ? localStorage : sessionStorage
      const savedToken = storage.getItem('token')

      // 解码JWT获取权限信息
      try {
        const payload = JSON.parse(atob(response.token.split('.')[1]))
        if (payload && payload.authorities) {
          setAuthorities(payload.authorities, shouldRemember)
        }
      } catch (jwtError) {
        // JWT解码失败不影响登录流程
      }

      // 更新全局认证状态
      updateAuthState(
        response.token,
        response.username || form.value.username,
        response.role || 'user',
        response.userId
      )

      
      // 等待一下确保状态更新完成
      await new Promise(resolve => setTimeout(resolve, 100))
      
      // 跳转到仪表板
      router.push('/dashboard')
    } else {
      throw new Error('登录响应格式异常')
    }
  } catch (err) {
    error.value = true
    // 登录失败时也要清除进行中状态
    setLoginInProgress(false)
  } finally {
    loading.value = false
  }
}

// 加载系统设置
const loadLoginSettings = async () => {
  try {
    const response = await getSystemSettings()
    
    // 处理响应数据
    let settingsData = null
    if (response?.success && response.data) {
      settingsData = response.data
    } else if (response?.login_page_title !== undefined || response?.login_title !== undefined) {
      settingsData = response
    } else if (response) {
      settingsData = response
    }
    
    // 更新登录标题
    if (settingsData?.login_page_title) {
      loginTitle.value = settingsData.login_page_title
    } else if (settingsData?.login_title) {
      // 向后兼容：如果没有新字段，使用旧字段的值作为登录页面标题
      loginTitle.value = settingsData.login_title
    }


    // 同时更新浏览器标题栏
    try {
      // 使用与登录页面内容一致的标题更新浏览器标题栏
      document.title = loginTitle.value
    } catch (error) {
    }
  } catch (error) {
    // 加载失败时使用默认标题
    loginTitle.value = '红岸网络空间安全对抗平台'
    // 同时更新浏览器标题栏为默认标题
    document.title = loginTitle.value
  }
}

// 组件挂载时加载设置
onMounted(() => {
  loadLoginSettings()
})
</script>

<style scoped>
.login-background {
  background-image: url('@/assets/hacker-bg.png');
  background-repeat: no-repeat;
  background-position: center center;
  background-size: contain;
  background-attachment: fixed;
  background-color: #000;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
}

@media (max-width: 768px) {
  .login-background {
    background-size: cover;
  }
}

.overlay {
  background-color: rgba(0, 0, 0, 0.75);
  padding: 40px;
  border-radius: 10px;
}

.login-card {
  background-color: rgba(10, 20, 10, 0.8);
  border: 1px solid #00ffcc;
  box-shadow: 0 0 20px #00ffcc55;
  color: #00ffcc;
  width: 400px;
}

.title {
  text-align: center;
  font-size: 20px;
  font-weight: bold;
  margin-bottom: 20px;
  color: #00ffcc;
}

.el-input__inner,
.el-checkbox__input.is-checked .el-checkbox__inner,
.el-button--primary {
  background-color: #001a1a;
  border-color: #00ffcc;
  color: #00ffcc;
}

.el-button--primary:hover {
  background-color: #00cc99;
  color: #000;
}

.login-button {
  width: 100%;
}
</style>