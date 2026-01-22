import { ref } from 'vue'
import { debouncedSaveTopology } from '@/utils/topologySaveManager'

export function useNodeOperations(props, emit) {
  // 初始化路由实例
  // 由于 'router' 赋值后未使用，移除该代码
  const renamePopup = ref({ visible: false, value: '', node: null })
  // 在contextMenu定义处添加
  const contextMenu = ref({
    visible: false,
    x: 0,
    y: 0,
    node: null
  });


  const startRename = (node) => {
    renamePopup.value = {
      visible: true,
      value: node.name || '',
      node
    }
  }

  const confirmRename = () => {
    if (!renamePopup.value.node) return
    const target = props.nodes.find(n => n.id === renamePopup.value.node.id)
    if (target) {
      target.name = renamePopup.value.value
      emit('update', { ...props })
      saveTopologyData()
    }
    renamePopup.value.visible = false
  }

  const deleteNode = (nodeId) => {
    const filteredNodes = props.nodes.filter(n => n.id !== nodeId)
    const filteredLinks = props.links.filter(l => l.source !== nodeId && l.target !== nodeId)
    emit('update', { ...props, nodes: filteredNodes, links: filteredLinks })
    saveTopologyData()
  }

  const saveTopologyData = async (customLinks) => {
    if (!props.selectedProject?.id) {
      // 无法保存：未选择项目
      return;
    }

    // 使用传入的自定义连线数据或默认使用props.links
    const linksToSave = customLinks || props.links;

    const payload = {
      projectId: props.selectedProject.id,
      nodes: props.nodes,
      links: linksToSave,
      customElements: props.customElements
    }

    // 开始保存拓扑数据

    try {
      const result = await debouncedSaveTopology(payload, { showMessage: true })
      
      if (result.success) {
        // 拓扑数据保存成功
      } else {
        // 拓扑数据保存失败
      }
      
      return result
    } catch (error) {
      // 保存过程中发生异常
      return { success: false, error }
    }
  }

  const outputNodeDebugInfo = () => {
    // 调试信息输出方法（生产环境已移除日志）
  }

  const cancelRename = () => {
    renamePopup.value.visible = false
    renamePopup.value.value = ''
    renamePopup.value.node = null
  }

  return {
    renamePopup,
    contextMenu,
    startRename,
    confirmRename,
    cancelRename,
    deleteNode,
    outputNodeDebugInfo,
    saveTopologyData
  }
}