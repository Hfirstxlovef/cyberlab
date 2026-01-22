<template>
  <div class="asset-page" :class="themeClass">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-content">
        <h2>📦 资产管理</h2>
        <p class="page-description">统一管理和监控企业资产，提升资产可见性与安全性</p>
      </div>
    </div>

    <el-tabs v-model="currentTab" @tab-change="handleTabChange" class="asset-tabs">
      <el-tab-pane label="📋 资产管理" name="assets">
        <AssetList
          v-if="currentTab === 'assets'"
          @project-selected="handleProjectSelected"
        />
      </el-tab-pane>
      <el-tab-pane label="🌐 拓扑图可视化" name="topology">
        <TopologyGraph
          v-if="currentTab === 'topology'"
          :selected-project="selectedProject"
          :key="topologyKey"
        />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { useRoute } from 'vue-router'
import { getUserRole } from '@/utils/auth'
import AssetList from './assets/AssetList.vue'
import TopologyGraph from './assets/TopologyGraph.vue'

const route = useRoute()
const currentTab = ref('assets')
const selectedProject = ref(null)
const topologyKey = ref(0)

// 主题切换：根据角色动态设置主题class
const role = getUserRole() || ''
const themeClass = computed(() => {
  return role === 'blue' ? 'theme-blue' : 'theme-admin'
})

// 从路由查询参数中获取项目信息
if (route.query.projectId) {
  selectedProject.value = {
    id: route.query.projectId,
    name: route.query.projectName || route.query.projectId
  }
  currentTab.value = 'topology'
}

// 处理项目选择事件
const handleProjectSelected = (project) => {
  selectedProject.value = project
  currentTab.value = 'topology'
  // 强制刷新拓扑组件
  topologyKey.value++
}

// 处理标签页切换
const handleTabChange = (tabName) => {
  if (tabName === 'topology' && selectedProject.value) {
    // 强制刷新拓扑组件以确保数据同步
    topologyKey.value++
  }
}

// 监听路由变化，动态更新项目选择
watch(() => route.query, (newQuery) => {
  if (newQuery.projectId) {
    selectedProject.value = {
      id: newQuery.projectId,
      name: newQuery.projectName || newQuery.projectId
    }
    currentTab.value = 'topology'
    topologyKey.value++
  }
}, { immediate: true })
</script>

<style scoped>
/* ============================================
   Apple Elegant White Style - 资产管理页面
   以复用现有为荣: 使用全局 apple-theme.css 的 CSS 变量
   以遵循规范为荣: 参考其他管理页面的结构
   ============================================ */

/* 页面主容器 - 提升优先级覆盖 main.css 的 #app 约束 */
.asset-page {
  padding: var(--spacing-xl);
  background: var(--apple-white);
  min-height: 100vh;
  font-family: var(--font-apple);
}

/* ============================================
   Page Header - 页面标题
   ============================================ */
.page-header {
  text-align: center;
  margin-bottom: var(--spacing-2xl);
  padding: var(--spacing-xl) 0;
}

.header-content h2 {
  font-size: 48px;
  font-weight: 700;
  color: var(--apple-text-primary);
  margin: 0 0 12px 0;
  letter-spacing: -0.5px;
  line-height: 1.1;
}

.page-description {
  font-size: 17px;
  color: var(--apple-text-secondary);
  margin: 0;
  font-weight: 400;
}

/* ============================================
   Tabs Styling - 标签页样式
   ============================================ */
.asset-tabs {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.95) 0%,
    rgba(248, 248, 248, 0.85) 100%);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 0.5px solid rgba(0, 0, 0, 0.04);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  overflow: hidden;
}

/* Element Plus Tabs 深度样式覆盖 */
.asset-tabs :deep(.el-tabs__header) {
  margin: 0;
  padding: 8px var(--spacing-lg);
  background: var(--apple-gray-1);
  border-bottom: 0.5px solid rgba(0, 0, 0, 0.04);
}

.asset-tabs :deep(.el-tabs__nav) {
  border: none;
}

.asset-tabs :deep(.el-tabs__item) {
  font-size: 15px;
  font-weight: 500;
  color: var(--apple-text-secondary);
  padding: 0 20px;
  height: 56px;
  line-height: 56px;
  border: none;
  transition: all 0.3s ease;
}

.asset-tabs :deep(.el-tabs__item:hover) {
  color: var(--apple-blue);
}

.asset-tabs :deep(.el-tabs__item.is-active) {
  color: var(--apple-blue);
  font-weight: 600;
}

.asset-tabs :deep(.el-tabs__active-bar) {
  background-color: var(--apple-blue);
  height: 3px;
  border-radius: 1.5px;
}

.asset-tabs :deep(.el-tabs__content) {
  padding: var(--spacing-lg);
}

/* ============================================
   Responsive Design
   ============================================ */
@media (max-width: 768px) {
  .asset-page {
    padding: 16px;
  }

  .header-content h2 {
    font-size: 36px;
  }

  .page-description {
    font-size: 15px;
  }

  .page-header {
    margin-bottom: var(--spacing-lg);
    padding: var(--spacing-lg) 0;
  }
}

@media (max-width: 576px) {
  .header-content h2 {
    font-size: 32px;
  }

  .page-header {
    padding: 16px 0;
  }
}

/* ============================================
   蓝队主题 - Blue Team Theme
   仅在蓝队用户登录时生效
   ============================================ */
.asset-page.theme-blue {
  background: linear-gradient(135deg, #0a1428 0%, #0d1a2d 50%, #0f1620 100%);
  min-height: 100vh;
}

.asset-page.theme-blue .page-header h2 {
  color: #ffffff;
  text-shadow: 0 2px 8px rgba(70, 130, 180, 0.3);
}

.asset-page.theme-blue .page-description {
  color: rgba(255, 255, 255, 0.7);
}

.asset-page.theme-blue .asset-tabs {
  background: linear-gradient(135deg,
    rgba(20, 30, 50, 0.85) 0%,
    rgba(13, 26, 45, 0.95) 100%);
  border: 1px solid rgba(70, 130, 180, 0.35);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.45),
              0 0 20px rgba(70, 130, 180, 0.08);
}

.asset-page.theme-blue :deep(.el-tabs__header) {
  background: rgba(20, 30, 50, 0.7);
  border-bottom: 1px solid rgba(70, 130, 180, 0.35);
}

.asset-page.theme-blue :deep(.el-tabs__item) {
  color: rgba(255, 255, 255, 0.7);
}

.asset-page.theme-blue :deep(.el-tabs__item:hover) {
  color: #00d4ff;
}

.asset-page.theme-blue :deep(.el-tabs__item.is-active) {
  color: #00d4ff;
  font-weight: 600;
}

.asset-page.theme-blue :deep(.el-tabs__active-bar) {
  background-color: #00d4ff;
}
</style>
