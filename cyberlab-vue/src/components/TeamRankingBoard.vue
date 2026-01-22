<template>
  <div class="ranking-board">
    <div class="ranking-header">
      <div class="header-icon">🏆</div>
      <h2>实时排行榜</h2>
    </div>

    <div class="ranking-tabs">
      <div
        class="tab"
        :class="{ active: teamType === 'red' }"
        @click="switchTeamType('red')">
        🔴 红队排名
      </div>
      <div
        class="tab"
        :class="{ active: teamType === 'blue' }"
        @click="switchTeamType('blue')">
        🔵 蓝队排名
      </div>
    </div>

    <div v-loading="loading" class="ranking-list">
      <div
        v-for="(team, index) in rankings"
        :key="team.id || team.teamName"
        class="ranking-item"
        :class="{ 'top-three': index < 3 }">
        <div class="rank-badge" :class="'rank-' + (index + 1)">
          <span v-if="index < 3">{{ ['🥇', '🥈', '🥉'][index] }}</span>
          <span v-else>{{ index + 1 }}</span>
        </div>
        <div class="team-info">
          <div class="team-name">{{ team.teamName }}</div>
          <div class="team-stats">
            <span>{{ team.approvedCount || 0 }} 个成果</span>
          </div>
        </div>
        <div class="team-score">
          <div class="score-value">{{ team.totalScore || 0 }}</div>
          <div class="score-label">分</div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="!loading && rankings.length === 0" class="empty-state">
        <div class="empty-icon">📊</div>
        <div class="empty-text">暂无排行数据</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import axios from '@/api/axios'

const props = defineProps({
  rangeId: {
    type: Number,
    required: true,
    default: 1 // 默认演练ID为1
  },
  autoRefresh: {
    type: Boolean,
    default: true
  },
  refreshInterval: {
    type: Number,
    default: 30000 // 默认30秒刷新一次
  }
})

const loading = ref(false)
const teamType = ref('red')
const rankings = ref([])
let refreshTimer = null

// 获取排行榜数据
const fetchRankings = async () => {
  loading.value = true
  try {
    const response = await axios.get('/achievements/rankings', {
      params: {
        rangeId: props.rangeId,
        teamType: teamType.value
      }
    })

    if (response && response.success && response.data) {
      rankings.value = response.data
    } else {
      rankings.value = []
    }
  } catch (error) {
    console.error('获取排行榜失败:', error)
    // 使用模拟数据
    rankings.value = generateMockData()
  } finally {
    loading.value = false
  }
}

// 生成模拟数据
const generateMockData = () => {
  if (teamType.value === 'red') {
    return [
      { id: 1, teamName: '红队Alpha', totalScore: 320, approvedCount: 5, ranking: 1 },
      { id: 2, teamName: '红队Beta', totalScore: 280, approvedCount: 4, ranking: 2 },
      { id: 3, teamName: '红队Gamma', totalScore: 250, approvedCount: 4, ranking: 3 },
      { id: 4, teamName: '红队Delta', totalScore: 180, approvedCount: 3, ranking: 4 }
    ]
  } else {
    return [
      { id: 5, teamName: '蓝队Alpha', totalScore: 290, approvedCount: 5, ranking: 1 },
      { id: 6, teamName: '蓝队Beta', totalScore: 260, approvedCount: 4, ranking: 2 },
      { id: 7, teamName: '蓝队Gamma', totalScore: 210, approvedCount: 3, ranking: 3 }
    ]
  }
}

// 切换队伍类型
const switchTeamType = (type) => {
  teamType.value = type
  fetchRankings()
}

// 监听队伍类型变化
watch(() => teamType.value, () => {
  fetchRankings()
})

// 启动自动刷新
const startAutoRefresh = () => {
  if (props.autoRefresh && !refreshTimer) {
    refreshTimer = setInterval(() => {
      fetchRankings()
    }, props.refreshInterval)
  }
}

// 停止自动刷新
const stopAutoRefresh = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

onMounted(() => {
  fetchRankings()
  startAutoRefresh()
})

// 组件卸载时清理定时器
import { onBeforeUnmount } from 'vue'
onBeforeUnmount(() => {
  stopAutoRefresh()
})
</script>

<style scoped>
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

.ranking-board {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.95) 0%,
    rgba(250, 250, 250, 0.9) 100%);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  padding: var(--spacing-lg);
  font-family: var(--font-apple);
}

.ranking-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-lg);
  padding-bottom: var(--spacing-md);
  border-bottom: 1px solid var(--apple-border);
}

.header-icon {
  font-size: 48px;
  filter: drop-shadow(0 4px 12px rgba(255, 149, 0, 0.3));
}

.ranking-header h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: var(--apple-text);
  letter-spacing: -0.5px;
}

.ranking-tabs {
  display: flex;
  gap: var(--spacing-xs);
  margin-bottom: var(--spacing-lg);
  background: rgba(0, 0, 0, 0.02);
  padding: 4px;
  border-radius: var(--radius-md);
}

.tab {
  flex: 1;
  padding: var(--spacing-sm);
  text-align: center;
  font-weight: 600;
  font-size: 15px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
  color: var(--apple-text-secondary);
}

.tab:hover {
  background: rgba(0, 0, 0, 0.04);
}

.tab.active {
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  color: var(--apple-blue);
}

.ranking-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
  min-height: 300px;
}

.ranking-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-md);
  background: rgba(0, 0, 0, 0.02);
  border-radius: var(--radius-md);
  transition: all 0.3s ease;
}

.ranking-item:hover {
  background: rgba(0, 122, 255, 0.04);
  transform: translateX(4px);
  box-shadow: 0 2px 8px rgba(0, 122, 255, 0.1);
}

.ranking-item.top-three {
  background: linear-gradient(135deg,
    rgba(255, 149, 0, 0.08) 0%,
    rgba(255, 149, 0, 0.12) 100%);
  border-left: 4px solid var(--apple-orange);
}

.rank-badge {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.04);
  border-radius: 50%;
  font-weight: 700;
  font-size: 18px;
  flex-shrink: 0;
}

.rank-badge.rank-1 {
  background: linear-gradient(135deg, #FFD700 0%, #FFA500 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(255, 215, 0, 0.4);
  font-size: 24px;
}

.rank-badge.rank-2 {
  background: linear-gradient(135deg, #C0C0C0 0%, #A8A8A8 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(192, 192, 192, 0.4);
  font-size: 24px;
}

.rank-badge.rank-3 {
  background: linear-gradient(135deg, #CD7F32 0%, #B8733F 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(205, 127, 50, 0.4);
  font-size: 24px;
}

.team-info {
  flex: 1;
}

.team-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--apple-text);
  margin-bottom: 4px;
}

.team-stats {
  font-size: 13px;
  color: var(--apple-text-secondary);
}

.team-score {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.score-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--apple-blue);
  font-family: -apple-system, "SF Pro Display", sans-serif;
  letter-spacing: -1px;
}

.score-label {
  font-size: 14px;
  color: var(--apple-text-secondary);
  font-weight: 500;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-xl);
  min-height: 300px;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: var(--spacing-md);
  opacity: 0.5;
}

.empty-text {
  font-size: 15px;
  color: var(--apple-text-secondary);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .ranking-board {
    padding: var(--spacing-md);
  }

  .header-icon {
    font-size: 36px;
  }

  .ranking-header h2 {
    font-size: 20px;
  }

  .rank-badge {
    width: 40px;
    height: 40px;
    font-size: 16px;
  }

  .rank-badge.rank-1,
  .rank-badge.rank-2,
  .rank-badge.rank-3 {
    font-size: 20px;
  }

  .team-name {
    font-size: 14px;
  }

  .score-value {
    font-size: 24px;
  }
}
</style>
