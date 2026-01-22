import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '@/layouts/MainLayout.vue'
import { isLoggedIn, getUserRole, getUserId } from '@/utils/auth'
import { useAuth } from '@/composables/useAuth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginPage.vue'),
      meta: { hideLayout: true, requiresAuth: false }
    },
    {
      path: '/',
      name: 'main',
      component: MainLayout,
      redirect: (to) => {
        // 根据用户角色重定向到对应的默认页面
        const userRole = getUserRole()
        if (userRole === 'license_admin') {
          return '/license-management'
        }
        return '/dashboard'
      },
      meta: { requiresAuth: true },
      children: [
        {
          path: 'dashboard',
          component: () => import('@/views/DashboardPage.vue'),
          meta: { requiresAuth: true, roles: ['admin', 'red', 'blue', 'judge'] }
        },
        {
          path: 'assets',
          component: () => import('@/views/AssetManagementPage.vue'),
          meta: { requiresAuth: true, roles: ['admin', 'red', 'blue', 'judge'] }
        },
        { 
          path: 'assets/project/:id', 
          component: () => import('@/views/assets/AssetProjectDetailPage.vue'),
          props: true,
          meta: { hidden: true, requiresAuth: true, roles: ['admin', 'red', 'blue', 'judge'] }
        },
        {
          path: 'topology',
          component: () => import('@/views/assets/TopologyGraph.vue'),
          meta: { requiresAuth: true, roles: ['admin', 'red', 'blue', 'judge'] }
        },
        {
          path: 'users',
          component: () => import('@/views/UserManagementPage.vue'),
          meta: { requiresAuth: true, roles: ['admin'] }
        },
        {
          path: 'teams',
          name: 'TeamManagement',
          component: () => import('@/views/TeamManagementPage.vue'),
          meta: { requiresAuth: true, roles: ['admin'] }
        },
        {
          path: 'my-team',
          name: 'MyTeam',
          component: () => import('@/views/MyTeamPage.vue'),
          meta: { requiresAuth: true, roles: ['red', 'blue'] }
        },
        {
          path: 'logs',
          component: () => import('@/views/SystemLogPage.vue'),
          meta: { requiresAuth: true, roles: ['admin'] }
        },
        {
          path: 'drills',
          component: () => import('@/views/SafetyDrillCenterPage.vue'),
          meta: { requiresAuth: true, roles: ['admin', 'red', 'blue', 'judge'] }
        },
        { 
          path: 'drills/create', 
          component: () => import('@/views/drills/DrillCreatePage.vue'), 
          meta: { hidden: true, requiresAuth: true, roles: ['admin', 'judge'] } 
        },
        { 
          path: 'drills/:id', 
          component: () => import('@/views/drills/DrillDetailPage.vue'), 
          props: true, 
          meta: { hidden: true, requiresAuth: true, roles: ['admin', 'red', 'blue', 'judge'] } 
        },
        {
          path: '/achievement',
          meta: { requiresAuth: true },
          children: [
            {
              path: 'submit',
              name: 'AchievementSubmit',
              component: () => import('@/views/AchievementSubmitPage.vue'),
              meta: { requiresAuth: true, roles: ['admin', 'red', 'blue'] }
            },
            {
              path: 'manage',
              name: 'AchievementManage',
              component: () => import('@/views/AchievementManagePage.vue'),
              meta: { requiresAuth: true, roles: ['admin', 'judge'] }
            },
            {
              path: 'red-team-submit',
              name: 'RedTeamSubmit',
              component: () => import('@/views/achievement/RedTeamSubmitPage.vue'),
              meta: { requiresAuth: true, roles: ['admin', 'red'] }
            },
            {
              path: 'blue-team-submit', 
              name: 'BlueTeamSubmit',
              component: () => import('@/views/achievement/BlueTeamSubmitPage.vue'),
              meta: { requiresAuth: true, roles: ['admin', 'blue'] }
            },
            {
              path: 'analytics',
              name: 'AchievementAnalytics',
              component: () => import('@/views/achievement/AchievementAnalyticsPage.vue'),
              meta: { requiresAuth: true, roles: ['admin', 'judge'] }
            },
            {
              path: 'my-submissions',
              name: 'RedTeamAchievementList',
              component: () => import('@/views/achievement/RedTeamAchievementListPage.vue'),
              meta: { requiresAuth: true, roles: ['red'] }
            },
            {
              path: 'my-blue-submissions',
              name: 'BlueTeamAchievementList',
              component: () => import('@/views/achievement/BlueTeamAchievementListPage.vue'),
              meta: { requiresAuth: true, roles: ['blue'] }
            }
          ]
        },
        {
          path: '/settings',
          name: 'SystemSettings',
          component: () => import('@/views/SystemSettingsPage.vue'),
          meta: {
            requiresAuth: true,
            roles: ['admin'] // 仅管理员可访问
          }
        },
        {
          path: '/backup',
          name: 'SystemBackup',
          component: () => import('@/views/SystemBackupPage.vue'),
          meta: {
            requiresAuth: true,
            roles: ['admin'] // 仅管理员可访问
          }
        },
        {
          path: '/license-management',
          name: 'LicenseManagement',
          component: () => import('@/views/LicenseManagementPage.vue'),
          meta: {
            requiresAuth: true,
            roles: ['license_admin'] // 仅 hongan (license_admin) 可访问
          }
        },
        {
          path: '/license-admin/users',
          name: 'LicenseAdminUsers',
          component: () => import('@/views/LicenseAdminUserManagementPage.vue'),
          meta: {
            requiresAuth: true,
            roles: ['license_admin'] // 仅授权管理员可访问
          }
        },
        {
          path: '/license-admin/help',
          name: 'LicenseAdminHelp',
          component: () => import('@/views/LicenseAdminHelpPage.vue'),
          meta: {
            requiresAuth: true,
            roles: ['license_admin'] // 仅授权管理员可访问
          }
        },
        {
          path: '/system-activation',
          name: 'SystemActivation',
          component: () => import('@/views/SystemActivationPage.vue'),
          meta: {
            requiresAuth: true,
            roles: ['admin', 'license_admin'] // 管理员和授权管理员都可以激活系统
          }
        },
        // 红队专用路由
        {
          path: '/red/recording-gateway',
          name: 'RecordingGateway',
          component: () => import('@/views/RecordingGateway.vue'),
          meta: {
            requiresAuth: true,
            roles: ['red', 'admin']
          }
        },
        {
          path: '/red/drill-info',
          name: 'RedTeamDrillInfo',
          component: () => import('@/views/red/RedTeamDrillInfo.vue'),
          meta: {
            requiresAuth: true,
            roles: ['red', 'admin'],
            requiresRecording: true  // 需要录屏才能访问
          }
        },
        {
          path: '/red/attack-workspace',
          name: 'RedAttackWorkspace',
          component: () => import('@/views/red/RedTeamDrillInfo.vue'),  // 暂用drill-info，后续可创建专门的workspace
          meta: {
            requiresAuth: true,
            roles: ['red', 'admin'],
            requiresRecording: true  // 需要录屏才能访问
          }
        },
        // 录屏管理（管理员）
        {
          path: '/admin/screen-recordings',
          name: 'ScreenRecordingManage',
          component: () => import('@/views/admin/ScreenRecordingManage.vue'),
          meta: {
            requiresAuth: true,
            roles: ['admin', 'judge']
          }
        }
      ]
    },
    // 录屏窗口路由 - 独立于主布局
    {
      path: '/recording-window',
      name: 'RecordingWindow',
      component: () => import('@/views/RecordingWindow.vue'),
      meta: {
        hideLayout: true,
        requiresAuth: true,
        roles: ['red', 'admin', 'blue', 'judge'] // 所有角色都可以录屏
      }
    },
    // 大屏路由 - 独立于主布局
    {
      path: '/bigscreen',
      name: 'BigScreen',
      beforeEnter: (to, from, next) => {
        // 根据用户角色重定向到对应的大屏
        const userRole = getUserRole()
        switch(userRole) {
          case 'admin':
            next('/bigscreen/admin')
            break
          case 'blue':
            next('/bigscreen/blue')
            break
          case 'red':
            next('/bigscreen/red')
            break
          case 'judge':
            next('/bigscreen/judge')
            break
          default:
            // 如果角色未知，使用通用大屏
            next()
        }
      },
      component: () => import('@/views/BigScreenDashboard.vue'),
      meta: {
        hideLayout: true,
        requiresAuth: true,
        roles: ['admin', 'judge'] // 管理员和裁判可以查看通用大屏
      }
    },
    // 角色特定的大屏路由
    {
      path: '/bigscreen/admin',
      name: 'AdminBigScreen',
      component: () => import('@/views/BigScreenDashboard.vue'), // 暂时使用通用大屏
      meta: {
        hideLayout: true,
        requiresAuth: true,
        roles: ['admin'] // 仅管理员可访问
      }
    },
    {
      path: '/bigscreen/blue',
      name: 'BlueTeamBigScreen',
      component: () => import('@/views/BlueTeamBigScreen.vue'),
      meta: {
        hideLayout: true,
        requiresAuth: true,
        roles: ['blue', 'admin'] // 蓝队和管理员可访问
      }
    },
    {
      path: '/bigscreen/red',
      name: 'RedTeamBigScreen',
      component: () => import('@/views/RedTeamBigScreen.vue'), // 使用红队专属大屏
      meta: {
        hideLayout: true,
        requiresAuth: true,
        roles: ['red', 'admin'] // 红队和管理员可访问
      }
    },
    {
      path: '/bigscreen/judge',
      name: 'JudgeBigScreen',
      component: () => import('@/views/JudgeBigScreen.vue'), // 使用裁判专属大屏
      meta: {
        hideLayout: true,
        requiresAuth: true,
        roles: ['judge', 'admin'] // 裁判和管理员可访问
      }
    }
  ]
})

// 路由守卫
router.beforeEach(async (to, from, next) => {
  const { waitForAuthInitialization } = useAuth()


  try {
    // 等待认证状态初始化完成
    const authState = await waitForAuthInitialization()

    const loggedIn = authState.isAuthenticated
    const userRole = authState.user?.role || getUserRole()
    const userId = authState.user?.id || getUserId()

    // 如果访问登录页面且已登录，根据角色跳转到对应页面
    if (to.path === '/login' && loggedIn) {
      if (userRole === 'license_admin') {
        next('/license-management')
      } else {
        next('/dashboard')
      }
      return
    }

    // 如果需要认证但未登录，跳转到登录页
    if (to.meta.requiresAuth && !loggedIn) {
      next('/login')
      return
    }

    // 如果需要特定角色权限
    if (to.meta.roles && to.meta.roles.length > 0) {
      if (!loggedIn) {
        next('/login')
        return
      }

      if (!to.meta.roles.includes(userRole)) {
        // 权限不足，根据用户角色跳转到对应的默认页面
        if (userRole === 'license_admin') {
          next('/license-management')
        } else {
          next('/dashboard')
        }
        return
      }
    }

    // 🔴 录屏准入检查 - 红队访问需录屏的页面时验证
    if (to.meta.requiresRecording && userRole === 'red') {
      // 验证 userId 是否存在
      if (!userId) {
        console.warn('用户 ID 缺失，无法验证录屏状态，跳转到录屏准入页')
        next('/red/recording-gateway')
        return
      }

      try {
        // 检查用户是否正在录屏
        const response = await fetch(`/api/screen-recording/status/${userId}`)
        const data = await response.json()

        if (!data.isRecording) {
          // 未录屏，跳转到录屏准入页
          next('/red/recording-gateway')
          return
        }
      } catch (error) {
        console.error('检查录屏状态失败:', error)
        // 出错时也跳转到录屏准入页
        next('/red/recording-gateway')
        return
      }
    }

    next()
  } catch (error) {
    // 出错时的降级处理
    const loggedIn = isLoggedIn()

    if (to.meta.requiresAuth && !loggedIn) {
      next('/login')
    } else {
      next()
    }
  }
})

export default router