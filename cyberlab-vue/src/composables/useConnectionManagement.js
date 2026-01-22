import { reactive } from 'vue'

export function useConnectionManagement(props, emit, saveTopologyData) {
  const connectingState = reactive({
    isActive: false,
    sourceNode: null,
    tempLink: null
  })

  const linkContextMenu = reactive({
    visible: false,
    x: 0,
    y: 0,
    link: null
  })

  const showConnectingTooltip = (sourceName) => {
    hideConnectingTooltip()
    const tooltip = document.createElement('div')
    tooltip.id = 'connecting-tooltip'
    tooltip.className = 'connecting-tooltip'
    tooltip.innerHTML = `正在连线：已选中 <strong>${sourceName}</strong>，点击另一个节点完成连线 (ESC键取消)`
    tooltip.style.zIndex = '99999'
    document.body.appendChild(tooltip)
  }

  const hideConnectingTooltip = () => {
    const tooltip = document.getElementById('connecting-tooltip')
    if (tooltip) {
      document.body.removeChild(tooltip)
    }
  }

  const startConnecting = (sourceNode) => {
    connectingState.isActive = true
    connectingState.sourceNode = sourceNode
    
    showConnectingTooltip(sourceNode.name || '未知节点')
  }

  const finishConnecting = (targetNode) => {
    if (!connectingState.isActive) {
      cancelConnecting()
      return
    }
    if (!connectingState.sourceNode) {
      cancelConnecting()
      return
    }
    if (!targetNode || !targetNode.id) {
      cancelConnecting()
      return
    }
    if (targetNode.id === connectingState.sourceNode.id) {
      // 不能连接节点到自身
      return
    }
    
    const existingLink = props.links.find(link => 
      link.source === connectingState.sourceNode.id && link.target === targetNode.id
    )
    if (existingLink) {
      // 该连接已存在
      cancelConnecting()
      return
    }

    const distance = Math.hypot(
      targetNode.x - connectingState.sourceNode.x,
      targetNode.y - connectingState.sourceNode.y
    )
    const curveness = distance > 200 ? 0.4 : 0.2
    
    const newLink = {
      id: `link-${Date.now()}`,
      source: connectingState.sourceNode.id,
      target: targetNode.id,
      label: {
        show: true,
        formatter: '连接',
        fontSize: 12,
        color: '#333',
        backgroundColor: 'rgba(255,255,255,0.7)',
        padding: [3, 6],
        borderRadius: 3
      },
      lineStyle: {
        color: '#666',
        width: 2,
        curveness: curveness,
        type: 'solid'
      },
      zlevel: 1,
      symbol: ['none', 'arrow'],
      symbolSize: 10
    }

    const updatedLinks = [...props.links, newLink]
    emit('update', { ...props, links: updatedLinks })
    saveTopologyData(updatedLinks)
    cancelConnecting()
  }

  const cancelConnecting = () => {
    connectingState.isActive = false
    connectingState.sourceNode = null
    hideConnectingTooltip()
  }

  const handleLinkMenuCommand = ({ command, link, color, newLabel, bandwidth }) => {
    if (!link) {
      // 没有连线数据
      return
    }

    // 处理连线命令

    switch (command) {
      case 'edit-label': {
        // 获取当前标签文本，需要处理不同的label格式
        let currentLabel = ''
        if (typeof link.label === 'string') {
          currentLabel = link.label
        } else if (link.label && typeof link.label === 'object') {
          currentLabel = link.label.formatter || link.label.text || ''
        }
        
        const labelToSet = newLabel !== undefined ? newLabel : prompt('请输入连线标签:', currentLabel)
        if (labelToSet !== null) { // 用户点击确定（包括空字符串）
          updateLink(link.id, { 
            label: labelToSet 
          })
        }
        break
      }
      case 'set-bandwidth': {
        const bandwidthToSet = bandwidth !== undefined ? bandwidth : prompt('请输入带宽值 (如: 100, 1000):', link.bandwidth || 100)
        if (bandwidthToSet && !isNaN(bandwidthToSet)) {
          updateLink(link.id, { 
            bandwidth: Number(bandwidthToSet), 
            label: { ...link.label, formatter: `${bandwidthToSet} Mbps` } 
          })
        }
        break
      }
      case 'change-color': {
        const colorToSet = color || prompt('请输入颜色值 (#RRGGBB):', link.lineStyle?.color || '#666666')
        if (colorToSet && /^#[0-9A-Fa-f]{6}$/.test(colorToSet)) {
          updateLink(link.id, { 
            lineStyle: { ...link.lineStyle, color: colorToSet } 
          })
        } else if (colorToSet) {
          // 颜色格式无效
        }
        break
      }
      case 'toggle-direction': {
        // 实现双向连接功能
        const isCurrentlyBidirectional = link.symbol?.[0] === 'arrow'
        if (isCurrentlyBidirectional) {
          // 切换为单向
          updateLink(link.id, {
            symbol: ['none', 'arrow'],
            label: { ...link.label, formatter: (link.label?.formatter || '连接').replace(' (双向)', '') }
          })
        } else {
          // 切换为双向
          updateLink(link.id, {
            symbol: ['arrow', 'arrow'],
            label: { ...link.label, formatter: (link.label?.formatter || '连接') + ' (双向)' }
          })
        }
        break
      }
      case 'delete-link':
        if (confirm('确定要删除此连接吗?')) {
          const updatedLinks = props.links.filter(l => l.id !== link.id)
          emit('update', { ...props, links: updatedLinks })
          saveTopologyData(updatedLinks)
        }
        break
      default:
        // 未知命令
    }
  }

  const updateLink = (linkId, updates) => {
    // 更新连线
    
    const updatedLinks = props.links.map(link => 
      link.id === linkId ? { ...link, ...updates } : link
    )
    
    // 更新后的连线列表
    
    // 发送更新事件给父组件
    emit('update', { ...props, links: updatedLinks })
    
    // 调用保存函数 - 修复参数传递
    if (saveTopologyData) {
      saveTopologyData()
    }
  }

  return {
    connectingState,
    linkContextMenu,
    startConnecting,
    finishConnecting,
    cancelConnecting,
    handleLinkMenuCommand,
    updateLink
  }
}