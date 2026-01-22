<template>
  <div class="event-timeline">
    <h3 class="chart-title">事件时间轴</h3>
    <div class="timeline-container">
      <div 
        class="timeline-item" 
        v-for="event in events" 
        :key="event.id"
        :class="[`event-${event.type}`, { 'new-event': event.isNew }]"
      >
        <div class="timeline-time">{{ event.time }}</div>
        <div class="timeline-marker">
          <span class="marker-dot"></span>
          <span class="marker-line"></span>
        </div>
        <div class="timeline-content">
          <div class="event-header">
            <span class="event-team">{{ event.team }}</span>
            <span class="event-score">{{ event.score > 0 ? '+' : '' }}{{ event.score }}</span>
          </div>
          <div class="event-title">{{ event.title }}</div>
          <div class="event-desc">{{ event.description }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  data: {
    type: Array,
    default: () => []
  }
})

const events = ref([])

const generateEvents = () => {
  const eventTypes = ['attack', 'defense', 'system', 'violation']
  const teams = ['🔴 红队', '🔵 蓝队']
  const attackEvents = [
    { title: 'SQL注入攻击', desc: '成功利用SQL注入漏洞获取数据库权限', score: 50 },
    { title: 'XSS攻击', desc: '成功执行跨站脚本攻击', score: 30 },
    { title: '文件上传漏洞', desc: '成功上传并执行恶意文件', score: 40 },
    { title: '权限提升', desc: '成功将普通用户权限提升为管理员', score: 60 }
  ]
  const defenseEvents = [
    { title: '攻击拦截', desc: '成功拦截SQL注入攻击尝试', score: 30 },
    { title: '入侵检测', desc: '检测到异常流量并及时响应', score: 25 },
    { title: '漏洞修复', desc: '及时修复高危漏洞', score: 35 },
    { title: '系统加固', desc: '完成关键系统安全加固', score: 40 }
  ]
  
  const newEvents = []
  const now = new Date()
  
  for (let i = 0; i < 10; i++) {
    const isAttack = Math.random() > 0.5
    const eventData = isAttack ? attackEvents : defenseEvents
    const selectedEvent = eventData[Math.floor(Math.random() * eventData.length)]
    const time = new Date(now.getTime() - i * 5 * 60000)
    
    newEvents.push({
      id: Date.now() + i,
      time: time.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' }),
      type: isAttack ? 'attack' : 'defense',
      team: isAttack ? teams[0] : teams[1],
      title: selectedEvent.title,
      description: selectedEvent.desc,
      score: isAttack ? selectedEvent.score : -selectedEvent.score,
      isNew: i === 0
    })
  }
  
  events.value = newEvents
}

let timer = null

onMounted(() => {
  generateEvents()
  timer = setInterval(() => {
    generateEvents()
  }, 30000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.event-timeline {
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

.timeline-container {
  flex: 1;
  overflow-y: auto;
  padding-right: 10px;
}

.timeline-container::-webkit-scrollbar {
  width: 6px;
}

.timeline-container::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 3px;
}

.timeline-container::-webkit-scrollbar-thumb {
  background: rgba(156, 39, 176, 0.3);
  border-radius: 3px;
}

.timeline-item {
  display: flex;
  gap: 15px;
  margin-bottom: 20px;
  position: relative;
  opacity: 0;
  animation: fadeIn 0.3s ease forwards;
}

@keyframes fadeIn {
  to {
    opacity: 1;
  }
}

.timeline-time {
  width: 60px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  text-align: right;
  padding-top: 2px;
  flex-shrink: 0;
}

.timeline-marker {
  position: relative;
  width: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
}

.marker-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #9c27b0;
  border: 2px solid rgba(156, 39, 176, 0.3);
  margin-top: 4px;
  z-index: 1;
}

.marker-line {
  position: absolute;
  top: 18px;
  width: 2px;
  height: calc(100% + 20px);
  background: rgba(156, 39, 176, 0.2);
}

.timeline-item:last-child .marker-line {
  display: none;
}

.timeline-content {
  flex: 1;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  padding: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: all 0.3s ease;
}

.timeline-item:hover .timeline-content {
  transform: translateX(5px);
  border-color: rgba(156, 39, 176, 0.3);
}

.event-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.event-team {
  font-size: 12px;
  font-weight: 500;
}

.event-score {
  font-size: 12px;
  font-weight: bold;
}

.event-attack .event-team {
  color: #f44336;
}

.event-attack .event-score {
  color: #f44336;
}

.event-attack .marker-dot {
  background: #f44336;
  border-color: rgba(244, 67, 54, 0.3);
}

.event-defense .event-team {
  color: #2196f3;
}

.event-defense .event-score {
  color: #2196f3;
}

.event-defense .marker-dot {
  background: #2196f3;
  border-color: rgba(33, 150, 243, 0.3);
}

.event-title {
  font-size: 13px;
  font-weight: 500;
  color: #fff;
  margin-bottom: 4px;
}

.event-desc {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.6);
  line-height: 1.4;
}

.new-event .timeline-content {
  animation: pulse 2s ease infinite;
}

@keyframes pulse {
  0%, 100% {
    box-shadow: 0 0 0 0 rgba(156, 39, 176, 0.4);
  }
  50% {
    box-shadow: 0 0 10px 5px rgba(156, 39, 176, 0.2);
  }
}
</style>