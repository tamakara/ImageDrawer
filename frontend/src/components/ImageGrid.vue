<template>
  <div class="image-grid-wrapper" ref="containerRef">
    <!-- 加载状态 -->
    <div v-if="imageStore.loading" class="loading-container">
      <el-skeleton :rows="6" animated />
    </div>

    <!-- 空状态 -->
    <el-empty
      v-else-if="!imageStore.loading && imageStore.images.length === 0"
      description="暂无图片数据"
      class="empty-state"
    >
      <el-button type="primary" @click="imageStore.query">刷新</el-button>
    </el-empty>

    <!-- 图片网格 -->
    <div v-else class="image-grid" :style="gridStyle">
      <ImageCard
        v-for="item in imageStore.images"
        :key="item.hash"
        :title="item.filename"
        :url="imageStore.getImageThumbnailUrl(item.hash)"
        :checked="selectedHashes.has(item.hash)"
        :device-type="deviceType"
        @click="() => openViewer(item)"
        @check="(val) => toggleSelect(item, val)"
        class="grid-item"
      />
    </div>

    <!-- 图片预览器 -->
    <ImageViewer
      v-if="selectedImage"
      :image="selectedImage"
      @close="closeViewer"
    />

    <!-- 响应式工具栏 -->
    <div v-if="imageStore.images.length > 0" class="floating-toolbar" :class="toolbarClass">
      <!-- 批量操作按钮 -->
      <div class="toolbar-actions">
        <el-button
          size="small"
          :type="checkAll ? 'primary' : 'default'"
          @click="toggleCheckAll(!checkAll)"
          :icon="checkAll ? 'CircleCheck' : 'Circle'"
        >
          {{ isMobile ? '' : (checkAll ? '取消全选' : '全选') }}
        </el-button>

        <el-button
          type="primary"
          size="small"
          :disabled="selectedImages.length === 0"
          @click="handleBatchDownload"
          :icon="Download"
        >
          {{ isMobile ? '' : '下载' }}
          <span v-if="selectedImages.length > 0" class="count-badge">
            {{ selectedImages.length }}
          </span>
        </el-button>

        <el-button
          type="danger"
          size="small"
          :disabled="selectedImages.length === 0"
          @click="confirmBatchDelete"
          :icon="Delete"
        >
          {{ isMobile ? '' : '删除' }}
        </el-button>
      </div>

      <!-- 分页器 -->
      <div class="toolbar-pagination">
        <el-pagination
          v-model:current-page="imageStore.params.page"
          v-model:page-size="imageStore.params.pageSize"
          :size="paginationSize"
          :background="!isMobile"
          :layout="paginationLayout"
          :total="imageStore.totalElements"
          :page-sizes="pageSizeOptions"
          @size-change="handlePageSizeChange"
          @current-change="handlePageChange"
          :hide-on-single-page="false"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Download, Delete } from '@element-plus/icons-vue'
import { useImageStore } from '@/stores/image'
import { getDeviceType, getResponsiveColumns, debounce, DeviceType } from '@/utils'
import ImageCard from './ImageCard.vue'
import ImageViewer from './ImageViewer.vue'
import { deleteImage, getImageFile, getImageFileZip, type Image } from '@/api'

const imageStore = useImageStore()
const containerRef = ref<HTMLElement>()
const selectedImage = ref<Image | null>(null)
const selectedHashes = ref<Set<string>>(new Set())
const deviceType = ref<DeviceType>(getDeviceType())
const columnsCount = ref(getResponsiveColumns())

// 计算属性
const selectedImages = computed(() =>
  imageStore.images.filter((img) => selectedHashes.value.has(img.hash))
)

const checkAll = computed({
  get() {
    return imageStore.images.length > 0 && selectedHashes.value.size === imageStore.images.length
  },
  set(val: boolean) {
    toggleCheckAll(val)
  }
})

const isMobile = computed(() => deviceType.value === DeviceType.MOBILE)
const isTablet = computed(() => deviceType.value === DeviceType.TABLET)

const gridStyle = computed(() => ({
  display: 'grid',
  gridTemplateColumns: `repeat(${columnsCount.value}, 1fr)`,
  gap: isMobile.value ? '8px' : isTablet.value ? '12px' : '16px',
  padding: isMobile.value ? '8px' : isTablet.value ? '12px' : '16px'
}))

const toolbarClass = computed(() => ({
  'toolbar-mobile': isMobile.value,
  'toolbar-tablet': isTablet.value,
  'toolbar-desktop': deviceType.value === DeviceType.DESKTOP
}))

const paginationSize = computed(() =>
  isMobile.value ? 'small' : 'default'
)

const paginationLayout = computed(() =>
  isMobile.value
    ? 'prev, pager, next'
    : isTablet.value
      ? 'total, prev, pager, next'
      : 'total, sizes, prev, pager, next, jumper'
)

const pageSizeOptions = computed(() =>
  isMobile.value ? [20, 50, 100] : [20, 50, 100, 200]
)

// 响应式处理
const handleResize = debounce(() => {
  deviceType.value = getDeviceType()
  columnsCount.value = getResponsiveColumns()
}, 300)

// 生命周期
onMounted(async () => {
  await imageStore.query()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})

// 图片预览
const openViewer = (image: Image) => {
  selectedImage.value = image
}

const closeViewer = () => {
  selectedImage.value = null
}

// 选择操作
function toggleCheckAll(val: boolean) {
  if (val) {
    selectedHashes.value = new Set(imageStore.images.map((img) => img.hash))
  } else {
    selectedHashes.value.clear()
  }
}

function toggleSelect(image: Image, checked: boolean) {
  if (checked) {
    selectedHashes.value.add(image.hash)
  } else {
    selectedHashes.value.delete(image.hash)
  }
}

// 分页操作
async function handlePageChange(_page: number) {
  selectedHashes.value.clear()
  await imageStore.query()
}

async function handlePageSizeChange(_size: number) {
  selectedHashes.value.clear()
  imageStore.params.page = 1
  await imageStore.query()
}

// 批量操作
async function handleBatchDownload() {
  const hashes = Array.from(selectedHashes.value)
  if (hashes.length === 0) {
    ElMessage.warning('请选择要下载的图片')
    return
  }

  try {
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

    ElMessage.success(`已开始下载 ${hashes.length} 张图片`)
  } catch (error) {
    console.error('下载失败:', error)
    ElMessage.error('下载失败，请重试')
  }
}

async function handleBatchDelete() {
  const hashes = Array.from(selectedHashes.value)
  if (hashes.length === 0) return

  try {
    await Promise.all(hashes.map(hash => deleteImage(hash)))

    imageStore.images = imageStore.images.filter(
      (img) => !selectedHashes.value.has(img.hash)
    )
    selectedHashes.value.clear()

    ElMessage.success(`已删�� ${hashes.length} 张图片`)

    // 如果当前页没有数据了，跳转到上一页
    if (imageStore.images.length === 0 && imageStore.params.page > 1) {
      imageStore.params.page--
      await imageStore.query()
    }
  } catch (error) {
    console.error('删除失败:', error)
    ElMessage.error('删除失败，请重试')
  }
}

function confirmBatchDelete() {
  const count = selectedImages.value.length
  ElMessageBox.confirm(
    `确定要删除这 ${count} 张图片吗？此操作不可撤销。`,
    '确认删除',
    {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(() => {
    handleBatchDelete()
  }).catch(() => {
    ElMessage.info('已取消删除')
  })
}
</script>

<style scoped>
.image-grid-wrapper {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--el-bg-color-page);
  overflow: hidden; /* 防止整体容器滚动 */
}

.loading-container {
  padding: 20px;
}

.empty-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.image-grid {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  /* 自定义滚动条样式 */
  scrollbar-width: thin;
  scrollbar-color: var(--el-border-color-dark) transparent;
}

/* Webkit 浏览器滚动条样式 */
.image-grid::-webkit-scrollbar {
  width: 8px;
}

.image-grid::-webkit-scrollbar-track {
  background: transparent;
}

.image-grid::-webkit-scrollbar-thumb {
  background-color: var(--el-border-color-dark);
  border-radius: 4px;
  transition: background-color 0.3s ease;
}

.image-grid::-webkit-scrollbar-thumb:hover {
  background-color: var(--el-border-color-darker);
}

.grid-item {
  width: 100%;
  /* 移除 aspect-ratio: 1; 让 ImageCard 使用自己的固定高度 */
}

.floating-toolbar {
  position: sticky;
  bottom: 0;
  background: var(--el-bg-color-overlay);
  backdrop-filter: blur(10px);
  border-top: 1px solid var(--el-border-color-light);
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.1);
  z-index: 100;
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.floating-toolbar.toolbar-mobile {
  padding: 8px 12px;
}

.toolbar-actions {
  display: flex;
  gap: 8px;
  justify-content: center;
  flex-wrap: wrap;
}

.toolbar-pagination {
  display: flex;
  justify-content: center;
}

.count-badge {
  background: var(--el-color-danger);
  color: white;
  border-radius: 10px;
  padding: 2px 6px;
  font-size: 12px;
  margin-left: 4px;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .toolbar-actions {
    gap: 4px;
  }

  .toolbar-actions :deep(.el-button) {
    flex: 1;
    min-width: 60px;
  }
}

@media (min-width: 769px) and (max-width: 1024px) {
  .floating-toolbar {
    flex-direction: row;
    justify-content: space-between;
    align-items: center;
  }

  .toolbar-pagination {
    justify-content: flex-end;
  }
}

@media (min-width: 1025px) {
  .floating-toolbar {
    flex-direction: row;
    justify-content: space-between;
    align-items: center;
  }
}
</style>
