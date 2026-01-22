<template>
  <div class="asset-project-detail-page" :class="themeClass">
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="header-content">
            <el-breadcrumb separator="/">
              <el-breadcrumb-item>
                <el-button link @click="goBack" class="breadcrumb-btn">
                  <el-icon><ArrowLeft /></el-icon>
                  资产管理
                </el-button>
              </el-breadcrumb-item>
              <el-breadcrumb-item>{{ projectInfo?.displayName || '项目详情' }}</el-breadcrumb-item>
            </el-breadcrumb>
            <h1 class="page-title">🏢 {{ projectInfo?.displayName || '加载中...' }}</h1>
          </div>
          <div>
            <el-button type="primary" size="small" @click="goBack">返回列表</el-button>
          </div>
        </div>
      </template>

      <el-tabs type="border-card" class="detail-tabs" v-model="activeTab">
        <el-tab-pane label="📦 项目资产" name="assets">
          <div class="project-assets-section">
            <ProjectAssetList
              :project-id="projectId"
              :project-name="projectName"
              @asset-updated="handleAssetUpdated"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="🖥️ 主机节点" name="nodes">
          <div class="host-nodes-section">
            <ProjectHostNodeManagement
              :project-id="projectId"
              :project-name="projectName"
              @node-updated="handleNodeUpdated"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="🐳 容器管理" name="containers">
          <div class="container-management-section">
            <ProjectContainerDiscovery
              :project-id="projectId"
              :project-name="projectName"
              @containers-imported="handleContainersImported"
              @switch-to-assets="handleSwitchToAssets"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="🌐 拓扑图" name="topology">
          <div class="topology-section">
            <TopologyGraph
              :selected-project="{ id: projectId, name: projectName }"
              :height="600"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { apiRequest } from '@/utils/apiRequest'
import { getUserRole } from '@/utils/auth'
import ProjectAssetList from '@/components/assets/ProjectAssetList.vue'
import ProjectHostNodeManagement from '@/components/assets/ProjectHostNodeManagement.vue'
import ProjectContainerDiscovery from '@/components/assets/ProjectContainerDiscovery.vue'
import TopologyGraph from './TopologyGraph.vue'

const route = useRoute()
const router = useRouter()

const projectId = computed(() => route.params.id || route.query.projectId)
const projectName = computed(() => route.query.projectName || projectId.value)

// 主题切换：根据角色动态设置主题class
const role = getUserRole() || ''
const themeClass = computed(() => {
  return role === 'blue' ? 'theme-blue' : 'theme-admin'
})

const projectInfo = ref(null)
const activeTab = ref('assets')

// 从projectId解析企业和项目信息
const parseProjectInfo = () => {
  if (projectId.value) {
    const parts = projectId.value.split('｜')
    if (parts.length === 2) {
      projectInfo.value = {
        company: parts[0],
        project: parts[1],
        displayName: projectId.value
      }
    } else {
      projectInfo.value = {
        company: '未知企业',
        project: projectId.value,
        displayName: projectId.value
      }
    }
  }
}


const goBack = () => {
  router.push('/assets')
}

const handleAssetUpdated = () => {
  ElMessage.success('资产更新成功')
}

const handleNodeUpdated = () => {
  ElMessage.success('节点更新成功')
}

const handleContainersImported = (result) => {
  ElMessage.success(`成功导入 ${result.importedCount} 个容器`)
}

const handleSwitchToAssets = () => {
  activeTab.value = 'assets'
  ElMessage.info('已切换到资产管理页面，请为资产配置部署节点')
}

onMounted(() => {
  parseProjectInfo()
})
</script>

<style scoped>
/* ============================================
   Apple Elegant White Style - 苹果高雅白风格
   ============================================ */

/* CSS Variables */
:root {
  --apple-white: #fbfbfd;
  --apple-gray: #f5f5f7;
  --apple-border: rgba(0, 0, 0, 0.04);
  --apple-text: #1d1d1f;
  --apple-text-secondary: #3c3c43;
  --apple-blue: #007aff;
  --apple-green: #34c759;
  --radius-sm: 12px;
  --radius-md: 16px;
  --radius-lg: 20px;
  --shadow-card: 0 4px 16px rgba(0, 0, 0, 0.06);
  --shadow-card-hover: 0 8px 32px rgba(0, 0, 0, 0.12);
  --spacing-md: 16px;
  --spacing-lg: 24px;
  --spacing-xl: 32px;
  --font-apple: -apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", "Helvetica Neue", sans-serif;
}

.asset-project-detail-page {
  padding: var(--spacing-xl);
  background: var(--apple-white);
  min-height: 100vh;
  font-family: var(--font-apple);
}

/* ============================================
   Card Styling - 主卡片
   ============================================ */
.asset-project-detail-page :deep(.el-card) {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.95) 0%,
    rgba(248, 248, 248, 0.85) 100%);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 0.5px solid var(--apple-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  transition: all 0.3s ease;
  overflow: hidden;
}

.asset-project-detail-page :deep(.el-card:hover) {
  box-shadow: var(--shadow-card-hover);
}

.asset-project-detail-page :deep(.el-card__header) {
  background: var(--apple-gray);
  border-bottom: 0.5px solid var(--apple-border);
  padding: 24px 24px 5px 24px;
}

.asset-project-detail-page :deep(.el-card__body) {
  padding: 0 20px 20px 20px;
}

/* ============================================
   Header Styling - 页面标题
   ============================================ */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: nowrap;
  gap: var(--spacing-md);
}

.header-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
  min-width: 0; /* 允许内容收缩 */
}

.header-content :deep(.el-breadcrumb) {
  font-size: 13px;
  color: var(--apple-text-secondary);
  flex-shrink: 0;
  white-space: nowrap;
  display: flex;
  align-items: center;
}

.header-content :deep(.el-breadcrumb__item) {
  display: inline-flex;
  align-items: center;
}

.header-content :deep(.el-breadcrumb__separator) {
  margin: 0 8px;
}

.breadcrumb-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 0;
  color: var(--apple-blue);
  font-weight: 500;
  transition: all 0.2s ease;
}

.breadcrumb-btn:hover {
  color: #0051d5;
  transform: translateX(-2px);
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--apple-text);
  margin: 0;
  letter-spacing: -0.5px;
  line-height: 1.2;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ============================================
   Buttons - 按钮样式
   ============================================ */
.card-header :deep(.el-button) {
  height: 40px;
  min-width: 110px;
  font-size: 14px;
  padding: 0 20px;
  border-radius: var(--radius-sm);
  font-weight: 500;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

.card-header :deep(.el-button--primary) {
  background: var(--apple-blue);
  border-color: var(--apple-blue);
}

.card-header :deep(.el-button--primary:hover) {
  background: #0051d5;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.3);
}

.card-header :deep(.el-button.is-plain) {
  background: rgba(0, 122, 255, 0.05);
  border-color: var(--apple-blue);
  color: var(--apple-blue);
}

.card-header :deep(.el-button.is-plain:hover) {
  background: rgba(0, 122, 255, 0.1);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.15);
}

/* ============================================
   Tabs Styling - 标签页
   ============================================ */
.detail-tabs {
  margin-top: 5px;
}

.detail-tabs :deep(.el-tabs__header) {
  margin: 0;
  padding: 8px var(--spacing-md);
  background: var(--apple-gray);
  border: 0.5px solid var(--apple-border);
  border-radius: var(--radius-md) var(--radius-md) 0 0;
}

.detail-tabs :deep(.el-tabs__nav) {
  border: none;
}

.detail-tabs :deep(.el-tabs__item) {
  font-size: 15px;
  font-weight: 500;
  color: var(--apple-text-secondary);
  padding: 0 20px;
  height: 52px;
  line-height: 52px;
  border: none;
  transition: all 0.3s ease;
}

.detail-tabs :deep(.el-tabs__item:hover) {
  color: var(--apple-blue);
}

.detail-tabs :deep(.el-tabs__item.is-active) {
  color: var(--apple-blue);
  font-weight: 600;
}

.detail-tabs :deep(.el-tabs__active-bar) {
  background-color: var(--apple-blue);
  height: 3px;
  border-radius: 1.5px;
}

.detail-tabs :deep(.el-tabs__content) {
  padding: 10px;
  background: white;
  border: 0.5px solid var(--apple-border);
  border-top: none;
  border-radius: 0 0 var(--radius-md) var(--radius-md);
}

/* ============================================
   Content Sections - 内容区域
   ============================================ */
.project-assets-section,
.host-nodes-section,
.container-management-section,
.topology-section,
.project-statistics-section {
  padding: 0;
  min-height: 400px;
}

/* ============================================
   Responsive Design - 响应式设计
   ============================================ */
@media (max-width: 768px) {
  .asset-project-detail-page {
    padding: 16px;
  }

  .card-header {
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
  }

  .page-title {
    font-size: 24px;
  }

  .card-header :deep(.el-button) {
    width: 100%;
  }

  .detail-tabs :deep(.el-tabs__item) {
    font-size: 13px;
    padding: 0 12px;
    height: 44px;
    line-height: 44px;
  }
}

@media (max-width: 576px) {
  .asset-project-detail-page {
    padding: 12px;
  }

  .page-title {
    font-size: 20px;
  }

  .asset-project-detail-page :deep(.el-card__header),
  .asset-project-detail-page :deep(.el-card__body) {
    padding: var(--spacing-md);
  }
}

/* ============================================
   蓝队主题 - Blue Team Theme
   仅在蓝队用户登录时生效
   ============================================ */
.asset-project-detail-page.theme-blue {
  background: linear-gradient(135deg, #0a1428 0%, #0d1a2d 50%, #0f1620 100%);
  min-height: 100vh;
  padding: 32px;
}

.asset-project-detail-page.theme-blue :deep(.el-card) {
  background: linear-gradient(135deg,
    rgba(20, 30, 50, 0.85) 0%,
    rgba(13, 26, 45, 0.95) 100%);
  backdrop-filter: blur(25px);
  -webkit-backdrop-filter: blur(25px);
  border: 1px solid rgba(70, 130, 180, 0.35);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.45),
              0 0 20px rgba(70, 130, 180, 0.08),
              inset 0 1px 1px rgba(255, 255, 255, 0.08);
}

.asset-project-detail-page.theme-blue :deep(.el-card__header) {
  background: rgba(20, 30, 50, 0.7);
  border-bottom: 1px solid rgba(70, 130, 180, 0.35);
}

.asset-project-detail-page.theme-blue .card-header {
  color: #ffffff;
}

.asset-project-detail-page.theme-blue .page-title {
  color: #ffffff !important;
  font-weight: 700 !important;
}

.asset-project-detail-page.theme-blue :deep(.el-breadcrumb__item) {
  color: rgba(255, 255, 255, 0.7);
}

.asset-project-detail-page.theme-blue .breadcrumb-btn {
  color: #00d4ff;
}

.asset-project-detail-page.theme-blue .breadcrumb-btn:hover {
  color: #1e90ff;
}

/* Tabs样式 */
.asset-project-detail-page.theme-blue .detail-tabs {
  background: transparent;
  border: none;
}

.asset-project-detail-page.theme-blue :deep(.el-tabs__header) {
  background: rgba(20, 30, 50, 0.7);
  border: 1px solid rgba(70, 130, 180, 0.35);
}

.asset-project-detail-page.theme-blue :deep(.el-tabs__item) {
  color: rgba(255, 255, 255, 0.7);
  border: none;
}

.asset-project-detail-page.theme-blue :deep(.el-tabs__item:hover) {
  color: #00d4ff;
}

.asset-project-detail-page.theme-blue :deep(.el-tabs__item.is-active) {
  color: #00d4ff;
  font-weight: 600;
}

.asset-project-detail-page.theme-blue :deep(.el-tabs__active-bar) {
  background-color: #00d4ff;
}

.asset-project-detail-page.theme-blue :deep(.el-tabs__content) {
  background: rgba(13, 26, 45, 0.6);
  border: 1px solid rgba(70, 130, 180, 0.25);
  border-top: none;
}

/* 按钮样式 */
.asset-project-detail-page.theme-blue :deep(.el-button--primary) {
  background: linear-gradient(135deg, #4682b4 0%, #1e90ff 100%);
  border-color: rgba(70, 130, 180, 0.6);
  color: #ffffff;
  box-shadow: 0 4px 15px rgba(70, 130, 180, 0.2),
              inset 0 1px 0 rgba(255, 255, 255, 0.1);
}

.asset-project-detail-page.theme-blue :deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, #5fa3d4 0%, #00d4ff 100%);
  border-color: rgba(70, 130, 180, 0.8);
  box-shadow: 0 8px 25px rgba(70, 130, 180, 0.4),
              0 0 20px rgba(30, 144, 255, 0.3),
              inset 0 1px 0 rgba(255, 255, 255, 0.15);
  transform: translateY(-2px);
}
</style>
