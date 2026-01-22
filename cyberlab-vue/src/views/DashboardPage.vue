<template>
  <div class="dashboard-container" :class="themeClass">
    <!-- 添加大屏模式按钮 -->
    <div class="dashboard-header">
      <el-button 
        type="primary" 
        size="large" 
        @click="openBigScreen"
        @mouseenter="onButtonHover"
        @mouseleave="onButtonLeave"
        class="bigscreen-btn"
        id="bigscreen-button"
      >
        🖥️ 开启大屏模式
      </el-button>
    </div>
    
    <!-- 管理员驾驶舱 -->
    <AdminDashboard v-if="userRole === 'admin'" />
    
    <!-- 裁判驾驶舱 -->
    <JudgeDashboard v-else-if="userRole === 'judge'" />
    
    <!-- 蓝队驾驶舱 -->
    <BlueTeamDashboard v-else-if="userRole === 'blue'" />
    
    <!-- 红队驾驶舱 -->
    <RedTeamDashboard v-else-if="userRole === 'red'" />
    
    <!-- 默认视图 -->
    <DefaultDashboard v-else />
  </div>
</template>

<script setup>
import { computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getUserRole, getToken } from '@/utils/auth'
import AdminDashboard from './dashboard/AdminDashboard.vue'
import JudgeDashboard from './dashboard/JudgeDashboard.vue'
import BlueTeamDashboard from './dashboard/BlueTeamDashboard.vue'
import RedTeamDashboard from './dashboard/RedTeamDashboard.vue'
import DefaultDashboard from './dashboard/DefaultDashboard.vue'

const userRole = computed(() => getUserRole())

// 主题类名计算（根据角色切换主题）
const themeClass = computed(() => {
  if (userRole.value === 'red') return 'theme-red'
  if (userRole.value === 'blue') return 'theme-blue'
  return 'theme-default'
})

import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

// 添加路由监控

// 监听路由变化
watch(() => route.path, (newPath, oldPath) => {
}, { immediate: true })

// 组件挂载时的调试信息
onMounted(() => {
})

// 按钮悬停事件处理
const onButtonHover = () => {
}

const onButtonLeave = () => {
}

const openBigScreen = () => {
  try {
    // 检查用户认证状态
    const token = getToken()

    if (!token) {
      ElMessage.error('请先登录后再使用大屏功能')
      router.push('/login')
      return
    }

    // 获取用户角色并确定大屏URL
    const currentRole = userRole.value

    let bigScreenPath = '/bigscreen' // 默认路径

    // 根据角色确定大屏路径
    switch(currentRole) {
      case 'admin':
        bigScreenPath = '/bigscreen/admin'
        break
      case 'blue':
        bigScreenPath = '/bigscreen/blue'
        break
      case 'red':
        bigScreenPath = '/bigscreen/red'
        break
      case 'judge':
        bigScreenPath = '/bigscreen/judge'
        break
      default:
        bigScreenPath = '/bigscreen'
    }

    // 构建完整URL
    const baseUrl = window.location.origin
    const fullUrl = `${baseUrl}${bigScreenPath}`

    // 在新标签页打开大屏
    ElMessage.success(`正在新标签页中打开${currentRole}角色大屏...`)

    // 打开新标签页
    const newWindow = window.open(fullUrl, '_blank')

    // 检查是否成功打开新窗口
    if (newWindow) {
      // 确保新窗口获得焦点
      newWindow.focus()
    } else {
      // 如果被浏览器阻止，提示用户
      ElMessage.warning('请允许弹窗，然后重试')
    }

  } catch (error) {
    console.error('打开大屏失败:', error)
    ElMessage.error('打开大屏失败，请重试')
  }
}

</script>

<style scoped>
.dashboard-container {
  height: 100%;
  background-color: #f5f7fa;
  position: relative;
}

.dashboard-header {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 20px;
  position: relative;
  z-index: 1000;
  padding: 10px 0;
}

.bigscreen-btn {
  background: linear-gradient(45deg, #00d4ff, #0099ff);
  border: none;
  box-shadow: 0 4px 15px rgba(0, 212, 255, 0.3);
  transition: all 0.3s ease;
  position: relative;
  z-index: 1001;
  pointer-events: auto;
  cursor: pointer;
  min-width: 160px;
  font-weight: 600;
}

.bigscreen-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 212, 255, 0.4);
  background: linear-gradient(45deg, #0099ff, #00d4ff);
}

.bigscreen-btn:active {
  transform: translateY(0);
  box-shadow: 0 2px 10px rgba(0, 212, 255, 0.5);
}

.bigscreen-btn:focus {
  outline: 2px solid #00d4ff;
  outline-offset: 2px;
}

/* 确保按钮在所有情况下都可点击 */
#bigscreen-button {
  pointer-events: auto !important;
  user-select: none;
  -webkit-user-select: none;
  -moz-user-select: none;
  -ms-user-select: none;
}

/* ============================================
   🔴 红队深色主题 - 融合"开启大屏模式"按钮
   ============================================ */

/* 红队主题容器 */
.dashboard-container.theme-red {
  background-color: transparent;
}

/* 红队按钮区域 */
.dashboard-container.theme-red .dashboard-header {
  background: transparent;
  padding: 10px 0;
}

/* 红队大屏按钮 */
.dashboard-container.theme-red .bigscreen-btn {
  background: linear-gradient(135deg, rgba(255, 59, 48, 0.2) 0%, rgba(255, 59, 48, 0.35) 100%);
  border: 0.5px solid rgba(255, 59, 48, 0.4);
  color: #ffffff;
  font-weight: 600;
  box-shadow: 0 4px 15px rgba(255, 59, 48, 0.25);
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

.dashboard-container.theme-red .bigscreen-btn:hover {
  background: linear-gradient(135deg, rgba(255, 59, 48, 0.3) 0%, rgba(255, 59, 48, 0.45) 100%);
  border-color: rgba(255, 59, 48, 0.6);
  transform: translateY(-2px);
  box-shadow: 0 6px 25px rgba(255, 59, 48, 0.4);
}

.dashboard-container.theme-red .bigscreen-btn:active {
  transform: translateY(0);
  box-shadow: 0 2px 12px rgba(255, 59, 48, 0.5);
}

.dashboard-container.theme-red .bigscreen-btn:focus {
  outline: 2px solid rgba(255, 59, 48, 0.5);
  outline-offset: 2px;
}

/* ============================================
   🔵 蓝队深蓝主题 - 融合"开启大屏模式"按钮
   ============================================ */

/* 蓝队主题容器 */
.dashboard-container.theme-blue {
  background-color: transparent;
}

/* 蓝队按钮区域 */
.dashboard-container.theme-blue .dashboard-header {
  background: transparent;
  padding: 10px 0;
}

/* 蓝队大屏按钮 */
.dashboard-container.theme-blue .bigscreen-btn {
  background: linear-gradient(135deg, rgba(70, 130, 180, 0.25) 0%, rgba(30, 144, 255, 0.4) 100%);
  border: 0.5px solid rgba(70, 130, 180, 0.5);
  color: #ffffff;
  font-weight: 600;
  box-shadow: 0 4px 15px rgba(70, 130, 180, 0.3);
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

.dashboard-container.theme-blue .bigscreen-btn:hover {
  background: linear-gradient(135deg, rgba(70, 130, 180, 0.35) 0%, rgba(30, 144, 255, 0.5) 100%);
  border-color: rgba(70, 130, 180, 0.7);
  transform: translateY(-2px);
  box-shadow: 0 6px 25px rgba(70, 130, 180, 0.45);
}

.dashboard-container.theme-blue .bigscreen-btn:active {
  transform: translateY(0);
  box-shadow: 0 2px 12px rgba(70, 130, 180, 0.5);
}

.dashboard-container.theme-blue .bigscreen-btn:focus {
  outline: 2px solid rgba(70, 130, 180, 0.6);
  outline-offset: 2px;
}
</style>