<template>
  <div class="rule-violations">
    <h3 class="chart-title">规则违规</h3>
    <div class="violations-list">
      <div 
        class="violation-item" 
        v-for="violation in violations" 
        :key="violation.id"
        :class="`severity-${violation.severity}`"
      >
        <div class="violation-header">
          <span class="violation-time">{{ violation.time }}</span>
          <span class="severity-badge">{{ getSeverityText(violation.severity) }}</span>
        </div>
        <div class="violation-team">{{ violation.team }}</div>
        <div class="violation-rule">违反规则: {{ violation.rule }}</div>
        <div class="violation-desc">{{ violation.description }}</div>
        <div class="violation-penalty">
          <span class="penalty-label">处罚:</span>
          <span class="penalty-value">{{ violation.penalty }}</span>
        </div>
      </div>
    </div>
    <div class="violations-summary">
      <div class="summary-item">
        <span class="summary-label">总违规次数</span>
        <span class="summary-value">{{ totalViolations }}</span>
      </div>
      <div class="summary-item">
        <span class="summary-label">红队违规</span>
        <span class="summary-value red">{{ redViolations }}</span>
      </div>
      <div class="summary-item">
        <span class="summary-label">蓝队违规</span>
        <span class="summary-value blue">{{ blueViolations }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  data: {
    type: Array,
    default: () => []
  }
})

const violations = ref([])

const generateViolations = () => {
  const teams = ['🔴 红队', '🔵 蓝队']
  const severities = ['low', 'medium', 'high']
  const rules = [
    { name: '未授权扫描', desc: '对非目标系统进行扫描', penalty: '扣除50分' },
    { name: '破坏系统', desc: '恶意破坏目标系统正常运行', penalty: '扣除100分' },
    { name: '越权操作', desc: '执行超出权限范围的操作', penalty: '扣除75分' },
    { name: '数据泄露', desc: '将演练数据泄露到外部', penalty: '扣除200分' },
    { name: '时间违规', desc: '在非演练时间进行操作', penalty: '警告' },
    { name: '团队协作', desc: '违反团队协作规则', penalty: '扣除30分' }
  ]
  
  const newViolations = []
  const now = new Date()
  
  for (let i = 0; i < 6; i++) {
    const team = teams[Math.floor(Math.random() * teams.length)]
    const severity = severities[Math.floor(Math.random() * severities.length)]
    const rule = rules[Math.floor(Math.random() * rules.length)]
    const time = new Date(now.getTime() - i * 30 * 60000)
    
    newViolations.push({
      id: Date.now() + i,
      time: time.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }),
      team: team,
      severity: severity,
      rule: rule.name,
      description: rule.desc,
      penalty: rule.penalty
    })
  }
  
  violations.value = newViolations
}

const totalViolations = computed(() => violations.value.length)
const redViolations = computed(() => violations.value.filter(v => v.team.includes('红队')).length)
const blueViolations = computed(() => violations.value.filter(v => v.team.includes('蓝队')).length)

const getSeverityText = (severity) => {
  const severityMap = {
    'low': '轻微',
    'medium': '中等',
    'high': '严重'
  }
  return severityMap[severity] || '未知'
}

let timer = null

onMounted(() => {
  generateViolations()
  timer = setInterval(generateViolations, 60000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.rule-violations {
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

.violations-list {
  flex: 1;
  overflow-y: auto;
  margin-bottom: 10px;
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

.severity-low {
  border-color: rgba(255, 193, 7, 0.3);
}

.severity-medium {
  border-color: rgba(255, 152, 0, 0.3);
  background: rgba(255, 152, 0, 0.05);
}

.severity-high {
  border-color: rgba(244, 67, 54, 0.3);
  background: rgba(244, 67, 54, 0.05);
}

.violation-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.violation-time {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.6);
}

.severity-badge {
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: 500;
}

.severity-low .severity-badge {
  background: rgba(255, 193, 7, 0.2);
  color: #ffc107;
}

.severity-medium .severity-badge {
  background: rgba(255, 152, 0, 0.2);
  color: #ff9800;
}

.severity-high .severity-badge {
  background: rgba(244, 67, 54, 0.2);
  color: #f44336;
}

.violation-team {
  font-size: 12px;
  font-weight: 500;
  margin-bottom: 4px;
}

.violation-rule {
  font-size: 12px;
  color: #fff;
  margin-bottom: 4px;
}

.violation-desc {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.6);
  margin-bottom: 6px;
}

.violation-penalty {
  display: flex;
  gap: 5px;
  font-size: 11px;
}

.penalty-label {
  color: rgba(255, 255, 255, 0.6);
}

.penalty-value {
  color: #ff9800;
  font-weight: 500;
}

.violations-summary {
  display: flex;
  gap: 10px;
  padding: 10px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  border: 1px solid rgba(156, 39, 176, 0.3);
}

.summary-item {
  flex: 1;
  text-align: center;
  padding: 5px;
}

.summary-label {
  display: block;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.6);
  margin-bottom: 4px;
}

.summary-value {
  display: block;
  font-size: 18px;
  font-weight: bold;
  color: #9c27b0;
}

.summary-value.red {
  color: #f44336;
}

.summary-value.blue {
  color: #2196f3;
}
</style>