<template>
  <div class="red-team-submit-page" :class="themeClass">
    <!-- 动态光晕背景层（仅红队可见） -->
    <div v-if="currentRole === 'red'" class="dynamic-glow-layer">
      <div class="glow-spot glow-1"></div>
      <div class="glow-spot glow-2"></div>
      <div class="glow-spot glow-3"></div>
    </div>

    <el-card class="page-card">
      <template #header>
        <div class="card-header">
          <span>🟥 攻击战报提交</span>
        </div>
      </template>

      <el-form ref="submitFormRef" :model="submitForm" :rules="submitRules" label-width="140px" class="submit-form">

        <!-- 基础信息 -->
        <el-divider content-position="left">基础信息</el-divider>

        <el-form-item label="所属演练" prop="drillId">
          <!-- 红队用户：使用原生select（样式可控） -->
          <select v-if="currentRole === 'red'" v-model="submitForm.drillId" class="custom-select red-team-select" required>
            <option value="" disabled>请选择演练</option>
            <option v-for="drill in drillList" :key="drill.id" :value="drill.id">
              {{ drill.name }}
            </option>
          </select>

          <!-- Admin用户：保持el-select -->
          <el-select v-else v-model="submitForm.drillId" placeholder="请选择演练" style="width: 100%">
            <el-option v-for="drill in drillList" :key="drill.id" :label="drill.name" :value="drill.id">
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="所属战队">
          <div v-if="loadingTeam" style="color: rgba(255, 255, 255, 0.6);">
            正在加载战队信息...
          </div>
          <div v-else-if="userTeam" class="team-info-display">
            <span class="team-name">{{ userTeam.name }}</span>
            <el-tag :type="userTeam.teamType === 'red' ? 'danger' : 'primary'" size="small">
              {{ userTeam.teamType === 'red' ? '红队' : '蓝队' }}
            </el-tag>
          </div>
          <el-alert v-else type="info" :closable="false" show-icon>
            <template #title>
              您尚未加入战队，将以个人身份提交成果
            </template>
          </el-alert>
        </el-form-item>

        <el-form-item label="攻击目标" prop="targetInfo">
          <el-input v-model="submitForm.targetInfo" type="textarea" :rows="2" placeholder="请描述攻击目标（IP、域名、系统等）">
          </el-input>
        </el-form-item>

        <!-- 攻击路径与过程 -->
        <el-divider content-position="left">攻击路径与过程</el-divider>

        <el-form-item label="攻击路径说明" prop="attackPath">
          <el-input v-model="submitForm.attackPath" type="textarea" :rows="4" placeholder="请详细描述攻击路径和步骤">
          </el-input>
        </el-form-item>

        <el-form-item label="使用工具与方法" prop="toolsAndMethods">
          <el-input v-model="submitForm.toolsAndMethods" type="textarea" :rows="3" placeholder="请列出使用的攻击工具、技术和方法">
          </el-input>
        </el-form-item>

        <!-- 漏洞信息 -->
        <el-divider content-position="left">漏洞发现与利用</el-divider>

        <el-form-item label="发现漏洞" prop="vulnerabilities">
          <el-input v-model="submitForm.vulnerabilities" type="textarea" :rows="4" placeholder="请详细描述发现的漏洞类型、位置、原理等">
          </el-input>
        </el-form-item>

        <el-form-item label="利用过程" prop="exploitProcess">
          <el-input v-model="submitForm.exploitProcess" type="textarea" :rows="4" placeholder="请描述漏洞利用的具体过程和步骤">
          </el-input>
        </el-form-item>

        <el-form-item label="CVSS评分" prop="cvssScore">
          <!-- 红队用户：使用单选按钮组（样式可控） -->
          <el-radio-group v-if="currentRole === 'red'" v-model="submitForm.cvssScore" class="red-team-radio-group">
            <el-radio value="low">低危 (0.1-3.9)</el-radio>
            <el-radio value="medium">中危 (4.0-6.9)</el-radio>
            <el-radio value="high">高危 (7.0-8.9)</el-radio>
            <el-radio value="critical">严重 (9.0-10.0)</el-radio>
          </el-radio-group>

          <!-- Admin用户：保持el-select -->
          <el-select v-else v-model="submitForm.cvssScore" placeholder="请选择CVSS评分">
            <el-option label="低危 (0.1-3.9)" value="low"></el-option>
            <el-option label="中危 (4.0-6.9)" value="medium"></el-option>
            <el-option label="高危 (7.0-8.9)" value="high"></el-option>
            <el-option label="严重 (9.0-10.0)" value="critical"></el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="影响评估" prop="impactAssessment">
          <el-input v-model="submitForm.impactAssessment" type="textarea" :rows="3" placeholder="请评估攻击造成的影响范围和严重程度">
          </el-input>
        </el-form-item>

        <!-- 证明材料上传 -->
        <el-divider content-position="left">证明材料</el-divider>

        <el-form-item label="漏洞截图">
          <el-upload ref="screenshotUpload" :file-list="screenshotFileList" :auto-upload="false" multiple
            accept="image/*" list-type="picture-card" :on-change="handleScreenshotChange">
            <el-icon>+</el-icon>
          </el-upload>
          <div class="upload-tip">支持上传漏洞截图、攻击链图等</div>
        </el-form-item>

        <el-form-item label="POC视频">
          <el-upload ref="pocVideoUpload" :file-list="pocVideoFileList" :auto-upload="false" accept="video/*"
            :on-change="handlePocVideoChange">
            <el-button type="primary">选择POC视频</el-button>
            <template #tip>
              <div class="el-upload__tip">支持上传攻击演示视频</div>
            </template>
          </el-upload>
        </el-form-item>

        <el-form-item label="日志与流量包">
          <el-upload ref="logUpload" :file-list="logFileList" :auto-upload="false" multiple
            :on-change="handleLogChange">
            <el-button type="primary">选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">支持上传攻击日志、网络流量包等证明文件</div>
            </template>
          </el-upload>
        </el-form-item>

        <el-form-item label="攻击时间区间" prop="attackTimeRange">
          <el-date-picker
            v-model="submitForm.attackTimeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="攻击开始时间"
            end-placeholder="攻击结束时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DDTHH:mm:ss"
            :default-time="[new Date(2000, 1, 1, 0, 0, 0), new Date(2000, 1, 1, 23, 59, 59)]"
            style="width: 100%">
          </el-date-picker>
          <div class="upload-tip">请标注攻击操作的时间区间，裁判/管理员将根据此时间段查找录像</div>
        </el-form-item>

        <!-- 提交按钮 -->
        <el-form-item>
          <el-button type="danger" @click="handleSubmit" :loading="submitting" size="large">
            🟥 提交红队攻击成果
          </el-button>
          <el-button @click="resetForm" size="large">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import axios from '@/api/axios'
import { getActiveRanges, getAllRanges } from '@/api/range'
import { getUserRole, getUserId, getUsername } from '@/utils/auth'
import { getTeamByUserId } from '@/api/team'

// 获取当前用户角色
const currentRole = computed(() => getUserRole())

// 主题类名（根据角色动态切换）
const themeClass = computed(() => {
  return currentRole.value === 'red' ? 'theme-red' : 'theme-default'
})

// 响应式数据
const submitFormRef = ref()
const submitting = ref(false)
const drillList = ref([])
const screenshotFileList = ref([])
const pocVideoFileList = ref([])
const logFileList = ref([])

// 用户战队信息
const userTeam = ref(null)
const loadingTeam = ref(false)
const teamError = ref('')

// 表单数据
const submitForm = reactive({
  drillId: '',
  targetInfo: '',
  attackPath: '',
  toolsAndMethods: '',
  vulnerabilities: '',
  exploitProcess: '',
  cvssScore: '',
  impactAssessment: '',
  attackTimeRange: [] // 攻击时间区间
})

// 表单验证规则
const submitRules = {
  drillId: [{ required: true, message: '请选择所属演练', trigger: 'change' }],
  targetInfo: [{ required: true, message: '请输入攻击目标', trigger: 'blur' }],
  attackPath: [{ required: true, message: '请描述攻击路径', trigger: 'blur' }],
  vulnerabilities: [{ required: true, message: '请描述发现的漏洞', trigger: 'blur' }],
  exploitProcess: [{ required: true, message: '请描述利用过程', trigger: 'blur' }],
  cvssScore: [{ required: true, message: '请选择CVSS评分', trigger: 'change' }],
  impactAssessment: [{ required: true, message: '请评估影响', trigger: 'blur' }],
  attackTimeRange: [{ required: true, message: '请选择攻击时间区间', trigger: 'change' }]
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
const handleScreenshotChange = (file, fileList) => {
  screenshotFileList.value = fileList
}

const handlePocVideoChange = (file, fileList) => {
  pocVideoFileList.value = fileList
}

const handleLogChange = (file, fileList) => {
  logFileList.value = fileList
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

    // 安全的 rangeId 类型转换，防止 NaN
    const rangeId = Number(submitForm.drillId)
    if (isNaN(rangeId) || rangeId <= 0) {
      ElMessage.error('请选择有效的演练')
      submitting.value = false
      return
    }

    // 验证并处理攻击时间区间
    let timeRange = null
    if (submitForm.attackTimeRange && submitForm.attackTimeRange.length === 2) {
      const startTime = submitForm.attackTimeRange[0]
      const endTime = submitForm.attackTimeRange[1]

      if (!startTime || !endTime) {
        ElMessage.error('请完整选择攻击时间区间（包括时间）')
        submitting.value = false
        return
      }

      timeRange = `${startTime},${endTime}`
    }

    // 调试日志：打印提交的数据

    const formData = new FormData()
    // 匹配后端Controller期望的参数名
    formData.append('rangeId', rangeId)
    formData.append('teamName', teamName)
    formData.append('targetName', submitForm.targetInfo) // 使用targetInfo作为targetName
    formData.append('description', `攻击路径: ${submitForm.attackPath}\n利用过程: ${submitForm.exploitProcess}\n发现漏洞: ${submitForm.vulnerabilities}\nCVSS评分: ${submitForm.cvssScore}\n影响评估: ${submitForm.impactAssessment}`)
    formData.append('attackMethod', submitForm.toolsAndMethods || '红队攻击')

    // 添加攻击时间区间
    if (timeRange) {
      formData.append('recordingTimeRange', timeRange)
    }

    // 添加各类文件
    screenshotFileList.value.forEach(file => {
      if (file.raw) formData.append('screenshots', file.raw)
    })

    pocVideoFileList.value.forEach(file => {
      if (file.raw) formData.append('pocVideos', file.raw)
    })

    logFileList.value.forEach(file => {
      if (file.raw) formData.append('logFiles', file.raw)
    })


    try {
      const response = await axios.post('achievements/submit', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })


      if (response.success) {
        ElMessage.success('红队攻击成果提交成功！')
        resetForm()
      } else {
        ElMessage.error(response.message || '提交失败')
      }
    } catch (apiError) {
      // 记录详细的错误信息（使用JSON.stringify查看完整响应）

      // 显示具体错误信息并停止执行
      if (apiError.response?.data?.message) {
        ElMessage.error(`❌ 提交失败: ${apiError.response.data.message}`)
        submitting.value = false
        return
      } else if (apiError.response?.status === 400) {
        ElMessage.error('❌ 请求参数错误，请检查表单填写是否完整正确')
        submitting.value = false
        return
      } else if (apiError.response?.status) {
        ElMessage.error(`❌ 提交失败: HTTP ${apiError.response.status}`)
        submitting.value = false
        return
      } else if (!apiError.response) {
        ElMessage.error('❌ 无法连接到服务器，请检查网络连接和后端服务状态')
        submitting.value = false
        return
      }

      // 如果没有具体错误信息，显示通用错误
      ElMessage.error('❌ 提交失败，请稍后重试')
      submitting.value = false
    }

  } catch (error) {
    ElMessage.error('请填写必填字段')
  } finally {
    submitting.value = false
  }
}

// 重置表单
const resetForm = () => {
  submitFormRef.value.resetFields()
  screenshotFileList.value = []
  pocVideoFileList.value = []
  logFileList.value = []
}

onMounted(() => {
  fetchDrills()
  fetchUserTeam()
})
</script>

<style scoped>
/* ============================================
   Apple Elegant White Style - 红队主题
   Red Team Submit Page
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
  --red-team-primary: #e74c3c;
  --red-team-secondary: #c0392b;
  --red-team-light: rgba(231, 76, 60, 0.1);
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

/* 战队信息显示样式 */
.team-info-display {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-sm) var(--spacing-md);
  background: rgba(231, 76, 60, 0.05);
  border-radius: var(--radius-sm);
  border: 1px solid rgba(231, 76, 60, 0.15);
}

.team-info-display .team-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--red-team-primary);
}

.theme-red .team-info-display {
  background: rgba(30, 30, 30, 0.5);
  border-color: rgba(255, 59, 48, 0.2);
}

.theme-red .team-info-display .team-name {
  color: #ffffff;
  text-shadow: 0 0 10px rgba(255, 59, 48, 0.4);
}

.red-team-submit-page {
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
  filter: drop-shadow(0 4px 12px rgba(231, 76, 60, 0.15));
}

.header-title {
  margin: 0;
  font-size: 32px;
  font-weight: 700;
  color: var(--apple-text);
  letter-spacing: -0.5px;
}

/* ============================================
   Main Card with Red Team Theme
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
  border-left: 4px solid var(--red-team-primary);
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
  background: linear-gradient(90deg, var(--red-team-primary), var(--red-team-secondary));
  opacity: 0.8;
}

.page-card:hover {
  box-shadow: var(--shadow-card-hover);
  transform: translateY(-2px);
}

.card-header {
  font-size: 18px;
  font-weight: 700;
  color: var(--red-team-primary);
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
}

/* ============================================
   Form Styling with Red Team Theme
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

.theme-default .submit-form :deep(.el-input__wrapper) {
  border-radius: var(--radius-sm);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
  border: 1px solid var(--apple-border);
}

.theme-default .submit-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 4px 12px rgba(231, 76, 60, 0.08);
  border-color: var(--red-team-primary);
}

.theme-default .submit-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 4px 16px rgba(231, 76, 60, 0.15);
  border-color: var(--red-team-primary);
}

.theme-default .submit-form :deep(.el-textarea__inner) {
  border-radius: var(--radius-sm);
  border: 1px solid var(--apple-border);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
  font-family: var(--font-apple);
  line-height: 1.6;
}

.theme-default .submit-form :deep(.el-textarea__inner:hover) {
  box-shadow: 0 4px 12px rgba(231, 76, 60, 0.08);
  border-color: var(--red-team-primary);
}

.theme-default .submit-form :deep(.el-textarea__inner:focus) {
  box-shadow: 0 4px 16px rgba(231, 76, 60, 0.15);
  border-color: var(--red-team-primary);
}

/* ============================================
   Dividers with Red Theme
   ============================================ */
.submit-form :deep(.el-divider) {
  margin: 30px 0 20px 0;
  border-color: rgba(231, 76, 60, 0.15);
}

.submit-form :deep(.el-divider__text) {
  font-weight: 700;
  color: var(--red-team-primary);
  font-size: 15px;
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.95) 0%,
    rgba(250, 250, 250, 0.9) 100%);
  padding: 0 var(--spacing-md);
}

/* ============================================
   Upload Components with Red Theme
   ============================================ */
.submit-form :deep(.el-upload--picture-card) {
  width: 120px;
  height: 120px;
  border-radius: var(--radius-md);
  border: 2px dashed rgba(231, 76, 60, 0.3);
  background: var(--red-team-light);
  transition: all 0.3s ease;
}

.submit-form :deep(.el-upload--picture-card:hover) {
  border-color: var(--red-team-primary);
  background: rgba(231, 76, 60, 0.15);
  transform: scale(1.02);
}

.submit-form :deep(.el-upload-list__item) {
  border-radius: var(--radius-md);
  border: 1px solid var(--apple-border);
  overflow: hidden;
  transition: all 0.2s ease;
}

.submit-form :deep(.el-upload-list__item:hover) {
  box-shadow: 0 4px 12px rgba(231, 76, 60, 0.12);
}

.upload-tip {
  color: var(--apple-text-secondary);
  font-size: 12px;
  margin-top: var(--spacing-xs);
  font-family: var(--font-apple);
}

/* ============================================
   Buttons with Red Team Gradient
   ============================================ */
.submit-form :deep(.el-button) {
  border-radius: var(--radius-sm);
  font-weight: 600;
  font-family: var(--font-apple);
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
  padding: 12px 24px;
  font-size: 14px;
}

.submit-form :deep(.el-button--danger) {
  background: linear-gradient(135deg, var(--red-team-primary) 0%, var(--red-team-secondary) 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(231, 76, 60, 0.25);
  color: #86868b !important;
  font-size: 15px;
  padding: 14px 28px;
}

.submit-form :deep(.el-button--danger span) {
  color: #86868b !important;
}

.submit-form :deep(.el-button--danger:hover) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(231, 76, 60, 0.35);
}

.submit-form :deep(.el-button--danger:active) {
  transform: translateY(0);
}

.submit-form :deep(.el-button--primary) {
  background: linear-gradient(135deg, var(--red-team-primary) 0%, var(--red-team-secondary) 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(231, 76, 60, 0.25);
  color: white !important;
}

.submit-form :deep(.el-button--primary:hover) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(231, 76, 60, 0.35);
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
   Select Components with Red Theme
   ============================================ */
.theme-default .submit-form :deep(.el-select) {
  width: 100%;
}

.theme-default .submit-form :deep(.el-select .el-input__wrapper) {
  border-radius: var(--radius-sm);
}

.theme-default .submit-form :deep(.el-select:hover .el-input__wrapper) {
  border-color: var(--red-team-primary);
}

.theme-default .submit-form :deep(.el-select .el-input.is-focus .el-input__wrapper) {
  border-color: var(--red-team-primary);
  box-shadow: 0 4px 16px rgba(231, 76, 60, 0.15);
}

/* ============================================
   Date Picker with Red Theme
   ============================================ */
.theme-default .submit-form :deep(.el-date-editor) {
  width: 100%;
}

.theme-default .submit-form :deep(.el-date-editor .el-input__wrapper) {
  border-radius: var(--radius-sm);
}

.theme-default .submit-form :deep(.el-date-editor:hover .el-input__wrapper) {
  border-color: var(--red-team-primary);
}

/* ============================================
   Upload Section Styling
   ============================================ */
.submit-form :deep(.el-form-item:has(.el-upload)) {
  padding: var(--spacing-md);
  background: var(--red-team-light);
  border-radius: var(--radius-md);
  margin-bottom: var(--spacing-lg);
  border: 1px solid rgba(231, 76, 60, 0.15);
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
  .red-team-submit-page {
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
   🔴 红队深色主题 - Red Team Dark Theme
   ============================================ */

.theme-red.red-team-submit-page {
  background: linear-gradient(135deg, #0a0a0a 0%, #1a0d0d 50%, #0f0f0f 100%);
  min-height: 100vh;
  position: relative;
  overflow: hidden;
}

/* 动态光晕背景层 */
.theme-red .dynamic-glow-layer {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
}

.theme-red .glow-spot {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.3;
}

.theme-red .glow-1 {
  top: 15%;
  left: 25%;
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(255, 59, 48, 0.2) 0%, transparent 70%);
  animation: glow-breath-1 8s ease-in-out infinite;
}

.theme-red .glow-2 {
  bottom: 20%;
  right: 15%;
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, rgba(204, 0, 0, 0.15) 0%, transparent 70%);
  animation: glow-breath-2 10s ease-in-out infinite;
  animation-delay: 2s;
}

.theme-red .glow-3 {
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 350px;
  height: 350px;
  background: radial-gradient(circle, rgba(255, 59, 48, 0.12) 0%, transparent 70%);
  animation: glow-breath-3 6s ease-in-out infinite;
  animation-delay: 1s;
}

@keyframes glow-breath-1 {
  0%, 100% {
    opacity: 0.25;
    transform: scale(1);
  }
  50% {
    opacity: 0.4;
    transform: scale(1.15);
  }
}

@keyframes glow-breath-2 {
  0%, 100% {
    opacity: 0.2;
    transform: scale(1);
  }
  50% {
    opacity: 0.35;
    transform: scale(1.2);
  }
}

@keyframes glow-breath-3 {
  0%, 100% {
    opacity: 0.18;
    transform: translate(-50%, -50%) scale(1);
  }
  50% {
    opacity: 0.32;
    transform: translate(-50%, -50%) scale(1.25);
  }
}

/* 主卡片 - 深色毛玻璃 */
.theme-red .page-card {
  background: rgba(20, 20, 20, 0.6);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 0.5px solid rgba(255, 59, 48, 0.2);
  border-left: 4px solid #ff3b30;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5), inset 0 0 0 0.5px rgba(255, 255, 255, 0.05);
  position: relative;
  z-index: 1;
}

.theme-red .page-card::before {
  background: linear-gradient(90deg, #ff3b30, #ff6b59);
  opacity: 1;
}

.theme-red .page-card:hover {
  box-shadow: 0 12px 48px rgba(255, 59, 48, 0.3), inset 0 0 0 0.5px rgba(255, 255, 255, 0.05);
  border-color: rgba(255, 59, 48, 0.3);
}

/* 卡片头部 */
.theme-red .page-card :deep(.el-card__header) {
  background: rgba(30, 30, 30, 0.5);
  border-bottom: 1px solid rgba(255, 59, 48, 0.2);
  color: #ffffff;
}

.theme-red .card-header {
  color: #ffffff;
  text-shadow: 0 0 20px rgba(255, 59, 48, 0.4);
}

.theme-red .page-card :deep(.el-card__body) {
  background: transparent;
}

/* 表单样式 */
.theme-red .submit-form :deep(.el-form-item__label) {
  color: rgba(255, 255, 255, 0.9);
  font-weight: 600;
}

.theme-red .submit-form :deep(.el-input__wrapper) {
  background: rgba(30, 30, 30, 0.6);
  border: 0.5px solid rgba(255, 59, 48, 0.2);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

.theme-red .submit-form :deep(.el-input__wrapper:hover) {
  background: rgba(30, 30, 30, 0.7);
  border-color: rgba(255, 59, 48, 0.4);
  box-shadow: 0 4px 12px rgba(255, 59, 48, 0.2);
}

.theme-red .submit-form :deep(.el-input__wrapper.is-focus) {
  background: rgba(30, 30, 30, 0.8);
  border-color: rgba(255, 59, 48, 0.6);
  box-shadow: 0 4px 16px rgba(255, 59, 48, 0.3);
}

.theme-red .submit-form :deep(.el-input__inner) {
  color: rgba(255, 255, 255, 0.9);
}

.theme-red .submit-form :deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.4);
}

/* 文本域样式 */
.theme-red .submit-form :deep(.el-textarea__inner) {
  background: rgba(30, 30, 30, 0.6);
  border: 0.5px solid rgba(255, 59, 48, 0.2);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  color: rgba(255, 255, 255, 0.9);
}

.theme-red .submit-form :deep(.el-textarea__inner::placeholder) {
  color: rgba(255, 255, 255, 0.4);
}

.theme-red .submit-form :deep(.el-textarea__inner:hover) {
  background: rgba(30, 30, 30, 0.7);
  border-color: rgba(255, 59, 48, 0.4);
  box-shadow: 0 4px 12px rgba(255, 59, 48, 0.2);
}

.theme-red .submit-form :deep(.el-textarea__inner:focus) {
  background: rgba(30, 30, 30, 0.8);
  border-color: rgba(255, 59, 48, 0.6);
  box-shadow: 0 4px 16px rgba(255, 59, 48, 0.3);
}

/* 分隔线 */
.theme-red .submit-form :deep(.el-divider) {
  border-color: rgba(255, 59, 48, 0.3);
}

.theme-red .submit-form :deep(.el-divider__text) {
  color: #ffffff;
  background: transparent;
  font-weight: 700;
  text-shadow: 0 0 15px rgba(255, 59, 48, 0.5);
}

/* ============================================
   选择器 - 地毯式修复（覆盖所有可能的样式点）
   ============================================ */

/* 1. 背景色修复 - 覆盖所有包装器 */
.theme-red .submit-form :deep(.el-select .el-input__wrapper),
.theme-red .submit-form :deep(.el-select .el-input),
.theme-red .submit-form :deep(.el-select) {
  background: rgba(30, 30, 30, 0.6) !important;
  border-color: rgba(255, 59, 48, 0.2) !important;
}

.theme-red .submit-form :deep(.el-select:hover .el-input__wrapper),
.theme-red .submit-form :deep(.el-select:hover .el-input) {
  background: rgba(30, 30, 30, 0.7) !important;
  border-color: rgba(255, 59, 48, 0.4) !important;
}

.theme-red .submit-form :deep(.el-select .el-input.is-focus .el-input__wrapper),
.theme-red .submit-form :deep(.el-select .el-input.is-focus) {
  background: rgba(30, 30, 30, 0.8) !important;
  border-color: rgba(255, 59, 48, 0.6) !important;
  box-shadow: 0 4px 16px rgba(255, 59, 48, 0.3) !important;
}

/* 2. 文字颜色修复 - 覆盖所有文字显示元素 */
/* 直接针对原生 input 元素（最关键！） */
.theme-red .submit-form :deep(.el-select input) {
  color: rgba(255, 255, 255, 0.9) !important;
  background: transparent !important;
}

/* 针对 Element Plus 内部类 */
.theme-red .submit-form :deep(.el-select .el-input__inner) {
  color: rgba(255, 255, 255, 0.9) !important;
  background: transparent !important;
}

.theme-red .submit-form :deep(.el-select__input) {
  color: rgba(255, 255, 255, 0.9) !important;
}

.theme-red .submit-form :deep(.el-select__selected-item) {
  color: rgba(255, 255, 255, 0.9) !important;
}

.theme-red .submit-form :deep(.el-select__selection) {
  color: rgba(255, 255, 255, 0.9) !important;
}

.theme-red .submit-form :deep(.el-select .el-select__tags-text) {
  color: rgba(255, 255, 255, 0.9) !important;
}

/* 3. 占位符颜色 */
.theme-red .submit-form :deep(.el-select__placeholder),
.theme-red .submit-form :deep(.el-select input::placeholder) {
  color: rgba(255, 255, 255, 0.4) !important;
}

/* 4. 选择器图标颜色 */
.theme-red .submit-form :deep(.el-select .el-input__suffix),
.theme-red .submit-form :deep(.el-select .el-input__suffix .el-icon) {
  color: rgba(255, 255, 255, 0.6) !important;
}

.theme-red .submit-form :deep(.el-select-dropdown) {
  background: rgba(20, 20, 20, 0.95);
  border: 0.5px solid rgba(255, 59, 48, 0.3);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
}

.theme-red .submit-form :deep(.el-select-dropdown__item) {
  color: rgba(255, 255, 255, 0.85);
}

.theme-red .submit-form :deep(.el-select-dropdown__item:hover) {
  background: rgba(255, 59, 48, 0.15);
  color: #ffffff;
}

.theme-red .submit-form :deep(.el-select-dropdown__item.is-selected) {
  background: rgba(255, 59, 48, 0.25);
  color: #ff3b30;
  font-weight: 600;
}

/* 上传组件 */
.theme-red .submit-form :deep(.el-upload--picture-card) {
  background: rgba(30, 30, 30, 0.5);
  border: 2px dashed rgba(255, 59, 48, 0.4);
}

.theme-red .submit-form :deep(.el-upload--picture-card:hover) {
  background: rgba(30, 30, 30, 0.6);
  border-color: rgba(255, 59, 48, 0.6);
}

.theme-red .submit-form :deep(.el-upload--picture-card .el-icon) {
  color: rgba(255, 255, 255, 0.6);
}

.theme-red .submit-form :deep(.el-upload-list__item) {
  background: rgba(30, 30, 30, 0.5);
  border: 0.5px solid rgba(255, 59, 48, 0.2);
}

.theme-red .submit-form :deep(.el-upload-list__item:hover) {
  background: rgba(30, 30, 30, 0.6);
  box-shadow: 0 4px 12px rgba(255, 59, 48, 0.2);
}

.theme-red .submit-form :deep(.el-upload-list__item-name) {
  color: rgba(255, 255, 255, 0.85);
}

.theme-red .upload-tip {
  color: rgba(255, 255, 255, 0.5);
}

.theme-red .submit-form :deep(.el-upload__tip) {
  color: rgba(255, 255, 255, 0.5);
}

/* 上传区域背景 */
.theme-red .submit-form :deep(.el-form-item:has(.el-upload)) {
  background: rgba(30, 30, 30, 0.3);
  border: 0.5px solid rgba(255, 59, 48, 0.2);
}

/* 按钮样式 - 保持红色渐变 */
.theme-red .submit-form :deep(.el-button--danger) {
  background: linear-gradient(135deg, rgba(255, 59, 48, 0.3) 0%, rgba(255, 59, 48, 0.5) 100%);
  border: 0.5px solid rgba(255, 59, 48, 0.5);
  color: #ffffff !important;
  box-shadow: 0 6px 20px rgba(255, 59, 48, 0.4);
}

.theme-red .submit-form :deep(.el-button--danger span) {
  color: #ffffff !important;
}

.theme-red .submit-form :deep(.el-button--danger:hover) {
  background: linear-gradient(135deg, rgba(255, 59, 48, 0.4) 0%, rgba(255, 59, 48, 0.6) 100%);
  border-color: rgba(255, 59, 48, 0.7);
  box-shadow: 0 8px 28px rgba(255, 59, 48, 0.5);
}

.theme-red .submit-form :deep(.el-button--primary) {
  background: linear-gradient(135deg, rgba(255, 59, 48, 0.25) 0%, rgba(255, 59, 48, 0.4) 100%);
  border: 0.5px solid rgba(255, 59, 48, 0.4);
  color: #ffffff !important;
  box-shadow: 0 4px 16px rgba(255, 59, 48, 0.3);
}

.theme-red .submit-form :deep(.el-button--primary span) {
  color: #ffffff !important;
}

.theme-red .submit-form :deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, rgba(255, 59, 48, 0.35) 0%, rgba(255, 59, 48, 0.5) 100%);
  border-color: rgba(255, 59, 48, 0.6);
  box-shadow: 0 6px 20px rgba(255, 59, 48, 0.4);
}

.theme-red .submit-form :deep(.el-button--default) {
  background: rgba(30, 30, 30, 0.5);
  border: 0.5px solid rgba(255, 255, 255, 0.15);
  color: rgba(255, 255, 255, 0.85);
}

.theme-red .submit-form :deep(.el-button--default:hover) {
  background: rgba(30, 30, 30, 0.6);
  border-color: rgba(255, 255, 255, 0.25);
  color: #ffffff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

/* Loading状态 */
.theme-red .submit-form :deep(.el-loading-mask) {
  background-color: rgba(10, 10, 10, 0.8);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

.theme-red .submit-form :deep(.el-loading-spinner .circular) {
  stroke: #ff3b30;
}

.theme-red .submit-form :deep(.el-loading-spinner .el-loading-text) {
  color: rgba(255, 255, 255, 0.8);
}

/* 表单验证错误提示 */
.theme-red .submit-form :deep(.el-form-item__error) {
  color: #ff6b59;
}

/* 日期选择器（如果有） */
.theme-red .submit-form :deep(.el-date-editor .el-input__wrapper) {
  background: rgba(30, 30, 30, 0.6);
  border-color: rgba(255, 59, 48, 0.2);
}

.theme-red .submit-form :deep(.el-date-editor:hover .el-input__wrapper) {
  background: rgba(30, 30, 30, 0.7);
  border-color: rgba(255, 59, 48, 0.4);
}

/* ============================================
   红队原生select深色样式
   ============================================ */
.theme-red .custom-select.red-team-select {
  width: 100%;
  background: rgba(30, 30, 30, 0.6);
  border: 0.5px solid rgba(255, 59, 48, 0.2);
  color: rgba(255, 255, 255, 0.9);
  padding: 12px 16px;
  border-radius: var(--radius-sm);
  font-family: var(--font-apple);
  font-size: 14px;
  transition: all 0.3s ease;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

.theme-red .custom-select.red-team-select:hover {
  background: rgba(30, 30, 30, 0.7);
  border-color: rgba(255, 59, 48, 0.4);
  box-shadow: 0 4px 12px rgba(255, 59, 48, 0.2);
}

.theme-red .custom-select.red-team-select:focus {
  outline: none;
  background: rgba(30, 30, 30, 0.8);
  border-color: rgba(255, 59, 48, 0.6);
  box-shadow: 0 4px 16px rgba(255, 59, 48, 0.3);
}

.theme-red .custom-select.red-team-select option {
  background: rgba(20, 20, 20, 0.95);
  color: rgba(255, 255, 255, 0.9);
  padding: 8px 12px;
}

.theme-red .custom-select.red-team-select option:hover {
  background: rgba(255, 59, 48, 0.15);
}

/* ============================================
   红队radio-group深色样式
   ============================================ */
.theme-red .red-team-radio-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.theme-red .red-team-radio-group :deep(.el-radio) {
  color: rgba(255, 255, 255, 0.9);
  font-size: 14px;
  margin-right: 0;
}

.theme-red .red-team-radio-group :deep(.el-radio__label) {
  color: rgba(255, 255, 255, 0.9);
  font-weight: 500;
}

.theme-red .red-team-radio-group :deep(.el-radio__input.is-checked .el-radio__inner) {
  background: #ff3b30;
  border-color: #ff3b30;
}

.theme-red .red-team-radio-group :deep(.el-radio__input.is-checked + .el-radio__label) {
  color: #ff3b30;
  font-weight: 600;
}

.theme-red .red-team-radio-group :deep(.el-radio__inner) {
  background: rgba(30, 30, 30, 0.6);
  border-color: rgba(255, 59, 48, 0.3);
  width: 16px;
  height: 16px;
}

.theme-red .red-team-radio-group :deep(.el-radio__inner:hover) {
  border-color: rgba(255, 59, 48, 0.6);
}

</style>

<style>
/* ============================================
   全局样式：Admin用户下拉菜单主题
   (不使用 scoped，以覆盖 Element Plus teleport 组件)
   ============================================ */

/* ⚪ 管理员浅色下拉菜单 */
.admin-select-dropdown {
  background: #ffffff !important;
  border: 1px solid rgba(0, 0, 0, 0.1) !important;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1),
              0 0 0 0.5px rgba(0, 0, 0, 0.04) !important;
  border-radius: 12px;
}

.admin-select-dropdown .el-select-dropdown__item {
  color: #1d1d1f !important;
  background: transparent;
  transition: all 0.2s ease;
}

.admin-select-dropdown .el-select-dropdown__item:hover {
  background: rgba(231, 76, 60, 0.08) !important;
  color: #1d1d1f !important;
}

.admin-select-dropdown .el-select-dropdown__item.is-selected {
  background: rgba(231, 76, 60, 0.12) !important;
  color: #e74c3c !important;
  font-weight: 600;
}

.admin-select-dropdown .el-select-dropdown__item.is-disabled {
  color: #c8c9cc !important;
  cursor: not-allowed;
}

.admin-select-dropdown .el-select-dropdown__empty {
  color: #86868b !important;
}
</style>