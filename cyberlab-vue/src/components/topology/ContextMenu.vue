<template>
  <Teleport to="body">
    <!-- 添加过渡动画 -->
    <transition
      name="context-menu"
      enter-active-class="context-menu-enter-active"
      leave-active-class="context-menu-leave-active"
      enter-from-class="context-menu-enter-from"
      leave-from-class="context-menu-leave-from"
    >
      <div 
        v-if="visible"
        class="context-menu"
        :style="menuStyle"
        @mouseleave="handleMouseLeave"
        @click.stop
        ref="menuRef"
      >
        <!-- 使用插槽内容而不是硬编码菜单项 -->
        <div @click="handleItemClick">
          <slot></slot>
        </div>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
import { ref, watch } from 'vue';

// 定义组件Props
const props = defineProps({
  x: { type: Number, default: 0 },
  y: { type: Number, default: 0 },
  visible: { type: Boolean, default: false }
})

// 定义emits
const emit = defineEmits(['command', 'close'])

// 创建菜单样式响应式对象
const menuStyle = ref({})
const menuRef = ref(null)

// 处理菜单项点击
const handleItemClick = (e) => {
  const command = e.target.getAttribute('data-command')
  if (command) {
    emit('command', command)
  }
}

// 处理鼠标离开（可选）
const handleMouseLeave = () => {
  // 可以在这里添加延迟关闭逻辑
}

// 更新菜单位置
const updatePosition = () => {
  const x = typeof props.x === 'number' ? props.x : 0
  const y = typeof props.y === 'number' ? props.y : 0
  
  menuStyle.value = {
    left: `${x}px`,
    top: `${y}px`,
    zIndex: 9999,
    minWidth: '180px',
    transition: 'opacity 0.15s ease, transform 0.15s ease'
  }

  // 菜单位置已设置
}

// 监听 x 和 y 的变化
watch([() => props.x, () => props.y], () => {
  updatePosition()
})
</script>

<style scoped>
/* Apple风格动画 */
.context-menu-enter-from,
.context-menu-leave-to {
  opacity: 0;
  transform: scale(0.9) translateY(-8px);
}

.context-menu-enter-active {
  transition: all 0.18s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}

.context-menu-leave-active {
  transition: all 0.12s cubic-bezier(0.55, 0.055, 0.675, 0.19);
}

/* Apple风格右键菜单 */
.context-menu {
  background: rgba(255, 255, 255, 0.85);
  border-radius: 12px;
  box-shadow: 
    0 0 0 1px rgba(0, 0, 0, 0.04),
    0 4px 16px rgba(0, 0, 0, 0.12),
    0 8px 32px rgba(0, 0, 0, 0.08);
  padding: 8px;
  border: none;
  overflow: hidden;
  position: fixed;
  z-index: 9999;
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Display", sans-serif;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  min-width: 180px;
}

/* Apple风格菜单项 */
.context-menu-item {
  padding: 10px 12px;
  cursor: pointer;
  transition: background-color 0.15s cubic-bezier(0.4, 0.0, 0.2, 1);
  font-size: 13px;
  font-weight: 400;
  color: #1d1d1f;
  display: flex;
  align-items: center;
  border-radius: 6px;
  margin: 1px 0;
  position: relative;
  line-height: 1.2;
}

.context-menu-item:hover {
  background-color: rgba(0, 122, 255, 0.1);
  color: #007aff;
}

.context-menu-item:active {
  background-color: rgba(0, 122, 255, 0.15);
  transform: scale(0.98);
  transition: all 0.1s ease;
}

/* 分割线样式 */
.context-menu-divider {
  height: 1px;
  background: rgba(0, 0, 0, 0.08);
  margin: 6px 8px;
}
</style>