<template>
  <div class="red-team-drill-info">
    <!-- 动态光晕背景层 -->
    <div class="dynamic-glow-layer">
      <div class="glow-spot glow-1"></div>
      <div class="glow-spot glow-2"></div>
      <div class="glow-spot glow-3"></div>
    </div>

    <el-card class="info-card">
      <template #header>
        <div class="card-header">
          <h2>🎯 演练目标信息</h2>
          <el-button
            v-if="hasAccess"
            @click="refreshData"
            :loading="loading"
            type="primary"
            size="small">
            刷新
          </el-button>
        </div>
      </template>

      <!-- 未授权状态：需要录屏 -->
      <div v-if="!hasAccess" class="unauthorized-state">
        <div class="icon-wrapper">
          <el-icon :size="80" color="#ff3b30">
            <VideoCamera />
          </el-icon>
        </div>
        <h3>需要开启录屏才能查看演练目标</h3>
        <p class="hint">为确保演练过程可追溯，请先开启屏幕录制</p>
        <el-button
          type="primary"
          size="large"
          @click="startRecordingForDrill"
          :loading="isStartingRecording">
          <el-icon><VideoCameraFilled /></el-icon>
          开始录屏
        </el-button>
        <el-alert
          type="info"
          :closable="false"
          show-icon
          style="margin-top: 30px">
          <template #title>
            <span>录屏说明</span>
          </template>
          <ul style="margin: 10px 0 0 0; padding-left: 20px;">
            <li>点击"开始录屏"将打开录屏窗口</li>
            <li>在录屏窗口中选择要共享的屏幕</li>
            <li>录屏开始后，本页面将自动显示演练目标信息</li>
            <li>本次登录会话中，关闭录屏后仍可查看目标信息</li>
          </ul>
        </el-alert>
      </div>

      <!-- 已授权状态：显示演练信息 -->
      <div v-else>
        <!-- 演练列表 -->
      <div v-if="!selectedDrill" class="drill-list">
        <h3>当前活跃演练</h3>
        <el-table :data="activeDrills" v-loading="loading" stripe>
          <el-table-column prop="name" label="演练名称" />
          <el-table-column prop="status" label="状态">
            <template #default="scope">
              <el-tag :type="getStatusType(scope.row.status)">
                {{ getStatusText(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="scope">
              <el-button 
                size="small" 
                type="primary"
                @click="selectDrill(scope.row)"
                :disabled="scope.row.status !== 'active'"
              >
                查看目标
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 演练目标信息 -->
      <div v-if="selectedDrill" class="drill-details">
        <div class="drill-header">
          <h3>{{ selectedDrill.name }}</h3>
          <el-button size="small" @click="selectedDrill = null">返回列表</el-button>
        </div>

        <el-divider />

        <!-- 互联网出口信息 -->
        <div class="target-info">
          <h4>🌐 互联网出口信息</h4>
          <el-alert 
            type="info" 
            :closable="false"
            show-icon
            title="目标范围"
            description="以下为本次演练的合法攻击目标，请勿攻击范围外的系统"
          />
          
          <div class="target-list" v-if="drillTargets.length > 0">
            <el-table :data="drillTargets" border class="target-table">
              <el-table-column prop="name" label="目标名称" />
              <el-table-column prop="ip" label="IP地址">
                <template #default="scope">
                  <el-tag type="success">{{ scope.row.ip }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="port" label="开放端口">
                <template #default="scope">
                  <span v-if="scope.row.ports && scope.row.ports.length > 0">
                    {{ scope.row.ports.join(', ') }}
                  </span>
                  <span v-else class="no-data">待探测</span>
                </template>
              </el-table-column>
              <el-table-column prop="description" label="描述" />
            </el-table>
          </div>

          <div v-else class="no-targets">
            <el-empty description="暂无目标信息" />
          </div>
        </div>

        <!-- 演练规则 -->
        <div class="drill-rules">
          <h4>📋 演练规则</h4>
          <el-card shadow="never" class="rules-card">
            <ul>
              <li>仅允许攻击上述列出的目标系统</li>
              <li>禁止使用拒绝服务(DoS/DDoS)攻击</li>
              <li>禁止删除或破坏目标系统数据</li>
              <li>发现漏洞后请及时提交报告</li>
              <li>遵守演练时间窗口限制</li>
            </ul>
          </el-card>
        </div>

        <!-- 提交入口 -->
        <div class="submit-section">
          <el-button type="primary" size="large" @click="goToSubmit">
            提交攻击成果
          </el-button>
        </div>
      </div>
      </div><!-- 关闭 v-else 的 div -->
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { VideoCamera, VideoCameraFilled } from '@element-plus/icons-vue'
import axios from '@/api/axios'
import { useRecordingChannel } from '@/composables/useRecordingChannel'

const router = useRouter()
const loading = ref(false)
const activeDrills = ref([])
const selectedDrill = ref(null)
const drillTargets = ref([])

// 录屏管理
const { isRecording, startRecording: openRecordingWindow, handleRecordingStatus, onMessage } = useRecordingChannel()
const isStartingRecording = ref(false)

// 检查是否有访问权限（录屏激活 或 本次会话已授权）
const hasAccess = computed(() => {
  return isRecording.value || sessionStorage.getItem('drill-access-granted') === 'true'
})

// 获取活跃演练列表
const fetchActiveDrills = async () => {
  loading.value = true
  try {
    const response = await axios.get('/red/active-drills')
    activeDrills.value = response || []
  } catch (error) {
    // 使用模拟数据
    activeDrills.value = [
      {
        id: 1,
        name: '苏州科技大学｜网络安全演练项目',
        status: 'active',
        startTime: '2025-01-01T00:00:00',
        endTime: '2025-12-31T23:59:59'
      }
    ]
  } finally {
    loading.value = false
  }
}

// 获取演练目标信息
const fetchDrillTargets = async (drillId) => {
  loading.value = true
  try {
    const response = await axios.get(`/red/drill-targets/${drillId}`)
    drillTargets.value = response || []
  } catch (error) {
    // 使用模拟数据
    drillTargets.value = [
      {
        name: 'Web服务器',
        ip: '192.168.1.100',
        ports: [80, 443],
        description: '主要Web应用服务器'
      },
      {
        name: 'API网关',
        ip: '192.168.1.101',
        ports: [8080, 8443],
        description: 'API服务网关'
      },
      {
        name: 'FTP服务器',
        ip: '192.168.1.102',
        ports: [21, 22],
        description: '文件传输服务'
      }
    ]
  } finally {
    loading.value = false
  }
}

// 选择演练
const selectDrill = async (drill) => {
  selectedDrill.value = drill
  await fetchDrillTargets(drill.id)
}

// 刷新数据
const refreshData = () => {
  if (selectedDrill.value) {
    fetchDrillTargets(selectedDrill.value.id)
  } else {
    fetchActiveDrills()
  }
}

// 获取状态类型
const getStatusType = (status) => {
  const statusMap = {
    'active': 'success',
    'pending': 'warning',
    'completed': 'info',
    'stopped': 'danger'
  }
  return statusMap[status] || 'info'
}

// 获取状态文本
const getStatusText = (status) => {
  const statusMap = {
    'active': '进行中',
    'pending': '待开始',
    'completed': '已完成',
    'stopped': '已停止'
  }
  return statusMap[status] || status
}

// 跳转到提交页面
const goToSubmit = () => {
  router.push('/achievement/red-team-submit')
}

// 开始录屏
const startRecordingForDrill = async () => {
  try {
    isStartingRecording.value = true
    await openRecordingWindow()
    ElMessage.success('请在弹出的窗口中开始录屏')
  } catch (error) {
    console.error('打开录屏窗口失败:', error)
    ElMessage.error('打开录屏窗口失败: ' + error.message)
  } finally {
    isStartingRecording.value = false
  }
}

// 监听录屏状态变化
watch(isRecording, (newValue) => {
  if (newValue) {
    // 录屏开始，授予访问权限
    sessionStorage.setItem('drill-access-granted', 'true')
    console.log('✅ 录屏已激活，授予演练目标访问权限')

    // 加载演练数据
    if (activeDrills.value.length === 0) {
      fetchActiveDrills()
    }
  }
})

onMounted(() => {
  // 监听录屏状态消息
  const cleanup = onMessage(handleRecordingStatus)
  if (cleanup) {
    // Vue 3.5+ 支持在 onMounted 中使用 onUnmounted
    const { onUnmounted } = require('vue')
    onUnmounted(cleanup)
  }

  // 如果已有访问权限，直接加载数据
  if (hasAccess.value) {
    fetchActiveDrills()
    console.log('📋 已有访问权限，加载演练数据')
  } else {
    console.log('🔒 未授权访问，需要开启录屏')
  }
})
</script>

<style scoped>
/* ============================================
   演练目标信息 - 深色黑客科技风 + Apple优雅
   ============================================ */

:root {
  --hacker-bg: #0a0a0a;
  --hacker-bg-secondary: #1a0d0d;
  --hacker-red: #ff3b30;
  --hacker-red-glow: rgba(255, 59, 48, 0.3);
  --hacker-text: #ffffff;
  --hacker-text-secondary: rgba(255, 255, 255, 0.7);
  --hacker-glass: rgba(20, 20, 20, 0.6);
  --font-apple: -apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", "Helvetica Neue", sans-serif;
  --font-mono: "SF Mono", Consolas, Monaco, monospace;
}

.red-team-drill-info {
  min-height: 100vh;
  background: linear-gradient(135deg, #0a0a0a 0%, #1a0d0d 50%, #0f0f0f 100%);
  color: var(--hacker-text);
  font-family: var(--font-apple);
  padding: 30px;
  position: relative;
  overflow: hidden;
}

/* 动态光晕背景层 */
.dynamic-glow-layer {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
}

.glow-spot {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.3;
}

.glow-1 {
  top: 15%;
  left: 25%;
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(255, 59, 48, 0.2) 0%, transparent 70%);
  animation: glow-breath-1 8s ease-in-out infinite;
}

.glow-2 {
  bottom: 20%;
  right: 15%;
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, rgba(204, 0, 0, 0.15) 0%, transparent 70%);
  animation: glow-breath-2 10s ease-in-out infinite;
  animation-delay: 2s;
}

.glow-3 {
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

/* 主卡片 */
.info-card {
  max-width: 1400px;
  margin: 0 auto;
  background: var(--hacker-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 16px;
  border: 0.5px solid rgba(255, 59, 48, 0.2);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4), inset 0 0 0 0.5px rgba(255, 255, 255, 0.05);
  position: relative;
  z-index: 1;
}

.info-card :deep(.el-card__header) {
  background: rgba(30, 30, 30, 0.5);
  border-bottom: 1px solid rgba(255, 59, 48, 0.2);
  padding: 20px 24px;
}

.info-card :deep(.el-card__body) {
  color: var(--hacker-text-secondary);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #ffffff;
  letter-spacing: -0.5px;
  text-shadow: 0 0 20px var(--hacker-red-glow);
}

/* 刷新按钮 */
.card-header :deep(.el-button) {
  background: linear-gradient(135deg, rgba(255, 59, 48, 0.15) 0%, rgba(255, 59, 48, 0.25) 100%);
  border: 0.5px solid rgba(255, 59, 48, 0.3);
  color: #ffffff;
  font-weight: 600;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
  box-shadow: 0 2px 8px rgba(255, 59, 48, 0.15);
}

.card-header :deep(.el-button:hover) {
  background: linear-gradient(135deg, rgba(255, 59, 48, 0.25) 0%, rgba(255, 59, 48, 0.35) 100%);
  border-color: rgba(255, 59, 48, 0.5);
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(255, 59, 48, 0.25);
}

/* 演练列表 */
.drill-list {
  margin-bottom: 20px;
}

.drill-list h3 {
  margin-bottom: 20px;
  font-size: 18px;
  font-weight: 600;
  color: #ffffff;
  letter-spacing: -0.3px;
}

/* 表格深色主题 */
.drill-list :deep(.el-table) {
  background: transparent;
  color: var(--hacker-text);
}

.drill-list :deep(.el-table__inner-wrapper)::before {
  display: none;
}

.drill-list :deep(.el-table th.el-table__cell) {
  background: rgba(30, 30, 30, 0.6);
  color: rgba(255, 255, 255, 0.9);
  font-weight: 600;
  border-bottom: 1px solid rgba(255, 59, 48, 0.2);
}

.drill-list :deep(.el-table tr) {
  background: transparent;
}

.drill-list :deep(.el-table td.el-table__cell) {
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.8);
}

.drill-list :deep(.el-table__body tr:hover > td) {
  background: rgba(255, 59, 48, 0.08) !important;
}

.drill-list :deep(.el-table__empty-block) {
  background: transparent;
}

/* 表格按钮 */
.drill-list :deep(.el-button--small) {
  background: linear-gradient(135deg, rgba(255, 59, 48, 0.15) 0%, rgba(255, 59, 48, 0.25) 100%);
  border: 0.5px solid rgba(255, 59, 48, 0.3);
  color: #ffffff;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

.drill-list :deep(.el-button--small:hover) {
  background: linear-gradient(135deg, rgba(255, 59, 48, 0.25) 0%, rgba(255, 59, 48, 0.35) 100%);
  border-color: rgba(255, 59, 48, 0.5);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(255, 59, 48, 0.2);
}

.drill-list :deep(.el-button--small.is-disabled) {
  background: rgba(142, 142, 147, 0.15);
  border-color: rgba(142, 142, 147, 0.2);
  color: rgba(255, 255, 255, 0.3);
}

/* Tag 标签 */
.drill-list :deep(.el-tag) {
  background: rgba(52, 199, 89, 0.2);
  border-color: rgba(52, 199, 89, 0.4);
  color: #34c759;
  font-weight: 600;
}

.drill-list :deep(.el-tag.el-tag--success) {
  background: rgba(52, 199, 89, 0.2);
  border-color: rgba(52, 199, 89, 0.4);
  color: #34c759;
}

.drill-list :deep(.el-tag.el-tag--warning) {
  background: rgba(255, 149, 0, 0.2);
  border-color: rgba(255, 149, 0, 0.4);
  color: #ff9500;
}

.drill-list :deep(.el-tag.el-tag--info) {
  background: rgba(0, 122, 255, 0.2);
  border-color: rgba(0, 122, 255, 0.4);
  color: #007aff;
}

.drill-list :deep(.el-tag.el-tag--danger) {
  background: rgba(255, 59, 48, 0.2);
  border-color: rgba(255, 59, 48, 0.4);
  color: #ff3b30;
}

/* 演练详情 */
.drill-details {
  animation: fadeIn 0.5s cubic-bezier(0.19, 1, 0.22, 1);
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.drill-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.drill-header h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #ffffff;
  letter-spacing: -0.3px;
}

.drill-header :deep(.el-button) {
  background: linear-gradient(135deg, rgba(142, 142, 147, 0.15) 0%, rgba(142, 142, 147, 0.25) 100%);
  border: 0.5px solid rgba(142, 142, 147, 0.3);
  color: #ffffff;
  font-weight: 600;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

.drill-header :deep(.el-button:hover) {
  background: linear-gradient(135deg, rgba(142, 142, 147, 0.25) 0%, rgba(142, 142, 147, 0.35) 100%);
  border-color: rgba(142, 142, 147, 0.5);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(142, 142, 147, 0.2);
}

/* 分割线 */
:deep(.el-divider) {
  background-color: rgba(255, 255, 255, 0.1);
}

/* 目标信息 */
.target-info {
  margin-bottom: 30px;
}

.target-info h4 {
  margin-bottom: 20px;
  font-size: 18px;
  font-weight: 600;
  color: #ffffff;
  letter-spacing: -0.3px;
}

/* Alert 提示 */
.target-info :deep(.el-alert) {
  background: rgba(0, 122, 255, 0.15);
  border: 0.5px solid rgba(0, 122, 255, 0.3);
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: 20px;
}

.target-info :deep(.el-alert__title) {
  color: #ffffff;
  font-weight: 600;
}

.target-info :deep(.el-alert__description) {
  color: rgba(255, 255, 255, 0.8);
}

.target-info :deep(.el-alert .el-alert__icon) {
  color: #007aff;
}

.target-list {
  margin-top: 15px;
}

/* 目标表格 */
.target-table {
  width: 100%;
}

.target-info :deep(.el-table) {
  background: transparent;
  color: var(--hacker-text);
}

.target-info :deep(.el-table__inner-wrapper)::before {
  display: none;
}

.target-info :deep(.el-table th.el-table__cell) {
  background: rgba(30, 30, 30, 0.6);
  color: rgba(255, 255, 255, 0.9);
  font-weight: 600;
  border-bottom: 1px solid rgba(255, 59, 48, 0.2);
}

.target-info :deep(.el-table tr) {
  background: transparent;
}

.target-info :deep(.el-table td.el-table__cell) {
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.8);
  border-left: 1px solid rgba(255, 255, 255, 0.05);
  border-right: 1px solid rgba(255, 255, 255, 0.05);
}

.target-info :deep(.el-table__body tr:hover > td) {
  background: rgba(255, 59, 48, 0.08) !important;
}

.target-info :deep(.el-tag.el-tag--success) {
  background: rgba(52, 199, 89, 0.2);
  border-color: rgba(52, 199, 89, 0.4);
  color: #34c759;
  font-family: var(--font-mono);
  font-weight: 600;
}

.no-data {
  color: rgba(255, 255, 255, 0.4);
  font-style: italic;
  font-size: 12px;
}

.no-targets {
  padding: 60px 0;
  text-align: center;
}

.no-targets :deep(.el-empty) {
  --el-empty-description-color: rgba(255, 255, 255, 0.6);
}

/* 演练规则 */
.drill-rules {
  margin-bottom: 30px;
}

.drill-rules h4 {
  margin-bottom: 20px;
  font-size: 18px;
  font-weight: 600;
  color: #ffffff;
  letter-spacing: -0.3px;
}

.rules-card {
  background: rgba(30, 30, 30, 0.5);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 0.5px solid rgba(255, 59, 48, 0.2);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
}

.rules-card :deep(.el-card__body) {
  padding: 24px;
}

.rules-card ul {
  margin: 0;
  padding-left: 24px;
  list-style-type: none;
}

.rules-card li {
  margin-bottom: 14px;
  color: rgba(255, 255, 255, 0.85);
  line-height: 1.8;
  font-size: 14px;
  position: relative;
  padding-left: 8px;
}

.rules-card li:last-child {
  margin-bottom: 0;
}

.rules-card li::before {
  content: '▸';
  position: absolute;
  left: -16px;
  color: #ff3b30;
  font-weight: 700;
  text-shadow: 0 0 10px rgba(255, 59, 48, 0.5);
}

/* 提交区域 */
.submit-section {
  text-align: center;
  padding: 30px 0 10px 0;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  margin-top: 20px;
}

.submit-section :deep(.el-button) {
  background: linear-gradient(135deg, rgba(255, 59, 48, 0.2) 0%, rgba(255, 59, 48, 0.35) 100%);
  border: 0.5px solid rgba(255, 59, 48, 0.4);
  color: #ffffff;
  font-weight: 600;
  font-size: 15px;
  padding: 14px 32px;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
  box-shadow: 0 4px 16px rgba(255, 59, 48, 0.2);
}

.submit-section :deep(.el-button:hover) {
  background: linear-gradient(135deg, rgba(255, 59, 48, 0.3) 0%, rgba(255, 59, 48, 0.45) 100%);
  border-color: rgba(255, 59, 48, 0.6);
  transform: translateY(-2px);
  box-shadow: 0 6px 24px rgba(255, 59, 48, 0.3);
}

.submit-section :deep(.el-button:active) {
  transform: translateY(0);
}

/* Loading 加载状态 */
:deep(.el-loading-mask) {
  background-color: rgba(10, 10, 10, 0.8);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

:deep(.el-loading-spinner .circular) {
  stroke: #ff3b30;
}

:deep(.el-loading-spinner .el-loading-text) {
  color: rgba(255, 255, 255, 0.8);
}

/* 未授权状态 */
.unauthorized-state {
  text-align: center;
  padding: 80px 40px;
  max-width: 700px;
  margin: 0 auto;
}

.unauthorized-state .icon-wrapper {
  margin-bottom: 30px;
}

.unauthorized-state .icon-wrapper :deep(.el-icon) {
  filter: drop-shadow(0 0 25px #ff3b30);
  animation: pulse-icon 2s ease-in-out infinite;
}

@keyframes pulse-icon {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.7;
    transform: scale(1.1);
  }
}

.unauthorized-state h3 {
  font-size: 24px;
  font-weight: 700;
  color: #ffffff;
  margin-bottom: 16px;
  letter-spacing: -0.5px;
}

.unauthorized-state .hint {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 40px;
  line-height: 1.6;
}

.unauthorized-state :deep(.el-button) {
  background: linear-gradient(135deg, rgba(255, 59, 48, 0.2) 0%, rgba(255, 59, 48, 0.35) 100%);
  border: 0.5px solid rgba(255, 59, 48, 0.4);
  color: #ffffff;
  font-weight: 600;
  font-size: 16px;
  padding: 16px 40px;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
  box-shadow: 0 4px 20px rgba(255, 59, 48, 0.25);
}

.unauthorized-state :deep(.el-button:hover) {
  background: linear-gradient(135deg, rgba(255, 59, 48, 0.3) 0%, rgba(255, 59, 48, 0.45) 100%);
  border-color: rgba(255, 59, 48, 0.6);
  transform: translateY(-2px);
  box-shadow: 0 6px 28px rgba(255, 59, 48, 0.35);
}

.unauthorized-state :deep(.el-alert) {
  background: rgba(0, 122, 255, 0.12);
  border: 0.5px solid rgba(0, 122, 255, 0.25);
  color: rgba(255, 255, 255, 0.9);
  text-align: left;
}

.unauthorized-state :deep(.el-alert__title) {
  color: #ffffff;
  font-weight: 600;
}

.unauthorized-state :deep(.el-alert ul) {
  color: rgba(255, 255, 255, 0.8);
  line-height: 1.8;
}

.unauthorized-state :deep(.el-alert ul li) {
  margin-bottom: 8px;
}

.unauthorized-state :deep(.el-alert .el-alert__icon) {
  color: #007aff;
}
</style>