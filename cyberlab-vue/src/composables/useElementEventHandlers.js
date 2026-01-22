/**
 * 元素事件处理器的组合式函数
 * 处理文字、矩形、圆形等自定义元素的交互事件
 */
export function useElementEventHandlers(elementUpdaters, customElements, emit, textEditDialog) {
  const { updateTextElement, updateRectElement, updateCircleElement } = elementUpdaters

  // 文字元素事件处理函数
  const handleEditTextContent = (textElement) => {
    textEditDialog.value = {
      visible: true,
      element: textElement
    }
  }

  const handleModifyFontSize = (textElement) => {
    // 快速字体大小调整
    const currentSize = textElement.style?.fontSize || 16
    const newSize = prompt('请输入字体大小 (12-48):', currentSize)

    if (newSize && !isNaN(newSize)) {
      const size = Math.max(12, Math.min(48, parseInt(newSize)))
      // 只更新样式，不传递其他属性以保持位置不变
      const styleUpdate = {
        style: {
          ...textElement.style,
          fontSize: size
        }
      }

      updateTextElement(textElement.id, styleUpdate)
    }
  }

  const handleModifyColor = (textElement, color) => {
    const newColor = color || textElement.style?.fill || '#333333'

    const styleUpdate = {
      style: {
        ...textElement.style,
        fill: newColor
      }
    }
    updateTextElement(textElement.id, styleUpdate)
  }

  const handleDeleteText = (textElement) => {
    if (confirm('确定要删除这个文字元素吗？')) {
      const index = customElements.value.findIndex(el => el.id === textElement.id)

      if (index > -1) {
        // 使用 splice 删除元素
        customElements.value.splice(index, 1)

        // 立即触发父组件更新
        const updatedArray = [...customElements.value]
        emit('update:customElements', updatedArray)

        // 强制重新渲染
        setTimeout(() => {
          debouncedRender()
        }, 100)
      }
    }
  }

  const handleTogglePin = (textElement) => {
    const newPinned = !textElement.pinned
    updateTextElement(textElement.id, {
      pinned: newPinned,
      draggable: !newPinned
    })
  }

  const handleSaveTextEdit = (updatedElement) => {
    updateTextElement(updatedElement.id, updatedElement)
  }

  // 矩形元素事件处理函数
  const handleRectEditLabel = (rectElement) => {
    const newLabel = prompt('请输入矩形标签:', rectElement.label || '')
    if (newLabel !== null) {
      updateRectElement(rectElement.id, { label: newLabel })
    }
  }

  const handleRectModifyBorderColor = (rectElement, color) => {
    const newColor = color || rectElement.style?.stroke || '#007aff'
    
    updateRectElement(rectElement.id, {
      style: {
        ...rectElement.style,
        stroke: newColor
      }
    })
  }

  const handleRectModifyFillColor = (rectElement, color) => {
    const newColor = color || rectElement.style?.fill || 'rgba(0, 122, 255, 0.3)'
    
    updateRectElement(rectElement.id, {
      style: {
        ...rectElement.style,
        fill: newColor
      }
    })
  }

  const handleRectAdjustSize = (rectElement) => {
    const currentWidth = rectElement.shape?.width || 100
    const currentHeight = rectElement.shape?.height || 60
    
    const newWidth = prompt('请输入矩形宽度:', currentWidth)
    if (newWidth && !isNaN(newWidth)) {
      const newHeight = prompt('请输入矩形高度:', currentHeight)
      if (newHeight && !isNaN(newHeight)) {
        updateRectElement(rectElement.id, {
          shape: {
            ...rectElement.shape,
            width: parseInt(newWidth),
            height: parseInt(newHeight)
          }
        })
      }
    }
  }

  const handleRectTogglePin = (rectElement) => {
    const index = customElements.value.findIndex(el => el.id === rectElement.id)
    if (index > -1) {
      customElements.value[index].pinned = !customElements.value[index].pinned
      emit('update:customElements', [...customElements.value])
    }
  }

  const handleRectDelete = (rectElement) => {
    const index = customElements.value.findIndex(el => el.id === rectElement.id)
    if (index > -1) {
      customElements.value.splice(index, 1)
      emit('update:customElements', [...customElements.value])
    }
  }

  // 圆形元素事件处理函数
  const handleCircleEditLabel = (circleElement) => {
    const newLabel = prompt('请输入圆形标签:', circleElement.label || '')
    if (newLabel !== null) {
      updateCircleElement(circleElement.id, { label: newLabel })
    }
  }

  const handleCircleModifyBorderColor = (circleElement, color) => {
    const newColor = color || circleElement.style?.stroke || '#ff9500'
    
    updateCircleElement(circleElement.id, {
      style: {
        ...circleElement.style,
        stroke: newColor
      }
    })
  }

  const handleCircleModifyFillColor = (circleElement, color) => {
    const newColor = color || circleElement.style?.fill || 'rgba(255, 149, 0, 0.3)'
    
    updateCircleElement(circleElement.id, {
      style: {
        ...circleElement.style,
        fill: newColor
      }
    })
  }

  const handleCircleAdjustRadius = (circleElement) => {
    const currentRadius = circleElement.shape?.r || 30
    const newRadius = prompt('请输入圆形半径:', currentRadius)
    if (newRadius && !isNaN(newRadius)) {
      updateCircleElement(circleElement.id, {
        shape: {
          ...circleElement.shape,
          r: parseInt(newRadius)
        }
      })
    }
  }

  const handleCircleTogglePin = (circleElement) => {
    const index = customElements.value.findIndex(el => el.id === circleElement.id)
    if (index > -1) {
      customElements.value[index].pinned = !customElements.value[index].pinned
      emit('update:customElements', [...customElements.value])
    }
  }

  const handleCircleDelete = (circleElement) => {
    const index = customElements.value.findIndex(el => el.id === circleElement.id)
    if (index > -1) {
      customElements.value.splice(index, 1)
      emit('update:customElements', [...customElements.value])
    }
  }

  return {
    // 文字元素事件处理器
    handleEditTextContent,
    handleModifyFontSize,
    handleModifyColor,
    handleDeleteText,
    handleTogglePin,
    handleSaveTextEdit,
    
    // 矩形元素事件处理器
    handleRectEditLabel,
    handleRectModifyBorderColor,
    handleRectModifyFillColor,
    handleRectAdjustSize,
    handleRectTogglePin,
    handleRectDelete,
    
    // 圆形元素事件处理器
    handleCircleEditLabel,
    handleCircleModifyBorderColor,
    handleCircleModifyFillColor,
    handleCircleAdjustRadius,
    handleCircleTogglePin,
    handleCircleDelete
  }
}