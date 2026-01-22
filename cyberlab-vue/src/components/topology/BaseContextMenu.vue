<template>
  <Teleport to="body">
    <transition name="context-menu" enter-active-class="context-menu-enter-active"
      leave-active-class="context-menu-leave-active" enter-from-class="context-menu-enter-from"
      leave-from-class="context-menu-leave-from">
      <div v-if="visible" class="base-context-menu" :style="menuStyle" @click.stop @mouseleave="handleMouseLeave"
        ref="menuRef">
        <!-- 菜单标题（可选） -->
        <div v-if="title" class="menu-title">
          <span class="title-icon">{{ titleIcon }}</span>
          <span class="title-text">{{ title }}</span>
        </div>

        <!-- 菜单项 -->
        <div class="menu-content">
          <slot></slot>
        </div>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
import { ref, watch, computed, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  x: { type: Number, default: 0 },
  y: { type: Number, default: 0 },
  title: { type: String, default: '' },
  titleIcon: { type: String, default: '' }
})

const emit = defineEmits(['close'])

const menuRef = ref(null)

const menuStyle = computed(() => ({
  left: `${props.x}px`,
  top: `${props.y}px`,
  zIndex: 9999
}))

const handleMouseLeave = () => {
  // 可选的自动关闭逻辑
  // emit('close')
}

// 点击外部关闭菜单
const handleClickOutside = (event) => {
  if (props.visible && menuRef.value && !menuRef.value.contains(event.target)) {
    emit('close')
  }
}

// 按ESC键关闭菜单
const handleKeyDown = (event) => {
  if (props.visible && event.key === 'Escape') {
    emit('close')
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  document.addEventListener('keydown', handleKeyDown)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  document.removeEventListener('keydown', handleKeyDown)
})

// 监听位置变化，确保菜单不超出屏幕边界
watch([() => props.x, () => props.y, () => props.visible], () => {
  if (!props.visible) return

  setTimeout(() => {
    if (!menuRef.value) return

    const rect = menuRef.value.getBoundingClientRect()
    const viewport = {
      width: window.innerWidth,
      height: window.innerHeight
    }

    let adjustedX = props.x
    let adjustedY = props.y

    // 防止菜单超出右边界
    if (props.x + rect.width > viewport.width) {
      adjustedX = viewport.width - rect.width - 10
    }

    // 防止菜单超出下边界
    if (props.y + rect.height > viewport.height) {
      adjustedY = viewport.height - rect.height - 10
    }

    // 防止菜单超出左边界和上边界
    adjustedX = Math.max(10, adjustedX)
    adjustedY = Math.max(10, adjustedY)

    if (adjustedX !== props.x || adjustedY !== props.y) {
      menuRef.value.style.left = `${adjustedX}px`
      menuRef.value.style.top = `${adjustedY}px`
    }
  }, 0)
})
</script>

<style scoped>
/* Apple风格动画 */
.context-menu-enter-from,
.context-menu-leave-to {
  opacity: 0;
  transform: scale(0.85) translateY(-8px);
}

.context-menu-enter-active {
  transition: all 0.2s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.context-menu-leave-active {
  transition: all 0.15s cubic-bezier(0.55, 0.055, 0.675, 0.19);
}

/* 基础菜单样式 - Apple风格 */
.base-context-menu {
  position: fixed;
  background: rgba(255, 255, 255, 0.85);
  border-radius: 12px;
  box-shadow:
    0 0 0 0.5px rgba(0, 0, 0, 0.04),
    0 4px 20px rgba(0, 0, 0, 0.12),
    0 12px 40px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'Helvetica Neue', Arial, sans-serif;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  min-width: 200px;
  max-width: 280px;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

/* 菜单标题 */
.menu-title {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px 8px;
  background: linear-gradient(135deg, rgba(0, 122, 255, 0.05) 0%, rgba(88, 86, 214, 0.05) 100%);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  margin-bottom: 4px;
}

.title-icon {
  font-size: 16px;
  line-height: 1;
}

.title-text {
  font-size: 13px;
  font-weight: 600;
  color: #1d1d1f;
  letter-spacing: -0.08px;
}

/* 菜单内容区域 */
.menu-content {
  padding: 6px;
}

/* 全局菜单项样式 */
.base-context-menu :deep(.menu-item) {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s cubic-bezier(0.4, 0.0, 0.2, 1);
  font-size: 13px;
  font-weight: 400;
  color: #1d1d1f;
  margin: 1px 0;
  position: relative;
  line-height: 1.3;
  user-select: none;
}

.base-context-menu :deep(.menu-item:hover) {
  background: rgba(0, 122, 255, 0.08);
  color: #007aff;
  transform: translateX(1px);
}

.base-context-menu :deep(.menu-item:active) {
  background: rgba(0, 122, 255, 0.12);
  transform: scale(0.98) translateX(1px);
  transition: all 0.1s ease;
}

/* 菜单图标 */
.base-context-menu :deep(.menu-icon) {
  font-size: 15px;
  width: 18px;
  height: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  line-height: 1;
}

.base-context-menu :deep(.menu-text) {
  flex: 1;
  font-weight: 400;
  letter-spacing: -0.05px;
}

/* 键盘快捷键样式 */
.base-context-menu :deep(.menu-shortcut) {
  font-size: 11px;
  color: #86868b;
  font-weight: 400;
  font-family: 'SF Mono', Monaco, monospace;
}

.base-context-menu :deep(.menu-item:hover .menu-shortcut) {
  color: rgba(0, 122, 255, 0.6);
}

/* 分割线 */
.base-context-menu :deep(.menu-divider) {
  height: 1px;
  background: rgba(0, 0, 0, 0.06);
  margin: 6px 8px;
}

/* 危险操作样式（删除等） */
.base-context-menu :deep(.menu-item.destructive) {
  color: #ff3b30;
}

.base-context-menu :deep(.menu-item.destructive:hover) {
  background: rgba(255, 59, 48, 0.08);
  color: #ff3b30;
}

.base-context-menu :deep(.menu-item.destructive:active) {
  background: rgba(255, 59, 48, 0.12);
}

/* 禁用状态 */
.base-context-menu :deep(.menu-item.disabled) {
  color: #86868b;
  cursor: not-allowed;
}

.base-context-menu :deep(.menu-item.disabled:hover) {
  background: transparent;
  color: #86868b;
  transform: none;
}

/* 选中状态 */
.base-context-menu :deep(.menu-item.selected) {
  background: rgba(0, 122, 255, 0.1);
  color: #007aff;
}

.base-context-menu :deep(.menu-item.selected::after) {
  content: '✓';
  position: absolute;
  right: 12px;
  font-size: 12px;
  color: #007aff;
}

/* 响应式适配 */
@media (max-width: 768px) {
  .base-context-menu {
    min-width: 180px;
    border-radius: 10px;
  }

  .base-context-menu :deep(.menu-item) {
    padding: 12px 14px;
    font-size: 14px;
  }

  .menu-title {
    padding: 14px 16px 10px;
  }
}

/* 暗色模式支持 */
@media (prefers-color-scheme: dark) {
  .base-context-menu {
    background: rgba(28, 28, 30, 0.9);
    border-color: rgba(255, 255, 255, 0.1);
  }

  .menu-title {
    background: linear-gradient(135deg, rgba(0, 122, 255, 0.1) 0%, rgba(88, 86, 214, 0.1) 100%);
    border-bottom-color: rgba(255, 255, 255, 0.1);
  }

  .title-text,
  .base-context-menu :deep(.menu-item) {
    color: #f2f2f7;
  }

  .base-context-menu :deep(.menu-divider) {
    background: rgba(255, 255, 255, 0.1);
  }

  .base-context-menu :deep(.menu-shortcut) {
    color: #8e8e93;
  }

  .base-context-menu :deep(.menu-item.disabled) {
    color: #8e8e93;
  }
}
</style>