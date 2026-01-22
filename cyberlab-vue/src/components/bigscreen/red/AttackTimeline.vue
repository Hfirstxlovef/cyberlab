<template>
  <div class="attack-timeline">
    <h3 class="chart-title">攻击时间线</h3>
    <div class="timeline-container">
      <div class="timeline-item" v-for="(event, index) in timelineEvents" :key="index">
        <div class="timeline-marker" :class="`marker-${event.type}`"></div>
        <div class="timeline-content">
          <div class="event-time">{{ event.time }}</div>
          <div class="event-title">{{ event.title }}</div>
          <div class="event-detail">{{ event.detail }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  data: {
    type: Array,
    default: () => []
  }
})

const timelineEvents = computed(() => props.data.length > 0 ? props.data : [
  {
    time: '23:45:12',
    type: 'success',
    title: '成功获取系统权限',
    detail: '目标: 192.168.1.100 - Web服务器'
  },
  {
    time: '23:32:08',
    type: 'exploit',
    title: '发现SQL注入漏洞',
    detail: '位置: /api/login - 参数: username'
  },
  {
    time: '23:15:30',
    type: 'scan',
    title: '端口扫描完成',
    detail: '发现开放端口: 80, 443, 8080, 3306'
  },
  {
    time: '22:58:45',
    type: 'recon',
    title: '信息收集',
    detail: '识别目标系统: nginx/1.18.0, PHP/7.4'
  },
  {
    time: '22:30:00',
    type: 'start',
    title: '演练开始',
    detail: '目标网络: 192.168.1.0/24'
  }
])
</script>

<style scoped>
.attack-timeline {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.chart-title {
  margin: 0 0 15px 0;
  font-size: 18px;
  color: #ab47bc;
  font-weight: 500;
}

.timeline-container {
  flex: 1;
  overflow-y: auto;
  padding-left: 20px;
  position: relative;
}

.timeline-container::before {
  content: '';
  position: absolute;
  left: 8px;
  top: 0;
  bottom: 0;
  width: 2px;
  background: rgba(255, 255, 255, 0.1);
}

.timeline-item {
  position: relative;
  padding-left: 30px;
  margin-bottom: 20px;
}

.timeline-marker {
  position: absolute;
  left: -12px;
  top: 0;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 2px solid;
  background: #1a2332;
}

.marker-success {
  border-color: #4caf50;
  box-shadow: 0 0 10px rgba(76, 175, 80, 0.5);
}

.marker-exploit {
  border-color: #ff9800;
  box-shadow: 0 0 10px rgba(255, 152, 0, 0.5);
}

.marker-scan {
  border-color: #2196f3;
  box-shadow: 0 0 10px rgba(33, 150, 243, 0.5);
}

.marker-recon {
  border-color: #9c27b0;
  box-shadow: 0 0 10px rgba(156, 39, 176, 0.5);
}

.marker-start {
  border-color: #607d8b;
  box-shadow: 0 0 10px rgba(96, 125, 139, 0.5);
}

.timeline-content {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 6px;
  padding: 10px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.event-time {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
  margin-bottom: 4px;
  font-family: monospace;
}

.event-title {
  font-size: 13px;
  color: #fff;
  font-weight: 500;
  margin-bottom: 4px;
}

.event-detail {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}
</style>