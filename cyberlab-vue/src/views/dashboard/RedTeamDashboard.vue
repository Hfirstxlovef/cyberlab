<template>
  <div class="red-team-dashboard">
    <!-- 动态光晕背景层 -->
    <div class="dynamic-glow-layer">
      <div class="glow-spot glow-1"></div>
      <div class="glow-spot glow-2"></div>
      <div class="glow-spot glow-3"></div>
    </div>

    <div class="dashboard-header">
      <h1>🔴 红队驾驶舱（作战视图面板）</h1>
      <p>攻击态势监控 - 渗透作战指挥中心</p>
    </div>

    <!-- 任务状态卡片 -->
    <div class="mission-cards">
      <el-row :gutter="20">
        <el-col :span="8">
          <el-card class="mission-card">
            <template #header>
              <span>📋 当前演练任务</span>
            </template>
            <div class="mission-info">
              <div class="mission-item">
                <span class="label">靶标IP:</span>
                <span class="value">{{ currentMission.targetIp }}</span>
              </div>
              <div class="mission-item">
                <span class="label">任务状态:</span>
                <el-tag :type="currentMission.status === '进行中' ? 'success' : 'info'">
                  {{ currentMission.status }}
                </el-tag>
              </div>
              <div class="mission-item">
                <span class="label">截止时间:</span>
                <span class="value">{{ currentMission.deadline }}</span>
              </div>
              <div class="mission-item">
                <span class="label">目标状态:</span>
                <el-tag :type="currentMission.targetOnline ? 'success' : 'danger'">
                  {{ currentMission.targetOnline ? '在线' : '离线' }}
                </el-tag>
              </div>
            </div>
          </el-card>
        </el-col>
        
        <el-col :span="8">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon">🎯</div>
              <div class="stat-info">
                <div class="stat-number">{{ attackStats.successCount }}</div>
                <div class="stat-label">提交成功次数</div>
              </div>
            </div>
          </el-card>
        </el-col>
        
        <el-col :span="8">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon">📊</div>
              <div class="stat-info">
                <div class="stat-number">{{ attackStats.successRate }}%</div>
                <div class="stat-label">攻击成功率</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 图表区域 -->
    <div class="charts-section">
      <el-row :gutter="20">
        <!-- 工具使用统计 -->
        <el-col :span="12">
          <el-card class="chart-card">
            <template #header>
              <span>🛠️ 攻击工具使用频率</span>
            </template>
            <div ref="toolChart" class="chart-container"></div>
          </el-card>
        </el-col>
        
        <!-- 漏洞类型分布 -->
        <el-col :span="12">
          <el-card class="chart-card">
            <template #header>
              <span>🔍 漏洞类型与利用率分布</span>
            </template>
            <div ref="vulnChart" class="chart-container"></div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 攻击地图 -->
    <div class="attack-map-section">
      <el-card class="map-card">
        <template #header>
          <span>🗺️ 攻击目标分布图</span>
        </template>
        <div class="attack-map">
          <div class="target-grid">
            <div 
              v-for="target in attackTargets" 
              :key="target.id"
              :class="['target-node', target.status]"
              @click="selectTarget(target)"
            >
              <div class="target-ip">{{ target.ip }}</div>
              <div class="target-status">{{ target.statusText }}</div>
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 快捷操作 -->
    <div class="quick-actions">
      <el-card>
        <template #header>
          <span>🛠️ 快捷功能</span>
        </template>
        <div class="action-buttons">
          <el-button type="danger" size="large" @click="submitReport">
            📝 上传攻击成果报告
          </el-button>
          <el-button type="primary" size="large" @click="viewSubmissions">
            📋 查看提交历史
          </el-button>
          <el-button type="warning" size="large" @click="checkReviewStatus">
            🔍 查看审核状态
          </el-button>
          <el-button type="success" size="large" @click="viewScore">
            🏆 查看队伍战绩
          </el-button>
        </div>
      </el-card>
    </div>

    <!-- 成果历史列表 -->
    <div class="submission-history">
      <el-card>
        <template #header>
          <span>📄 我的成果历史提交</span>
        </template>
        <el-table :data="submissionHistory" style="width: 100%">
          <el-table-column prop="time" label="提交时间" width="180" />
          <el-table-column prop="type" label="漏洞类型" width="120" />
          <el-table-column prop="target" label="攻击目标" width="150" />
          <el-table-column prop="tool" label="使用工具" width="120" />
          <el-table-column prop="status" label="审核状态" width="120">
            <template #default="scope">
              <el-tag :type="getStatusColor(scope.row.status)">{{ scope.row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="score" label="得分" width="80" />
          <el-table-column label="操作">
            <template #default="scope">
              <el-button size="small" @click="viewDetail(scope.row)">查看详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'

const router = useRouter()

// 数据状态
const currentMission = ref({
  targetIp: '192.168.1.100',
  status: '进行中',
  deadline: '2024-01-15 18:00:00',
  targetOnline: true
})

const attackStats = ref({
  successCount: 12,
  totalAttempts: 18,
  successRate: 67
})

const attackTargets = ref([
  { id: 1, ip: '192.168.1.100', status: 'compromised', statusText: '已攻破' },
  { id: 2, ip: '192.168.1.101', status: 'attacking', statusText: '攻击中' },
  { id: 3, ip: '192.168.1.102', status: 'pending', statusText: '待攻击' },
  { id: 4, ip: '192.168.1.103', status: 'failed', statusText: '攻击失败' },
  { id: 5, ip: '192.168.1.104', status: 'pending', statusText: '待攻击' },
  { id: 6, ip: '192.168.1.105', status: 'compromised', statusText: '已攻破' }
])

const submissionHistory = ref([
  {
    time: '2024-01-15 14:30:25',
    type: 'SQL注入',
    target: '192.168.1.100',
    tool: 'SQLMap',
    status: '已通过',
    score: 85
  },
  {
    time: '2024-01-15 13:45:10',
    type: 'XSS',
    target: '192.168.1.101',
    tool: 'BeEF',
    status: '待审核',
    score: '-'
  },
  {
    time: '2024-01-15 12:20:30',
    type: '文件上传',
    target: '192.168.1.102',
    tool: '手工测试',
    status: '已驳回',
    score: 0
  }
])

// 图表引用
const toolChart = ref(null)
const vulnChart = ref(null)

// 初始化工具使用图表
const initToolChart = () => {
  const chart = echarts.init(toolChart.value)
  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(10, 10, 10, 0.95)',
      borderColor: 'rgba(255, 59, 48, 0.3)',
      borderWidth: 1,
      textStyle: {
        color: '#ffffff',
        fontSize: 13,
        fontFamily: '-apple-system, BlinkMacSystemFont, "SF Pro Display"'
      }
    },
    legend: {
      bottom: 10,
      left: 'center',
      textStyle: {
        color: 'rgba(255, 255, 255, 0.8)',
        fontSize: 12
      }
    },
    series: [
      {
        name: '工具使用频率',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['50%', '45%'],
        data: [
          { value: 35, name: 'Nmap' },
          { value: 25, name: 'SQLMap' },
          { value: 20, name: 'Metasploit' },
          { value: 15, name: 'Burp Suite' },
          { value: 5, name: '其他工具' }
        ],
        itemStyle: {
          borderRadius: 8,
          borderColor: 'rgba(20, 20, 20, 0.6)',
          borderWidth: 2
        },
        label: {
          color: 'rgba(255, 255, 255, 0.9)',
          fontSize: 13,
          fontWeight: 500,
          formatter: '{b}: {c}次'
        },
        labelLine: {
          lineStyle: {
            color: 'rgba(255, 255, 255, 0.3)'
          }
        },
        emphasis: {
          itemStyle: {
            shadowBlur: 20,
            shadowOffsetX: 0,
            shadowColor: 'rgba(255, 59, 48, 0.5)'
          }
        },
        color: ['#ff3b30', '#ff9500', '#ffcc00', '#34c759', '#007aff']
      }
    ]
  }
  chart.setOption(option)

  // 添加窗口大小变化监听
  window.addEventListener('resize', () => {
    chart.resize()
  })
}

// 初始化漏洞类型图表
const initVulnChart = () => {
  const chart = echarts.init(vulnChart.value)
  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow',
        shadowStyle: {
          color: 'rgba(255, 59, 48, 0.1)'
        }
      },
      backgroundColor: 'rgba(10, 10, 10, 0.95)',
      borderColor: 'rgba(255, 59, 48, 0.3)',
      borderWidth: 1,
      textStyle: {
        color: '#ffffff',
        fontSize: 13,
        fontFamily: '-apple-system, BlinkMacSystemFont, "SF Pro Display"'
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: ['SQL注入', 'XSS', '文件上传', 'CSRF', '权限提升', '信息泄露'],
      axisLine: {
        lineStyle: {
          color: 'rgba(255, 255, 255, 0.2)'
        }
      },
      axisLabel: {
        color: 'rgba(255, 255, 255, 0.8)',
        fontSize: 12,
        fontFamily: '-apple-system, BlinkMacSystemFont, "SF Pro Text"'
      },
      axisTick: {
        show: false
      }
    },
    yAxis: {
      type: 'value',
      name: '利用成功率(%)',
      nameTextStyle: {
        color: 'rgba(255, 255, 255, 0.7)',
        fontSize: 12,
        padding: [0, 0, 0, -40]
      },
      max: 100,
      axisLine: {
        show: false
      },
      axisLabel: {
        color: 'rgba(255, 255, 255, 0.6)',
        fontSize: 11
      },
      splitLine: {
        lineStyle: {
          color: 'rgba(255, 255, 255, 0.08)',
          type: 'dashed'
        }
      }
    },
    series: [
      {
        name: '成功率',
        type: 'bar',
        data: [85, 70, 60, 45, 30, 55],
        barWidth: '50%',
        itemStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: '#ff6b59' },
              { offset: 1, color: '#ff3b30' }
            ]
          },
          borderRadius: [6, 6, 0, 0],
          shadowColor: 'rgba(255, 59, 48, 0.3)',
          shadowBlur: 10
        },
        emphasis: {
          itemStyle: {
            shadowBlur: 20,
            shadowColor: 'rgba(255, 59, 48, 0.5)'
          }
        },
        label: {
          show: true,
          position: 'top',
          color: 'rgba(255, 255, 255, 0.9)',
          fontSize: 12,
          fontWeight: 600,
          formatter: '{c}%'
        }
      }
    ]
  }
  chart.setOption(option)

  // 添加窗口大小变化监听
  window.addEventListener('resize', () => {
    chart.resize()
  })
}

// 工具函数
const getStatusColor = (status) => {
  const colorMap = {
    '已通过': 'success',
    '待审核': 'warning',
    '已驳回': 'danger'
  }
  return colorMap[status] || 'info'
}

// 操作函数
const selectTarget = (target) => {
  ElMessage.info(`选择目标: ${target.ip} (${target.statusText})`)
}

const submitReport = () => {
  router.push('/achievement/red-team-submit')
}

const viewSubmissions = () => {
  ElMessage.info('查看提交历史')
}

const checkReviewStatus = () => {
  ElMessage.info('查看审核状态')
}

const viewScore = () => {
  ElMessage.info('查看队伍战绩')
}

const viewDetail = (row) => {
  ElMessage.info(`查看详情: ${row.type} - ${row.target}`)
}

onMounted(() => {
  nextTick(() => {
    initToolChart()
    initVulnChart()
  })
})
</script>

<style scoped>
/* ============================================
   红队驾驶舱 - 深色科技风 + Apple优雅
   ============================================ */

:root {
  --hacker-bg: #0a0a0a;
  --hacker-bg-secondary: #1a0d0d;
  --hacker-red: #ff3b30;
  --hacker-red-glow: rgba(255, 59, 48, 0.3);
  --hacker-text: #ffffff;
  --hacker-text-secondary: rgba(255, 255, 255, 0.7);
  --hacker-glass: rgba(20, 20, 20, 0.6);
  --font-apple: -apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", "Helvetica Neue", sans-serif;
  --font-mono: "SF Mono", Consolas, Monaco, monospace;
}

.red-team-dashboard {
  min-height: 100vh;
  background: linear-gradient(135deg, #0a0a0a 0%, #1a0d0d 50%, #0f0f0f 100%);
  color: var(--hacker-text);
  font-family: var(--font-apple);
  padding: 30px;
  position: relative;
  overflow-x: hidden;
}

/* 动态光晕背景层 */
.dynamic-glow-layer {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
}

.glow-spot {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.3;
}

.glow-1 {
  top: 15%;
  left: 25%;
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(255, 59, 48, 0.2) 0%, transparent 70%);
  animation: glow-breath-1 8s ease-in-out infinite;
}

.glow-2 {
  bottom: 20%;
  right: 15%;
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, rgba(204, 0, 0, 0.15) 0%, transparent 70%);
  animation: glow-breath-2 10s ease-in-out infinite;
  animation-delay: 2s;
}

.glow-3 {
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

/* 页面标题 */
.dashboard-header {
  margin-bottom: 40px;
  text-align: center;
  position: relative;
  z-index: 1;
}

.dashboard-header h1 {
  font-size: 36px;
  font-weight: 700;
  margin: 0 0 12px 0;
  background: linear-gradient(135deg, #ff3b30 0%, #ff6b59 100%);
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  letter-spacing: -0.5px;
  text-shadow: 0 0 40px var(--hacker-red-glow);
  filter: drop-shadow(0 0 25px var(--hacker-red-glow));
}

.dashboard-header p {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.7);
  margin: 0;
  font-weight: 500;
  letter-spacing: 0.5px;
}

/* 任务卡片区域 */
.mission-cards {
  margin-bottom: 30px;
  position: relative;
  z-index: 1;
}

/* Element Plus 深色主题覆盖 */
.mission-cards :deep(.el-card),
.charts-section :deep(.el-card),
.attack-map-section :deep(.el-card),
.quick-actions :deep(.el-card),
.submission-history :deep(.el-card) {
  background: var(--hacker-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 16px;
  border: 0.5px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4), inset 0 0 0 0.5px rgba(255, 255, 255, 0.05);
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
  color: var(--hacker-text);
}

.mission-cards :deep(.el-card):hover,
.charts-section :deep(.el-card):hover,
.attack-map-section :deep(.el-card):hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 48px rgba(255, 59, 48, 0.2), 0 0 30px rgba(255, 59, 48, 0.1);
  border-color: rgba(255, 59, 48, 0.3);
}

.mission-cards :deep(.el-card__header),
.charts-section :deep(.el-card__header),
.attack-map-section :deep(.el-card__header),
.quick-actions :deep(.el-card__header),
.submission-history :deep(.el-card__header) {
  background: rgba(30, 30, 30, 0.5);
  border-bottom: 1px solid rgba(255, 59, 48, 0.2);
  color: #ffffff;
  font-weight: 600;
  padding: 16px 20px;
  font-size: 15px;
}

.mission-cards :deep(.el-card__header span),
.charts-section :deep(.el-card__header span),
.attack-map-section :deep(.el-card__header span),
.quick-actions :deep(.el-card__header span),
.submission-history :deep(.el-card__header span) {
  color: #ffffff;
}

.mission-cards :deep(.el-card__body) {
  color: var(--hacker-text-secondary);
}

/* 任务信息 */
.mission-card {
  position: relative;
}

.mission-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  width: 3px;
  height: 100%;
  background: linear-gradient(180deg, rgba(255, 59, 48, 0.8) 0%, rgba(255, 59, 48, 0.3) 100%);
  border-radius: 16px 0 0 16px;
}

.mission-info {
  padding: 10px 0;
}

.mission-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.mission-item:last-child {
  margin-bottom: 0;
}

.label {
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  font-size: 14px;
}

.value {
  color: rgba(255, 255, 255, 0.7);
  font-family: var(--font-mono);
  font-size: 13px;
}

/* Element Plus Tag 深色主题 */
.mission-item :deep(.el-tag) {
  background: rgba(52, 199, 89, 0.2);
  border: 0.5px solid rgba(52, 199, 89, 0.4);
  color: #34c759;
  font-weight: 600;
  padding: 4px 12px;
  font-size: 12px;
}

.mission-item :deep(.el-tag.el-tag--info) {
  background: rgba(0, 122, 255, 0.2);
  border-color: rgba(0, 122, 255, 0.4);
  color: #007aff;
}

.mission-item :deep(.el-tag.el-tag--danger) {
  background: rgba(255, 59, 48, 0.2);
  border-color: rgba(255, 59, 48, 0.4);
  color: #ff3b30;
}

/* 统计卡片 */
.stat-card {
  position: relative;
  overflow: hidden;
}

.stat-card::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(255, 59, 48, 0.05) 0%, transparent 70%);
  opacity: 0;
  transition: opacity 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

.stat-card:hover::before {
  opacity: 1;
}

.stat-content {
  display: flex;
  align-items: center;
  padding: 24px;
  position: relative;
  z-index: 1;
}

.stat-icon {
  font-size: 42px;
  margin-right: 20px;
  filter: drop-shadow(0 0 15px rgba(255, 59, 48, 0.4));
}

.stat-info {
  flex: 1;
}

.stat-number {
  font-size: 32px;
  font-weight: 700;
  color: #ffffff;
  margin-bottom: 6px;
  font-family: var(--font-mono);
  letter-spacing: -0.5px;
  text-shadow: 0 0 20px var(--hacker-red-glow);
}

.stat-label {
  color: rgba(255, 255, 255, 0.6);
  font-size: 14px;
  font-weight: 500;
}

/* 图表区域 */
.charts-section {
  margin-bottom: 30px;
  position: relative;
  z-index: 1;
}

.chart-card {
  position: relative;
}

.chart-card :deep(.el-card__header) {
  position: relative;
}

.chart-card :deep(.el-card__header)::after {
  content: '';
  position: absolute;
  left: 20px;
  bottom: 0;
  width: 50px;
  height: 2px;
  background: linear-gradient(90deg, #ff3b30 0%, transparent 100%);
  box-shadow: 0 0 10px rgba(255, 59, 48, 0.5);
}

.chart-container {
  height: 320px;
}

/* 攻击地图 */
.attack-map-section {
  margin-bottom: 30px;
  position: relative;
  z-index: 1;
}

.map-card :deep(.el-card__header)::after {
  content: '';
  position: absolute;
  left: 20px;
  bottom: 0;
  width: 50px;
  height: 2px;
  background: linear-gradient(90deg, #ff3b30 0%, transparent 100%);
  box-shadow: 0 0 10px rgba(255, 59, 48, 0.5);
}

.attack-map {
  padding: 10px 0;
}

.target-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 18px;
  padding: 20px;
}

.target-node {
  padding: 20px;
  border-radius: 12px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
  background: rgba(30, 30, 30, 0.5);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid;
  position: relative;
  overflow: hidden;
}

.target-node::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 3px;
  height: 100%;
  opacity: 0;
  transition: opacity 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

.target-node:hover {
  transform: translateY(-4px);
}

.target-node:hover::before {
  opacity: 1;
}

.target-node.compromised {
  border-color: rgba(52, 199, 89, 0.4);
  box-shadow: 0 4px 16px rgba(52, 199, 89, 0.15);
}

.target-node.compromised::before {
  background: linear-gradient(180deg, #34c759 0%, rgba(52, 199, 89, 0.3) 100%);
}

.target-node.compromised:hover {
  box-shadow: 0 8px 28px rgba(52, 199, 89, 0.3);
  border-color: rgba(52, 199, 89, 0.6);
}

.target-node.attacking {
  border-color: rgba(255, 59, 48, 0.5);
  box-shadow: 0 4px 16px rgba(255, 59, 48, 0.2);
  animation: pulse-attack 2s cubic-bezier(0.19, 1, 0.22, 1) infinite;
}

.target-node.attacking::before {
  background: linear-gradient(180deg, #ff3b30 0%, rgba(255, 59, 48, 0.3) 100%);
  opacity: 1;
}

@keyframes pulse-attack {
  0%, 100% {
    box-shadow: 0 4px 16px rgba(255, 59, 48, 0.2);
    border-color: rgba(255, 59, 48, 0.5);
  }
  50% {
    box-shadow: 0 8px 28px rgba(255, 59, 48, 0.4);
    border-color: rgba(255, 59, 48, 0.7);
  }
}

.target-node.pending {
  border-color: rgba(142, 142, 147, 0.3);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
}

.target-node.pending::before {
  background: linear-gradient(180deg, #8e8e93 0%, rgba(142, 142, 147, 0.3) 100%);
}

.target-node.pending:hover {
  box-shadow: 0 8px 28px rgba(142, 142, 147, 0.2);
  border-color: rgba(142, 142, 147, 0.5);
}

.target-node.failed {
  border-color: rgba(220, 53, 69, 0.4);
  box-shadow: 0 4px 16px rgba(220, 53, 69, 0.15);
}

.target-node.failed::before {
  background: linear-gradient(180deg, #dc3545 0%, rgba(220, 53, 69, 0.3) 100%);
}

.target-node.failed:hover {
  box-shadow: 0 8px 28px rgba(220, 53, 69, 0.25);
  border-color: rgba(220, 53, 69, 0.6);
}

.target-ip {
  font-weight: 700;
  margin-bottom: 10px;
  font-size: 15px;
  color: rgba(255, 255, 255, 0.95);
  font-family: var(--font-mono);
  letter-spacing: 0.5px;
}

.target-status {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  font-weight: 500;
}

/* 快捷操作 */
.quick-actions {
  margin-bottom: 30px;
  position: relative;
  z-index: 1;
}

.action-buttons {
  display: flex;
  gap: 18px;
  flex-wrap: wrap;
  padding: 10px 0;
}

.action-buttons :deep(.el-button) {
  flex: 1;
  min-width: 200px;
  background: linear-gradient(135deg, rgba(255, 59, 48, 0.15) 0%, rgba(255, 59, 48, 0.25) 100%);
  border: 0.5px solid rgba(255, 59, 48, 0.3);
  color: #ffffff;
  font-weight: 600;
  font-size: 14px;
  padding: 14px 24px;
  border-radius: 10px;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
  box-shadow: 0 4px 12px rgba(255, 59, 48, 0.1);
}

.action-buttons :deep(.el-button:hover) {
  background: linear-gradient(135deg, rgba(255, 59, 48, 0.25) 0%, rgba(255, 59, 48, 0.35) 100%);
  border-color: rgba(255, 59, 48, 0.5);
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(255, 59, 48, 0.25);
}

.action-buttons :deep(.el-button:active) {
  transform: translateY(-1px);
}

.action-buttons :deep(.el-button.el-button--danger) {
  background: linear-gradient(135deg, rgba(255, 59, 48, 0.2) 0%, rgba(255, 59, 48, 0.3) 100%);
}

.action-buttons :deep(.el-button.el-button--primary) {
  background: linear-gradient(135deg, rgba(0, 122, 255, 0.15) 0%, rgba(0, 122, 255, 0.25) 100%);
  border-color: rgba(0, 122, 255, 0.3);
}

.action-buttons :deep(.el-button.el-button--primary:hover) {
  background: linear-gradient(135deg, rgba(0, 122, 255, 0.25) 0%, rgba(0, 122, 255, 0.35) 100%);
  border-color: rgba(0, 122, 255, 0.5);
  box-shadow: 0 8px 24px rgba(0, 122, 255, 0.25);
}

.action-buttons :deep(.el-button.el-button--warning) {
  background: linear-gradient(135deg, rgba(255, 149, 0, 0.15) 0%, rgba(255, 149, 0, 0.25) 100%);
  border-color: rgba(255, 149, 0, 0.3);
}

.action-buttons :deep(.el-button.el-button--warning:hover) {
  background: linear-gradient(135deg, rgba(255, 149, 0, 0.25) 0%, rgba(255, 149, 0, 0.35) 100%);
  border-color: rgba(255, 149, 0, 0.5);
  box-shadow: 0 8px 24px rgba(255, 149, 0, 0.25);
}

.action-buttons :deep(.el-button.el-button--success) {
  background: linear-gradient(135deg, rgba(52, 199, 89, 0.15) 0%, rgba(52, 199, 89, 0.25) 100%);
  border-color: rgba(52, 199, 89, 0.3);
}

.action-buttons :deep(.el-button.el-button--success:hover) {
  background: linear-gradient(135deg, rgba(52, 199, 89, 0.25) 0%, rgba(52, 199, 89, 0.35) 100%);
  border-color: rgba(52, 199, 89, 0.5);
  box-shadow: 0 8px 24px rgba(52, 199, 89, 0.25);
}

/* 成果历史 */
.submission-history {
  margin-bottom: 30px;
  position: relative;
  z-index: 1;
}

.submission-history :deep(.el-table) {
  background: transparent;
  color: var(--hacker-text);
}

.submission-history :deep(.el-table__inner-wrapper)::before {
  display: none;
}

.submission-history :deep(.el-table th.el-table__cell) {
  background: rgba(30, 30, 30, 0.6);
  color: rgba(255, 255, 255, 0.9);
  font-weight: 600;
  border-bottom: 1px solid rgba(255, 59, 48, 0.2);
}

.submission-history :deep(.el-table tr) {
  background: transparent;
}

.submission-history :deep(.el-table td.el-table__cell) {
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.8);
}

.submission-history :deep(.el-table__body tr:hover > td) {
  background: rgba(255, 59, 48, 0.08) !important;
}

.submission-history :deep(.el-table__empty-block) {
  background: transparent;
}

.submission-history :deep(.el-button--small) {
  background: linear-gradient(135deg, rgba(0, 122, 255, 0.15) 0%, rgba(0, 122, 255, 0.25) 100%);
  border: 0.5px solid rgba(0, 122, 255, 0.3);
  color: #ffffff;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

.submission-history :deep(.el-button--small:hover) {
  background: linear-gradient(135deg, rgba(0, 122, 255, 0.25) 0%, rgba(0, 122, 255, 0.35) 100%);
  border-color: rgba(0, 122, 255, 0.5);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.2);
}

.submission-history :deep(.el-tag.el-tag--success) {
  background: rgba(52, 199, 89, 0.2);
  border-color: rgba(52, 199, 89, 0.4);
  color: #34c759;
}

.submission-history :deep(.el-tag.el-tag--warning) {
  background: rgba(255, 149, 0, 0.2);
  border-color: rgba(255, 149, 0, 0.4);
  color: #ff9500;
}

.submission-history :deep(.el-tag.el-tag--danger) {
  background: rgba(255, 59, 48, 0.2);
  border-color: rgba(255, 59, 48, 0.4);
  color: #ff3b30;
}
</style>