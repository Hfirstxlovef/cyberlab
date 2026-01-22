<template>
  <div class="video-player">
    <video
      ref="videoElement"
      :src="videoUrl"
      controls
      controlsList="nodownload"
      class="video-element"
      @loadedmetadata="onLoadedMetadata"
      @error="onError"
    >
      您的浏览器不支持视频播放
    </video>

    <div v-if="loading" class="video-loading">
      <el-icon class="is-loading">
        <Loading />
      </el-icon>
      <span>加载中...</span>
    </div>

    <div v-if="error" class="video-error">
      <el-icon>
        <CircleClose />
      </el-icon>
      <span>{{ error }}</span>
    </div>

    <div v-if="showInfo" class="video-info">
      <div class="info-item">
        <span class="label">时长:</span>
        <span class="value">{{ formatDuration(duration) }}</span>
      </div>
      <div class="info-item">
        <span class="label">分辨率:</span>
        <span class="value">{{ videoWidth }} × {{ videoHeight }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { Loading, CircleClose } from '@element-plus/icons-vue'

const props = defineProps({
  videoUrl: {
    type: String,
    required: true
  },
  showInfo: {
    type: Boolean,
    default: false
  },
  autoplay: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['loaded', 'error', 'ended'])

const videoElement = ref(null)
const loading = ref(true)
const error = ref(null)
const duration = ref(0)
const videoWidth = ref(0)
const videoHeight = ref(0)

// 格式化时长
const formatDuration = (seconds) => {
  if (!seconds || seconds === 0) return '00:00'

  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const secs = Math.floor(seconds % 60)

  if (hours > 0) {
    return `${hours}:${String(minutes).padStart(2, '0')}:${String(secs).padStart(2, '0')}`
  }
  return `${minutes}:${String(secs).padStart(2, '0')}`
}

// 视频元数据加载完成
const onLoadedMetadata = () => {
  loading.value = false
  if (videoElement.value) {
    duration.value = videoElement.value.duration
    videoWidth.value = videoElement.value.videoWidth
    videoHeight.value = videoElement.value.videoHeight

    emit('loaded', {
      duration: duration.value,
      width: videoWidth.value,
      height: videoHeight.value
    })

    if (props.autoplay) {
      videoElement.value.play().catch(err => {
        console.error('自动播放失败:', err)
      })
    }
  }
}

// 视频加载错误
const onError = (event) => {
  loading.value = false
  error.value = '视频加载失败，请检查文件是否存在'
  console.error('视频加载错误:', event)
  emit('error', event)
}

// 播放控制
const play = () => {
  if (videoElement.value) {
    videoElement.value.play()
  }
}

const pause = () => {
  if (videoElement.value) {
    videoElement.value.pause()
  }
}

const seek = (time) => {
  if (videoElement.value) {
    videoElement.value.currentTime = time
  }
}

const setPlaybackRate = (rate) => {
  if (videoElement.value) {
    videoElement.value.playbackRate = rate
  }
}

// 监听 videoUrl 变化
watch(() => props.videoUrl, () => {
  loading.value = true
  error.value = null
  duration.value = 0
  videoWidth.value = 0
  videoHeight.value = 0
})

// 监听播放结束
onMounted(() => {
  if (videoElement.value) {
    videoElement.value.addEventListener('ended', () => {
      emit('ended')
    })
  }
})

// 清理
onUnmounted(() => {
  if (videoElement.value) {
    videoElement.value.pause()
    videoElement.value.src = ''
  }
})

// 暴露方法给父组件
defineExpose({
  play,
  pause,
  seek,
  setPlaybackRate
})
</script>

<style scoped>
.video-player {
  position: relative;
  width: 100%;
  background: #000;
  border-radius: 8px;
  overflow: hidden;
}

.video-element {
  width: 100%;
  height: auto;
  display: block;
  max-height: 70vh;
}

.video-loading,
.video-error {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  color: #fff;
  font-size: 16px;
}

.video-loading .el-icon {
  font-size: 40px;
}

.video-error .el-icon {
  font-size: 40px;
  color: #f56c6c;
}

.video-info {
  position: absolute;
  bottom: 60px;
  left: 10px;
  background: rgba(0, 0, 0, 0.7);
  padding: 8px 12px;
  border-radius: 4px;
  color: #fff;
  font-size: 12px;
  display: flex;
  gap: 15px;
}

.info-item {
  display: flex;
  gap: 5px;
}

.info-item .label {
  opacity: 0.7;
}

.info-item .value {
  font-weight: 600;
}
</style>
