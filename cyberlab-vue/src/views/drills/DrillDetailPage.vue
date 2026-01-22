<template>
  <div class="drill-detail-page" :class="themeClass">
    <!-- Dashboard Header with large emoji and centered title -->
    <div class="dashboard-header">
      <div class="header-content">
        <h2>🎯 {{ drill?.name || '演练详情' }}</h2>
        <p class="page-description">{{ drill?.description || '管理演练配置、监控容器状态和项目资产' }}</p>
      </div>
      <div class="header-actions">
        <el-button plain size="large" :icon="FullScreen" @click="toggleFullscreen">全屏</el-button>
        <el-button type="primary" size="large" :icon="Back" @click="goBack">返回列表</el-button>
      </div>
    </div>

    <!-- Content Card with Apple Style -->
    <el-card class="content-card">
      <el-tabs type="border-card" class="detail-tabs">
        <el-tab-pane label="📋 基本信息">
          <div class="basic-info-section">
            <!-- 编辑控制按钮 - 仅管理员可见 -->
            <div class="edit-controls" v-if="!isEditing && role !== 'blue'">
              <el-button type="primary" size="default" :icon="Edit" @click="startEditing">编辑信息</el-button>
            </div>
            <div class="edit-controls" v-else-if="isEditing && role !== 'blue'">
              <el-button type="success" size="default" :icon="Check" @click="saveChanges" :loading="saving">保存</el-button>
              <el-button size="default" :icon="Close" @click="cancelEditing">取消</el-button>
            </div>

            <!-- 演练基本信息展示/编辑表单 -->
            <el-row :gutter="24" class="info-row">
              <el-col :xs="24" :sm="24" :md="8">
                <el-card shadow="never" class="info-card">
                  <template #header>
                    <div class="info-card-header">
                      <span class="info-icon">📝</span>
                      <span class="card-title">演练信息</span>
                    </div>
                  </template>

                  <el-form v-if="isEditing" :model="editForm" label-width="120px" class="edit-form">
                    <el-form-item label="演练名称">
                      <el-input v-model="editForm.name" placeholder="请输入演练名称" />
                    </el-form-item>

                    <el-form-item label="演练描述">
                      <el-input v-model="editForm.description" type="textarea" :rows="3" placeholder="请输入演练描述" />
                    </el-form-item>

                    <el-form-item label="演练类型">
                      <el-select v-model="editForm.drillType" placeholder="请选择演练类型" style="width: 100%">
                        <el-option label="攻防演练" value="attack_defense" />
                        <el-option label="蓝队演练" value="blue_team" />
                        <el-option label="红队演练" value="red_team" />
                        <el-option label="混合演练" value="mixed" />
                      </el-select>
                    </el-form-item>

                    <el-form-item label="难度等级">
                      <el-select v-model="editForm.difficultyLevel" placeholder="请选择难度等级" style="width: 100%">
                        <el-option label="初级" value="beginner" />
                        <el-option label="中级" value="intermediate" />
                        <el-option label="高级" value="advanced" />
                      </el-select>
                    </el-form-item>

                    <el-form-item label="最大参与人数">
                      <el-input-number v-model="editForm.maxParticipants" :min="1" :max="100" placeholder="请输入最大参与人数" style="width: 100%" />
                    </el-form-item>
                  </el-form>

                  <el-descriptions v-else :column="1" border class="info-descriptions">
                    <el-descriptions-item label="演练名称">
                      <span class="description-content">{{ drill?.name || '未设置' }}</span>
                    </el-descriptions-item>
                    <el-descriptions-item label="演练描述">
                      <span class="description-content">{{ drill?.description || '未设置' }}</span>
                    </el-descriptions-item>
                    <el-descriptions-item label="演练类型">
                      <el-tag :type="getDrillTypeTagType(drill?.drillType)">{{ getDrillTypeLabel(drill?.drillType) }}</el-tag>
                    </el-descriptions-item>
                    <el-descriptions-item label="难度等级">
                      <el-tag :type="getDifficultyTagType(drill?.difficultyLevel)">{{ getDifficultyLabel(drill?.difficultyLevel) }}</el-tag>
                    </el-descriptions-item>
                    <el-descriptions-item label="最大参与人数">
                      <span class="description-content">{{ drill?.maxParticipants || '未限制' }}人</span>
                    </el-descriptions-item>
                  </el-descriptions>
                </el-card>
              </el-col>

              <el-col :xs="24" :sm="24" :md="8">
                <el-card shadow="never" class="info-card">
                  <template #header>
                    <div class="info-card-header">
                      <span class="info-icon">⚡</span>
                      <span class="card-title">状态信息</span>
                    </div>
                  </template>

                  <el-descriptions :column="1" border class="info-descriptions">
                    <el-descriptions-item label="当前状态">
                      <el-tag :type="getStatusTagType(drill?.status)" size="default">{{ getStatusLabel(drill?.status) }}</el-tag>
                    </el-descriptions-item>
                    <el-descriptions-item label="创建时间">
                      <span class="description-content">{{ formatDateTime(drill?.createdAt) }}</span>
                    </el-descriptions-item>
                    <el-descriptions-item label="更新时间">
                      <span class="description-content">{{ formatDateTime(drill?.updatedAt) || '未更新' }}</span>
                    </el-descriptions-item>
                    <el-descriptions-item label="创建者ID">
                      <span class="description-content">{{ drill?.creatorId || '未设置' }}</span>
                    </el-descriptions-item>
                    <el-descriptions-item label="拓扑项目ID">
                      <div v-if="drill?.topologyProjectId" class="topology-project-info">
                        <span class="topology-project-id">{{ drill.topologyProjectId }}</span>
                        <el-button link size="small" @click="goToTopologyEditor">查看拓扑</el-button>
                      </div>
                      <span v-else class="description-content text-muted">未关联</span>
                    </el-descriptions-item>
                  </el-descriptions>
                </el-card>
              </el-col>

              <el-col :xs="24" :sm="24" :md="8">
                <el-card shadow="never" class="info-card">
                  <template #header>
                    <div class="card-header-with-actions">
                      <div class="info-card-header">
                        <span class="info-icon">📊</span>
                        <span class="card-title">项目资产统计</span>
                      </div>
                      <el-button link size="small" :icon="Refresh" @click="refreshAssetStats" :loading="loadingAssetStats">刷新</el-button>
                    </div>
                  </template>

                  <div v-if="loadingAssetStats" class="asset-stats-loading">
                    <el-skeleton :rows="3" animated />
                  </div>

                  <div v-else-if="assetStats" class="asset-stats">
                    <el-descriptions :column="1" border class="info-descriptions">
                      <el-descriptions-item label="关联资产总数">
                        <el-tag type="primary">{{ assetStats.totalAssets || 0 }}</el-tag>
                      </el-descriptions-item>
                      <el-descriptions-item label="容器资产">
                        <el-tag type="success">{{ assetStats.containerAssets || 0 }}</el-tag>
                      </el-descriptions-item>
                      <el-descriptions-item label="已启用资产">
                        <el-tag type="info">{{ assetStats.enabledAssets || 0 }}</el-tag>
                      </el-descriptions-item>
                      <el-descriptions-item label="网络分组">
                        <el-tag type="warning">{{ assetStats.networkSegments || 0 }} 个</el-tag>
                      </el-descriptions-item>
                      <el-descriptions-item label="部署节点">
                        <el-tag>{{ assetStats.deploymentNodes || 0 }} 个</el-tag>
                      </el-descriptions-item>
                    </el-descriptions>

                    <div class="asset-stats-actions" v-if="assetStats.totalAssets > 0">
                      <el-button type="primary" size="default" @click="viewProjectAssets">查看资产详情</el-button>
                    </div>
                  </div>

                  <div v-else class="no-asset-stats">
                    <el-empty description="暂无资产统计数据" :image-size="60">
                      <el-button type="primary" size="small" @click="refreshAssetStats">获取统计</el-button>
                    </el-empty>
                  </div>
                </el-card>
              </el-col>
            </el-row>

            <!-- 网络拓扑展示区域 -->
            <el-row class="topology-row">
              <el-col :span="24">
                <el-card shadow="never" class="topology-card">
                  <template #header>
                    <div class="card-header-with-actions">
                      <div class="info-card-header">
                        <span class="info-icon">🌐</span>
                        <span class="card-title">网络拓扑</span>
                      </div>
                      <el-button size="default" :icon="FullScreen" @click="viewFullTopology">全屏查看</el-button>
                    </div>
                  </template>

                  <div class="topology-container">
                    <div v-if="drill?.topologyData && hasValidTopologyData" class="topology-content">
                      <!-- 集成拓扑可视化组件 -->
                      <DrillTopologyViewer :topology-data="drill.topologyData" :height="300" />
                    </div>
                    <div v-else class="no-topology">
                      <el-empty description="暂无拓扑数据">
                        <template #image>
                          <el-icon size="80"><Connection /></el-icon>
                        </template>
                        <template #default>
                          <div v-if="drill?.topologyProjectId" class="topology-actions">
                            <p>关联的拓扑项目：<strong>{{ getTopologyProjectDisplayName(drill.topologyProjectId) }}</strong></p>
                            <p class="topology-project-id">项目ID：{{ drill.topologyProjectId }}</p>
                            <el-button type="primary" @click="goToTopologyEditor">编辑拓扑图</el-button>
                          </div>
                          <div v-else class="topology-actions">
                            <p>该演练尚未关联拓扑图</p>
                            <el-button type="primary" @click="associateTopology">关联拓扑项目</el-button>
                          </div>
                        </template>
                      </el-empty>
                    </div>
                  </div>
                </el-card>
              </el-col>
            </el-row>
          </div>
        </el-tab-pane>

        <el-tab-pane label="🐳 容器状态" v-if="drillId">
          <div class="container-status-section">
            <DrillContainerStatus :drill-id="Number(drillId)" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 全屏拓扑查看Dialog -->
    <el-dialog
      v-model="showFullTopologyDialog"
      title="🌐 网络拓扑全屏查看"
      :width="'90%'"
      :top="'5vh'"
      :show-close="true"
      :close-on-click-modal="false"
      class="full-topology-dialog"
    >
      <div class="full-topology-container">
        <DrillTopologyViewer
          v-if="drill?.topologyData"
          :topology-data="drill.topologyData"
          :height="600"
          :show-labels="true"
        />
        <div v-else class="no-topology-data">
          <el-empty description="暂无拓扑数据">
            <template #image>
              <el-icon size="80"><Connection /></el-icon>
            </template>
          </el-empty>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button :icon="Close" @click="closeFullTopologyDialog">关闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, ElDialog } from 'element-plus'
import { Connection, FullScreen, Back, Edit, Check, Close, Refresh } from '@element-plus/icons-vue'
import DrillContainerStatus from './DrillContainerStatus.vue'
import DrillTopologyViewer from './DrillTopologyViewer.vue'
import { getProjectAssetStats } from '@/api/projectMonitor'
import { getUserRole } from '@/utils/auth'

const route = useRoute()
const router = useRouter()
const drillId = route.params.id

// 主题支持
const role = getUserRole() || ''
const themeClass = computed(() => role === 'blue' ? 'theme-blue' : 'theme-admin')

// 为body添加主题class，以便全局组件应用主题
onMounted(() => {
  if (role === 'blue') {
    document.body.classList.add('blue-theme-active')
  }
})

// 组件卸载时移除body的主题class
onUnmounted(() => {
  if (role === 'blue') {
    document.body.classList.remove('blue-theme-active')
  }
})

const drill = ref(null)
const isEditing = ref(false)
const saving = ref(false)
const showFullTopologyDialog = ref(false)
const assetStats = ref(null)
const loadingAssetStats = ref(false)
const editForm = reactive({
  name: '',
  description: '',
  drillType: '',
  difficultyLevel: '',
  maxParticipants: null
})

// 检查是否有有效的拓扑数据
const hasValidTopologyData = computed(() => {
  if (!drill.value?.topologyData) {
    return false
  }

  const data = drill.value.topologyData

  // 检查是否有节点、连线或自定义元素中的任意一种
  const hasNodes = data.nodes && Array.isArray(data.nodes) && data.nodes.length > 0
  const hasLinks = data.links && Array.isArray(data.links) && data.links.length > 0
  const hasCustomElements = data.customElements && Array.isArray(data.customElements) && data.customElements.length > 0


  // 只要有其中任意一种元素就认为是有效的
  const isValid = hasNodes || hasLinks || hasCustomElements

  return isValid
})

const fetchDrillDetail = async () => {
  try {
    const res = await fetch(`/api/range/${drillId}`, {
      headers: {
        'Authorization': 'Bearer ' + localStorage.getItem('token'),
        'Content-Type': 'application/json'
      }
    })
    if (!res.ok) throw new Error('请求失败')
    const drillData = await res.json()


    drill.value = drillData

    // 自动获取资产统计
    if (drillData.topologyProjectId) {
      await refreshAssetStats()
    }
  } catch (error) {
    ElMessage.error('获取演练详情失败')
  }
}

const goBack = () => {
  router.push('/drills')
}

const toggleFullscreen = async () => {
  try {
    // 使用更安全的全屏API调用方式
    if (document.fullscreenElement) {
      // 当前已经是全屏状态，退出全屏
      await document.exitFullscreen()
    } else {
      // 寻找页面根元素进行全屏
      const elem = document.querySelector('.drill-detail-page') || document.documentElement

      // 检查元素是否存在且在文档中
      if (!elem || !document.contains(elem)) {
        ElMessage.error('无法找到页面元素，全屏功能暂时不可用')
        return
      }

      // 检查浏览器是否支持全屏API
      if (!elem.requestFullscreen) {
        ElMessage.warning('当前浏览器不支持全屏功能')
        return
      }

      // 请求全屏，带错误处理
      await elem.requestFullscreen()
      ElMessage.success('已进入全屏模式，按ESC键退出')
    }
  } catch (error) {
    if (error.name === 'NotAllowedError') {
      ElMessage.error('全屏请求被拒绝，请检查浏览器设置或重试')
    } else if (error.name === 'TypeError') {
      ElMessage.error('全屏功能不可用，可能元素已被移除')
    } else {
      ElMessage.error('全屏功能异常: ' + error.message)
    }
  }
}

// 开始编辑
const startEditing = () => {
  if (!drill.value) return

  // 复制当前数据到编辑表单
  editForm.name = drill.value.name || ''
  editForm.description = drill.value.description || ''
  editForm.drillType = drill.value.drillType || ''
  editForm.difficultyLevel = drill.value.difficultyLevel || ''
  editForm.maxParticipants = drill.value.maxParticipants || null

  isEditing.value = true
}

// 取消编辑
const cancelEditing = () => {
  isEditing.value = false
  // 清空表单
  Object.keys(editForm).forEach(key => {
    editForm[key] = ''
  })
  editForm.maxParticipants = null
}

// 保存更改
const saveChanges = async () => {
  try {
    saving.value = true

    const updateData = {
      name: editForm.name,
      description: editForm.description,
      drillType: editForm.drillType,
      difficultyLevel: editForm.difficultyLevel,
      maxParticipants: editForm.maxParticipants
    }

    const res = await fetch(`/api/range/${drillId}`, {
      method: 'PUT',
      headers: {
        'Authorization': 'Bearer ' + localStorage.getItem('token'),
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(updateData)
    })

    if (!res.ok) throw new Error('更新失败')

    const result = await res.json()
    if (result.success) {
      ElMessage.success('演练信息更新成功')
      isEditing.value = false
      // 重新获取数据
      await fetchDrillDetail()
    } else {
      throw new Error(result.message || '更新失败')
    }
  } catch (error) {
    ElMessage.error('保存失败: ' + error.message)
  } finally {
    saving.value = false
  }
}

// 查看完整拓扑
const viewFullTopology = () => {
  if (!hasValidTopologyData.value) {
    ElMessage.warning('暂无拓扑数据')
    return
  }
  showFullTopologyDialog.value = true
}

// 跳转到拓扑编辑器
const goToTopologyEditor = () => {
  if (drill.value?.topologyProjectId) {
    router.push({
      path: '/topology',
      query: {
        projectId: drill.value.topologyProjectId,
        projectName: `演练项目-${drill.value.name || drill.value.id}`
      }
    })
  }
}

// 关联拓扑项目
const associateTopology = async () => {
  try {
    const { value: projectId } = await ElMessageBox.prompt(
      '请输入要关联的拓扑项目ID（建议格式：drill-{演练名称}）',
      '关联拓扑项目',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPlaceholder: `drill-${drill.value?.name || 'new'}`,
        inputValue: `drill-${drill.value?.name || drill.value?.id || 'new'}`
      }
    )

    if (!projectId || !projectId.trim()) {
      ElMessage.warning('项目ID不能为空')
      return
    }

    // 更新演练的拓扑项目ID
    const updateData = { topologyProjectId: projectId.trim() }

    const res = await fetch(`/api/range/${drillId}`, {
      method: 'PUT',
      headers: {
        'Authorization': 'Bearer ' + localStorage.getItem('token'),
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(updateData)
    })

    if (!res.ok) throw new Error('关联失败')

    ElMessage.success('拓扑项目关联成功')
    // 重新获取演练数据
    await fetchDrillDetail()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('关联拓扑项目失败: ' + (error.message || '未知错误'))
    }
  }
}

// 关闭全屏拓扑Dialog
const closeFullTopologyDialog = () => {
  showFullTopologyDialog.value = false
}

// 获取演练类型标签样式
const getDrillTypeTagType = (type) => {
  const typeMap = {
    'attack_defense': 'danger',
    'blue_team': 'primary',
    'red_team': 'warning',
    'mixed': 'success'
  }
  return typeMap[type] || 'info'
}

// 获取演练类型标签文本
const getDrillTypeLabel = (type) => {
  const labelMap = {
    'attack_defense': '攻防演练',
    'blue_team': '蓝队演练',
    'red_team': '红队演练',
    'mixed': '混合演练'
  }
  return labelMap[type] || '未设置'
}

// 获取难度等级标签样式
const getDifficultyTagType = (level) => {
  const levelMap = {
    'beginner': 'success',
    'intermediate': 'warning',
    'advanced': 'danger'
  }
  return levelMap[level] || 'info'
}

// 获取难度等级标签文本
const getDifficultyLabel = (level) => {
  const labelMap = {
    'beginner': '初级',
    'intermediate': '中级',
    'advanced': '高级'
  }
  return labelMap[level] || '未设置'
}

// 获取状态标签样式
const getStatusTagType = (status) => {
  const statusMap = {
    'running': 'success',
    '运行中': 'success',
    'paused': 'warning',
    '已暂停': 'warning',
    'stopped': 'info',
    '已停止': 'info'
  }
  return statusMap[status] || 'info'
}

// 获取状态标签文本
const getStatusLabel = (status) => {
  return status || '未知状态'
}

// 格式化日期时间
const formatDateTime = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return date.toLocaleString('zh-CN')
}

// 获取拓扑项目显示名称
const getTopologyProjectDisplayName = (projectId) => {
  if (!projectId) return '未知项目'

  // 如果ID包含'drill-'前缀，去除前缀显示更友好的名称
  if (projectId.startsWith('drill-')) {
    const displayName = projectId.substring(6) // 移除'drill-'前缀
    return displayName || projectId
  }

  return projectId
}

// 刷新资产统计信息
const refreshAssetStats = async () => {
  if (!drill.value?.topologyProjectId) {
    return
  }

  try {
    loadingAssetStats.value = true

    const stats = await getProjectAssetStats(drill.value.topologyProjectId)
    assetStats.value = stats || {
      totalAssets: 0,
      containerAssets: 0,
      enabledAssets: 0,
      networkSegments: 0,
      deploymentNodes: 0,
      projectId: drill.value.topologyProjectId
    }

  } catch (error) {
    // 不显示错误消息，只是不显示统计数据
    assetStats.value = null
  } finally {
    loadingAssetStats.value = false
  }
}

// 查看项目资产详情
const viewProjectAssets = () => {
  if (!drill.value?.topologyProjectId) {
    ElMessage.warning('未关联拓扑项目')
    return
  }

  // 跳转到资产管理页面，并筛选当前项目
  router.push({
    path: '/assets',
    query: {
      projectId: drill.value.topologyProjectId,
      projectName: getTopologyProjectDisplayName(drill.value.topologyProjectId)
    }
  })
}

onMounted(fetchDrillDetail)
</script>

<style scoped>
/* ============================================
   Apple Elegant White Style - 苹果高雅白风格
   ============================================ */

/* CSS Variables for consistency */
:root {
  --apple-white: #fbfbfd;
  --apple-gray: #f5f5f7;
  --apple-border: rgba(0, 0, 0, 0.04);
  --apple-text: #1d1d1f;
  --apple-text-secondary: #86868b;
  --apple-blue: #007aff;
  --apple-green: #34c759;
  --apple-orange: #ff9500;
  --apple-red: #ff3b30;
  --radius-sm: 12px;
  --radius-md: 16px;
  --radius-lg: 20px;
  --radius-xl: 24px;
  --shadow-card: 0 4px 16px rgba(0, 0, 0, 0.06);
  --shadow-card-hover: 0 8px 32px rgba(0, 0, 0, 0.12);
  --spacing-xs: 8px;
  --spacing-sm: 12px;
  --spacing-md: 16px;
  --spacing-lg: 24px;
  --spacing-xl: 32px;
  --spacing-2xl: 48px;
  --font-apple: -apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", "Helvetica Neue", sans-serif;
}

.drill-detail-page {
  padding: var(--spacing-xl);
  background: var(--apple-white);
  min-height: 100vh;
  font-family: var(--font-apple);
}

/* ============================================
   Dashboard Header - Large Emoji & Centered Title
   ============================================ */
.dashboard-header {
  text-align: center;
  margin-bottom: var(--spacing-xl);
  padding: var(--spacing-lg) 0;
  position: relative;
}

.header-content h2 {
  font-size: 42px;
  font-weight: 700;
  color: var(--apple-text);
  margin: 0 0 var(--spacing-sm) 0;
  letter-spacing: -0.5px;
  line-height: 1.1;
}

.page-description {
  font-size: 16px;
  color: var(--apple-text-secondary);
  margin: 0 0 var(--spacing-lg) 0;
  font-weight: 400;
}

.header-actions {
  display: flex;
  justify-content: center;
  gap: var(--spacing-md);
  flex-wrap: wrap;
  margin-top: var(--spacing-lg);
}

.header-actions .el-button {
  min-width: 120px;
  height: 44px;
  font-size: 15px;
  font-weight: 500;
  border-radius: var(--radius-sm);
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

.header-actions .el-button--primary {
  background: var(--apple-blue);
  border-color: var(--apple-blue);
}

.header-actions .el-button--primary:hover {
  background: #0051d5;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.3);
}

/* ============================================
   Content Card
   ============================================ */
.content-card {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.95) 0%,
    rgba(248, 248, 248, 0.85) 100%);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 0.5px solid var(--apple-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  transition: all 0.3s ease;
}

.content-card :deep(.el-card__body) {
  padding: var(--spacing-lg);
}

/* ============================================
   Tabs Styling
   ============================================ */
.detail-tabs {
  border-radius: var(--radius-md);
  overflow: hidden;
  border: none;
}

.detail-tabs :deep(.el-tabs__header) {
  background: var(--apple-gray);
  margin: 0;
  border-radius: var(--radius-md) var(--radius-md) 0 0;
}

.detail-tabs :deep(.el-tabs__item) {
  font-size: 15px;
  font-weight: 500;
  color: var(--apple-text-secondary);
  transition: all 0.3s ease;
}

.detail-tabs :deep(.el-tabs__item.is-active) {
  color: var(--apple-blue);
  font-weight: 600;
}

.detail-tabs :deep(.el-tabs__item:hover) {
  color: var(--apple-blue);
}

/* ============================================
   Basic Info Section
   ============================================ */
.basic-info-section {
  padding: var(--spacing-lg) 0;
}

.edit-controls {
  text-align: right;
  margin-bottom: var(--spacing-lg);
}

.edit-controls .el-button {
  min-width: 100px;
  height: 40px;
  border-radius: var(--radius-sm);
  font-weight: 500;
}

.info-row {
  margin-bottom: var(--spacing-xl);
}

.topology-row {
  margin-top: var(--spacing-xl);
}

/* ============================================
   Info Cards
   ============================================ */
.info-card {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.98) 0%,
    rgba(248, 248, 248, 0.9) 100%);
  backdrop-filter: blur(15px);
  -webkit-backdrop-filter: blur(15px);
  border: 0.5px solid var(--apple-border);
  border-radius: var(--radius-md);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
  height: 100%;
  margin-bottom: var(--spacing-lg);
}

.info-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.08);
}

.info-card :deep(.el-card__header) {
  background: transparent;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  padding: var(--spacing-md) var(--spacing-lg);
}

.info-card :deep(.el-card__body) {
  padding: var(--spacing-lg);
}

.info-card-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.info-icon {
  font-size: 24px;
  line-height: 1;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.1));
}

.card-title {
  font-weight: 600;
  font-size: 16px;
  color: var(--apple-text);
}

.card-header-with-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

/* ============================================
   Descriptions
   ============================================ */
.info-descriptions :deep(.el-descriptions__label) {
  font-weight: 500;
  color: var(--apple-text-secondary);
  font-size: 14px;
}

.info-descriptions :deep(.el-descriptions__content) {
  font-size: 14px;
}

.description-content {
  color: var(--apple-text);
  line-height: 1.6;
  word-break: break-word;
}

.text-muted {
  color: var(--apple-text-secondary);
}

/* ============================================
   Edit Form
   ============================================ */
.edit-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: var(--apple-text);
}

.edit-form :deep(.el-input__inner),
.edit-form :deep(.el-textarea__inner) {
  border-radius: var(--radius-sm);
  border-color: rgba(0, 0, 0, 0.1);
}

.edit-form :deep(.el-input__inner:focus),
.edit-form :deep(.el-textarea__inner:focus) {
  border-color: var(--apple-blue);
}

/* ============================================
   Topology Card
   ============================================ */
.topology-card {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.98) 0%,
    rgba(248, 248, 248, 0.9) 100%);
  backdrop-filter: blur(15px);
  -webkit-backdrop-filter: blur(15px);
  border: 0.5px solid var(--apple-border);
  border-radius: var(--radius-md);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  min-height: 400px;
}

.topology-card :deep(.el-card__header) {
  background: transparent;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  padding: var(--spacing-md) var(--spacing-lg);
}

.topology-container {
  min-height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.topology-content {
  width: 100%;
  height: 300px;
}

.no-topology {
  width: 100%;
  height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.topology-actions {
  text-align: center;
  padding: var(--spacing-lg);
}

.topology-actions p {
  margin-bottom: var(--spacing-md);
  color: var(--apple-text-secondary);
  font-size: 14px;
}

.topology-project-id {
  font-size: 12px;
  color: var(--apple-text-secondary);
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
}

.topology-project-info {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  flex-wrap: wrap;
}

/* ============================================
   Asset Stats
   ============================================ */
.asset-stats-loading {
  padding: var(--spacing-lg);
}

.asset-stats {
  padding-bottom: var(--spacing-sm);
}

.asset-stats-actions {
  margin-top: var(--spacing-md);
  text-align: center;
  padding-top: var(--spacing-md);
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}

.no-asset-stats {
  min-height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* ============================================
   Container Status Section
   ============================================ */
.container-status-section {
  padding-top: var(--spacing-lg);
}

/* ============================================
   Full Topology Dialog
   ============================================ */
.full-topology-dialog :deep(.el-dialog) {
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card-hover);
}

.full-topology-dialog :deep(.el-dialog__header) {
  background: var(--apple-gray);
  border-radius: var(--radius-lg) var(--radius-lg) 0 0;
  padding: var(--spacing-lg);
}

.full-topology-dialog :deep(.el-dialog__title) {
  font-size: 20px;
  font-weight: 600;
  color: var(--apple-text);
}

.full-topology-dialog :deep(.el-dialog__body) {
  padding: var(--spacing-lg);
}

.full-topology-container {
  width: 100%;
  min-height: 600px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--apple-white);
  border-radius: var(--radius-md);
}

.no-topology-data {
  width: 100%;
  height: 600px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.dialog-footer {
  text-align: center;
}

.dialog-footer .el-button {
  min-width: 120px;
  height: 40px;
  border-radius: var(--radius-sm);
}

/* ============================================
   Responsive Design - Mobile Optimization
   ============================================ */
@media (max-width: 1200px) {
  .info-row .el-col {
    margin-bottom: var(--spacing-lg);
  }
}

@media (max-width: 992px) {
  .header-content h2 {
    font-size: 36px;
  }

  .page-description {
    font-size: 15px;
  }
}

@media (max-width: 768px) {
  .drill-detail-page {
    padding: var(--spacing-md);
  }

  .header-content h2 {
    font-size: 32px;
  }

  .page-description {
    font-size: 14px;
  }

  .dashboard-header {
    padding: var(--spacing-md) 0;
  }

  .header-actions {
    flex-direction: column;
    width: 100%;
  }

  .header-actions .el-button {
    width: 100%;
  }

  .info-row .el-col {
    margin-bottom: var(--spacing-md);
  }
}

@media (max-width: 576px) {
  .header-content h2 {
    font-size: 28px;
  }

  .full-topology-dialog :deep(.el-dialog) {
    width: 95% !important;
  }
}

/* ==================== 蓝队主题样式 ==================== */
/* 主容器 */
.drill-detail-page.theme-blue {
  background: linear-gradient(135deg, #0a1428 0%, #0d1a2d 50%, #0f1620 100%) !important;
  min-height: 100vh;
  padding: var(--spacing-xl);
}

/* Dashboard header */
.drill-detail-page.theme-blue .dashboard-header {
  background: linear-gradient(135deg,
    rgba(20, 30, 50, 0.85) 0%,
    rgba(13, 26, 45, 0.95) 100%) !important;
  border: 1px solid rgba(70, 130, 180, 0.35) !important;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.45),
              0 0 20px rgba(70, 130, 180, 0.08) !important;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  padding: var(--spacing-xl);
  margin-bottom: var(--spacing-xl);
}

.drill-detail-page.theme-blue .header-content h2 {
  color: #ffffff !important;
  text-shadow: 0 0 20px rgba(0, 212, 255, 0.3) !important;
  font-weight: 600;
}

.drill-detail-page.theme-blue .header-content .page-description {
  color: rgba(255, 255, 255, 0.75) !important;
}

/* Header actions buttons */
.drill-detail-page.theme-blue .header-actions :deep(.el-button) {
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1) !important;
}

.drill-detail-page.theme-blue .header-actions :deep(.el-button--primary) {
  background: linear-gradient(135deg, #4682b4 0%, #1e90ff 100%) !important;
  border: 1px solid rgba(70, 130, 180, 0.6) !important;
  color: #ffffff !important;
  box-shadow: 0 4px 15px rgba(70, 130, 180, 0.2) !important;
}

.drill-detail-page.theme-blue .header-actions :deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, #5a9fd4 0%, #3ea8ff 100%) !important;
  transform: translateY(-2px);
}

.drill-detail-page.theme-blue .header-actions :deep(.el-button.is-plain) {
  background: rgba(20, 30, 50, 0.6) !important;
  border: 1px solid rgba(70, 130, 180, 0.4) !important;
  color: rgba(255, 255, 255, 0.85) !important;
}

.drill-detail-page.theme-blue .header-actions :deep(.el-button.is-plain:hover) {
  background: rgba(70, 130, 180, 0.3) !important;
  color: #ffffff !important;
}

/* 卡片样式 */
.drill-detail-page.theme-blue :deep(.el-card) {
  background: linear-gradient(135deg,
    rgba(20, 30, 50, 0.85) 0%,
    rgba(13, 26, 45, 0.95) 100%) !important;
  border: 1px solid rgba(70, 130, 180, 0.35) !important;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.45),
              0 0 20px rgba(70, 130, 180, 0.08) !important;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
}

.drill-detail-page.theme-blue :deep(.el-card__header) {
  background: linear-gradient(135deg,
    rgba(70, 130, 180, 0.2) 0%,
    rgba(30, 144, 255, 0.15) 100%) !important;
  border-bottom: 1px solid rgba(70, 130, 180, 0.3) !important;
}

.drill-detail-page.theme-blue :deep(.el-card.content-card) {
  border-radius: 16px;
}

/* Info cards */
.drill-detail-page.theme-blue .info-card-header {
  color: #ffffff !important;
  font-weight: 600;
}

.drill-detail-page.theme-blue .info-card-header .info-icon {
  text-shadow: 0 0 10px rgba(0, 212, 255, 0.3);
}

.drill-detail-page.theme-blue .card-title {
  color: #ffffff !important;
  text-shadow: 0 0 10px rgba(0, 212, 255, 0.2);
}

/* Tabs */
.drill-detail-page.theme-blue :deep(.el-tabs--border-card) {
  background: linear-gradient(135deg,
    rgba(13, 26, 45, 0.9) 0%,
    rgba(10, 20, 40, 0.95) 100%) !important;
  border: 1px solid rgba(70, 130, 180, 0.4) !important;
  box-shadow: none !important;
}

.drill-detail-page.theme-blue :deep(.el-tabs__header) {
  background: linear-gradient(135deg,
    rgba(70, 130, 180, 0.15) 0%,
    rgba(30, 144, 255, 0.1) 100%) !important;
  border-bottom: 1px solid rgba(70, 130, 180, 0.3) !important;
}

.drill-detail-page.theme-blue :deep(.el-tabs__item) {
  color: rgba(255, 255, 255, 0.7) !important;
  border: none !important;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

.drill-detail-page.theme-blue :deep(.el-tabs__item:hover) {
  color: #ffffff !important;
}

.drill-detail-page.theme-blue :deep(.el-tabs__item.is-active) {
  color: #00d4ff !important;
  background: rgba(70, 130, 180, 0.2) !important;
  border-bottom: 2px solid #00d4ff !important;
  font-weight: 600;
}

.drill-detail-page.theme-blue :deep(.el-tabs__content) {
  color: rgba(255, 255, 255, 0.85) !important;
}

/* Descriptions 描述列表 */
.drill-detail-page.theme-blue :deep(.el-descriptions) {
  background: transparent !important;
}

.drill-detail-page.theme-blue :deep(.el-descriptions__header) {
  color: #ffffff !important;
}

.drill-detail-page.theme-blue :deep(.el-descriptions__body) {
  background: transparent !important;
}

.drill-detail-page.theme-blue :deep(.el-descriptions__label) {
  background: rgba(70, 130, 180, 0.15) !important;
  color: rgba(255, 255, 255, 0.8) !important;
  border-color: rgba(70, 130, 180, 0.2) !important;
  font-weight: 500;
}

.drill-detail-page.theme-blue :deep(.el-descriptions__content) {
  background: rgba(20, 30, 50, 0.3) !important;
  color: rgba(255, 255, 255, 0.9) !important;
  border-color: rgba(70, 130, 180, 0.2) !important;
}

.drill-detail-page.theme-blue .description-content {
  color: rgba(255, 255, 255, 0.9) !important;
}

/* 表单元素 */
.drill-detail-page.theme-blue :deep(.el-form-item__label) {
  color: rgba(255, 255, 255, 0.85) !important;
  font-weight: 500;
}

.drill-detail-page.theme-blue :deep(.el-input__wrapper) {
  background: rgba(20, 30, 50, 0.6) !important;
  border: 1px solid rgba(70, 130, 180, 0.35) !important;
  box-shadow: 0 0 0 1px transparent inset !important;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1) !important;
}

.drill-detail-page.theme-blue :deep(.el-input__wrapper:hover) {
  border-color: rgba(70, 130, 180, 0.5) !important;
}

.drill-detail-page.theme-blue :deep(.el-input__wrapper.is-focus) {
  border-color: #4682b4 !important;
  box-shadow: 0 0 0 1px #4682b4 inset,
              0 0 12px rgba(70, 130, 180, 0.3) !important;
}

.drill-detail-page.theme-blue :deep(.el-input__inner) {
  color: #ffffff !important;
}

.drill-detail-page.theme-blue :deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.4) !important;
}

.drill-detail-page.theme-blue :deep(.el-textarea__inner) {
  background: rgba(20, 30, 50, 0.6) !important;
  border: 1px solid rgba(70, 130, 180, 0.35) !important;
  color: #ffffff !important;
}

.drill-detail-page.theme-blue :deep(.el-textarea__inner::placeholder) {
  color: rgba(255, 255, 255, 0.4) !important;
}

.drill-detail-page.theme-blue :deep(.el-select .el-input__wrapper) {
  background: rgba(20, 30, 50, 0.6) !important;
  border: 1px solid rgba(70, 130, 180, 0.35) !important;
}

.drill-detail-page.theme-blue :deep(.el-input-number .el-input__wrapper) {
  background: rgba(20, 30, 50, 0.6) !important;
  border: 1px solid rgba(70, 130, 180, 0.35) !important;
}

/* 按钮样式 */
.drill-detail-page.theme-blue :deep(.el-button--primary) {
  background: linear-gradient(135deg, #4682b4 0%, #1e90ff 100%) !important;
  border: 1px solid rgba(70, 130, 180, 0.6) !important;
  color: #ffffff !important;
  box-shadow: 0 4px 15px rgba(70, 130, 180, 0.2),
              inset 0 1px 0 rgba(255, 255, 255, 0.1) !important;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1) !important;
}

.drill-detail-page.theme-blue :deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, #5a9fd4 0%, #3ea8ff 100%) !important;
  box-shadow: 0 6px 20px rgba(70, 130, 180, 0.35),
              inset 0 1px 0 rgba(255, 255, 255, 0.15) !important;
  transform: translateY(-2px);
}

.drill-detail-page.theme-blue :deep(.el-button--success) {
  background: linear-gradient(135deg, #34c759 0%, #30d158 100%) !important;
  border: 1px solid rgba(52, 199, 89, 0.6) !important;
  box-shadow: 0 4px 15px rgba(52, 199, 89, 0.2) !important;
}

.drill-detail-page.theme-blue :deep(.el-button--success:hover) {
  background: linear-gradient(135deg, #4ad766 0%, #46e165 100%) !important;
  transform: translateY(-2px);
}

.drill-detail-page.theme-blue :deep(.el-button--default) {
  background: rgba(20, 30, 50, 0.6) !important;
  border: 1px solid rgba(70, 130, 180, 0.4) !important;
  color: rgba(255, 255, 255, 0.85) !important;
}

.drill-detail-page.theme-blue :deep(.el-button--default:hover) {
  background: rgba(70, 130, 180, 0.3) !important;
  color: #ffffff !important;
}

.drill-detail-page.theme-blue :deep(.el-button.is-link) {
  color: #00d4ff !important;
  font-weight: 500;
}

.drill-detail-page.theme-blue :deep(.el-button.is-link:hover) {
  color: #4ddbff !important;
  text-shadow: 0 0 8px rgba(0, 212, 255, 0.4) !important;
}

/* Tags */
.drill-detail-page.theme-blue :deep(.el-tag) {
  border: 1px solid rgba(70, 130, 180, 0.4) !important;
  background: rgba(20, 30, 50, 0.6) !important;
  color: #ffffff !important;
  font-weight: 500;
}

.drill-detail-page.theme-blue :deep(.el-tag--primary) {
  background: rgba(70, 130, 180, 0.3) !important;
  border-color: rgba(70, 130, 180, 0.6) !important;
  color: #00d4ff !important;
}

.drill-detail-page.theme-blue :deep(.el-tag--success) {
  background: rgba(52, 199, 89, 0.2) !important;
  border-color: rgba(52, 199, 89, 0.5) !important;
  color: #34c759 !important;
}

.drill-detail-page.theme-blue :deep(.el-tag--warning) {
  background: rgba(255, 149, 0, 0.2) !important;
  border-color: rgba(255, 149, 0, 0.5) !important;
  color: #ff9500 !important;
}

.drill-detail-page.theme-blue :deep(.el-tag--danger) {
  background: rgba(255, 59, 48, 0.2) !important;
  border-color: rgba(255, 59, 48, 0.5) !important;
  color: #ff3b30 !important;
}

.drill-detail-page.theme-blue :deep(.el-tag--info) {
  background: rgba(70, 130, 180, 0.2) !important;
  border-color: rgba(70, 130, 180, 0.5) !important;
  color: #4682b4 !important;
}

/* Skeleton loading */
.drill-detail-page.theme-blue :deep(.el-skeleton) {
  background: transparent !important;
}

.drill-detail-page.theme-blue :deep(.el-skeleton__item) {
  background: rgba(70, 130, 180, 0.15) !important;
}

/* Empty state */
.drill-detail-page.theme-blue :deep(.el-empty__description p) {
  color: rgba(255, 255, 255, 0.6) !important;
}

.drill-detail-page.theme-blue :deep(.el-empty__image svg) {
  fill: rgba(255, 255, 255, 0.3) !important;
}

/* 全屏对话框 */
.drill-detail-page.theme-blue :deep(.el-dialog) {
  background: linear-gradient(135deg,
    rgba(13, 26, 45, 0.98) 0%,
    rgba(10, 20, 40, 0.99) 100%) !important;
  border: 1px solid rgba(70, 130, 180, 0.4) !important;
  border-radius: 16px;
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.6),
              0 0 30px rgba(70, 130, 180, 0.15) !important;
  backdrop-filter: blur(30px);
  -webkit-backdrop-filter: blur(30px);
}

.drill-detail-page.theme-blue :deep(.el-dialog__header) {
  background: linear-gradient(135deg,
    rgba(70, 130, 180, 0.25) 0%,
    rgba(30, 144, 255, 0.2) 100%) !important;
  border-bottom: 1px solid rgba(70, 130, 180, 0.35) !important;
  border-radius: 16px 16px 0 0;
}

.drill-detail-page.theme-blue :deep(.el-dialog__title) {
  color: #ffffff !important;
  font-weight: 600;
  text-shadow: 0 0 15px rgba(0, 212, 255, 0.3) !important;
}

.drill-detail-page.theme-blue :deep(.el-dialog__headerbtn .el-dialog__close) {
  color: rgba(255, 255, 255, 0.7) !important;
}

.drill-detail-page.theme-blue :deep(.el-dialog__headerbtn:hover .el-dialog__close) {
  color: #ffffff !important;
}

.drill-detail-page.theme-blue :deep(.el-dialog__body) {
  background: transparent;
}

.drill-detail-page.theme-blue :deep(.el-dialog__footer) {
  border-top: 1px solid rgba(70, 130, 180, 0.25) !important;
  background: rgba(20, 30, 50, 0.5);
  border-radius: 0 0 16px 16px;
}

/* Topology info */
.drill-detail-page.theme-blue .topology-project-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.drill-detail-page.theme-blue .topology-project-id {
  color: #00d4ff !important;
  font-weight: 600;
  font-family: 'Courier New', monospace;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .drill-detail-page.theme-blue {
    padding: var(--spacing-lg);
  }

  .drill-detail-page.theme-blue .dashboard-header {
    padding: var(--spacing-lg);
    margin-bottom: var(--spacing-lg);
  }
}
</style>

<!-- 全局样式 - 处理 teleport 到 body 的组件 -->
<style>
/* 蓝队主题：el-select 下拉菜单 */
body.blue-theme-active .el-select-dropdown {
  background: linear-gradient(135deg,
    rgba(13, 26, 45, 0.98) 0%,
    rgba(10, 20, 40, 0.99) 100%) !important;
  border: 1px solid rgba(70, 130, 180, 0.4) !important;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.6),
              0 0 20px rgba(70, 130, 180, 0.15) !important;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
}

body.blue-theme-active .el-select-dropdown .el-select-dropdown__item {
  color: rgba(255, 255, 255, 0.85) !important;
  background: transparent !important;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

body.blue-theme-active .el-select-dropdown .el-select-dropdown__item:hover {
  background: rgba(70, 130, 180, 0.3) !important;
  color: #ffffff !important;
}

body.blue-theme-active .el-select-dropdown .el-select-dropdown__item.selected {
  background: rgba(70, 130, 180, 0.4) !important;
  color: #00d4ff !important;
  font-weight: 600;
}

/* el-popper 箭头 */
body.blue-theme-active .el-select-dropdown .el-popper__arrow::before {
  background: rgba(13, 26, 45, 0.98) !important;
  border: 1px solid rgba(70, 130, 180, 0.4) !important;
}

/* 对话框遮罩 */
body.blue-theme-active .el-overlay {
  background-color: rgba(0, 0, 0, 0.7) !important;
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
}

/* Message Box */
body.blue-theme-active .el-message-box {
  background: linear-gradient(135deg,
    rgba(13, 26, 45, 0.98) 0%,
    rgba(10, 20, 40, 0.99) 100%) !important;
  border: 1px solid rgba(70, 130, 180, 0.4) !important;
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.6) !important;
  backdrop-filter: blur(30px);
  -webkit-backdrop-filter: blur(30px);
}

body.blue-theme-active .el-message-box__header {
  background: linear-gradient(135deg,
    rgba(70, 130, 180, 0.2) 0%,
    rgba(30, 144, 255, 0.15) 100%) !important;
}

body.blue-theme-active .el-message-box__title {
  color: #ffffff !important;
}

body.blue-theme-active .el-message-box__content {
  color: rgba(255, 255, 255, 0.85) !important;
}

body.blue-theme-active .el-message-box__close {
  color: rgba(255, 255, 255, 0.7) !important;
}

body.blue-theme-active .el-message-box__close:hover {
  color: #ffffff !important;
}
</style>
