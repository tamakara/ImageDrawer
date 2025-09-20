<template>
  <div
    class="image-card"
    :class="cardClass"
    @click="handleClick"
    @touchstart="handleTouchStart"
    @touchend="handleTouchEnd"
  >
    <!-- 图片容器 -->
    <div class="image-container">
      <el-image
        :src="url"
        :alt="title"
        fit="cover"
        :loading="'lazy'"
        :preview-teleported="true"
        class="card-image"
        @load="handleImageLoad"
        @error="handleImageError"
      >
        <template #placeholder>
          <div class="image-placeholder">
            <el-icon class="placeholder-icon">
              <Picture />
            </el-icon>
          </div>
        </template>
        <template #error>
          <div class="image-error">
            <el-icon class="error-icon">
              <Picture />
            </el-icon>
            <span class="error-text">���载失败</span>
          </div>
        </template>
      </el-image>

      <!-- 选择覆盖层 -->
      <div v-if="checked" class="selection-overlay">
        <el-icon class="selection-icon">
          <CircleCheck />
        </el-icon>
      </div>

      <!-- 加载指示器 -->
      <div v-if="loading" class="loading-overlay">
        <el-icon class="loading-icon is-loading">
          <Loading />
        </el-icon>
      </div>
    </div>

    <!-- 卡片信息 -->
    <div class="card-footer">
      <div class="filename-container">
        <el-tooltip
          :content="title"
          placement="top"
          :disabled="!isTitleTruncated"
        >
          <span class="filename" ref="filenameRef">{{ title }}</span>
        </el-tooltip>
      </div>

      <el-checkbox
        :model-value="checked"
        @change="handleCheck"
        @click.stop
        class="card-checkbox"
        :size="checkboxSize"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { Picture, CircleCheck, Loading } from '@element-plus/icons-vue'
import { DeviceType } from '@/utils'

interface Props {
  title: string
  url: string
  checked?: boolean
  deviceType?: DeviceType
}

interface Emits {
  (e: 'click'): void
  (e: 'check', value: boolean): void
}

const props = withDefaults(defineProps<Props>(), {
  checked: false,
  deviceType: DeviceType.DESKTOP
})

const emit = defineEmits<Emits>()

const loading = ref(false)
const imageLoaded = ref(false)
const imageError = ref(false)
const isTouchDevice = ref(false)
const touchStartTime = ref(0)
const filenameRef = ref<HTMLElement>()
const isTitleTruncated = ref(false)

// 计算属性
const cardClass = computed(() => ({
  [`card-${props.deviceType}`]: true,
  'card-checked': props.checked,
  'card-loading': loading.value,
  'card-touch': isTouchDevice.value
}))

const checkboxSize = computed(() => {
  switch (props.deviceType) {
    case DeviceType.MOBILE:
      return 'large'
    case DeviceType.TABLET:
      return 'default'
    default:
      return 'small'
  }
})

// 事件处理
const handleClick = () => {
  emit('click')
}

const handleCheck = (value: boolean) => {
  emit('check', value)
}

const handleTouchStart = () => {
  touchStartTime.value = Date.now()
}

const handleTouchEnd = () => {
  const touchDuration = Date.now() - touchStartTime.value
  if (touchDuration < 200) {
    // 短按视为点击
    handleClick()
  }
}

const handleImageLoad = () => {
  loading.value = false
  imageLoaded.value = true
  imageError.value = false
}

const handleImageError = () => {
  loading.value = false
  imageLoaded.value = false
  imageError.value = true
}

// 检查标题是否被截断
const checkTitleTruncation = async () => {
  await nextTick()
  if (filenameRef.value) {
    isTitleTruncated.value = filenameRef.value.scrollWidth > filenameRef.value.clientWidth
  }
}

// 生命周期
onMounted(() => {
  isTouchDevice.value = 'ontouchstart' in window
  loading.value = true
  checkTitleTruncation()
})
</script>

<style scoped>
.image-card {
  position: relative;
  border-radius: 12px;
  overflow: hidden;
  background: var(--el-bg-color);
  border: 2px solid transparent;
  transition: all 0.3s ease;
  cursor: pointer;
  user-select: none;
}

.image-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  border-color: var(--el-color-primary-light-7);
}

.card-checked {
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 2px var(--el-color-primary-light-8);
}

.card-loading {
  pointer-events: none;
}

/* 设备特定样式 */
.card-mobile {
  border-radius: 8px;
  font-size: 12px;
}

.card-mobile:hover {
  transform: none;
}

.card-tablet {
  border-radius: 10px;
  font-size: 13px;
}

.card-desktop {
  border-radius: 12px;
  font-size: 14px;
}

.card-touch {
  -webkit-tap-highlight-color: transparent;
}

/* 图片容器 */
.image-container {
  position: relative;
  width: 100%;
  height: 280px; /* 增加桌面端高度到280px */
  overflow: hidden;
}

/* 设备特定的图片容器高度 */
.card-mobile .image-container {
  height: 200px; /* 移动端高度增加到200px */
}

.card-tablet .image-container {
  height: 240px; /* 平板端高度增加到240px */
}

.card-desktop .image-container {
  height: 280px; /* 桌面端高度280px，显示更完整的图片 */
}

.card-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.image-card:hover .card-image {
  transform: scale(1.05);
}

.card-mobile:hover .card-image {
  transform: none;
}

/* 占位符和错误状态 */
.image-placeholder,
.image-error {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-placeholder);
}

.placeholder-icon,
.error-icon {
  font-size: 32px;
  margin-bottom: 8px;
}

.card-mobile .placeholder-icon,
.card-mobile .error-icon {
  font-size: 24px;
  margin-bottom: 4px;
}

.error-text {
  font-size: 12px;
}

/* 选择覆盖层 */
.selection-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(64, 158, 255, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(2px);
}

.selection-icon {
  font-size: 32px;
  color: var(--el-color-primary);
  background: white;
  border-radius: 50%;
  padding: 4px;
}

.card-mobile .selection-icon {
  font-size: 24px;
  padding: 2px;
}

/* 加载覆盖层 */
.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(2px);
}

.loading-icon {
  font-size: 24px;
  color: var(--el-color-primary);
}

/* 卡片底部 */
.card-footer {
  padding: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--el-bg-color);
  border-top: 1px solid var(--el-border-color-lighter);
}

.card-mobile .card-footer {
  padding: 8px;
  gap: 6px;
}

.card-tablet .card-footer {
  padding: 10px;
  gap: 7px;
}

.filename-container {
  flex: 1;
  min-width: 0;
}

.filename {
  display: block;
  font-weight: 500;
  color: var(--el-text-color-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.4;
}

.card-checkbox {
  flex-shrink: 0;
}

/* 响应式调整 */
@media (max-width: 480px) {
  .image-card {
    border-radius: 6px;
  }

  .card-footer {
    padding: 6px;
  }

  .filename {
    font-size: 11px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .image-card,
  .card-image {
    transition: none;
  }

  .image-card:hover {
    transform: none;
  }

  .image-card:hover .card-image {
    transform: none;
  }
}
</style>
