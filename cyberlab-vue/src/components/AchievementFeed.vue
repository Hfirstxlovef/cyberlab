<template>
  <div class="achievement-feed">
    <div class="feed-header">
      <h3>🏆 最新通过的成果</h3>
    </div>

    <transition-group name="feed-list" tag="div" class="feed-list">
      <div
        v-for="achievement in achievements"
        :key="achievement.id"
        class="feed-item"
        :class="getTeamClass(achievement.teamName)"
      >
        <div class="feed-item-header">
          <el-avatar :size="40" :style="{ background: getAvatarColor(achievement.teamName) }">
            {{ achievement.teamName.substring(0, 2) }}
          </el-avatar>
          <div class="team-info">
            <div class="team-name">{{ achievement.teamName }}</div>
            <div class="time">{{ formatTime(achievement.reviewTime) }}</div>
          </div>
          <el-tag :type="getTeamType(achievement.teamName)" size="small">
            {{ getTeamLabel(achievement.teamName) }}
          </el-tag>
        </div>

        <div class="feed-item-content">
          <div class="target">
            攻破目标：<strong>{{ achievement.targetName }}</strong>
          </div>
          <div class="method" v-if="achievement.attackMethod">
            使用方法：<strong>{{ achievement.attackMethod }}</strong>
          </div>
          <div class="description" v-if="achievement.description">
            {{ truncateText(achievement.description, 80) }}
          </div>
        </div>

        <div class="feed-item-footer">
          <el-button size="small" type="primary" text @click="viewDetail(achievement)">
            查看详情
            <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
      </div>
    </transition-group>

    <div v-if="achievements.length === 0" class="empty-state">
      <el-empty description="暂无最新通过的成果" :image-size="80" />
    </div>

    <!-- 详情弹窗 -->
    <el-dialog
      v-model="detailVisible"
      :title="`成果详情 - ${currentAchievement?.teamName}`"
      width="600px"
    >
      <el-descriptions v-if="currentAchievement" :column="1" border>
        <el-descriptions-item label="团队名称">
          {{ currentAchievement.teamName }}
        </el-descriptions-item>
        <el-descriptions-item label="攻破目标">
          {{ currentAchievement.targetName }}
        </el-descriptions-item>
        <el-descriptions-item label="攻击方法" v-if="currentAchievement.attackMethod">
          {{ currentAchievement.attackMethod }}
        </el-descriptions-item>
        <el-descriptions-item label="描述">
          {{ currentAchievement.description }}
        </el-descriptions-item>
        <el-descriptions-item label="提交时间">
          {{ formatDateTime(currentAchievement.submitTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="审核时间">
          {{ formatDateTime(currentAchievement.reviewTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag type="success">已通过</el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ArrowRight } from '@element-plus/icons-vue'
import { pollLatestAchievements } from '@/api/achievement'

const achievements = ref([])
const detailVisible = ref(false)
const currentAchievement = ref(null)
let stopPolling = null

// 轮询获取最新成果
onMounted(() => {
  stopPolling = pollLatestAchievements((newAchievements) => {
    // 将新成果插入到列表前面，保持最多20条
    achievements.value = [...newAchievements, ...achievements.value].slice(0, 20)
  }, 10000) // 每10秒轮询一次
})

onUnmounted(() => {
  if (stopPolling) stopPolling()
})

// 格式化时间（相对时间）
const formatTime = (time) => {
  if (!time) return '-'

  const now = new Date()
  const reviewTime = new Date(time)
  const diff = Math.floor((now - reviewTime) / 1000 / 60)

  if (diff < 1) return '刚刚'
  if (diff < 60) return `${diff}分钟前`
  if (diff < 1440) return `${Math.floor(diff / 60)}小时前`
  return reviewTime.toLocaleDateString('zh-CN')
}

// 格式化完整日期时间
const formatDateTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

// 获取团队类型标签
const getTeamType = (teamName) => {
  if (teamName.includes('红') || teamName.toLowerCase().includes('red')) return 'danger'
  if (teamName.includes('蓝') || teamName.toLowerCase().includes('blue')) return 'primary'
  return 'info'
}

// 获取团队标签文本
const getTeamLabel = (teamName) => {
  if (teamName.includes('红') || teamName.toLowerCase().includes('red')) return '红队攻击'
  if (teamName.includes('蓝') || teamName.toLowerCase().includes('blue')) return '蓝队防守'
  return '成果'
}

// 获取团队样式类
const getTeamClass = (teamName) => {
  if (teamName.includes('红') || teamName.toLowerCase().includes('red')) return 'red-team-item'
  if (teamName.includes('蓝') || teamName.toLowerCase().includes('blue')) return 'blue-team-item'
  return ''
}

// 获取头像颜色
const getAvatarColor = (teamName) => {
  if (teamName.includes('红') || teamName.toLowerCase().includes('red')) return '#f56c6c'
  if (teamName.includes('蓝') || teamName.toLowerCase().includes('blue')) return '#409eff'
  return '#909399'
}

// 截断文本
const truncateText = (text, maxLength) => {
  if (!text) return ''
  if (text.length <= maxLength) return text
  return text.substring(0, maxLength) + '...'
}

// 查看详情
const viewDetail = (achievement) => {
  currentAchievement.value = achievement
  detailVisible.value = true
}
</script>

<style scoped>
/* ========== Apple 风格成果流 ========== */

.achievement-feed {
  /* 毛玻璃卡片效果 */
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: saturate(180%) blur(20px);
  -webkit-backdrop-filter: saturate(180%) blur(20px);
  border: 0.5px solid rgba(0, 0, 0, 0.04);
  border-radius: var(--radius-xl, 20px);
  padding: var(--spacing-lg, 32px);
  box-shadow:
    0 2px 8px rgba(0, 0, 0, 0.04),
    0 8px 32px rgba(0, 0, 0, 0.03);
  height: 100%;
  display: flex;
  flex-direction: column;
  font-family: var(--font-apple, -apple-system, BlinkMacSystemFont, "SF Pro Display", sans-serif);
}

.feed-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-md, 24px);
  padding-bottom: var(--spacing-sm, 16px);
  border-bottom: 0.5px solid rgba(0, 0, 0, 0.06);
}

.feed-header h3 {
  margin: 0;
  font-size: var(--font-xl, 24px);
  font-weight: var(--font-weight-semibold, 600);
  letter-spacing: var(--letter-spacing-tight, -0.5px);
  color: var(--apple-text-primary, #1d1d1f);
}

.feed-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm, 16px);
  /* Apple 风格滚动条已在全局样式中定义 */
}

.feed-item {
  /* Apple 卡片效果 */
  background: rgba(255, 255, 255, 0.8);
  border-radius: var(--radius-lg, 16px);
  padding: var(--spacing-md, 24px);
  border: 0.5px solid rgba(0, 0, 0, 0.04);
  box-shadow:
    0 2px 8px rgba(0, 0, 0, 0.04),
    0 8px 32px rgba(0, 0, 0, 0.03);
  transition: all 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  /* Apple 流畅曲线 */
}

.feed-item:hover {
  transform: translateY(-4px) scale(1.01);
  box-shadow:
    0 8px 24px rgba(0, 0, 0, 0.08),
    0 16px 48px rgba(0, 0, 0, 0.06);
}

.red-team-item {
  border-left: 3px solid var(--apple-red, #f56565);
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.85) 0%,
    rgba(255, 245, 245, 0.7) 100%);
}

.blue-team-item {
  border-left: 3px solid var(--apple-indigo, #3742fa);
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.85) 0%,
    rgba(245, 248, 255, 0.7) 100%);
}

/* 动画效果 */
.feed-list-enter-active {
  animation: slideIn 0.5s;
}

.feed-list-leave-active {
  transition: all 0.3s;
}

.feed-list-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateX(-20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.feed-item-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.team-info {
  flex: 1;
}

.team-name {
  font-weight: var(--font-weight-semibold, 600);
  font-size: var(--font-md, 16px);
  color: var(--apple-text-primary, #1d1d1f);
  letter-spacing: -0.3px;
}

.time {
  font-size: var(--font-sm, 14px);
  color: var(--apple-text-secondary, #6e6e73);
  margin-top: 4px;
  font-weight: var(--font-weight-regular, 400);
}

.feed-item-content {
  margin: var(--spacing-sm, 16px) 0;
  line-height: 1.6;
}

.target,
.method {
  margin-bottom: var(--spacing-xs, 8px);
  font-size: var(--font-sm, 14px);
  color: var(--apple-text-secondary, #6e6e73);
}

.target strong,
.method strong {
  color: var(--apple-text-primary, #1d1d1f);
  font-weight: var(--font-weight-semibold, 600);
}

.description {
  color: var(--apple-text-tertiary, #86868b);
  font-size: var(--font-xs, 12px);
  margin-top: var(--spacing-sm, 16px);
  padding-left: var(--spacing-sm, 16px);
  border-left: 2px solid rgba(0, 113, 227, 0.3);
  line-height: 1.6;
  font-weight: var(--font-weight-regular, 400);
}

.feed-item-footer {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

.empty-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-3xl, 96px) var(--spacing-xl, 48px);
  color: var(--apple-text-tertiary, #86868b);
  font-size: var(--font-md, 16px);
  font-weight: var(--font-weight-regular, 400);
}

/* ========== Apple 风格优化结束 ========== */
</style>
