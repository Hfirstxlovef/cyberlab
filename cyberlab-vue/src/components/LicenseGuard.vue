<template>
  <!-- 授权过期全局遮罩层 -->
  <Teleport to="body">
    <Transition name="license-fade">
      <div v-if="isExpired && !isLoading" class="license-guard-overlay">
        <!-- 背景毛玻璃效果 -->
        <div class="overlay-backdrop"></div>

        <!-- 授权过期提示卡片 -->
        <div class="license-expired-modal">
          <!-- 图标区域 -->
          <div class="modal-icon">
            <div class="icon-wrapper">
              🔒
            </div>
          </div>

          <!-- 标题 -->
          <h1 class="modal-title">授权已过期</h1>

          <!-- 描述信息 -->
          <div class="modal-description">
            <p class="description-main">
              {{ message || '系统授权已过期，所有功能已被禁用' }}
            </p>
            <p class="description-detail">
              过期时间：<strong>{{ expiryDateFormatted }}</strong>
            </p>
          </div>

          <!-- 授权信息卡片 -->
          <div class="license-info-card">
            <div class="info-item">
              <span class="info-label">产品序列号</span>
              <span class="info-value">{{ serialNumber || '未知' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">授权持有者</span>
              <span class="info-value">{{ owner || '未知' }}</span>
            </div>
          </div>

          <!-- 联系方式 -->
          <div class="contact-section">
            <p class="contact-title">如需续期或获取技术支持，请联系：</p>
            <div class="contact-details">
              <div class="contact-item">
                <el-icon class="contact-icon"><User /></el-icon>
                <span class="contact-text">{{ supportContact }}</span>
              </div>
              <div class="contact-item">
                <el-icon class="contact-icon"><Message /></el-icon>
                <span class="contact-text contact-email">{{ supportEmail }}</span>
              </div>
            </div>
          </div>

          <!-- 操作按钮 */
          <div class="modal-actions">
            <el-button type="primary" size="large" @click="handleRefresh" :loading="refreshing">
              <el-icon><Refresh /></el-icon>
              重新验证授权
            </el-button>
            <el-button size="large" @click="handleLogout">
              <el-icon><SwitchButton /></el-icon>
              退出系统
            </el-button>
          </div>

          <!-- 底部品牌信息 -->
          <div class="modal-footer">
            <span class="footer-brand">蟑螂恶霸团队</span>
            <span class="footer-product">红岸网络空间安全对抗平台</span>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Message, Refresh, SwitchButton } from '@element-plus/icons-vue'
import { getLicenseInfo } from '@/composables/useLicense'
import { removeToken } from '@/utils/auth'

const router = useRouter()
const refreshing = ref(false)

// 获取全局授权状态
const {
  licenseInfo,
  isExpired,
  loading: isLoading
} = getLicenseInfo()

// 计算属性
const message = computed(() => licenseInfo.value?.message || '')
const serialNumber = computed(() => licenseInfo.value?.serialNumber || '')
const expiryDateFormatted = computed(() => licenseInfo.value?.expiryDateFormatted || '')
const owner = computed(() => licenseInfo.value?.owner || '')
const supportContact = computed(() => licenseInfo.value?.supportContact || '蟑螂恶霸团队')
const supportEmail = computed(() => licenseInfo.value?.supportEmail || 'sun740883686@foxmail.com')

/**
 * 重新验证授权
 */
const handleRefresh = async () => {
  refreshing.value = true
  try {
    // 重新加载页面以触发授权验证
    window.location.reload()
  } catch (error) {
    ElMessage.error('授权验证失败，请稍后重试')
    refreshing.value = false
  }
}

/**
 * 退出系统
 */
const handleLogout = () => {
  // 清除令牌
  removeToken()
  // 跳转到登录页
  router.push('/login')
  ElMessage.info('已退出系统')
}
</script>

<style scoped>
/* ========== Apple 风格授权过期遮罩 ========== */

.license-guard-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-md, 24px);
  font-family: var(--font-apple, -apple-system, BlinkMacSystemFont, "SF Pro Display", sans-serif);
}

/* 背景毛玻璃效果 */
.overlay-backdrop {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
}

/* 授权过期提示卡片 */
.license-expired-modal {
  position: relative;
  z-index: 1;
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.95) 0%,
    rgba(248, 248, 248, 0.9) 100%);
  backdrop-filter: blur(40px);
  -webkit-backdrop-filter: blur(40px);
  border: 0.5px solid rgba(0, 0, 0, 0.1);
  border-radius: var(--radius-2xl, 24px);
  padding: var(--spacing-2xl, 64px) var(--spacing-xl, 48px);
  max-width: 560px;
  width: 100%;
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.12),
    0 24px 96px rgba(0, 0, 0, 0.16);
  animation: modalSlideIn 0.5s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}

@keyframes modalSlideIn {
  from {
    opacity: 0;
    transform: translateY(-40px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* 图标区域 */
.modal-icon {
  display: flex;
  justify-content: center;
  margin-bottom: var(--spacing-lg, 32px);
}

.icon-wrapper {
  width: 96px;
  height: 96px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 56px;
  background: linear-gradient(135deg,
    rgba(245, 101, 101, 0.1) 0%,
    rgba(245, 101, 101, 0.05) 100%);
  border-radius: 50%;
  border: 2px solid rgba(245, 101, 101, 0.2);
  animation: iconPulse 2s ease-in-out infinite;
}

@keyframes iconPulse {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.05);
    opacity: 0.9;
  }
}

/* 标题 */
.modal-title {
  margin: 0 0 var(--spacing-md, 24px) 0;
  font-size: var(--font-3xl, 48px);
  font-weight: var(--font-weight-bold, 700);
  color: var(--apple-red, #f56565);
  text-align: center;
  letter-spacing: var(--letter-spacing-tight, -0.5px);
  line-height: 1.1;
}

/* 描述信息 */
.modal-description {
  margin-bottom: var(--spacing-lg, 32px);
  text-align: center;
}

.description-main {
  margin: 0 0 var(--spacing-sm, 16px) 0;
  font-size: var(--font-lg, 18px);
  font-weight: var(--font-weight-medium, 500);
  color: var(--apple-text-primary, #1d1d1f);
  line-height: 1.5;
}

.description-detail {
  margin: 0;
  font-size: var(--font-md, 16px);
  color: var(--apple-text-secondary, #6e6e73);
  line-height: 1.5;
}

.description-detail strong {
  color: var(--apple-red, #f56565);
  font-weight: var(--font-weight-semibold, 600);
}

/* 授权信息卡片 */
.license-info-card {
  background: rgba(255, 255, 255, 0.7);
  border: 0.5px solid rgba(0, 0, 0, 0.06);
  border-radius: var(--radius-lg, 16px);
  padding: var(--spacing-md, 24px);
  margin-bottom: var(--spacing-lg, 32px);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm, 16px);
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-sm, 16px);
  background: rgba(255, 255, 255, 0.5);
  border-radius: var(--radius-md, 12px);
}

.info-label {
  font-size: var(--font-sm, 14px);
  font-weight: var(--font-weight-medium, 500);
  color: var(--apple-text-secondary, #6e6e73);
}

.info-value {
  font-size: var(--font-md, 16px);
  font-weight: var(--font-weight-semibold, 600);
  color: var(--apple-text-primary, #1d1d1f);
  font-family: 'Monaco', 'Menlo', 'Courier New', monospace;
}

/* 联系方式 */
.contact-section {
  background: rgba(0, 113, 227, 0.05);
  border: 0.5px solid rgba(0, 113, 227, 0.1);
  border-radius: var(--radius-lg, 16px);
  padding: var(--spacing-md, 24px);
  margin-bottom: var(--spacing-lg, 32px);
}

.contact-title {
  margin: 0 0 var(--spacing-sm, 16px) 0;
  font-size: var(--font-md, 16px);
  font-weight: var(--font-weight-medium, 500);
  color: var(--apple-text-secondary, #6e6e73);
  text-align: center;
}

.contact-details {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm, 16px);
}

.contact-item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: var(--spacing-sm, 16px);
  background: rgba(255, 255, 255, 0.6);
  border-radius: var(--radius-md, 12px);
}

.contact-icon {
  font-size: 20px;
  color: var(--apple-blue, #0071e3);
}

.contact-text {
  font-size: var(--font-md, 16px);
  font-weight: var(--font-weight-semibold, 600);
  color: var(--apple-text-primary, #1d1d1f);
}

.contact-email {
  font-family: 'Monaco', 'Menlo', 'Courier New', monospace;
  font-size: var(--font-sm, 14px);
  color: var(--apple-blue, #0071e3);
}

/* 操作按钮 */
.modal-actions {
  display: flex;
  gap: var(--spacing-md, 24px);
  margin-bottom: var(--spacing-lg, 32px);
}

.modal-actions .el-button {
  flex: 1;
  border-radius: var(--radius-md, 12px);
  font-weight: var(--font-weight-medium, 500);
  padding: 16px 24px;
  font-size: var(--font-md, 16px);
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  border: none;
}

.modal-actions :deep(.el-button--primary) {
  background: var(--apple-blue, #0071e3);
}

.modal-actions :deep(.el-button--primary:hover) {
  background: var(--apple-blue-hover, #0077ed);
  transform: translateY(-2px);
}

.modal-actions :deep(.el-button:hover) {
  transform: translateY(-2px);
}

.modal-actions :deep(.el-button:active) {
  transform: scale(0.98);
}

/* 底部品牌信息 */
.modal-footer {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding-top: var(--spacing-md, 24px);
  border-top: 0.5px solid rgba(0, 0, 0, 0.06);
}

.footer-brand {
  font-size: var(--font-md, 16px);
  font-weight: var(--font-weight-semibold, 600);
  color: var(--apple-text-primary, #1d1d1f);
  letter-spacing: 0.5px;
}

.footer-product {
  font-size: var(--font-sm, 14px);
  color: var(--apple-text-tertiary, #86868b);
  letter-spacing: 0.3px;
}

/* 过渡动画 */
.license-fade-enter-active,
.license-fade-leave-active {
  transition: opacity 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}

.license-fade-enter-from,
.license-fade-leave-to {
  opacity: 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .license-expired-modal {
    padding: var(--spacing-lg, 32px) var(--spacing-md, 24px);
    max-width: 100%;
  }

  .modal-title {
    font-size: var(--font-2xl, 32px);
  }

  .description-main {
    font-size: var(--font-md, 16px);
  }

  .modal-actions {
    flex-direction: column;
    gap: var(--spacing-sm, 16px);
  }

  .icon-wrapper {
    width: 72px;
    height: 72px;
    font-size: 42px;
  }
}

/* ========== Apple 风格授权过期遮罩结束 ========== */
</style>
