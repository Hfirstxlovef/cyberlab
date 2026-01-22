<template>
  <el-dialog
    title="🏢 新建项目资产"
    v-model="visible"
    width="600px"
    :close-on-click-modal="false"
    :lock-scroll="false"
    class="apple-project-dialog">
    <el-form :model="form" label-width="120px" :rules="rules" ref="formRef">
      <el-form-item label="🏢 企业名称" prop="company">
        <el-input
          v-model="form.company"
          placeholder="请输入企业名称，如：苏州科技大学"
          maxlength="50"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="📁 项目名称" prop="project">
        <el-input
          v-model="form.project"
          placeholder="请输入项目名称，如：网络安全演练项目"
          maxlength="50"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="📝 项目描述">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="3"
          placeholder="填写项目描述信息（可选）"
          maxlength="200"
          show-word-limit
        />
      </el-form-item>

      <!-- 预览项目ID -->
      <el-form-item label="🔖 项目标识">
        <div class="project-id-preview">
          <el-tag type="info" size="large">
            {{ projectIdPreview }}
          </el-tag>
          <div class="preview-hint">此标识将用于资产分组和管理</div>
        </div>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false" class="cancel-button">❌ 取消</el-button>
      <el-button type="primary" @click="submitForm" class="confirm-button" :loading="submitting">
        ✅ 创建项目
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  modelValue: Boolean
})
const emit = defineEmits(['update:modelValue', 'submit'])

const visible = ref(false)
const formRef = ref(null)
const submitting = ref(false)

const form = reactive({
  company: '',
  project: '',
  description: ''
})

// 项目ID预览
const projectIdPreview = computed(() => {
  const company = form.company.trim() || '企业名称'
  const project = form.project.trim() || '项目名称'
  return `${company}｜${project}`
})

const resetForm = () => {
  Object.assign(form, {
    company: '',
    project: '',
    description: ''
  })
}

// 验证规则
const validateProjectName = (rule, value, callback) => {
  if (!value || !value.trim()) {
    callback(new Error('请输入项目名称'))
  } else if (value.length < 2) {
    callback(new Error('项目名称至少2个字符'))
  } else if (/[｜|\\/*?"<>]/.test(value)) {
    callback(new Error('项目名称不能包含特殊字符：｜ | \\ / * ? " < >'))
  } else {
    callback()
  }
}

const validateCompanyName = (rule, value, callback) => {
  if (!value || !value.trim()) {
    callback(new Error('请输入企业名称'))
  } else if (value.length < 2) {
    callback(new Error('企业名称至少2个字符'))
  } else if (/[｜|\\/*?"<>]/.test(value)) {
    callback(new Error('企业名称不能包含特殊字符：｜ | \\ / * ? " < >'))
  } else {
    callback()
  }
}

const rules = {
  company: [
    { required: true, validator: validateCompanyName, trigger: 'blur' }
  ],
  project: [
    { required: true, validator: validateProjectName, trigger: 'blur' }
  ]
}

// 外部打开时设置数据
watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val) {
    resetForm()
  }
})

// 双向绑定 visible 到外部
watch(visible, (val) => {
  emit('update:modelValue', val)
})

const submitForm = () => {
  formRef.value.validate((valid) => {
    if (valid) {
      submitting.value = true

      // 组装项目数据
      const projectData = {
        company: form.company.trim(),
        project: form.project.trim(),
        description: form.description.trim(),
        projectId: `${form.company.trim()}｜${form.project.trim()}`
      }

      emit('submit', projectData)

      // 提交后重置加载状态（父组件负责关闭对话框）
      setTimeout(() => {
        submitting.value = false
      }, 1000)
    } else {
      ElMessage.warning('请完善表单信息')
    }
  })
}
</script>

<style scoped>
/* ============================================
   Component-specific Styles - 组件内部样式
   仅用于不在 Dialog 内部的元素
   ============================================ */

/* 项目ID预览区域 */
.project-id-preview {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.preview-hint {
  font-size: 13px !important;
  color: #86868b !important;
  font-weight: 400 !important;
  letter-spacing: -0.1px !important;
  font-style: normal !important;
  margin-top: 4px;
}
</style>

<!-- ============================================
     Global Styles for Teleported Dialog
     全局样式 - 用于 Element Plus 传送到 body 的对话框
     以复用现有为荣: 使用全局 apple-theme.css 的 CSS 变量
     ============================================ -->
<style>
/* ============================================
   Animations - 动画效果
   ============================================ */
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

/* 输入框聚焦光晕动画 */
@keyframes focusGlow {
  0% {
    box-shadow: 0 0 0 0 rgba(0, 122, 255, 0.3);
  }
  100% {
    box-shadow: 0 0 0 4px rgba(0, 122, 255, 0.12);
  }
}

/* 按钮加载脉冲动画 */
@keyframes pulse {
  0%, 100% {
    opacity: 0.8;
    transform: scale(1);
  }
  50% {
    opacity: 0.6;
    transform: scale(0.98);
  }
}

/* ============================================
   Dialog Container - 对话框容器
   ============================================ */
/* Apple Project Dialog Container - 三层阴影系统 */
.apple-project-dialog {
  border-radius: var(--radius-lg) !important;
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.98) 0%,
    rgba(250, 250, 252, 0.95) 100%) !important;
  backdrop-filter: blur(24px) saturate(180%) !important;
  -webkit-backdrop-filter: blur(24px) saturate(180%) !important;
  border: 1px solid rgba(0, 0, 0, 0.06) !important;
  /* Apple风格三层阴影 - 更精致的深度感 */
  box-shadow:
    0 20px 50px rgba(0, 0, 0, 0.12),
    0 10px 25px rgba(0, 0, 0, 0.08),
    0 5px 10px rgba(0, 0, 0, 0.04) !important;
  overflow: hidden !important;
  font-family: var(--font-apple) !important;
  animation: dialogFadeIn 0.35s cubic-bezier(0.19, 1, 0.22, 1) !important;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1) !important;
}

/* Dialog Header */
.apple-project-dialog .el-dialog__header {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.98) 0%,
    rgba(248, 248, 248, 0.95) 100%) !important;
  backdrop-filter: blur(10px) !important;
  -webkit-backdrop-filter: blur(10px) !important;
  padding: 24px !important;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06) !important;
  margin: 0 !important;
}

/* Dialog Title */
.apple-project-dialog .el-dialog__title {
  font-size: 18px !important;
  font-weight: 700 !important;
  color: #1d1d1f !important;
  letter-spacing: -0.3px !important;
  line-height: 1.4 !important;
}

/* Close Button */
.apple-project-dialog .el-dialog__headerbtn {
  top: 24px !important;
  right: 24px !important;
  width: 32px !important;
  height: 32px !important;
  border-radius: 50% !important;
  transition: all 0.3s ease !important;
  background: transparent !important;
}

.apple-project-dialog .el-dialog__headerbtn:hover {
  background: rgba(0, 0, 0, 0.06) !important;
  transform: rotate(90deg) !important;
}

.apple-project-dialog .el-dialog__headerbtn .el-dialog__close {
  color: #6e6e73 !important;
  font-size: 18px !important;
  font-weight: 600 !important;
  transition: color 0.3s ease !important;
}

.apple-project-dialog .el-dialog__headerbtn .el-dialog__close:hover {
  color: #007aff !important;
}

/* Dialog Body */
.apple-project-dialog .el-dialog__body {
  padding: 24px !important;
  background: rgba(255, 255, 255, 0.95) !important;
}

/* Dialog Footer */
.apple-project-dialog .el-dialog__footer {
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
.apple-project-dialog .el-form {
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", "Helvetica Neue", "Segoe UI", sans-serif !important;
}

.apple-project-dialog .el-form-item {
  margin-bottom: 22px !important;
}

.apple-project-dialog .el-form-item__label {
  font-size: 15px !important;
  font-weight: 600 !important;
  color: #1d1d1f !important;
  line-height: 40px !important;
  padding-right: 12px !important;
  letter-spacing: -0.2px !important;
}

.apple-project-dialog .el-form-item__content {
  line-height: 40px !important;
}

/* ============================================
   Input Styling - 输入框样式
   ============================================ */
.apple-project-dialog .el-input__wrapper {
  border-radius: 12px !important;
  border: 1px solid rgba(0, 0, 0, 0.08) !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04) !important;
  transition: all 0.3s ease !important;
  background: white !important;
  padding: 10px 15px !important;
}

.apple-project-dialog .el-input__wrapper:hover {
  border-color: var(--apple-blue) !important;
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.08) !important;
}

.apple-project-dialog .el-input__wrapper.is-focus {
  border-color: var(--apple-blue) !important;
  box-shadow: 0 4px 16px rgba(0, 122, 255, 0.15) !important;
  animation: focusGlow 0.3s ease !important;
}

.apple-project-dialog .el-input__inner {
  color: #1d1d1f !important;
  font-size: 15px !important;
  font-weight: 400 !important;
  letter-spacing: -0.2px !important;
  line-height: 22px !important;
  height: 22px !important;
}

.apple-project-dialog .el-input__inner::placeholder {
  color: #86868b !important;
  font-weight: 400 !important;
}

/* ============================================
   Textarea Styling - 文本域样式
   ============================================ */
.apple-project-dialog .el-textarea__inner {
  border-radius: 12px !important;
  border: 1px solid rgba(0, 0, 0, 0.08) !important;
  padding: 12px 15px !important;
  font-size: 15px !important;
  font-weight: 400 !important;
  letter-spacing: -0.2px !important;
  color: #1d1d1f !important;
  transition: all 0.3s ease !important;
  line-height: 1.6 !important;
  resize: vertical !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04) !important;
}

.apple-project-dialog .el-textarea__inner:hover {
  border-color: var(--apple-blue) !important;
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.08) !important;
}

.apple-project-dialog .el-textarea__inner:focus {
  border-color: var(--apple-blue) !important;
  box-shadow: 0 4px 16px rgba(0, 122, 255, 0.15) !important;
  animation: focusGlow 0.3s ease !important;
}

.apple-project-dialog .el-textarea__inner::placeholder {
  color: #86868b !important;
  font-weight: 400 !important;
}

/* ============================================
   Word Count - 字数统计样式
   ============================================ */
.apple-project-dialog .el-input__count {
  color: #86868b !important;
  font-size: 12px !important;
  font-weight: 400 !important;
  background: transparent !important;
  letter-spacing: -0.1px !important;
}

.apple-project-dialog .el-input__count-inner {
  background: transparent !important;
}

/* ============================================
   Tag Styling - 标签样式（项目ID预览）
   ============================================ */
.apple-project-dialog .el-tag {
  font-size: 16px !important;
  font-weight: 600 !important;
  letter-spacing: -0.5px !important;
  padding: 12px 18px !important;
  border-radius: 12px !important;
  background: linear-gradient(135deg,
    rgba(0, 122, 255, 0.08) 0%,
    rgba(0, 122, 255, 0.12) 100%) !important;
  border: 1.5px solid rgba(0, 122, 255, 0.2) !important;
  color: var(--apple-blue) !important;
  /* 等宽字体 - 代码风格 */
  font-family: 'SF Mono', 'Monaco', 'Consolas', 'Courier New', monospace !important;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1) !important;
  box-shadow: 0 2px 8px rgba(0, 122, 255, 0.1) !important;
}

/* 标签悬停效果 */
.apple-project-dialog .el-tag:hover {
  border-color: rgba(0, 122, 255, 0.35) !important;
  background: linear-gradient(135deg,
    rgba(0, 122, 255, 0.12) 0%,
    rgba(0, 122, 255, 0.16) 100%) !important;
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.18) !important;
  transform: translateY(-1px) !important;
}

/* ============================================
   Button Styling - 按钮样式
   ============================================ */
.apple-project-dialog .el-button {
  height: 44px !important;
  min-width: 110px !important;
  font-size: 15px !important;
  padding: 0 24px !important;
  border-radius: 12px !important;
  font-weight: 600 !important;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1) !important;
  border: none !important;
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", "Helvetica Neue", "Segoe UI", sans-serif !important;
}

/* 取消按钮 */
.apple-project-dialog .el-button:not(.el-button--primary) {
  background: rgba(120, 120, 128, 0.12) !important;
  color: #1d1d1f !important;
}

.apple-project-dialog .el-button:not(.el-button--primary):hover {
  background: rgba(120, 120, 128, 0.18) !important;
  transform: translateY(-2px) !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1) !important;
}

/* 创建项目按钮 */
.apple-project-dialog .el-button--primary {
  background: linear-gradient(135deg, #007aff 0%, #0051d5 100%) !important;
  color: white !important;
  box-shadow: 0 2px 8px rgba(0, 122, 255, 0.25) !important;
}

.apple-project-dialog .el-button--primary:hover {
  background: linear-gradient(135deg, #0051d5 0%, #003db3 100%) !important;
  transform: translateY(-2px) !important;
  box-shadow: 0 6px 20px rgba(0, 122, 255, 0.4) !important;
}

.apple-project-dialog .el-button.is-loading {
  opacity: 0.8 !important;
  pointer-events: none !important;
  animation: pulse 1.5s ease-in-out infinite !important;
}

/* ============================================
   Responsive Design - 响应式设计
   ============================================ */
@media (max-width: 768px) {
  .apple-project-dialog {
    width: 90% !important;
    margin: 24px auto !important;
  }

  .apple-project-dialog .el-dialog__header {
    padding: 20px !important;
  }

  .apple-project-dialog .el-dialog__body {
    padding: 20px !important;
  }

  .apple-project-dialog .el-dialog__footer {
    padding: 16px 20px !important;
    flex-wrap: wrap !important;
  }

  .apple-project-dialog .el-button {
    flex: 1 !important;
    min-width: 100px !important;
  }
}

@media (max-width: 576px) {
  .apple-project-dialog {
    width: 95vw !important;
    margin: 16px auto !important;
    max-height: calc(100vh - 32px) !important;
  }

  .apple-project-dialog .el-dialog__header {
    padding: 16px !important;
  }

  .apple-project-dialog .el-dialog__title {
    font-size: 16px !important;
  }

  .apple-project-dialog .el-dialog__body {
    padding: 16px !important;
    max-height: calc(100vh - 160px) !important;
  }

  .apple-project-dialog .el-dialog__footer {
    padding: 12px 16px !important;
    gap: 8px !important;
  }

  .apple-project-dialog .el-form-item__label {
    font-size: 14px !important;
  }

  .apple-project-dialog .el-input__inner,
  .apple-project-dialog .el-textarea__inner {
    font-size: 14px !important;
  }

  .apple-project-dialog .el-button {
    height: 40px !important;
    font-size: 14px !important;
    min-width: auto !important;
  }

  /* 移动端优化：项目ID标签 */
  .apple-project-dialog .el-tag {
    font-size: 14px !important;
    padding: 8px 12px !important;
    word-break: break-all !important;
  }
}
</style>
