import { discoverProjectContainers, discoverProjectContainersDirect, discoverProjectImages } from '@/api/containers'

// 容器数据缓存服务
class ContainerCacheService {
  constructor() {
    this.cache = new Map()
    this.cacheTimeout = 5 * 60 * 1000 // 5分钟缓存
    this.requestQueue = new Map() // 防止重复请求
  }

  // 生成缓存key
  generateKey(type, params = {}) {
    const paramsStr = Object.keys(params)
      .sort()
      .map(key => `${key}=${params[key]}`)
      .join('&')
    return `${type}:${paramsStr}`
  }

  // 检查缓存是否有效
  isValid(cacheItem) {
    return cacheItem && (Date.now() - cacheItem.timestamp) < this.cacheTimeout
  }

  // 获取缓存数据
  get(key) {
    const cacheItem = this.cache.get(key)
    if (this.isValid(cacheItem)) {
      return cacheItem.data
    }
    this.cache.delete(key) // 清除过期缓存
    return null
  }

  // 设置缓存数据
  set(key, data) {
    this.cache.set(key, {
      data,
      timestamp: Date.now()
    })
  }

  // 清除特定缓存
  clear(pattern) {
    for (const [key] of this.cache) {
      if (pattern && key.includes(pattern)) {
        this.cache.delete(key)
      }
    }
  }

  // 清除所有缓存
  clearAll() {
    this.cache.clear()
    this.requestQueue.clear()
  }

  // 防重复请求
  async executeWithDeduplication(key, asyncFn) {
    // 检查是否有正在进行的相同请求
    if (this.requestQueue.has(key)) {
      return this.requestQueue.get(key)
    }

    // 执行异步请求
    const promise = asyncFn().finally(() => {
      this.requestQueue.delete(key)
    })

    this.requestQueue.set(key, promise)
    return promise
  }

  // 获取容器列表（带缓存） - 使用新的直接探测API
  async getContainers(projectId, useCache = true) {
    const cacheKey = this.generateKey('containers', { projectId })

    if (useCache) {
      const cached = this.get(cacheKey)
      if (cached) {
        return cached
      }
    }

    return this.executeWithDeduplication(cacheKey, async () => {
      try {
        // 使用直接探测API（基于资产IP直接探测）
        // 不需要主机节点关联，更简洁
        const result = await discoverProjectContainersDirect(projectId)
        this.set(cacheKey, result)
        return result
      } catch (error) {
        throw error
      }
    })
  }

  // 获取镜像列表（带缓存） - 使用新的直接探测API
  async getImages(projectId, useCache = true) {
    const cacheKey = this.generateKey('images', { projectId })

    if (useCache) {
      const cached = this.get(cacheKey)
      if (cached) {
        return cached
      }
    }

    return this.executeWithDeduplication(cacheKey, async () => {
      try {
        // 使用直接探测API（基于资产IP直接探测Docker镜像）
        const result = await discoverProjectImages(projectId)
        this.set(cacheKey, result)
        return result
      } catch (error) {
        throw error
      }
    })
  }

  // 获取节点列表（带缓存）
  async getNodes(projectId, useCache = true) {
    const cacheKey = this.generateKey('nodes', { projectId })
    
    if (useCache) {
      const cached = this.get(cacheKey)
      if (cached) {
        return cached
      }
    }

    return this.executeWithDeduplication(cacheKey, async () => {
      try {
        const response = await fetch(`/api/host-nodes/project/${encodeURIComponent(projectId)}`, {
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`
          }
        })

        if (response.ok) {
          const result = await response.json()
          this.set(cacheKey, result)
          return result
        } else {
          throw new Error(`HTTP ${response.status}`)
        }
      } catch (error) {
        throw error
      }
    })
  }

  // 获取统计信息（带缓存）
  async getStatistics(projectId, useCache = true) {
    const cacheKey = this.generateKey('statistics', { projectId })
    
    if (useCache) {
      const cached = this.get(cacheKey)
      if (cached) {
        return cached
      }
    }

    return this.executeWithDeduplication(cacheKey, async () => {
      try {
        const response = await fetch(`/api/host-nodes/project/${encodeURIComponent(projectId)}/statistics`, {
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`
          }
        })

        if (response.ok) {
          const result = await response.json()
          this.set(cacheKey, result)
          return result
        } else {
          throw new Error(`HTTP ${response.status}`)
        }
      } catch (error) {
        throw error
      }
    })
  }

  // 容器操作后清除相关缓存
  invalidateContainerCache(projectId) {
    this.clear('containers')
    this.clear('statistics')
  }

  // 节点操作后清除相关缓存
  invalidateNodeCache(projectId) {
    this.clear('nodes')
    this.clear('statistics')
  }
}

// 创建全局缓存实例
export const containerCache = new ContainerCacheService()

// 性能监控工具
export class PerformanceMonitor {
  constructor() {
    this.metrics = new Map()
  }

  // 开始计时
  start(operation) {
    this.metrics.set(operation, {
      startTime: performance.now(),
      endTime: null,
      duration: null
    })
  }

  // 结束计时
  end(operation) {
    const metric = this.metrics.get(operation)
    if (metric) {
      metric.endTime = performance.now()
      metric.duration = metric.endTime - metric.startTime
    }
    return metric?.duration || 0
  }

  // 获取性能数据
  getMetrics() {
    const result = {}
    for (const [operation, metric] of this.metrics) {
      if (metric.duration !== null) {
        result[operation] = metric.duration
      }
    }
    return result
  }

  // 清除指标
  clear() {
    this.metrics.clear()
  }

  // 记录操作性能
  async measure(operation, asyncFn) {
    this.start(operation)
    try {
      const result = await asyncFn()
      const duration = this.end(operation)
      return result
    } catch (error) {
      this.end(operation)
      throw error
    }
  }
}

// 创建全局性能监控实例
export const performanceMonitor = new PerformanceMonitor()

// 批量操作优化器
export class BatchOperationManager {
  constructor() {
    this.pendingOperations = new Map()
    this.batchTimeout = 100 // 100ms内的操作会被批量处理
  }

  // 添加批量操作
  addOperation(type, data, handler) {
    if (!this.pendingOperations.has(type)) {
      this.pendingOperations.set(type, {
        items: [],
        handler,
        timeout: null
      })
    }

    const operation = this.pendingOperations.get(type)
    operation.items.push(data)

    // 清除之前的超时
    if (operation.timeout) {
      clearTimeout(operation.timeout)
    }

    // 设置新的批量处理超时
    operation.timeout = setTimeout(() => {
      this.executeBatch(type)
    }, this.batchTimeout)
  }

  // 执行批量操作
  async executeBatch(type) {
    const operation = this.pendingOperations.get(type)
    if (!operation || operation.items.length === 0) {
      return
    }

    const { items, handler } = operation
    this.pendingOperations.delete(type)

    try {
      await handler(items)
    } catch (error) {
    }
  }

  // 立即执行所有待处理的批量操作
  async flushAll() {
    const types = Array.from(this.pendingOperations.keys())
    await Promise.all(types.map(type => this.executeBatch(type)))
  }
}

// 创建全局批量操作管理器
export const batchManager = new BatchOperationManager()

export default {
  containerCache,
  performanceMonitor,
  batchManager
}