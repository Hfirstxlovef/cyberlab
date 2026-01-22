<template>
  <div class="alert-panel">
    <div class="chart-header">
      <h3>⚠️ 实时告警</h3>
      <div class="alert-stats">
        <span class="alert-count critical">严重: {{ criticalCount }}</span>
        <span class="alert-count warning">警告: {{ warningCount }}</span>
      </div>
    </div>
    
    <div class="alert-list">
      <div 
        v-for="alert in data" 
        :key="alert.id"
        class="alert-item"
        :class="alert.level">
        <div class="alert-indicator" :class="alert.level"></div>
        <div class="alert-content">
          <div class="alert-title">{{ alert.title }}</div>
          <div class="alert-description">{{ alert.description }}</div>
          <div class="alert-meta">
            <span class="alert-time">{{ formatTime(alert.time) }}</span>
            <span class="alert-source">{{ alert.source }}</span>
          </div>
        </div>
        <div class="alert-actions">
          <button class="action-btn resolve" @click="resolveAlert(alert)">
            处理
          </button>
          <button class="action-btn ignore" @click="ignoreAlert(alert)">
            忽略
          </button>
        </div>
      </div>
    </div>
    
    <div class="alert-footer">
      <button class="footer-btn" @click="clearAll">清空所有</button>
      <button class="footer-btn" @click="exportAlerts">导出日志</button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  data: {
    type: Array,
    default: () => [
      {
        id: 1,
        level: 'critical',
        title: '检测到SQL注入攻击',
        description: '来自IP 192.168.1.100的恶意SQL注入尝试',
        time: new Date(Date.now() - 2 * 60 * 1000),
        source: 'WAF'
      },
      {
        id: 2,
        level: 'warning',
        title: '异常登录尝试',
        description: '用户admin连续5次登录失败',
        time: new Date(Date.now() - 5 * 60 * 1000),
        source: '认证系统'
      },
      {
        id: 3,
        level: 'critical',
        title: '系统资源告警',
        description: 'CPU使用率超过90%，持续时间5分钟',
        time: new Date(Date.now() - 8 * 60 * 1000),
        source: '监控系统'
      },
      {
        id: 4,
        level: 'warning',
        title: '文件上传异常',
        description: '检测到可疑文件上传行为',
        time: new Date(Date.now() - 12 * 60 * 1000),
        source: '文件监控'
      },
      {
        id: 5,
        level: 'info',
        title: '新用户注册',
        description: '用户test123完成注册',
        time: new Date(Date.now() - 15 * 60 * 1000),
        source: '用户系统'
      }
    ]
  }
})

const criticalCount = computed(() => {
  return props.data.filter(alert => alert.level === 'critical').length
})

const warningCount = computed(() => {
  return props.data.filter(alert => alert.level === 'warning').length
})

const formatTime = (time) => {
  const now = new Date()
  const diff = Math.floor((now - time) / 1000 / 60) // 分钟差
  
  if (diff < 1) return '刚刚'
  if (diff < 60) return `${diff}分钟前`
  if (diff < 1440) return `${Math.floor(diff / 60)}小时前`
  return time.toLocaleDateString()
}

const resolveAlert = (alert) => {
  // 这里应该调用实际的告警处理API
}

const ignoreAlert = (alert) => {
  // 这里应该调用实际的告警忽略API
}

const clearAll = () => {
  // 这里应该调用实际的清空API
}

const exportAlerts = () => {
  // 这里应该调用实际的导出API
}
</script>

<style scoped>
/* ============================================
   Apple Elegant White Style - Alert Panel
   ============================================ */

:root {
  --apple-text: #1d1d1f;
  --apple-text-secondary: #86868b;
  --apple-border: rgba(0, 0, 0, 0.04);
  --apple-blue: #007aff;
  --apple-green: #34c759;
  --apple-orange: #ff9500;
  --apple-red: #ff3b30;
  --font-apple: -apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", "Helvetica Neue", sans-serif;
}

.alert-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
  font-family: var(--font-apple);
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.chart-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--apple-text);
  letter-spacing: -0.3px;
}

.alert-stats {
  display: flex;
  gap: 15px;
  font-size: 12px;
}

.alert-count {
  padding: 4px 8px;
  border-radius: 8px;
  font-weight: 600;
}

.alert-count.critical {
  background: rgba(255, 59, 48, 0.1);
  color: var(--apple-red);
}

.alert-count.warning {
  background: rgba(255, 149, 0, 0.1);
  color: var(--apple-orange);
}

.alert-list {
  flex: 1;
  overflow-y: auto;
  margin-bottom: 15px;
}

.alert-item {
  display: flex;
  align-items: flex-start;
  padding: 12px;
  margin-bottom: 8px;
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.95) 0%,
    rgba(250, 250, 250, 0.9) 100%);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-radius: 10px;
  border-left: 3px solid transparent;
  border: 0.5px solid var(--apple-border);
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.alert-item:hover {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 1) 0%,
    rgba(248, 248, 248, 0.95) 100%);
  transform: translateX(2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.alert-item.critical {
  border-left-color: var(--apple-red);
}

.alert-item.warning {
  border-left-color: var(--apple-orange);
}

.alert-item.info {
  border-left-color: var(--apple-blue);
}

.alert-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-top: 6px;
  margin-right: 12px;
  flex-shrink: 0;
  animation: pulse 2s infinite;
}

.alert-indicator.critical {
  background: var(--apple-red);
  box-shadow: 0 0 8px rgba(255, 59, 48, 0.4);
}

.alert-indicator.warning {
  background: var(--apple-orange);
  box-shadow: 0 0 8px rgba(255, 149, 0, 0.4);
}

.alert-indicator.info {
  background: var(--apple-blue);
  box-shadow: 0 0 8px rgba(0, 122, 255, 0.4);
}

.alert-content {
  flex: 1;
  min-width: 0;
}

.alert-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--apple-text);
  margin-bottom: 4px;
  letter-spacing: -0.2px;
}

.alert-description {
  font-size: 12px;
  color: var(--apple-text-secondary);
  margin-bottom: 6px;
  line-height: 1.4;
}

.alert-meta {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: var(--apple-text-secondary);
  font-weight: 500;
}

.alert-actions {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-left: 12px;
}

.action-btn {
  padding: 4px 8px;
  border: none;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
  font-family: var(--font-apple);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
  min-width: 40px;
}

.action-btn.resolve {
  background: linear-gradient(135deg, var(--apple-green) 0%, #28a745 100%);
  color: #ffffff;
  box-shadow: 0 2px 6px rgba(52, 199, 89, 0.2);
}

.action-btn.resolve:hover {
  transform: translateY(-1px);
  box-shadow: 0 3px 10px rgba(52, 199, 89, 0.3);
}

.action-btn.ignore {
  background: rgba(0, 0, 0, 0.04);
  color: var(--apple-text-secondary);
  border: 0.5px solid rgba(0, 0, 0, 0.08);
}

.action-btn.ignore:hover {
  background: rgba(0, 0, 0, 0.08);
  transform: translateY(-1px);
}

.alert-footer {
  display: flex;
  gap: 10px;
  padding-top: 15px;
  border-top: 1px solid var(--apple-border);
}

.footer-btn {
  flex: 1;
  padding: 8px 16px;
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.95) 0%,
    rgba(250, 250, 250, 0.9) 100%);
  color: var(--apple-text);
  border: 0.5px solid var(--apple-border);
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  font-family: var(--font-apple);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.04);
}

.footer-btn:hover {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 1) 0%,
    rgba(248, 248, 248, 0.95) 100%);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

/* 滚动条样式 - Apple风格 */
.alert-list::-webkit-scrollbar {
  width: 4px;
}

.alert-list::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.04);
  border-radius: 2px;
}

.alert-list::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.15);
  border-radius: 2px;
  transition: background 0.2s ease;
}

.alert-list::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.25);
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}
</style>