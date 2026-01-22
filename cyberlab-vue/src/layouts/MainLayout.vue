<template>
  <div class="dashboard-container" :class="themeClass">
    <!-- 红队动态光晕背景层 -->
    <div v-if="currentRole === 'red'" class="dynamic-glow-layer">
      <div class="glow-spot glow-1"></div>
      <div class="glow-spot glow-2"></div>
      <div class="glow-spot glow-3"></div>
    </div>

    <!-- 蓝队动态光晕背景层 -->
    <div v-else-if="currentRole === 'blue'" class="dynamic-glow-layer">
      <div class="glow-spot glow-blue-1"></div>
      <div class="glow-spot glow-blue-2"></div>
      <div class="glow-spot glow-blue-3"></div>
    </div>

    <el-container>
      <el-aside width="220px" class="sidebar">
        <div class="logo">
          <img v-if="systemSettings.system_logo" :src="getLogoUrl(systemSettings.system_logo)" class="logo-image" alt="系统Logo" />
          <span v-else class="logo-text">🔐</span>
          <span class="logo-title">{{ systemSettings.sidebar_title || 'CyberLab' }}</span>
        </div>
        <el-menu :default-active="activePath" class="el-menu-vertical" background-color="#ffffff" text-color="#1d1d1f"
          active-text-color="#007aff">

          <!-- ========== 授权管理员专属菜单 ========== -->
          <template v-if="isLicenseAdmin">
            <el-menu-item index="/license-management" @click="goTo('/license-management')">🎫 授权码管理</el-menu-item>
            <el-menu-item index="/license-admin/users" @click="goTo('/license-admin/users')">👤 用户管理</el-menu-item>
            <el-menu-item index="/license-admin/help" @click="goTo('/license-admin/help')">💡 使用帮助</el-menu-item>
          </template>

          <!-- ========== 其他角色菜单 ========== -->
          <template v-else>
            <!-- 仪表板 - 所有角色都可以访问 -->
            <el-menu-item index="/dashboard" @click="goTo('/dashboard')">🚀 网络空间安全驾驶舱</el-menu-item>

            <!-- 资产管理 - 红队不可见 -->
            <el-menu-item index="/assets" @click="goTo('/assets')" v-if="!isRedTeam">💻 资产管理</el-menu-item>

            <!-- 安全演练中心 - 红队看到简化版本 -->
            <el-menu-item index="/drills" @click="goTo('/drills')" v-if="!isRedTeam">🛡️ 安全演练中心</el-menu-item>

            <!-- 红队演练信息 - 仅红队可见 -->
            <el-menu-item index="/red/drill-info" @click="goTo('/red/drill-info')" v-if="isRedTeam">🎯 演练目标信息</el-menu-item>

          <!-- 成果管理子菜单 - 根据角色显示不同项目 -->
          <el-sub-menu index="/achievement" v-if="hasPermission(['admin', 'red', 'blue', 'judge'])">
            <template #title>
              <span>🏆 成果管理</span>
            </template>
            <!-- 红队提交 - 仅红队和管理员可见 -->
            <el-menu-item index="/achievement/red-team-submit" @click="goTo('/achievement/red-team-submit')"
              v-if="hasPermission(['admin', 'red'])">🟥 红队提交</el-menu-item>
            <!-- 我的成果 - 仅红队可见 -->
            <el-menu-item index="/achievement/my-submissions" @click="goTo('/achievement/my-submissions')"
              v-if="hasPermission(['red'])">📋 我的成果</el-menu-item>
            <!-- 蓝队提交 - 仅蓝队和管理员可见 -->
            <el-menu-item index="/achievement/blue-team-submit" @click="goTo('/achievement/blue-team-submit')"
              v-if="hasPermission(['admin', 'blue'])">🟦 蓝队提交</el-menu-item>
            <!-- 我的成果 - 仅蓝队可见 -->
            <el-menu-item index="/achievement/my-blue-submissions" @click="goTo('/achievement/my-blue-submissions')"
              v-if="hasPermission(['blue'])">📋 我的成果</el-menu-item>
            <!-- 统一管理 - 管理员和裁判可见 -->
            <el-menu-item index="/achievement/manage" @click="goTo('/achievement/manage')"
              v-if="hasPermission(['admin', 'judge'])">📋 统一管理</el-menu-item>
            <!-- 数据分析 - 管理员和裁判可见 -->
            <el-menu-item index="/achievement/analytics" @click="goTo('/achievement/analytics')"
              v-if="hasPermission(['admin', 'judge'])">📈 数据分析</el-menu-item>
            <!-- 录屏管理 - 管理员和裁判可见 -->
            <el-menu-item index="/admin/screen-recordings" @click="goTo('/admin/screen-recordings')"
              v-if="hasPermission(['admin', 'judge'])">🎬 录屏管理</el-menu-item>
          </el-sub-menu>

          <!-- 用户管理 - 仅管理员可见 -->
          <el-menu-item index="/users" @click="goTo('/users')" v-if="isAdmin">👤 用户管理</el-menu-item>

          <!-- 战队管理 - 仅管理员可见 -->
          <el-menu-item index="/teams" @click="goTo('/teams')" v-if="isAdmin">👥 战队管理</el-menu-item>

          <!-- 我的战队 - 红队/蓝队可见 -->
          <el-menu-item index="/my-team" @click="goTo('/my-team')" v-if="isRedTeam || isBlueTeam">👥 我的战队</el-menu-item>

          <!-- 系统日志 - 仅管理员可见 -->
          <el-menu-item index="/logs" @click="goTo('/logs')" v-if="isAdmin">📝 系统日志</el-menu-item>

          <!-- 系统备份 - 仅管理员可见 -->
          <el-menu-item index="/backup" @click="goTo('/backup')" v-if="isAdmin">💾 系统备份</el-menu-item>

          <!-- 系统设置 - 仅管理员可见 -->
          <el-menu-item index="/settings" @click="goTo('/settings')" v-if="isAdmin">⚙️ 系统设置</el-menu-item>
          </template>
        </el-menu>
      </el-aside>

      <el-container>
        <el-header class="header-bar">
          <div class="welcome">
            <span>欢迎你，{{ username }}</span>
            <el-tag v-if="!isLoggingOut" :type="getRoleTagType(currentRole)" size="small" class="role-tag">{{ getRoleDisplayName(currentRole) }}</el-tag>
            <el-tag v-else type="info" size="small" class="role-tag">正在退出...</el-tag>
          </div>
          <div class="header-center">
          </div>
          <el-button type="primary" plain size="small" @click="logout" :loading="isLoggingOut" :disabled="isLoggingOut">退出登录</el-button>
        </el-header>

        <el-main>
          <router-view /> <!-- ✅ 动态渲染子页面 -->
        </el-main>
      </el-container>
    </el-container>

    <!-- API状态检查器 -->
    <ApiStatusChecker v-if="showApiChecker" />
  </div>
</template>

<script setup>
import { ref, onMounted, computed, reactive, onUnmounted, watchEffect } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { getToken, getUsername, getUserRole, getAuthorities, clearUserInfo } from '@/utils/auth'
import { getSystemSettings } from '@/api/settings'
import ApiStatusChecker from '@/components/ApiStatusChecker.vue'
import { useRecordingChannel } from '@/composables/useRecordingChannel'


const router = useRouter()
const route = useRoute()
const { clearAuthState } = useAuth()

// 获取录屏状态（用于控制红队演练目标菜单显示）
const { isRecording, handleRecordingStatus, onMessage } = useRecordingChannel()

const username = ref('访客')
const showApiChecker = ref(true)
const isLoggingOut = ref(false)

// 添加响应式的认证状态
const authState = reactive({
  token: null,
  role: null,
  authorities: [],
  refreshTrigger: 0 // 用于强制刷新计算属性
})

// 系统设置数据
const systemSettings = reactive({
  system_logo: '',
  sidebar_title: 'CyberLab',
  website_title: 'CyberLab平台',
  login_page_title: '红岸网络空间安全对抗平台'
})

// 当前菜单高亮路径
const activePath = computed(() => route.path)

// 主题类名计算（根据角色切换主题）
const themeClass = computed(() => {
  if (currentRole.value === 'red') return 'theme-red'
  if (currentRole.value === 'blue') return 'theme-blue'
  return 'theme-default'
})

// 获取当前用户角色
const currentRole = computed(() => {
  // 依赖refreshTrigger来触发重新计算
  authState.refreshTrigger
  return getUserRole()
})

// 判断是否为管理员
const isAdmin = computed(() => {
  // 依赖refreshTrigger来触发重新计算
  authState.refreshTrigger
  const role = getUserRole()
  const authorities = getAuthorities()

  // 方法1：根据角色判断
  if (role === 'admin') return true

  // 方法2：根据权限判断
  if (authorities.includes('ROLE_admin')) return true

  return false
})

// 判断是否为红队
const isRedTeam = computed(() => {
  return currentRole.value === 'red'
})

// 判断是否为蓝队
const isBlueTeam = computed(() => {
  return currentRole.value === 'blue'
})

// 判断是否为裁判
const isJudge = computed(() => {
  return currentRole.value === 'judge'
})

// 判断是否为授权管理员
const isLicenseAdmin = computed(() => {
  return currentRole.value === 'license_admin'
})

// 检查用户是否有权限访问某个菜单项
const hasPermission = (allowedRoles) => {
  if (!allowedRoles || allowedRoles.length === 0) return true
  return allowedRoles.includes(currentRole.value)
}

// 获取角色显示名称
const getRoleDisplayName = (role) => {
  const roleMap = {
    red: '红队',
    blue: '蓝队',
    judge: '裁判',
    admin: '管理员',
    license_admin: '红岸授权员'
  }
  return roleMap[role] || '未知角色'
}

// 获取角色标签类型
const getRoleTagType = (role) => {
  const typeMap = {
    red: 'danger',
    blue: 'primary',
    judge: 'warning',
    admin: 'success',
    license_admin: 'warning'  // 使用警告色（橙色）表示授权员
  }
  return typeMap[role] || 'info'
}

// 更新认证状态
const updateAuthState = () => {
  authState.token = getToken()
  authState.role = getUserRole()
  authState.authorities = getAuthorities()
  authState.refreshTrigger += 1 // 触发计算属性重新计算
  
  // 同时更新用户名显示
  const currentUsername = getUsername()
  if (currentUsername) {
    username.value = currentUsername
  }
}

// 加载系统设置
const loadSystemSettings = async () => {
  try {
    // 只有管理员才尝试加载系统设置
    // 其他用户（包括 license_admin）使用默认设置或缓存，避免403错误
    if (!isAdmin.value || isLicenseAdmin.value) {
      // 使用本地存储的设置（如果有）
      const cachedSettings = localStorage.getItem('systemSettings')
      if (cachedSettings) {
        Object.assign(systemSettings, JSON.parse(cachedSettings))
      }
      updatePageTitle()
      updateFavicon()
      return
    }
    
    const response = await getSystemSettings()
    
    // 正确处理axios拦截器已处理的响应
    if (response?.success && response.data) {
      // 标准API响应格式：{success: true, data: {...}}
      Object.assign(systemSettings, response.data)
    } else if (response?.system_logo !== undefined || response?.login_title !== undefined) {
      // 直接是设置数据
      Object.assign(systemSettings, response)
    } else if (response) {
      // 其他情况，尝试使用response本身
      Object.assign(systemSettings, response)
    }
    
    // 缓存设置到本地，供非管理员用户使用
    localStorage.setItem('systemSettings', JSON.stringify(systemSettings))
    
    // 动态更新网站标题和favicon
    updatePageTitle()
    updateFavicon()
    
  } catch (error) {
    // 尝试使用缓存的设置
    const cachedSettings = localStorage.getItem('systemSettings')
    if (cachedSettings) {
      Object.assign(systemSettings, JSON.parse(cachedSettings))
      updatePageTitle()
      updateFavicon()
    }
  }
}

// 获取Logo URL - 使用相对路径通过Vite代理，避免CORS问题
const getLogoUrl = (logoPath) => {
  if (!logoPath) return ''

  // 如果是完整URL，直接返回
  if (logoPath.startsWith('http')) {
    return logoPath
  }

  // 返回相对路径，通过Vite代理访问（/uploads会被代理到后端）
  return logoPath
}

// 更新页面标题
const updatePageTitle = () => {
  // 优先使用新的website_title字段，向后兼容login_title
  if (systemSettings.website_title) {
    document.title = systemSettings.website_title
  } else if (systemSettings.login_title) {
    document.title = systemSettings.login_title
  }
}

// 更新favicon
const updateFavicon = () => {
  if (systemSettings.system_logo) {
    const favicon = document.querySelector('link[rel="icon"]') || document.createElement('link')
    favicon.rel = 'icon'
    favicon.type = 'image/png'
    favicon.href = getLogoUrl(systemSettings.system_logo)
    
    if (!document.querySelector('link[rel="icon"]')) {
      document.head.appendChild(favicon)
    }
  }
}

// 系统设置更新事件监听器
let settingsEventListener = null

onMounted(() => {
  const token = getToken()
  if (!token) {
    router.push('/login')
  } else {
    // 初始化认证状态
    updateAuthState()
  }

  // 检查是否显示API状态检查器
  const hideApiChecker = localStorage.getItem('hideApiChecker')
  showApiChecker.value = hideApiChecker !== 'true'
  
  // 加载系统设置
  loadSystemSettings()

  // 监听录屏状态变化（用于控制红队演练目标菜单显示）
  const cleanupRecordingListener = onMessage(handleRecordingStatus)
  if (cleanupRecordingListener) {
    onUnmounted(cleanupRecordingListener)
  }

  // 监听系统设置更新事件
  settingsEventListener = (event) => {
    Object.assign(systemSettings, event.detail)
    updatePageTitle()
    updateFavicon()
  }
  window.addEventListener('settingsUpdated', settingsEventListener)
  
  // 监听存储变化（当令牌刷新时会触发）
  const handleStorageChange = () => {
    updateAuthState()
  }
  window.addEventListener('storage', handleStorageChange)
  
  // 监听认证状态变化
  const { addAuthListener } = useAuth()
  const removeAuthListener = addAuthListener(() => {
    updateAuthState()
  })
  
  // 定期检查认证状态（处理sessionStorage变化）
  const authCheckInterval = setInterval(() => {
    updateAuthState()
  }, 1000) // 每秒检查一次
  
  // 清理函数
  onUnmounted(() => {
    clearInterval(authCheckInterval)
    removeAuthListener()
    window.removeEventListener('storage', handleStorageChange)
  })
})

const logout = async () => {
  if (isLoggingOut.value) return // 防止重复执行

  try {
    isLoggingOut.value = true

    // 等待状态完全清除，避免显示中间状态（"未知角色"）
    await clearAuthState()
    clearUserInfo()

    // 清除演练目标访问授权（确保重新登录后需要重新录屏）
    sessionStorage.removeItem('drill-access-granted')
    console.log('🔒 已清除演练目标访问授权')

    // 清除完成后再跳转到登录页
    await router.push('/login')
  } catch (error) {
    // 如果路由跳转失败，确保用户信息已清除
    console.error('Logout route navigation failed:', error)
    await clearAuthState()
    clearUserInfo()
    // 强制跳转到登录页
    try {
      await router.push('/login')
    } catch (routerError) {
      // 如果路由完全失败，使用window.location但保持相对路径
      window.location.pathname = '/login'
    }
  } finally {
    isLoggingOut.value = false
  }
}

const goTo = (path) => {
  if (route.path !== path) router.push(path)
}

// 组件卸载时清理事件监听器
onUnmounted(() => {
  if (settingsEventListener) {
    window.removeEventListener('settingsUpdated', settingsEventListener)
    settingsEventListener = null
  }
})
</script>

<style scoped>
.dashboard-container {
  min-height: 100vh;
  background-color: #f5f7fa;
}

.el-container {
  min-height: 100vh;
}

.sidebar {
  background-color: #ffffff;
  border-right: 1px solid #e6e6e6;
  padding-top: 20px;
}

.logo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 20px;
  font-weight: 600;
  text-align: center;
  margin-bottom: 20px;
  color: #007aff;
  padding: 0 10px;
}

.logo-image {
  width: 32px;
  height: 32px;
  object-fit: contain;
  border-radius: 4px;
}

.logo-text {
  font-size: 24px;
}

.logo-title {
  font-size: 18px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #ffffff;
  color: #333;
  padding: 12px 24px;
  border-bottom: 1px solid #e6e6e6;
}

.header-center {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  margin: 0 20px;
}

.welcome {
  display: flex;
  align-items: center;
  gap: 12px;
}

.role-tag {
  font-weight: 500;
}

.el-main {
  background-color: #f9f9f9;
  padding: 30px;
  overflow-y: auto;
}

/* 优雅的左对齐菜单 */
:deep(.el-menu-vertical) {
  border-right: none;
}

:deep(.el-menu-item) {
  padding-left: 24px !important;
  padding-right: 24px !important;
  height: 48px;
  line-height: 48px;
  display: flex;
  align-items: center;
  transition: all 0.3s ease;
}

:deep(.el-sub-menu__title) {
  padding-left: 24px !important;
  padding-right: 24px !important;
  height: 48px;
  line-height: 48px;
  display: flex;
  align-items: center;
  transition: all 0.3s ease;
}

/* 子菜单缩进 */
:deep(.el-menu--inline .el-menu-item) {
  padding-left: 48px !important;
  background-color: #fafafa;
}

/* 激活状态优化 */
:deep(.el-menu-item.is-active) {
  background-color: rgba(0, 122, 255, 0.08);
  border-left: 3px solid #007aff;
  padding-left: 21px !important;
  font-weight: 500;
}

/* 子菜单激活状态 */
:deep(.el-menu--inline .el-menu-item.is-active) {
  padding-left: 45px !important;
}

/* 悬停效果 */
:deep(.el-menu-item:hover) {
  background-color: rgba(0, 122, 255, 0.05);
}

:deep(.el-sub-menu__title:hover) {
  background-color: rgba(0, 122, 255, 0.05);
}

/* 展开的子菜单容器 */
:deep(.el-menu--inline) {
  background-color: #fafafa;
}

/* ============================================
   🔴 红队深色主题 - 贴近红队驾驶舱风格
   ============================================ */

/* 红队主题容器 */
.theme-red.dashboard-container {
  background: linear-gradient(135deg, #0a0a0a 0%, #1a0d0d 50%, #0f0f0f 100%);
  position: relative;
  overflow: hidden;
}

/* 动态光晕背景层 */
.theme-red .dynamic-glow-layer {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
}

.theme-red .glow-spot {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.3;
}

.theme-red .glow-1 {
  top: 15%;
  left: 25%;
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(255, 59, 48, 0.2) 0%, transparent 70%);
  animation: glow-breath-1 8s ease-in-out infinite;
}

.theme-red .glow-2 {
  bottom: 20%;
  right: 15%;
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, rgba(204, 0, 0, 0.15) 0%, transparent 70%);
  animation: glow-breath-2 10s ease-in-out infinite;
  animation-delay: 2s;
}

.theme-red .glow-3 {
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

/* 红队侧边栏 */
.theme-red .sidebar {
  background: rgba(20, 20, 20, 0.7);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-right: 1px solid rgba(255, 59, 48, 0.15);
  box-shadow: 0 0 30px rgba(255, 59, 48, 0.1);
  position: relative;
  z-index: 10;
}

/* 红队Logo */
.theme-red .logo {
  background: linear-gradient(135deg, #ff3b30 0%, #ff6b59 100%);
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  filter: drop-shadow(0 0 20px rgba(255, 59, 48, 0.4));
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", "Helvetica Neue", sans-serif;
}

.theme-red .logo-image {
  filter: drop-shadow(0 0 10px rgba(255, 59, 48, 0.5));
}

/* 红队菜单容器 */
.theme-red .el-menu-vertical {
  background-color: transparent !important;
  border-right: none;
}

/* 红队菜单项 - 默认状态 */
.theme-red :deep(.el-menu-item) {
  background-color: transparent;
  color: rgba(255, 255, 255, 0.9) !important;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Text", "Helvetica Neue", sans-serif;
  font-weight: 500;
}

/* 红队菜单项 - 悬停状态 */
.theme-red :deep(.el-menu-item:hover) {
  background-color: rgba(255, 59, 48, 0.08) !important;
  color: rgba(255, 255, 255, 0.95) !important;
  transform: translateX(2px);
}

/* 红队菜单项 - 激活状态 */
.theme-red :deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, rgba(255, 59, 48, 0.15) 0%, transparent 100%) !important;
  border-left: 3px solid #ff3b30;
  padding-left: 21px !important;
  color: #ff3b30 !important;
  font-weight: 600;
  box-shadow: 0 0 20px rgba(255, 59, 48, 0.2);
}

/* 红队子菜单标题 */
.theme-red :deep(.el-sub-menu__title) {
  background-color: transparent !important;
  color: rgba(255, 255, 255, 0.9) !important;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
  font-weight: 500;
}

.theme-red :deep(.el-sub-menu__title:hover) {
  background-color: rgba(255, 59, 48, 0.08) !important;
  color: rgba(255, 255, 255, 0.95) !important;
}

/* 红队子菜单图标 */
.theme-red :deep(.el-sub-menu__icon-arrow) {
  color: rgba(255, 255, 255, 0.6) !important;
}

/* 红队子菜单容器 */
.theme-red :deep(.el-menu--inline) {
  background-color: rgba(15, 15, 15, 0.5) !important;
}

/* 红队子菜单项 */
.theme-red :deep(.el-menu--inline .el-menu-item) {
  background-color: transparent !important;
  color: rgba(255, 255, 255, 0.85) !important;
}

.theme-red :deep(.el-menu--inline .el-menu-item:hover) {
  background-color: rgba(255, 59, 48, 0.08) !important;
  color: rgba(255, 255, 255, 0.95) !important;
}

.theme-red :deep(.el-menu--inline .el-menu-item.is-active) {
  background: linear-gradient(90deg, rgba(255, 59, 48, 0.2) 0%, transparent 100%) !important;
  padding-left: 45px !important;
  color: #ff6b59 !important;
}

/* 红队顶部栏 */
.theme-red .header-bar {
  background: rgba(15, 15, 15, 0.85);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(255, 59, 48, 0.2);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
  color: rgba(255, 255, 255, 0.95);
  position: relative;
  z-index: 10;
}

/* 红队欢迎文字 */
.theme-red .welcome {
  color: rgba(255, 255, 255, 0.95);
  font-weight: 500;
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Text", "Helvetica Neue", sans-serif;
}

.theme-red .welcome span {
  color: rgba(255, 255, 255, 0.95);
}

/* 红队角色标签 */
.theme-red .role-tag {
  background: linear-gradient(135deg, rgba(255, 59, 48, 0.2) 0%, rgba(255, 59, 48, 0.3) 100%);
  border: 0.5px solid rgba(255, 59, 48, 0.4);
  color: #ff3b30;
  font-weight: 600;
  box-shadow: 0 0 10px rgba(255, 59, 48, 0.3);
}

/* 红队退出按钮 */
.theme-red .header-bar :deep(.el-button) {
  background: linear-gradient(135deg, rgba(255, 59, 48, 0.15) 0%, rgba(255, 59, 48, 0.25) 100%);
  border: 0.5px solid rgba(255, 59, 48, 0.3);
  color: #ffffff;
  font-weight: 600;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
  box-shadow: 0 2px 8px rgba(255, 59, 48, 0.15);
}

.theme-red .header-bar :deep(.el-button:hover) {
  background: linear-gradient(135deg, rgba(255, 59, 48, 0.25) 0%, rgba(255, 59, 48, 0.35) 100%);
  border-color: rgba(255, 59, 48, 0.5);
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(255, 59, 48, 0.25);
}

.theme-red .header-bar :deep(.el-button:active) {
  transform: translateY(0);
}

/* 红队主内容区 */
.theme-red .el-main {
  background-color: transparent;
  position: relative;
  z-index: 1;
}

/* 红队 Element Plus 组件全局覆盖 */
.theme-red :deep(.el-container) {
  position: relative;
  z-index: 1;
}

/* ============================================
   🔵 蓝队深蓝主题 - 专业防御者风格
   ============================================ */

/* 蓝队主题容器 */
.theme-blue.dashboard-container {
  background: linear-gradient(135deg, #0a1428 0%, #0d1a2d 50%, #1e3a5f 100%);
  position: relative;
  overflow: hidden;
}

/* 蓝队动态光晕背景层 */
.theme-blue .dynamic-glow-layer {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
}

.theme-blue .glow-spot {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.3;
}

.theme-blue .glow-blue-1 {
  top: 15%;
  left: 20%;
  width: 450px;
  height: 450px;
  background: radial-gradient(circle, rgba(70, 130, 180, 0.25) 0%, transparent 70%);
  animation: glow-breath-blue-1 8s ease-in-out infinite;
}

.theme-blue .glow-blue-2 {
  bottom: 15%;
  right: 20%;
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, rgba(30, 144, 255, 0.2) 0%, transparent 70%);
  animation: glow-breath-blue-2 10s ease-in-out infinite;
  animation-delay: 2s;
}

.theme-blue .glow-blue-3 {
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(0, 212, 255, 0.15) 0%, transparent 70%);
  animation: glow-breath-blue-3 6s ease-in-out infinite;
  animation-delay: 1s;
}

@keyframes glow-breath-blue-1 {
  0%, 100% {
    opacity: 0.25;
    transform: scale(1);
  }
  50% {
    opacity: 0.4;
    transform: scale(1.15);
  }
}

@keyframes glow-breath-blue-2 {
  0%, 100% {
    opacity: 0.2;
    transform: scale(1);
  }
  50% {
    opacity: 0.35;
    transform: scale(1.2);
  }
}

@keyframes glow-breath-blue-3 {
  0%, 100% {
    opacity: 0.18;
    transform: translate(-50%, -50%) scale(1);
  }
  50% {
    opacity: 0.32;
    transform: translate(-50%, -50%) scale(1.25);
  }
}

/* 蓝队侧边栏 */
.theme-blue .sidebar {
  background: rgba(20, 30, 50, 0.7);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-right: 1px solid rgba(70, 130, 180, 0.2);
  box-shadow: 0 0 30px rgba(70, 130, 180, 0.15);
  position: relative;
  z-index: 10;
}

/* 蓝队Logo */
.theme-blue .logo {
  background: linear-gradient(135deg, #4682b4 0%, #1e90ff 50%, #00d4ff 100%);
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  filter: drop-shadow(0 0 20px rgba(70, 130, 180, 0.4));
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", "Helvetica Neue", sans-serif;
}

.theme-blue .logo-image {
  filter: drop-shadow(0 0 10px rgba(70, 130, 180, 0.5));
}

/* 蓝队菜单容器 */
.theme-blue .el-menu-vertical {
  background-color: transparent !important;
  border-right: none;
}

/* 蓝队菜单项 - 默认状态 */
.theme-blue :deep(.el-menu-item) {
  background-color: transparent;
  color: rgba(255, 255, 255, 0.9) !important;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Text", "Helvetica Neue", sans-serif;
  font-weight: 500;
}

/* 蓝队菜单项 - 悬停状态 */
.theme-blue :deep(.el-menu-item:hover) {
  background-color: rgba(70, 130, 180, 0.1) !important;
  color: rgba(255, 255, 255, 0.95) !important;
  transform: translateX(2px);
}

/* 蓝队菜单项 - 激活状态 */
.theme-blue :deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, rgba(70, 130, 180, 0.2) 0%, transparent 100%) !important;
  border-left: 3px solid #4682b4;
  padding-left: 21px !important;
  color: #4682b4 !important;
  font-weight: 600;
  box-shadow: 0 0 20px rgba(70, 130, 180, 0.25);
}

/* 蓝队子菜单标题 */
.theme-blue :deep(.el-sub-menu__title) {
  background-color: transparent !important;
  color: rgba(255, 255, 255, 0.9) !important;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
  font-weight: 500;
}

.theme-blue :deep(.el-sub-menu__title:hover) {
  background-color: rgba(70, 130, 180, 0.1) !important;
  color: rgba(255, 255, 255, 0.95) !important;
}

/* 蓝队子菜单图标 */
.theme-blue :deep(.el-sub-menu__icon-arrow) {
  color: rgba(255, 255, 255, 0.6) !important;
}

/* 蓝队子菜单容器 */
.theme-blue :deep(.el-menu--inline) {
  background-color: rgba(15, 20, 35, 0.5) !important;
}

/* 蓝队子菜单项 */
.theme-blue :deep(.el-menu--inline .el-menu-item) {
  background-color: transparent !important;
  color: rgba(255, 255, 255, 0.85) !important;
}

.theme-blue :deep(.el-menu--inline .el-menu-item:hover) {
  background-color: rgba(70, 130, 180, 0.1) !important;
  color: rgba(255, 255, 255, 0.95) !important;
}

.theme-blue :deep(.el-menu--inline .el-menu-item.is-active) {
  background: linear-gradient(90deg, rgba(70, 130, 180, 0.25) 0%, transparent 100%) !important;
  padding-left: 45px !important;
  color: #1e90ff !important;
}

/* 蓝队顶部栏 */
.theme-blue .header-bar {
  background: rgba(15, 20, 35, 0.85);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(70, 130, 180, 0.25);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
  color: rgba(255, 255, 255, 0.95);
  position: relative;
  z-index: 10;
}

/* 蓝队欢迎文字 */
.theme-blue .welcome {
  color: rgba(255, 255, 255, 0.95);
  font-weight: 500;
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Text", "Helvetica Neue", sans-serif;
}

.theme-blue .welcome span {
  color: rgba(255, 255, 255, 0.95);
}

/* 蓝队角色标签 */
.theme-blue .role-tag {
  background: linear-gradient(135deg, rgba(70, 130, 180, 0.25) 0%, rgba(30, 144, 255, 0.35) 100%);
  border: 0.5px solid rgba(70, 130, 180, 0.5);
  color: #1e90ff;
  font-weight: 600;
  box-shadow: 0 0 10px rgba(70, 130, 180, 0.3);
}

/* 蓝队退出按钮 */
.theme-blue .header-bar :deep(.el-button) {
  background: linear-gradient(135deg, rgba(70, 130, 180, 0.2) 0%, rgba(30, 144, 255, 0.3) 100%);
  border: 0.5px solid rgba(70, 130, 180, 0.4);
  color: #ffffff;
  font-weight: 600;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
  box-shadow: 0 2px 8px rgba(70, 130, 180, 0.2);
}

.theme-blue .header-bar :deep(.el-button:hover) {
  background: linear-gradient(135deg, rgba(70, 130, 180, 0.3) 0%, rgba(30, 144, 255, 0.4) 100%);
  border-color: rgba(70, 130, 180, 0.6);
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(70, 130, 180, 0.35);
}

.theme-blue .header-bar :deep(.el-button:active) {
  transform: translateY(0);
}

/* 蓝队主内容区 */
.theme-blue .el-main {
  background-color: transparent;
  position: relative;
  z-index: 1;
}

/* 蓝队 Element Plus 组件全局覆盖 */
.theme-blue :deep(.el-container) {
  position: relative;
  z-index: 1;
}
</style>