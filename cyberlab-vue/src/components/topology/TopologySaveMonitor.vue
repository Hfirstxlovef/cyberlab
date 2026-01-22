<template>
  <div class="topology-save-monitor">
    <!-- 保存状态指示器 -->
    <div class="save-status-indicator" :class="statusClass" @click="handleStatusClick">
      <el-icon v-if="saveStatus.isSaving" class="is-loading">
        <Loading />
      </el-icon>
      <el-icon v-else-if="saveStatus.failedAttempts > 0">
        <Warning />
      </el-icon>
      <el-icon v-else-if="saveStatus.lastSaveTime">
        <SuccessFilled />
      </el-icon>
      <el-icon v-else>
        <Document />
      </el-icon>
      
      <span class="status-text">{{ statusText }}</span>
      
      <!-- 待保存计数 -->
      <el-badge 
        v-if="saveStatus.pendingSaveCount > 0" 
        :value="saveStatus.pendingSaveCount" 
        class="pending-badge"
      />
    </div>

    <!-- 详细信息弹窗 -->
    <el-dialog
      v-model="showDetailsDialog"
      title="拓扑图保存监控"
      width="600px"
      :close-on-click-modal="true"
      :close-on-press-escape="true"
    >
      <div class="save-monitor-content">
        <!-- 实时状态 -->
        <el-card class="status-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span>实时状态</span>
              <el-button 
                size="small" 
                @click="refreshStatus"
                :loading="refreshing"
              >
                刷新
              </el-button>
            </div>
          </template>
          
          <el-descriptions :column="2" border>
            <el-descriptions-item label="当前状态">
              <el-tag :type="saveStatus.isSaving ? 'warning' : 'success'">
                {{ saveStatus.isSaving ? '保存中...' : '空闲' }}
              </el-tag>
            </el-descriptions-item>
            
            <el-descriptions-item label="待保存项目">
              <el-tag :type="saveStatus.pendingSaveCount > 0 ? 'warning' : 'info'">
                {{ saveStatus.pendingSaveCount }} 个
              </el-tag>
            </el-descriptions-item>
            
            <el-descriptions-item label="上次保存时间">
              {{ formatTime(saveStatus.lastSaveTime) }}
            </el-descriptions-item>
            
            <el-descriptions-item label="连续失败次数">
              <el-tag :type="saveStatus.failedAttempts > 0 ? 'danger' : 'success'">
                {{ saveStatus.failedAttempts }}
              </el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 统计信息 -->
        <el-card class="stats-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span>统计信息</span>
              <el-button 
                size="small" 
                type="danger" 
                @click="resetStats"
              >
                重置统计
              </el-button>
            </div>
          </template>
          
          <el-row :gutter="20">
            <el-col :span="8">
              <div class="stat-item">
                <div class="stat-number">{{ saveStats.totalSaves }}</div>
                <div class="stat-label">总保存次数</div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="stat-item">
                <div class="stat-number success">{{ saveStats.successfulSaves }}</div>
                <div class="stat-label">成功次数</div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="stat-item">
                <div class="stat-number error">{{ saveStats.failedSaves }}</div>
                <div class="stat-label">失败次数</div>
              </div>
            </el-col>
          </el-row>
          
          <el-divider />
          
          <el-descriptions :column="1" border>
            <el-descriptions-item label="平均响应时间">
              {{ saveStats.averageResponseTime }}ms
            </el-descriptions-item>
            <el-descriptions-item label="成功率">
              {{ successRate }}%
            </el-descriptions-item>
            <el-descriptions-item label="最后错误" v-if="saveStats.lastError">
              <el-text type="danger" class="error-text">
                {{ saveStats.lastError?.message || '未知错误' }}
              </el-text>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 待保存队列 -->
        <el-card class="queue-card" shadow="never" v-if="queueStatus.queueSize > 0">
          <template #header>
            <div class="card-header">
              <span>待保存队列 ({{ queueStatus.queueSize }})</span>
              <div>
                <el-button 
                  size="small" 
                  type="primary" 
                  @click="saveAllPending"
                  :loading="savingAll"
                >
                  立即全部保存
                </el-button>
                <el-button 
                  size="small" 
                  type="danger" 
                  @click="clearQueue"
                >
                  清空队列
                </el-button>
              </div>
            </div>
          </template>
          
          <el-table :data="queueStatus.pendingItems" size="small">
            <el-table-column prop="projectId" label="项目ID" width="120" />
            <el-table-column prop="nodesCount" label="节点数" width="80" />
            <el-table-column prop="linksCount" label="连线数" width="80" />
            <el-table-column prop="customElementsCount" label="自定义元素" width="100" />
            <el-table-column prop="timestamp" label="加入队列时间" min-width="140">
              <template #default="{ row }">
                {{ formatTime(new Date(row.timestamp)) }}
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </div>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showDetailsDialog = false">
            关闭
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { 
  saveStatus, 
  saveStats, 
  topologySaveManager 
} from '@/utils/topologySaveManager'
import { 
  Loading, 
  Warning, 
  SuccessFilled, 
  Document 
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

// 响应式数据
const showDetailsDialog = ref(false)
const refreshing = ref(false)
const savingAll = ref(false)
const queueStatus = ref({
  queueSize: 0,
  pendingItems: [],
  isSaving: false,
  lastSaveTime: null,
  failedAttempts: 0
})

// 定时器
let statusTimer = null

// 计算属性
const statusClass = computed(() => {
  if (saveStatus.value.isSaving) return 'saving'
  if (saveStatus.value.failedAttempts > 0) return 'error'
  if (saveStatus.value.lastSaveTime) return 'success'
  return 'idle'
})

const statusText = computed(() => {
  if (saveStatus.value.isSaving) return '保存中...'
  if (saveStatus.value.failedAttempts > 0) return `保存失败 (${saveStatus.value.failedAttempts})`
  if (saveStatus.value.lastSaveTime) {
    const timeDiff = Date.now() - new Date(saveStatus.value.lastSaveTime).getTime()
    if (timeDiff < 60000) return '刚刚保存'
    if (timeDiff < 3600000) return `${Math.floor(timeDiff / 60000)}分钟前保存`
    return '较早保存'
  }
  return '未保存'
})

const successRate = computed(() => {
  if (saveStats.totalSaves === 0) return 0
  return Math.round((saveStats.successfulSaves / saveStats.totalSaves) * 100)
})

// 方法
const formatTime = (date) => {
  if (!date) return '从未'
  return new Date(date).toLocaleString('zh-CN')
}

const refreshStatus = async () => {
  refreshing.value = true
  try {
    queueStatus.value = topologySaveManager.getQueueStatus()
    await new Promise(resolve => setTimeout(resolve, 300)) // 模拟延迟
  } finally {
    refreshing.value = false
  }
}

const resetStats = async () => {
  try {
    await ElMessageBox.confirm(
      '这将清空所有保存统计信息，确定继续吗？',
      '确认重置',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    
    topologySaveManager.resetStats()
    ElMessage.success('统计信息已重置')
  } catch {
    // 用户取消
  }
}

const saveAllPending = async () => {
  savingAll.value = true
  try {
    const result = await topologySaveManager.saveAll()
    if (result.failed === 0) {
      ElMessage.success(`全部保存成功 (${result.successful}个项目)`)
    } else {
      ElMessage.warning(`保存完成：${result.successful}个成功，${result.failed}个失败`)
    }
  } catch (error) {
    ElMessage.error('批量保存失败：' + error.message)
  } finally {
    savingAll.value = false
    refreshStatus()
  }
}

const clearQueue = async () => {
  try {
    await ElMessageBox.confirm(
      '这将清空所有待保存的数据，确定继续吗？',
      '确认清空队列',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    
    topologySaveManager.clearQueue()
    refreshStatus()
    ElMessage.success('保存队列已清空')
  } catch {
    // 用户取消
  }
}


// 点击状态指示器显示详情
const handleStatusClick = () => {
  refreshStatus()
  showDetailsDialog.value = true
}

// 生命周期
onMounted(() => {
  refreshStatus()
  
  // 定时刷新状态
  statusTimer = setInterval(() => {
    refreshStatus()
  }, 5000) // 每5秒刷新一次
})

onUnmounted(() => {
  if (statusTimer) {
    clearInterval(statusTimer)
  }
})

// 暴露方法给父组件
defineExpose({
  showDetails: () => {
    refreshStatus()
    showDetailsDialog.value = true
  }
})
</script>

<script>
export default {
  name: 'TopologySaveMonitor'
}
</script>

<style scoped>
.topology-save-monitor {
  display: inline-block;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 0;
}

.save-status-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 14px;
  position: relative;
  border: 1px solid transparent;
  user-select: none;
}

.save-status-indicator:hover {
  background-color: var(--el-fill-color-light);
  border-color: var(--el-border-color);
}

.save-status-indicator.idle {
  color: var(--el-text-color-regular);
}

.save-status-indicator.saving {
  color: var(--el-color-warning);
  background-color: var(--el-color-warning-light-9);
}

.save-status-indicator.success {
  color: var(--el-color-success);
}

.save-status-indicator.error {
  color: var(--el-color-danger);
  background-color: var(--el-color-danger-light-9);
}

.pending-badge {
  margin-left: 4px;
}

.save-monitor-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-item {
  text-align: center;
  padding: 12px 0;
}

.stat-number {
  font-size: 24px;
  font-weight: bold;
  color: var(--el-text-color-primary);
}

.stat-number.success {
  color: var(--el-color-success);
}

.stat-number.error {
  color: var(--el-color-danger);
}

.stat-label {
  font-size: 12px;
  color: var(--el-text-color-regular);
  margin-top: 4px;
}

.error-text {
  font-family: monospace;
  font-size: 12px;
  word-break: break-all;
}

.status-card, .stats-card, .queue-card {
  margin-bottom: 0;
}

.is-loading {
  animation: rotating 2s linear infinite;
}

@keyframes rotating {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}
</style>