<template>
  <div class="toolbar">
    <!-- 基础操作按钮 -->
    <button class="toolbar-btn primary" @click="$emit('save')">💾 保存拓扑图</button>
    <button class="toolbar-btn warning" @click="$emit('clear')">🗑️ 清空拓扑图</button>
    <button class="toolbar-btn info" @click="$emit('back')">↩️ 返回</button>
    <button class="toolbar-btn success" @click="$emit('fullscreen')">🔍 全屏显示</button>

    <!-- 添加图形元素 -->
    <button class="toolbar-btn primary" @click="$emit('add-text')">📝 添加文字</button>
    <button class="toolbar-btn warning" @click="$emit('add-shape', 'rect')">⬜ 添加矩形</button>
    <button class="toolbar-btn success" @click="$emit('add-shape', 'circle')">⭕ 添加圆形</button>
  </div>
</template>

<script setup>
// 声明组件触发的自定义事件
const emit = defineEmits([
  'clear',
  'back',
  'fullscreen',
  'add-text',
  'add-shape',
  'save'
])
</script>

<style scoped>
/* ============================================
   Apple Elegant White Style - Toolbar
   苹果高雅白风格 - 工具栏
   ============================================ */

/* CSS Variables for consistency */
:root {
  --apple-white: #fbfbfd;
  --apple-gray: #f5f5f7;
  --apple-border: rgba(0, 0, 0, 0.04);
  --apple-text: #1d1d1f;
  --apple-text-secondary: #86868b;
  --apple-blue: #007aff;
  --apple-green: #34c759;
  --apple-orange: #ff9500;
  --apple-red: #ff3b30;
  --apple-purple: #af52de;
  --radius-sm: 12px;
  --radius-md: 16px;
  --shadow-btn: 0 2px 8px rgba(0, 0, 0, 0.06);
  --shadow-btn-hover: 0 4px 16px rgba(0, 0, 0, 0.12);
  --font-apple: -apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", "Helvetica Neue", "Segoe UI", "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif;
}

.toolbar {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
  padding: 16px 20px;
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.95) 0%,
    rgba(248, 248, 248, 0.85) 100%);
  border-radius: var(--radius-md);
  border: 0.5px solid var(--apple-border);
  align-items: center;
  min-height: 70px;
  z-index: 100;
  position: relative;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  font-family: var(--font-apple);
}

/* ============================================
   Apple White Frosted Glass Buttons
   ============================================ */
.toolbar-btn {
  padding: 10px 18px;
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.95) 0%,
    rgba(250, 250, 250, 0.9) 100%);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 130px;
  justify-content: center;
  box-shadow: var(--shadow-btn);
  position: relative;
  overflow: hidden;
}

.toolbar-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, var(--btn-color-start), var(--btn-color-end));
  opacity: 0;
  transition: opacity 0.3s ease;
}

.toolbar-btn:hover::before {
  opacity: 1;
}

.toolbar-btn:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-btn-hover);
  border-color: rgba(0, 0, 0, 0.1);
}

.toolbar-btn:active {
  transform: translateY(0);
  box-shadow: var(--shadow-btn);
  transition: all 0.1s ease;
}

/* ============================================
   Button Color Variants with Apple Colors
   ============================================ */

/* Apple System Blue - Primary Actions */
.toolbar-btn.primary {
  color: var(--apple-blue);
  --btn-color-start: #007aff;
  --btn-color-end: #5ac8fa;
}

.toolbar-btn.primary:hover {
  background: linear-gradient(135deg,
    rgba(0, 122, 255, 0.08) 0%,
    rgba(90, 200, 250, 0.06) 100%);
}

/* Apple System Red - Warning/Destructive Actions */
.toolbar-btn.warning {
  color: var(--apple-red);
  --btn-color-start: #ff3b30;
  --btn-color-end: #ff453a;
}

.toolbar-btn.warning:hover {
  background: linear-gradient(135deg,
    rgba(255, 59, 48, 0.08) 0%,
    rgba(255, 69, 58, 0.06) 100%);
}

/* Apple System Green - Success Actions */
.toolbar-btn.success {
  color: var(--apple-green);
  --btn-color-start: #34c759;
  --btn-color-end: #30d158;
}

.toolbar-btn.success:hover {
  background: linear-gradient(135deg,
    rgba(52, 199, 89, 0.08) 0%,
    rgba(48, 209, 88, 0.06) 100%);
}

/* Apple System Purple - Info Actions */
.toolbar-btn.info {
  color: var(--apple-purple);
  --btn-color-start: #af52de;
  --btn-color-end: #bf5af2;
}

.toolbar-btn.info:hover {
  background: linear-gradient(135deg,
    rgba(175, 82, 222, 0.08) 0%,
    rgba(191, 90, 242, 0.06) 100%);
}

/* ============================================
   Responsive Design
   ============================================ */
@media (max-width: 768px) {
  .toolbar {
    padding: 12px 16px;
    gap: 8px;
  }

  .toolbar-btn {
    min-width: 110px;
    padding: 8px 14px;
    font-size: 13px;
  }
}

@media (max-width: 576px) {
  .toolbar {
    flex-direction: column;
  }

  .toolbar-btn {
    width: 100%;
  }
}
</style>