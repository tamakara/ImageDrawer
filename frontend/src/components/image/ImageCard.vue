<template>
  <div
    class="image-card"
    :class="cardClasses"
    @click="handleCardClick"
  >
    <!-- 图片容器 -->
    <div class="image-container">
      <!-- 选择覆盖层 -->
      <div v-if="selected" class="selection-overlay">
        <div class="selection-indicator">
          <el-icon class="selection-icon">
            <CircleCheckFilled />
          </el-icon>
        </div>
      </div>

      <!-- 图片主体 -->
      <el-image
        :src="thumbnailUrl"
        :alt="image.filename"
        fit="cover"
        :loading="'lazy'"
        class="card-image"
        @load="handleImageLoad"
        @error="handleImageError"
      >
        <template #placeholder>
          <div class="image-placeholder">
            <el-icon class="placeholder-icon">
              <Picture />
            </el-icon>
            <div class="placeholder-text">加载中...</div>
          </div>
        </template>
        <template #error>
          <div class="image-error">
            <el-icon class="error-icon">
              <PictureFilled />
            </el-icon>
            <div class="error-text">加载失败</div>
          </div>
        </template>
      </el-image>

      <!-- 悬浮操作层 -->
      <div class="hover-overlay">
        <div class="image-actions">
          <el-button
            circle
            size="small"
            @click.stop="handleQuickView"
            title="预览"
          >
            <el-icon><View /></el-icon>
          </el-button>
          <el-button
            circle
            size="small"
            @click.stop="handleQuickDownload"
            title="下载"
          >
            <el-icon><Download /></el-icon>
          </el-button>
        </div>
      </div>
    </div>

    <!-- 卡片信息 -->
    <div class="card-footer">
      <div class="image-info">
        <el-tooltip
          :content="image.filename"
          placement="top"
          :disabled="!isTitleTruncated"
        >
          <div class="filename" ref="filenameRef">{{ image.filename }}</div>
        </el-tooltip>
        <div class="image-meta">
          <span class="file-size">{{ formatFileSize(image.size || 0) }}</span>
          <span class="rating-badge" :class="`rating-${image.rating}`">
            {{ getRatingLabel(image.rating) }}
          </span>
        </div>
      </div>

      <div class="card-controls">
        <el-checkbox
          :model-value="selected"
          @change="handleSelect"
          @click.stop
          class="selection-checkbox"
          size="large"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Picture,
  PictureFilled,
  CircleCheckFilled,
  View,
  Download
} from '@element-plus/icons-vue'
import { DeviceType } from '@/utils'
import { formatFileSize } from '@/utils'
import { getImageFile } from '@/api'
import { THUMBNAIL_BASE_URL } from '@/constants'
import type { Image } from '@/api'

interface Props {
  image: Image
  selected?: boolean
  deviceType?: DeviceType
}

interface Emits {
  (e: 'click', image: Image): void
  (e: 'select', image: Image, selected: boolean): void
}

const props = withDefaults(defineProps<Props>(), {
  selected: false,
  deviceType: DeviceType.DESKTOP
})

const emit = defineEmits<Emits>()

const filenameRef = ref<HTMLElement>()
const isTitleTruncated = ref(false)
const imageLoaded = ref(false)
const imageError = ref(false)

// 计算属性
const cardClasses = computed(() => ({
  [`card-${props.deviceType}`]: true,
  'card-selected': props.selected,
  'card-loaded': imageLoaded.value,
  'card-error': imageError.value
}))

const thumbnailUrl = computed(() => {
  // 使用正确的缩略图API路径
  return `${THUMBNAIL_BASE_URL}/${props.image.hash}?size=300`
})

// 事件处理
const handleCardClick = () => {
  emit('click', props.image)
}

const handleSelect = (selected: boolean) => {
  emit('select', props.image, selected)
}

const handleImageLoad = () => {
  imageLoaded.value = true
  imageError.value = false
}

const handleImageError = () => {
  imageLoaded.value = false
  imageError.value = true
}

const handleQuickView = () => {
  emit('click', props.image)
}

const handleQuickDownload = async () => {
  try {
    ElMessage.info('开始下载图片...')
    const { blob, filename } = await getImageFile(props.image.hash)

    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = filename
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)

    ElMessage.success('下载完成')
  } catch (error) {
    console.error('下载失败:', error)
    ElMessage.error('下载失败')
  }
}

// 工具函数
const getRatingLabel = (rating: string): string => {
  const labels: Record<string, string> = {
    'general': '一般',
    'sensitive': '敏感',
    'explicit': '裸露'
  }
  return labels[rating] || '未知'
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
  checkTitleTruncation()
})
</script>

<style scoped>
.image-card {
  /* 确保卡片完全填充网格单元格 */
  width: 100%;
  height: 100%;
  position: relative;
  background: var(--dark-bg-elevated);
  border: 2px solid transparent;
  border-radius: var(--radius-lg);
  overflow: hidden;
  cursor: pointer;
  transition: all var(--transition-base);
  user-select: none;
  /* 确保内容不会溢出网格 */
  box-sizing: border-box;
  /* 防止flex或其他布局影响 */
  display: flex;
  flex-direction: column;
}

.image-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--dark-shadow-heavy);
  border-color: var(--dark-border-accent);
  /* 确保悬浮效果不影响其他卡片位置 */
  z-index: 10;
}

.card-selected {
  border-color: var(--dark-primary);
  box-shadow: 0 0 0 2px var(--dark-primary);
}

/* 设备特定样式 */
.card-mobile {
  border-radius: var(--radius-md);
}

.card-mobile:hover {
  transform: none;
}

.card-tablet {
  border-radius: var(--radius-lg);
}

.card-desktop {
  border-radius: var(--radius-lg);
}

/* 图片容器 - 修复压扁问题 */
.image-container {
  position: relative;
  width: 100%;
  /* 给图片容器固定的宽高比，避免被压扁 */
  aspect-ratio: 1;
  overflow: hidden;
  background: var(--dark-surface-secondary);
  /* 确保图片容器不会超出卡片 */
  flex-shrink: 0;
}

.card-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform var(--transition-base);
  /* 禁用Element Plus的图片预览功能 */
  pointer-events: none;
  /* 确保图片完整显示 */
  display: block;
}

/* 重新启用卡片点击，但禁用图片预览 */
.image-card .card-image {
  pointer-events: none;
}

.image-card {
  pointer-events: auto;
}

/* Element Plus 图片组件样式修复 */
:deep(.el-image) {
  width: 100%;
  height: 100%;
  display: block;
}

:deep(.el-image__inner) {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

:deep(.el-image__wrapper) {
  width: 100%;
  height: 100%;
  display: block;
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
  gap: var(--spacing-sm);
  background: var(--dark-surface-primary);
  color: var(--dark-text-tertiary);
}

.placeholder-icon,
.error-icon {
  font-size: 32px;
}

.placeholder-text,
.error-text {
  font-size: 12px;
  text-align: center;
}

.image-error {
  color: var(--dark-error);
}

/* 选择覆盖层 */
.selection-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(66, 133, 244, 0.3);
  backdrop-filter: blur(2px);
  display: flex;
  align-items: flex-start;
  justify-content: flex-end;
  padding: var(--spacing-md);
  z-index: 10;
}

.selection-indicator {
  background: var(--dark-primary);
  border-radius: 50%;
  padding: var(--spacing-xs);
  box-shadow: var(--dark-shadow-medium);
}

.selection-icon {
  color: white;
  font-size: 20px;
}

/* 悬浮操作层 */
.hover-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: all var(--transition-base);
  z-index: 5;
}

.image-card:hover .hover-overlay {
  opacity: 1;
}

.card-mobile .hover-overlay {
  display: none;
}

.image-actions {
  display: flex;
  gap: var(--spacing-md);
}

/* 卡片底部 */
.card-footer {
  padding: var(--spacing-md);
  background: var(--dark-surface-primary);
  border-top: 1px solid var(--dark-border-secondary);
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--spacing-sm);
}

.image-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.filename {
  font-size: 13px;
  font-weight: 500;
  color: var(--dark-text-primary);
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.image-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--spacing-sm);
}

.file-size {
  font-size: 11px;
  color: var(--dark-text-tertiary);
}

.rating-badge {
  font-size: 10px;
  padding: 2px 6px;
  border-radius: var(--radius-xs);
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.rating-general {
  background: var(--dark-success);
  color: white;
}

.rating-sensitive {
  background: var(--dark-warning);
  color: white;
}

.rating-explicit {
  background: var(--dark-error);
  color: white;
}

.card-controls {
  flex-shrink: 0;
  display: flex;
  align-items: center;
}

.selection-checkbox {
  transform: scale(1.1);
}

/* 响应式调整 */
@media (max-width: 768px) {
  .card-footer {
    padding: var(--spacing-sm);
  }

  .filename {
    font-size: 12px;
  }

  .file-size {
    font-size: 10px;
  }

  .rating-badge {
    font-size: 9px;
    padding: 1px 4px;
  }
}

/* 加载动画 */
@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

.image-placeholder {
  animation: pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}

/* 无障碍 */
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

  .image-placeholder {
    animation: none;
  }
}

/* Element Plus 组件样式覆盖 */
:deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background-color: var(--dark-primary);
  border-color: var(--dark-primary);
}

:deep(.el-button) {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(4px);
  border: none;
}

:deep(.el-button:hover) {
  background: white;
  transform: scale(1.05);
}
</style>
