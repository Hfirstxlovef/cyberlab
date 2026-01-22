import { ref } from 'vue'

/**
 * 绘图工具管理组合式API
 */
export function useDrawingTool() {
  const currentTool = ref(null)

  const setTool = (tool) => {
    currentTool.value = tool
    
    // 设置鼠标样式
    if (tool) {
      document.body.style.cursor = tool === 'text' ? 'text' : 'crosshair'
    } else {
      document.body.style.cursor = 'default'
    }
  }

  const clearTool = () => {
    setTool(null)
  }

  const isToolActive = (tool) => {
    return currentTool.value === tool
  }

  return {
    currentTool,
    setTool,
    clearTool,
    isToolActive
  }
}

/**
 * 自定义元素管理组合式API
 */
export function useCustomElements() {
  const customElements = ref([])
  const selectedElement = ref(null)

  const addElement = (element) => {
    customElements.value.push(element)
  }

  const updateElement = (elementId, updates) => {
    const index = customElements.value.findIndex(el => el.id === elementId)
    if (index !== -1) {
      customElements.value[index] = {
        ...customElements.value[index],
        ...updates
      }
      
      // 同步更新选中状态
      if (selectedElement.value?.id === elementId) {
        selectedElement.value = customElements.value[index]
      }
    }
  }

  const deleteElement = (elementId) => {
    const index = customElements.value.findIndex(el => el.id === elementId)
    if (index !== -1) {
      customElements.value.splice(index, 1)
      
      // 如果删除的是选中元素，清除选中状态
      if (selectedElement.value?.id === elementId) {
        selectedElement.value = null
      }
    }
  }

  const selectElement = (element) => {
    selectedElement.value = element
  }

  const clearSelection = () => {
    selectedElement.value = null
  }

  const clearAll = () => {
    customElements.value = []
    selectedElement.value = null
  }

  return {
    customElements,
    selectedElement,
    addElement,
    updateElement,
    deleteElement,
    selectElement,
    clearSelection,
    clearAll
  }
}

/**
 * 拓扑图数据管理组合式API
 */
export function useTopologyData() {
  const nodes = ref([])
  const links = ref([])

  const updateNodes = (newNodes) => {
    nodes.value = newNodes
  }

  const updateLinks = (newLinks) => {
    links.value = newLinks
  }

  const updateData = (data) => {
    if (data.nodes) nodes.value = data.nodes
    if (data.links) links.value = data.links
  }

  const addNode = (node) => {
    nodes.value.push(node)
  }

  const updateNode = (nodeId, updates) => {
    const index = nodes.value.findIndex(n => n.id === nodeId)
    if (index !== -1) {
      nodes.value[index] = {
        ...nodes.value[index],
        ...updates
      }
    }
  }

  const deleteNode = (nodeId) => {
    // 删除节点
    const nodeIndex = nodes.value.findIndex(n => n.id === nodeId)
    if (nodeIndex !== -1) {
      nodes.value.splice(nodeIndex, 1)
    }
    
    // 删除相关连线
    links.value = links.value.filter(link => 
      link.source !== nodeId && link.target !== nodeId
    )
  }

  const addLink = (link) => {
    links.value.push(link)
  }

  const updateLink = (linkId, updates) => {
    const index = links.value.findIndex(l => l.id === linkId)
    if (index !== -1) {
      links.value[index] = {
        ...links.value[index],
        ...updates
      }
    }
  }

  const deleteLink = (linkId) => {
    const index = links.value.findIndex(l => l.id === linkId)
    if (index !== -1) {
      links.value.splice(index, 1)
    }
  }

  return {
    nodes,
    links,
    updateNodes,
    updateLinks,
    updateData,
    addNode,
    updateNode,
    deleteNode,
    addLink,
    updateLink,
    deleteLink
  }
}

/**
 * 对话框状态管理组合式API
 */
export function useDialogStates() {
  const dialogStates = ref({
    rename: { visible: false, node: null },
    detail: { visible: false, node: null },
    type: { visible: false, node: null },
    icon: { visible: false, node: null },
    connection: { visible: false, node: null }
  })

  const showDialog = (dialogType, node = null) => {
    dialogStates.value[dialogType].visible = true
    dialogStates.value[dialogType].node = node
  }

  const closeDialog = (dialogType) => {
    dialogStates.value[dialogType].visible = false
    dialogStates.value[dialogType].node = null
  }

  const closeAllDialogs = () => {
    Object.keys(dialogStates.value).forEach(key => {
      dialogStates.value[key].visible = false
      dialogStates.value[key].node = null
    })
  }

  return {
    dialogStates,
    showDialog,
    closeDialog,
    closeAllDialogs
  }
}

/**
 * 元素创建工厂组合式API
 */
export function useElementFactory() {
  const createElement = (tool, x, y) => {
    if (!tool || typeof x !== 'number' || typeof y !== 'number') {
      return null
    }
    
    const id = `${tool}-${Date.now()}`
    let element = null
    
    switch (tool) {
      case 'text':
        element = {
          id,
          type: 'text',
          x: Math.max(0, x),
          y: Math.max(16, y),
          chartX: Math.max(0, x),
          chartY: Math.max(16, y),
          left: Math.max(0, x),
          top: Math.max(16, y),
          style: {
            text: '新文本',
            fontSize: '16px',
            fill: '#333333',
            fontWeight: 'normal'
          }
        }
        break
      
      case 'rect':
        element = {
          id,
          type: 'rect',
          x: Math.max(0, x - 50),
          y: Math.max(0, y - 30),
          chartX: Math.max(0, x - 50),
          chartY: Math.max(0, y - 30),
          left: Math.max(0, x - 50),
          top: Math.max(0, y - 30),
          width: 100,
          height: 60,
          shape: {
            width: 100,
            height: 60
          },
          style: {
            fill: 'rgba(0, 122, 255, 0.1)',
            stroke: '#007aff',
            strokeWidth: '2px'
          }
        }
        break
      
      case 'circle':
        element = {
          id,
          type: 'circle',
          x: Math.max(30, x - 30),
          y: Math.max(30, y - 30),
          chartX: Math.max(30, x - 30),
          chartY: Math.max(30, y - 30),
          left: Math.max(30, x - 30),
          top: Math.max(30, y - 30),
          radius: 30,
          shape: {
            r: 30
          },
          style: {
            fill: 'rgba(255, 149, 0, 0.1)',
            stroke: '#ff9500',
            strokeWidth: '2px'
          }
        }
        break
      
      default:
        return null
    }
    
    return element
  }

  return {
    createElement
  }
}