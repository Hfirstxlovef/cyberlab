<template>
  <div class="system-settings-page">
    <div class="page-header">
      <h2>系统设置</h2>
      <p class="page-description">配置系统的基本信息和外观设置</p>
    </div>

    <el-card class="settings-card">
      <el-form ref="settingsForm" :model="settings" :rules="rules" label-width="140px" class="settings-form">
        <!-- 基础配置 -->
        <div class="settings-section">
          <h3 class="section-title">基础配置</h3>

          <!-- 系统Logo -->
          <el-form-item label="系统Logo" prop="system_logo">
            <div class="logo-upload-container">
              <el-upload class="logo-uploader" :action="uploadAction" :show-file-list="false"
                :before-upload="beforeLogoUpload" :on-success="handleLogoSuccess" :on-error="handleLogoError"
                :headers="uploadHeaders">
                <img v-if="logoUrl" :src="logoUrl" class="logo-preview" />
                <el-icon v-else class="logo-uploader-icon">
                  <Plus />
                </el-icon>
              </el-upload>
              <div class="upload-tips">
                <p>支持 JPG、PNG 格式，文件大小不超过 2MB</p>
                <p>建议尺寸：200x60 像素</p>
              </div>
            </div>
          </el-form-item>

          <!-- 网站标题（浏览器标题栏） -->
          <el-form-item label="网站标题" prop="website_title">
            <el-input v-model="settings.website_title" placeholder="请输入网站标题（浏览器标题栏显示）" maxlength="50" show-word-limit
              style="width: 400px;" />
            <div class="field-tip">此标题将显示在浏览器标题栏中</div>
          </el-form-item>

          <!-- 登录页面标题 -->
          <el-form-item label="登录页面标题" prop="login_page_title">
            <el-input v-model="settings.login_page_title" placeholder="请输入登录页面显示的标题" maxlength="50" show-word-limit
              style="width: 400px;" />
            <div class="field-tip">此标题将显示在登录窗口中</div>
          </el-form-item>

          <!-- 侧边栏标题 -->
          <el-form-item label="侧边栏标题" prop="sidebar_title">
            <el-input v-model="settings.sidebar_title" placeholder="请输入侧边栏显示的标题" maxlength="20" show-word-limit
              style="width: 400px;" />
            <div class="field-tip">此标题将显示在侧边栏Logo旁边</div>
          </el-form-item>
        </div>

        <!-- 授权信息 - Apple风格授权卡片 -->
        <div class="settings-section">
          <h3 class="section-title">授权信息</h3>

          <!-- Apple风格授权卡片 -->
          <div class="license-card-wrapper">
            <div class="apple-license-card" :class="licenseCardClass">
              <!-- 卡片头部 -->
              <div class="license-header">
                <div class="license-logo">
                  <span class="logo-icon">🔐</span>
                  <span class="logo-text">蟑螂恶霸团队</span>
                </div>
                <el-tag :type="statusColor" size="large" class="license-status-badge">
                  {{ statusText }}
                </el-tag>
              </div>

              <!-- 加载状态 -->
              <div v-if="licenseLoading" class="license-loading">
                <el-icon class="is-loading"><Loading /></el-icon>
                <span>正在验证授权...</span>
              </div>

              <!-- 授权详情 -->
              <div v-else class="license-details">
                <!-- 序列号 -->
                <div class="license-item">
                  <div class="item-label">产品序列号</div>
                  <div class="item-value serial-number">
                    {{ serialNumber || '未设置' }}
                  </div>
                </div>

                <!-- 授权码 -->
                <div class="license-item">
                  <div class="item-label">授权码</div>
                  <div class="item-value license-code">
                    {{ licenseCode || '未设置' }}
                  </div>
                </div>

                <!-- 持有者 -->
                <div class="license-item">
                  <div class="item-label">授权持有者</div>
                  <div class="item-value">{{ owner || '未设置' }}</div>
                </div>

                <!-- 过期时间 -->
                <div class="license-item">
                  <div class="item-label">有效期至</div>
                  <div class="item-value expiry-date" :class="{ 'expired': isExpired, 'warning': isNearExpiry }">
                    {{ expiryDateFormatted || '未设置' }}
                    <span v-if="!isExpired && daysRemaining > 0" class="days-remaining">
                      (剩余 {{ daysRemaining }} 天)
                    </span>
                  </div>
                </div>

                <!-- 状态消息 -->
                <div v-if="message" class="license-message" :class="'message-' + statusColor">
                  <el-icon><InfoFilled /></el-icon>
                  <span>{{ message }}</span>
                </div>

                <!-- 联系方式 -->
                <div v-if="isExpired || isNearExpiry" class="license-contact">
                  <div class="contact-info">
                    <p>如需续期或技术支持，请联系：</p>
                    <p class="contact-detail">
                      <strong>{{ supportContact }}</strong>
                    </p>
                    <p class="contact-email">{{ supportEmail }}</p>
                  </div>
                </div>
              </div>

              <!-- 卡片底部装饰 -->
              <div class="license-footer">
                <span class="footer-text">红岸网络空间安全对抗平台</span>
              </div>
            </div>
          </div>

          <!-- 编辑授权信息（管理员可编辑） -->
          <el-collapse class="license-edit-section">
            <el-collapse-item title="📝 编辑授权信息（管理员）" name="edit">
              <!-- 产品序列号 -->
              <el-form-item label="产品序列号" prop="serial_number">
                <el-input v-model="settings.serial_number" placeholder="格式：CYBERLAB-YYYY-MM-DD-NNNNN" maxlength="50" style="width: 100%;">
                  <template #prepend>SN:</template>
                </el-input>
                <div class="field-tip">格式：CYBERLAB-过期年-过期月-过期日-唯一标识符（5位数字）</div>
              </el-form-item>

              <!-- 产品授权码 -->
              <el-form-item label="产品授权码" prop="license_code">
                <el-input v-model="settings.license_code" type="textarea" :rows="2" placeholder="格式：CL-ZL3B4T34M-PRO2025-ACTIVE-ABCD1234" maxlength="200"
                  show-word-limit style="width: 100%;" />
                <div class="field-tip">格式：CL-团队标识-版本-状态-校验码</div>
              </el-form-item>

              <!-- 授权持有者 -->
              <el-form-item label="授权持有者" prop="license_owner">
                <el-input v-model="settings.license_owner" placeholder="请输入授权持有者名称" maxlength="100" style="width: 100%;" />
              </el-form-item>
            </el-collapse-item>
          </el-collapse>
        </div>

        <!-- 操作按钮 -->
        <el-form-item class="form-actions">
          <el-button type="primary" @click="saveSettings" :loading="saving">
            <el-icon>
              <Check />
            </el-icon>
            保存设置
          </el-button>
          <el-button @click="resetForm">
            <el-icon>
              <Refresh />
            </el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Check, Refresh, Loading, InfoFilled } from '@element-plus/icons-vue'
import { getSystemSettings, saveSystemSettings } from '@/api/settings'
import { getToken } from '@/utils/auth'
import { getLicenseInfo } from '@/composables/useLicense'

// 八耻八荣：使用全局授权状态，避免创建新的 useLicense 实例导致性能问题
// getLicenseInfo() 返回全局共享的授权状态，不会启动新的定时器
const {
  loading: licenseLoading,
  isValid,
  isExpired,
  isNearExpiry,
  daysRemaining,
  statusText,
  statusColor,
  message,
  serialNumber,
  licenseCode,
  expiryDateFormatted,
  owner,
  supportContact,
  supportEmail,
  fetchLicenseInfo
} = getLicenseInfo()

// 授权卡片样式类
const licenseCardClass = computed(() => {
  if (isExpired.value) return 'card-expired'
  if (isNearExpiry.value) return 'card-warning'
  if (isValid.value) return 'card-active'
  return 'card-inactive'
})

// 响应式数据
const settingsForm = ref(null)
const saving = ref(false)
const logoUrl = ref('')

// 设置数据
const settings = reactive({
  system_logo: '',
  website_title: '',
  login_page_title: '',
  sidebar_title: '',
  serial_number: '',
  license_code: '',
  license_owner: ''
})

// 表单验证规则
const rules = {
  website_title: [
    { required: true, message: '请输入网站标题', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' },
    { 
      validator: (rule, value, callback) => {
        if (value && value.trim().length === 0) {
          callback(new Error('网站标题不能为空字符'))
        } else {
          callback()
        }
      }, 
      trigger: 'blur' 
    }
  ],
  login_page_title: [
    { required: true, message: '请输入登录页面标题', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' },
    { 
      validator: (rule, value, callback) => {
        if (value && value.trim().length === 0) {
          callback(new Error('登录页面标题不能为空字符'))
        } else {
          callback()
        }
      }, 
      trigger: 'blur' 
    }
  ],
  sidebar_title: [
    { required: true, message: '请输入侧边栏标题', trigger: 'blur' },
    { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' },
    { 
      validator: (rule, value, callback) => {
        if (value && value.trim().length === 0) {
          callback(new Error('侧边栏标题不能为空字符'))
        } else {
          callback()
        }
      }, 
      trigger: 'blur' 
    }
  ],
  serial_number: [
    { required: true, message: '请输入产品序列号', trigger: 'blur' },
    { min: 6, max: 30, message: '序列号长度在 6 到 30 个字符', trigger: 'blur' },
    { 
      pattern: /^[A-Z0-9-]+$/, 
      message: '序列号只能包含大写字母、数字和短横线', 
      trigger: 'blur' 
    }
  ],
  license_code: [
    { 
      validator: (rule, value, callback) => {
        if (!value || value.trim().length === 0) {
          callback(new Error('请输入产品授权码'))
        } else if (value.trim().length < 6) {
          callback(new Error('授权码长度不能少于6个字符'))
        } else if (value.trim().length > 200) {
          callback(new Error('授权码长度不能超过200个字符'))
        } else {
          callback()
        }
      }, 
      trigger: 'blur' 
    }
  ],
  system_logo: [
    { 
      validator: (rule, value, callback) => {
        if (value && typeof value !== 'string') {
          callback(new Error('Logo路径格式错误'))
        } else if (value && value.length > 500) {
          callback(new Error('Logo路径过长'))
        } else {
          callback()
        }
      }, 
      trigger: 'blur' 
    }
  ]
}

// 上传配置 - 使用相对路径通过Vite代理，避免CORS问题
const uploadAction = computed(() => {
  return '/api/settings/upload/logo'
})

const uploadHeaders = computed(() => {
  const token = getToken()
  return token ? { Authorization: `Bearer ${token}` } : {}
})

// 授权码验证函数
const validateLicenseCode = (licenseCode) => {
  if (!licenseCode || typeof licenseCode !== 'string') {
    return { valid: false, reason: '授权码为空' }
  }
  
  const trimmedCode = licenseCode.trim()
  
  // 在开发环境下，允许默认授权码
  if (import.meta.env.DEV && trimmedCode === '开发环境默认授权码') {
    return { valid: true, reason: '开发环境默认授权码' }
  }
  
  // 检查是否为默认未设置状态
  const invalidStates = ['未设置授权码', '未授权', '', 'null', 'undefined']
  if (invalidStates.includes(trimmedCode.toLowerCase())) {
    return { valid: false, reason: '未设置有效授权码' }
  }
  
  // 基本格式验证（这里可以根据实际授权码格式调整）
  if (trimmedCode.length < 10) {
    return { valid: false, reason: '授权码长度不足' }
  }
  
  // 检查是否包含基本的授权码特征（字母数字组合）
  const hasLetters = /[A-Za-z]/.test(trimmedCode)
  const hasNumbers = /[0-9]/.test(trimmedCode)
  
  if (!hasLetters || !hasNumbers) {
    return { valid: false, reason: '授权码格式不正确' }
  }
  
  // 检查是否包含明显的测试或无效字符串
  const testPatterns = ['test', 'demo', 'sample', 'example', '123456', 'aaaaaa']
  const lowerCode = trimmedCode.toLowerCase()
  for (const pattern of testPatterns) {
    if (lowerCode.includes(pattern)) {
      return { valid: false, reason: '检测到测试授权码' }
    }
  }
  
  return { valid: true, reason: '授权码格式正确' }
}

// 授权状态
const licenseStatus = computed(() => {
  const validation = validateLicenseCode(settings.license_code)
  
  if (!validation.valid) {
    return { 
      type: 'danger', 
      text: '未授权',
      detail: validation.reason
    }
  }
  
  // 开发环境的特殊处理
  if (import.meta.env.DEV && settings.license_code === '开发环境默认授权码') {
    return { 
      type: 'warning', 
      text: '开发模式',
      detail: '当前为开发环境，使用默认授权码'
    }
  }
  
  // 这里可以添加服务器端授权码验证的调用
  // 目前基于客户端格式验证显示状态
  return { 
    type: 'success', 
    text: '格式正确',
    detail: '授权码格式验证通过，建议联系管理员确认授权状态'
  }
})

// 加载设置数据
const loadSettings = async () => {
  try {
    const response = await getSystemSettings()
    
    
    // 数据安全性验证
    if (!response) {
      throw new Error('服务器响应为空')
    }
    
    // 正确的响应数据解析 - axios拦截器已经返回了response.data
    let settingsData = null
    
    // 因为axios拦截器返回response.data，所以response就是服务器的响应
    if (response) {
      if (response.success && response.data) {
        // 标准API响应格式：{success: true, data: {...}}
        settingsData = response.data
      } else if (response.system_logo !== undefined || 
                 response.login_title !== undefined || 
                 response.sidebar_title !== undefined) {
        // 直接是设置数据的情况
        settingsData = response
      } else {
        // 尝试使用response本身
        settingsData = response
      }
    } else {
      throw new Error('响应数据为空')
    }
    
    
    // 验证settingsData不为空且是对象
    if (!settingsData || typeof settingsData !== 'object') {
      throw new Error('解析后的设置数据无效')
    }
    
    // 安全地更新设置数据，只更新已定义的字段
    const allowedFields = ['system_logo', 'website_title', 'login_page_title', 'sidebar_title', 'serial_number', 'license_code']
    allowedFields.forEach(field => {
      if (Object.prototype.hasOwnProperty.call(settingsData, field) && settingsData[field] !== undefined) {
        settings[field] = settingsData[field]
      }
    })
    
    // 处理向后兼容性：如果没有新字段，从旧字段转换
    if (!settings.website_title && settingsData.login_title) {
      settings.website_title = settingsData.login_title
    }
    if (!settings.login_page_title) {
      settings.login_page_title = '红岸网络空间安全对抗平台'
    }
    
    // 安全地设置Logo URL
    logoUrl.value = settings.system_logo ? getLogoUrl(settings.system_logo) : ''
    
    
  } catch (error) {
    
    // 识别不同类型的错误
    const isNetworkError = error.code === 'ERR_NETWORK' || error.code === 'ECONNABORTED'
    const isAuthError = error.response?.status === 401 || error.response?.status === 403
    const isNotFoundError = error.response?.status === 404
    const isServerError = error.response?.status >= 500
    
    // 只对真正的错误显示错误消息，对数据解析问题使用默认值
    if (isAuthError) {
      ElMessage.error('权限不足，无法访问系统设置')
    } else if (isNotFoundError) {
      ElMessage.error('系统设置功能暂未开放')
    } else if (isServerError) {
      ElMessage.error('服务器错误，请稍后重试')
    } else if (isNetworkError) {
      ElMessage.error('网络连接失败，请检查网络连接')
    } else {
      // 数据解析错误不显示给用户，只在控制台记录
    }
    
    // 设置默认值，避免界面显示异常
    Object.assign(settings, {
      system_logo: '',
      website_title: 'CyberLab网络空间安全攻防实验室',
      login_page_title: '红岸网络空间安全对抗平台',
      sidebar_title: 'CyberLab',
      serial_number: 'DEMO-2024-001',
      license_code: '开发环境默认授权码'
    })
    logoUrl.value = ''
  }
}

// Logo上传前验证
const beforeLogoUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('上传图片大小不能超过 2MB!')
    return false
  }
  return true
}

// Logo上传成功
const handleLogoSuccess = (response) => {
  try {
    
    // 处理不同的响应格式
    const responseData = response.data || response
    
    // 检查多种成功状态标识
    const isSuccess = responseData.success === true || 
                     responseData.code === 200 || 
                     responseData.status === 'success' ||
                     (response.status >= 200 && response.status < 300)
    
    if (isSuccess) {
      // 获取上传后的URL，支持多种字段名
      let uploadedUrl = responseData.url || 
                       responseData.file_url || 
                       responseData.path || 
                       responseData.data?.url ||
                       responseData.data?.file_url
      
      if (uploadedUrl) {
        // 构建完整的图片URL用于预览
        const fullUrl = getLogoUrl(uploadedUrl)

        // 更新预览和设置数据
        logoUrl.value = fullUrl
        settings.system_logo = uploadedUrl  // 保存相对路径到设置中

        // 触发全局事件，通知侧边栏等组件更新Logo
        window.dispatchEvent(new CustomEvent('settingsUpdated', {
          detail: { system_logo: uploadedUrl }
        }))

        // 更新localStorage缓存
        const cachedSettings = localStorage.getItem('systemSettings')
        if (cachedSettings) {
          const parsedSettings = JSON.parse(cachedSettings)
          parsedSettings.system_logo = uploadedUrl
          localStorage.setItem('systemSettings', JSON.stringify(parsedSettings))
        }

        ElMessage.success('Logo上传成功')
      } else {
        throw new Error('服务器未返回文件URL')
      }
    } else {
      const errorMsg = responseData.message || 
                      responseData.msg || 
                      responseData.error || 
                      'Logo上传失败'
      throw new Error(errorMsg)
    }
  } catch (error) {
    ElMessage.error(`Logo上传失败: ${error.message}`)
  }
}

// 获取Logo完整URL（用于显示）- 使用相对路径通过Vite代理，避免CORS问题
const getLogoUrl = (logoPath) => {
  if (!logoPath) return ''

  // 如果是完整URL，直接返回
  if (logoPath.startsWith('http')) {
    return logoPath
  }

  // 返回相对路径，通过Vite代理访问（/uploads会被代理到后端）
  return logoPath
}

// Logo上传失败
const handleLogoError = (error) => {
  ElMessage.error('Logo上传失败')
}

// 保存设置
const saveSettings = async () => {
  try {
    const valid = await settingsForm.value.validate()
    if (!valid) return

    saving.value = true
    
    // 数据清理和验证
    const settingsToSave = {
      system_logo: settings.system_logo || '',
      website_title: (settings.website_title || '').trim(),
      login_page_title: (settings.login_page_title || '').trim(),
      sidebar_title: (settings.sidebar_title || '').trim(),
      serial_number: (settings.serial_number || '').trim(),
      license_code: (settings.license_code || '').trim()
    }
    
    
    const response = await saveSystemSettings(settingsToSave)
    

    // 安全地检查响应数据
    if (!response) {
      throw new Error('服务器无响应')
    }
    
    // 处理服务器响应格式 { success: true, message: '', data: {...} }
    const responseData = response.data || response
    const isSuccess = responseData.success === true || responseData.code === 200 || response.status === 200
    
    if (isSuccess) {
      ElMessage.success(responseData.message || '设置保存成功')
      // 刷新授权信息
      await fetchLicenseInfo()
      // 触发全局事件，更新其他组件的显示
      window.dispatchEvent(new CustomEvent('settingsUpdated', {
        detail: { ...settingsToSave }
      }))
    } else {
      const errorMsg = responseData.message || responseData.msg || '保存失败'
      throw new Error(errorMsg)
    }
    
  } catch (error) {
    
    // 识别不同类型的错误
    const isNetworkError = error.code === 'ERR_NETWORK' || error.code === 'ECONNABORTED'
    const isAuthError = error.response?.status === 401 || error.response?.status === 403
    const isNotFoundError = error.response?.status === 404
    const isServerError = error.response?.status >= 500
    
    // 统一的错误处理
    if (isAuthError) {
      ElMessage.error('权限不足，无法保存系统设置')
    } else if (isNotFoundError) {
      ElMessage.error('系统设置保存功能暂未开放')
    } else if (isServerError) {
      ElMessage.error('服务器错误，保存失败，请稍后重试')
    } else if (isNetworkError) {
      ElMessage.error('网络连接失败，请检查网络连接')
    } else {
      ElMessage.error(`保存设置失败: ${error.message || '未知错误'}`)
    }
  } finally {
    saving.value = false
  }
}

// 重置表单
const resetForm = async () => {
  try {
    await ElMessageBox.confirm('确定要重置所有设置吗？', '确认重置', {
      type: 'warning'
    })
    await loadSettings()
    ElMessage.success('设置已重置')
  } catch {
    // 用户取消
  }
}

// 全局事件监听器引用
let settingsEventListener = null

// 组件挂载时加载数据
onMounted(async () => {
  loadSettings()

  // 八耻八荣：主动获取授权信息，确保首次访问时也能正确显示
  // 避免首次进入显示"未认证"，刷新后才正常的问题
  await fetchLicenseInfo()

  // 监听其他组件的设置更新事件（如果需要）
  settingsEventListener = (event) => {
  }
  window.addEventListener('settingsUpdated', settingsEventListener)
})

// 组件卸载时清理资源
onUnmounted(() => {
  // 清理全局事件监听器
  if (settingsEventListener) {
    window.removeEventListener('settingsUpdated', settingsEventListener)
    settingsEventListener = null
  }
  
  // 清理可能的定时器或其他资源
  if (saving.value) {
    saving.value = false
  }
})
</script>

<style scoped>
.system-settings-page {
  /* 移除 padding，使用 MainLayout 的统一 padding */
  max-width: 900px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0 0 8px 0;
  color: #303133;
  font-size: 24px;
  font-weight: 600;
}

.page-description {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.settings-card {
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.settings-form {
  padding: 20px;
}

.settings-section {
  margin-bottom: 40px;
}

.section-title {
  margin: 0 0 20px 0;
  padding-bottom: 10px;
  border-bottom: 2px solid #e4e7ed;
  color: #303133;
  font-size: 18px;
  font-weight: 600;
}

.logo-upload-container {
  display: flex;
  align-items: flex-start;
  gap: 20px;
}

.logo-uploader {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: border-color 0.3s;
}

.logo-uploader:hover {
  border-color: #409eff;
}

.logo-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 200px;
  height: 60px;
  text-align: center;
  line-height: 60px;
}

.logo-preview {
  width: 200px;
  height: 60px;
  object-fit: contain;
  display: block;
}

.upload-tips {
  color: #909399;
  font-size: 12px;
  line-height: 1.5;
  flex-shrink: 0;
}

.upload-tips p {
  margin: 0 0 4px 0;
}

.field-tip {
  color: #909399;
  font-size: 12px;
  line-height: 1.4;
  margin-top: 4px;
}

.license-status {
  margin-top: 8px;
}

.license-detail {
  margin: 4px 0 0 0;
  color: #909399;
  font-size: 12px;
  line-height: 1.4;
}

.form-actions {
  margin-top: 40px;
  text-align: center;
  padding-top: 20px;
  border-top: 1px solid #e4e7ed;
}

.form-actions .el-button {
  margin: 0 10px;
  min-width: 120px;
}

/* 表单项对齐优化 */
:deep(.el-form-item__label) {
  text-align: right;
  padding-right: 12px;
  font-weight: 500;
}

:deep(.el-form-item__content) {
  display: flex;
  align-items: flex-start;
}

/* ========== Apple 风格授权卡片 ========== */

.license-card-wrapper {
  margin: 20px 0;
}

.apple-license-card {
  /* Apple 毛玻璃卡片效果 */
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.9) 0%,
    rgba(248, 248, 248, 0.85) 100%);
  backdrop-filter: saturate(180%) blur(20px);
  -webkit-backdrop-filter: saturate(180%) blur(20px);
  border: 0.5px solid rgba(0, 0, 0, 0.04);
  border-radius: var(--radius-xl, 20px);
  padding: var(--spacing-lg, 32px);
  box-shadow: var(--shadow-card,
    0 2px 8px rgba(0, 0, 0, 0.04),
    0 8px 32px rgba(0, 0, 0, 0.03));
  transition: all 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  font-family: var(--font-apple, -apple-system, BlinkMacSystemFont, "SF Pro Display", sans-serif);
}

.apple-license-card:hover {
  box-shadow: var(--shadow-card-hover,
    0 8px 24px rgba(0, 0, 0, 0.08),
    0 16px 48px rgba(0, 0, 0, 0.06));
}

/* 卡片状态样式 */
.card-active {
  border-left: 4px solid var(--apple-green, #34c759);
}

.card-warning {
  border-left: 4px solid var(--apple-orange, #ff9500);
  background: linear-gradient(135deg,
    rgba(255, 249, 235, 0.9) 0%,
    rgba(255, 245, 220, 0.85) 100%);
}

.card-expired {
  border-left: 4px solid var(--apple-red, #f56565);
  background: linear-gradient(135deg,
    rgba(255, 245, 245, 0.9) 0%,
    rgba(255, 240, 240, 0.85) 100%);
  filter: grayscale(0.3);
}

.card-inactive {
  border-left: 4px solid var(--apple-text-tertiary, #86868b);
  filter: grayscale(0.5);
}

/* 卡片头部 */
.license-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-md, 24px);
  padding-bottom: var(--spacing-sm, 16px);
  border-bottom: 0.5px solid rgba(0, 0, 0, 0.06);
}

.license-logo {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-icon {
  font-size: 32px;
}

.logo-text {
  font-size: var(--font-lg, 18px);
  font-weight: var(--font-weight-semibold, 600);
  color: var(--apple-text-primary, #1d1d1f);
  letter-spacing: -0.3px;
}

.license-status-badge {
  font-size: var(--font-sm, 14px);
  font-weight: var(--font-weight-medium, 500);
  padding: 8px 16px;
  border-radius: var(--radius-md, 12px);
}

/* 加载状态 */
.license-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: var(--spacing-xl, 48px) 0;
  color: var(--apple-text-secondary, #6e6e73);
  font-size: var(--font-md, 16px);
}

/* 授权详情 */
.license-details {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md, 24px);
}

.license-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.item-label {
  font-size: var(--font-sm, 14px);
  font-weight: var(--font-weight-medium, 500);
  color: var(--apple-text-secondary, #6e6e73);
  letter-spacing: -0.2px;
}

.item-value {
  font-size: var(--font-md, 16px);
  font-weight: var(--font-weight-regular, 400);
  color: var(--apple-text-primary, #1d1d1f);
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: var(--radius-md, 12px);
  border: 0.5px solid rgba(0, 0, 0, 0.04);
  word-break: break-all;
}

.serial-number {
  font-family: 'Monaco', 'Menlo', 'Courier New', monospace;
  font-size: var(--font-sm, 14px);
  letter-spacing: 1px;
}

.license-code {
  font-family: 'Monaco', 'Menlo', 'Courier New', monospace;
  font-size: var(--font-xs, 12px);
  letter-spacing: 0.5px;
}

.expiry-date {
  display: flex;
  align-items: center;
  gap: 8px;
}

.expiry-date.warning {
  color: var(--apple-orange, #ff9500);
  font-weight: var(--font-weight-semibold, 600);
}

.expiry-date.expired {
  color: var(--apple-red, #f56565);
  font-weight: var(--font-weight-semibold, 600);
}

.days-remaining {
  font-size: var(--font-sm, 14px);
  color: var(--apple-text-tertiary, #86868b);
  font-weight: var(--font-weight-regular, 400);
}

/* 状态消息 */
.license-message {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  border-radius: var(--radius-md, 12px);
  font-size: var(--font-sm, 14px);
  line-height: 1.6;
}

.message-success {
  background: rgba(52, 199, 89, 0.1);
  color: var(--apple-green, #34c759);
  border: 0.5px solid rgba(52, 199, 89, 0.2);
}

.message-warning {
  background: rgba(255, 149, 0, 0.1);
  color: var(--apple-orange, #ff9500);
  border: 0.5px solid rgba(255, 149, 0, 0.2);
}

.message-danger {
  background: rgba(245, 101, 101, 0.1);
  color: var(--apple-red, #f56565);
  border: 0.5px solid rgba(245, 101, 101, 0.2);
}

/* 联系方式 */
.license-contact {
  margin-top: var(--spacing-sm, 16px);
  padding: var(--spacing-md, 24px);
  background: rgba(0, 113, 227, 0.05);
  border-radius: var(--radius-lg, 16px);
  border: 0.5px solid rgba(0, 113, 227, 0.1);
}

.contact-info p {
  margin: 0 0 8px 0;
  font-size: var(--font-sm, 14px);
  color: var(--apple-text-secondary, #6e6e73);
  line-height: 1.6;
}

.contact-detail {
  font-size: var(--font-lg, 18px);
  color: var(--apple-blue, #0071e3);
  font-weight: var(--font-weight-semibold, 600);
}

.contact-email {
  font-family: 'Monaco', 'Menlo', 'Courier New', monospace;
  font-size: var(--font-xs, 12px);
  color: var(--apple-text-tertiary, #86868b);
}

/* 卡片底部 */
.license-footer {
  margin-top: var(--spacing-md, 24px);
  padding-top: var(--spacing-sm, 16px);
  border-top: 0.5px solid rgba(0, 0, 0, 0.06);
  text-align: center;
}

.footer-text {
  font-size: var(--font-xs, 12px);
  color: var(--apple-text-tertiary, #86868b);
  letter-spacing: 0.5px;
}

/* 编辑区域 */
.license-edit-section {
  margin-top: var(--spacing-md, 24px);
  border: 0.5px solid rgba(0, 0, 0, 0.06);
  border-radius: var(--radius-lg, 16px);
  background: rgba(255, 255, 255, 0.5);
}

.license-edit-section :deep(.el-collapse-item__header) {
  font-weight: var(--font-weight-medium, 500);
  color: var(--apple-text-secondary, #6e6e73);
  padding: var(--spacing-sm, 16px) var(--spacing-md, 24px);
}

.license-edit-section :deep(.el-collapse-item__content) {
  padding: 0 var(--spacing-md, 24px) var(--spacing-md, 24px);
}

/* ========== Apple 风格授权卡片结束 ========== */

/* 响应式设计 */
@media (max-width: 768px) {
  .system-settings-page {
    padding: 0;
    /* 移动端也不需要额外 padding */
  }

  .settings-form {
    padding: 15px;
  }

  :deep(.el-form-item) {
    flex-direction: column;
  }

  :deep(.el-form-item__label) {
    text-align: left;
    margin-bottom: 8px;
  }

  .logo-upload-container {
    flex-direction: column;
    gap: 10px;
  }

  /* 授权卡片响应式 */
  .apple-license-card {
    padding: var(--spacing-md, 24px);
  }

  .license-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .logo-icon {
    font-size: 24px;
  }

  .logo-text {
    font-size: var(--font-md, 16px);
  }
}
</style>