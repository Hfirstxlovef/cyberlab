<template>
  <div class="achievement-submit-page">
    <el-card class="page-card">
      <template #header>
        <div class="card-header">
          <span>📝 提交攻防成果</span>
        </div>
      </template>

      <el-form 
        ref="submitFormRef" 
        :model="submitForm" 
        :rules="submitRules" 
        label-width="120px"
        class="submit-form">
        
        <!-- 所属演练 -->
        <el-form-item label="所属演练" prop="rangeId">
          <el-select v-model="submitForm.rangeId" placeholder="请选择演练" style="width: 100%">
            <el-option 
              v-for="range in rangeList" 
              :key="range.id" 
              :label="range.name" 
              :value="range.id">
            </el-option>
          </el-select>
        </el-form-item>

        <!-- 提交方名称 -->
        <el-form-item label="队伍名称" prop="teamName">
          <el-input v-model="submitForm.teamName" placeholder="请输入队伍名称"></el-input>
        </el-form-item>

        <!-- 攻击目标 -->
        <el-form-item label="攻击目标" prop="targetName">
          <el-input v-model="submitForm.targetName" placeholder="请输入攻击目标名称"></el-input>
        </el-form-item>

        <!-- 攻击描述 -->
        <el-form-item label="攻击描述" prop="description">
          <el-input 
            v-model="submitForm.description" 
            type="textarea" 
            :rows="4" 
            placeholder="请详细描述攻击过程和发现的漏洞">
          </el-input>
        </el-form-item>

        <!-- 攻击工具/方法 -->
        <el-form-item label="攻击工具/方法">
          <el-input v-model="submitForm.attackMethod" placeholder="请输入使用的攻击工具或方法（可选）"></el-input>
        </el-form-item>

        <!-- 漏洞截图 -->
        <el-form-item label="漏洞截图">
          <el-upload
            ref="screenshotUpload"
            :file-list="screenshotFileList"
            :auto-upload="false"
            multiple
            accept="image/*"
            list-type="picture-card"
            :on-change="handleScreenshotChange">
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>

        <!-- 证明文件 -->
        <el-form-item label="证明文件">
          <el-upload
            ref="proofUpload"
            :file-list="proofFileList"
            :auto-upload="false"
            multiple
            :on-change="handleProofChange">
            <el-button type="primary">选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">支持上传视频、日志等证明文件</div>
            </template>
          </el-upload>
        </el-form-item>

        <!-- 提交按钮 -->
        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">提交成果</el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import axios from 'axios'

// 响应式数据
const submitFormRef = ref()
const screenshotUpload = ref()
const proofUpload = ref()
const submitting = ref(false)
const rangeList = ref([])
const screenshotFileList = ref([])
const proofFileList = ref([])

// 表单数据
const submitForm = reactive({
  rangeId: '',
  teamName: '',
  targetName: '',
  description: '',
  attackMethod: ''
})

// 表单验证规则
const submitRules = {
  rangeId: [{ required: true, message: '请选择所属演练', trigger: 'change' }],
  teamName: [{ required: true, message: '请输入队伍名称', trigger: 'blur' }],
  targetName: [{ required: true, message: '请输入攻击目标', trigger: 'blur' }],
  description: [{ required: true, message: '请输入攻击描述', trigger: 'blur' }]
}

// 获取演练列表
const fetchRanges = async () => {
  try {
    const response = await axios.get('/range')
    rangeList.value = response.data.filter(range => range.status === 'running')
  } catch {
    ElMessage.error('失败')
  } finally {
    // 这里不需要设置rejecting状态，因为它未被定义且未被使用
  }
}

// 处理截图文件变化
const handleScreenshotChange = (file, fileList) => {
  screenshotFileList.value = fileList
}

// 处理证明文件变化
const handleProofChange = (file, fileList) => {
  proofFileList.value = fileList
}

// 提交成果
const handleSubmit = async () => {
  try {
    await submitFormRef.value.validate()
    
    submitting.value = true
    
    const formData = new FormData()
    formData.append('rangeId', submitForm.rangeId)
    formData.append('teamName', submitForm.teamName)
    formData.append('targetName', submitForm.targetName)
    formData.append('description', submitForm.description)
    formData.append('attackMethod', submitForm.attackMethod)
    
    // 添加截图文件
    screenshotFileList.value.forEach(file => {
      if (file.raw) {
        formData.append('screenshots', file.raw)
      }
    })
    
    // 添加证明文件
    proofFileList.value.forEach(file => {
      if (file.raw) {
        formData.append('proofFiles', file.raw)
      }
    })
    
    const response = await axios.post('/api/achievements/submit', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    
    if (response.data.success) {
      ElMessage.success('成果提交成功')
      resetForm()
    } else {
      ElMessage.error(response.data.message || '提交失败')
    }
  } catch {    ElMessage.error('提交失败: ' + (error.response?.data?.message || error.message))
  } finally {
    submitting.value = false
  }
}

// 重置表单
const resetForm = () => {
  submitFormRef.value.resetFields()
  screenshotFileList.value = []
  proofFileList.value = []
}

// 页面加载时获取演练列表
onMounted(() => {
  fetchRanges()
})
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

.achievement-submit-page {
  background: transparent;
  padding: var(--spacing-lg);
  font-family: var(--font-apple);
  min-height: 100vh;
}

/* ============================================
   Header with Large Emoji
   ============================================ */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-xl);
  padding-bottom: var(--spacing-lg);
  border-bottom: 1px solid var(--apple-border);
}

.header-content {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.header-icon {
  font-size: 48px;
  line-height: 1;
  filter: drop-shadow(0 4px 12px rgba(0, 0, 0, 0.08));
}

.header-title {
  margin: 0;
  font-size: 32px;
  font-weight: 700;
  color: var(--apple-text);
  letter-spacing: -0.5px;
}

/* ============================================
   Main Card with Apple Aesthetic
   ============================================ */
.page-card {
  max-width: 900px;
  margin: 0 auto;
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

.page-card:hover {
  box-shadow: var(--shadow-card-hover);
  transform: translateY(-2px);
}

.card-header {
  font-size: 18px;
  font-weight: 700;
  color: var(--apple-text);
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.card-header span {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
}

/* ============================================
   Form Styling
   ============================================ */
.submit-form {
  margin-top: var(--spacing-lg);
  padding: var(--spacing-md);
}

.submit-form :deep(.el-form-item__label) {
  font-weight: 600;
  color: var(--apple-text);
  font-family: var(--font-apple);
}

.submit-form :deep(.el-input__wrapper) {
  border-radius: var(--radius-sm);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
  border: 1px solid var(--apple-border);
}

.submit-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.08);
  border-color: var(--apple-blue);
}

.submit-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 4px 16px rgba(0, 122, 255, 0.15);
  border-color: var(--apple-blue);
}

.submit-form :deep(.el-textarea__inner) {
  border-radius: var(--radius-sm);
  border: 1px solid var(--apple-border);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
  font-family: var(--font-apple);
  line-height: 1.6;
}

.submit-form :deep(.el-textarea__inner:hover) {
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.08);
  border-color: var(--apple-blue);
}

.submit-form :deep(.el-textarea__inner:focus) {
  box-shadow: 0 4px 16px rgba(0, 122, 255, 0.15);
  border-color: var(--apple-blue);
}

.submit-form :deep(.el-select) {
  width: 100%;
}

.submit-form :deep(.el-select .el-input__wrapper) {
  border-radius: var(--radius-sm);
}

/* ============================================
   Upload Components
   ============================================ */
.submit-form :deep(.el-upload-list--picture-card) {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-sm);
}

.submit-form :deep(.el-upload--picture-card) {
  width: 120px;
  height: 120px;
  border-radius: var(--radius-md);
  border: 2px dashed var(--apple-border);
  background: rgba(0, 122, 255, 0.02);
  transition: all 0.3s ease;
}

.submit-form :deep(.el-upload--picture-card:hover) {
  border-color: var(--apple-blue);
  background: rgba(0, 122, 255, 0.06);
  transform: scale(1.02);
}

.submit-form :deep(.el-upload-list__item) {
  border-radius: var(--radius-md);
  border: 1px solid var(--apple-border);
  overflow: hidden;
}

.submit-form :deep(.el-upload__tip) {
  font-size: 12px;
  color: var(--apple-text-secondary);
  margin-top: var(--spacing-xs);
  font-family: var(--font-apple);
}

/* ============================================
   Buttons
   ============================================ */
.submit-form :deep(.el-button) {
  border-radius: var(--radius-sm);
  font-weight: 600;
  font-family: var(--font-apple);
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
  padding: 12px 24px;
  font-size: 14px;
}

.submit-form :deep(.el-button--primary) {
  background: linear-gradient(135deg, var(--apple-blue) 0%, #0051d5 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.25);
}

.submit-form :deep(.el-button--primary:hover) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 122, 255, 0.35);
}

.submit-form :deep(.el-button--primary:active) {
  transform: translateY(0);
}

.submit-form :deep(.el-button--default) {
  background: rgba(0, 0, 0, 0.02);
  border: 1px solid var(--apple-border);
  color: var(--apple-text);
}

.submit-form :deep(.el-button--default:hover) {
  background: rgba(0, 0, 0, 0.04);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

/* ============================================
   Upload Section Styling
   ============================================ */
.submit-form :deep(.el-form-item:has(.el-upload)) {
  padding: var(--spacing-md);
  background: rgba(0, 122, 255, 0.02);
  border-radius: var(--radius-md);
  margin-bottom: var(--spacing-lg);
  border: 1px solid rgba(0, 122, 255, 0.08);
}

/* ============================================
   Responsive Design
   ============================================ */
@media (max-width: 768px) {
  .achievement-submit-page {
    padding: var(--spacing-md);
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--spacing-md);
  }

  .header-icon {
    font-size: 40px;
  }

  .header-title {
    font-size: 26px;
  }

  .page-card {
    border-radius: var(--radius-md);
  }

  .submit-form {
    padding: var(--spacing-sm);
  }

  .submit-form :deep(.el-form-item__label) {
    font-size: 14px;
  }
}

@media (max-width: 576px) {
  .header-icon {
    font-size: 36px;
  }

  .header-title {
    font-size: 22px;
  }

  .submit-form :deep(.el-button) {
    width: 100%;
    margin-bottom: var(--spacing-sm);
  }

  .submit-form :deep(.el-upload--picture-card) {
    width: 100px;
    height: 100px;
  }
}

/* ============================================
   Loading Animation
   ============================================ */
.submit-form :deep(.el-button.is-loading) {
  position: relative;
}

.submit-form :deep(.el-button.is-loading::before) {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  animation: loading-shine 1.5s infinite;
}

@keyframes loading-shine {
  0% {
    transform: translateX(-100%);
  }
  100% {
    transform: translateX(100%);
  }
}
</style>