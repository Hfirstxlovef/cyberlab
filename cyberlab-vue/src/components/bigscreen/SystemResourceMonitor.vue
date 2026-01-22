<template>
    <div class="system-resource-monitor">
        <div class="chart-header">
            <h3>💻 系统资源监控</h3>
            <div class="status-indicator">
                <span class="status-dot" :class="systemStatus"></span>
                <span>{{ statusText }}</span>
            </div>
        </div>

        <div class="resource-grid">
            <div class="resource-item">
                <div class="resource-label">CPU使用率</div>
                <div class="resource-progress">
                    <div class="progress-bar">
                        <div class="progress-fill cpu" :style="{ width: data.cpu + '%' }"></div>
                    </div>
                    <span class="resource-value">{{ data.cpu }}%</span>
                </div>
            </div>

            <div class="resource-item">
                <div class="resource-label">内存使用率</div>
                <div class="resource-progress">
                    <div class="progress-bar">
                        <div class="progress-fill memory" :style="{ width: data.memory + '%' }"></div>
                    </div>
                    <span class="resource-value">{{ data.memory }}%</span>
                </div>
            </div>

            <div class="resource-item">
                <div class="resource-label">磁盘使用率</div>
                <div class="resource-progress">
                    <div class="progress-bar">
                        <div class="progress-fill disk" :style="{ width: data.disk + '%' }"></div>
                    </div>
                    <span class="resource-value">{{ data.disk }}%</span>
                </div>
            </div>

            <div class="resource-item">
                <div class="resource-label">网络流量</div>
                <div class="resource-progress">
                    <div class="progress-bar">
                        <div class="progress-fill network" :style="{ width: data.network + '%' }"></div>
                    </div>
                    <span class="resource-value">{{ data.network }}%</span>
                </div>
            </div>
        </div>

        <div class="resource-chart" ref="chartRef"></div>
    </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
    data: {
        type: Object,
        default: () => ({
            cpu: 45,
            memory: 68,
            disk: 32,
            network: 25,
            history: []
        })
    }
})

const chartRef = ref(null)
let chart = null

const systemStatus = computed(() => {
    const maxUsage = Math.max(props.data.cpu, props.data.memory, props.data.disk)
    if (maxUsage > 80) return 'critical'
    if (maxUsage > 60) return 'warning'
    return 'normal'
})

const statusText = computed(() => {
    switch (systemStatus.value) {
        case 'critical': return '高负载'
        case 'warning': return '中等负载'
        default: return '正常'
    }
})

const initChart = () => {
    if (!chartRef.value) return

    chart = echarts.init(chartRef.value)

    const option = {
        backgroundColor: 'transparent',
        grid: {
            top: 20,
            left: 40,
            right: 20,
            bottom: 30
        },
        xAxis: {
            type: 'category',
            data: Array.from({ length: 20 }, (_, index) => `${index * 5}s`),
            axisLine: { lineStyle: { color: 'rgba(0, 0, 0, 0.1)' } },
            axisLabel: { color: '#86868b', fontSize: 10, fontFamily: '-apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", "Helvetica Neue", sans-serif' }
        },
        yAxis: {
            type: 'value',
            max: 100,
            axisLine: { lineStyle: { color: 'rgba(0, 0, 0, 0.1)' } },
            axisLabel: { color: '#86868b', fontSize: 10, fontFamily: '-apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", "Helvetica Neue", sans-serif' },
            splitLine: { lineStyle: { color: 'rgba(0, 0, 0, 0.04)' } }
        },
        series: [
            {
                name: 'CPU',
                type: 'line',
                data: generateHistoryData(props.data.cpu),
                smooth: true,
                lineStyle: { color: '#ff3b30', width: 2 },
                areaStyle: { color: 'rgba(255, 59, 48, 0.1)' },
                symbol: 'none'
            },
            {
                name: '内存',
                type: 'line',
                data: generateHistoryData(props.data.memory),
                smooth: true,
                lineStyle: { color: '#af52de', width: 2 },
                areaStyle: { color: 'rgba(175, 82, 222, 0.1)' },
                symbol: 'none'
            },
            {
                name: '磁盘',
                type: 'line',
                data: generateHistoryData(props.data.disk),
                smooth: true,
                lineStyle: { color: '#007aff', width: 2 },
                areaStyle: { color: 'rgba(0, 122, 255, 0.1)' },
                symbol: 'none'
            }
        ]
    }

    chart.setOption(option)
}

const generateHistoryData = (currentValue) => {
    return Array.from({ length: 20 }, () => {
        const variation = Math.random() * 20 - 10
        return Math.max(0, Math.min(100, currentValue + variation))
    })
}

watch(() => props.data, () => {
    if (chart) {
        const option = chart.getOption()
        option.series[0].data = generateHistoryData(props.data.cpu)
        option.series[1].data = generateHistoryData(props.data.memory)
        option.series[2].data = generateHistoryData(props.data.disk)
        chart.setOption(option)
    }
}, { deep: true })

onMounted(async () => {
    await nextTick()
    initChart()
})
</script>

<style scoped>
/* ============================================
   Apple Elegant White Style - System Resource Monitor
   ============================================ */

:root {
    --apple-text: #1d1d1f;
    --apple-text-secondary: #86868b;
    --apple-red: #ff3b30;
    --apple-orange: #ff9500;
    --apple-green: #34c759;
    --apple-blue: #007aff;
    --apple-purple: #af52de;
    --font-apple: -apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", "Helvetica Neue", sans-serif;
}

.system-resource-monitor {
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

.status-indicator {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
    font-weight: 500;
    color: var(--apple-text-secondary);
}

.status-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    animation: pulse 2s infinite;
}

.status-dot.normal {
    background: var(--apple-green);
    box-shadow: 0 0 8px rgba(52, 199, 89, 0.4);
}

.status-dot.warning {
    background: var(--apple-orange);
    box-shadow: 0 0 8px rgba(255, 149, 0, 0.4);
}

.status-dot.critical {
    background: var(--apple-red);
    box-shadow: 0 0 8px rgba(255, 59, 48, 0.4);
}

.resource-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 15px;
    margin-bottom: 20px;
}

.resource-item {
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.resource-label {
    font-size: 12px;
    font-weight: 500;
    color: var(--apple-text-secondary);
}

.resource-progress {
    display: flex;
    align-items: center;
    gap: 10px;
}

.progress-bar {
    flex: 1;
    height: 6px;
    background: rgba(0, 0, 0, 0.06);
    border-radius: 3px;
    overflow: hidden;
}

.progress-fill {
    height: 100%;
    border-radius: 3px;
    transition: width 0.3s ease;
}

.progress-fill.cpu {
    background: linear-gradient(90deg, var(--apple-red) 0%, #ff6b59 100%);
}

.progress-fill.memory {
    background: linear-gradient(90deg, var(--apple-purple) 0%, #c77dff 100%);
}

.progress-fill.disk {
    background: linear-gradient(90deg, var(--apple-blue) 0%, #5ac8fa 100%);
}

.progress-fill.network {
    background: linear-gradient(90deg, var(--apple-green) 0%, #65d872 100%);
}

.resource-value {
    font-size: 12px;
    font-weight: 600;
    color: var(--apple-text);
    min-width: 35px;
    text-align: right;
    letter-spacing: -0.2px;
}

.resource-chart {
    flex: 1;
    min-height: 150px;
}

@keyframes pulse {
    0%,
    100% {
        opacity: 1;
    }

    50% {
        opacity: 0.5;
    }
}
</style>