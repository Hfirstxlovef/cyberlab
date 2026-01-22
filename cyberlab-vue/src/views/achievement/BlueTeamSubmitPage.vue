<template>
  <div class="blue-team-submit-page" :class="themeClass">
    <el-card class="page-card">
      <template #header>
        <div class="card-header">
          <span>🟦 防守战报提交</span>
        </div>
      </template>

      <el-form 
        ref="submitFormRef" 
        :model="submitForm" 
        :rules="submitRules" 
        label-width="140px"
        class="submit-form">
        
        <!-- 基础信息 -->
        <el-divider content-position="left">基础信息</el-divider>
        
        <el-form-item label="所属演练" prop="drillId">
          <!-- 蓝队用户：使用原生select（样式可控） -->
          <select v-if="currentRole === 'blue'" v-model="submitForm.drillId" class="custom-select blue-team-select" required>
            <option value="" disabled>请选择演练</option>
            <option v-for="drill in drillList" :key="drill.id" :value="drill.id">
              {{ drill.name }}
            </option>
          </select>

          <!-- Admin用户：保持el-select -->
          <el-select v-else v-model="submitForm.drillId" placeholder="请选择演练" style="width: 100%">
            <el-option
              v-for="drill in drillList"
              :key="drill.id"
              :label="drill.name"
              :value="drill.id">
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="所属战队">
          <div v-if="loadingTeam" style="color: rgba(255, 255, 255, 0.6);">
            正在加载战队信息...
          </div>
          <div v-else-if="userTeam" class="team-info-display">
            <span class="team-name">{{ userTeam.name }}</span>
            <el-tag :type="userTeam.teamType === 'blue' ? 'primary' : 'danger'" size="small">
              {{ userTeam.teamType === 'blue' ? '蓝队' : '红队' }}
            </el-tag>
          </div>
          <el-alert v-else type="info" :closable="false" show-icon>
            <template #title>
              您尚未加入战队，将以个人身份提交成果
            </template>
          </el-alert>
        </el-form-item>

        <el-form-item label="防守内容分类" prop="defenseType">
          <!-- 蓝队用户：使用原生select（样式可控） -->
          <select v-if="currentRole === 'blue'" v-model="submitForm.defenseType" class="custom-select blue-team-select" required>
            <option value="" disabled>请选择防守类型</option>
            <option value="detection">威胁检测</option>
            <option value="blocking">攻击阻断</option>
            <option value="tracing">事件溯源</option>
            <option value="response">应急响应</option>
            <option value="forensics">取证分析</option>
            <option value="hardening">安全加固</option>
          </select>

          <!-- Admin用户：保持el-select -->
          <el-select v-else v-model="submitForm.defenseType" placeholder="请选择防守类型">
            <el-option label="威胁检测" value="detection"></el-option>
            <el-option label="攻击阻断" value="blocking"></el-option>
            <el-option label="事件溯源" value="tracing"></el-option>
            <el-option label="应急响应" value="response"></el-option>
            <el-option label="取证分析" value="forensics"></el-option>
            <el-option label="安全加固" value="hardening"></el-option>
          </el-select>
        </el-form-item>

        <!-- 事件过程 -->
        <el-divider content-position="left">事件过程</el-divider>
        
        <el-form-item label="事件起始时间" prop="incidentStartTime">
          <el-date-picker
            v-model="submitForm.incidentStartTime"
            type="datetime"
            placeholder="选择事件开始时间"
            style="width: 100%">
          </el-date-picker>
        </el-form-item>

        <el-form-item label="事件结束时间" prop="incidentEndTime">
          <el-date-picker
            v-model="submitForm.incidentEndTime"
            type="datetime"
            placeholder="选择事件结束时间"
            style="width: 100%">
          </el-date-picker>
        </el-form-item>

        <el-form-item label="事件过程说明" prop="incidentProcess">
          <el-input 
            v-model="submitForm.incidentProcess" 
            type="textarea" 
            :rows="5" 
            placeholder="请详细描述事件的发现、分析、处置全过程">
          </el-input>
        </el-form-item>

        <el-form-item label="响应过程" prop="responseProcess">
          <el-input 
            v-model="submitForm.responseProcess" 
            type="textarea" 
            :rows="4" 
            placeholder="请描述应急响应的具体步骤和措施">
          </el-input>
        </el-form-item>

        <el-form-item label="处置结果" prop="responseResult">
          <el-input 
            v-model="submitForm.responseResult" 
            type="textarea" 
            :rows="3" 
            placeholder="请描述最终的处置结果和效果">
          </el-input>
        </el-form-item>

        <!-- 技术分析 -->
        <el-divider content-position="left">技术分析</el-divider>
        
        <el-form-item label="检测方法" prop="detectionMethod">
          <el-input 
            v-model="submitForm.detectionMethod" 
            type="textarea" 
            :rows="3" 
            placeholder="请描述使用的检测技术和方法">
          </el-input>
        </el-form-item>

        <el-form-item label="分析工具" prop="analysisTools">
          <el-input 
            v-model="submitForm.analysisTools" 
            placeholder="请列出使用的分析工具和平台">
          </el-input>
        </el-form-item>

        <el-form-item label="取证链条" prop="forensicsChain">
          <el-input 
            v-model="submitForm.forensicsChain" 
            type="textarea" 
            :rows="4" 
            placeholder="请描述完整的取证分析链条和证据">
          </el-input>
        </el-form-item>

        <!-- 改进建议 -->
        <el-divider content-position="left">改进建议</el-divider>
        
        <el-form-item label="封堵方案" prop="blockingSolution">
          <el-input 
            v-model="submitForm.blockingSolution" 
            type="textarea" 
            :rows="3" 
            placeholder="请提供针对性的封堵方案（可选）">
          </el-input>
        </el-form-item>

        <el-form-item label="补丁建议" prop="patchRecommendation">
          <el-input 
            v-model="submitForm.patchRecommendation" 
            type="textarea" 
            :rows="3" 
            placeholder="请提供安全补丁或加固建议（可选）">
          </el-input>
        </el-form-item>

        <!-- 证明材料上传 -->
        <el-divider content-position="left">证明材料</el-divider>
        
        <el-form-item label="监测日志截图">
          <el-upload
            ref="logScreenshotUpload"
            :file-list="logScreenshotFileList"
            :auto-upload="false"
            multiple
            accept="image/*"
            list-type="picture-card"
            :on-change="handleLogScreenshotChange">
            <el-icon><Plus /></el-icon>
          </el-upload>
          <div class="upload-tip">上传监测系统的告警和日志截图</div>
        </el-form-item>

        <el-form-item label="应急记录表">
          <el-upload
            ref="emergencyRecordUpload"
            :file-list="emergencyRecordFileList"
            :auto-upload="false"
            multiple
            :on-change="handleEmergencyRecordChange">
            <el-button type="primary">选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">上传应急响应记录表、处置报告等</div>
            </template>
          </el-upload>
        </el-form-item>

        <el-form-item label="取证文件">
          <el-upload
            ref="forensicsUpload"
            :file-list="forensicsFileList"
            :auto-upload="false"
            multiple
            :on-change="handleForensicsChange">
            <el-button type="primary">选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">上传取证镜像、内存dump、网络流量等</div>
            </template>
          </el-upload>
        </el-form-item>

        <el-form-item label="分析报告">
          <el-upload
            ref="analysisReportUpload"
            :file-list="analysisReportFileList"
            :auto-upload="false"
            multiple
            accept=".pdf,.doc,.docx"
            :on-change="handleAnalysisReportChange">
            <el-button type="primary">选择报告</el-button>
            <template #tip>
              <div class="el-upload__tip">上传详细的技术分析报告</div>
            </template>
          </el-upload>
        </el-form-item>

        <!-- 提交按钮 -->
        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting" size="large">
            🟦 提交蓝队防守成果
          </el-button>
          <el-button @click="resetForm" size="large">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import axios from '@/api/axios'
import { getActiveRanges, getAllRanges } from '@/api/range'
import { getUserRole, getUsername, getUserId } from '@/utils/auth'
import { getTeamByUserId } from '@/api/team'

// 主题支持
const role = getUserRole() || ''
const currentRole = computed(() => getUserRole())
const themeClass = computed(() => role === 'blue' ? 'theme-blue' : 'theme-admin')

// 为body添加主题class，以便全局组件应用主题
onMounted(() => {
  if (role === 'blue') {
    document.body.classList.add('blue-theme-active')
  }
  fetchDrills()
  fetchUserTeam()
})

// 组件卸载时移除body的主题class
onUnmounted(() => {
  if (role === 'blue') {
    document.body.classList.remove('blue-theme-active')
  }
})

// 响应式数据
const submitFormRef = ref()
const submitting = ref(false)
const drillList = ref([])
const logScreenshotFileList = ref([])
const emergencyRecordFileList = ref([])
const forensicsFileList = ref([])
const analysisReportFileList = ref([])

// 用户战队信息
const userTeam = ref(null)
const loadingTeam = ref(false)
const teamError = ref('')

// 表单数据
const submitForm = reactive({
  drillId: '',
  teamName: '',
  defenseType: '',
  incidentStartTime: '',
  incidentEndTime: '',
  incidentProcess: '',
  responseProcess: '',
  responseResult: '',
  detectionMethod: '',
  analysisTools: '',
  forensicsChain: '',
  blockingSolution: '',
  patchRecommendation: ''
})

// 表单验证规则
const submitRules = {
  drillId: [{ required: true, message: '请选择所属演练', trigger: 'change' }],
  teamName: [{ required: true, message: '请输入蓝队名称', trigger: 'blur' }],
  defenseType: [{ required: true, message: '请选择防守类型', trigger: 'change' }],
  incidentStartTime: [{ required: true, message: '请选择事件开始时间', trigger: 'change' }],
  incidentProcess: [{ required: true, message: '请描述事件过程', trigger: 'blur' }],
  responseProcess: [{ required: true, message: '请描述响应过程', trigger: 'blur' }],
  responseResult: [{ required: true, message: '请描述处置结果', trigger: 'blur' }]
}

// 获取演练列表
const fetchDrills = async () => {
  try {
    // 使用专用的API方法获取正在运行的演练
    const response = await getActiveRanges()
    drillList.value = response.map(range => ({
      id: range.id,
      name: range.name
    }))
  } catch (error) {
    // 如果专用API失败，尝试获取所有演练并过滤
    try {
      const response = await getAllRanges()
      drillList.value = response.filter(range => range.status === 'running').map(range => ({
        id: range.id,
        name: range.name
      }))
    } catch (fallbackError) {
      // 提供默认数据
      drillList.value = [
        { id: 1, name: '春季攻防演练' },
        { id: 2, name: '夏季红蓝对抗' },
        { id: 3, name: '秋季实战演习' }
      ]
    }
  }
}

// 获取用户所属战队
const fetchUserTeam = async () => {
  try {
    loadingTeam.value = true
    teamError.value = ''

    const userId = getUserId()
    if (!userId) {
      teamError.value = '无法获取用户信息'
      return
    }

    const response = await getTeamByUserId(userId)
    if (response) {
      userTeam.value = response
    } else {
      teamError.value = '您尚未加入任何战队'
    }
  } catch (error) {
    teamError.value = '获取战队信息失败'
  } finally {
    loadingTeam.value = false
  }
}

// 文件上传处理函数
const handleLogScreenshotChange = (file, fileList) => {
  logScreenshotFileList.value = fileList
}

const handleEmergencyRecordChange = (file, fileList) => {
  emergencyRecordFileList.value = fileList
}

const handleForensicsChange = (file, fileList) => {
  forensicsFileList.value = fileList
}

const handleAnalysisReportChange = (file, fileList) => {
  analysisReportFileList.value = fileList
}

// 提交成果
const handleSubmit = async () => {
  try {
    await submitFormRef.value.validate()

    submitting.value = true

    // 确定 teamName
    let teamName
    if (userTeam.value) {
      teamName = userTeam.value.name
    } else {
      const username = getUsername()
      if (!username) {
        ElMessage.error('无法获取用户信息，请重新登录')
        submitting.value = false
        return
      }
      teamName = `个人 - ${username}`
    }

    const formData = new FormData()
    // 匹配后端Controller期望的参数名
    formData.append('rangeId', submitForm.drillId) // 使用drillId作为rangeId
    formData.append('teamName', teamName)
    formData.append('targetName', submitForm.defenseType || '防御系统') // 使用defenseType作为targetName
    formData.append('description', `防御类型: ${submitForm.defenseType}\n事件过程: ${submitForm.incidentProcess}\n响应过程: ${submitForm.responseProcess}\n检测方法: ${submitForm.detectionMethod}\n分析工具: ${submitForm.analysisTools}\n阻断方案: ${submitForm.blockingSolution}`)
    formData.append('attackMethod', submitForm.analysisTools || '蓝队防御')
    
    
    // 添加各类文件
    logScreenshotFileList.value.forEach(file => {
      if (file.raw) formData.append('screenshots', file.raw)
    })
    
    emergencyRecordFileList.value.forEach(file => {
      if (file.raw) formData.append('proofFiles', file.raw)
    })
    
    forensicsFileList.value.forEach(file => {
      if (file.raw) formData.append('proofFiles', file.raw)
    })
    
    analysisReportFileList.value.forEach(file => {
      if (file.raw) formData.append('proofFiles', file.raw)
    })
    
    const response = await axios.post('/achievements/submit', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    
    if (response.success) {
      ElMessage.success('蓝队防守成果提交成功！')
      resetForm()
    } else {
      ElMessage.error(response.message || '提交失败')
    }
  } catch (error) {
    ElMessage.error('提交失败: ' + (error.response?.data?.message || error.message))
  } finally {
    submitting.value = false
  }
}

// 重置表单
const resetForm = () => {
  submitFormRef.value.resetFields()
  logScreenshotFileList.value = []
  emergencyRecordFileList.value = []
  forensicsFileList.value = []
  analysisReportFileList.value = []
}
</script>

<style scoped>
/* ============================================
   Apple Elegant White Style - 蓝队主题
   Blue Team Submit Page
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
  --blue-team-primary: #3498db;
  --blue-team-secondary: #2980b9;
  --blue-team-light: rgba(52, 152, 219, 0.1);
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

.blue-team-submit-page {
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
  filter: drop-shadow(0 4px 12px rgba(52, 152, 219, 0.15));
}

.header-title {
  margin: 0;
  font-size: 32px;
  font-weight: 700;
  color: var(--apple-text);
  letter-spacing: -0.5px;
}

/* ============================================
   Main Card with Blue Team Theme
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
  border-left: 4px solid var(--blue-team-primary);
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
  position: relative;
  overflow: hidden;
}

.page-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, var(--blue-team-primary), var(--blue-team-secondary));
  opacity: 0.8;
}

.page-card:hover {
  box-shadow: var(--shadow-card-hover);
  transform: translateY(-2px);
}

.card-header {
  font-size: 18px;
  font-weight: 700;
  color: var(--blue-team-primary);
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
}

/* ============================================
   Form Styling with Blue Team Theme
   ============================================ */
.submit-form {
  max-width: 100%;
  padding: var(--spacing-md);
}

.submit-form :deep(.el-form-item__label) {
  font-weight: 600;
  color: var(--apple-text);
  font-family: var(--font-apple);
}

/* 战队信息显示样式 */
.team-info-display {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-sm) var(--spacing-md);
  background: rgba(52, 152, 219, 0.05);
  border-radius: var(--radius-sm);
  border: 1px solid rgba(52, 152, 219, 0.15);
}

.team-info-display .team-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--blue-team-primary);
}

.submit-form :deep(.el-input__wrapper) {
  border-radius: var(--radius-sm);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
  border: 1px solid var(--apple-border);
}

.submit-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 4px 12px rgba(52, 152, 219, 0.08);
  border-color: var(--blue-team-primary);
}

.submit-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 4px 16px rgba(52, 152, 219, 0.15);
  border-color: var(--blue-team-primary);
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
  box-shadow: 0 4px 12px rgba(52, 152, 219, 0.08);
  border-color: var(--blue-team-primary);
}

.submit-form :deep(.el-textarea__inner:focus) {
  box-shadow: 0 4px 16px rgba(52, 152, 219, 0.15);
  border-color: var(--blue-team-primary);
}

/* ============================================
   Dividers with Blue Theme
   ============================================ */
.submit-form :deep(.el-divider) {
  margin: 30px 0 20px 0;
  border-color: rgba(52, 152, 219, 0.15);
}

.submit-form :deep(.el-divider__text) {
  font-weight: 700;
  color: var(--blue-team-primary);
  font-size: 15px;
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.95) 0%,
    rgba(250, 250, 250, 0.9) 100%);
  padding: 0 var(--spacing-md);
}

/* ============================================
   Upload Components with Blue Theme
   ============================================ */
.submit-form :deep(.el-upload--picture-card) {
  width: 120px;
  height: 120px;
  border-radius: var(--radius-md);
  border: 2px dashed rgba(52, 152, 219, 0.3);
  background: var(--blue-team-light);
  transition: all 0.3s ease;
}

.submit-form :deep(.el-upload--picture-card:hover) {
  border-color: var(--blue-team-primary);
  background: rgba(52, 152, 219, 0.15);
  transform: scale(1.02);
}

.submit-form :deep(.el-upload-list__item) {
  border-radius: var(--radius-md);
  border: 1px solid var(--apple-border);
  overflow: hidden;
  transition: all 0.2s ease;
}

.submit-form :deep(.el-upload-list__item:hover) {
  box-shadow: 0 4px 12px rgba(52, 152, 219, 0.12);
}

.upload-tip {
  color: var(--apple-text-secondary);
  font-size: 12px;
  margin-top: var(--spacing-xs);
  font-family: var(--font-apple);
}

/* ============================================
   Buttons with Blue Team Gradient
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
  background: linear-gradient(135deg, var(--blue-team-primary) 0%, var(--blue-team-secondary) 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(52, 152, 219, 0.25);
  color: #86868b !important;
  font-size: 15px;
  padding: 14px 28px;
}

.submit-form :deep(.el-button--primary span) {
  color: #86868b !important;
}

.submit-form :deep(.el-button--primary:hover) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(52, 152, 219, 0.35);
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

.submit-form :deep(.el-button.is-loading) {
  position: relative;
  overflow: hidden;
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

/* ============================================
   Select Components with Blue Theme
   ============================================ */
.submit-form :deep(.el-select) {
  width: 100%;
}

.submit-form :deep(.el-select .el-input__wrapper) {
  border-radius: var(--radius-sm);
}

.submit-form :deep(.el-select:hover .el-input__wrapper) {
  border-color: var(--blue-team-primary);
}

.submit-form :deep(.el-select .el-input.is-focus .el-input__wrapper) {
  border-color: var(--blue-team-primary);
  box-shadow: 0 4px 16px rgba(52, 152, 219, 0.15);
}

/* ============================================
   Date Picker with Blue Theme
   ============================================ */
.submit-form :deep(.el-date-editor) {
  width: 100%;
}

.submit-form :deep(.el-date-editor .el-input__wrapper) {
  border-radius: var(--radius-sm);
}

.submit-form :deep(.el-date-editor:hover .el-input__wrapper) {
  border-color: var(--blue-team-primary);
}

.submit-form :deep(.el-date-editor .el-input.is-focus .el-input__wrapper) {
  border-color: var(--blue-team-primary);
  box-shadow: 0 4px 16px rgba(52, 152, 219, 0.15);
}

/* ============================================
   Upload Section Styling
   ============================================ */
.submit-form :deep(.el-form-item:has(.el-upload)) {
  padding: var(--spacing-md);
  background: var(--blue-team-light);
  border-radius: var(--radius-md);
  margin-bottom: var(--spacing-lg);
  border: 1px solid rgba(52, 152, 219, 0.15);
}

/* 专门修复上传按钮文字显示 */
.submit-form :deep(.el-form-item:has(.el-upload) .el-button--primary) {
  color: #86868b !important;
}

.submit-form :deep(.el-form-item:has(.el-upload) .el-button--primary span) {
  color: #86868b !important;
}

.submit-form :deep(.el-upload .el-button--primary) {
  color: #86868b !important;
}

.submit-form :deep(.el-upload .el-button--primary span) {
  color: #86868b !important;
}

/* ============================================
   Responsive Design
   ============================================ */
@media (max-width: 768px) {
  .blue-team-submit-page {
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
   🟦 蓝队深色主题 - Blue Team Dark Theme
   ============================================ */

.blue-team-submit-page.theme-blue {
  background: transparent;
}

/* 页面卡片 - 深蓝色渐变 */
.blue-team-submit-page.theme-blue .page-card {
  background: linear-gradient(135deg,
    rgba(13, 26, 45, 0.98) 0%,
    rgba(10, 20, 40, 0.99) 100%) !important;
  border: 1px solid rgba(70, 130, 180, 0.4) !important;
  border-left: 4px solid #00d4ff !important;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.6),
              0 0 20px rgba(70, 130, 180, 0.15) !important;
  backdrop-filter: blur(30px);
  -webkit-backdrop-filter: blur(30px);
}

.blue-team-submit-page.theme-blue .page-card::before {
  background: linear-gradient(90deg, #00d4ff, #4682b4) !important;
  opacity: 1;
}

.blue-team-submit-page.theme-blue .page-card:hover {
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.7),
              0 0 30px rgba(70, 130, 180, 0.2) !important;
  border-color: rgba(0, 212, 255, 0.5) !important;
}

/* 卡片标题 - 青色发光 */
.blue-team-submit-page.theme-blue .card-header {
  color: #00d4ff !important;
  text-shadow: 0 0 15px rgba(0, 212, 255, 0.5);
}

/* 表单样式 */
.blue-team-submit-page.theme-blue .submit-form :deep(.el-form-item__label) {
  color: rgba(255, 255, 255, 0.9) !important;
  font-weight: 600;
}

/* 战队信息显示 - 蓝队主题 */
.blue-team-submit-page.theme-blue .team-info-display {
  background: rgba(30, 30, 30, 0.5);
  border-color: rgba(0, 212, 255, 0.2);
}

.blue-team-submit-page.theme-blue .team-info-display .team-name {
  color: #00d4ff;
  text-shadow: 0 0 10px rgba(0, 212, 255, 0.4);
}

/* 输入框 - 深蓝色背景 */
.blue-team-submit-page.theme-blue .submit-form :deep(.el-input__wrapper) {
  background: linear-gradient(135deg,
    rgba(20, 30, 50, 0.6) 0%,
    rgba(15, 25, 45, 0.7) 100%) !important;
  border: 1px solid rgba(70, 130, 180, 0.3) !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3) !important;
}

.blue-team-submit-page.theme-blue .submit-form :deep(.el-input__wrapper:hover) {
  border-color: rgba(0, 212, 255, 0.5) !important;
  box-shadow: 0 4px 12px rgba(0, 212, 255, 0.15) !important;
}

.blue-team-submit-page.theme-blue .submit-form :deep(.el-input__wrapper.is-focus) {
  border-color: #00d4ff !important;
  box-shadow: 0 4px 16px rgba(0, 212, 255, 0.25) !important;
}

.blue-team-submit-page.theme-blue .submit-form :deep(.el-input__inner) {
  color: rgba(255, 255, 255, 0.95) !important;
}

.blue-team-submit-page.theme-blue .submit-form :deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.4) !important;
}

/* 文本域 */
.blue-team-submit-page.theme-blue .submit-form :deep(.el-textarea__inner) {
  background: linear-gradient(135deg,
    rgba(20, 30, 50, 0.6) 0%,
    rgba(15, 25, 45, 0.7) 100%) !important;
  border: 1px solid rgba(70, 130, 180, 0.3) !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3) !important;
  color: rgba(255, 255, 255, 0.95) !important;
}

.blue-team-submit-page.theme-blue .submit-form :deep(.el-textarea__inner:hover) {
  border-color: rgba(0, 212, 255, 0.5) !important;
  box-shadow: 0 4px 12px rgba(0, 212, 255, 0.15) !important;
}

.blue-team-submit-page.theme-blue .submit-form :deep(.el-textarea__inner:focus) {
  border-color: #00d4ff !important;
  box-shadow: 0 4px 16px rgba(0, 212, 255, 0.25) !important;
}

.blue-team-submit-page.theme-blue .submit-form :deep(.el-textarea__inner::placeholder) {
  color: rgba(255, 255, 255, 0.4) !important;
}

/* 分割线 - 青色 */
.blue-team-submit-page.theme-blue .submit-form :deep(.el-divider) {
  border-color: rgba(0, 212, 255, 0.25) !important;
}

.blue-team-submit-page.theme-blue .submit-form :deep(.el-divider__text) {
  color: #00d4ff !important;
  background: linear-gradient(135deg,
    rgba(13, 26, 45, 0.98) 0%,
    rgba(10, 20, 40, 0.99) 100%) !important;
  font-weight: 700;
  text-shadow: 0 0 10px rgba(0, 212, 255, 0.4);
}

/* 上传组件 - 深蓝色 */
.blue-team-submit-page.theme-blue .submit-form :deep(.el-upload--picture-card) {
  background: linear-gradient(135deg,
    rgba(20, 30, 50, 0.5) 0%,
    rgba(15, 25, 45, 0.6) 100%) !important;
  border: 2px dashed rgba(0, 212, 255, 0.4) !important;
}

.blue-team-submit-page.theme-blue .submit-form :deep(.el-upload--picture-card:hover) {
  border-color: #00d4ff !important;
  background: linear-gradient(135deg,
    rgba(20, 30, 50, 0.7) 0%,
    rgba(15, 25, 45, 0.8) 100%) !important;
  box-shadow: 0 4px 16px rgba(0, 212, 255, 0.2);
}

.blue-team-submit-page.theme-blue .submit-form :deep(.el-upload--picture-card .el-icon) {
  color: #00d4ff !important;
}

.blue-team-submit-page.theme-blue .submit-form :deep(.el-upload-list__item) {
  background: rgba(20, 30, 50, 0.6) !important;
  border: 1px solid rgba(70, 130, 180, 0.3) !important;
}

.blue-team-submit-page.theme-blue .submit-form :deep(.el-upload-list__item:hover) {
  border-color: rgba(0, 212, 255, 0.5) !important;
  box-shadow: 0 4px 12px rgba(0, 212, 255, 0.15);
}

.blue-team-submit-page.theme-blue .upload-tip {
  color: rgba(255, 255, 255, 0.6) !important;
}

.blue-team-submit-page.theme-blue .submit-form :deep(.el-upload__tip) {
  color: rgba(255, 255, 255, 0.6) !important;
}

/* 按钮 - 青色渐变 */
.blue-team-submit-page.theme-blue .submit-form :deep(.el-button--primary) {
  background: linear-gradient(135deg,
    rgba(0, 212, 255, 0.9) 0%,
    rgba(70, 130, 180, 0.9) 100%) !important;
  border: 1px solid rgba(0, 212, 255, 0.6) !important;
  box-shadow: 0 4px 16px rgba(0, 212, 255, 0.3) !important;
  color: #0a1428 !important;
  font-weight: 700;
}

.blue-team-submit-page.theme-blue .submit-form :deep(.el-button--primary span) {
  color: #0a1428 !important;
}

.blue-team-submit-page.theme-blue .submit-form :deep(.el-button--primary:hover) {
  box-shadow: 0 6px 24px rgba(0, 212, 255, 0.4) !important;
  transform: translateY(-2px);
  border-color: #00d4ff !important;
}

.blue-team-submit-page.theme-blue .submit-form :deep(.el-button--primary:active) {
  transform: translateY(0);
}

/* 默认按钮 */
.blue-team-submit-page.theme-blue .submit-form :deep(.el-button--default) {
  background: linear-gradient(135deg,
    rgba(30, 40, 60, 0.8) 0%,
    rgba(20, 30, 50, 0.9) 100%) !important;
  border: 1px solid rgba(70, 130, 180, 0.35) !important;
  color: rgba(255, 255, 255, 0.9) !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

.blue-team-submit-page.theme-blue .submit-form :deep(.el-button--default:hover) {
  border-color: rgba(0, 212, 255, 0.5) !important;
  box-shadow: 0 4px 12px rgba(0, 212, 255, 0.15);
  transform: translateY(-2px);
}

/* ============================================
   蓝队原生select深色样式
   ============================================ */
.theme-blue .custom-select.blue-team-select {
  width: 100%;
  background: rgba(30, 30, 30, 0.6);
  border: 0.5px solid rgba(0, 212, 255, 0.2);
  color: rgba(255, 255, 255, 0.9);
  padding: 12px 16px;
  border-radius: var(--radius-sm);
  font-family: var(--font-apple);
  font-size: 14px;
  transition: all 0.3s ease;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

.theme-blue .custom-select.blue-team-select:hover {
  background: rgba(30, 30, 30, 0.7);
  border-color: rgba(0, 212, 255, 0.4);
  box-shadow: 0 4px 12px rgba(0, 212, 255, 0.2);
}

.theme-blue .custom-select.blue-team-select:focus {
  outline: none;
  background: rgba(30, 30, 30, 0.8);
  border-color: rgba(0, 212, 255, 0.6);
  box-shadow: 0 4px 16px rgba(0, 212, 255, 0.3);
}

.theme-blue .custom-select.blue-team-select option {
  background: rgba(20, 20, 20, 0.95);
  color: rgba(255, 255, 255, 0.9);
  padding: 8px 12px;
}

.theme-blue .custom-select.blue-team-select option:hover {
  background: rgba(0, 212, 255, 0.15);
}

/* Select 下拉框（Element Plus） */
.blue-team-submit-page.theme-blue .submit-form :deep(.el-select .el-input__wrapper) {
  background: linear-gradient(135deg,
    rgba(20, 30, 50, 0.6) 0%,
    rgba(15, 25, 45, 0.7) 100%) !important;
  border: 1px solid rgba(70, 130, 180, 0.3) !important;
}

.blue-team-submit-page.theme-blue .submit-form :deep(.el-select:hover .el-input__wrapper) {
  border-color: rgba(0, 212, 255, 0.5) !important;
}

.blue-team-submit-page.theme-blue .submit-form :deep(.el-select .el-input.is-focus .el-input__wrapper) {
  border-color: #00d4ff !important;
  box-shadow: 0 4px 16px rgba(0, 212, 255, 0.25) !important;
}

.blue-team-submit-page.theme-blue .submit-form :deep(.el-select__placeholder) {
  color: rgba(255, 255, 255, 0.4) !important;
}

/* 日期选择器 */
.blue-team-submit-page.theme-blue .submit-form :deep(.el-date-editor .el-input__wrapper) {
  background: linear-gradient(135deg,
    rgba(20, 30, 50, 0.6) 0%,
    rgba(15, 25, 45, 0.7) 100%) !important;
  border: 1px solid rgba(70, 130, 180, 0.3) !important;
}

.blue-team-submit-page.theme-blue .submit-form :deep(.el-date-editor:hover .el-input__wrapper) {
  border-color: rgba(0, 212, 255, 0.5) !important;
}

.blue-team-submit-page.theme-blue .submit-form :deep(.el-date-editor .el-input.is-focus .el-input__wrapper) {
  border-color: #00d4ff !important;
  box-shadow: 0 4px 16px rgba(0, 212, 255, 0.25) !important;
}

.blue-team-submit-page.theme-blue .submit-form :deep(.el-date-editor .el-input__inner) {
  color: rgba(255, 255, 255, 0.95) !important;
}

.blue-team-submit-page.theme-blue .submit-form :deep(.el-date-editor .el-input__prefix) {
  color: rgba(255, 255, 255, 0.6) !important;
}

/* 上传区域背景 */
.blue-team-submit-page.theme-blue .submit-form :deep(.el-form-item:has(.el-upload)) {
  background: linear-gradient(135deg,
    rgba(20, 30, 50, 0.4) 0%,
    rgba(15, 25, 45, 0.5) 100%) !important;
  border: 1px solid rgba(70, 130, 180, 0.25) !important;
  border-radius: 16px;
}

/* 上传按钮文字颜色修正 */
.blue-team-submit-page.theme-blue .submit-form :deep(.el-form-item:has(.el-upload) .el-button--primary) {
  color: #0a1428 !important;
}

.blue-team-submit-page.theme-blue .submit-form :deep(.el-form-item:has(.el-upload) .el-button--primary span) {
  color: #0a1428 !important;
}

.blue-team-submit-page.theme-blue .submit-form :deep(.el-upload .el-button--primary) {
  color: #0a1428 !important;
}

.blue-team-submit-page.theme-blue .submit-form :deep(.el-upload .el-button--primary span) {
  color: #0a1428 !important;
}

/* Loading状态 */
.blue-team-submit-page.theme-blue .submit-form :deep(.el-button.is-loading) {
  opacity: 0.85;
}

.blue-team-submit-page.theme-blue .submit-form :deep(.el-button.is-loading::before) {
  background: linear-gradient(90deg,
    transparent,
    rgba(255, 255, 255, 0.3),
    transparent);
}

/* ============================================
   全局组件样式 - Global Components (body.blue-theme-active)
   ============================================ */

/* Select 下拉面板 */
body.blue-theme-active .el-select-dropdown {
  background: linear-gradient(135deg,
    rgba(13, 26, 45, 0.98) 0%,
    rgba(10, 20, 40, 0.99) 100%) !important;
  border: 1px solid rgba(70, 130, 180, 0.4) !important;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.6),
              0 0 20px rgba(70, 130, 180, 0.15) !important;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
}

body.blue-theme-active .el-select-dropdown__item {
  color: rgba(255, 255, 255, 0.9) !important;
}

body.blue-theme-active .el-select-dropdown__item:hover {
  background: rgba(0, 212, 255, 0.15) !important;
  color: #00d4ff !important;
}

body.blue-theme-active .el-select-dropdown__item.selected {
  background: rgba(0, 212, 255, 0.2) !important;
  color: #00d4ff !important;
  font-weight: 600;
}

/* 日期选择器面板 */
body.blue-theme-active .el-picker-panel {
  background: linear-gradient(135deg,
    rgba(13, 26, 45, 0.98) 0%,
    rgba(10, 20, 40, 0.99) 100%) !important;
  border: 1px solid rgba(70, 130, 180, 0.4) !important;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.6) !important;
  backdrop-filter: blur(20px);
}

body.blue-theme-active .el-picker-panel__body {
  color: rgba(255, 255, 255, 0.9) !important;
}

body.blue-theme-active .el-date-picker__header-label {
  color: rgba(255, 255, 255, 0.9) !important;
}

body.blue-theme-active .el-date-table th {
  color: rgba(255, 255, 255, 0.6) !important;
}

body.blue-theme-active .el-date-table td {
  color: rgba(255, 255, 255, 0.8) !important;
}

body.blue-theme-active .el-date-table td.available:hover {
  background: rgba(0, 212, 255, 0.15) !important;
  color: #00d4ff !important;
}

body.blue-theme-active .el-date-table td.current {
  background: rgba(0, 212, 255, 0.25) !important;
  color: #00d4ff !important;
}

body.blue-theme-active .el-time-panel {
  background: linear-gradient(135deg,
    rgba(13, 26, 45, 0.98) 0%,
    rgba(10, 20, 40, 0.99) 100%) !important;
  border: 1px solid rgba(70, 130, 180, 0.4) !important;
}

body.blue-theme-active .el-time-panel__content {
  color: rgba(255, 255, 255, 0.9) !important;
}

/* Message 消息提示 */
body.blue-theme-active .el-message {
  background: linear-gradient(135deg,
    rgba(13, 26, 45, 0.98) 0%,
    rgba(10, 20, 40, 0.99) 100%) !important;
  border: 1px solid rgba(70, 130, 180, 0.4) !important;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.6) !important;
  backdrop-filter: blur(20px);
}

body.blue-theme-active .el-message__content {
  color: rgba(255, 255, 255, 0.95) !important;
}

body.blue-theme-active .el-message--success .el-message__icon {
  color: #52c41a !important;
}

body.blue-theme-active .el-message--error .el-message__icon {
  color: #ff4d4f !important;
}

/* Overlay */
body.blue-theme-active .el-overlay {
  background-color: rgba(0, 0, 0, 0.7) !important;
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
}
</style>