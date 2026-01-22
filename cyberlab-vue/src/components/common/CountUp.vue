<template>
  <span>{{ displayValue }}</span>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'

const props = defineProps({
  endVal: {
    type: Number,
    default: 0
  },
  startVal: {
    type: Number,
    default: 0
  },
  duration: {
    type: Number,
    default: 2
  },
  decimals: {
    type: Number,
    default: 0
  },
  separator: {
    type: String,
    default: ','
  },
  prefix: {
    type: String,
    default: ''
  },
  suffix: {
    type: String,
    default: ''
  },
  useEasing: {
    type: Boolean,
    default: true
  },
  autoplay: {
    type: Boolean,
    default: true
  }
})

const displayValue = ref(props.startVal)
let animationId = null

// 缓动函数
const easeOutExpo = (t, b, c, d) => {
  return c * (-Math.pow(2, -10 * t / d) + 1) * 1024 / 1023 + b
}

// 格式化数字
const formatNumber = (num) => {
  const fixed = num.toFixed(props.decimals)
  const parts = fixed.split('.')
  parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, props.separator)
  return props.prefix + parts.join('.') + props.suffix
}

// 开始动画
const startAnimation = () => {
  if (animationId) {
    cancelAnimationFrame(animationId)
  }

  const startTime = Date.now()
  const startValue = props.startVal
  const endValue = props.endVal
  const duration = props.duration * 1000 // 转换为毫秒

  const animate = () => {
    const now = Date.now()
    const elapsed = now - startTime
    const progress = Math.min(elapsed / duration, 1)

    let currentValue
    if (props.useEasing) {
      currentValue = easeOutExpo(progress, startValue, endValue - startValue, 1)
    } else {
      currentValue = startValue + (endValue - startValue) * progress
    }

    displayValue.value = formatNumber(currentValue)

    if (progress < 1) {
      animationId = requestAnimationFrame(animate)
    } else {
      displayValue.value = formatNumber(endValue)
    }
  }

  animate()
}

// 重置动画
const reset = () => {
  if (animationId) {
    cancelAnimationFrame(animationId)
  }
  displayValue.value = formatNumber(props.startVal)
}

// 监听 endVal 变化
watch(() => props.endVal, () => {
  if (props.autoplay) {
    startAnimation()
  }
})

// 组件挂载时自动开始动画
onMounted(() => {
  if (props.autoplay) {
    startAnimation()
  } else {
    displayValue.value = formatNumber(props.endVal)
  }
})

// 暴露方法给父组件
defineExpose({
  start: startAnimation,
  reset
})
</script>

<style scoped>
span {
  display: inline-block;
}
</style>