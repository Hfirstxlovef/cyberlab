<template>
  <div class="screen-recording-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>🎬 攻击录屏管理</span>
        </div>
      </template>

      <!-- 搜索栏 -->
      <el-form :inline="true" class="search-form">
        <el-form-item label="红队成员">
          <el-input
            v-model="searchForm.username"
            placeholder="输入用户名"
            clearable
            style="width: 200px" />
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="searchForm.timeRange"
            type="datetimerange"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            format="YYYY-MM-DD HH:mm:ss"
            style="width: 380px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="searchRecordings" :icon="Search">
            搜索
          </el-button>
          <el-button @click="resetSearch" :icon="Refresh">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 录屏列表 -->
      <el-table
        :data="recordingList"
        v-loading="loading"
        style="width: 100%"
        :header-cell-style="{ background: 'transparent', color: '#1d1d1f', fontWeight: '600', borderBottom: '1px solid rgba(0,0,0,0.06)' }"
        :row-style="{ height: '60px' }"
        :cell-style="{ padding: '16px 12px', borderBottom: '1px solid rgba(0,0,0,0.04)' }"
        empty-text="暂无录屏数据">
        <el-table-column prop="id" label="ID" width="80" align="center" sortable />
        <el-table-column prop="username" label="录屏人" width="140" sortable />
        <el-table-column prop="fileName" label="文件名" min-width="250" show-overflow-tooltip sortable />
        <el-table-column prop="fileSize" label="文件大小" width="120" align="center" sortable>
          <template #default="{ row }">
            {{ formatFileSize(row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center" sortable>
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              type="primary"
              @click="playVideo(row)"
              :disabled="row.status !== 'completed'">
              <el-icon><VideoPlay /></el-icon>
              播放
            </el-button>
            <el-button
              size="small"
              type="success"
              @click="downloadVideo(row)"
              :disabled="row.status !== 'completed'">
              <el-icon><Download /></el-icon>
              下载
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-if="recordingList.length > 0"
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        class="pagination"
        @current-change="fetchRecordings"
        @size-change="fetchRecordings" />
    </el-card>

    <!-- 视频播放器弹窗 -->
    <el-dialog
      v-model="playerVisible"
      title="录屏回放"
      width="80%"
      :close-on-click-modal="false">
      <div v-if="currentVideo" class="video-container">
        <div class="video-info">
          <p><strong>录屏人：</strong>{{ currentVideo.username }}</p>
          <p><strong>开始时间：</strong>{{ formatDateTime(currentVideo.startTime) }}</p>
          <p><strong>时长：</strong>{{ formatDuration(currentVideo.durationSeconds) }}</p>
        </div>
        <video
          ref="videoPlayer"
          controls
          :src="`/api/screen-recording/${currentVideo.id}/stream`"
          style="width: 100%; max-height: 70vh; background: #000;">
          您的浏览器不支持视频播放
        </video>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, VideoPlay, Download } from '@element-plus/icons-vue'
import axios from '@/api/axios'

const loading = ref(false)
const recordingList = ref([])
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

const searchForm = reactive({
  username: '',
  timeRange: []
})

const playerVisible = ref(false)
const currentVideo = ref(null)
const videoPlayer = ref(null)

// 获取录屏列表
async function fetchRecordings() {
  loading.value = true
  try {
    const params = {}

    if (searchForm.username) {
      params.username = searchForm.username
    }

    if (searchForm.timeRange && searchForm.timeRange.length === 2) {
      params.startTime = searchForm.timeRange[0].toISOString()
      params.endTime = searchForm.timeRange[1].toISOString()
    }

    const response = await axios.get('/screen-recording/search', { params })

    // 安全处理响应数据
    const data = Array.isArray(response) ? response : []

    recordingList.value = data
    total.value = data.length

    // 简单的客户端分页
    const start = (currentPage.value - 1) * pageSize.value
    const end = start + pageSize.value
    recordingList.value = data.slice(start, end)

  } catch (error) {
    console.error('获取录屏列表失败:', error)
    ElMessage.error('获取录屏列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索录屏
function searchRecordings() {
  currentPage.value = 1
  fetchRecordings()
}

// 重置搜索
function resetSearch() {
  searchForm.username = ''
  searchForm.timeRange = []
  currentPage.value = 1
  fetchRecordings()
}

// 播放视频
function playVideo(recording) {
  currentVideo.value = recording
  playerVisible.value = true
}

// 下载视频
function downloadVideo(recording) {
  try {
    // 直接使用下载链接，让浏览器处理下载
    const url = `/api/screen-recording/${recording.id}/download`
    const link = document.createElement('a')
    link.href = url
    link.download = recording.fileName || `recording_${recording.id}.webm`
    link.target = '_blank'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)

    ElMessage.success('开始下载')
  } catch (error) {
    console.error('下载失败:', error)
    ElMessage.error('下载失败')
  }
}

// 格式化日期时间
function formatDateTime(dateTime) {
  if (!dateTime) return '-'
  const date = new Date(dateTime)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

// 格式化时长
function formatDuration(seconds) {
  if (!seconds || seconds <= 0) return '0:00'

  // 防御性编程：取绝对值并向下取整
  const totalSeconds = Math.floor(Math.abs(seconds))

  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const secs = totalSeconds % 60

  if (hours > 0) {
    return `${hours}:${String(minutes).padStart(2, '0')}:${String(secs).padStart(2, '0')}`
  }
  return `${minutes}:${String(secs).padStart(2, '0')}`
}

// 格式化文件大小
function formatFileSize(bytes) {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
  if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
  return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB'
}

// 获取状态类型
function getStatusType(status) {
  const typeMap = {
    recording: 'warning',
    uploading: 'info',
    completed: 'success',
    failed: 'danger'
  }
  return typeMap[status] || 'info'
}

// 获取状态文本
function getStatusText(status) {
  const textMap = {
    recording: '录制中',
    uploading: '上传中',
    completed: '已完成',
    failed: '失败'
  }
  return textMap[status] || status
}

onMounted(() => {
  fetchRecordings()
})
</script>

<style scoped>
/* ============================================
   Apple Elegant White Style - 录屏管理页
   Screen Recording Management Page
   ============================================ */

/* CSS Variables */
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
  --apple-purple: #af52de;
  --radius-sm: 12px;
  --radius-md: 16px;
  --radius-lg: 20px;
  --shadow-card: 0 4px 16px rgba(0, 0, 0, 0.06);
  --shadow-card-hover: 0 8px 32px rgba(0, 0, 0, 0.12);
  --spacing-xs: 8px;
  --spacing-sm: 12px;
  --spacing-md: 16px;
  --spacing-lg: 24px;
  --spacing-xl: 32px;
  --font-apple: -apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", "Helvetica Neue", sans-serif;
}

.screen-recording-manage {
  background: transparent;
  padding: var(--spacing-lg);
  font-family: var(--font-apple);
  min-height: 100vh;
}

/* ============================================
   Main Card
   ============================================ */
:deep(.el-card) {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.95) 0%,
    rgba(250, 250, 250, 0.9) 100%);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  border: 0.5px solid var(--apple-border);
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

:deep(.el-card:hover) {
  box-shadow: var(--shadow-card-hover);
  transform: translateY(-2px);
}

.card-header {
  font-size: 18px;
  font-weight: 700;
  color: var(--apple-text);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header span {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
}

/* ============================================
   Search Form Styling
   ============================================ */
.search-form {
  margin-bottom: var(--spacing-lg);
  padding: var(--spacing-md);
  background: rgba(0, 122, 255, 0.02);
  border-radius: var(--radius-md);
  border: 1px solid rgba(0, 122, 255, 0.08);
}

.search-form :deep(.el-form-item__label) {
  font-weight: 600;
  color: var(--apple-text);
  font-family: var(--font-apple);
}

.search-form :deep(.el-input__wrapper) {
  border-radius: var(--radius-sm);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
  border: 1px solid var(--apple-border);
}

.search-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.08);
  border-color: var(--apple-blue);
}

.search-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 4px 16px rgba(0, 122, 255, 0.15);
  border-color: var(--apple-blue);
}

.search-form :deep(.el-date-editor) {
  border-radius: var(--radius-sm);
}

.search-form :deep(.el-date-editor .el-input__wrapper) {
  border-radius: var(--radius-sm);
}

.search-form :deep(.el-button) {
  border-radius: var(--radius-sm);
  font-weight: 600;
  font-family: var(--font-apple);
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
  padding: 10px 20px;
}

.search-form :deep(.el-button--primary) {
  background: linear-gradient(135deg, var(--apple-blue) 0%, #0051d5 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.25);
}

.search-form :deep(.el-button--primary:hover) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 122, 255, 0.35);
}

.search-form :deep(.el-button--default) {
  background: rgba(0, 0, 0, 0.02);
  border: 1px solid var(--apple-border);
  color: var(--apple-text);
}

.search-form :deep(.el-button--default:hover) {
  background: rgba(0, 0, 0, 0.04);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

/* ============================================
   Table Styling
   ============================================ */
:deep(.el-table) {
  background: transparent;
  border-radius: var(--radius-md);
  overflow: hidden;
  font-family: var(--font-apple);
}

:deep(.el-table__inner-wrapper::before) {
  display: none; /* Remove default border */
}

:deep(.el-table thead) {
  background: rgba(0, 0, 0, 0.02);
}

:deep(.el-table__row) {
  transition: all 0.2s ease;
  background: transparent;
}

:deep(.el-table__row:hover) {
  background: rgba(0, 122, 255, 0.02) !important;
  transform: scale(1.001);
}

:deep(.el-table__body-wrapper) {
  border-radius: var(--radius-sm);
}

:deep(.el-table--enable-row-hover .el-table__body tr:hover>td) {
  background-color: transparent !important;
}

:deep(.el-table__empty-text) {
  color: var(--apple-text-secondary);
  font-weight: 500;
}

/* ============================================
   Tags and Badges
   ============================================ */
:deep(.el-tag) {
  border-radius: var(--radius-sm);
  border: none;
  font-weight: 600;
  padding: 4px 12px;
  font-size: 12px;
}

:deep(.el-tag--warning) {
  background: rgba(255, 149, 0, 0.1);
  color: var(--apple-orange);
}

:deep(.el-tag--success) {
  background: rgba(52, 199, 89, 0.1);
  color: var(--apple-green);
}

:deep(.el-tag--danger) {
  background: rgba(255, 59, 48, 0.1);
  color: var(--apple-red);
}

:deep(.el-tag--info) {
  background: rgba(0, 0, 0, 0.05);
  color: var(--apple-text-secondary);
}

/* ============================================
   Action Buttons
   ============================================ */
:deep(.el-table .el-button) {
  border-radius: var(--radius-sm);
  font-weight: 600;
  font-size: 13px;
  margin: 0 var(--spacing-xs) 0 0;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

:deep(.el-table .el-button:last-child) {
  margin-right: 0;
}

:deep(.el-table .el-button:hover) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

:deep(.el-table .el-button--primary) {
  background: linear-gradient(135deg, var(--apple-blue) 0%, #0051d5 100%);
  border: none;
  box-shadow: 0 2px 8px rgba(0, 122, 255, 0.2);
}

:deep(.el-table .el-button--primary:hover) {
  box-shadow: 0 6px 16px rgba(0, 122, 255, 0.3);
}

:deep(.el-table .el-button--success) {
  background: linear-gradient(135deg, var(--apple-green) 0%, #28a745 100%);
  border: none;
  box-shadow: 0 2px 8px rgba(52, 199, 89, 0.2);
}

:deep(.el-table .el-button--success:hover) {
  box-shadow: 0 6px 16px rgba(52, 199, 89, 0.3);
}

:deep(.el-table .el-button.is-disabled) {
  background: rgba(0, 0, 0, 0.04);
  color: var(--apple-text-secondary);
  border: 1px solid var(--apple-border);
  transform: none;
  box-shadow: none;
}

/* ============================================
   Pagination
   ============================================ */
.pagination {
  margin-top: var(--spacing-lg);
  text-align: center;
  padding: var(--spacing-md) 0;
}

:deep(.el-pagination) {
  font-family: var(--font-apple);
  font-weight: 500;
}

:deep(.el-pagination button),
:deep(.el-pager li) {
  border-radius: var(--radius-sm);
  background: rgba(0, 0, 0, 0.02);
  border: 1px solid var(--apple-border);
  transition: all 0.2s ease;
}

:deep(.el-pagination button:hover),
:deep(.el-pager li:hover) {
  background: rgba(0, 122, 255, 0.06);
  border-color: var(--apple-blue);
  transform: translateY(-1px);
}

:deep(.el-pager li.is-active) {
  background: var(--apple-blue);
  color: white;
  border-color: var(--apple-blue);
}

/* ============================================
   Video Player Dialog
   ============================================ */
:deep(.el-dialog) {
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-card-hover);
  font-family: var(--font-apple);
}

:deep(.el-dialog__header) {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.98) 0%,
    rgba(248, 248, 248, 0.95) 100%);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--apple-border);
  padding: var(--spacing-lg);
}

:deep(.el-dialog__title) {
  font-weight: 700;
  color: var(--apple-text);
  font-size: 18px;
}

:deep(.el-dialog__body) {
  padding: var(--spacing-lg);
  background: rgba(255, 255, 255, 0.95);
}

.video-container {
  width: 100%;
}

.video-info {
  margin-bottom: var(--spacing-md);
  padding: var(--spacing-md);
  background: rgba(0, 122, 255, 0.04);
  border-radius: var(--radius-md);
  border: 1px solid rgba(0, 122, 255, 0.08);
}

.video-info p {
  margin: var(--spacing-xs) 0;
  color: var(--apple-text);
  line-height: 1.6;
}

.video-info strong {
  color: var(--apple-text);
  font-weight: 600;
}

video {
  border-radius: var(--radius-md);
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

/* ============================================
   Responsive Design
   ============================================ */
@media (max-width: 1200px) {
  .screen-recording-manage {
    padding: var(--spacing-md);
  }
}

@media (max-width: 768px) {
  .screen-recording-manage {
    padding: var(--spacing-sm);
  }

  .card-header {
    font-size: 16px;
  }

  .search-form {
    padding: var(--spacing-sm);
  }

  .search-form :deep(.el-form-item) {
    margin-bottom: var(--spacing-sm);
  }

  :deep(.el-table .el-button) {
    font-size: 12px;
    padding: 6px 12px;
  }

  :deep(.el-dialog) {
    width: 90% !important;
  }
}

@media (max-width: 576px) {
  .card-header {
    font-size: 14px;
  }

  .search-form :deep(.el-form-item) {
    display: block;
    margin-bottom: var(--spacing-sm);
  }

  .search-form :deep(.el-button) {
    width: 100%;
    margin-bottom: var(--spacing-xs);
  }

  :deep(.el-table .el-button) {
    width: 100%;
    margin-bottom: var(--spacing-xs);
  }

  .pagination {
    text-align: center;
  }

  :deep(.el-pagination) {
    justify-content: center;
  }
}
</style>
