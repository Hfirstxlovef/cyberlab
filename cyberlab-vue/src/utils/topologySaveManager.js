/**
 * 拓扑图保存管理器
 * 功能：防抖保存、错误处理、状态管理、重试机制
 */
import { ref, reactive } from 'vue'
import { saveTopology } from '@/api/topology'
import { ElMessage, ElNotification } from 'element-plus'

// 全局保存状态
export const saveStatus = ref({
  isSaving: false,
  lastSaveTime: null,
  pendingSaveCount: 0,
  failedAttempts: 0
})

// 保存统计信息
export const saveStats = reactive({
  totalSaves: 0,
  successfulSaves: 0,
  failedSaves: 0,
  averageResponseTime: 0,
  lastError: null
})

class TopologySaveManager {
  constructor() {
    this.saveQueue = new Map() // 项目ID -> 保存数据
    this.saveTimers = new Map()  // 项目ID -> 定时器
    this.debounceDelay = 1000    // 防抖延迟（毫秒）
    this.maxRetries = 3          // 最大重试次数
    this.retryDelay = 2000       // 重试延迟（毫秒）
  }

  /**
   * 防抖保存拓扑数据
   * @param {Object} data - 拓扑数据 {projectId, nodes, links, customElements}
   * @param {Object} options - 选项 {immediate: boolean, showMessage: boolean}
   */
  async debouncedSave(data, options = {}) {
    const { immediate = false, showMessage = true } = options
    const projectId = data.projectId

    if (!projectId) {
      // projectId 不能为空
      return { success: false, error: 'projectId 不能为空' }
    }

    // 更新队列中的数据
    this.saveQueue.set(projectId, { ...data, timestamp: Date.now() })
    saveStatus.value.pendingSaveCount = this.saveQueue.size

    // 清除之前的定时器
    if (this.saveTimers.has(projectId)) {
      clearTimeout(this.saveTimers.get(projectId))
    }

    if (immediate) {
      return await this._executeSave(projectId, showMessage)
    }

    // 设置新的防抖定时器
    return new Promise((resolve) => {
      const timer = setTimeout(async () => {
        const result = await this._executeSave(projectId, showMessage)
        resolve(result)
      }, this.debounceDelay)

      this.saveTimers.set(projectId, timer)
    })
  }

  /**
   * 执行保存操作（带重试机制）
   * @private
   */
  async _executeSave(projectId, showMessage = true) {
    const data = this.saveQueue.get(projectId)
    if (!data) {
      return { success: false, error: '找不到待保存的数据' }
    }

    saveStatus.value.isSaving = true
    const startTime = Date.now()

    try {
      const result = await this._saveWithRetry(data, this.maxRetries)
      
      if (result.success) {
        // 保存成功
        this.saveQueue.delete(projectId)
        this.saveTimers.delete(projectId)
        saveStatus.value.pendingSaveCount = this.saveQueue.size
        saveStatus.value.lastSaveTime = new Date()
        saveStatus.value.failedAttempts = 0

        // 更新统计
        saveStats.totalSaves++
        saveStats.successfulSaves++
        const responseTime = Date.now() - startTime
        saveStats.averageResponseTime = Math.round(
          (saveStats.averageResponseTime * (saveStats.totalSaves - 1) + responseTime) / saveStats.totalSaves
        )

        if (showMessage) {
          ElMessage.success({
            message: '拓扑图保存成功',
            duration: 2000,
            showClose: true
          })
        }

        // 保存成功
      } else {
        // 保存失败
        saveStatus.value.failedAttempts++
        saveStats.failedSaves++
        saveStats.lastError = result.error

        if (showMessage) {
          ElNotification.error({
            title: '拓扑图保存失败',
            message: `错误信息: ${result.error?.message || '未知错误'}`,
            duration: 5000,
            showClose: true
          })
        }

        // 保存失败
      }

      return result
    } finally {
      saveStatus.value.isSaving = false
    }
  }

  /**
   * 带重试机制的保存
   * @private
   */
  async _saveWithRetry(data, maxRetries) {
    for (let attempt = 1; attempt <= maxRetries; attempt++) {
      try {
        // 尝试保存
        
        await saveTopology(data)
        return { success: true }
      } catch (error) {
        // 保存失败
        
        if (attempt === maxRetries) {
          return { 
            success: false, 
            error: error,
            attempts: attempt
          }
        }

        // 指数退避重试
        const delay = this.retryDelay * Math.pow(2, attempt - 1)
        await new Promise(resolve => setTimeout(resolve, delay))
      }
    }
  }

  /**
   * 立即保存所有待保存的数据
   */
  async saveAll() {
    const promises = []
    for (const projectId of this.saveQueue.keys()) {
      promises.push(this._executeSave(projectId, false))
    }
    
    const results = await Promise.allSettled(promises)
    
    const successful = results.filter(r => r.status === 'fulfilled' && r.value.success).length
    const failed = results.length - successful

    if (failed === 0) {
      ElMessage.success(`全部保存成功 (${successful}个项目)`)
    } else {
      ElMessage.warning(`保存完成：${successful}个成功，${failed}个失败`)
    }

    return { successful, failed, total: results.length }
  }

  /**
   * 清空保存队列
   */
  clearQueue() {
    // 清除所有定时器
    for (const timer of this.saveTimers.values()) {
      clearTimeout(timer)
    }
    
    this.saveQueue.clear()
    this.saveTimers.clear()
    saveStatus.value.pendingSaveCount = 0

    // 保存队列已清空
  }

  /**
   * 获取保存队列状态
   */
  getQueueStatus() {
    const queueItems = Array.from(this.saveQueue.entries()).map(([projectId, data]) => ({
      projectId,
      timestamp: data.timestamp,
      nodesCount: data.nodes?.length || 0,
      linksCount: data.links?.length || 0,
      customElementsCount: data.customElements?.length || 0
    }))

    return {
      queueSize: this.saveQueue.size,
      pendingItems: queueItems,
      isSaving: saveStatus.value.isSaving,
      lastSaveTime: saveStatus.value.lastSaveTime,
      failedAttempts: saveStatus.value.failedAttempts
    }
  }

  /**
   * 重置统计信息
   */
  resetStats() {
    saveStats.totalSaves = 0
    saveStats.successfulSaves = 0
    saveStats.failedSaves = 0
    saveStats.averageResponseTime = 0
    saveStats.lastError = null
    saveStatus.value.failedAttempts = 0
  }
}

// 创建全局实例
export const topologySaveManager = new TopologySaveManager()

// 导出便捷方法
export const debouncedSaveTopology = (data, options) => {
  return topologySaveManager.debouncedSave(data, options)
}

export const saveTopologyImmediate = (data) => {
  return topologySaveManager.debouncedSave(data, { immediate: true })
}

// 页面卸载时自动保存
if (typeof window !== 'undefined') {
  window.addEventListener('beforeunload', () => {
    topologySaveManager.saveAll()
  })
}