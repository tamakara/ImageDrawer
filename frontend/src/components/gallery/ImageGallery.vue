<template>
  <div class="image-gallery">
    <!-- 加载状态 -->
    <div v-if="imageStore.loading" class="gallery-loading">
      <div class="loading-content">
        <el-skeleton :rows="6" animated />
        <div class="loading-text">正在加载图片...</div>
      </div>
    </div>

    <!-- 空状态 -->
    <el-empty
      v-else-if="!imageStore.loading && imageStore.images.length === 0"
      description="暂无符合条件的图片"
      class="gallery-empty"
    >
      <template #image>
        <el-icon size="64" color="var(--dark-text-tertiary)">
          <Picture />
        </el-icon>
      </template>
      <el-button type="primary" @click="handleRefresh" :icon="Refresh">
        刷新数据
      </el-button>
    </el-empty>

    <!-- 图片网格 -->
    <div v-else class="gallery-container">
      <!-- 工具栏 -->
      <div class="gallery-toolbar border-secondary bg-surface shadow-light">
        <div class="toolbar-left">
          <div class="selection-info">
            <el-checkbox
              v-model="isAllSelected"
              :indeterminate="isIndeterminate"
              @change="handleSelectAll"
            >
              {{ selectedCount > 0 ? `已选择 ${selectedCount} 张` : '全选' }}
            </el-checkbox>
          </div>
        </div>

        <div class="toolbar-right">
          <el-button-group v-if="selectedCount > 0" class="batch-actions">
            <el-button
              type="primary"
              :icon="Download"
              @click="handleBatchDownload"
            >
              下载 ({{ selectedCount }})
            </el-button>
            <el-button
              type="danger"
              :icon="Delete"
              @click="handleBatchDelete"
            >
              删除
            </el-button>
          </el-button-group>

          <div class="view-controls">
            <el-tooltip content="网格大小">
              <el-slider
                v-model="gridSize"
                :min="2"
                :max="8"
                :step="1"
                style="width: 80px;"
                @change="handleGridSizeChange"
              />
            </el-tooltip>
          </div>
        </div>
      </div>

      <!-- 图片网格 -->
      <div class="gallery-grid" :style="gridStyle">
        <ImageCard
          v-for="image in imageStore.images"
          :key="image.hash"
          :image="image"
          :selected="selectedImages.has(image.hash)"
          :device-type="deviceType"
          @click="handleImageClick"
          @select="handleImageSelect"
          class="gallery-item"
        />
      </div>

      <!-- 分页工具栏 -->
      <div class="gallery-pagination border-secondary bg-surface">
        <div class="pagination-info">
          <span class="info-text">
            第 {{ imageStore.params.page }} 页，共 {{ totalPages }} 页
          </span>
          <span class="info-text">
            显示第 {{ startIndex }}-{{ endIndex }} 项，共 {{ imageStore.totalElements }} 项
          </span>
        </div>

        <el-pagination
          v-model:current-page="imageStore.params.page"
          v-model:page-size="imageStore.params.pageSize"
          :total="imageStore.totalElements"
          :page-sizes="[20, 50, 100, 200]"
          layout="sizes, prev, pager, next, jumper"
          background
          @size-change="handlePageSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- 图片预览 -->
    <ImageViewer
      v-if="viewerImage"
      :image="viewerImage"
      :images="imageStore.images"
      @close="closeViewer"
      @navigate="handleViewerNavigate"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Picture, Refresh, Download, Delete } from '@element-plus/icons-vue'
import { useImageStore } from '@/stores/image'
import { getDeviceType, getResponsiveColumns, debounce, DeviceType } from '@/utils'
import { deleteImage, getImageFile, getImageFileZip, type Image } from '@/api'
import ImageCard from '@/components/image/ImageCard.vue'
import ImageViewer from '@/components/image/ImageViewer.vue'

const imageStore = useImageStore()
const deviceType = ref<DeviceType>(getDeviceType())
const selectedImages = ref<Set<string>>(new Set())
const viewerImage = ref<Image | null>(null)
const gridSize = ref(getResponsiveColumns())

// 计算属性
const selectedCount = computed(() => selectedImages.value.size)

const isAllSelected = computed({
  get: () => imageStore.images.length > 0 && selectedImages.value.size === imageStore.images.length,
  set: (value: boolean) => handleSelectAll(value)
})

const isIndeterminate = computed(() =>
  selectedImages.value.size > 0 && selectedImages.value.size < imageStore.images.length
)

const gridStyle = computed(() => ({
  display: 'grid',
  gridTemplateColumns: `repeat(${gridSize.value}, 1fr)`,
  gap: 'var(--spacing-md)',
  padding: 'var(--spacing-lg)'
}))

const totalPages = computed(() =>
  Math.ceil(imageStore.totalElements / imageStore.params.pageSize)
)

const startIndex = computed(() =>
  (imageStore.params.page - 1) * imageStore.params.pageSize + 1
)

const endIndex = computed(() =>
  Math.min(imageStore.params.page * imageStore.params.pageSize, imageStore.totalElements)
)

// 响应式处理
const handleResize = debounce(() => {
  deviceType.value = getDeviceType()
  if (!gridSize.value) {
    gridSize.value = getResponsiveColumns()
  }
}, 300)

// 生命周期
onMounted(async () => {
  await imageStore.query()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})

// 监听设备类型变化，自动调整网格大小
watch(deviceType, () => {
  gridSize.value = getResponsiveColumns()
})

// 事件处理
const handleRefresh = async () => {
  try {
    selectedImages.value.clear()
    await imageStore.query()
    ElMessage.success('刷新完成')
  } catch (error) {
    ElMessage.error('刷新失败')
  }
}

const handleSelectAll = (selected: boolean) => {
  if (selected) {
    selectedImages.value = new Set(imageStore.images.map(img => img.hash))
  } else {
    selectedImages.value.clear()
  }
}

const handleImageSelect = (image: Image, selected: boolean) => {
  if (selected) {
    selectedImages.value.add(image.hash)
  } else {
    selectedImages.value.delete(image.hash)
  }
}

const handleImageClick = (image: Image) => {
  viewerImage.value = image
}

const closeViewer = () => {
  viewerImage.value = null
}

const handleViewerNavigate = (direction: 'prev' | 'next') => {
  if (!viewerImage.value) return

  const currentIndex = imageStore.images.findIndex(img => img.hash === viewerImage.value!.hash)
  let nextIndex: number

  if (direction === 'prev') {
    nextIndex = currentIndex > 0 ? currentIndex - 1 : imageStore.images.length - 1
  } else {
    nextIndex = currentIndex < imageStore.images.length - 1 ? currentIndex + 1 : 0
  }

  viewerImage.value = imageStore.images[nextIndex]
}

const handleGridSizeChange = (size: number) => {
  gridSize.value = size
}

// 分页处理
const handlePageChange = async (_page: number) => {
  selectedImages.value.clear()
  await imageStore.query()
}

const handlePageSizeChange = async (_size: number) => {
  selectedImages.value.clear()
  imageStore.params.page = 1
  await imageStore.query()
}

// 批量操作
const handleBatchDownload = async () => {
  const hashes = Array.from(selectedImages.value)
  if (hashes.length === 0) return

  try {
    ElMessage.info('开始准备下载文件...')

    const { blob, filename } = hashes.length === 1
      ? await getImageFile(hashes[0])
      : await getImageFileZip(hashes)

    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = filename
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)

    ElMessage.success(`开始下载 ${hashes.length} 张图片`)
  } catch (error) {
    console.error('下载失败:', error)
    ElMessage.error('下载失败:请重试')
  }
}

const handleBatchDelete = async () => {
  const hashes = Array.from(selectedImages.value)
  if (hashes.length === 0) return

  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${hashes.length} 张图片吗？此操作不可撤销。`,
      '批量删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
        customClass: 'dark-message-box'
      }
    )

    ElMessage.info('正在删除图片...')
    await Promise.all(hashes.map(hash => deleteImage(hash)))

    // 更�������本地状态
    imageStore.images = imageStore.images.filter(
      img => !selectedImages.value.has(img.hash)
    )
    selectedImages.value.clear()

    ElMessage.success(`已成功删除 ${hashes.length} 张图片`)

    // 如果当前页没有数据了，跳转到上一页
    if (imageStore.images.length === 0 && imageStore.params.page > 1) {
      imageStore.params.page--
      await imageStore.query()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删��失败，请重试')
    }
  }
}
</script>

<style scoped>
.image-gallery {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--dark-bg-secondary);
}

/* 加载状��� */
.gallery-loading {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-2xl);
}

.loading-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-lg);
  max-width: 400px;
  width: 100%;
}

.loading-text {
  color: var(--dark-text-secondary);
  font-size: 14px;
}

/* 空状态 */
.gallery-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 画廊容器 */
.gallery-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow-y: auto; /* 改为允许垂直滚动 */
  overflow-x: hidden; /* 隐藏水平滚动 */
}

/* 工具栏 */
.gallery-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--spacing-md) var(--spacing-lg);
  border-bottom: 1px solid var(--dark-border-secondary);
  background: var(--dark-surface-primary);
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.selection-info {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.batch-actions {
  display: flex;
  gap: var(--spacing-sm);
}

.view-controls {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

/* 图片网格 */
.gallery-grid {
  flex: 1;
  background: var(--dark-bg-secondary);
  /* 确保网格容器正确约束子元素 */
  contain: layout style paint;
}

.gallery-item {
  width: 100%;
  /* 严格控制宽高比，防止重叠 */
  aspect-ratio: 1;
  /* 确保网格项不会溢出 */
  min-width: 0;
  min-height: 0;
  /* 防止内容溢出 */
  overflow: hidden;
  border-radius: var(--radius-lg);
  transition: all var(--transition-fast);
  /* 确保在网格中的位置固定 */
  position: relative;
  /* 防止z-index导致的重叠 */
  z-index: 1;
}

.gallery-item:hover {
  transform: translateY(-2px);
  box-shadow: var(--dark-shadow-medium);
  /* 悬浮时提高z-index，但不影响布局 */
  z-index: 2;
}

/* 分页工具栏 */
.gallery-pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--spacing-md) var(--spacing-lg);
  border-top: 1px solid var(--dark-border-secondary);
  background: var(--dark-surface-primary);
}

.pagination-info {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.info-text {
  font-size: 12px;
  color: var(--dark-text-tertiary);
}

/* 滚动条样式 */
.gallery-container::-webkit-scrollbar {
  width: 8px;
}

.gallery-container::-webkit-scrollbar-track {
  background: var(--dark-bg-primary);
}

.gallery-container::-webkit-scrollbar-thumb {
  background: var(--dark-border-primary);
  border-radius: var(--radius-sm);
}

.gallery-container::-webkit-scrollbar-thumb:hover {
  background: var(--dark-border-secondary);
}

/* Element Plus 组件样式覆盖 */
:deep(.el-checkbox__label) {
  color: var(--dark-text-primary);
}

:deep(.el-slider__runway) {
  background-color: var(--dark-surface-secondary);
}

:deep(.el-slider__bar) {
  background-color: var(--dark-primary);
}

:deep(.el-slider__button) {
  border-color: var(--dark-primary);
}

:deep(.el-pagination) {
  --el-pagination-bg-color: var(--dark-surface-secondary);
  --el-pagination-text-color: var(--dark-text-primary);
}

/* 删除原来 gallery-grid 的滚动条样式，因为现在滚动在 gallery-container 上 */
/* .gallery-grid::-webkit-scrollbar {
  width: 8px;
}

.gallery-grid::-webkit-scrollbar-track {
  background: var(--dark-bg-primary);
}

.gallery-grid::-webkit-scrollbar-thumb {
  background: var(--dark-border-primary);
  border-radius: var(--radius-sm);
}

.gallery-grid::-webkit-scrollbar-thumb:hover {
  background: var(--dark-border-secondary);
} */
</style>
