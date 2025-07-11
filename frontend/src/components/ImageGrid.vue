<template>
  <div class="image-list-wrapper" ref="containerRef">
    <el-row :gutter="16" justify="center">
      <el-col
          v-for="item in imageStore.images"
          :key="item.hash"
          :span="colSpan"
          class="image-grid-item"
      >
        <ImageCard
            :title="item.filename"
            :url="imageStore.getImageThumbnailUrl(item.hash)"
            :checked="selectedHashes.has(item.hash)"
            @click="() => openViewer(item)"
            @check="(val) => toggleSelect(item, val)"
            style="height: 280px"
        />
      </el-col>
    </el-row>

    <ImageViewer
        v-if="selectedImage"
        :image="selectedImage"
        @close="closeViewer"
    />

    <!-- 底部悬浮工具栏，两行 -->
    <div class="batch-toolbar" v-show="imageStore.images.length">
      <div class="toolbar-top">
        <el-button
            size="small"
            :type="checkAll ? 'primary' : 'default'"
            @click="toggleCheckAll(!checkAll)"
        >
          {{ checkAll ? '取消全选' : '全选' }}
        </el-button>

        <el-button
            type="primary"
            size="small"
            :disabled="selectedImages.length === 0"
            @click="handleBatchDownload"
        >
          批量下载
        </el-button>

        <el-button
            type="danger"
            size="small"
            :disabled="selectedImages.length === 0"
            @click="confirmBatchDelete"
        >
          批量删除
        </el-button>
      </div>

      <div class="toolbar-bottom">
        <el-pagination
            size="small"
            background
            layout="total, sizes, prev, pager, next, jumper"
            :total="imageStore.totalElements"
            :page-size="imageStore.params.pageSize"
            :current-page="imageStore.params.page"
            :page-sizes="[20, 50, 100, 200]"
            @size-change="handlePageSizeChange"
            @current-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { useImageStore } from '../stores/image'
import ImageCard from './ImageCard.vue'
import ImageViewer from './ImageViewer.vue'
import { deleteImage, getImageFile, getImageFileZip, type Image } from '../api'

const imageStore = useImageStore()

const colSpan = 6
const selectedImage = ref<Image | null>(null)

onMounted(async () => {
  await imageStore.query()
})

const openViewer = (image: Image) => {
  selectedImage.value = image
}
const closeViewer = () => {
  selectedImage.value = null
}

const selectedHashes = ref<Set<string>>(new Set())
const selectedImages = computed(() =>
    imageStore.images.filter((img) => selectedHashes.value.has(img.hash))
)
const checkAll = computed({
  get() {
    return (
        imageStore.images.length > 0 &&
        selectedHashes.value.size === imageStore.images.length
    )
  },
  set(val: boolean) {
    toggleCheckAll(val)
  },
})

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

async function handlePageChange(page: number) {
  imageStore.params.page = page
  await imageStore.query()
}

async function handlePageSizeChange(size: number) {
  imageStore.params.pageSize = size
  imageStore.params.page = 1
  await imageStore.query()
}

async function handleBatchDownload() {
  const hashes = Array.from(selectedHashes.value)
  if (hashes.length === 0) {
    ElMessage.warning('请选择要下载的图片')
    return
  }

  const { blob, filename } =
      hashes.length === 1
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
}

async function handleBatchDelete() {
  const hashes = Array.from(selectedHashes.value)
  if (hashes.length === 0) {
    ElMessage.warning('请选择要删除的图片')
    return
  }

  try {
    for (const hash of hashes) {
      await deleteImage(hash)
    }

    imageStore.images = imageStore.images.filter(
        (img) => !selectedHashes.value.has(img.hash)
    )
    selectedHashes.value.clear()

    ElMessage.success(`已删除 ${hashes.length} 张图片`)
  } catch (e) {
    console.error(e)
    ElMessage.error('删除失败')
  }
}

function confirmBatchDelete() {
  ElMessageBox.confirm(
      `确定要删除这 ${selectedImages.value.length} 张图片吗？此操作不可撤销。`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
  )
      .then(() => {
        handleBatchDelete()
      })
      .catch(() => {
        ElMessage.info('已取消删除')
      })
}
</script>

<style scoped>
.image-list-wrapper {
  padding: 15px;
  background: var(--el-bg-color-page);
  min-height: 100vh;
  box-sizing: border-box;
}

.image-grid-item {
  height: 280px;
  margin-bottom: 16px;
}

.batch-toolbar {
  position: fixed;
  bottom: 10px;
  left: 50%;
  transform: translateX(-50%);
  background-color: var(--el-bg-color-overlay);
  box-shadow: var(--el-box-shadow-lighter);
  border: 1px solid var(--el-border-color-light);
  padding: 8px 20px 12px;
  border-radius: 8px;

  display: flex;
  flex-direction: column;
  align-items: center;

  max-width: 95vw;
  white-space: nowrap;

  z-index: 100;
  backdrop-filter: blur(8px);
}

.toolbar-top {
  display: flex;
  gap: 12px;
  margin-bottom: 8px;
}

.toolbar-bottom {
  width: 100%;
  display: flex;
  justify-content: center;
}
</style>
