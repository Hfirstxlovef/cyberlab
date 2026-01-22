import { ref, watch, nextTick } from 'vue'

/**
 * 自定义元素管理 Composable
 * 管理画布上的自定义元素（文字、矩形、圆形、梯形等）
 */
export function useCustomElements(props, emit, chartRef) {
  // 自定义元素数组
  const customElements = ref([])

  // 防抖渲染定时器
  let renderTimer = null

  // 状态更新锁，防止竞态条件
  let isUpdating = false

  // 防抖渲染函数 - 禁用独立渲染，完全依赖useTopologyChart
  const debouncedRender = () => {
    // 防抖渲染被调用，但已禁用独立渲染
    // 不再独立渲染，完全依赖useTopologyChart的渲染机制
    // 这避免了双重渲染机制的冲突
    if (renderTimer) {
      clearTimeout(renderTimer)
    }
    // 只是清理定时器，不执行实际渲染
    renderTimer = null
  }

  // 安全的状态更新函数
  const safeUpdateElements = (updateFn) => {
    if (isUpdating) {
      // 状态正在更新中，跳过本次更新
      return false
    }

    isUpdating = true
    try {
      const result = updateFn()
      return result
    } catch {
      // 更新元素状态失败
      return false
    } finally {
      // 使用 nextTick 确保在下一个事件循环中释放锁
      nextTick(() => {
        isUpdating = false
      })
    }
  }

  // 监听父组件传入的自定义元素变化
  watch(() => props.customElements, (newElements) => {
    // 接收到新元素

    if (Array.isArray(newElements)) {
      safeUpdateElements(() => {
        // 创建元素的深拷贝，避免响应式问题
        const cleanElements = JSON.parse(JSON.stringify(newElements)).map((element, index) => {
          // 处理元素和坐标信息

          // 确保每个元素都有唯一的id和正确的结构
          const cleanElement = {
            ...element,
            id: element.id || `${element.type || 'element'}-${Date.now()}-${index}`,
            $action: 'replace', // 添加替换动作
            z: element.z || 1000 // 设置合适的层级
          }

          // 统一坐标处理 - 保留所有坐标属性，让useTopologyChart决定使用哪个
          if (element.x !== undefined && element.y !== undefined) {
            // 优先使用现有的x,y坐标
            cleanElement.x = element.x
            cleanElement.y = element.y
          } else if (element.chartX !== undefined && element.chartY !== undefined) {
            // 使用图表坐标
            cleanElement.x = element.chartX
            cleanElement.y = element.chartY
            cleanElement.chartX = element.chartX
            cleanElement.chartY = element.chartY
          } else if (element.left !== undefined && element.top !== undefined) {
            // 使用像素坐标
            cleanElement.x = element.left
            cleanElement.y = element.top
            cleanElement.left = element.left
            cleanElement.top = element.top
          } else {
            // 设置默认坐标
            cleanElement.x = 0
            cleanElement.y = 0
          }

          // 保留原有坐标属性，让useTopologyChart处理
          if (element.chartX !== undefined) cleanElement.chartX = element.chartX
          if (element.chartY !== undefined) cleanElement.chartY = element.chartY
          if (element.left !== undefined) cleanElement.left = element.left
          if (element.top !== undefined) cleanElement.top = element.top

          // 确保样式对象存在
          if (!cleanElement.style) {
            cleanElement.style = {}
          }

          // 确保形状对象存在（对于有形状的元素）
          if (element.type !== 'text' && !cleanElement.shape) {
            cleanElement.shape = element.shape || {}
          }

          return cleanElement
        })

        customElements.value = cleanElements

        // 不再调用延迟渲染，完全依赖useTopologyChart
        // nextTick(() => {
        //   debouncedRender()
        // })
        return true
      })
    } else {
      // 接收到的不是数组
    }
  }, { immediate: true, deep: true })

  // 更新文字元素的通用函数
  const updateTextElement = (elementId, updates) => {
    return safeUpdateElements(() => {
      const index = customElements.value.findIndex(el => el.id === elementId)

      if (index > -1) {
        const oldElement = customElements.value[index]

        // 深度合并，确保嵌套对象（如style）正确合并
        const newElement = {
          ...oldElement,
          ...updates,
          $action: 'replace', // 确保替换动作
          // 确保样式对象正确合并
          style: {
            ...oldElement.style,
            ...(updates.style || {})
          }
        }

        // 处理坐标更新 - 保留所有坐标属性
        if (updates.chartX !== undefined && updates.chartY !== undefined) {
          newElement.x = updates.chartX
          newElement.y = updates.chartY
          newElement.chartX = updates.chartX
          newElement.chartY = updates.chartY
        } else if (updates.left !== undefined && updates.top !== undefined) {
          newElement.x = updates.left
          newElement.y = updates.top
          newElement.left = updates.left
          newElement.top = updates.top
        }

        // 使用 Vue 的响应式更新方式
        customElements.value.splice(index, 1, newElement)

        // 立即触发父组件更新，确保数据同步
        const updatedArray = [...customElements.value]
        emit('update:customElements', updatedArray)

        // 不再调用延迟渲染，完全依赖useTopologyChart
        // nextTick(() => {
        //   debouncedRender()
        // })

        return true
      }
      return false
    })
  }

  // 更新矩形元素的通用函数
  const updateRectElement = (elementId, updates) => {
    return safeUpdateElements(() => {
      const index = customElements.value.findIndex(el => el.id === elementId)

      if (index > -1) {
        const oldElement = customElements.value[index]

        // 深度合并，确保嵌套对象正确合并
        const newElement = {
          ...oldElement,
          ...updates,
          $action: 'replace', // 确保替换动作
          style: {
            ...oldElement.style,
            ...(updates.style || {})
          },
          shape: {
            ...oldElement.shape,
            ...(updates.shape || {})
          }
        }

        // 处理坐标更新 - 保留所有坐标属性
        if (updates.chartX !== undefined && updates.chartY !== undefined) {
          newElement.x = updates.chartX
          newElement.y = updates.chartY
          newElement.chartX = updates.chartX
          newElement.chartY = updates.chartY
        } else if (updates.left !== undefined && updates.top !== undefined) {
          newElement.x = updates.left
          newElement.y = updates.top
          newElement.left = updates.left
          newElement.top = updates.top
        }

        customElements.value.splice(index, 1, newElement)

        // 立即同步到父组件
        const updatedArray = [...customElements.value]
        emit('update:customElements', updatedArray)

        // 不再调用延迟渲染，完全依赖useTopologyChart
        // nextTick(() => {
        //   debouncedRender()
        // })

        return true
      }
      return false
    })
  }

  // 更新圆形元素的通用函数
  const updateCircleElement = (elementId, updates) => {
    return safeUpdateElements(() => {
      const index = customElements.value.findIndex(el => el.id === elementId)

      if (index > -1) {
        const oldElement = customElements.value[index]

        // 深度合并，确保嵌套对象正确合并
        const newElement = {
          ...oldElement,
          ...updates,
          $action: 'replace', // 确保替换动作
          style: {
            ...oldElement.style,
            ...(updates.style || {})
          },
          shape: {
            ...oldElement.shape,
            ...(updates.shape || {})
          }
        }

        // 处理坐标更新 - 保留所有坐标属性
        if (updates.chartX !== undefined && updates.chartY !== undefined) {
          newElement.x = updates.chartX
          newElement.y = updates.chartY
          newElement.chartX = updates.chartX
          newElement.chartY = updates.chartY
        } else if (updates.left !== undefined && updates.top !== undefined) {
          newElement.x = updates.left
          newElement.y = updates.top
          newElement.left = updates.left
          newElement.top = updates.top
        }

        customElements.value.splice(index, 1, newElement)

        // 立即同步到父组件
        const updatedArray = [...customElements.value]
        emit('update:customElements', updatedArray)

        // 不再调用延迟渲染，完全依赖useTopologyChart
        // nextTick(() => {
        //   debouncedRender()
        // })

        return true
      }
      return false
    })
  }

  // 删除元素
  const deleteElement = (elementId) => {
    return safeUpdateElements(() => {
      const index = customElements.value.findIndex(el => el.id === elementId)
      if (index > -1) {
        customElements.value.splice(index, 1)
        emit('update:customElements', [...customElements.value])
        // 不再调用延迟渲染，完全依赖useTopologyChart
        // nextTick(() => {
        //   debouncedRender()
        // })
        return true
      }
      return false
    })
  }

  // 添加元素
  const addElement = (element) => {
    return safeUpdateElements(() => {
      customElements.value.push(element)
      emit('update:customElements', [...customElements.value])
      // 不再调用延迟渲染，完全依赖useTopologyChart
      // nextTick(() => {
      //   debouncedRender()
      // })
      return true
    })
  }

  // 坐标转换辅助函数 (已废弃，使用 TopologyCanvas 中的 safeConvertCoords)
  const convertPixelToChart = (pixelX, pixelY) => {
    // convertPixelToChart 已废弃，请使用 TopologyCanvas 中的 safeConvertCoords
    let chartX = pixelX
    let chartY = pixelY

    try {
      if (chartRef.value && typeof chartRef.value.convertFromPixel === 'function') {
        const chartCoords = chartRef.value.convertFromPixel({ seriesIndex: 0 }, [pixelX, pixelY])
        if (Array.isArray(chartCoords) && chartCoords.length >= 2) {
          chartX = chartCoords[0]
          chartY = chartCoords[1]
        }
      }
    } catch {
      // 坐标转换失败，使用像素坐标
    }

    return { chartX, chartY }
  }

  // 清理定时器和状态
  const cleanup = () => {
    if (renderTimer) {
      clearTimeout(renderTimer)
      renderTimer = null
    }
    isUpdating = false
  }

  return {
    customElements,
    debouncedRender,
    updateTextElement,
    updateRectElement,
    updateCircleElement,
    deleteElement,
    addElement,
    convertPixelToChart,
    cleanup
  }
}