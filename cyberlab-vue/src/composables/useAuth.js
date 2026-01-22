import { ref, computed } from 'vue'
import { getToken, getUsername, getUserRole, getUserId } from '@/utils/auth'

// 全局认证状态
const authState = ref({
  isInitialized: false,
  isAuthenticated: false,
  user: null,
  token: null,
  loginInProgress: false
})

// 认证状态监听器列表
const authListeners = new Set()

// 添加认证状态监听器
const addAuthListener = (callback) => {
  authListeners.add(callback)
  return () => authListeners.delete(callback)
}

// 通知所有监听器
const notifyAuthListeners = () => {
  authListeners.forEach(callback => {
    try {
      callback(authState.value)
    } catch (error) {
    }
  })
}

// 初始化认证状态
const initializeAuth = async () => {

  const token = getToken()
  const username = getUsername()
  const role = getUserRole()
  const userId = getUserId()

  // 检查token是否过期
  let isValidAuth = false
  if (token) {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]))
      const currentTime = Date.now() / 1000
      if (payload.exp > currentTime) {
        isValidAuth = true
      } else {
        // 清除过期的token和相关信息
        await clearAuthState()
        return
      }
    } catch (error) {
      await clearAuthState()
      return
    }
  }

  authState.value = {
    isInitialized: true,
    isAuthenticated: isValidAuth,
    user: isValidAuth ? { username, role, id: userId } : null,
    token: isValidAuth ? token : null,
    loginInProgress: false
  }

  notifyAuthListeners()
}

// 设置登录进行中状态
const setLoginInProgress = (inProgress) => {
  authState.value.loginInProgress = inProgress
  notifyAuthListeners()
}

// 更新认证状态（登录成功后调用）
const updateAuthState = (token, username, role, userId = null) => {

  authState.value = {
    isInitialized: true,
    isAuthenticated: true,
    user: { username, role, id: userId },
    token,
    loginInProgress: false
  }

  notifyAuthListeners()
}

// 清除认证状态（登出时调用）
const clearAuthState = async () => {

  // 清除存储中的认证信息
  const { clearUserInfo } = await import('@/utils/auth')
  clearUserInfo()

  authState.value = {
    isInitialized: true,
    isAuthenticated: false,
    user: null,
    token: null,
    loginInProgress: false
  }

  notifyAuthListeners()
}

// 等待认证状态初始化完成
const waitForAuthInitialization = () => {
  return new Promise((resolve) => {
    if (authState.value.isInitialized && !authState.value.loginInProgress) {
      resolve(authState.value)
      return
    }

    // 添加临时监听器等待初始化完成
    const removeListener = addAuthListener((state) => {
      if (state.isInitialized && !state.loginInProgress) {
        removeListener()
        resolve(state)
      }
    })
  })
}

// 等待登录完成
const waitForLoginCompletion = () => {
  return new Promise((resolve) => {
    if (authState.value.isInitialized && !authState.value.loginInProgress) {
      resolve(authState.value)
      return
    }

    // 添加临时监听器等待登录完成
    const removeListener = addAuthListener((state) => {
      if (state.isInitialized && !state.loginInProgress) {
        removeListener()
        resolve(state)
      }
    })
  })
}

// 可计算属性
const isAuthenticated = computed(() => authState.value.isAuthenticated)
const isInitialized = computed(() => authState.value.isInitialized)
const loginInProgress = computed(() => authState.value.loginInProgress)
const currentUser = computed(() => authState.value.user)

export function useAuth() {
  return {
    // 状态
    authState: authState.value,
    isAuthenticated,
    isInitialized,
    loginInProgress,
    currentUser,
    
    // 方法
    initializeAuth,
    setLoginInProgress,
    updateAuthState,
    clearAuthState,
    waitForAuthInitialization,
    waitForLoginCompletion,
    addAuthListener
  }
}

// 初始化认证状态（应用启动时调用）
if (typeof window !== 'undefined') {
  // 页面加载时自动初始化
  initializeAuth().catch(error => {
    // 静默处理初始化错误
  })
}