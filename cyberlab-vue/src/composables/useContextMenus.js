import { ref } from 'vue'

/**
 * 右键菜单管理 Composable
 * 管理各种类型元素的右键菜单状态
 */
export function useContextMenus() {
  // 文字右键菜单状态
  const textContextMenu = ref({
    visible: false,
    x: 0,
    y: 0,
    element: null
  })
  
  // 矩形右键菜单状态
  const rectContextMenu = ref({
    visible: false,
    x: 0,
    y: 0,
    element: null
  })
  
  // 圆形右键菜单状态
  const circleContextMenu = ref({
    visible: false,
    x: 0,
    y: 0,
    element: null
  })
  
  // 文字编辑对话框状态
  const textEditDialog = ref({
    visible: false,
    element: null
  })
  
  // 关闭文字右键菜单
  const closeTextContextMenu = () => {
    textContextMenu.value = {
      visible: false,
      x: 0,
      y: 0,
      element: null
    }
  }
  
  // 关闭矩形右键菜单
  const closeRectContextMenu = () => {
    rectContextMenu.value = {
      visible: false,
      x: 0,
      y: 0,
      element: null
    }
  }
  
  // 关闭圆形右键菜单
  const closeCircleContextMenu = () => {
    circleContextMenu.value = {
      visible: false,
      x: 0,
      y: 0,
      element: null
    }
  }
  
  // 关闭文字编辑对话框
  const closeTextEditDialog = () => {
    textEditDialog.value = {
      visible: false,
      element: null
    }
  }
  
  // 显示文字右键菜单
  const showTextContextMenu = (x, y, element) => {
    textContextMenu.value = {
      visible: true,
      x: Math.max(0, Math.min(window.innerWidth - 200, x)),
      y: Math.max(0, Math.min(window.innerHeight - 300, y)),
      element
    }
    
  }
  
  // 显示矩形右键菜单
  const showRectContextMenu = (x, y, element) => {
    rectContextMenu.value = {
      visible: true,
      x: Math.max(0, Math.min(window.innerWidth - 200, x)),
      y: Math.max(0, Math.min(window.innerHeight - 350, y)),
      element
    }
  }
  
  // 显示圆形右键菜单
  const showCircleContextMenu = (x, y, element) => {
    circleContextMenu.value = {
      visible: true,
      x: Math.max(0, Math.min(window.innerWidth - 200, x)),
      y: Math.max(0, Math.min(window.innerHeight - 350, y)),
      element
    }
  }
  
  // 显示文字编辑对话框
  const showTextEditDialog = (element) => {
    textEditDialog.value = {
      visible: true,
      element
    }
  }
  
  // 关闭所有菜单
  const closeAllMenus = () => {
    closeTextContextMenu()
    closeRectContextMenu()
    closeCircleContextMenu()
    closeTextEditDialog()
  }
  
  // 全局点击处理器
  const handleGlobalClick = (e) => {
    // 检查点击是否在菜单内部
    const textMenuEl = e.target.closest('.text-context-menu')
    const rectMenuEl = e.target.closest('.rect-context-menu')  
    const circleMenuEl = e.target.closest('.circle-context-menu')
    
    // 如果点击在菜单外部，关闭对应菜单
    if (textContextMenu.value.visible && !textMenuEl) {
      closeTextContextMenu()
    }
    
    if (rectContextMenu.value.visible && !rectMenuEl) {
      closeRectContextMenu()
    }
    
    if (circleContextMenu.value.visible && !circleMenuEl) {
      closeCircleContextMenu()
    }
  }
  
  return {
    // 状态
    textContextMenu,
    rectContextMenu,
    circleContextMenu,
    textEditDialog,
    
    // 关闭方法
    closeTextContextMenu,
    closeRectContextMenu,
    closeCircleContextMenu,
    closeTextEditDialog,
    closeAllMenus,
    
    // 显示方法
    showTextContextMenu,
    showRectContextMenu,
    showCircleContextMenu,
    showTextEditDialog,
    
    // 事件处理
    handleGlobalClick
  }
}