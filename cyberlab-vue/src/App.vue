<template>
  <router-view />
  <!-- 授权过期全局守卫 - 仅在授权过期时显示 -->
  <LicenseGuard v-if="shouldShowLicenseGuard" />
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import LicenseGuard from '@/components/LicenseGuard.vue'
import { useLicense } from '@/composables/useLicense'
import { isLoggedIn } from '@/utils/auth'

// 八耻八荣：只在已登录时初始化授权检查，避免未登录时发起不必要的请求
const shouldInitLicense = isLoggedIn()
let licenseState = null

if (shouldInitLicense) {
  // 初始化全局授权状态（长刷新间隔，避免性能开销）
  licenseState = useLicense({
    autoRefresh: true,
    refreshInterval: 600000 // 10分钟刷新一次（平衡性能和实时性）
  })
}

// 获取当前路由（用于判断是否在登录页面）
const route = useRoute()

// 八耻八荣：完整的条件判断，只在正确的时机显示授权守卫
const shouldShowLicenseGuard = computed(() => {
  // 1. 登录页面永远不显示授权守卫（用户还没登录，不应该检查授权）
  if (route.path === '/login') {
    return false
  }

  // 2. 未登录用户不显示授权守卫
  if (!isLoggedIn()) {
    return false
  }

  // 3. 已登录且授权过期时才显示守卫遮罩层
  return licenseState?.isExpired?.value === true
})
</script>


<style scoped>
/* 全局样式可选填 */
</style>