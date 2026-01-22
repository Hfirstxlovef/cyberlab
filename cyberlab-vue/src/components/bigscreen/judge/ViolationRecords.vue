<template>
  <div class="violation-records">
    <h3 class="chart-title">违规记录</h3>
    <div class="violations-container">
      <div class="violations-list">
        <div 
          class="violation-item" 
          v-for="violation in violations" 
          :key="violation.id"
          :class="[`team-${violation.team}`, { 'new-violation': violation.isNew }]"
        >
          <div class="violation-header">
            <span class="violation-time">{{ violation.time }}</span>
            <span class="violation-team">{{ getTeamText(violation.team) }}</span>
          </div>
          <div class="violation-type">
            <span class="type-icon">⚠️</span>
            <span class="type-text">{{ violation.type }}</span>
          </div>
          <div class="violation-desc">{{ violation.description }}</div>
          <div class="violation-footer">
            <span class="penalty-label">处罚:</span>
            <span class="penalty-value">{{ violation.penalty }}</span>
          </div>
        </div>
      </div>
      
      <div class="violations-stats">
        <div class="stat-card">
          <div class="stat-label">红队违规</div>
          <div class="stat-value red">{{ redViolations }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">蓝队违规</div>
          <div class="stat-value blue">{{ blueViolations }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">总扣分</div>
          <div class="stat-value">{{ totalPenalty }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  violations: {
    type: Array,
    default: () => []
  }
})

const violations = ref([])

const generateViolations = () => {
  const violationTypes = [
    { type: '非法扫描', desc: '对非目标系统进行端口扫描', penalty: '-50分' },
    { type: '系统破坏', desc: '恶意删除目标系统文件', penalty: '-100分' },
    { type: '越权操作', desc: '尝试访问未授权的管理后台', penalty: '-75分' },
    { type: '规则违反', desc: '在非演练时间进行攻击', penalty: '警告' },
    { type: '信息泄露', desc: '将演练信息发布到外部', penalty: '-200分' },
    { type: '团队协作违规', desc: '未经授权单独行动', penalty: '-30分' }
  ]
  
  const newViolations = []
  const now = new Date()
  
  for (let i = 0; i < 5; i++) {
    const team = Math.random() > 0.5 ? 'red' : 'blue'
    const violation = violationTypes[Math.floor(Math.random() * violationTypes.length)]
    const time = new Date(now.getTime() - i * 15 * 60000)
    
    newViolations.push({
      id: Date.now() + i,
      time: time.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }),
      team: team,
      type: violation.type,
      description: violation.desc,
      penalty: violation.penalty,
      isNew: i === 0
    })
  }
  
  violations.value = newViolations
}

const redViolations = computed(() => 
  violations.value.filter(v => v.team === 'red').length
)

const blueViolations = computed(() => 
  violations.value.filter(v => v.team === 'blue').length
)

const totalPenalty = computed(() => {
  let total = 0
  violations.value.forEach(v => {
    const match = v.penalty.match(/-(\d+)/)
    if (match) {
      total += parseInt(match[1])
    }
  })
  return total
})

const getTeamText = (team) => {
  return team === 'red' ? '🔴 红队' : '🔵 蓝队'
}

let timer = null

onMounted(() => {
  if (props.violations.length > 0) {
    violations.value = props.violations
  } else {
    generateViolations()
  }
  
  timer = setInterval(() => {
    if (props.violations.length === 0) {
      generateViolations()
    }
  }, 60000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.violation-records {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.chart-title {
  margin: 0 0 15px 0;
  font-size: 18px;
  color: #9c27b0;
  font-weight: 500;
}

.violations-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.violations-list {
  flex: 1;
  overflow-y: auto;
  padding-right: 5px;
}

.violations-list::-webkit-scrollbar {
  width: 6px;
}

.violations-list::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 3px;
}

.violations-list::-webkit-scrollbar-thumb {
  background: rgba(156, 39, 176, 0.3);
  border-radius: 3px;
}

.violation-item {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 10px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: all 0.3s ease;
}

.violation-item:hover {
  transform: translateX(5px);
}

.team-red {
  border-color: rgba(244, 67, 54, 0.3);
}

.team-blue {
  border-color: rgba(33, 150, 243, 0.3);
}

.new-violation {
  animation: newViolationPulse 2s ease infinite;
}

@keyframes newViolationPulse {
  0%, 100% {
    box-shadow: 0 0 0 0 rgba(156, 39, 176, 0.4);
  }
  50% {
    box-shadow: 0 0 10px 5px rgba(156, 39, 176, 0.2);
  }
}

.violation-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.violation-time {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.6);
}

.violation-team {
  font-size: 12px;
  font-weight: 500;
}

.violation-type {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
}

.type-icon {
  font-size: 14px;
}

.type-text {
  font-size: 13px;
  font-weight: 500;
  color: #ff9800;
}

.violation-desc {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 8px;
  line-height: 1.4;
}

.violation-footer {
  display: flex;
  gap: 5px;
  font-size: 11px;
}

.penalty-label {
  color: rgba(255, 255, 255, 0.6);
}

.penalty-value {
  color: #f44336;
  font-weight: 500;
}

.violations-stats {
  display: flex;
  gap: 10px;
}

.stat-card {
  flex: 1;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  padding: 12px;
  text-align: center;
  border: 1px solid rgba(156, 39, 176, 0.2);
}

.stat-label {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.6);
  margin-bottom: 6px;
}

.stat-value {
  font-size: 20px;
  font-weight: bold;
  color: #9c27b0;
}

.stat-value.red {
  color: #f44336;
}

.stat-value.blue {
  color: #2196f3;
}
</style>