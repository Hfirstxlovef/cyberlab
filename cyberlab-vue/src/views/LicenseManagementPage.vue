<template>
  <div class="license-management-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2>🎫 授权管理</h2>
      <p class="page-description">hongan 用户专属 - 生成和管理系统授权码</p>
    </div>

    <!-- 当前激活授权卡片 -->
    <el-card class="current-license-card apple-card">
      <template #header>
        <div class="card-header">
          <span class="header-icon">🟢</span>
          <span class="header-title">当前激活授权</span>
          <el-button
            type="primary"
            size="small"
            @click="refreshCurrentLicense"
            :loading="loadingCurrent"
            :icon="Refresh">
            刷新
          </el-button>
        </div>
      </template>

      <div v-if="loadingCurrent" class="loading-state">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>加载中...</span>
      </div>

      <div v-else-if="currentLicense" class="license-details">
        <div class="detail-row">
          <span class="label">序列号</span>
          <span class="value serial-number">{{ currentLicense.serialNumber }}</span>
        </div>
        <div class="detail-row">
          <span class="label">授权码</span>
          <span class="value license-code">{{ currentLicense.licenseCode }}</span>
        </div>
        <div class="detail-row">
          <span class="label">授权给</span>
          <span class="value">{{ currentLicense.issuedTo }}</span>
        </div>
        <div class="detail-row">
          <span class="label">有效期至</span>
          <span class="value" :class="{'text-danger': currentLicense.isExpired, 'text-warning': currentLicense.isNearExpiry}">
            {{ currentLicense.expiryDateFormatted }}
            <el-tag v-if="!currentLicense.isExpired" size="small" :type="currentLicense.statusColor">
              剩余 {{ currentLicense.daysRemaining }} 天
            </el-tag>
            <el-tag v-else size="small" type="danger">已过期</el-tag>
          </span>
        </div>
      </div>

      <el-empty v-else description="未设置当前授权" :image-size="80" />
    </el-card>

    <!-- 生成新授权表单 -->
    <el-card class="generate-card apple-card">
      <template #header>
        <div class="card-header">
          <span class="header-icon">✨</span>
          <span class="header-title">生成新授权</span>
        </div>
      </template>

      <el-form
        ref="generateFormRef"
        :model="generateForm"
        :rules="generateRules"
        label-width="100px"
        @submit.prevent="handleGenerate">

        <el-form-item label="授权给" prop="issuedTo">
          <el-input
            v-model="generateForm.issuedTo"
            placeholder="请输入客户名称或组织名称"
            maxlength="100"
            show-word-limit />
        </el-form-item>

        <el-form-item label="版本" prop="edition">
          <el-select v-model="generateForm.edition" placeholder="请选择版本" style="width: 100%;">
            <el-option label="🏆 专业版 (PRO)" value="PRO" />
            <el-option label="🏢 企业版 (ENTERPRISE)" value="ENTERPRISE" />
            <el-option label="🔬 试用版 (TRIAL)" value="TRIAL" />
          </el-select>
        </el-form-item>

        <el-form-item label="有效期至" prop="expiryDate">
          <el-date-picker
            v-model="generateForm.expiryDate"
            type="date"
            placeholder="选择过期日期"
            :disabled-date="disabledDate"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 100%;" />
        </el-form-item>

        <el-form-item label="备注" prop="notes">
          <el-input
            v-model="generateForm.notes"
            type="textarea"
            :rows="2"
            placeholder="可选：输入备注信息"
            maxlength="500"
            show-word-limit />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            @click="handleGenerate"
            :loading="generating"
            :icon="DocumentAdd">
            生成授权码
          </el-button>
          <el-button @click="resetGenerateForm" :icon="RefreshLeft">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 授权历史列表 -->
    <el-card class="history-card apple-card">
      <template #header>
        <div class="card-header">
          <span class="header-icon">📋</span>
          <span class="header-title">授权历史</span>
          <el-input
            v-model="searchKeyword"
            placeholder="搜索序列号、授权码、客户名称"
            :prefix-icon="Search"
            clearable
            style="width: 300px;"
            @input="handleSearch" />
        </div>
      </template>

      <el-table
        :data="licenses"
        v-loading="loadingList"
        stripe
        style="width: 100%">

        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-icon v-if="row.isCurrent" :size="24" color="#34c759"><StarFilled /></el-icon>
            <el-tag v-else :type="row.statusColor" size="small">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="serialNumber" label="序列号" min-width="200">
          <template #default="{ row }">
            <span class="serial-number-text">{{ row.serialNumber }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="editionText" label="版本" width="100" />

        <el-table-column prop="issuedTo" label="授权给" min-width="150" show-overflow-tooltip />

        <el-table-column label="有效期至" width="180">
          <template #default="{ row }">
            <span :class="{'text-danger': row.isExpired, 'text-warning': row.isNearExpiry}">
              {{ row.expiryDateFormatted }}
            </span>
            <div v-if="!row.isExpired" class="days-remaining-tag">
              剩余 {{ row.daysRemaining }} 天
            </div>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="!row.isCurrent && row.status === 'ACTIVE'"
              type="success"
              size="small"
              @click="handleSetCurrent(row)">
              设为当前
            </el-button>

            <el-button
              v-if="row.isCurrent"
              type="primary"
              size="small"
              @click="handleExtend(row)">
              延期
            </el-button>

            <el-button
              v-if="row.status === 'ACTIVE' && !row.isCurrent"
              type="warning"
              size="small"
              @click="handleDeactivate(row)">
              停用
            </el-button>

            <el-button
              v-if="row.status === 'INACTIVE'"
              type="info"
              size="small"
              @click="handleActivate(row)">
              激活
            </el-button>

            <el-button
              v-if="!row.isCurrent"
              type="danger"
              size="small"
              @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 延期对话框 -->
    <el-dialog
      v-model="extendDialogVisible"
      title="延长授权有效期"
      width="400px">

      <el-form :model="extendForm" label-width="120px">
        <el-form-item label="当前过期日期">
          <span>{{ extendingLicense?.expiryDateFormatted }}</span>
        </el-form-item>

        <el-form-item label="新过期日期" required>
          <el-date-picker
            v-model="extendForm.newExpiryDate"
            type="date"
            placeholder="选择新的过期日期"
            :disabled-date="disabledExtendDate"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 100%;" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="extendDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmExtend" :loading="extending">确定延期</el-button>
      </template>
    </el-dialog>

    <!-- 授权码生成成功对话框 -->
    <el-dialog
      v-model="generatedDialogVisible"
      title="✅ 授权码生成成功"
      width="600px"
      :close-on-click-modal="false"
      :close-on-press-escape="false">

      <el-alert
        type="success"
        :closable="false"
        show-icon
        style="margin-bottom: 20px;">
        <template #title>
          <strong>授权码已生成，请复制并分发给客户</strong>
        </template>
      </el-alert>

      <div class="generated-license-info">
        <div class="info-item">
          <div class="info-label">📋 序列号</div>
          <div class="info-value-container">
            <el-input
              v-model="generatedLicense.serialNumber"
              readonly
              class="code-input">
              <template #suffix>
                <el-button
                  link
                  type="primary"
                  @click="copyToClipboard(generatedLicense.serialNumber, '序列号')">
                  复制
                </el-button>
              </template>
            </el-input>
          </div>
        </div>

        <div class="info-item">
          <div class="info-label">🔑 授权码</div>
          <div class="info-value-container">
            <el-input
              v-model="generatedLicense.licenseCode"
              readonly
              class="code-input">
              <template #suffix>
                <el-button
                  link
                  type="primary"
                  @click="copyToClipboard(generatedLicense.licenseCode, '授权码')">
                  复制
                </el-button>
              </template>
            </el-input>
          </div>
        </div>

        <div class="info-item">
          <div class="info-label">👤 授权给</div>
          <div class="info-value">{{ generatedLicense.issuedTo }}</div>
        </div>

        <div class="info-item">
          <div class="info-label">📦 版本</div>
          <div class="info-value">{{ generatedLicense.editionText }}</div>
        </div>

        <div class="info-item">
          <div class="info-label">📅 有效期至</div>
          <div class="info-value">{{ generatedLicense.expiryDateFormatted }}</div>
        </div>
      </div>

      <el-alert
        type="warning"
        :closable="false"
        show-icon
        style="margin-top: 20px;">
        <template #title>
          <strong>重要提示</strong>
        </template>
        <div style="margin-top: 8px; line-height: 1.6;">
          1. 请将序列号和授权码一并发送给客户<br>
          2. 客户需在系统激活页面输入这两个值进行激活<br>
          3. 授权码生成后不会再次显示，请妥善保管
        </div>
      </el-alert>

      <template #footer>
        <el-button type="primary" @click="generatedDialogVisible = false">
          我已复制，关闭
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Refresh, Loading, DocumentAdd, RefreshLeft, Search, StarFilled
} from '@element-plus/icons-vue'
import {
  getCurrentLicense,
  getLicenseList,
  searchLicenses,
  generateLicense,
  setCurrentLicense,
  extendLicense,
  activateLicense,
  deactivateLicense,
  deleteLicense
} from '@/api/licenseManagement'

// 响应式数据
const currentLicense = ref(null)
const licenses = ref([])
const loadingCurrent = ref(false)
const loadingList = ref(false)
const generating = ref(false)
const extending = ref(false)
const searchKeyword = ref('')

// 生成表单
const generateFormRef = ref(null)
const generateForm = reactive({
  issuedTo: '',
  edition: 'PRO',
  expiryDate: '',
  notes: ''
})

const generateRules = {
  issuedTo: [
    { required: true, message: '请输入授权给谁', trigger: 'blur' }
  ],
  edition: [
    { required: true, message: '请选择版本', trigger: 'change' }
  ],
  expiryDate: [
    { required: true, message: '请选择过期日期', trigger: 'change' }
  ]
}

// 延期对话框
const extendDialogVisible = ref(false)
const extendingLicense = ref(null)
const extendForm = reactive({
  newExpiryDate: ''
})

// 生成成功对话框
const generatedDialogVisible = ref(false)
const generatedLicense = reactive({
  serialNumber: '',
  licenseCode: '',
  issuedTo: '',
  editionText: '',
  expiryDateFormatted: ''
})

/**
 * 加载当前授权
 */
const loadCurrentLicense = async () => {
  loadingCurrent.value = true
  try {
    const response = await getCurrentLicense()
    // Axios 响应结构: {data: {success, data}, ...}
    if (response.data && response.data.success && response.data.data) {
      currentLicense.value = response.data.data
    } else {
      currentLicense.value = null
    }
  } catch (error) {
    console.error('加载当前授权失败:', error)
    ElMessage.error('加载当前授权失败')
  } finally {
    loadingCurrent.value = false
  }
}

/**
 * 加载授权列表
 */
const loadLicenseList = async () => {
  loadingList.value = true
  try {
    const response = await getLicenseList()
    // Axios 响应结构: {data: {success, data}, ...}
    if (response.data && response.data.success) {
      licenses.value = response.data.data || []
    }
  } catch (error) {
    console.error('加载授权列表失败:', error)
    ElMessage.error('加载授权列表失败')
  } finally {
    loadingList.value = false
  }
}

/**
 * 搜索授权
 */
const handleSearch = async () => {
  if (!searchKeyword.value.trim()) {
    loadLicenseList()
    return
  }

  loadingList.value = true
  try {
    const response = await searchLicenses(searchKeyword.value)
    // Axios 响应结构: {data: {success, data}, ...}
    if (response.data && response.data.success) {
      licenses.value = response.data.data || []
    }
  } catch (error) {
    console.error('搜索失败:', error)
    ElMessage.error('搜索失败')
  } finally {
    loadingList.value = false
  }
}

/**
 * 生成新授权
 * hongan 用户生成授权码 - 无需验证，直接保存
 */
const handleGenerate = async () => {
  try {
    await generateFormRef.value.validate()

    generating.value = true
    const response = await generateLicense(generateForm)

    // Axios 响应结构: {data: {success, data, message}, status, headers, ...}
    // 我们需要的数据在 response.data.data 中
    if (response.data && response.data.success && response.data.data) {
      const licenseData = response.data.data

      // 填充生成的授权信息到对话框
      generatedLicense.serialNumber = licenseData.serialNumber
      generatedLicense.licenseCode = licenseData.licenseCode
      generatedLicense.issuedTo = licenseData.issuedTo
      generatedLicense.editionText = licenseData.editionText
      generatedLicense.expiryDateFormatted = licenseData.expiryDateFormatted

      // 显示生成成功对话框（不自动激活，由 admin 输入验证后激活）
      generatedDialogVisible.value = true

      // 重置表单并刷新列表
      resetGenerateForm()
      loadLicenseList()
    } else {
      console.error('授权生成失败 - 响应格式错误:', response.data)
      ElMessage.error(response.data?.message || '授权生成失败')
    }
  } catch (error) {
    if (error !== false) {  // 不是表单验证失败
      console.error('生成授权失败:', error)
      ElMessage.error(error.response?.data?.message || '生成授权失败')
    }
  } finally {
    generating.value = false
  }
}

/**
 * 复制到剪贴板
 */
const copyToClipboard = async (text, label) => {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success(`${label}已复制到剪贴板`)
  } catch (error) {
    console.error('复制失败:', error)
    ElMessage.error('复制失败，请手动复制')
  }
}

/**
 * 设置为当前授权
 */
const handleSetCurrent = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定将此授权设置为当前激活授权吗？\n序列号：${row.serialNumber}`,
      '确认操作',
      { type: 'warning' }
    )

    const response = await setCurrentLicense(row.id)

    if (response.data && response.data.success) {
      ElMessage.success('已设置为当前授权，系统配置已同步')
      loadCurrentLicense()
      loadLicenseList()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('设置当前授权失败:', error)
      ElMessage.error(error.response?.data?.message || '设置失败')
    }
  }
}

/**
 * 延长有效期
 */
const handleExtend = (row) => {
  extendingLicense.value = row
  extendForm.newExpiryDate = ''
  extendDialogVisible.value = true
}

const confirmExtend = async () => {
  if (!extendForm.newExpiryDate) {
    ElMessage.warning('请选择新的过期日期')
    return
  }

  extending.value = true
  try {
    const response = await extendLicense(extendingLicense.value.id, extendForm.newExpiryDate)

    if (response.data && response.data.success) {
      ElMessage.success('授权有效期延长成功')
      extendDialogVisible.value = false
      loadCurrentLicense()
      loadLicenseList()
    }
  } catch (error) {
    console.error('延长授权失败:', error)
    ElMessage.error(error.response?.data?.message || '延长失败')
  } finally {
    extending.value = false
  }
}

/**
 * 激活授权
 */
const handleActivate = async (row) => {
  try {
    await ElMessageBox.confirm(`确定激活此授权吗？`, '确认操作', { type: 'info' })

    const response = await activateLicense(row.id)

    if (response.data && response.data.success) {
      ElMessage.success('授权激活成功')
      loadLicenseList()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('激活失败:', error)
      ElMessage.error(error.response?.data?.message || '激活失败')
    }
  }
}

/**
 * 停用授权
 */
const handleDeactivate = async (row) => {
  try {
    await ElMessageBox.confirm(`确定停用此授权吗？`, '确认操作', { type: 'warning' })

    const response = await deactivateLicense(row.id)

    if (response.data && response.data.success) {
      ElMessage.success('授权停用成功')
      loadLicenseList()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('停用失败:', error)
      ElMessage.error(error.response?.data?.message || '停用失败')
    }
  }
}

/**
 * 删除授权
 */
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定删除此授权吗？此操作不可恢复！\n序列号：${row.serialNumber}`,
      '危险操作',
      { type: 'error', confirmButtonText: '确定删除' }
    )

    const response = await deleteLicense(row.id)

    if (response.data && response.data.success) {
      ElMessage.success('授权删除成功')
      loadLicenseList()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error(error.response?.data?.message || '删除失败')
    }
  }
}

/**
 * 刷新当前授权
 */
const refreshCurrentLicense = () => {
  loadCurrentLicense()
}

/**
 * 重置生成表单
 */
const resetGenerateForm = () => {
  generateFormRef.value?.resetFields()
  generateForm.issuedTo = ''
  generateForm.edition = 'PRO'
  generateForm.expiryDate = ''
  generateForm.notes = ''
}

/**
 * 日期选择限制（不能选择过去的日期）
 */
const disabledDate = (date) => {
  return date < new Date(new Date().setHours(0, 0, 0, 0))
}

/**
 * 延期日期限制（必须晚于当前过期日期）
 */
const disabledExtendDate = (date) => {
  if (!extendingLicense.value) return false
  const currentExpiry = new Date(extendingLicense.value.expiryDate)
  return date <= currentExpiry
}

// 页面加载时获取数据
onMounted(() => {
  loadCurrentLicense()
  loadLicenseList()
})
</script>

<style scoped>
.license-management-page {
  max-width: 1400px;
  margin: 0 auto;
}

/* 页面头部 */
.page-header {
  margin-bottom: var(--spacing-lg, 32px);
}

.page-header h2 {
  margin: 0 0 8px 0;
  font-size: var(--font-3xl, 32px);
  font-weight: var(--font-weight-bold, 700);
  color: var(--apple-text-primary, #1d1d1f);
}

.page-description {
  margin: 0;
  font-size: var(--font-md, 16px);
  color: var(--apple-text-secondary, #6e6e73);
}

/* Apple 风格卡片 */
.apple-card {
  margin-bottom: var(--spacing-lg, 32px);
  border-radius: var(--radius-xl, 20px);
  border: 0.5px solid rgba(0, 0, 0, 0.04);
  box-shadow: var(--shadow-card,
    0 2px 8px rgba(0, 0, 0, 0.04),
    0 8px 32px rgba(0, 0, 0, 0.03));
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}

.apple-card:hover {
  box-shadow: var(--shadow-card-hover,
    0 8px 24px rgba(0, 0, 0, 0.08),
    0 16px 48px rgba(0, 0, 0, 0.06));
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-icon {
  font-size: 24px;
}

.header-title {
  flex: 1;
  font-size: var(--font-lg, 18px);
  font-weight: var(--font-weight-semibold, 600);
  color: var(--apple-text-primary, #1d1d1f);
}

/* 当前授权卡片 */
.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: var(--spacing-xl, 48px);
  color: var(--apple-text-secondary, #6e6e73);
}

.license-details {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md, 24px);
}

.detail-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-sm, 16px);
  background: rgba(255, 255, 255, 0.6);
  border-radius: var(--radius-md, 12px);
  border: 0.5px solid rgba(0, 0, 0, 0.04);
}

.detail-row .label {
  font-size: var(--font-sm, 14px);
  font-weight: var(--font-weight-medium, 500);
  color: var(--apple-text-secondary, #6e6e73);
}

.detail-row .value {
  font-size: var(--font-md, 16px);
  font-weight: var(--font-weight-semibold, 600);
  color: var(--apple-text-primary, #1d1d1f);
}

.serial-number, .license-code {
  font-family: 'Monaco', 'Menlo', 'Courier New', monospace;
  font-size: var(--font-sm, 14px);
  letter-spacing: 0.5px;
}

.text-danger {
  color: var(--apple-red, #f56565);
}

.text-warning {
  color: var(--apple-orange, #ff9500);
}

/* 表格样式 */
.serial-number-text {
  font-family: 'Monaco', 'Menlo', 'Courier New', monospace;
  font-size: var(--font-xs, 12px);
}

.days-remaining-tag {
  font-size: var(--font-xs, 12px);
  color: var(--apple-text-tertiary, #86868b);
  margin-top: 4px;
}

/* 生成成功对话框样式 */
.generated-license-info {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-label {
  font-size: 14px;
  font-weight: 500;
  color: #6e6e73;
}

.info-value-container {
  width: 100%;
}

.code-input {
  font-family: 'Monaco', 'Menlo', 'Courier New', monospace;
  font-size: 14px;
}

.info-value {
  font-size: 16px;
  font-weight: 600;
  color: #1d1d1f;
  padding: 12px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 8px;
  border: 0.5px solid rgba(0, 0, 0, 0.08);
}

/* 响应式 */
@media (max-width: 768px) {
  .license-management-page {
    padding: var(--spacing-sm, 16px);
  }

  .detail-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}
</style>
