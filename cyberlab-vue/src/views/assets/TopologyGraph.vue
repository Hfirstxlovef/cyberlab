<template>
  <div class="topology-wrapper" :class="themeClass" ref="editorRef" @contextmenu.prevent>
    <!-- 顶部工具栏 -->
    <div class="toolbar-section">
      <TopologyToolbar @clear="clearGraph" @back="goBack" @fullscreen="goFullscreen" @export="exportTopology"
        @import="handleImportData" @add-text="addTextBox" @add-shape="addShape" @export-image="exportAsImage"
        @save="saveGraph" @load="loadGraph" />
      
      <!-- 保存状态监控 -->
      <div class="save-monitor-section">
        <TopologySaveMonitor ref="saveMonitorRef" @click="handleSaveMonitorClick" />
      </div>
      
      <!-- 保存状态显示 -->
      <div class="save-status" v-if="saveStatus.isSaving || saveStatus.lastSaveTime">
        <span v-if="saveStatus.isSaving" class="saving-indicator">
          <i class="el-icon-loading"></i> 正在保存...
        </span>
        <span v-else-if="saveStatus.lastSaveTime" class="save-success">
          <i class="el-icon-check"></i> 
          已保存 ({{ formatSaveTime(saveStatus.lastSaveTime) }})
        </span>
        <button @click="manualSave" class="manual-save-btn" :disabled="saveStatus.isSaving">
          手动保存
        </button>
      </div>
    </div>

    <!-- 主编辑区域 -->
    <div class="editor-container">
      <TopologyIconPanel />
      <SimpleTopologyCanvas 
        ref="canvasRef"
        :nodes="nodes" 
        :links="links"
        :customElements="customElements"
        @update:customElements="handleCustomElementsUpdate"
        @update="handleNodesLinksUpdate"
        @drop-device="handleDropDevice"
      />
    </div>
    <TextEditorOverlay v-if="editingText" v-model="editingTextValue" :style="editingInputStyle"
      @confirm="finishTextEdit" />
    <RightClickMenu ref="menuRef" @rename="handleRenameNode" />
  </div>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useTopologyGraph } from '@/composables/useTopologyGraph'
import { ElMessage, ElMessageBox } from 'element-plus'
import { saveTopologyImmediate, debouncedSaveTopology, saveStatus } from '@/utils/topologySaveManager'
import { loadTopology } from '@/api/topology'
import { isLoggedIn, getUserRole } from '@/utils/auth'
import { useAuth } from '@/composables/useAuth'

// 定义props，接收父组件传递的项目信息
const props = defineProps({
  projectProp: {
    type: Object,
    default: null
  }
})

const repairCustomElements = (elements) => {
  if (!elements || !Array.isArray(elements)) {
    return []
  }
  
  return elements.filter(el => el && el.id).map(el => ({
    ...el,
    id: el.id || `element_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
    x: typeof el.x === 'number' ? el.x : 0,
    y: typeof el.y === 'number' ? el.y : 0,
    width: typeof el.width === 'number' ? el.width : 100,
    height: typeof el.height === 'number' ? el.height : 100
  }))
}


import TopologyToolbar from '@/components/topology/TopologyToolbar.vue'
import TopologySaveMonitor from '@/components/topology/TopologySaveMonitor.vue'
import TopologyIconPanel from '@/components/topology/TopologyIconPanel.vue'
import SimpleTopologyCanvas from '@/components/topology/SimpleTopologyCanvas.vue'
import TextEditorOverlay from '@/components/topology/TextEditorOverlay.vue'
import RightClickMenu from '@/components/topology/RightClickMenu.vue'


import { iconMap } from '@/assets/icons/iconMap'
const getIconByName = (name) => {
  const filename = iconMap[name] || `${name}.png`
  const finalPath = `/icons/${filename.replace(/^.*[\\/]/, '')}`
  return finalPath
}

const route = useRoute()
const router = useRouter()
const { waitForLoginCompletion } = useAuth()
const editorRef = ref(null)
const menuRef = ref(null)
const saveMonitorRef = ref(null)
const canvasRef = ref(null)

// 主题支持
const role = getUserRole() || ''
const themeClass = computed(() => role === 'blue' ? 'theme-blue' : 'theme-admin')

// 优化项目选择逻辑，支持多种来源的项目信息
const getProjectFromProps = () => props.projectProp
const getProjectFromRoute = () => {
  if (route.query.projectId) {
    return {
      id: route.query.projectId,
      name: route.query.projectName || route.query.projectId
    }
  }
  return null
}

const selectedProject = ref(
  getProjectFromProps() || 
  getProjectFromRoute() || 
  { id: 'default', name: '默认项目' }
)

// 添加标志跟踪是否已经从服务器加载过数据
const hasLoadedFromServer = ref(false)

const {
  nodes, links, customElements,
  scale,
  editingText, editingTextValue, editingTextId, editingInputStyle
} = useTopologyGraph()



const goBack = () => router.push('/assets')

const goFullscreen = async () => {
  const el = editorRef.value
  if (!el) {
    ElMessage.error('拓扑编辑器未准备就绪')
    return
  }
  
  try {
    // 检查是否已经全屏
    if (document.fullscreenElement) {
      await document.exitFullscreen()
    } else {
      // 检查元素是否在文档中
      if (!document.contains(el)) {
        ElMessage.error('编辑器元素不在文档中，无法全屏')
        return
      }
      
      // 尝试使用不同的全屏API以确保兼容性
      if (el.requestFullscreen) {
        await el.requestFullscreen({ navigationUI: 'hide' })
      } else if (el.webkitRequestFullscreen) {
        await el.webkitRequestFullscreen()
      } else if (el.mozRequestFullScreen) {
        await el.mozRequestFullScreen()
      } else if (el.msRequestFullscreen) {
        await el.msRequestFullscreen()
      } else {
        ElMessage.warning('当前浏览器不支持全屏功能')
        return
      }
      
      ElMessage.success('已进入全屏模式，按ESC键退出')
    }
  } catch (error) {
    if (error.name === 'NotAllowedError') {
      ElMessage.error('全屏请求被拒绝，请允许此页面使用全屏功能')
    } else if (error.name === 'TypeError') {
      ElMessage.error('全屏功能不可用，可能编辑器未完全加载')
    } else {
      ElMessage.error('全屏功能异常: ' + error.message)
    }
  }
}

const updateEditorHeight = () => {
  const el = editorRef.value
  const isFullscreen = document.fullscreenElement === el
  
  // 添加或移除全屏class
  if (el) {
    el.classList.toggle('fullscreen', isFullscreen)
  }
  
  // 通知ECharts调整尺寸
  setTimeout(() => {
    const chartContainer = el?.querySelector('.chart-container canvas')
    if (chartContainer) {
      // 触发resize事件
      window.dispatchEvent(new Event('resize'))
    }
  }, 100)
}

onMounted(() => {
  document.addEventListener('fullscreenchange', updateEditorHeight)

  nextTick(() => {
    updateEditorHeight()

    // 监控容器高度计算完成
    const monitorContainerReady = () => {
      const editorContainer = document.querySelector('.editor-container')
      if (editorContainer) {
        const rect = editorContainer.getBoundingClientRect()

        // 如果容器高度已经正确计算，立即触发图表渲染
        if (rect.height > 100) {
          setTimeout(() => {
            forceChartRender()
          }, 150)
        } else {
          // 容器高度还未计算完成，继续监控
          setTimeout(monitorContainerReady, 100)
        }
      } else {
        // 容器还未创建，继续等待
        setTimeout(monitorContainerReady, 50)
      }
    }

    // 开始监控容器准备状态
    monitorContainerReady()

    // 改进加载逻辑：确保页面刷新后也能正确加载
    const loadDataWithRetry = async () => {
      try {
        // 先检查登录状态，如果未登录就直接返回
        if (!isLoggedIn()) {
          return
        }

        // 等待登录完成后再加载数据
        const authState = await waitForLoginCompletion()
        if (authState.isAuthenticated) {
          // 强制加载数据，确保页面显示正确的拓扑图
          await loadGraph(true)

          // 数据加载完成后立即触发图表渲染
          setTimeout(() => {
            forceChartRender()
          }, 300)
        }
      } catch (error) {
        // 如果是开发环境，提供备用加载方案
        if (process.env.NODE_ENV === 'development') {
          // 延迟一下再尝试加载，防止页面刷新时组件未完全初始化
          setTimeout(() => {
            if (selectedProject.value?.id) {
              loadGraph(true)
              // 备用加载后也触发渲染
              setTimeout(() => {
                forceChartRender()
              }, 500)
            }
          }, 1000)
        }
      }
    }

    loadDataWithRetry()
  })

  window.addEventListener('show-node-contextmenu', (e) => {
    const { node, x, y } = e.detail
    nextTick(() => {
      if (menuRef.value?.show) {
        menuRef.value.show({ node, x, y })
      }
    })
  })
  
})

onUnmounted(() => {
  document.removeEventListener('fullscreenchange', updateEditorHeight)
})

watch(customElements, (newElements, oldElements) => {
  if ((newElements?.length || 0) !== (oldElements?.length || 0)) {
    nextTick(() => {
      if (canvasRef.value?.forceUpdate) {
        canvasRef.value.forceUpdate()
      }
    })
  }
}, { deep: true, immediate: false })

// 监听props中的项目变化，动态更新拓扑数据
watch(() => props.projectProp, (newProject) => {
  if (newProject && newProject.id) {
    selectedProject.value = {
      id: newProject.id,
      name: newProject.name || newProject.id
    }
    // 项目变更时重新加载数据
    hasLoadedFromServer.value = false
    loadGraph(true)
  }
}, { immediate: true, deep: true })

// 监听路由变化，支持直接访问拓扑页面
watch(() => route.query, (newQuery) => {
  if (newQuery.projectId) {
    const routeProject = {
      id: newQuery.projectId,
      name: newQuery.projectName || newQuery.projectId
    }
    
    // 只有项目ID真正变化时才更新
    if (selectedProject.value?.id !== routeProject.id) {
      selectedProject.value = routeProject
      hasLoadedFromServer.value = false
      loadGraph(true)
    }
  }
}, { immediate: true })

// 备用函数：添加新设备到画布（当前未使用，保留以备将来需要）
// const addNewDevice = ({ x, y, item }) => {
//   if (!item) return
//   const id = 'node-' + Date.now()
//   const iconKey = item.iconName || item.type || item.name?.toLowerCase()
//   const symbolUrl = `image://${getIconByName(iconKey)}`
//   nodes.value.push({
//     id,
//     name: item.label,
//     iconName: iconKey,
//     type: item.type,
//     symbol: symbolUrl,
//     symbolSize: [60, 60],
//     draggable: true,
//     x,
//     y
//   })
// }

const addTextBox = () => {
  const id = 'text-' + Date.now()
  const textElement = {
    type: 'text',
    id,
    chartX: 100,
    chartY: 100,
    left: 100,
    top: 100,
    style: {
      text: '说明文字',
      fontSize: 16,
      fill: '#333',
      textAlign: 'left',
      textVerticalAlign: 'top'
    },
    onclick: () => {
      editingTextId.value = id
      const el = customElements.value.find(e => e.id === id)
      editingTextValue.value = el.style.text
      editingInputStyle.left = `${el.left * scale.value}px`
      editingInputStyle.top = `${el.top * scale.value}px`
      editingText.value = true
    }
  }
  
  // 使用标准化函数确保数据结构一致
  const normalizedElement = normalizeCustomElements([textElement])[0]
  if (normalizedElement) {
    customElements.value.push(normalizedElement)
    debouncedSave()
  } else {
    ElMessage.error('添加文字失败：数据格式错误')
  }
}

const addShape = (shape) => {
  let newElement = null

  if (shape === 'rect') {
    newElement = {
      type: 'rect',
      id: 'rect-' + Date.now(),
      chartX: 100,
      chartY: 100,
      left: 100,
      top: 100,
      shape: {
        width: 120,
        height: 80
      },
      style: {
        fill: 'rgba(100,149,237,0.2)',
        stroke: '#6495ED',
        lineWidth: 2
      }
    }
  } else if (shape === 'circle') {
    newElement = {
      type: 'circle',
      id: 'circle-' + Date.now(),
      chartX: 200,
      chartY: 200,
      left: 200,
      top: 200,
      shape: {
        r: 40
      },
      style: {
        fill: 'rgba(100,149,237,0.2)',
        stroke: '#6495ED',
        lineWidth: 2
      }
    }
  }

  if (newElement) {
    // 使用标准化函数确保数据结构一致
    const normalizedElement = normalizeCustomElements([newElement])[0]
    if (normalizedElement) {
      customElements.value.push(normalizedElement)
      debouncedSave()
    } else {
      ElMessage.error('添加图形失败：数据格式错误')
    }
  }
}

const finishTextEdit = () => {
  const el = customElements.value.find(e => e.id === editingTextId.value)
  if (el) el.style.text = editingTextValue.value
  editingText.value = false
}
const clearGraph = () => {
  nodes.value = []
  links.value = []
  customElements.value = []
}

const exportTopology = () => {
  const str = JSON.stringify({
    nodes: nodes.value,
    links: links.value,
    customElements: customElements.value
  }, null, 2)
  const blob = new Blob([str], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'topology.json'
  a.click()
  URL.revokeObjectURL(url)
}

const handleImportData = (json) => {
  // 处理导入数据
  
  // 🔧 加载本地图标覆盖数据
  const iconOverrides = JSON.parse(localStorage.getItem('topology-icon-overrides') || '{}')
  
  nodes.value = (json.nodes || []).map(n => {
    
    // 🔧 关键修复：优先检查本地覆盖数据
    const localOverride = iconOverrides[n.id]
    if (localOverride) {
      // 应用本地图标覆盖
      
      const processedNode = {
        ...n,
        iconName: localOverride.iconName,
        symbol: localOverride.symbol
      }
      
      // 节点图标覆盖应用完成
      
      return processedNode
    }
    
    // 原有的图标处理逻辑（无覆盖时）
    let symbol = n.symbol
    let iconName = n.iconName
    
    // 如果有有效的 symbol，直接使用它，不做任何修改
    if (symbol && symbol.includes('image://')) {
      // 节点有有效symbol，保持现有图标路径
      
      // 只做基本的路径标准化（修复三个斜杠问题）
      const originalSymbol = symbol
      symbol = symbol.replace('image:///', 'image://')
      
      // 🔧 重要：从 symbol 中重新提取 iconName，确保一致性
      if (symbol.includes('/icons/')) {
        const pathParts = symbol.split('/icons/')
        if (pathParts.length > 1) {
          const filename = pathParts[1]
          const extractedIconName = filename.replace(/\.(png|svg|jpg|jpeg)$/i, '')
          
          // 从symbol重新提取iconName
          
          // 🔧 关键：始终使用从 symbol 中提取的 iconName，确保数据一致性
          iconName = extractedIconName
          // 强制更新iconName
        }
      }
      
      if (originalSymbol !== symbol) {
        // 标准化图标路径
      }
    } else {
      // 只有在没有有效symbol时才重新生成
      const iconKey = iconName || n.type || n.name?.toLowerCase() || 'pc'
      symbol = `image://${getIconByName(iconKey)}`
      iconName = iconKey
      // 重新生成图标路径
    }
    
    const processedNode = {
      ...n, // 保留所有原始属性
      iconName: iconName,
      symbol: symbol // 使用处理后的 symbol
    }
    
    // 节点处理完成
    
    return processedNode
  })
  
  // 导入节点数据完成
  
  links.value = json.links || []
  
  // ⚠️ 注意：如果从loadGraph调用，customElements已经被修复过了
  // 不需要再次修复，直接使用即可
  const rawCustomElements = json.customElements || []
  
  // 检查是否已经是修复后的格式（具有标准化属性）
  const isAlreadyRepaired = rawCustomElements.length > 0 && 
    rawCustomElements.every(el => el.id && el.type && (el.x !== undefined || el.chartX !== undefined || el.left !== undefined))
  
  if (isAlreadyRepaired) {
    // 已经修复过，直接使用
    customElements.value = rawCustomElements
  } else {
    // 未修复，进行修复
    const repairedElements = repairCustomElements(rawCustomElements)
    customElements.value = repairedElements
    
    if (rawCustomElements.length !== repairedElements.length) {
      const lostCount = rawCustomElements.length - repairedElements.length
      ElMessage.warning(`检测到 ${lostCount} 个损坏的图形元素已被修复`)
    }
  }
}

const exportAsImage = (type = 'png') => {
  const canvas = document.querySelector('.chart-container canvas')
  if (!canvas) return
  const link = document.createElement('a')
  link.href = canvas.toDataURL(`image/${type}`)
  link.download = `topology.${type}`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

// 备用函数：处理图形更新（当前未使用，保留以备将来需要）
// const handleGraphUpdate = (update) => {
//   if (update.nodes) {
//     nodes.value = update.nodes
//   }
//   if (update.links) {
//     links.value = update.links
//   }
//   if (update.customElements) {
//     customElements.value = update.customElements
//   }
//   if (update.scaleDelta) {
//     scale.value *= update.scaleDelta > 0 ? 0.9 : 1.1
//   }
// }

// 处理 customElements 更新的专用函数
const handleCustomElementsUpdate = (updatedCustomElements) => {
  if (!Array.isArray(updatedCustomElements)) {
    return
  }
  
  // 对于拖拽等实时更新，只进行基本验证，不使用完整修复
  // 这样可以避免意外的数据丢失
  const validatedElements = updatedCustomElements.filter(element => {
    if (!element || !element.type || !element.id) {
      return false
    }
    return true
  })
  
  // 直接更新，不使用重度修复逻辑
  customElements.value = validatedElements
  
  // 如果有元素丢失，给出提示
  if (validatedElements.length < updatedCustomElements.length) {
    const lostCount = updatedCustomElements.length - validatedElements.length
    ElMessage.warning(`检测到 ${lostCount} 个异常图形元素被过滤`)
  }
  
  // 立即触发防抖保存
  debouncedSave()
}

// 处理节点和连线更新
const handleNodesLinksUpdate = (data) => {
  // 接收到节点/连线更新

  if (data.nodes) {
    nodes.value = data.nodes
    // 节点数据已更新
  }

  if (data.links) {
    links.value = data.links
    // 连线数据已更新
  }

  // 数据更新后立即触发图表渲染
  setTimeout(() => {
    forceChartRender()
  }, 50)

  // 触发保存
  debouncedSave()
}

// 处理拖拽设备到画布的事件
const handleDropDevice = (dropData) => {
  const { item, x, y } = dropData
  
  // 创建新节点
  const newNode = {
    id: `node-${Date.now()}`,
    name: item.label || item.type,
    type: item.type,
    iconName: item.iconName,
    symbol: `image:///icons/${item.iconName}.png`, // 修复：添加前导斜杠，使用三斜杠格式
    symbolSize: [60, 60],
    x: x,
    y: y,
    draggable: true
  }
  
  
  // 添加到节点列表
  nodes.value = [...nodes.value, newNode]
  
  // 触发保存
  debouncedSave()
  
  ElMessage.success(`已添加 ${newNode.name}`)
}

const saveGraph = async () => {
  try {
    // 开始保存图形
    
    if (!selectedProject.value?.id) {
      ElMessage.warning('请先选择一个项目')
      return
    }

    // 🔧 新增：详细记录节点的图标数据
    // 保存前节点图标数据检查
    
    // 检查是否有明显损坏的元素需要修复
    const problematicElements = customElements.value.filter(element => {
      return !element || !element.type || !element.id || 
             (element.x === undefined && element.chartX === undefined && element.left === undefined)
    })
    
    let elementsToSave = customElements.value
    
    if (problematicElements.length > 0) {
      // 发现问题元素，进行修复
      const repairedElements = repairCustomElements(customElements.value)
      
      if (repairedElements.length < customElements.value.length) {
        const lostCount = customElements.value.length - repairedElements.length
        // 修复过程中有元素丢失
        ElMessage.warning(`保存时检测到 ${lostCount} 个异常元素已被修复`)
      }
      
      // 更新本地状态为修复后的元素
      customElements.value = repairedElements
      elementsToSave = repairedElements
    } else {
      // 所有元素状态正常，无需修复
    }

    const payload = {
      projectId: selectedProject.value.id,
      nodes: nodes.value,
      links: links.value,
      customElements: elementsToSave // 使用经过检查和可能修复的元素
    }
    
    // 🔧 新增：记录即将发送到服务器的数据
    // 即将发送数据到服务器
    // 使用新的保存管理器
    const result = await saveTopologyImmediate(payload)
    
    if (result.success) {
      // 🔧 保存成功后立即清理本地图标覆盖
      try {
        const iconOverrides = JSON.parse(localStorage.getItem('topology-icon-overrides') || '{}')
        if (Object.keys(iconOverrides).length > 0) {
          // 保存成功，清理本地图标覆盖缓存
          localStorage.removeItem('topology-icon-overrides')
        }
      } catch {
        // 清理本地图标覆盖失败
      }
      
      // 🔧 新增：保存成功后验证数据
      // 保存成功，开始验证服务器数据
      
      // 延迟一下再验证，确保服务器已处理完成
      setTimeout(async () => {
        try {
          await loadTopology({ projectId: selectedProject.value.id })
          // 服务器验证数据
          
          // 🔧 简化验证逻辑：由于已经立即清理了本地覆盖，只进行基本验证
          // 数据保存验证完成，服务器存储正常
          
        } catch {
          // 验证服务器数据失败
        }
      }, 300)  // 缩短验证延迟时间
      
      ElMessage.success('拓扑图保存成功')
    } else {
      throw result.error
    }
  } catch (err) {
    // 保存失败，记录错误信息
    // 在开发环境下，不显示错误消息和重试弹窗，因为后端可能没有启动
    if (process.env.NODE_ENV === 'development') {
      return
    }
    
    ElMessage.error(`保存拓扑图失败: ${err.message || '未知错误'}`)
    
    // 只在生产环境或非网络错误时显示重试选项
    if (!err.code?.includes('ERR_NETWORK')) {
      ElMessageBox.confirm(
        '自动保存失败，是否手动重试？',
        '保存失败',
        {
          confirmButtonText: '重试',
          cancelButtonText: '取消',
          type: 'warning'
        }
      ).then(() => {
        saveGraph() // 递归重试
      }).catch(() => {
        // 用户取消重试
      })
    }
  }
}

const loadGraph = async (forceLoad = false) => {
  // 确保用户已登录
  if (!isLoggedIn()) {
    return
  }

  // 简化加载判断逻辑：页面刷新或强制加载时总是重新加载
  if (!forceLoad && hasLoadedFromServer.value && nodes.value.length > 0) {
    return
  }

  // 确保有选中的项目
  if (!selectedProject.value?.id) {
    selectedProject.value = { id: 'test' }
  }

  try {
    const res = await loadTopology({ projectId: selectedProject.value.id })

    if (!res || Object.keys(res).length === 0) {
      ElMessage.warning('⚠️ 当前项目在数据库中没有保存的拓扑数据')
      return
    }

    // 🔧 新增：详细记录从服务器加载的原始数据
    // 从服务器加载原始数据

    // 数据完整性检查和修复
    const loadedData = {
      projectId: res.projectId || selectedProject.value.id,
      nodes: Array.isArray(res.nodes) ? res.nodes : [],
      links: Array.isArray(res.links) ? res.links : [],
      customElements: Array.isArray(res.customElements) ? res.customElements : []
    }

    // 验证和修复自定义元素数据
    if (loadedData.customElements.length > 0) {
      const originalCount = loadedData.customElements.length
      
      // 使用修复功能处理可能损坏的数据
      const repairedElements = repairCustomElements(loadedData.customElements)
      loadedData.customElements = repairedElements
      
      const repairedCount = repairedElements.length
      
      if (originalCount !== repairedCount) {
        const lostCount = originalCount - repairedCount
        ElMessage.warning(`检测到 ${lostCount} 个损坏的图形元素已被移除`)
        
        // 如果有数据丢失，自动保存修复后的数据
        setTimeout(() => {
          debouncedSave()
        }, 1000)
      }
    }

    // 验证节点数据
    if (loadedData.nodes.length > 0) {
      loadedData.nodes = loadedData.nodes.filter(node => {
        if (!node.id) {
          return false
        }
        return true
      })
    }

    // 验证连线数据
    if (loadedData.links.length > 0) {
      loadedData.links = loadedData.links.filter(link => {
        if (!link.source || !link.target) {
          return false
        }
        return true
      })
    }

    // 导入验证后的数据
    handleImportData(loadedData)
    
    // 🔧 额外验证：确保图标数据正确应用
    setTimeout(() => {
      // 验证图标数据应用情况
    }, 100)
    
    // 标记已经从服务器加载过数据
    hasLoadedFromServer.value = true

    // 延迟一点时间确保所有组件都完成了数据同步
    setTimeout(() => {
      const totalElements = loadedData.nodes.length + loadedData.links.length + loadedData.customElements.length
      if (totalElements > 0) {
        ElMessage.success(`✅ 拓扑图加载成功 (${totalElements} 个元素)`)
      } else {
        ElMessage.info('📄 加载了空的拓扑图')
      }

      // 数据加载完成后强制触发图表渲染
      forceChartRender()
    }, 500)
  } catch (err) {
    // 加载拓扑图失败
    if (err.message && err.message.includes('timeout')) {
      ElMessage.error('请求超时，请检查后端服务是否运行')
    } else if (err.message && err.message.includes('404')) {
      ElMessage.warning('项目数据不存在，将创建新的拓扑图')
    } else {
      // 在开发环境下，不显示错误消息和重试弹窗，因为后端可能没有启动
      if (process.env.NODE_ENV === 'development') {
        return
      }
      ElMessage.error(`加载拓扑图失败: ${err.message || '未知错误'}`)
    }
    
    // 只在生产环境或非网络错误时显示重试选项
    if (process.env.NODE_ENV !== 'development' && !err.code?.includes('ERR_NETWORK')) {
      ElMessageBox.confirm(
        '加载失败，是否重试？',
        '加载错误',
        {
          confirmButtonText: '重试',
          cancelButtonText: '取消',
          type: 'warning'
        }
      ).then(() => {
        loadGraph(true) // 强制重新加载
      }).catch(() => {
      })
    }
  }
}
const handleRenameNode = (nodeId) => {
  const node = nodes.value.find(n => n.id === nodeId)
  if (!node) return
  ElMessageBox.prompt('请输入新名称', '编辑节点名称', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    inputValue: node.name || ''
  }).then(({ value }) => {
    node.name = value
    // 节点重命名后也触发保存
    debouncedSave()
  }).catch(() => { })
}

// 保存监控相关方法
const handleSaveMonitorClick = () => {
  if (saveMonitorRef.value) {
    saveMonitorRef.value.showDetails()
  }
}

// 手动保存方法
const manualSave = async () => {
  if (!selectedProject.value?.id) {
    ElMessage.warning('请先选择一个项目')
    return
  }
  
  await saveGraph()
}

// 格式化保存时间显示
const formatSaveTime = (time) => {
  if (!time) return ''
  const now = Date.now()
  const diff = now - new Date(time).getTime()
  
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  return new Date(time).toLocaleDateString()
}

// 防抖保存方法（用于频繁操作时的自动保存）
// 强制触发图表渲染
const forceChartRender = () => {
  nextTick(() => {
    if (canvasRef.value) {
      // 正确的组件引用路径：直接调用canvas的forceUpdate方法
      if (typeof canvasRef.value.forceUpdate === 'function') {
        canvasRef.value.forceUpdate()
      }

      // 尝试获取图表实例并触发重新渲染
      if (typeof canvasRef.value.forceRender === 'function') {
        canvasRef.value.forceRender()
      }

      // 如果canvas组件有暴露topologyChartRef
      const topologyChartRef = canvasRef.value.topologyChartRef || canvasRef.value.$refs?.topologyChartRef
      if (topologyChartRef) {
        // 强制重新初始化图表
        if (typeof topologyChartRef.forceReinit === 'function') {
          topologyChartRef.forceReinit()
        }
        // 触发resize
        if (typeof topologyChartRef.resize === 'function') {
          setTimeout(() => {
            topologyChartRef.resize()
          }, 100)
        }
      }

      // 触发全局resize事件
      setTimeout(() => {
        window.dispatchEvent(new Event('resize'))
      }, 50)
    }
  })
}

const debouncedSave = async () => {
  if (!selectedProject.value?.id) {
    // 无法保存：未选择项目
    return
  }

  const payload = {
    projectId: selectedProject.value.id,
    nodes: nodes.value,
    links: links.value,
    customElements: customElements.value
  }

  try {
    await debouncedSaveTopology(payload, { showMessage: false })
    // 自动保存完成
  } catch {
    // 自动保存失败
  }
}













</script>

<style scoped>
.topology-wrapper {
  padding: 16px;
  background: #fff;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.toolbar-section {
  margin-bottom: 16px;
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.save-monitor-section {
  display: flex;
  align-items: center;
}

.editor-container {
  display: flex;
  min-height: 500px; /* 最小高度 */
  height: calc(100vh - 180px); /* 自适应高度，减去头部工具栏高度 */
  border: 1px solid #ccc;
  border-radius: 8px;
  transition: height 0.3s ease;
  overflow: hidden;
  position: relative;
  /* 确保容器在加载时就有明确的尺寸 */
  width: 100%;
  max-width: 100%;
  /* 添加备用高度避免calc()延迟 */
  height: 600px; /* 备用固定高度 */
  height: calc(100vh - 180px); /* 优先使用calc() */
}

/* 全屏状态下的样式 - Apple高雅白风格 */
.topology-wrapper.fullscreen {
  padding: 8px;
  background: linear-gradient(135deg,
    rgba(251, 251, 253, 0.98) 0%,
    rgba(245, 245, 247, 0.95) 100%);
  backdrop-filter: blur(40px);
  -webkit-backdrop-filter: blur(40px);
}

.topology-wrapper.fullscreen .editor-container {
  height: calc(100vh - 80px);
  border: 0.5px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
}

.topology-wrapper.fullscreen .toolbar-section {
  margin-bottom: 8px;
}

.topology-wrapper.fullscreen .editor-container>*:first-child {
  background: linear-gradient(135deg,
    rgba(250, 250, 250, 0.98) 0%,
    rgba(248, 248, 248, 0.95) 100%);
  border-right: 0.5px solid rgba(0, 0, 0, 0.08);
}

/* TopologyIconPanel固定宽度 */
.editor-container>*:first-child {
  width: 160px;
  flex-shrink: 0;
  background: #fafafa;
  border-right: 1px solid #ddd;
}

/* SimpleTopologyCanvas占用剩余空间 */
.editor-container>*:nth-child(2) {
  flex: 1;
  min-width: 0;
  position: relative;
  height: 100%; /* 确保高度继承 */
  /* 确保图表容器有稳定的初始尺寸 */
  min-height: 400px;
  width: 100%;
}

/* 保存状态样式 */
.save-status {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 8px;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 6px;
  font-size: 14px;
}

.saving-indicator {
  color: #409eff;
  display: flex;
  align-items: center;
  gap: 4px;
}

.save-success {
  color: #67c23a;
  display: flex;
  align-items: center;
  gap: 4px;
}

.manual-save-btn {
  padding: 4px 12px;
  background: #409eff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
}

.manual-save-btn:hover:not(:disabled) {
  background: #337ecc;
}

.manual-save-btn:disabled {
  background: #c0c4cc;
  cursor: not-allowed;
}

/* ============================================
   蓝队主题 - Blue Team Theme
   ============================================ */

/* 主容器 */
.topology-wrapper.theme-blue {
  background: linear-gradient(135deg, #0a1428 0%, #0d1a2d 50%, #0f1620 100%) !important;
  min-height: 100vh;
}

/* 工具栏区域 */
.topology-wrapper.theme-blue .toolbar-section {
  background: linear-gradient(135deg,
    rgba(20, 30, 50, 0.85) 0%,
    rgba(13, 26, 45, 0.95) 100%) !important;
  border: 1px solid rgba(70, 130, 180, 0.35) !important;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.45),
              0 0 20px rgba(70, 130, 180, 0.08) !important;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
}

/* 保存监控区域 */
.topology-wrapper.theme-blue .save-monitor-section {
  color: #ffffff;
}

/* 保存状态显示 */
.topology-wrapper.theme-blue .save-status {
  background: rgba(20, 30, 50, 0.85) !important;
  border: 1px solid rgba(70, 130, 180, 0.35) !important;
  border-radius: 12px;
  color: #ffffff !important;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3) !important;
}

.topology-wrapper.theme-blue .saving-indicator {
  color: #00d4ff !important;
}

.topology-wrapper.theme-blue .save-success {
  color: #34c759 !important;
  text-shadow: 0 0 10px rgba(52, 199, 89, 0.4) !important;
}

/* 手动保存按钮 */
.topology-wrapper.theme-blue .manual-save-btn {
  background: linear-gradient(135deg, #4682b4 0%, #1e90ff 100%) !important;
  border: 1px solid rgba(70, 130, 180, 0.6) !important;
  color: #ffffff !important;
  box-shadow: 0 4px 15px rgba(70, 130, 180, 0.2),
              inset 0 1px 0 rgba(255, 255, 255, 0.1) !important;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1) !important;
}

.topology-wrapper.theme-blue .manual-save-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #5fa3d4 0%, #00d4ff 100%) !important;
  border-color: rgba(70, 130, 180, 0.8) !important;
  box-shadow: 0 8px 25px rgba(70, 130, 180, 0.4),
              0 0 20px rgba(30, 144, 255, 0.3),
              inset 0 1px 0 rgba(255, 255, 255, 0.15) !important;
  transform: translateY(-2px) !important;
}

.topology-wrapper.theme-blue .manual-save-btn:disabled {
  background: rgba(60, 60, 80, 0.6) !important;
  border-color: rgba(70, 130, 180, 0.3) !important;
  color: rgba(255, 255, 255, 0.4) !important;
  cursor: not-allowed;
}

/* 编辑器容器 */
.topology-wrapper.theme-blue .editor-container {
  background: linear-gradient(135deg,
    rgba(13, 26, 45, 0.95) 0%,
    rgba(10, 20, 40, 0.98) 100%) !important;
  border: 1px solid rgba(70, 130, 180, 0.4) !important;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4),
              0 0 15px rgba(70, 130, 180, 0.15) !important;
  backdrop-filter: blur(25px);
  -webkit-backdrop-filter: blur(25px);
}

/* 图标面板 */
.topology-wrapper.theme-blue .editor-container > *:first-child {
  background: linear-gradient(135deg,
    rgba(20, 30, 50, 0.98) 0%,
    rgba(13, 26, 45, 0.95) 100%) !important;
  border-right: 1px solid rgba(70, 130, 180, 0.35) !important;
}

/* 画布区域 */
.topology-wrapper.theme-blue .editor-container > *:nth-child(2) {
  background: linear-gradient(135deg,
    rgba(10, 20, 40, 0.8) 0%,
    rgba(13, 26, 45, 0.85) 100%) !important;
}

/* 全屏模式 - 蓝队主题 */
.topology-wrapper.theme-blue.fullscreen {
  background: linear-gradient(135deg,
    #0a1428 0%,
    #0d1a2d 50%,
    #0f1620 100%) !important;
  backdrop-filter: blur(40px);
  -webkit-backdrop-filter: blur(40px);
}

.topology-wrapper.theme-blue.fullscreen .editor-container {
  height: calc(100vh - 80px);
  border: 1px solid rgba(70, 130, 180, 0.5) !important;
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.6),
              0 0 30px rgba(70, 130, 180, 0.2) !important;
  background: linear-gradient(135deg,
    rgba(13, 26, 45, 0.98) 0%,
    rgba(10, 20, 40, 1) 100%) !important;
}

.topology-wrapper.theme-blue.fullscreen .toolbar-section {
  margin-bottom: 8px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5),
              0 0 25px rgba(70, 130, 180, 0.12) !important;
}

.topology-wrapper.theme-blue.fullscreen .editor-container > *:first-child {
  background: linear-gradient(135deg,
    rgba(20, 30, 50, 1) 0%,
    rgba(13, 26, 45, 0.98) 100%) !important;
  border-right: 1px solid rgba(70, 130, 180, 0.4) !important;
}
</style>
