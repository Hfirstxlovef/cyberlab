// import { ref } from 'vue'

/**
 * 修复TopologyCanvas.vue中的重复声明问题的工具函数
 * 删除文件中的重复函数定义
 */
export function cleanupDuplicateDeclarations() {
  // 需要删除的重复函数名列表
  const duplicateFunctions = [
    'debouncedRender',
    'updateTextElement', 
    'updateRectElement',
    'updateCircleElement',
    'closeTextContextMenu',
    'closeRectContextMenu', 
    'closeCircleContextMenu',
    'closeTextEditDialog',
    'handleGlobalClick',
    'setCurrentTool',
    'getRectHandles',
    'getCircleHandles', 
    'startResize',
    'handleNodeClick',
    'handleCanvasClick',
    'handleDrop',
    'handleCanvasContextMenu'
  ]
  
  
  return {
    duplicateFunctions,
    message: '这些函数已经在composables中定义，需要从主组件中删除重复定义'
  }
}