<template>
  <el-dialog
    v-model="visible"
    :width="dialogWidth"
    :fullscreen="isFullscreen"
    :show-close="false"
    class="image-viewer-dialog"
    append-to-body
    @close="handleClose"
  >
    <template #header>
      <div class="viewer-header">
        <div class="header-info">
          <h3 class="image-title">{{ image.filename }}</h3>
          <div class="image-details">
            <span class="detail-item">
              <el-icon><Picture /></el-icon>
              {{ formatFileSize(image.size || 0) }}
            </span>
            <span class="detail-item">
              <el-icon><Calendar /></el-icon>
              {{ formatDate(image.uploadTime) }}
            </span>
            <span class="rating-badge" :class="`rating-${image.rating}`">
              {{ getRatingLabel(image.rating) }}
            </span>
          </div>
        </div>

        <div class="header-actions">
          <el-button-group>
            <el-tooltip content="上一张 (←)">
              <el-button
                :icon="ArrowLeft"
                @click="handlePrevious"
                :disabled="!hasPrevious"
              />
            </el-tooltip>

            <el-tooltip content="下一张 (→)">
              <el-button
                :icon="ArrowRight"
                @click="handleNext"
                :disabled="!hasNext"
              />
            </el-tooltip>

            <el-tooltip :content="isFullscreen ? '退出全屏 (F)' : '全屏 (F)'">
              <el-button
                :icon="isFullscreen ? Aim : FullScreen"
                @click="toggleFullscreen"
              />
            </el-tooltip>

            <el-tooltip content="下载 (D)">
              <el-button
                :icon="Download"
                @click="handleDownload"
              />
            </el-tooltip>

            <el-tooltip content="关闭 (ESC)">
              <el-button
                :icon="Close"
                @click="handleClose"
              />
            </el-tooltip>
          </el-button-group>
        </div>
      </div>
    </template>

    <div class="viewer-content" @click="handleContentClick">
      <div class="image-wrapper" ref="imageWrapperRef">
        <el-image
          :src="imageUrl"
          :alt="image.filename"
          fit="contain"
          class="viewer-image"
          :style="imageStyle"
          @load="handleImageLoad"
          @error="handleImageError"
        >
          <template #placeholder>
            <div class="image-loading">
              <el-icon class="loading-icon" size="48">
                <Loading />
              </el-icon>
              <div class="loading-text">图片加载中...</div>
            </div>
          </template>
          <template #error>
            <div class="image-error">
              <el-icon class="error-icon" size="48">
                <PictureFilled />
              </el-icon>
              <div class="error-text">图片加载失败</div>
            </div>
          </template>
        </el-image>
      </div>

      <!-- 缩放控制 -->
      <div class="zoom-controls" v-if="!isMobile">
        <el-button-group>
          <el-tooltip content="放大 (+)">
            <el-button :icon="ZoomIn" @click="zoomIn" />
          </el-tooltip>
          <el-tooltip content="缩小 (-)">
            <el-button :icon="ZoomOut" @click="zoomOut" />
          </el-tooltip>
          <el-tooltip content="适应窗口 (0)">
            <el-button @click="resetZoom">
              <span style="font-size: 12px;">1:1</span>
            </el-button>
          </el-tooltip>
        </el-button-group>
        <div class="zoom-info">{{ Math.round(zoomLevel * 100) }}%</div>
      </div>
    </div>

    <!-- 标签信息 -->
    <template #footer v-if="image.tags && image.tags.length > 0">
      <div class="image-tags">
        <div class="tags-label">标签：</div>
        <div class="tags-list">
          <el-tag
            v-for="tag in image.tags"
            :key="tag"
            size="small"
            class="tag-item"
          >
            {{ tag }}
          </el-tag>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Picture,
  Calendar,
  ArrowLeft,
  ArrowRight,
  FullScreen,
  Aim,
  Download,
  Close,
  Loading,
  PictureFilled,
  ZoomIn,
  ZoomOut
} from '@element-plus/icons-vue'
import { formatFileSize } from '@/utils'
import { getImageFile } from '@/api'
import { IMAGE_BASE_URL } from '@/constants'
import type { Image } from '@/api'

interface Props {
  image: Image
  images?: Image[]
}

interface Emits {
  (e: 'close'): void
  (e: 'navigate', direction: 'prev' | 'next'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const visible = ref(true)
const isFullscreen = ref(false)
const zoomLevel = ref(1)
const imageWrapperRef = ref<HTMLElement>()
const isMobile = ref(false)

// 计算属性
const imageUrl = computed(() => `${IMAGE_BASE_URL}/${props.image.hash}`)

const dialogWidth = computed(() => {
  if (isMobile.value) return '100%'
  return isFullscreen.value ? '100%' : '90%'
})

const imageStyle = computed(() => ({
  transform: `scale(${zoomLevel.value})`,
  transition: 'transform 0.3s ease',
  cursor: zoomLevel.value > 1 ? 'grab' : 'default'
}))

const currentIndex = computed(() => {
  if (!props.images) return -1
  return props.images.findIndex(img => img.hash === props.image.hash)
})

const hasPrevious = computed(() => {
  return props.images && currentIndex.value > 0
})

const hasNext = computed(() => {
  return props.images && currentIndex.value < props.images.length - 1
})

// 事件处理
const handleClose = () => {
  visible.value = false
  emit('close')
}

const handlePrevious = () => {
  if (hasPrevious.value) {
    emit('navigate', 'prev')
  }
}

const handleNext = () => {
  if (hasNext.value) {
    emit('navigate', 'next')
  }
}

const toggleFullscreen = () => {
  isFullscreen.value = !isFullscreen.value
}

const handleDownload = async () => {
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
    console.error('下��失败:', error)
    ElMessage.error('下载失败')
  }
}

const handleContentClick = () => {
  // 点击内容区域可以关闭查看器
  // handleClose()
}

const handleImageLoad = () => {
  resetZoom()
}

const handleImageError = () => {
  ElMessage.error('图片加载失败')
}

// 缩���控制
const zoomIn = () => {
  zoomLevel.value = Math.min(zoomLevel.value * 1.2, 5)
}

const zoomOut = () => {
  zoomLevel.value = Math.max(zoomLevel.value / 1.2, 0.1)
}

const resetZoom = () => {
  zoomLevel.value = 1
}

// 键盘事件
const handleKeydown = (event: KeyboardEvent) => {
  switch (event.key) {
    case 'Escape':
      handleClose()
      break
    case 'ArrowLeft':
      handlePrevious()
      break
    case 'ArrowRight':
      handleNext()
      break
    case 'f':
    case 'F':
      toggleFullscreen()
      break
    case 'd':
    case 'D':
      handleDownload()
      break
    case '+':
    case '=':
      zoomIn()
      break
    case '-':
      zoomOut()
      break
    case '0':
      resetZoom()
      break
  }
}

// 工具函数
const formatDate = (dateString?: string): string => {
  if (!dateString) return '未知'
  try {
    return new Date(dateString).toLocaleDateString('zh-CN')
  } catch {
    return '未知'
  }
}

const getRatingLabel = (rating: string): string => {
  const labels: Record<string, string> = {
    'general': '一般',
    'sensitive': '敏感',
    'explicit': '裸露'
  }
  return labels[rating] || '未知'
}

// 检测移动设备
const checkMobile = () => {
  isMobile.value = window.innerWidth <= 768
}

// 生命周期
onMounted(() => {
  checkMobile()
  window.addEventListener('keydown', handleKeydown)
  window.addEventListener('resize', checkMobile)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('resize', checkMobile)
})
</script>

<style scoped>
/* 对话框样式 */
:deep(.image-viewer-dialog) {
  --el-dialog-bg-color: var(--dark-bg-elevated);
  --el-dialog-border-radius: var(--radius-lg);
}

:deep(.el-dialog__header) {
  padding: 0;
  margin: 0;
  border-bottom: 1px solid var(--dark-border-secondary);
}

:deep(.el-dialog__body) {
  padding: 0;
}

:deep(.el-dialog__footer) {
  padding: var(--spacing-lg);
  border-top: 1px solid var(--dark-border-secondary);
  background: var(--dark-surface-primary);
}

/* 头部样式 */
.viewer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--spacing-lg);
  background: var(--dark-surface-primary);
}

.header-info {
  flex: 1;
  min-width: 0;
}

.image-title {
  margin: 0 0 var(--spacing-sm) 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--dark-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.image-details {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  flex-wrap: wrap;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  font-size: 13px;
  color: var(--dark-text-secondary);
}

.rating-badge {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: var(--radius-sm);
  font-weight: 500;
  text-transform: uppercase;
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

.header-actions {
  flex-shrink: 0;
  margin-left: var(--spacing-lg);
}

/* 内容样式 */
.viewer-content {
  position: relative;
  min-height: 60vh;
  background: var(--dark-bg-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.image-wrapper {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.viewer-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

/* 加载和错误状态 */
.image-loading,
.image-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-2xl);
  color: var(--dark-text-tertiary);
}

.loading-icon {
  animation: spin 1s linear infinite;
}

.error-icon {
  color: var(--dark-error);
}

.loading-text,
.error-text {
  font-size: 14px;
}

/* 缩放控制 */
.zoom-controls {
  position: absolute;
  bottom: var(--spacing-lg);
  right: var(--spacing-lg);
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  background: rgba(0, 0, 0, 0.8);
  backdrop-filter: blur(8px);
  padding: var(--spacing-sm);
  border-radius: var(--radius-md);
}

.zoom-info {
  color: white;
  font-size: 12px;
  font-weight: 500;
  min-width: 40px;
  text-align: center;
}

/* 标签区域 */
.image-tags {
  display: flex;
  align-items: flex-start;
  gap: var(--spacing-md);
  flex-wrap: wrap;
}

.tags-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--dark-text-primary);
  flex-shrink: 0;
}

.tags-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-sm);
  flex: 1;
}

.tag-item {
  margin: 0;
}

/* 动画 */
@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* 响应式调整 */
@media (max-width: 768px) {
  .viewer-header {
    flex-direction: column;
    gap: var(--spacing-md);
    align-items: stretch;
  }

  .header-actions {
    margin-left: 0;
    align-self: center;
  }

  .image-details {
    justify-content: center;
  }

  .zoom-controls {
    display: none;
  }

  .image-tags {
    flex-direction: column;
    align-items: stretch;
  }
}

/* Element Plus 组件样式覆盖 */
:deep(.el-button-group .el-button) {
  background: var(--dark-surface-secondary);
  border-color: var(--dark-border-secondary);
  color: var(--dark-text-secondary);
}

:deep(.el-button-group .el-button:hover) {
  background: var(--dark-surface-hover);
  border-color: var(--dark-border-accent);
  color: var(--dark-text-primary);
}

:deep(.el-button-group .el-button:disabled) {
  background: var(--dark-surface-primary);
  border-color: var(--dark-border-primary);
  color: var(--dark-text-disabled);
}

:deep(.el-tag) {
  background: var(--dark-surface-secondary);
  border-color: var(--dark-border-secondary);
  color: var(--dark-text-primary);
}
</style>
