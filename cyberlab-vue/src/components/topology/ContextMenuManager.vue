<template>
  <div>
    <!-- 节点右键菜单 -->
    <BaseContextMenu :visible="contextMenu.visible" :x="contextMenu.x" :y="contextMenu.y" title="节点选项" title-icon="🔘"
      @close="contextMenu.visible = false">
      <div class="menu-item" @click="handleMenuCommand('rename')">
        <span class="menu-icon">✏️</span>
        <span class="menu-text">编辑名称</span>
        <span class="menu-shortcut">⏎</span>
      </div>

      <div class="menu-item" @click="handleMenuCommand('set-type')">
        <span class="menu-icon">🏷️</span>
        <span class="menu-text">设置类型</span>
        <span class="menu-shortcut">⌘T</span>
      </div>

      <div class="menu-item" @click="handleMenuCommand('icon')">
        <span class="menu-icon">🎨</span>
        <span class="menu-text">修改图标</span>
        <span class="menu-shortcut">⌘I</span>
      </div>

      <div class="menu-divider"></div>

      <div class="menu-item" @click="handleMenuCommand('connect')">
        <span class="menu-icon">🔗</span>
        <span class="menu-text">添加连接</span>
        <span class="menu-shortcut">⌘L</span>
      </div>

      <div class="menu-item" @click="handleMenuCommand('center')">
        <span class="menu-icon">🧭</span>
        <span class="menu-text">定位中心</span>
        <span class="menu-shortcut">⌘G</span>
      </div>

      <div class="menu-divider"></div>

      <div class="menu-item" @click="handleMenuCommand('detail')">
        <span class="menu-icon">📄</span>
        <span class="menu-text">查看详情</span>
        <span class="menu-shortcut">Space</span>
      </div>

      <div class="menu-divider"></div>

      <div class="menu-item destructive" @click="handleMenuCommand('delete')">
        <span class="menu-icon">🗑️</span>
        <span class="menu-text">删除节点</span>
        <span class="menu-shortcut">⌫</span>
      </div>
    </BaseContextMenu>

    <!-- 连接线右键菜单 -->
    <ConnectionContextMenu :visible="linkContextMenu.visible" :x="linkContextMenu.x" :y="linkContextMenu.y"
      :element="linkContextMenu.link" @edit-label="() => handleLinkMenuCommand('edit-label')"
      @set-bandwidth="() => handleLinkMenuCommand('set-bandwidth')"
      @change-color="(element, color) => handleLinkMenuCommand('change-color', color)"
      @set-direction="(element, direction) => handleLinkMenuCommand('set-direction', direction)"
      @toggle-direction="() => handleLinkMenuCommand('toggle-direction')"
      @delete-connection="() => handleLinkMenuCommand('delete-link')" @close="linkContextMenu.visible = false" />
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import BaseContextMenu from './BaseContextMenu.vue'
import ConnectionContextMenu from './ConnectionContextMenu.vue'

const emit = defineEmits([
  'start-rename',
  'show-icon-dialog',
  'show-detail-dialog',
  'show-set-type-dialog',
  'start-connecting',
  'delete-node',
  'handle-link-command'
])

// 节点右键菜单状态
const contextMenu = reactive({
  visible: false,
  x: 0,
  y: 0,
  node: null
})

// 连接线右键菜单状态  
const linkContextMenu = reactive({
  visible: false,
  x: 0,
  y: 0,
  link: null
})

// 显示节点右键菜单
const showNodeContextMenu = (e, node) => {

  const menuWidth = 180
  const menuHeight = 7 * 32
  let menuX = e.clientX + 10
  let menuY = e.clientY + 10
  const viewportWidth = window.innerWidth
  const viewportHeight = window.innerHeight

  // 边界检查与调整
  if (menuX + menuWidth > viewportWidth) {
    menuX = e.clientX - menuWidth - 10
  }
  if (menuY + menuHeight > viewportHeight) {
    menuY = e.clientY - menuHeight - 10
  }
  if (menuY < 0) {
    menuY = 10
  }
  if (menuX < 0) {
    menuX = 10
  }

  // 先关闭其他菜单
  linkContextMenu.visible = false

  contextMenu.visible = true
  contextMenu.x = menuX
  contextMenu.y = menuY
  contextMenu.node = node
}

// 显示连接线右键菜单
const showLinkContextMenu = (e, link) => {

  const menuWidth = 180
  const menuHeight = 4 * 32
  let menuX = e.clientX + 10
  let menuY = e.clientY + 10
  const viewportWidth = window.innerWidth
  const viewportHeight = window.innerHeight

  if (menuX + menuWidth > viewportWidth) {
    menuX = e.clientX - menuWidth - 10
  }
  if (menuY + menuHeight > viewportHeight) {
    menuY = e.clientY - menuHeight - 10
  }
  if (menuY < 0) {
    menuY = 10
  }
  if (menuX < 0) {
    menuX = 10
  }

  // 先隐藏节点菜单
  contextMenu.visible = false

  linkContextMenu.visible = true
  linkContextMenu.x = menuX
  linkContextMenu.y = menuY
  linkContextMenu.link = link
}

// 处理节点菜单命令
const handleMenuCommand = (command) => {
  const node = contextMenu.node
  if (!node) {
    return
  }

  contextMenu.visible = false

  switch (command) {
    case 'rename':
      emit('start-rename', node)
      break
    case 'set-type':
      emit('show-set-type-dialog', node)
      break
    case 'icon':
      emit('show-icon-dialog', node)
      break
    case 'connect':
      emit('start-connecting', node)
      break
    case 'center':
      // 实现定位到节点中心的功能
      if (window.topologyChart) {
        try {
          const chart = window.topologyChart
          const centerX = node.x || 0
          const centerY = node.y || 0


          // 使用正确的方法来实现图表的平移和缩放到节点位置
          const option = chart.getOption()
          if (option && option.series && option.series[0]) {
            // 获取当前图表的缩放和平移状态
            const zr = chart.getZr()
            const viewWidth = zr.getWidth()
            const viewHeight = zr.getHeight()

            // 计算需要的平移距离，让节点居中显示
            const targetPixel = chart.convertToPixel({ seriesIndex: 0 }, [centerX, centerY])
            const centerPixelX = viewWidth / 2
            const centerPixelY = viewHeight / 2

            // 计算平移距离（保留以备将来使用）
            const deltaX = centerPixelX - targetPixel[0]
            const deltaY = centerPixelY - targetPixel[1]

            // 使用brushEnd事件来实现平移
            chart.dispatchAction({
              type: 'brushEnd',
              areas: []
            })

            // 通过修改series的center来实现居中
            const currentOption = chart.getOption()
            if (currentOption.series[0].center) {
              currentOption.series[0].center = [centerPixelX, centerPixelY]
            }

            // 使用更温和的方式：高亮节点并给出提示
            chart.dispatchAction({
              type: 'highlight',
              seriesIndex: 0,
              name: node.name || node.id
            })

            // 给用户一个视觉反馈

            // 清除高亮
            setTimeout(() => {
              chart.dispatchAction({
                type: 'downplay',
                seriesIndex: 0,
                name: node.name || node.id
              })
            }, 2000)
          }
        } catch (error) {
          // 如果定位失败，至少提供一个有用的反馈
          alert(`节点 "${node.name || node.id}" 位置: (${node.x}, ${node.y})`)
        }
      } else {
        alert(`节点 "${node.name || node.id}" 位置: (${node.x || 0}, ${node.y || 0})`)
      }
      break
    case 'detail':
      emit('show-detail-dialog', node)
      break
    case 'delete':
      // 添加确认对话框
      if (confirm(`确定要删除节点 "${node.name || node.id}" 吗？`)) {
        emit('delete-node', node.id)
      }
      break
    default:
  }
}

// 处理连接线菜单命令
const handleLinkMenuCommand = (command, extra = null) => {
  const link = linkContextMenu.link
  if (!link) {
    return
  }

  linkContextMenu.visible = false

  // 直接调用父组件的处理函数，传递额外参数
  emit('handle-link-command', { command, link, extra })
}

// 暴露方法给父组件
defineExpose({
  showNodeContextMenu,
  showLinkContextMenu,
  contextMenu,
  linkContextMenu
})
</script>

<style scoped>
/* SVG图标样式 */
.menu-icon {
  width: 16px;
  height: 16px;
  margin-right: 8px;
  opacity: 0.7;
  transition: opacity 0.15s ease;
}

.context-menu-item:hover .menu-icon {
  opacity: 1;
}

/* 破坏性操作样式 */
.context-menu-item.destructive {
  color: #ff3b30;
}

.context-menu-item.destructive:hover {
  background-color: rgba(255, 59, 48, 0.1);
  color: #ff3b30;
}

.context-menu-item.destructive .menu-icon {
  opacity: 0.8;
}

.context-menu-item.destructive:hover .menu-icon {
  opacity: 1;
}
</style>