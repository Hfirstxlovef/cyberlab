<template>
  <div class="blue-team-dashboard">
    <!-- 四角装饰框架 -->
    <div class="page-corner-frame corner-frame-tl"></div>
    <div class="page-corner-frame corner-frame-tr"></div>
    <div class="page-corner-frame corner-frame-bl"></div>
    <div class="page-corner-frame corner-frame-br"></div>

    <!-- 动态光晕背景层 -->
    <div class="dynamic-glow-layer">
      <div class="glow-spot glow-blue-1"></div>
      <div class="glow-spot glow-blue-2"></div>
      <div class="glow-spot glow-blue-3"></div>
    </div>

    <div class="dashboard-header">
      <!-- 左上角装饰 -->
      <div class="corner-decoration corner-top-left"></div>
      <div class="corner-decoration corner-top-right"></div>

      <!-- 左侧装饰线 -->
      <div class="header-decoration-left"></div>

      <div class="header-content">
        <div class="title-section">
          <h1>🔐 蓝队驾驶舱（防御态势面板）</h1>
          <div class="title-underline"></div>
          <p>防御态势监控 - 网络安全防护指挥中心</p>
        </div>
      </div>

      <!-- 右侧装饰线 -->
      <div class="header-decoration-right"></div>
    </div>

    <!-- 防御统计卡片 -->
    <div class="defense-stats">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon">🛡️</div>
              <div class="stat-info">
                <div class="stat-number">{{ defenseStats.blockedAttacks }}</div>
                <div class="stat-label">今日拦截次数</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon">💻</div>
              <div class="stat-info">
                <div class="stat-number">{{ defenseStats.protectedAssets }}</div>
                <div class="stat-label">被攻击资产数</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon">📊</div>
              <div class="stat-info">
                <div class="stat-number">{{ defenseStats.successRate }}%</div>
                <div class="stat-label">防御成功率</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon">🏆</div>
              <div class="stat-info">
                <div class="stat-number">{{ defenseStats.teamScore }}</div>
                <div class="stat-label">当前得分</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 图表区域 -->
    <div class="charts-section">
      <el-row :gutter="20">
        <!-- 攻击来源分析 -->
        <el-col :span="12">
          <el-card class="chart-card">
            <template #header>
              <span>📊 攻击来源统计</span>
            </template>
            <div ref="attackSourceChart" class="chart-container"></div>
          </el-card>
        </el-col>
        
        <!-- 防御趋势 -->
        <el-col :span="12">
          <el-card class="chart-card">
            <template #header>
              <span>📈 防御成功趋势</span>
            </template>
            <div ref="defenseTrendChart" class="chart-container"></div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 系统监控面板 -->
    <div class="monitoring-section">
      <el-card class="monitoring-card">
        <template #header>
          <span>💻 重要系统状态监控</span>
        </template>
        <div class="system-grid">
          <div 
            v-for="system in monitoredSystems" 
            :key="system.id"
            :class="['system-node', system.status]"
          >
            <div class="system-name">{{ system.name }}</div>
            <div class="system-ip">{{ system.ip }}</div>
            <div class="system-status">
              <el-tag :type="getSystemStatusColor(system.status)">
                {{ system.statusText }}
              </el-tag>
            </div>
            <div class="system-metrics">
              <div>CPU: {{ system.cpu }}%</div>
              <div>内存: {{ system.memory }}%</div>
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 快捷操作 -->
    <div class="quick-actions">
      <el-card>
        <template #header>
          <span>⛑️ 快捷功能</span>
        </template>
        <div class="action-buttons">
          <el-button type="primary" size="large" @click="submitDefenseRecord">
            📝 提交拦截记录
          </el-button>
          <el-button type="success" size="large" @click="downloadLogs">
            📥 下载攻击日志样本
          </el-button>
          <el-button type="warning" size="large" @click="viewScore">
            🏆 查看本队评分进度
          </el-button>
          <el-button type="info" size="large" @click="analyzeAttackPath">
            🔍 溯源分析路径
          </el-button>
        </div>
      </el-card>
    </div>

    <!-- 攻击日志和防御记录 -->
    <div class="logs-section">
      <el-row :gutter="20">
        <!-- 攻击日志 -->
        <el-col :span="12">
          <el-card>
            <template #header>
              <span>🚨 攻击日志</span>
            </template>
            <el-table :data="attackLogs" style="width: 100%" size="small">
              <el-table-column prop="time" label="时间" width="120" />
              <el-table-column prop="sourceIp" label="攻击源" width="120" />
              <el-table-column prop="targetIp" label="目标" width="120" />
              <el-table-column prop="attackType" label="攻击类型" />
              <el-table-column prop="status" label="状态" width="80">
                <template #default="scope">
                  <el-tag :type="scope.row.status === '已拦截' ? 'success' : 'danger'" size="small">
                    {{ scope.row.status }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
        
        <!-- 防御记录 -->
        <el-col :span="12">
          <el-card>
            <template #header>
              <span>🛡️ 防御记录</span>
            </template>
            <el-table :data="defenseRecords" style="width: 100%" size="small">
              <el-table-column prop="time" label="时间" width="120" />
              <el-table-column prop="action" label="防御动作" />
              <el-table-column prop="target" label="保护目标" width="120" />
              <el-table-column prop="result" label="结果" width="80">
                <template #default="scope">
                  <el-tag :type="scope.row.result === '成功' ? 'success' : 'warning'" size="small">
                    {{ scope.row.result }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 溯源分析 -->
    <div class="trace-analysis">
      <el-card>
        <template #header>
          <span>🔍 溯源分析路径可视化</span>
        </template>
        <div class="trace-path">
          <div class="trace-step" v-for="(step, index) in tracePath" :key="index">
            <div class="step-number">{{ index + 1 }}</div>
            <div class="step-content">
              <div class="step-title">{{ step.title }}</div>
              <div class="step-detail">{{ step.detail }}</div>
              <div class="step-time">{{ step.time }}</div>
            </div>
            <div class="step-arrow" v-if="index < tracePath.length - 1">→</div>
          </div>
        </div>
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
const defenseStats = ref({
  blockedAttacks: 45,
  protectedAssets: 12,
  successRate: 89,
  teamScore: 342
})

const monitoredSystems = ref([
  {
    id: 1,
    name: 'Web服务器',
    ip: '192.168.1.10',
    status: 'normal',
    statusText: '正常',
    cpu: 45,
    memory: 62
  },
  {
    id: 2,
    name: '数据库服务器',
    ip: '192.168.1.20',
    status: 'warning',
    statusText: '警告',
    cpu: 78,
    memory: 85
  },
  {
    id: 3,
    name: '文件服务器',
    ip: '192.168.1.30',
    status: 'normal',
    statusText: '正常',
    cpu: 32,
    memory: 45
  },
  {
    id: 4,
    name: '邮件服务器',
    ip: '192.168.1.40',
    status: 'critical',
    statusText: '严重',
    cpu: 95,
    memory: 98
  }
])

const attackLogs = ref([
  {
    time: '14:30:25',
    sourceIp: '10.0.0.100',
    targetIp: '192.168.1.10',
    attackType: 'SQL注入',
    status: '已拦截'
  },
  {
    time: '14:28:15',
    sourceIp: '10.0.0.101',
    targetIp: '192.168.1.20',
    attackType: 'XSS攻击',
    status: '已拦截'
  },
  {
    time: '14:25:10',
    sourceIp: '10.0.0.102',
    targetIp: '192.168.1.30',
    attackType: '文件上传',
    status: '攻击成功'
  }
])

const defenseRecords = ref([
  {
    time: '14:30:30',
    action: '防火墙拦截',
    target: '192.168.1.10',
    result: '成功'
  },
  {
    time: '14:28:20',
    action: 'WAF过滤',
    target: '192.168.1.20',
    result: '成功'
  },
  {
    time: '14:25:15',
    action: '入侵检测',
    target: '192.168.1.30',
    result: '延迟'
  }
])

const tracePath = ref([
  {
    title: '攻击发起',
    detail: '来源IP: 10.0.0.100',
    time: '14:30:20'
  },
  {
    title: '端口扫描',
    detail: '扫描目标: 192.168.1.10:80',
    time: '14:30:22'
  },
  {
    title: '漏洞探测',
    detail: 'SQL注入测试',
    time: '14:30:24'
  },
  {
    title: '防御响应',
    detail: 'WAF规则触发',
    time: '14:30:25'
  },
  {
    title: '攻击拦截',
    detail: '连接已断开',
    time: '14:30:26'
  }
])

// 图表引用
const attackSourceChart = ref(null)
const defenseTrendChart = ref(null)

// 初始化攻击来源图表
const initAttackSourceChart = () => {
  const chart = echarts.init(attackSourceChart.value)
  const option = {
    tooltip: {
      trigger: 'item'
    },
    series: [
      {
        name: '攻击来源',
        type: 'pie',
        radius: '60%',
        data: [
          { value: 40, name: '外部网络' },
          { value: 30, name: '内部网络' },
          { value: 20, name: 'DMZ区域' },
          { value: 10, name: '未知来源' }
        ],
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }
    ]
  }
  chart.setOption(option)
}

// 初始化防御趋势图表
const initDefenseTrendChart = () => {
  const chart = echarts.init(defenseTrendChart.value)
  const option = {
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['攻击次数', '拦截次数'],
      bottom: 0
    },
    xAxis: {
      type: 'category',
      data: ['09:00', '10:00', '11:00', '12:00', '13:00', '14:00', '15:00']
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '攻击次数',
        type: 'line',
        data: [8, 12, 15, 10, 18, 22, 16],
        itemStyle: { color: '#ff4757' }
      },
      {
        name: '拦截次数',
        type: 'line',
        data: [7, 11, 13, 9, 16, 20, 15],
        itemStyle: { color: '#2ed573' }
      }
    ]
  }
  chart.setOption(option)
}

// 工具函数
const getSystemStatusColor = (status) => {
  const colorMap = {
    'normal': 'success',
    'warning': 'warning',
    'critical': 'danger'
  }
  return colorMap[status] || 'info'
}

// 操作函数
const submitDefenseRecord = () => {
  router.push('/achievement/blue-team-submit')
}

const downloadLogs = () => {
  ElMessage.success('攻击日志样本下载中...')
}

const viewScore = () => {
  ElMessage.info('查看本队评分进度')
}

const analyzeAttackPath = () => {
  ElMessage.info('启动溯源分析')
}

onMounted(() => {
  nextTick(() => {
    initAttackSourceChart()
    initDefenseTrendChart()
  })
})
</script>

<style scoped>
/* ============================================
   蓝队防御仪表盘 - Blue Team Defense Dashboard
   专业安全运营中心主题 - Professional SOC Theme
   ============================================ */

/* CSS Variables - 蓝队防御主题 */
:root {
  --blue-defender-bg: #0a1428;
  --blue-defender-bg-secondary: #0d1a2d;
  --blue-steel: #4682b4;
  --blue-electric: #1e90ff;
  --blue-cyan: #00d4ff;
  --blue-ice: #a8d8ea;
  --blue-glow-primary: rgba(70, 130, 180, 0.3);
  --blue-glow-accent: rgba(30, 144, 255, 0.25);
  --blue-glow-cyan: rgba(0, 212, 255, 0.2);
  --radius-sm: 12px;
  --radius-md: 16px;
  --radius-lg: 20px;
  --spacing-xs: 8px;
  --spacing-sm: 12px;
  --spacing-md: 16px;
  --spacing-lg: 24px;
  --spacing-xl: 32px;
  --font-apple: -apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", "Helvetica Neue", sans-serif;
  --font-mono: "SF Mono", Consolas, Monaco, monospace;
}

.blue-team-dashboard {
  min-height: 100vh;
  background:
    /* 微妙的网格纹理 */
    repeating-linear-gradient(
      0deg,
      rgba(70, 130, 180, 0.02) 0px,
      transparent 1px,
      transparent 40px,
      rgba(70, 130, 180, 0.02) 41px
    ),
    repeating-linear-gradient(
      90deg,
      rgba(70, 130, 180, 0.02) 0px,
      transparent 1px,
      transparent 40px,
      rgba(70, 130, 180, 0.02) 41px
    ),
    /* 主背景渐变 */
    linear-gradient(135deg, #0a1428 0%, #0d1a2d 50%, #0f1620 100%);
  padding: var(--spacing-lg);
  font-family: var(--font-apple);
  color: #ffffff;
  position: relative;
  overflow-x: hidden;
}

/* 四角装饰框架 */
.page-corner-frame {
  position: fixed;
  width: 80px;
  height: 80px;
  z-index: 100;
  pointer-events: none;
}

.corner-frame-tl {
  top: 20px;
  left: 20px;
  border-top: 3px solid rgba(70, 130, 180, 0.5);
  border-left: 3px solid rgba(70, 130, 180, 0.5);
  border-top-left-radius: 8px;
  box-shadow: 0 0 20px rgba(70, 130, 180, 0.3),
              inset 0 0 20px rgba(70, 130, 180, 0.1);
  animation: corner-pulse 4s ease-in-out infinite;
}

.corner-frame-tr {
  top: 20px;
  right: 20px;
  border-top: 3px solid rgba(70, 130, 180, 0.5);
  border-right: 3px solid rgba(70, 130, 180, 0.5);
  border-top-right-radius: 8px;
  box-shadow: 0 0 20px rgba(70, 130, 180, 0.3),
              inset 0 0 20px rgba(70, 130, 180, 0.1);
  animation: corner-pulse 4s ease-in-out infinite;
  animation-delay: 1s;
}

.corner-frame-bl {
  bottom: 20px;
  left: 20px;
  border-bottom: 3px solid rgba(70, 130, 180, 0.5);
  border-left: 3px solid rgba(70, 130, 180, 0.5);
  border-bottom-left-radius: 8px;
  box-shadow: 0 0 20px rgba(70, 130, 180, 0.3),
              inset 0 0 20px rgba(70, 130, 180, 0.1);
  animation: corner-pulse 4s ease-in-out infinite;
  animation-delay: 2s;
}

.corner-frame-br {
  bottom: 20px;
  right: 20px;
  border-bottom: 3px solid rgba(70, 130, 180, 0.5);
  border-right: 3px solid rgba(70, 130, 180, 0.5);
  border-bottom-right-radius: 8px;
  box-shadow: 0 0 20px rgba(70, 130, 180, 0.3),
              inset 0 0 20px rgba(70, 130, 180, 0.1);
  animation: corner-pulse 4s ease-in-out infinite;
  animation-delay: 3s;
}

@keyframes corner-pulse {
  0%, 100% {
    opacity: 0.6;
    box-shadow: 0 0 20px rgba(70, 130, 180, 0.3),
                inset 0 0 20px rgba(70, 130, 180, 0.1);
  }
  50% {
    opacity: 1;
    box-shadow: 0 0 30px rgba(70, 130, 180, 0.5),
                inset 0 0 30px rgba(70, 130, 180, 0.2);
  }
}

/* 动态光晕背景层 - Dynamic Glow Layer */
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

.glow-blue-1 {
  top: 15%;
  left: 20%;
  width: 450px;
  height: 450px;
  background: radial-gradient(circle, rgba(70, 130, 180, 0.25) 0%, transparent 70%);
  animation: glow-breath-blue-1 9s cubic-bezier(0.45, 0.05, 0.55, 0.95) infinite;
}

.glow-blue-2 {
  bottom: 15%;
  right: 20%;
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, rgba(30, 144, 255, 0.2) 0%, transparent 70%);
  animation: glow-breath-blue-2 11s cubic-bezier(0.45, 0.05, 0.55, 0.95) infinite;
  animation-delay: 2s;
}

.glow-blue-3 {
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(0, 212, 255, 0.15) 0%, transparent 70%);
  animation: glow-breath-blue-3 7s cubic-bezier(0.45, 0.05, 0.55, 0.95) infinite;
  animation-delay: 1s;
}

@keyframes glow-breath-blue-1 {
  0%, 100% {
    opacity: 0.25;
    transform: scale(1);
  }
  50% {
    opacity: 0.45;
    transform: scale(1.18);
  }
}

@keyframes glow-breath-blue-2 {
  0%, 100% {
    opacity: 0.2;
    transform: scale(1);
  }
  50% {
    opacity: 0.4;
    transform: scale(1.22);
  }
}

@keyframes glow-breath-blue-3 {
  0%, 100% {
    opacity: 0.18;
    transform: translate(-50%, -50%) scale(1);
  }
  50% {
    opacity: 0.38;
    transform: translate(-50%, -50%) scale(1.28);
  }
}

/* 防御护盾波纹扩散动画 - 从中心向外的防御波 */
@keyframes shield-ripple {
  0% {
    transform: translate(-50%, -50%) scale(0.65);
    opacity: 0.7;
  }
  40% {
    opacity: 0.4;
  }
  100% {
    transform: translate(-50%, -50%) scale(1.4);
    opacity: 0;
  }
}

/* 防御网格呼吸动画 - 稳定的防护系统 */
@keyframes defense-breath {
  0%, 100% {
    opacity: 0.35;
    transform: scale(1);
  }
  50% {
    opacity: 0.75;
    transform: scale(1.08);
  }
}

/* 防御护盾波纹 - 同心圆扩散效果（对比红队的旋转雷达） */
.blue-team-dashboard::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 800px;
  height: 800px;
  background:
    radial-gradient(circle, transparent 30%, rgba(70, 130, 180, 0.15) 31%, transparent 32%),
    radial-gradient(circle, transparent 45%, rgba(30, 144, 255, 0.12) 46%, transparent 47%),
    radial-gradient(circle, transparent 60%, rgba(70, 130, 180, 0.1) 61%, transparent 62%);
  border-radius: 50%;
  pointer-events: none;
  animation: shield-ripple 5s ease-out infinite;
  z-index: 0;
  filter: blur(1px);
}

/* 六边形防御网格呼吸 - 系统化防护效果 */
.blue-team-dashboard::after {
  content: '';
  position: absolute;
  top: 20%;
  right: 15%;
  width: 500px;
  height: 500px;
  background:
    radial-gradient(circle, rgba(30, 144, 255, 0.08) 0%, transparent 60%),
    repeating-conic-gradient(
      from 0deg at 50% 50%,
      rgba(70, 130, 180, 0.15) 0deg,
      transparent 60deg,
      rgba(70, 130, 180, 0.15) 120deg
    );
  border-radius: 50%;
  pointer-events: none;
  animation: defense-breath 6s ease-in-out infinite;
  z-index: 0;
  filter: blur(2px);
}

.dashboard-header {
  margin-bottom: var(--spacing-xl);
  text-align: center;
  position: relative;
  z-index: 1;
  padding: 24px 40px;
  background: rgba(20, 30, 50, 0.7);
  backdrop-filter: blur(25px);
  -webkit-backdrop-filter: blur(25px);
  border-radius: var(--radius-lg);
  border: 1px solid rgba(70, 130, 180, 0.35);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.4),
              inset 0 1px 0 rgba(255, 255, 255, 0.08),
              0 0 40px rgba(70, 130, 180, 0.1);
}

/* 四角装饰 */
.corner-decoration {
  position: absolute;
  width: 30px;
  height: 30px;
  z-index: 5;
}

.corner-top-left {
  top: 0;
  left: 0;
  border-top: 2px solid rgba(70, 130, 180, 0.6);
  border-left: 2px solid rgba(70, 130, 180, 0.6);
  border-top-left-radius: 4px;
  box-shadow: 0 0 15px rgba(70, 130, 180, 0.4);
}

.corner-top-right {
  top: 0;
  right: 0;
  border-top: 2px solid rgba(70, 130, 180, 0.6);
  border-right: 2px solid rgba(70, 130, 180, 0.6);
  border-top-right-radius: 4px;
  box-shadow: 0 0 15px rgba(70, 130, 180, 0.4);
}

/* 左右装饰线 */
.header-decoration-left {
  position: absolute;
  left: 50px;
  top: 50%;
  transform: translateY(-50%);
  width: 2px;
  height: 60%;
  background: linear-gradient(180deg,
    transparent 0%,
    rgba(70, 130, 180, 0.6) 50%,
    transparent 100%);
  box-shadow: 0 0 10px rgba(70, 130, 180, 0.4);
  animation: pulse-line 3s ease-in-out infinite;
}

.header-decoration-right {
  position: absolute;
  right: 50px;
  top: 50%;
  transform: translateY(-50%);
  width: 2px;
  height: 60%;
  background: linear-gradient(180deg,
    transparent 0%,
    rgba(70, 130, 180, 0.6) 50%,
    transparent 100%);
  box-shadow: 0 0 10px rgba(70, 130, 180, 0.4);
  animation: pulse-line 3s ease-in-out infinite;
  animation-delay: 1.5s;
}

@keyframes pulse-line {
  0%, 100% {
    opacity: 0.5;
  }
  50% {
    opacity: 1;
  }
}

.header-content {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  padding: 0 70px;
}

.title-section {
  flex: 1;
  text-align: center;
}

.dashboard-header::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  width: 4px;
  height: 100%;
  background: linear-gradient(180deg,
    rgba(70, 130, 180, 0.9) 0%,
    rgba(30, 144, 255, 0.6) 50%,
    rgba(70, 130, 180, 0.9) 100%);
  border-radius: var(--radius-lg) 0 0 var(--radius-lg);
  box-shadow: 0 0 15px rgba(70, 130, 180, 0.5);
}

.dashboard-header h1 {
  font-size: 32px;
  font-weight: 700;
  margin-bottom: var(--spacing-sm);
  background: linear-gradient(135deg,
    #4682b4 0%,
    #5fa3d4 25%,
    #1e90ff 50%,
    #6bb6ff 75%,
    #00d4ff 100%);
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  text-shadow: 0 0 35px rgba(70, 130, 180, 0.5);
  filter: drop-shadow(0 2px 15px rgba(70, 130, 180, 0.4))
          drop-shadow(0 0 25px rgba(30, 144, 255, 0.3));
  letter-spacing: -0.5px;
  animation: title-shimmer 8s ease-in-out infinite;
}

@keyframes title-shimmer {
  0%, 100% {
    filter: drop-shadow(0 2px 15px rgba(70, 130, 180, 0.4))
            drop-shadow(0 0 25px rgba(30, 144, 255, 0.3));
  }
  50% {
    filter: drop-shadow(0 2px 20px rgba(70, 130, 180, 0.6))
            drop-shadow(0 0 35px rgba(30, 144, 255, 0.5));
  }
}

.title-underline {
  width: 200px;
  height: 2px;
  margin: 12px auto;
  background: linear-gradient(90deg,
    transparent 0%,
    rgba(70, 130, 180, 0.3) 20%,
    rgba(30, 144, 255, 0.6) 50%,
    rgba(70, 130, 180, 0.3) 80%,
    transparent 100%);
  box-shadow: 0 0 10px rgba(30, 144, 255, 0.4);
  position: relative;
}

.title-underline::before,
.title-underline::after {
  content: '';
  position: absolute;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: rgba(30, 144, 255, 0.8);
  box-shadow: 0 0 8px rgba(30, 144, 255, 0.6);
  top: 50%;
  transform: translateY(-50%);
}

.title-underline::before {
  left: -10px;
}

.title-underline::after {
  right: -10px;
}

.dashboard-header p {
  color: rgba(255, 255, 255, 0.7);
  font-size: 16px;
  font-weight: 500;
}

.defense-stats {
  margin-bottom: var(--spacing-xl);
  position: relative;
  z-index: 1;
}

/* 统计卡片样式 - Stat Cards */
:deep(.stat-card) {
  background: rgba(20, 30, 50, 0.65);
  backdrop-filter: blur(25px);
  -webkit-backdrop-filter: blur(25px);
  border-radius: var(--radius-lg);
  border: 1px solid rgba(70, 130, 180, 0.35);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.45),
              0 0 20px rgba(70, 130, 180, 0.08),
              inset 0 1px 1px rgba(255, 255, 255, 0.08),
              inset 0 -1px 1px rgba(0, 0, 0, 0.1);
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
  overflow: hidden;
  position: relative;
}

:deep(.stat-card::before) {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  width: 3px;
  height: 100%;
  background: linear-gradient(180deg,
    rgba(70, 130, 180, 0.9) 0%,
    rgba(30, 144, 255, 0.6) 50%,
    rgba(70, 130, 180, 0.9) 100%);
  border-radius: 16px 0 0 16px;
  box-shadow: 0 0 15px rgba(70, 130, 180, 0.6),
              inset 0 0 10px rgba(30, 144, 255, 0.4);
}

:deep(.stat-card::after) {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: var(--radius-lg);
  padding: 1px;
  background: linear-gradient(135deg,
    rgba(70, 130, 180, 0.3) 0%,
    transparent 20%,
    transparent 80%,
    rgba(30, 144, 255, 0.3) 100%);
  -webkit-mask: linear-gradient(#fff 0 0) content-box,
                linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.4s ease;
}

:deep(.stat-card:hover) {
  transform: translateY(-4px);
  box-shadow: 0 12px 48px rgba(70, 130, 180, 0.3),
              0 0 40px rgba(70, 130, 180, 0.2),
              inset 0 1px 2px rgba(255, 255, 255, 0.12);
  border-color: rgba(70, 130, 180, 0.5);
}

:deep(.stat-card:hover::after) {
  opacity: 1;
}

:deep(.stat-card .el-card__body) {
  padding: 0;
}

.stat-content {
  display: flex;
  align-items: center;
  padding: var(--spacing-lg);
}

.stat-icon {
  font-size: 48px;
  margin-right: var(--spacing-md);
  filter: drop-shadow(0 4px 12px rgba(70, 130, 180, 0.3));
}

.stat-number {
  font-size: 36px;
  font-weight: 700;
  color: #00d4ff;
  font-family: var(--font-mono);
  text-shadow: 0 0 20px rgba(0, 212, 255, 0.5);
  letter-spacing: -1px;
}

.stat-label {
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
  font-weight: 500;
  margin-top: var(--spacing-xs);
}

.charts-section, .monitoring-section, .quick-actions, .logs-section, .trace-analysis {
  margin-bottom: var(--spacing-xl);
  position: relative;
  z-index: 1;
}

/* 图表卡片样式 - Chart Cards */
:deep(.chart-card), :deep(.monitoring-card), :deep(.el-card) {
  background: rgba(20, 30, 50, 0.65);
  backdrop-filter: blur(25px);
  -webkit-backdrop-filter: blur(25px);
  border-radius: var(--radius-lg);
  border: 1px solid rgba(70, 130, 180, 0.35);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.45),
              0 0 20px rgba(70, 130, 180, 0.08),
              inset 0 1px 1px rgba(255, 255, 255, 0.08),
              inset 0 -1px 1px rgba(0, 0, 0, 0.1);
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
  overflow: hidden;
  position: relative;
}

:deep(.chart-card::before), :deep(.monitoring-card::before), :deep(.el-card::before) {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  width: 3px;
  height: 100%;
  background: linear-gradient(180deg,
    rgba(70, 130, 180, 0.9) 0%,
    rgba(30, 144, 255, 0.6) 50%,
    rgba(70, 130, 180, 0.9) 100%);
  border-radius: 16px 0 0 16px;
  box-shadow: 0 0 15px rgba(70, 130, 180, 0.6),
              inset 0 0 10px rgba(30, 144, 255, 0.4);
  z-index: 1;
}

/* 卡片边角装饰 */
:deep(.chart-card::after), :deep(.monitoring-card::after), :deep(.el-card::after) {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: var(--radius-lg);
  padding: 1px;
  background: linear-gradient(135deg,
    rgba(70, 130, 180, 0.3) 0%,
    transparent 20%,
    transparent 80%,
    rgba(30, 144, 255, 0.3) 100%);
  -webkit-mask: linear-gradient(#fff 0 0) content-box,
                linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.4s ease;
}

:deep(.chart-card:hover), :deep(.monitoring-card:hover), :deep(.el-card:hover) {
  transform: translateY(-3px);
  box-shadow: 0 12px 48px rgba(70, 130, 180, 0.3),
              0 0 40px rgba(70, 130, 180, 0.2),
              inset 0 1px 2px rgba(255, 255, 255, 0.12);
  border-color: rgba(70, 130, 180, 0.5);
}

:deep(.chart-card:hover::after), :deep(.monitoring-card:hover::after), :deep(.el-card:hover::after) {
  opacity: 1;
}

:deep(.el-card__header) {
  background: rgba(30, 40, 60, 0.5);
  border-bottom: 1px solid rgba(70, 130, 180, 0.2);
  padding: var(--spacing-md) var(--spacing-lg);
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  font-size: 16px;
}

.chart-container {
  height: 300px;
  position: relative;
  z-index: 2;
}

/* 系统监控网格 - System Monitoring Grid */
.system-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: var(--spacing-md);
  padding: var(--spacing-lg);
}

.system-node {
  padding: var(--spacing-md);
  border-radius: var(--radius-md);
  border: 2px solid rgba(70, 130, 180, 0.3);
  background: rgba(30, 40, 60, 0.5);
  backdrop-filter: blur(10px);
  transition: all 0.3s ease;
}

.system-node:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(70, 130, 180, 0.2);
}

.system-node.normal {
  border-color: rgba(52, 199, 89, 0.5);
  background: rgba(52, 199, 89, 0.1);
}

.system-node.warning {
  border-color: rgba(255, 149, 0, 0.5);
  background: rgba(255, 149, 0, 0.1);
}

.system-node.critical {
  border-color: rgba(255, 59, 48, 0.5);
  background: rgba(255, 59, 48, 0.1);
  animation: pulse-critical 2s infinite;
}

@keyframes pulse-critical {
  0%, 100% {
    box-shadow: 0 0 0 rgba(255, 59, 48, 0.4);
  }
  50% {
    box-shadow: 0 0 20px rgba(255, 59, 48, 0.6);
  }
}

.system-name {
  font-weight: 700;
  margin-bottom: var(--spacing-xs);
  color: rgba(255, 255, 255, 0.95);
  font-size: 15px;
}

.system-ip {
  color: #00d4ff;
  margin-bottom: var(--spacing-xs);
  font-family: var(--font-mono);
  font-size: 13px;
}

.system-status {
  margin-bottom: var(--spacing-xs);
}

.system-metrics {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  font-family: var(--font-mono);
}

/* 快捷操作按钮 - Quick Action Buttons */
.action-buttons {
  display: flex;
  gap: var(--spacing-md);
  flex-wrap: wrap;
  padding: var(--spacing-lg);
}

:deep(.action-buttons .el-button) {
  flex: 1;
  min-width: 200px;
  border-radius: var(--radius-md);
  font-weight: 600;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
  background: rgba(70, 130, 180, 0.2);
  border: 1px solid rgba(70, 130, 180, 0.4);
  color: #4682b4;
}

:deep(.action-buttons .el-button:hover) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(70, 130, 180, 0.3);
  background: rgba(70, 130, 180, 0.3);
  color: #ffffff;
}

/* 表格样式 - Table Styles */
:deep(.el-table) {
  background: transparent;
  color: rgba(255, 255, 255, 0.9);
}

:deep(.el-table__inner-wrapper::before) {
  display: none;
}

:deep(.el-table thead) {
  background: rgba(30, 40, 60, 0.5);
}

:deep(.el-table th.el-table__cell) {
  background: rgba(30, 40, 60, 0.5) !important;
  color: #4682b4 !important;
  font-weight: 700;
  border-bottom: 2px solid rgba(70, 130, 180, 0.3);
}

:deep(.el-table__row) {
  background: transparent;
  transition: all 0.2s ease;
}

:deep(.el-table__row:hover) {
  background: rgba(70, 130, 180, 0.08) !important;
}

:deep(.el-table td.el-table__cell) {
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.85);
}

/* 溯源分析路径 - Trace Analysis Path */
.trace-path {
  display: flex;
  align-items: center;
  padding: var(--spacing-lg);
  overflow-x: auto;
}

.trace-step {
  display: flex;
  align-items: center;
  min-width: 200px;
}

.step-number {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #4682b4 0%, #1e90ff 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  margin-right: var(--spacing-sm);
  box-shadow: 0 4px 12px rgba(70, 130, 180, 0.3);
  font-size: 16px;
}

.step-content {
  flex: 1;
}

.step-title {
  font-weight: 700;
  margin-bottom: 4px;
  color: rgba(255, 255, 255, 0.95);
  font-size: 15px;
}

.step-detail {
  color: #00d4ff;
  font-size: 13px;
  margin-bottom: 2px;
  font-family: var(--font-mono);
}

.step-time {
  color: rgba(255, 255, 255, 0.5);
  font-size: 11px;
  font-family: var(--font-mono);
}

.step-arrow {
  font-size: 24px;
  color: #4682b4;
  margin: 0 var(--spacing-md);
  text-shadow: 0 0 10px rgba(70, 130, 180, 0.5);
}

/* 标签样式 - Tag Styles */
:deep(.el-tag) {
  border-radius: var(--radius-sm);
  border: none;
  font-weight: 600;
  padding: 4px 12px;
  font-size: 12px;
}

:deep(.el-tag--success) {
  background: rgba(52, 199, 89, 0.2);
  color: #34c759;
}

:deep(.el-tag--warning) {
  background: rgba(255, 149, 0, 0.2);
  color: #ff9500;
}

:deep(.el-tag--danger) {
  background: rgba(255, 59, 48, 0.2);
  color: #ff3b30;
}

/* 响应式设计 - Responsive Design */
@media (max-width: 768px) {
  .blue-team-dashboard {
    padding: var(--spacing-md);
  }

  .action-buttons {
    flex-direction: column;
  }

  :deep(.action-buttons .el-button) {
    width: 100%;
    min-width: auto;
  }

  .system-grid {
    grid-template-columns: 1fr;
  }

  .trace-step {
    min-width: 150px;
  }
}
</style>