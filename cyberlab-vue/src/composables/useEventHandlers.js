/**
 * 事件处理模块 Composable
 * 统一管理各种事件处理逻辑
 */
export function useEventHandlers() {
  
  // 创建扩展的事件发送器
  const createExtendedEmit = (emit, contextMenus) => {
    const { 
      showTextContextMenu, 
      showRectContextMenu, 
      showCircleContextMenu
    } = contextMenus
    
    return (eventName, ...args) => {
      
      if (eventName === 'text-contextmenu') {
        const data = args[0]
        if (data && data.element && typeof data.x === 'number' && typeof data.y === 'number') {
          showTextContextMenu(data.x, data.y, data.element)
        } else {
        }
      } else if (eventName === 'node-contextmenu') {
        const data = args[0]
        if (data && data.node && typeof data.x === 'number' && typeof data.y === 'number') {
          // 这里保持原有逻辑，调用ContextMenuManager
          emit(eventName, ...args)
        } else {
        }
      } else if (eventName === 'link-contextmenu') {
        const data = args[0]
        if (data && data.link && typeof data.x === 'number' && typeof data.y === 'number') {
          // 这里保持原有逻辑，调用ContextMenuManager
          emit(eventName, ...args)
        } else {
        }
      } else if (eventName === 'rect-contextmenu') {
        const data = args[0]
        if (data && data.element && typeof data.x === 'number' && typeof data.y === 'number') {
          showRectContextMenu(data.x, data.y, data.element)
        } else {
        }
      } else if (eventName === 'circle-contextmenu') {
        const data = args[0]
        if (data && data.element && typeof data.x === 'number' && typeof data.y === 'number') {
          showCircleContextMenu(data.x, data.y, data.element)
        } else {
        }
      } else {
        emit(eventName, ...args)
      }
    }
  }
  
  // 创建画布点击处理器
  const createCanvasClickHandler = (drawingTool, customElements) => {
    const { currentTool, createElement } = drawingTool
    const { addElement, convertPixelToChart } = customElements
    
    return (e) => {
      if (!currentTool.value) {
        return
      }
      
      // 获取点击坐标
      const rect = e.currentTarget.getBoundingClientRect()
      const x = e.clientX - rect.left
      const y = e.clientY - rect.top
      
      // 创建新元素
      const newElement = createElement(currentTool.value, x, y, convertPixelToChart)
      
      if (newElement) {
        addElement(newElement)
        // 创建后清除工具选择
        drawingTool.setCurrentTool(null)
      }
    }
  }
  
  // 创建全局点击处理器
  const createGlobalClickHandler = (contextMenus) => {
    return (e) => {
      contextMenus.handleGlobalClick(e)
    }
  }
  
  // 创建ESC键处理器
  const createKeydownHandler = (connectionState) => {
    return (e) => {
      if (e.key === 'Escape' && connectionState?.isActive) {
        connectionState.cancelConnecting()
      }
    }
  }
  
  // 处理拖放事件
  const createDropHandler = (emit, saveTopologyData) => {
    return (e) => {
      try {
        e.preventDefault()
        const data = JSON.parse(e.dataTransfer.getData('text/plain'))
        
        if (data.type === 'device') {
          const rect = e.currentTarget.getBoundingClientRect()
          const x = e.clientX - rect.left
          const y = e.clientY - rect.top
          
          const device = data.device
          emit('drop-device', { item: device, x, y })
          saveTopologyData()
        }
      } catch (error) {
      }
    }
  }
  
  // 创建画布右键菜单处理器
  const createCanvasContextMenuHandler = (contextMenuManagerRef, emit) => {
    return (e) => {
      e.preventDefault()
      
      // 检查是否点击到了节点
      const clickedNode = findClickedNode(e)
      
      if (clickedNode) {
        // 如果点击到了节点，显示节点右键菜单
        if (contextMenuManagerRef.value) {
          contextMenuManagerRef.value.showNodeContextMenu(e, clickedNode)
          emit('node-contextmenu', clickedNode)
        }
      } else {
        // 关闭所有上下文菜单
        if (contextMenuManagerRef.value) {
          if (contextMenuManagerRef.value.contextMenu) {
            contextMenuManagerRef.value.contextMenu.visible = false
          }
        }
      }
    }
  }
  
  // 辅助函数：查找点击的节点
  const findClickedNode = () => {
    // 这里需要根据实际的节点检测逻辑来实现
    // 暂时返回null，需要在使用时传入具体的节点检测逻辑
    return null
  }
  
  // 元素操作事件处理器
  const createElementHandlers = (customElements) => {
    const { updateTextElement, updateRectElement, updateCircleElement, deleteElement } = customElements
    
    return {
      // 文字元素事件处理
      handleEditTextContent: (element, newContent) => {
        updateTextElement(element.id, {
          style: { text: newContent }
        })
      },
      
      handleModifyFontSize: (element, newSize) => {
        updateTextElement(element.id, {
          style: { fontSize: newSize }
        })
      },
      
      handleModifyTextColor: (element, newColor) => {
        updateTextElement(element.id, {
          style: { fill: newColor }
        })
      },
      
      handleDeleteText: (element) => {
        deleteElement(element.id)
      },
      
      handleTogglePin: (element) => {
        updateTextElement(element.id, {
          pinned: !element.pinned
        })
      },
      
      // 矩形元素事件处理
      handleRectEditLabel: (element, newLabel) => {
        updateRectElement(element.id, {
          label: newLabel
        })
      },
      
      handleRectModifyBorderColor: (element, newColor) => {
        updateRectElement(element.id, {
          style: { stroke: newColor }
        })
      },
      
      handleRectModifyFillColor: (element, newColor) => {
        updateRectElement(element.id, {
          style: { fill: newColor }
        })
      },
      
      handleRectAdjustSize: (element, newWidth, newHeight) => {
        updateRectElement(element.id, {
          shape: { width: newWidth, height: newHeight }
        })
      },
      
      handleRectDelete: (element) => {
        deleteElement(element.id)
      },
      
      // 圆形元素事件处理
      handleCircleEditLabel: (element, newLabel) => {
        updateCircleElement(element.id, {
          label: newLabel
        })
      },
      
      handleCircleModifyBorderColor: (element, newColor) => {
        updateCircleElement(element.id, {
          style: { stroke: newColor }
        })
      },
      
      handleCircleModifyFillColor: (element, newColor) => {
        updateCircleElement(element.id, {
          style: { fill: newColor }
        })
      },
      
      handleCircleAdjustRadius: (element, newRadius) => {
        updateCircleElement(element.id, {
          shape: { r: newRadius }
        })
      },
      
      handleCircleDelete: (element) => {
        deleteElement(element.id)
      }
    }
  }
  
  return {
    createExtendedEmit,
    createCanvasClickHandler,
    createGlobalClickHandler,
    createKeydownHandler,
    createDropHandler,
    createCanvasContextMenuHandler,
    createElementHandlers,
    findClickedNode
  }
}