import { reactive } from 'vue';

export function useConnectionOperations() {
  // 连线状态管理
  const connectingState = reactive({
    isActive: false,
    sourceNode: null,
    tempLink: null
  })

  // 开始连线
  const startConnecting = (sourceNode) => {
    connectingState.isActive = true;
    connectingState.sourceNode = sourceNode;
    // ... 原有实现 ...
  }

  // 结束连线
  const finishConnecting = () => {
    // ... 原有实现 ...
  }

  // 取消连线
  const cancelConnecting = () => {
    // ... 原有实现 ...
  }

  // 显示/隐藏连线提示
  const showConnectingTooltip = () => { /* ... */ }
  const hideConnectingTooltip = () => { /* ... */ }

  return {
    connectingState,
    startConnecting,
    finishConnecting,
    cancelConnecting,
    showConnectingTooltip,
    hideConnectingTooltip
  }
}