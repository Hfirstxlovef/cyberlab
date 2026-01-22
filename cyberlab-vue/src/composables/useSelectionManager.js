import { ref, computed } from 'vue'

/**
 * 选择管理 Composable
 * 处理组件的选择、复制粘贴等操作（移除框选功能）
 */
export function useSelectionManager() {
  // 选中的节点 ID 列表
  const selectedNodes = ref(new Set())
  
  // 选中的自定义元素 ID 列表
  const selectedElements = ref(new Set())
  
  // 复制的数据
  const clipboard = ref({
    nodes: [],
    elements: [],
    timestamp: null
  })
  
  // 计算属性：是否有选中项
  const hasSelection = computed(() => {
    try {
      return selectedNodes.value.size > 0 || selectedElements.value.size > 0
    } catch {
      return false
    }
  })
  
  // 选择单个节点
  const selectNode = (nodeId, multiSelect = false) => {
    if (!multiSelect) {
      selectedNodes.value.clear()
      selectedElements.value.clear()
    }
    selectedNodes.value.add(nodeId)
  }
  
  // 选择单个元素
  const selectElement = (elementId, multiSelect = false) => {
    if (!multiSelect) {
      selectedNodes.value.clear()
      selectedElements.value.clear()
    }
    selectedElements.value.add(elementId)
  }
  
  // 取消选择节点
  const deselectNode = (nodeId) => {
    selectedNodes.value.delete(nodeId)
  }
  
  // 取消选择元素
  const deselectElement = (elementId) => {
    selectedElements.value.delete(elementId)
  }
  
  // 清空所有选择
  const clearSelection = () => {
    selectedNodes.value.clear()
    selectedElements.value.clear()
  }
  
  // 复制选中项
  const copySelection = (nodes, customElements) => {
    const nodesToCopy = nodes.value.filter(node => selectedNodes.value.has(node.id))
    const elementsToCopy = customElements.value.filter(element => selectedElements.value.has(element.id))
    
    clipboard.value = {
      nodes: JSON.parse(JSON.stringify(nodesToCopy)),
      elements: JSON.parse(JSON.stringify(elementsToCopy)),
      timestamp: Date.now()
    }
    
    return clipboard.value.nodes.length + clipboard.value.elements.length
  }
  
  // 粘贴复制项
  const pasteFromClipboard = (offsetX = 50, offsetY = 50) => {
    if (!clipboard.value.timestamp) return { nodes: [], elements: [] }
    
    const pastedNodes = clipboard.value.nodes.map(node => ({
      ...node,
      id: `${node.id}_copy_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
      x: node.x + offsetX,
      y: node.y + offsetY,
      name: `${node.name}_副本`
    }))
    
    const pastedElements = clipboard.value.elements.map(element => ({
      ...element,
      id: `${element.id}_copy_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
      left: (element.left || 0) + offsetX,
      top: (element.top || 0) + offsetY,
      chartX: (element.chartX || 0) + offsetX,
      chartY: (element.chartY || 0) + offsetY
    }))
    
    return { nodes: pastedNodes, elements: pastedElements }
  }
  
  // 删除选中项
  const deleteSelected = (nodes, links, customElements) => {
    const deletedCount = selectedNodes.value.size + selectedElements.value.size
    
    // 删除选中的节点
    if (selectedNodes.value.size > 0) {
      // 删除相关连线
      const nodeIds = Array.from(selectedNodes.value)
      links.value = links.value.filter(link => 
        !nodeIds.includes(link.source) && !nodeIds.includes(link.target)
      )
      
      // 删除节点
      nodes.value = nodes.value.filter(node => !selectedNodes.value.has(node.id))
    }
    
    // 删除选中的自定义元素
    if (selectedElements.value.size > 0) {
      customElements.value = customElements.value.filter(element => 
        !selectedElements.value.has(element.id)
      )
    }
    
    // 清空选择
    clearSelection()
    
    return deletedCount
  }
  
  // 检查节点是否被选中
  const isNodeSelected = (nodeId) => {
    return selectedNodes.value.has(nodeId)
  }
  
  // 检查元素是否被选中
  const isElementSelected = (elementId) => {
    return selectedElements.value.has(elementId)
  }
  
  return {
    // 状态
    selectedNodes,
    selectedElements,
    clipboard,
    hasSelection,
    
    // 选择操作
    selectNode,
    selectElement,
    deselectNode,
    deselectElement,
    clearSelection,
    
    // 复制粘贴操作
    copySelection,
    pasteFromClipboard,
    
    // 删除操作
    deleteSelected,
    
    // 查询方法
    isNodeSelected,
    isElementSelected
  }
}