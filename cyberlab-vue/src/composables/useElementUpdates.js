import { nextTick } from 'vue'

/**
 * 元素更新功能的组合式函数
 * 处理文字、矩形、圆形等自定义元素的更新操作
 */
export function useElementUpdates(customElements, emit, debouncedRender, contextMenus) {
  const { closeTextContextMenu, closeRectContextMenu, closeCircleContextMenu } = contextMenus

  // 更新文字元素的通用函数
  const updateTextElement = (elementId, updates) => {
    const index = customElements.value.findIndex(el => el.id === elementId)

    if (index > -1) {
      const oldElement = customElements.value[index]

      // 深度合并，确保嵌套对象（如style）正确合并
      const newElement = {
        ...oldElement,
        ...updates,
        // 确保样式对象正确合并
        style: {
          ...oldElement.style,
          ...(updates.style || {})
        }
      }

      // 使用 Vue 的响应式更新方式
      customElements.value.splice(index, 1, newElement)

      // 立即触发父组件更新，确保数据同步
      const updatedArray = [...customElements.value]
      emit('update:customElements', updatedArray)

      // 关闭右键菜单
      closeTextContextMenu()

      // 立即强制重新渲染，不使用延迟
      nextTick(() => {
        debouncedRender()
      })
    }
  }

  // 更新矩形元素的通用函数
  const updateRectElement = (elementId, updates) => {
    const index = customElements.value.findIndex(el => el.id === elementId)

    if (index > -1) {
      const oldElement = customElements.value[index]

      // 深度合并，确保嵌套对象（如style、shape）正确合并
      const newElement = {
        ...oldElement,
        ...updates,
        // 确保样式对象正确合并
        style: {
          ...oldElement.style,
          ...(updates.style || {})
        },
        // 确保形状对象正确合并
        shape: {
          ...oldElement.shape,
          ...(updates.shape || {})
        }
      }

      // 使用 Vue 的响应式更新方式
      customElements.value.splice(index, 1, newElement)

      // 立即触发父组件更新，确保数据同步
      const updatedArray = [...customElements.value]
      emit('update:customElements', updatedArray)

      // 关闭右键菜单
      closeRectContextMenu()

      // 立即强制重新渲染
      nextTick(() => {
        debouncedRender()
      })
    }
  }

  // 更新圆形元素的通用函数
  const updateCircleElement = (elementId, updates) => {
    const index = customElements.value.findIndex(el => el.id === elementId)

    if (index > -1) {
      const oldElement = customElements.value[index]

      // 深度合并，确保嵌套对象（如style、shape）正确合并
      const newElement = {
        ...oldElement,
        ...updates,
        // 确保样式对象正确合并
        style: {
          ...oldElement.style,
          ...(updates.style || {})
        },
        // 确保形状对象正确合并
        shape: {
          ...oldElement.shape,
          ...(updates.shape || {})
        }
      }

      // 使用 Vue 的响应式更新方式
      customElements.value.splice(index, 1, newElement)

      // 立即触发父组件更新，确保数据同步
      const updatedArray = [...customElements.value]
      emit('update:customElements', updatedArray)

      // 关闭右键菜单
      closeCircleContextMenu()

      // 立即强制重新渲染
      nextTick(() => {
        debouncedRender()
      })
    }
  }

  return {
    updateTextElement,
    updateRectElement,
    updateCircleElement
  }
}