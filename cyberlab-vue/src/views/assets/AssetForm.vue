<template>
  <el-dialog
    :title="form.id ? '✏️ 编辑资产' : '➕ 新增资产'"
    v-model="visible"
    width="650px"
    :close-on-click-modal="false"
    :lock-scroll="false"
    class="apple-asset-dialog">
    <el-form :model="form" label-width="120px" :rules="rules" ref="formRef">
      <el-form-item label="📝 名称" prop="name">
        <el-input v-model="form.name" placeholder="请输入资产名称" />
      </el-form-item>

      <el-form-item label="🌐 IP / 域名" prop="ip">
        <el-input v-model="form.ip" placeholder="请输入 IP 或域名" />
      </el-form-item>

      <el-form-item label="🏢 企业" prop="company">
        <el-input v-model="form.company" placeholder="请输入所属企业" />
      </el-form-item>

      <el-form-item label="👤 负责人" prop="owner">
        <el-input v-model="form.owner" placeholder="请输入负责人" />
      </el-form-item>

      <el-form-item label="📁 项目功能区" prop="project">
        <el-input v-model="form.project" placeholder="请输入项目功能区名称" />
      </el-form-item>

      <el-form-item label="👁️ 可见性" prop="visibility">
        <el-select v-model="form.visibility" placeholder="请选择">
          <el-option label="Red Team" value="red" />
          <el-option label="Blue Team" value="blue" />
          <el-option label="Both" value="both" />
        </el-select>
      </el-form-item>

      <el-form-item label="🎯 是否靶场">
        <el-switch v-model="form.isTarget" />
      </el-form-item>

      <el-form-item label="⚡ 是否启用">
        <el-switch v-model="form.enabled" />
      </el-form-item>

      <!-- 资产类型选择 -->
      <el-form-item label="📦 资产类型" prop="assetType">
        <el-select v-model="form.assetType" placeholder="请选择资产类型">
          <el-option label="服务器" value="server" />
          <el-option label="容器" value="container" />
          <el-option label="服务" value="service" />
          <el-option label="网络设备" value="network" />
        </el-select>
      </el-form-item>

      <!-- 平台配置 - 简化版 - 只在资产类型为容器时显示 -->
      <template v-if="form.assetType === 'container'">
        <el-divider content-position="left">🐳 容器探测配置</el-divider>

      <el-form-item label="🔧 平台类型" prop="assetPlatform">
        <el-select v-model="form.assetPlatform" placeholder="选择平台类型">
          <el-option label="Docker" value="docker">
            <span>🐳 Docker</span>
          </el-option>
          <el-option label="Kubernetes" value="k8s">
            <span>☸️ Kubernetes</span>
          </el-option>
          <el-option label="Docker & Kubernetes" value="both">
            <span>🐳 ☸️ Docker & Kubernetes</span>
          </el-option>
        </el-select>
      </el-form-item>

      <!-- Docker 配置 -->
      <div v-if="form.assetPlatform === 'docker' || form.assetPlatform === 'both'" class="platform-config">
        <el-form-item label="🐳 Docker端口">
          <el-input-number
            v-model="form.dockerPort"
            :min="1"
            :max="65535"
            placeholder="默认2375"
            style="width: 100%;"
          />
          <div class="form-tip">默认Docker API端口为2375，如需修改请输入实际端口</div>
        </el-form-item>

        <el-form-item label="🔍 启用探测">
          <el-switch v-model="form.dockerApiEnabled" />
          <span class="form-tip" style="margin-left: 10px;">开启后将自动探测该资产的Docker容器</span>
        </el-form-item>
      </div>

      <!-- Kubernetes 配置 -->
      <div v-if="form.assetPlatform === 'k8s' || form.assetPlatform === 'both'" class="platform-config">
        <el-form-item label="☸️ K8s API" prop="k8sApiServer">
          <el-input
            v-model="form.k8sApiServer"
            placeholder="如：https://192.168.1.100:6443"
          />
          <div class="form-tip">Kubernetes API Server完整地址</div>
        </el-form-item>

        <el-form-item label="🔑 访问令牌" prop="k8sToken">
          <el-input
            v-model="form.k8sToken"
            type="password"
            show-password
            placeholder="请输入K8s访问令牌"
          />
          <div class="form-tip">用于访问Kubernetes API的Bearer Token</div>
        </el-form-item>
      </div>
      </template>

      <el-form-item label="📋 备注">
        <el-input v-model="form.notes" type="textarea" :rows="2" placeholder="填写备注信息" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false" class="cancel-button">❌ 取消</el-button>
      <el-button type="primary" @click="submitForm" class="confirm-button">✅ 确认</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  modelValue: Boolean,
  data: Object
})
const emit = defineEmits(['update:modelValue', 'submit'])

const visible = ref(false)
const formRef = ref(null)

const form = reactive({
  id: null,
  name: '',
  ip: '',
  company: '',
  owner: '',
  project: '',
  visibility: '',
  isTarget: false,
  enabled: true,
  notes: '',
  assetType: 'server',
  // 新增：平台配置字段
  assetPlatform: 'docker',
  dockerPort: 2375,
  dockerApiEnabled: true,
  k8sApiServer: '',
  k8sToken: ''
})

const resetForm = () => {
  Object.assign(form, {
    id: null,
    name: '',
    ip: '',
    company: '',
    owner: '',
    project: '',
    visibility: '',
    isTarget: false,
    enabled: true,
    notes: '',
    assetType: 'server',
    assetPlatform: 'docker',
    dockerPort: 2375,
    dockerApiEnabled: true,
    k8sApiServer: '',
    k8sToken: ''
  })
}

// IP验证器：拒绝中文字符，只允许字母、数字、点、连字符
const validateIp = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入 IP 或域名'))
    return
  }

  // 检查是否包含中文字符
  if (/[\u4e00-\u9fa5]/.test(value)) {
    callback(new Error('IP 或域名不能包含中文字符'))
    return
  }

  // 检查是否只包含有效字符（字母、数字、点、连字符）
  if (!/^[a-zA-Z0-9.-]+$/.test(value)) {
    callback(new Error('IP 或域名只能包含字母、数字、点和连字符'))
    return
  }

  callback()
}

const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  ip: [
    { required: true, message: '请输入 IP 或域名', trigger: 'blur' },
    { validator: validateIp, trigger: 'blur' }
  ],
  company: [{ required: true, message: '请输入企业名', trigger: 'blur' }],
  owner: [{ required: true, message: '请输入负责人', trigger: 'blur' }],
  project: [{ required: true, message: '请输入项目功能区', trigger: 'blur' }],
  visibility: [{ required: true, message: '请选择可见性', trigger: 'change' }]
}

// 外部打开时设置数据
watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val) {
    if (props.data) Object.assign(form, props.data)
    else resetForm()
  }
})

// 双向绑定 visible 到外部
watch(visible, (val) => {
  emit('update:modelValue', val)
})

const submitForm = () => {
  formRef.value.validate((valid) => {
    if (valid) {
      emit('submit', { ...form })
      visible.value = false
    } else {
      ElMessage.warning('请完善表单信息')
    }
  })
}
</script>

<!-- ============================================
     Global Styles for Teleported Dialog
     全局样式 - 用于 Element Plus 传送到 body 的对话框
     ============================================ -->
<style>
/* Apple Asset Dialog Container */
.apple-asset-dialog {
  border-radius: 20px !important;
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.98) 0%,
    rgba(250, 250, 252, 0.95) 100%) !important;
  backdrop-filter: blur(24px) saturate(180%) !important;
  -webkit-backdrop-filter: blur(24px) saturate(180%) !important;
  border: 1px solid rgba(0, 0, 0, 0.06) !important;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12), 0 2px 8px rgba(0, 0, 0, 0.08) !important;
  overflow: hidden !important;
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", "Helvetica Neue", "Segoe UI", sans-serif !important;
  animation: dialogFadeIn 0.3s cubic-bezier(0.19, 1, 0.22, 1) !important;
}

@keyframes dialogFadeIn {
  from {
    opacity: 0;
    transform: scale(0.95) translateY(-10px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

/* Dialog Header */
.apple-asset-dialog .el-dialog__header {
  background: linear-gradient(135deg,
    rgba(245, 245, 247, 0.95) 0%,
    rgba(250, 250, 252, 0.9) 100%) !important;
  padding: 20px 24px !important;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06) !important;
  margin: 0 !important;
}

/* Dialog Title */
.apple-asset-dialog .el-dialog__title {
  font-size: 20px !important;
  font-weight: 700 !important;
  color: #1d1d1f !important;
  letter-spacing: -0.5px !important;
  line-height: 1.4 !important;
}

/* Close Button */
.apple-asset-dialog .el-dialog__headerbtn {
  top: 20px !important;
  right: 20px !important;
  width: 32px !important;
  height: 32px !important;
  border-radius: 50% !important;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1) !important;
  background: transparent !important;
}

.apple-asset-dialog .el-dialog__headerbtn:hover {
  background: rgba(0, 0, 0, 0.06) !important;
  transform: rotate(90deg) !important;
}

.apple-asset-dialog .el-dialog__headerbtn .el-dialog__close {
  color: #6e6e73 !important;
  font-size: 18px !important;
  font-weight: 600 !important;
}

/* Dialog Body */
.apple-asset-dialog .el-dialog__body {
  padding: 24px !important;
  background: rgba(255, 255, 255, 0.95) !important;
  max-height: 65vh !important;
  overflow-y: auto !important;
}

/* Custom Scrollbar */
.apple-asset-dialog .el-dialog__body::-webkit-scrollbar {
  width: 10px !important;
}

.apple-asset-dialog .el-dialog__body::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.02) !important;
  border-radius: 8px !important;
  margin: 4px 0 !important;
}

.apple-asset-dialog .el-dialog__body::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.12) !important;
  border-radius: 8px !important;
  border: 2px solid transparent !important;
  background-clip: content-box !important;
  transition: all 0.3s ease !important;
}

.apple-asset-dialog .el-dialog__body::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.2) !important;
  background-clip: content-box !important;
}

/* Dialog Footer */
.apple-asset-dialog .el-dialog__footer {
  padding: 16px 24px !important;
  background: linear-gradient(135deg,
    rgba(245, 245, 247, 0.95) 0%,
    rgba(250, 250, 252, 0.9) 100%) !important;
  border-top: 1px solid rgba(0, 0, 0, 0.06) !important;
  display: flex !important;
  justify-content: flex-end !important;
  gap: 12px !important;
  margin: 0 !important;
}

/* ============================================
   Form Styling - 表单样式
   ============================================ */
.apple-asset-dialog .el-form {
  font-family: var(--font-apple) !important;
}

.apple-asset-dialog .el-form-item {
  margin-bottom: 22px !important;
}

.apple-asset-dialog .el-form-item__label {
  font-size: 14px !important;
  font-weight: 600 !important;
  color: var(--apple-text-primary) !important;
  line-height: 40px !important;
  padding-right: 12px !important;
}

.apple-asset-dialog .el-form-item__content {
  line-height: 40px !important;
}

/* ============================================
   Input Styling - 输入框样式
   ============================================ */
.apple-asset-dialog .el-input,
.apple-asset-dialog .el-textarea,
.apple-asset-dialog .el-select,
.apple-asset-dialog .el-input-number {
  width: 100% !important;
}

.apple-asset-dialog .el-input__wrapper {
  border-radius: var(--radius-md) !important;
  border: 1.5px solid rgba(0, 0, 0, 0.12) !important;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04) !important;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1) !important;
  background: white !important;
  padding: 9px 13px !important;
}

.apple-asset-dialog .el-input__wrapper:hover {
  border-color: var(--apple-blue) !important;
  box-shadow: 0 2px 8px rgba(0, 122, 255, 0.15) !important;
}

.apple-asset-dialog .el-input__wrapper.is-focus {
  border-color: var(--apple-blue) !important;
  box-shadow: 0 0 0 4px rgba(0, 122, 255, 0.12) !important;
}

.apple-asset-dialog .el-input__inner {
  color: var(--apple-text-primary) !important;
  font-size: 14px !important;
  line-height: 22px !important;
  height: 22px !important;
}

.apple-asset-dialog .el-input__inner::placeholder {
  color: rgba(110, 110, 115, 0.6) !important;
}

/* Textarea - 文本域 */
.apple-asset-dialog .el-textarea__inner {
  border-radius: var(--radius-md) !important;
  border: 1.5px solid rgba(0, 0, 0, 0.12) !important;
  padding: 12px 13px !important;
  font-size: 14px !important;
  color: var(--apple-text-primary) !important;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1) !important;
  line-height: 1.6 !important;
  resize: vertical !important;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04) !important;
}

.apple-asset-dialog .el-textarea__inner:hover {
  border-color: var(--apple-blue) !important;
  box-shadow: 0 2px 8px rgba(0, 122, 255, 0.15) !important;
}

.apple-asset-dialog .el-textarea__inner:focus {
  border-color: var(--apple-blue) !important;
  box-shadow: 0 0 0 4px rgba(0, 122, 255, 0.12) !important;
}

.apple-asset-dialog .el-textarea__inner::placeholder {
  color: rgba(110, 110, 115, 0.6) !important;
}

/* Input Number - 数字输入框 */
.apple-asset-dialog .el-input-number__decrease,
.apple-asset-dialog .el-input-number__increase {
  width: 36px !important;
  border-radius: var(--radius-md) !important;
  background: rgba(0, 122, 255, 0.08) !important;
  color: var(--apple-blue) !important;
  border: none !important;
  transition: all 0.2s cubic-bezier(0.19, 1, 0.22, 1) !important;
}

.apple-asset-dialog .el-input-number__decrease:hover,
.apple-asset-dialog .el-input-number__increase:hover {
  background: rgba(0, 122, 255, 0.15) !important;
  color: #0051d5 !important;
  transform: scale(1.05) !important;
}

/* ============================================
   Select Styling - 下拉选择框样式
   ============================================ */
.apple-asset-dialog .el-select__wrapper {
  border-radius: var(--radius-md) !important;
  border: 1.5px solid rgba(0, 0, 0, 0.12) !important;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04) !important;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1) !important;
  background: white !important;
}

.apple-asset-dialog .el-select__wrapper:hover {
  border-color: var(--apple-blue) !important;
  box-shadow: 0 2px 8px rgba(0, 122, 255, 0.15) !important;
}

.apple-asset-dialog .el-select__wrapper.is-focused {
  border-color: var(--apple-blue) !important;
  box-shadow: 0 0 0 4px rgba(0, 122, 255, 0.12) !important;
}

/* ============================================
   Switch Styling - 开关样式
   ============================================ */
.apple-asset-dialog .el-switch {
  --el-switch-on-color: var(--apple-green) !important;
  --el-switch-off-color: rgba(120, 120, 128, 0.32) !important;
  height: 32px !important;
}

.apple-asset-dialog .el-switch__core {
  border-radius: 16px !important;
  height: 32px !important;
  min-width: 52px !important;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1) !important;
  border: none !important;
}

.apple-asset-dialog .el-switch__action {
  width: 28px !important;
  height: 28px !important;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.18), 0 1px 2px rgba(0, 0, 0, 0.12) !important;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1) !important;
}

/* ============================================
   Divider Styling - 分隔线样式
   ============================================ */
.apple-asset-dialog .el-divider {
  margin: 28px 0 24px 0 !important;
  border-color: rgba(0, 0, 0, 0.08) !important;
}

.apple-asset-dialog .el-divider__text {
  font-weight: 600 !important;
  font-size: 15px !important;
  color: var(--apple-text-primary) !important;
  background: rgba(255, 255, 255, 0.95) !important;
  padding: 0 16px !important;
}

/* ============================================
   Button Styling - 按钮样式
   ============================================ */
.apple-asset-dialog .el-button {
  height: 44px !important;
  min-width: 110px !important;
  font-size: 15px !important;
  padding: 0 24px !important;
  border-radius: var(--radius-md) !important;
  font-weight: 600 !important;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1) !important;
  border: none !important;
  font-family: var(--font-apple) !important;
}

/* 取消按钮 */
.apple-asset-dialog .el-button:not(.el-button--primary):not(.el-button--danger) {
  background: rgba(120, 120, 128, 0.12) !important;
  color: var(--apple-text-primary) !important;
}

.apple-asset-dialog .el-button:not(.el-button--primary):not(.el-button--danger):hover {
  background: rgba(120, 120, 128, 0.18) !important;
  transform: translateY(-2px) !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1) !important;
}

/* 确认按钮 */
.apple-asset-dialog .el-button--primary {
  background: linear-gradient(135deg, var(--apple-blue) 0%, #0051d5 100%) !important;
  color: white !important;
  box-shadow: 0 2px 8px rgba(0, 122, 255, 0.25) !important;
}

.apple-asset-dialog .el-button--primary:hover {
  background: linear-gradient(135deg, #0051d5 0%, #003db3 100%) !important;
  transform: translateY(-2px) !important;
  box-shadow: 0 6px 20px rgba(0, 122, 255, 0.4) !important;
}

/* ============================================
   Platform Config Section - 平台配置区域（AssetForm 特有）
   ============================================ */
.apple-asset-dialog .platform-config {
  background: linear-gradient(135deg,
    rgba(0, 122, 255, 0.04) 0%,
    rgba(0, 122, 255, 0.08) 100%) !important;
  padding: 20px !important;
  border-radius: var(--radius-md) !important;
  margin: 16px 0 !important;
  border: 1.5px solid rgba(0, 122, 255, 0.15) !important;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1) !important;
  box-shadow: 0 2px 8px rgba(0, 122, 255, 0.06) !important;
}

.apple-asset-dialog .platform-config:hover {
  border-color: rgba(0, 122, 255, 0.25) !important;
  box-shadow: 0 4px 16px rgba(0, 122, 255, 0.12) !important;
  transform: translateY(-1px) !important;
}

.apple-asset-dialog .platform-config .el-form-item {
  margin-bottom: 18px !important;
}

.apple-asset-dialog .platform-config .el-form-item:last-child {
  margin-bottom: 0 !important;
}

.apple-asset-dialog .form-tip {
  font-size: 12px !important;
  color: rgba(110, 110, 115, 0.8) !important;
  margin-top: 6px !important;
  display: block !important;
  line-height: 1.5 !important;
  font-style: italic !important;
}

/* ============================================
   Responsive Design - 响应式设计
   ============================================ */
@media (max-width: 768px) {
  .apple-asset-dialog {
    width: 95% !important;
    margin: 20px auto !important;
  }

  .apple-asset-dialog .el-form-item__label {
    width: 100% !important;
    text-align: left !important;
    padding-bottom: 8px !important;
  }

  .apple-asset-dialog .el-form-item__content {
    margin-left: 0 !important;
  }

  .apple-asset-dialog .platform-config {
    padding: 16px !important;
  }

  .apple-asset-dialog .el-button {
    flex: 1 !important;
    min-width: auto !important;
  }
}

@media (max-width: 576px) {
  .apple-asset-dialog .el-dialog__header,
  .apple-asset-dialog .el-dialog__body,
  .apple-asset-dialog .el-dialog__footer {
    padding: 16px !important;
  }

  .apple-asset-dialog .el-dialog__title {
    font-size: 18px !important;
  }

  .apple-asset-dialog .platform-config {
    padding: 12px !important;
  }
}
</style>