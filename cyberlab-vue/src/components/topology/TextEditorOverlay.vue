<template>
  <input
    class="text-edit-input"
    :value="props.modelValue"
    @input="e => emit('update:modelValue', e.target.value)"
    @blur="emit('confirm')"
    @keydown.enter="emit('confirm')"
    :style="props.style"
    ref="inputRef"
  />
</template>

<script setup>
import { onMounted, ref, nextTick, defineProps, defineEmits } from 'vue'

const props = defineProps({
  modelValue: String,
  style: Object
})

const emit = defineEmits(['update:modelValue', 'confirm'])

const inputRef = ref(null)

onMounted(() => {
  nextTick(() => {
    inputRef.value?.focus()
  })
})
</script>

<style scoped>
.text-edit-input {
  position: absolute;
  border: 1px solid #ccc;
  padding: 2px;
  background: #fff;
  font-size: 14px;
  z-index: 10;
}
</style>