<template>
  <div class="system-activation-page">
    <div class="page-header">
      <h2>🔐 系统激活</h2>
      <p class="page-description">输入授权码激活 CyberLab 系统</p>
    </div>

    <el-card class="activation-card apple-card">
      <template #header>
        <div class="card-header">
          <span class="header-icon">🎫</span>
          <span class="header-title">授权码验证</span>
        </div>
      </template>

      <el-alert
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 24px;">
        <template #title>
          <strong>请输入从授权管理员处获得的序列号和授权码</strong>
        </template>
        <div style="margin-top: 8px; line-height: 1.6;">
          两个字段都必须填写，且必须与授权管理员生成的内容完全一致
        </div>
      </el-alert>

      <el-form
        ref="activationFormRef"
        :model="activationForm"
        :rules="activationRules"
        label-width="100px"
        @submit.prevent="handleActivate">

        <el-form-item label="序列号" prop="serialNumber">
          <el-input
            v-model="activationForm.serialNumber"
            placeholder="格式：CYBERLAB-YYYY-MM-DD-XXXXX"
            maxlength="50"
            clearable>
            <template #prefix>
              <span>📋</span>
            </template>
          </el-input>
          <div class="help-text">示例：CYBERLAB-2025-12-31-00001</div>
        </el-form-item>

        <el-form-item label="授权码" prop="licenseCode">
          <el-input
            v-model="activationForm.licenseCode"
            placeholder="格式：CL-ZL3B4T34M-PRO2025-ACTIVE-A7F3D2E8"
            maxlength="100"
            clearable
            show-password>
            <template #prefix>
              <span>🔑</span>
            </template>
          </el-input>
          <div class="help-text">示例：CL-ZL3B4T34M-PRO2025-ACTIVE-A7F3D2E8</div>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            @click="handleActivate"
            :loading="activating"
            :icon="Key"
            style="width: 100%;">
            {{ activating ? '验证中...' : '立即激活系统' }}
          </el-button>
        </el-form-item>
      </el-form>

      <el-divider />

      <div class="tips-section">
        <h4>💡 常见问题</h4>
        <ul>
          <li><strong>序列号格式错误？</strong> 请确认格式为 CYBERLAB-日期-编号</li>
          <li><strong>授权码验证失败？</strong> 请检查是否完整复制，区分大小写</li>
          <li><strong>授权已过期？</strong> 请联系授权管理员延长有效期</li>
          <li><strong>授权已停用？</strong> 请联系授权管理员重新激活</li>
        </ul>
      </div>
    </el-card>

    <!-- 当前授权信息卡片 -->
    <el-card class="current-license-info apple-card" v-if="currentLicense">
      <template #header>
        <div class="card-header">
          <span class="header-icon">✅</span>
          <span class="header-title">当前系统授权</span>
        </div>
      </template>

      <div class="license-details">
        <div class="detail-item">
          <span class="label">序列号</span>
          <span class="value serial-number">{{ currentLicense.serialNumber }}</span>
        </div>
        <div class="detail-item">
          <span class="label">授权给</span>
          <span class="value">{{ currentLicense.issuedTo }}</span>
        </div>
        <div class="detail-item">
          <span class="label">版本</span>
          <span class="value">{{ currentLicense.editionText }}</span>
        </div>
        <div class="detail-item">
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
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Key } from '@element-plus/icons-vue'
import { activateByCode, getCurrentLicense } from '@/api/licenseManagement'

// 响应式数据
const activating = ref(false)
const currentLicense = ref(null)

// 激活表单
const activationFormRef = ref(null)
const activationForm = reactive({
  serialNumber: '',
  licenseCode: ''
})

const activationRules = {
  serialNumber: [
    { required: true, message: '请输入序列号', trigger: 'blur' },
    { min: 10, message: '序列号格式不正确', trigger: 'blur' }
  ],
  licenseCode: [
    { required: true, message: '请输入授权码', trigger: 'blur' },
    { min: 10, message: '授权码格式不正确', trigger: 'blur' }
  ]
}

/**
 * 加载当前授权信息
 */
const loadCurrentLicense = async () => {
  try {
    const response = await getCurrentLicense()
    // Axios 响应结构: {data: {success, data}, ...}
    if (response.data && response.data.success && response.data.data) {
      currentLicense.value = response.data.data
      console.log('✅ 当前授权加载成功:', response.data.data.serialNumber)
    } else {
      currentLicense.value = null
      console.log('ℹ️ 未找到当前授权')
    }
  } catch (error) {
    console.error('❌ 加载当前授权失败:', error)
    currentLicense.value = null
  }
}

/**
 * 处理激活
 */
const handleActivate = async () => {
  try {
    // 表单验证
    await activationFormRef.value.validate()

    activating.value = true

    // 调用激活接口
    const response = await activateByCode({
      serialNumber: activationForm.serialNumber.trim(),
      licenseCode: activationForm.licenseCode.trim()
    })

    // Axios 响应结构: {data: {success, message}, ...}
    if (response.data && response.data.success) {
      ElMessage.success({
        message: '系统激活成功！授权信息已生效',
        duration: 5000,
        showClose: true
      })

      // 重置表单
      activationFormRef.value.resetFields()
      activationForm.serialNumber = ''
      activationForm.licenseCode = ''

      // 刷新当前授权信息（关键：立即显示最新授权状态）
      await loadCurrentLicense()

      // 如果激活成功，3秒后跳转到系统设置或授权管理页面（可选）
      setTimeout(() => {
        // 可以根据用户角色跳转到不同页面
        // 如果是 admin，可以跳转到系统设置
        // 如果是 license_admin，可以跳转到授权管理
        // 这里暂时不跳转，让用户查看当前授权信息
      }, 3000)
    }
  } catch (error) {
    if (error !== false) {  // 不是表单验证失败
      console.error('系统激活失败:', error)

      const errorMessage = error.response?.data?.message || '系统激活失败，请检查输入的序列号和授权码'

      ElMessage.error({
        message: errorMessage,
        duration: 5000,
        showClose: true
      })
    }
  } finally {
    activating.value = false
  }
}

// 页面加载时获取当前授权信息
onMounted(() => {
  loadCurrentLicense()
})
</script>

<style scoped>
.system-activation-page {
  max-width: 800px;
  margin: 0 auto;
}

/* 页面头部 */
.page-header {
  margin-bottom: var(--spacing-lg, 32px);
  text-align: center;
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
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04), 0 8px 32px rgba(0, 0, 0, 0.03);
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}

.apple-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08), 0 16px 48px rgba(0, 0, 0, 0.06);
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

/* 表单样式 */
.help-text {
  font-size: 12px;
  color: #86868b;
  margin-top: 4px;
}

/* 提示区域 */
.tips-section {
  padding: 16px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 12px;
  border: 0.5px solid rgba(0, 0, 0, 0.04);
}

.tips-section h4 {
  margin: 0 0 12px 0;
  font-size: 16px;
  font-weight: 600;
  color: #1d1d1f;
}

.tips-section ul {
  margin: 0;
  padding-left: 20px;
  line-height: 1.8;
}

.tips-section li {
  margin-bottom: 8px;
  color: #6e6e73;
}

.tips-section strong {
  color: #1d1d1f;
}

/* 授权详情 */
.license-details {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 12px;
  border: 0.5px solid rgba(0, 0, 0, 0.04);
}

.detail-item .label {
  font-size: 14px;
  font-weight: 500;
  color: #6e6e73;
}

.detail-item .value {
  font-size: 16px;
  font-weight: 600;
  color: #1d1d1f;
}

.serial-number {
  font-family: 'Monaco', 'Menlo', 'Courier New', monospace;
  font-size: 14px;
  letter-spacing: 0.5px;
}

.text-danger {
  color: #f56565;
}

.text-warning {
  color: #ff9500;
}

/* 响应式 */
@media (max-width: 768px) {
  .system-activation-page {
    padding: var(--spacing-sm, 16px);
  }

  .detail-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}
</style>
