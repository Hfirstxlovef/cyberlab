
/**
 * 画布连线交互管理 Composable
 * 专门处理画布上的连线相关交互逻辑
 */
export function useCanvasConnection(connectionManagement) {
  // 从现有的连线管理中解构需要的功能
  const { connectingState, startConnecting, finishConnecting, cancelConnecting } = connectionManagement
  
  // 处理节点点击事件
  const handleNodeClick = (node) => {
    if (connectingState.isActive) {
      finishConnecting(node)
    } else {
      startConnecting(node)
    }
  }
  
  // 处理画布点击事件（连线相关）
  const handleCanvasClickForConnection = (params) => {
    if (params.dataType === 'node') {
      // 点击到节点
      handleNodeClick(params.data)
      return true // 表示已处理
    } else if (params.dataType === 'edge') {
      // 点击到边
      return false // 让其他逻辑处理
    } else if (connectingState.isActive) {
      // 点击到空白区域且正在连线，取消连线
      cancelConnecting()
      return true // 表示已处理
    }
    
    return false // 未处理，让其他逻辑继续
  }
  
  // 处理ESC键取消连线
  const handleEscapeKey = (e) => {
    if (e.key === 'Escape' && connectingState.isActive) {
      cancelConnecting()
      return true // 表示已处理
    }
    return false // 未处理
  }
  
  // 检查是否正在连线
  const isConnecting = () => {
    return connectingState.isActive
  }
  
  // 获取连线状态信息
  const getConnectionInfo = () => {
    return {
      isActive: connectingState.isActive,
      sourceNode: connectingState.sourceNode,
      tempLine: connectingState.tempLine
    }
  }
  
  return {
    // 状态
    connectingState,
    
    // 连线操作
    startConnecting,
    finishConnecting, 
    cancelConnecting,
    
    // 事件处理器
    handleNodeClick,
    handleCanvasClickForConnection,
    handleEscapeKey,
    
    // 辅助方法
    isConnecting,
    getConnectionInfo
  }
}