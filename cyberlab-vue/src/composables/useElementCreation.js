
/**
 * 自定义元素创建和管理的Composable
 */
export function useElementCreation(chartRef, customElements, emit) {
  // 添加测试文字功能
  const addTestText = () => {
    // 将像素坐标转换为图表坐标系
    let chartX = 300, chartY = 150
    try {
      if (chartRef.value) {
        const chartCoords = chartRef.value.convertFromPixel({ seriesIndex: 0 }, [300, 150])
        chartX = chartCoords[0]
        chartY = chartCoords[1]
      }
    } catch {
      // 坐标转换失败，使用默认值
    }

    const testTextElement = {
      type: 'text',
      id: `test-text-${Date.now()}`,
      chartX: chartX,
      chartY: chartY,
      left: 300,
      top: 150,
      style: {
        text: '测试文字 - 右键点击我',
        fontSize: 18,
        fill: '#ff0000',
        fontWeight: 'bold',
        textAlign: 'left',
        textVerticalAlign: 'top'
      },
      draggable: true,
      pinned: false
    }

    customElements.value.push(testTextElement)
    const updatedElements = [...customElements.value]
    emit('update:customElements', updatedElements)
  }

  // 创建新元素
  const createElement = (tool, x, y) => {
    let newElement

    if (tool === 'text') {
      let chartX = x, chartY = y
      try {
        if (chartRef.value) {
          const chartCoords = chartRef.value.convertFromPixel({ seriesIndex: 0 }, [x, y])
          chartX = chartCoords[0]
          chartY = chartCoords[1]
        }
      } catch {
      // 坐标转换失败，使用默认值
    }

      newElement = {
        type: 'text',
        id: `text-${Date.now()}`,
        chartX: chartX,
        chartY: chartY,
        left: x,
        top: y,
        style: {
          text: '新文本',
          fontSize: 16,
          fill: '#333',
          fontWeight: 'bold'
        }
      }
    } else if (tool === 'rect') {
      let chartX = x - 50, chartY = y - 30
      try {
        if (chartRef.value) {
          const chartCoords = chartRef.value.convertFromPixel({ seriesIndex: 0 }, [x - 50, y - 30])
          chartX = chartCoords[0]
          chartY = chartCoords[1]
        }
      } catch {
      // 坐标转换失败，使用默认值
    }

      newElement = {
        type: 'rect',
        id: `rect-${Date.now()}`,
        chartX: chartX,
        chartY: chartY,
        left: x - 50,
        top: y - 30,
        shape: {
          width: 100,
          height: 60
        },
        style: {
          fill: 'rgba(0, 122, 255, 0.12)',
          stroke: 'rgba(0, 122, 255, 0.5)',
          lineWidth: 2,
          lineDash: [8, 4],
          shadowBlur: 4,
          shadowColor: 'rgba(0, 122, 255, 0.2)',
          shadowOffsetX: 0,
          shadowOffsetY: 1
        }
      }
    } else if (tool === 'circle') {
      let chartX = x, chartY = y
      try {
        if (chartRef.value) {
          const chartCoords = chartRef.value.convertFromPixel({ seriesIndex: 0 }, [x, y])
          chartX = chartCoords[0]
          chartY = chartCoords[1]
        }
      } catch {
      // 坐标转换失败，使用默认值
    }

      newElement = {
        type: 'circle',
        id: `circle-${Date.now()}`,
        chartX: chartX,
        chartY: chartY,
        left: x,
        top: y,
        shape: {
          r: 30
        },
        style: {
          fill: 'rgba(255, 149, 0, 0.08)',
          stroke: 'rgba(255, 149, 0, 0.6)',
          lineWidth: 1.5
        }
      }
    } else if (tool === 'trapezoid') {
      let chartX = x - 50, chartY = y - 30
      try {
        if (chartRef.value) {
          const chartCoords = chartRef.value.convertFromPixel({ seriesIndex: 0 }, [x - 50, y - 30])
          chartX = chartCoords[0]
          chartY = chartCoords[1]
        }
      } catch {
      // 坐标转换失败，使用默认值
    }

      newElement = {
        type: 'polygon',
        id: `trapezoid-${Date.now()}`,
        chartX: chartX,
        chartY: chartY,
        left: x - 50,
        top: y - 30,
        shape: {
          points: [
            [20, 0], [80, 0], [100, 60], [0, 60]
          ]
        },
        style: {
          fill: 'rgba(255, 193, 7, 0.08)',
          stroke: 'rgba(255, 193, 7, 0.6)',
          lineWidth: 1.5
        }
      }
    }

    if (newElement) {
      customElements.value.push(newElement)
      const updatedElements = [...customElements.value]
      emit('update:customElements', updatedElements)
      return newElement
    }

    return null
  }

  // 更新元素的通用函数
  const updateElement = (elementId, updates) => {
    const index = customElements.value.findIndex(el => el.id === elementId)

    if (index > -1) {
      const oldElement = customElements.value[index]

      // 深度合并，确保嵌套对象正确合并
      const newElement = {
        ...oldElement,
        ...updates,
        style: {
          ...oldElement.style,
          ...(updates.style || {})
        }
      }

      // 如果是矩形或圆形，也合并shape属性
      if (oldElement.type === 'rect' || oldElement.type === 'circle') {
        newElement.shape = {
          ...oldElement.shape,
          ...(updates.shape || {})
        }
      }

      customElements.value.splice(index, 1, newElement)
      const updatedArray = [...customElements.value]
      emit('update:customElements', updatedArray)

      return newElement
    }
    return null
  }

  // 删除元素
  const deleteElement = (elementId) => {
    const index = customElements.value.findIndex(el => el.id === elementId)
    if (index > -1) {
      customElements.value.splice(index, 1)
      emit('update:customElements', [...customElements.value])
      return true
    }
    return false
  }

  // 添加测试图形函数
  const addTestShapes = () => {
    const testElements = [
      {
        type: 'rect',
        id: `rect-test-${Date.now()}`,
        left: 100,
        top: 100,
        chartX: 100,
        chartY: 100,
        shape: { width: 100, height: 60 },
        style: {
          fill: 'rgba(0, 122, 255, 0.12)',
          stroke: 'rgba(0, 122, 255, 0.5)',
          lineWidth: 2,
          lineDash: [8, 4],
          shadowBlur: 4,
          shadowColor: 'rgba(0, 122, 255, 0.2)',
          shadowOffsetX: 0,
          shadowOffsetY: 1
        },
        draggable: true
      },
      {
        type: 'circle',
        id: `circle-test-${Date.now()}`,
        left: 250,
        top: 100,
        chartX: 250,
        chartY: 100,
        shape: { r: 30 },
        style: {
          fill: 'rgba(255, 149, 0, 0.08)',
          stroke: 'rgba(255, 149, 0, 0.6)',
          lineWidth: 1.5
        },
        draggable: true
      },
      {
        type: 'polygon',
        id: `trapezoid-test-${Date.now()}`,
        left: 400,
        top: 100,
        chartX: 400,
        chartY: 100,
        shape: {
          points: [[20, 0], [80, 0], [100, 60], [0, 60]]
        },
        style: {
          fill: 'rgba(255, 193, 7, 0.08)',
          stroke: 'rgba(255, 193, 7, 0.6)',
          lineWidth: 1.5
        },
        draggable: true
      },
      {
        type: 'text',
        id: `text-test-${Date.now()}`,
        left: 100,
        top: 200,
        chartX: 100,
        chartY: 200,
        style: {
          text: '测试文字 - 可右键编辑',
          fontSize: 16,
          fill: '#333',
          fontWeight: 'bold',
          textAlign: 'left',
          textVerticalAlign: 'top'
        },
        draggable: true,
        pinned: false
      }
    ]

    testElements.forEach(element => {
      customElements.value.push(element)
    })

    const updatedElements = [...customElements.value]
    emit('update:customElements', updatedElements)
  }

  return {
    addTestText,
    createElement,
    updateElement,
    deleteElement,
    addTestShapes
  }
}