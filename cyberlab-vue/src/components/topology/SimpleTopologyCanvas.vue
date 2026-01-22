<template>
  <div class="simple-topology-container">
    <!-- 主画布区域 -->
    <div 
      ref="canvasRef" 
      class="canvas-area"
      @click="handleCanvasClick"
      @dragover.prevent
      @drop="handleDrop"
    >
      <!-- ECharts 图表容器 -->
      <TopologyChart 
        ref="topologyChartRef"
        :nodes="localNodes" 
        :links="localLinks"
        :scale="scale"
        :layout="layoutSettings.layout"
        :curveness="layoutSettings.curveness"
        @node-contextmenu="handleNodeContextMenu"
        @link-contextmenu="handleLinkContextMenu"
        @canvas-click="handleChartCanvasClick"
      />
      
      <!-- 布局控制面板 -->
      <TopologyLayoutControls
        ref="layoutControlsRef"
        :node-count="localNodes.length"
        :link-count="localLinks.length"
        @layout-change="handleLayoutChange"
        @curveness-change="handleCurvenessChange"
        @link-style-change="handleLinkStyleChange"
        @default-direction-change="handleDefaultDirectionChange"
        @optimize-layout="handleOptimizeLayout"
      />
      
      <!-- 自定义元素管理器 -->
      <CustomElementsManager
        ref="customElementsManagerRef"
        :custom-elements="localCustomElements"
        :canvas-ref="canvasRef"
        :chart-ref="topologyChartRef"
        @update:customElements="handleCustomElementsUpdate"
        @element-contextmenu="handleElementContextMenu"
        @element-selected="handleElementSelected"
        @render="forceRender"
      />
    </div>
    
    <!-- 自定义元素右键菜单 -->
    <ElementContextMenus 
      ref="elementContextMenusRef" 
      :is-unmounted="false" 
      @update-element="handleElementUpdate"
      @delete-element="handleElementDelete" 
      @render="forceRender" 
    />
    
    <!-- 节点和连线右键菜单 -->
    <ContextMenuManager 
      ref="contextMenuManagerRef" 
      @start-rename="handleStartRename"
      @show-icon-dialog="handleShowIconDialog" 
      @show-detail-dialog="handleShowDetailDialog"
      @show-set-type-dialog="handleShowSetTypeDialog" 
      @start-connecting="handleStartConnecting" 
      @delete-node="handleDeleteNode"
      @handle-link-command="handleLinkMenuCommand" 
    />
    
    <!-- 对话框管理器 -->
    <DialogManagerNew
      ref="dialogManagerRef"
      :nodes="localNodes"
      :links="localLinks"
      @update-data="handleDataUpdate"
      @render-chart="forceRender"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue'
import TopologyChart from './TopologyChart.vue'
import CustomElementsManager from './CustomElementsManager.vue'
import ElementContextMenus from './ElementContextMenus.vue'
import ContextMenuManager from './ContextMenuManager.vue'
import DialogManagerNew from './DialogManagerNew.vue'
import TopologyLayoutControls from './TopologyLayoutControls.vue'
import { 
  useCustomElements, 
  useTopologyData, 
  useElementFactory 
} from '@/composables/useTopologyComponents'

// Props
const props = defineProps({
  nodes: {
    type: Array,
    default: () => []
  },
  links: {
    type: Array,
    default: () => []
  },
  customElements: {
    type: Array,
    default: () => []
  },
  scale: {
    type: Number,
    default: 1
  }
})

// Emits
const emit = defineEmits(['update:customElements', 'update', 'drop-device'])

// 组合式API
const { 
  customElements: localCustomElements, 
  addElement, 
  updateElement, 
  deleteElement, 
  selectElement, 
  clearSelection, 
  clearAll 
} = useCustomElements()
const { 
  nodes: localNodes, 
  links: localLinks, 
  updateData 
} = useTopologyData()
const { createElement } = useElementFactory()

// 组件引用
const canvasRef = ref(null)
const topologyChartRef = ref(null)
const customElementsManagerRef = ref(null)
const elementContextMenusRef = ref(null)
const contextMenuManagerRef = ref(null)
const dialogManagerRef = ref(null)
const layoutControlsRef = ref(null)

// 布局设置
const layoutSettings = ref({
  layout: 'none',
  curveness: 0,
  linkStyle: 'straight',
  defaultDirection: 'forward'
})

// 初始化数据
onMounted(() => {
  // 同步props数据到本地状态
  localNodes.value = [...props.nodes]
  localLinks.value = [...props.links]
  localCustomElements.value = [...props.customElements]

  // 等待CSS calc()计算完成后再触发图表渲染
  nextTick(() => {
    // 多重延迟确保CSS完全生效
    setTimeout(() => {
      // 检查容器是否准备完毕
      const canvasArea = document.querySelector('.canvas-area')
      const editorContainer = document.querySelector('.editor-container')

      if (canvasArea && editorContainer) {
        const editorRect = editorContainer.getBoundingClientRect()

        // 如果容器高度已经计算出来，触发强制渲染
        if (editorRect.height > 100) {
          setTimeout(() => {
            forceRender()
          }, 100)
        } else {
          // 如果容器还未准备好，再等待一段时间
          setTimeout(() => {
            forceRender()
          }, 500)
        }
      }
    }, 200)
  })
})

// 监听props变化
watch(() => props.nodes, (newNodes) => {
  localNodes.value = [...newNodes]
}, { deep: true })

watch(() => props.links, (newLinks) => {
  localLinks.value = [...newLinks]
}, { deep: true })

watch(() => props.customElements, (newCustomElements) => {
  localCustomElements.value = [...newCustomElements]
}, { deep: true })

// 工具栏事件处理
// 画布点击事件处理
const handleCanvasClick = (e) => {
  // 如果点击的是空白区域，清除选中状态
  if (e.target === e.currentTarget || e.target.tagName === 'svg') {
    clearSelection()
  }
}

const handleChartCanvasClick = ({ x, y }) => {
  // 图表点击处理逻辑（如果需要的话）
}

// 自定义元素事件处理
const handleCustomElementsUpdate = (updatedElements) => {
  localCustomElements.value = updatedElements
  emit('update:customElements', updatedElements)
}

const handleElementContextMenu = (eventData) => {
  const { element } = eventData
  
  if (element.type === 'text') {
    elementContextMenusRef.value?.showTextContextMenu(eventData)
  } else if (element.type === 'rect') {
    elementContextMenusRef.value?.showRectContextMenu(eventData)
  } else if (element.type === 'circle') {
    elementContextMenusRef.value?.showCircleContextMenu(eventData)
  }
}

const handleElementSelected = (element) => {
  selectElement(element)
}

const handleElementUpdate = (elementId, updates) => {
  updateElement(elementId, updates)
  emit('update:customElements', [...localCustomElements.value])
}

const handleElementDelete = (elementId) => {
  deleteElement(elementId)
  emit('update:customElements', [...localCustomElements.value])
}

// 节点和连线右键菜单事件处理
const handleNodeContextMenu = ({ node, x, y }) => {
  const mockEvent = { clientX: x, clientY: y, preventDefault: () => {} }
  contextMenuManagerRef.value?.showNodeContextMenu(mockEvent, node)
}

const handleLinkContextMenu = ({ link, x, y }) => {
  const mockEvent = { clientX: x, clientY: y, preventDefault: () => {} }
  contextMenuManagerRef.value?.showLinkContextMenu(mockEvent, link)
}

// 节点操作事件处理
const handleStartRename = (node) => {
  dialogManagerRef.value?.showRenameDialog(node)
}

const handleShowIconDialog = (node) => {
  dialogManagerRef.value?.showIconDialog(node)
}

const handleShowDetailDialog = (node) => {
  dialogManagerRef.value?.showDetailDialog(node)
}

const handleShowSetTypeDialog = (node) => {
  dialogManagerRef.value?.showTypeDialog(node)
}

const handleStartConnecting = (node) => {
  dialogManagerRef.value?.showConnectionDialog(node)
}

const handleDeleteNode = (nodeId) => {
  if (confirm('确定要删除这个节点吗？')) {
    const updatedNodes = localNodes.value.filter(n => n.id !== nodeId)
    const updatedLinks = localLinks.value.filter(l => l.source !== nodeId && l.target !== nodeId)
    
    emit('update', { nodes: updatedNodes, links: updatedLinks })
  }
}

const handleLinkMenuCommand = (data) => {
  const { command, link, extra } = data
  
  switch (command) {
    case 'edit-label': {
      const newLabel = prompt('请输入连线标签:', link.label || '')
      if (newLabel !== null) {
        const updatedLinks = [...localLinks.value]
        const linkIndex = updatedLinks.findIndex(l => l.source === link.source && l.target === link.target)
        if (linkIndex !== -1) {
          updatedLinks[linkIndex] = { ...updatedLinks[linkIndex], label: newLabel }
          emit('update', { nodes: localNodes.value, links: updatedLinks })
        }
      }
      break
    }
    case 'set-bandwidth': {
      const bandwidth = prompt('请输入带宽:', link.bandwidth || '')
      if (bandwidth !== null && bandwidth.trim() !== '') {
        const updatedLinks = [...localLinks.value]
        const linkIndex = updatedLinks.findIndex(l => l.source === link.source && l.target === link.target)
        if (linkIndex !== -1) {
          updatedLinks[linkIndex] = { ...updatedLinks[linkIndex], bandwidth }
          emit('update', { nodes: localNodes.value, links: updatedLinks })
          // 带宽已设置
        }
      } else {
        // 带宽设置被取消或为空
      }
      break
    }
    case 'change-color': {
      const color = extra
      if (color) {
        const updatedLinks = [...localLinks.value]
        const linkIndex = updatedLinks.findIndex(l => l.source === link.source && l.target === link.target)
        if (linkIndex !== -1) {
          const currentLink = updatedLinks[linkIndex]
          updatedLinks[linkIndex] = { 
            ...currentLink, 
            lineStyle: { 
              ...(currentLink.lineStyle || {}), 
              color 
            }
          }
          emit('update', { nodes: localNodes.value, links: updatedLinks })
          // 连线颜色已设置
        }
      } else {
        // 颜色修改失败：未提供颜色值
      }
      break
    }
    case 'set-direction': {
      const direction = extra
      if (direction) {
        const updatedLinks = [...localLinks.value]
        const linkIndex = updatedLinks.findIndex(l => l.source === link.source && l.target === link.target)
        if (linkIndex !== -1) {
          updatedLinks[linkIndex] = { ...updatedLinks[linkIndex], direction }
          emit('update', { nodes: localNodes.value, links: updatedLinks })
          // 连线方向已设置
        }
      }
      break
    }
    case 'toggle-direction': {
      const updatedLinks = [...localLinks.value]
      const linkIndex = updatedLinks.findIndex(l => l.source === link.source && l.target === link.target)
      if (linkIndex !== -1) {
        const currentDirection = updatedLinks[linkIndex].direction || 'forward'
        let newDirection
        
        // 循环切换方向：单向 -> 反向 -> 双向 -> 无向 -> 单向
        switch (currentDirection) {
          case 'forward':
            newDirection = 'reverse'
            break
          case 'reverse':
            newDirection = 'bidirectional'
            break
          case 'bidirectional':
            newDirection = 'none'
            break
          case 'none':
          default:
            newDirection = 'forward'
            break
        }
        
        updatedLinks[linkIndex] = { ...updatedLinks[linkIndex], direction: newDirection }
        emit('update', { nodes: localNodes.value, links: updatedLinks })
        // 连线方向已切换
      }
      break
    }
    case 'delete-link': {
      if (confirm('确定要删除这条连线吗？')) {
        const updatedLinks = localLinks.value.filter(l => !(l.source === link.source && l.target === link.target))
        emit('update', { nodes: localNodes.value, links: updatedLinks })
      }
      break
    }
  }
}

// 数据更新处理
const handleDataUpdate = (data) => {
  updateData(data)
  emit('update', data)
}

// 拖放处理
const handleDrop = (e) => {
  if (!canvasRef.value) return
  
  const rect = canvasRef.value.getBoundingClientRect()
  const dataStr = e.dataTransfer.getData('application/json')
  
  if (!dataStr) return

  try {
    const device = JSON.parse(dataStr)
    const x = (e.clientX - rect.left) / props.scale
    const y = (e.clientY - rect.top) / props.scale
    
    emit('drop-device', { item: device, x, y })
  } catch {
    // 拖放数据解析失败
  }
}

// 强制重新渲染
const forceRender = () => {
  nextTick(() => {
    topologyChartRef.value?.updateChart()
  })
}

// 布局控制事件处理
const handleLayoutChange = (layout) => {
  layoutSettings.value.layout = layout
  // 布局模式切换
  forceRender()
}

const handleCurvenessChange = (curveness) => {
  layoutSettings.value.curveness = curveness
  // 连线弯曲度设为
  forceRender()
}

const handleLinkStyleChange = (linkStyle) => {
  layoutSettings.value.linkStyle = linkStyle
  // 连线样式切换
  forceRender()
}

const handleDefaultDirectionChange = (defaultDirection) => {
  layoutSettings.value.defaultDirection = defaultDirection
  // 默认连线方向设为
}

const handleOptimizeLayout = (optimizeSettings) => {
  layoutSettings.value.layout = optimizeSettings.layout
  layoutSettings.value.curveness = optimizeSettings.curveness
  
  // 智能优化布局
  
  // 提供用户反馈
  if (optimizeSettings.nodeCount > 20) {
    alert(`检测到${optimizeSettings.nodeCount}个节点，建议使用力导向布局以获得更好的视觉效果。`)
  }
  
  forceRender()
}

// 强制更新方法 - 与forceRender功能相同，但提供更明确的命名
const forceUpdate = () => {
  forceRender()
}

// 获取图表组件引用的方法
const getTopologyChartRef = () => {
  return topologyChartRef.value
}

// 暴露方法给父组件使用
defineExpose({
  forceRender,
  forceUpdate,
  getTopologyChartRef,
  topologyChartRef: topologyChartRef.value
})

// 组件卸载清理
onUnmounted(() => {
  // 清理可能的全局状态
  document.body.style.cursor = 'default'
})

</script>

<style scoped>
/* Apple 高雅白风格 - 画布容器 */
.simple-topology-container {
  position: relative;
  width: 100%;
  height: 100%;
  background:
    radial-gradient(circle, rgba(0, 122, 255, 0.03) 1px, transparent 1px),
    radial-gradient(circle, rgba(0, 122, 255, 0.03) 1px, transparent 1px),
    #ffffff;
  background-size: 20px 20px, 20px 20px, 100% 100%;
  background-position: 0 0, 10px 10px, 0 0;
  border: 1px solid rgba(142, 142, 147, 0.12);
  border-radius: 12px;
  overflow: hidden;
  box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.02);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
}

.canvas-area {
  position: relative;
  width: 100%;
  height: 100%;
  background: transparent;
}
</style>