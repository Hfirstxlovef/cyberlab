import { ref, reactive } from 'vue'

export function useTopologyGraph() {
  // 节点、边、图形元素状态
  const nodes = ref([])
  const links = ref([])
  const customElements = ref([])

  // 拖拽设备图标
  const draggedDevice = ref(null)

  // 图缩放比例
  const scale = ref(1)

  // 当前选中节点/编辑中的连线
  const selectedNode = ref(null)
  const isEditingLink = ref(false)
  const editingLink = ref(null)

  // 文字编辑相关
  const editingText = ref(false)
  const editingTextValue = ref('')
  const editingTextId = ref(null)
  const editingInputStyle = reactive({ position: 'absolute', left: '0px', top: '0px', zIndex: 1000 })

  return {
    nodes,
    links,
    customElements,
    draggedDevice,
    scale,
    selectedNode,
    isEditingLink,
    editingLink,
    editingText,
    editingTextValue,
    editingTextId,
    editingInputStyle
  }
}