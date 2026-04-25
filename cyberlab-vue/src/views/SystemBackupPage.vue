<template>
  <div class="system-backup-page">
    <div class="dashboard-header">
      <div class="header-content">
        <h2>💾 系统备份与恢复</h2>
        <p class="page-description">管理系统备份，保障数据安全</p>
      </div>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-cards">
      <el-col :xs="24" :sm="8" :md="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon">💾</div>
            <div class="stat-info">
              <div class="stat-number">{{ statistics.totalBackups }}</div>
              <div class="stat-label">备份总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8" :md="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon">✅</div>
            <div class="stat-info">
              <div class="stat-number">{{ statistics.completedBackups }}</div>
              <div class="stat-label">成功备份</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8" :md="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon">📦</div>
            <div class="stat-info">
              <div class="stat-number">{{ formatFileSize(statistics.totalSize) }}</div>
              <div class="stat-label">总占用空间</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 操作按钮 -->
    <el-card class="action-card">
      <el-space wrap>
        <el-button type="primary" :icon="Plus" @click="showCreateDialog">
          创建备份
        </el-button>
        <el-button :icon="Upload" @click="showUploadDialog">
          上传备份
        </el-button>
        <el-button type="success" :icon="Search" @click="handleScanBackups" :loading="scanning">
          扫描备份
        </el-button>
        <el-button :icon="Refresh" @click="loadBackups">
          刷新列表
        </el-button>
      </el-space>
    </el-card>

    <!-- 备份列表 -->
    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>备份记录</span>
          <el-input
            v-model="searchText"
            placeholder="搜索备份名称"
            style="width: 200px;"
            :prefix-icon="Search"
            clearable
          />
        </div>
      </template>

      <el-table
        :data="filteredBackups"
        v-loading="loading"
        style="width: 100%"
        :default-sort="{ prop: 'createdTime', order: 'descending' }"
      >
        <el-table-column prop="name" label="备份名称" min-width="150">
          <template #default="{ row }">
            <div class="backup-name">
              <el-icon><Document /></el-icon>
              <span>{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="type" label="类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getTypeTagType(row.type)">
              {{ getTypeDisplayName(row.type) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)">
              <el-icon v-if="row.status === 'pending' || row.status === 'restoring'" class="is-loading">
                <Loading />
              </el-icon>
              {{ getStatusDisplayName(row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="fileSize" label="文件大小" width="120" sortable>
          <template #default="{ row }">
            {{ formatFileSize(row.fileSize) }}
          </template>
        </el-table-column>

        <el-table-column prop="createdTime" label="创建时间" width="180" sortable>
          <template #default="{ row }">
            {{ formatDateTime(row.createdTime) }}
          </template>
        </el-table-column>

        <el-table-column prop="createdBy" label="创建者" width="120" />

        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-space>
              <el-button
                type="success"
                size="small"
                :icon="RefreshRight"
                @click="handleRestore(row)"
                :disabled="row.status !== 'completed'"
                :loading="row.status === 'restoring'"
              >
                恢复
              </el-button>
              <el-button
                type="primary"
                size="small"
                :icon="Download"
                @click="handleDownload(row)"
                :disabled="row.status !== 'completed'"
              >
                下载
              </el-button>
              <el-button
                type="danger"
                size="small"
                :icon="Delete"
                @click="handleDelete(row)"
                :disabled="row.status === 'pending' || row.status === 'restoring'"
              >
                删除
              </el-button>
            </el-space>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 创建备份对话框 -->
    <el-dialog
      v-model="createDialogVisible"
      title="创建系统备份"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="createFormRef"
        :model="createForm"
        :rules="createRules"
        label-width="100px"
      >
        <el-form-item label="备份名称" prop="name">
          <el-input
            v-model="createForm.name"
            placeholder="请输入备份名称"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="备份类型" prop="type">
          <el-radio-group v-model="createForm.type">
            <el-radio value="full">完整备份</el-radio>
            <el-radio value="database">仅数据库</el-radio>
            <el-radio value="files">仅文件</el-radio>
          </el-radio-group>
          <div class="form-tip">
            <p v-if="createForm.type === 'full'">包含数据库、上传文件和配置文件</p>
            <p v-else-if="createForm.type === 'database'">仅备份MySQL数据库</p>
            <p v-else-if="createForm.type === 'files'">仅备份上传的文件</p>
          </div>
        </el-form-item>

        <el-form-item label="备份描述" prop="description">
          <el-input
            v-model="createForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入备份描述（可选）"
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="加密保护" prop="password">
          <el-input
            v-model="createForm.password"
            type="password"
            placeholder="请设置备份密码（必需，8位以上）"
            maxlength="100"
            show-password
          />
          <div class="form-tip">
            <p style="color: #ff3b30;">🔒 备份密码为必需项（安全加固）</p>
            <p>所有备份将使用AES-256强制加密，密码长度至少8位</p>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          @click="handleCreate"
          :loading="creating"
        >
          创建备份
        </el-button>
      </template>
    </el-dialog>

    <!-- 上传备份对话框 -->
    <el-dialog
      v-model="uploadDialogVisible"
      title="上传外部备份"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="uploadFormRef"
        :model="uploadForm"
        :rules="uploadRules"
        label-width="100px"
      >
        <el-form-item label="备份名称" prop="name">
          <el-input
            v-model="uploadForm.name"
            placeholder="请输入备份名称"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="备份类型" prop="type">
          <el-radio-group v-model="uploadForm.type">
            <el-radio value="full">完整备份</el-radio>
            <el-radio value="database">仅数据库</el-radio>
            <el-radio value="files">仅文件</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="备份文件" prop="file">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            accept=".zip"
            :on-change="handleFileChange"
            :on-exceed="handleExceed"
          >
            <template #trigger>
              <el-button type="primary">选择文件</el-button>
            </template>
            <template #tip>
              <div class="el-upload__tip">
                只支持上传ZIP格式的备份文件，大小不超过2GB
              </div>
            </template>
          </el-upload>
        </el-form-item>

        <el-form-item label="备份描述" prop="description">
          <el-input
            v-model="uploadForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入备份描述（可选）"
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          @click="handleUpload"
          :loading="uploading"
        >
          上传备份
        </el-button>
      </template>
    </el-dialog>

    <!-- 恢复备份对话框（合并确认和密码输入） -->
    <el-dialog
      v-model="restoreDialogVisible"
      title="恢复系统备份"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-alert
        title="警告：恢复备份将覆盖当前系统数据"
        type="warning"
        :closable="false"
        style="margin-bottom: 20px;"
      >
        <template #default>
          <p style="margin: 8px 0 0 0;">
            恢复操作将替换当前数据库和文件，此操作不可撤销。<br />
            备份名称：<strong>{{ selectedBackup?.name }}</strong><br />
            备份类型：<strong>{{ selectedBackup?.type ? getTypeDisplayName(selectedBackup.type) : '' }}</strong><br />
            创建时间：<strong>{{ selectedBackup?.createdTime ? formatDateTime(selectedBackup.createdTime) : '' }}</strong>
          </p>
        </template>
      </el-alert>

      <el-form
        ref="restoreFormRef"
        :model="restoreForm"
        :rules="restoreRules"
        label-width="100px"
      >
        <el-form-item label="解密密码" prop="password">
          <el-input
            v-model="restoreForm.password"
            type="password"
            placeholder="请输入备份密码（8位以上）"
            maxlength="100"
            show-password
            autofocus
          />
          <div class="form-tip">
            <p style="color: #ff3b30;">🔒 所有备份均已加密，请输入创建时设置的密码</p>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="restoreDialogVisible = false">取消</el-button>
        <el-button
          type="danger"
          @click="confirmRestore"
          :loading="restoring"
        >
          确认恢复
        </el-button>
      </template>
    </el-dialog>

    <!-- 下载备份对话框（添加密码验证） -->
    <el-dialog
      v-model="downloadDialogVisible"
      title="下载加密备份"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-alert
        title="安全验证：需要输入备份密码"
        type="info"
        :closable="false"
        style="margin-bottom: 20px;"
      >
        <template #default>
          <p style="margin: 8px 0 0 0;">
            为保护备份文件安全，下载前需验证密码。<br />
            备份名称：<strong>{{ selectedBackup?.name }}</strong><br />
            文件大小：<strong>{{ selectedBackup?.fileSize ? formatFileSize(selectedBackup.fileSize) : '' }}</strong>
          </p>
        </template>
      </el-alert>

      <el-form
        ref="downloadFormRef"
        :model="downloadForm"
        :rules="downloadRules"
        label-width="100px"
      >
        <el-form-item label="备份密码" prop="password">
          <el-input
            v-model="downloadForm.password"
            type="password"
            placeholder="请输入备份密码（8位以上）"
            maxlength="100"
            show-password
            autofocus
          />
          <div class="form-tip">
            <p style="color: #007aff;">🔐 验证通过后将开始下载备份文件</p>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="downloadDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          @click="confirmDownload"
          :loading="downloading"
        >
          验证并下载
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus,
  Upload,
  Refresh,
  Search,
  Document,
  Download,
  Delete,
  RefreshRight,
  Loading
} from '@element-plus/icons-vue'
import {
  getBackupList,
  createBackup,
  restoreBackup,
  deleteBackup,
  downloadBackup,
  uploadBackup,
  getBackupStatistics,
  scanBackups
} from '@/api/backup'
import { getUsername } from '@/utils/auth'

// 响应式数据
const loading = ref(false)
const searching = ref(false)
const scanning = ref(false)
const searchText = ref('')
const backups = ref([])
const statistics = reactive({
  totalBackups: 0,
  completedBackups: 0,
  totalSize: 0
})

// 创建备份对话框
const createDialogVisible = ref(false)
const creating = ref(false)
const createFormRef = ref(null)
const createForm = reactive({
  name: '',
  type: 'full',
  description: '',
  password: ''
})

const createRules = {
  name: [
    { required: true, message: '请输入备份名称', trigger: 'blur' },
    { min: 2, max: 200, message: '长度在 2 到 200 个字符', trigger: 'blur' }
  ],
  type: [
    { required: true, message: '请选择备份类型', trigger: 'change' }
  ],
  // ✅ 修复：添加密码必需验证（安全加固）
  password: [
    { required: true, message: '请设置备份密码', trigger: 'blur' },
    { min: 8, message: '密码长度至少8位', trigger: 'blur' }
  ]
}

// 上传备份对话框
const uploadDialogVisible = ref(false)
const uploading = ref(false)
const uploadFormRef = ref(null)
const uploadRef = ref(null)
const uploadForm = reactive({
  name: '',
  type: 'full',
  description: '',
  file: null
})

const uploadRules = {
  name: [
    { required: true, message: '请输入备份名称', trigger: 'blur' },
    { min: 2, max: 200, message: '长度在 2 到 200 个字符', trigger: 'blur' }
  ],
  type: [
    { required: true, message: '请选择备份类型', trigger: 'change' }
  ],
  file: [
    { required: true, message: '请选择备份文件', trigger: 'change' }
  ]
}

// ✅ 修复：恢复备份对话框（合并确认和密码输入）
const restoreDialogVisible = ref(false)
const restoring = ref(false)
const restoreFormRef = ref(null)
const selectedBackup = ref(null)
const restoreForm = reactive({
  password: ''
})

const restoreRules = {
  password: [
    { required: true, message: '请输入备份密码', trigger: 'blur' },
    { min: 8, message: '密码长度至少8位', trigger: 'blur' }
  ]
}

// ✅ 修复：下载备份对话框（添加密码输入）
const downloadDialogVisible = ref(false)
const downloading = ref(false)
const downloadFormRef = ref(null)
const downloadForm = reactive({
  password: ''
})

const downloadRules = {
  password: [
    { required: true, message: '请输入备份密码', trigger: 'blur' },
    { min: 8, message: '密码长度至少8位', trigger: 'blur' }
  ]
}

// 过滤后的备份列表
const filteredBackups = computed(() => {
  if (!searchText.value) {
    return backups.value
  }
  return backups.value.filter(backup =>
    backup.name.toLowerCase().includes(searchText.value.toLowerCase())
  )
})

// 加载备份列表
const loadBackups = async () => {
  loading.value = true
  try {
    const response = await getBackupList()
    if (response.success) {
      backups.value = response.data || []
    } else {
      ElMessage.error(response.message || '加载备份列表失败')
    }
  } catch (error) {
    console.error('加载备份列表失败:', error)
    ElMessage.error('加载备份列表失败: ' + error.message)
  } finally {
    loading.value = false
  }
}

// 加载统计信息
const loadStatistics = async () => {
  try {
    const response = await getBackupStatistics()
    if (response.success) {
      Object.assign(statistics, response.data)
    }
  } catch (error) {
    console.error('加载统计信息失败:', error)
  }
}

// 显示创建对话框
const showCreateDialog = () => {
  // 生成默认备份名称
  const now = new Date()
  const dateStr = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}`
  const timeStr = `${String(now.getHours()).padStart(2, '0')}${String(now.getMinutes()).padStart(2, '0')}`
  createForm.name = `系统备份_${dateStr}_${timeStr}`
  createForm.type = 'full'
  createForm.description = ''
  createForm.password = ''
  createDialogVisible.value = true
}

// 创建备份
const handleCreate = async () => {
  try {
    const valid = await createFormRef.value.validate()
    if (!valid) return

    creating.value = true

    const username = getUsername()
    const requestData = {
      name: createForm.name,
      type: createForm.type,
      description: createForm.description,
      createdBy: username || 'admin',
      password: createForm.password
    }

    const response = await createBackup(requestData)

    if (response.success) {
      const message = createForm.password ? '加密备份任务已启动，正在后台执行' : '备份任务已启动，正在后台执行'
      ElMessage.success(message)
      createDialogVisible.value = false
      await loadBackups()
      await loadStatistics()
      // 启动轮询检查备份状态
      startPolling()
    } else {
      ElMessage.error(response.message || '创建备份失败')
    }
  } catch (error) {
    console.error('创建备份失败:', error)
    let errorMsg = '创建备份失败: ' + error.message
    if (error.response?.data?.message) {
      errorMsg = '创建备份失败: ' + error.response.data.message
    } else if (error.response?.status) {
      errorMsg = `创建备份失败: HTTP ${error.response.status}`
    }
    ElMessage.error(errorMsg)
  } finally {
    creating.value = false
  }
}

// 显示上传对话框
const showUploadDialog = () => {
  uploadForm.name = ''
  uploadForm.type = 'full'
  uploadForm.description = ''
  uploadForm.file = null
  uploadDialogVisible.value = true
}

// 文件选择变化
const handleFileChange = (file) => {
  uploadForm.file = file.raw
}

// 文件超出限制
const handleExceed = () => {
  ElMessage.warning('最多只能上传1个文件')
}

// 上传备份
const handleUpload = async () => {
  try {
    const valid = await uploadFormRef.value.validate()
    if (!valid) return

    if (!uploadForm.file) {
      ElMessage.error('请选择备份文件')
      return
    }

    uploading.value = true

    const username = getUsername()
    const formData = new FormData()
    formData.append('file', uploadForm.file)
    formData.append('name', uploadForm.name)
    formData.append('type', uploadForm.type)
    formData.append('description', uploadForm.description || '')
    formData.append('createdBy', username || 'admin')

    const response = await uploadBackup(formData)

    if (response.success) {
      ElMessage.success('备份文件上传成功')
      uploadDialogVisible.value = false
      await loadBackups()
      await loadStatistics()
    } else {
      ElMessage.error(response.message || '上传备份失败')
    }
  } catch (error) {
    console.error('上传备份失败:', error)
    ElMessage.error('上传备份失败: ' + error.message)
  } finally {
    uploading.value = false
  }
}

// 恢复备份
// ✅ 修复：简化恢复逻辑，打开统一对话框
const handleRestore = (backup) => {
  selectedBackup.value = backup
  restoreForm.password = ''
  restoreDialogVisible.value = true
}

// 确认恢复备份
const confirmRestore = async () => {
  try {
    const valid = await restoreFormRef.value.validate()
    if (!valid) return

    restoring.value = true
    const response = await restoreBackup(selectedBackup.value.id, restoreForm.password)

    if (response.success) {
      ElMessage.success('恢复任务已启动，正在后台执行')
      restoreDialogVisible.value = false
      await loadBackups()
      // 启动轮询检查恢复状态
      startPolling()
    } else {
      if (response.requiresPassword) {
        ElMessage.error('该备份已加密，需要提供正确的密码')
      } else {
        ElMessage.error(response.message || '恢复备份失败')
      }
    }
  } catch (error) {
    console.error('恢复备份失败:', error)
    ElMessage.error('恢复备份失败: ' + (error.message || error))
  } finally {
    restoring.value = false
  }
}

// ✅ 修复：下载备份改为需要密码验证（安全加固）
const handleDownload = (backup) => {
  selectedBackup.value = backup
  downloadForm.password = ''
  downloadDialogVisible.value = true
}

// 确认下载备份
const confirmDownload = async () => {
  try {
    const valid = await downloadFormRef.value.validate()
    if (!valid) return

    downloading.value = true
    ElMessage.info('正在下载备份文件，请稍候...')

    // 使用axios下载，自动携带认证token
    const response = await downloadBackup(selectedBackup.value.id, downloadForm.password)

    // response.data 本身就是 Blob 对象（axios拦截器返回完整response）
    const blob = response.data
    const url = window.URL.createObjectURL(blob)

    // 创建临时下载链接并触发下载
    const link = document.createElement('a')
    link.href = url
    link.download = selectedBackup.value.filePath
    document.body.appendChild(link)
    link.click()

    // 清理
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)

    ElMessage.success('备份文件下载成功')
    downloadDialogVisible.value = false
  } catch (error) {
    console.error('下载备份失败:', error)
    ElMessage.error('下载备份失败: ' + (error.response?.data?.message || error.message))
  } finally {
    downloading.value = false
  }
}

// 删除备份
const handleDelete = async (backup) => {
  try {
    await ElMessageBox.confirm(
      `
        <div class="apple-delete-confirm">
          <div class="delete-icon">🗑️</div>
          <div class="delete-title">确认删除备份</div>
          <div class="delete-info">
            <div class="backup-name-info">
              <strong>${backup.name}</strong>
            </div>
            <div class="warning-text">此操作将永久删除备份文件，无法恢复</div>
          </div>
        </div>
      `,
      '',
      {
        dangerouslyUseHTMLString: true,
        customClass: 'apple-message-box',
        confirmButtonText: '删除',
        confirmButtonClass: 'apple-delete-button',
        cancelButtonText: '取消',
        cancelButtonClass: 'apple-cancel-button',
        showClose: false,
        center: true
      }
    )

    const response = await deleteBackup(backup.id)

    if (response.success) {
      ElMessage.success('备份已删除')
      await loadBackups()
      await loadStatistics()
    } else {
      ElMessage.error(response.message || '删除备份失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除备份失败:', error)
      ElMessage.error('删除备份失败: ' + error.message)
    }
  }
}

// 扫描物理备份文件
// ✅ 修复：用于恢复数据库后，重新导入物理文件的备份记录
const handleScanBackups = async () => {
  try {
    await ElMessageBox.confirm(
      '此操作将扫描 backups 目录下的所有备份文件，并导入到数据库中。\n已存在的备份记录不会被重复导入。',
      '扫描物理备份文件',
      {
        type: 'info',
        confirmButtonText: '开始扫描',
        cancelButtonText: '取消'
      }
    )

    scanning.value = true
    ElMessage.info('正在扫描备份文件，请稍候...')

    const response = await scanBackups()

    if (response.success) {
      const importedCount = response.data?.importedCount || 0
      if (importedCount > 0) {
        ElMessage.success(`扫描完成，成功导入 ${importedCount} 个备份文件`)
      } else {
        ElMessage.info('扫描完成，没有发现新的备份文件')
      }
      await loadBackups()
      await loadStatistics()
    } else {
      ElMessage.error(response.message || '扫描备份文件失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('扫描备份文件失败:', error)
      ElMessage.error('扫描备份文件失败: ' + error.message)
    }
  } finally {
    scanning.value = false
  }
}

// 获取类型显示名称
const getTypeDisplayName = (type) => {
  const typeMap = {
    full: '完整备份',
    database: '仅数据库',
    files: '仅文件'
  }
  return typeMap[type] || type
}

// 获取类型标签类型
const getTypeTagType = (type) => {
  const typeMap = {
    full: 'primary',
    database: 'success',
    files: 'warning'
  }
  return typeMap[type] || 'info'
}

// 获取状态显示名称
const getStatusDisplayName = (status) => {
  const statusMap = {
    pending: '创建中',
    completed: '已完成',
    failed: '失败',
    restoring: '恢复中'
  }
  return statusMap[status] || status
}

// 获取状态标签类型
const getStatusTagType = (status) => {
  const statusMap = {
    pending: 'warning',
    completed: 'success',
    failed: 'danger',
    restoring: 'info'
  }
  return statusMap[status] || 'info'
}

// 格式化文件大小
const formatFileSize = (bytes) => {
  if (!bytes || bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
}

// 格式化日期时间
const formatDateTime = (dateTime) => {
  if (!dateTime) return '-'
  const date = new Date(dateTime)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  const second = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}:${second}`
}

// 轮询定时器
let pollingTimer = null

// 启动轮询
const startPolling = () => {
  if (pollingTimer) {
    clearInterval(pollingTimer)
  }
  pollingTimer = setInterval(async () => {
    await loadBackups()
    // 如果没有正在进行的任务，停止轮询
    const hasRunning = backups.value.some(
      b => b.status === 'pending' || b.status === 'restoring'
    )
    if (!hasRunning) {
      stopPolling()
      await loadStatistics()
    }
  }, 3000) // 每3秒检查一次
}

// 停止轮询
const stopPolling = () => {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
  }
}

// 组件挂载
onMounted(async () => {
  await loadBackups()
  await loadStatistics()

  // 如果有正在进行的任务，启动轮询
  const hasRunning = backups.value.some(
    b => b.status === 'pending' || b.status === 'restoring'
  )
  if (hasRunning) {
    startPolling()
  }
})

// 组件卸载
onUnmounted(() => {
  stopPolling()
})
</script>

<style scoped>
/* ========== Apple 风格系统备份页面 ========== */

.system-backup-page {
  padding: var(--spacing-lg, 32px);
  background: var(--apple-white, #fbfbfd);
  min-height: 100vh;
  max-width: 1400px;
  margin: 0 auto;
  font-family: var(--font-apple, -apple-system, BlinkMacSystemFont, "SF Pro Display", sans-serif);
}

/* 页面标题区域 */
.dashboard-header {
  margin-bottom: var(--spacing-2xl, 64px);
  position: relative;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  min-height: 80px;
}

.header-content {
  text-align: center;
  position: relative;
  top: 0;
}

.dashboard-header h2 {
  color: var(--apple-text-primary, #1d1d1f);
  margin: 0 0 var(--spacing-sm, 16px) 0;
  font-size: var(--font-3xl, 48px);
  font-weight: var(--font-weight-semibold, 600);
  letter-spacing: var(--letter-spacing-tight, -0.5px);
  line-height: 1.1;
}

.page-description {
  margin: 0;
  color: var(--apple-text-secondary, #6e6e73);
  font-size: var(--font-lg, 18px);
  font-weight: var(--font-weight-regular, 400);
}

/* 统计卡片区域 */
.stats-cards {
  margin-bottom: var(--spacing-2xl, 64px);
}

.stat-card {
  /* Apple 产品卡片风格 */
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.9) 0%,
    rgba(248, 248, 248, 0.8) 100%);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 0.5px solid rgba(0, 0, 0, 0.04);
  border-radius: var(--radius-xl, 20px);
  box-shadow: var(--shadow-card, 0 4px 16px rgba(0, 0, 0, 0.06));
  transition: all var(--duration-slow, 0.4s) var(--ease-out, cubic-bezier(0.19, 1, 0.22, 1));
  cursor: pointer;
}

.stat-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--shadow-card-hover, 0 8px 32px rgba(0, 0, 0, 0.12));
}

.stat-content {
  display: flex;
  align-items: center;
  padding: var(--spacing-md, 24px);
}

.stat-icon {
  font-size: 48px;
  margin-right: var(--spacing-md, 24px);
}

.stat-info {
  flex: 1;
}

.stat-number {
  font-size: var(--font-2xl, 32px);
  font-weight: var(--font-weight-semibold, 600);
  color: var(--apple-text-primary, #1d1d1f);
  letter-spacing: -0.5px;
}

.stat-label {
  color: var(--apple-text-secondary, #6e6e73);
  font-size: var(--font-md, 16px);
  font-weight: var(--font-weight-regular, 400);
  margin-top: 4px;
}

/* 操作按钮卡片 */
.action-card {
  margin-bottom: var(--spacing-2xl, 64px);
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 0.5px solid rgba(0, 0, 0, 0.04);
  border-radius: var(--radius-xl, 20px);
  box-shadow: var(--shadow-card, 0 4px 16px rgba(0, 0, 0, 0.06));
}

/* 表格卡片 */
.table-card {
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 0.5px solid rgba(0, 0, 0, 0.04);
  border-radius: var(--radius-xl, 20px);
  box-shadow: var(--shadow-card, 0 4px 16px rgba(0, 0, 0, 0.06));
  transition: all var(--duration-slow, 0.4s) var(--ease-out, cubic-bezier(0.19, 1, 0.22, 1));
  margin-bottom: var(--spacing-2xl, 64px);
}

.table-card:hover {
  box-shadow: var(--shadow-lg, 0 12px 40px rgba(0, 0, 0, 0.15));
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.backup-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.backup-name .el-icon {
  color: #007aff;
}

/* Element Plus 卡片覆盖 */
:deep(.el-card) {
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 0.5px solid rgba(0, 0, 0, 0.04);
  border-radius: var(--radius-xl, 20px);
  box-shadow: var(--shadow-card, 0 4px 16px rgba(0, 0, 0, 0.06));
}

:deep(.el-card__header) {
  font-size: var(--font-lg, 18px);
  font-weight: var(--font-weight-semibold, 600);
  color: var(--apple-text-primary, #1d1d1f);
  border-bottom: 0.5px solid rgba(0, 0, 0, 0.06);
  padding: var(--spacing-md, 24px);
}

:deep(.el-card__body) {
  padding: var(--spacing-md, 24px);
}

/* 按钮优化 */
:deep(.el-button) {
  border-radius: 12px;
  font-weight: 500;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

:deep(.el-button:hover) {
  transform: translateY(-2px);
}

/* 对话框优化 */
:deep(.el-dialog) {
  border-radius: var(--radius-xl, 20px);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.15);
}

:deep(.el-dialog__header) {
  font-size: var(--font-lg, 18px);
  font-weight: var(--font-weight-semibold, 600);
  color: var(--apple-text-primary, #1d1d1f);
  padding: var(--spacing-md, 24px);
  border-bottom: 0.5px solid rgba(0, 0, 0, 0.06);
}

/* 表单优化 */
.form-tip {
  margin-top: 8px;
  font-size: 12px;
  color: var(--apple-text-secondary, #6e6e73);
  line-height: 1.5;
}

.form-tip p {
  margin: 0;
}

/* 上传组件提示 */
.el-upload__tip {
  margin-top: 8px;
  font-size: 12px;
  color: var(--apple-text-secondary, #6e6e73);
  line-height: 1.5;
}

/* 加载图标旋转动画 */
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

/* 响应式设计 */
@media (max-width: 768px) {
  .system-backup-page {
    padding: 16px;
  }

  .dashboard-header h2 {
    font-size: 32px;
  }

  .page-description {
    font-size: 14px;
  }

  .stats-cards {
    margin-bottom: 32px;
  }

  .stat-card {
    margin-bottom: 10px;
  }

  .stat-number {
    font-size: 24px;
  }

  .stat-label {
    font-size: 14px;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .card-header .el-input {
    width: 100% !important;
  }

  :deep(.el-table) {
    font-size: 12px;
  }

  :deep(.el-button--small) {
    padding: 5px 8px;
    font-size: 12px;
  }
}

/* ========== Apple 风格优化结束 ========== */
</style>

<style>
/* ========== Apple 风格删除确认框（全局样式，MessageBox 挂载在 body）========== */

.apple-message-box {
  background: rgba(255, 255, 255, 0.98) !important;
  backdrop-filter: blur(40px) !important;
  -webkit-backdrop-filter: blur(40px) !important;
  border: 0.5px solid rgba(0, 0, 0, 0.06) !important;
  border-radius: 24px !important;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15),
              0 8px 24px rgba(0, 0, 0, 0.08) !important;
  padding: 0 !important;
  min-width: 420px !important;
  overflow: hidden;
}

.apple-message-box .el-message-box__header {
  display: none !important;
}

.apple-message-box .el-message-box__content {
  padding: 40px 32px 24px !important;
  color: #1d1d1f;
}

.apple-message-box .el-message-box__message {
  margin: 0 !important;
  padding: 0 !important;
}

.apple-message-box .el-message-box__btns {
  padding: 0 32px 32px !important;
  display: flex;
  gap: 12px;
  justify-content: center;
}

/* 删除确认框内容 */
.apple-delete-confirm {
  text-align: center !important;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.apple-delete-confirm .delete-icon {
  font-size: 64px;
  margin-bottom: 20px;
  animation: deleteIconPulse 2s ease-in-out infinite;
  line-height: 1;
}

@keyframes deleteIconPulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.1);
  }
}

.apple-delete-confirm .delete-title {
  font-size: 22px;
  font-weight: 600;
  color: #1d1d1f;
  margin-bottom: 20px;
  letter-spacing: -0.3px;
  text-align: center !important;
  width: 100%;
}

.apple-delete-confirm .delete-info {
  margin-top: 0;
  text-align: center !important;
  width: 100%;
}

.apple-delete-confirm .backup-name-info {
  font-size: 15px;
  color: #1d1d1f;
  margin: 0 0 16px 0 !important;
  line-height: 1.6;
  text-align: center !important;
  padding: 0 !important;
}

.apple-delete-confirm .backup-name-info strong {
  font-weight: 600;
  color: #ff3b30;
  display: block;
  margin-top: 4px;
}

.apple-delete-confirm .warning-text {
  font-size: 14px;
  color: #86868b;
  margin: 0 !important;
  line-height: 1.6;
  text-align: center !important;
  padding: 0 !important;
  max-width: 340px;
}

/* Apple 风格按钮 */
.apple-delete-button {
  flex: 1;
  height: 44px !important;
  background: linear-gradient(180deg, #ff3b30 0%, #e8342a 100%) !important;
  border: none !important;
  border-radius: 12px !important;
  font-size: 15px !important;
  font-weight: 600 !important;
  color: #ffffff !important;
  letter-spacing: 0.2px;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1) !important;
  box-shadow: 0 2px 8px rgba(255, 59, 48, 0.3) !important;
}

.apple-delete-button:hover {
  transform: translateY(-2px) !important;
  box-shadow: 0 4px 16px rgba(255, 59, 48, 0.4) !important;
  background: linear-gradient(180deg, #ff4d42 0%, #f03d33 100%) !important;
}

.apple-delete-button:active {
  transform: translateY(0) !important;
  box-shadow: 0 1px 4px rgba(255, 59, 48, 0.3) !important;
}

.apple-cancel-button {
  flex: 1;
  height: 44px !important;
  background: rgba(0, 0, 0, 0.04) !important;
  border: 0.5px solid rgba(0, 0, 0, 0.08) !important;
  border-radius: 12px !important;
  font-size: 15px !important;
  font-weight: 600 !important;
  color: #1d1d1f !important;
  letter-spacing: 0.2px;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1) !important;
}

.apple-cancel-button:hover {
  background: rgba(0, 0, 0, 0.08) !important;
  transform: translateY(-2px) !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08) !important;
}

.apple-cancel-button:active {
  transform: translateY(0) !important;
  background: rgba(0, 0, 0, 0.12) !important;
}

/* 响应式优化 */
@media (max-width: 768px) {
  .apple-message-box {
    min-width: 320px !important;
    margin: 0 16px !important;
  }

  .apple-message-box .el-message-box__content {
    padding: 32px 24px 20px !important;
  }

  .apple-message-box .el-message-box__btns {
    padding: 0 24px 24px !important;
    flex-direction: column;
  }

  .apple-delete-confirm .delete-icon {
    font-size: 52px;
  }

  .apple-delete-confirm .delete-title {
    font-size: 20px;
  }

  .apple-delete-button,
  .apple-cancel-button {
    width: 100% !important;
  }
}

/* ========== Apple 风格删除确认框结束 ========== */
</style>
