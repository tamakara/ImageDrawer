<template>
  <div class="app">
    <el-container>
      <!-- 顶部导航栏 -->
      <el-header height="60px">
        <div class="header-content">
          <div class="logo">
            <h2>图片管理器</h2>
          </div>
          <div class="header-stats">
            <span v-if="imageStore.total > 0">共 {{ imageStore.total }} 张图片</span>
            <span v-if="imageStore.hasSelection" class="selection-info">
              已选择 {{ imageStore.selectedImages.size }} 张
            </span>
          </div>
          <div class="header-actions">
            <el-button
              :icon="Refresh"
              @click="imageStore.refresh"
              :loading="imageStore.loading"
              title="刷新"
            >
              刷新
            </el-button>
          </div>
        </div>
      </el-header>

      <el-container>
        <!-- 左侧过滤面板 -->
        <el-aside width="300px">
          <FilterPanel />
        </el-aside>

        <!-- 主内容区域 -->
        <el-main>
          <!-- 快捷操作栏 -->
          <div class="toolbar" v-if="imageStore.hasImages">
            <div class="toolbar-left">
              <el-checkbox
                :indeterminate="isIndeterminate"
                v-model="isAllSelected"
                @change="handleSelectAll"
              >
                全选
              </el-checkbox>
              <span class="selection-count" v-if="imageStore.hasSelection">
                已选择 {{ imageStore.selectedImages.size }} 张
              </span>
            </div>
            <div class="toolbar-right">
              <el-button-group v-if="imageStore.hasSelection">
                <el-button
                  type="primary"
                  :icon="Download"
                  @click="downloadSelected"
                  :loading="downloading"
                >
                  下载
                </el-button>
                <el-button
                  type="danger"
                  :icon="Delete"
                  @click="deleteSelected"
                  :loading="deleting"
                >
                  删除
                </el-button>
              </el-button-group>
            </div>
          </div>

          <!-- 图片网格 -->
          <div v-if="imageStore.loading" class="loading">
            <el-icon class="is-loading"><Loading /></el-icon>
            <span style="margin-left: 8px">加载中...</span>
          </div>

          <div v-else-if="imageStore.isEmpty" class="empty-state">
            <div class="empty-icon">📷</div>
            <div class="empty-title">暂无图片</div>
            <div class="empty-subtitle">上传一些图片开始使用吧</div>
          </div>

          <div v-else>
            <div class="image-grid">
              <ImageCard
                v-for="image in imageStore.images"
                :key="image.hash"
                :image="image"
                @preview="showPreview"
                @delete="deleteImage"
              />
            </div>

            <!-- 分页控制 -->
            <div class="pagination-wrapper" v-if="imageStore.totalPages > 1">
              <el-pagination
                v-model:current-page="currentPage"
                :page-size="imageStore.queryParams.pageSize"
                :total="imageStore.total"
                :page-sizes="[20, 50, 100, 200]"
                layout="total, sizes, prev, pager, next, jumper"
                @size-change="handleSizeChange"
                @current-change="handleCurrentChange"
              />
            </div>
          </div>
        </el-main>

        <!-- 右侧上传面板 -->
        <el-aside width="300px">
          <UploadPanel />
        </el-aside>
      </el-container>
    </el-container>

    <!-- 图片预览对话框 -->
    <el-dialog
      v-model="previewVisible"
      :title="previewImage?.filename"
      width="90%"
      center
      class="preview-dialog"
    >
      <div v-if="previewImage" class="preview-content">
        <div class="preview-image-container">
          <img
            :src="imageAPI.getImageUrl(previewImage.hash)"
            :alt="previewImage.filename"
            class="preview-image"
          />
        </div>
        <div class="preview-info">
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">文件名:</span>
              <span class="info-value">{{ previewImage.filename }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">尺寸:</span>
              <span class="info-value">{{ previewImage.width }}×{{ previewImage.height }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">大小:</span>
              <span class="info-value">{{ formatSize(previewImage.size) }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">格式:</span>
              <span class="info-value">{{ previewImage.mimetype }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">评级:</span>
              <span class="info-value rating" :class="`rating-${previewImage.rating}`">
                {{ getRatingText(previewImage.rating) }}
              </span>
            </div>
            <div class="info-item" v-if="previewImage.uploadTime">
              <span class="info-label">上传时间:</span>
              <span class="info-value">{{ formatDate(previewImage.uploadTime) }}</span>
            </div>
          </div>

          <div v-if="previewImage.tags.length" class="preview-tags">
            <div class="tags-label">标签:</div>
            <div class="tags-list">
              <span class="tag" v-for="tag in previewImage.tags" :key="tag">
                {{ tag }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Loading, Refresh, Download, Delete } from '@element-plus/icons-vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { useImageStore } from './store'
import { imageAPI } from './api'
import FilterPanel from './FilterPanel.vue'
import UploadPanel from './UploadPanel.vue'
import ImageCard from './ImageCard.vue'
import type { Image } from './types'

const imageStore = useImageStore()

const previewVisible = ref(false)
const previewImage = ref<Image | null>(null)
const currentPage = ref(1)
const downloading = ref(false)
const deleting = ref(false)

// 全选相关计算属性
const isAllSelected = computed({
  get: () => {
    return imageStore.hasImages &&
           imageStore.images.every(img => imageStore.isImageSelected(img.hash))
  },
  set: (value: boolean) => {
    if (value) {
      imageStore.selectAllImages()
    } else {
      imageStore.clearSelection()
    }
  }
})

const isIndeterminate = computed(() => {
  const selectedCount = imageStore.selectedImages.size
  const totalCount = imageStore.images.length
  return selectedCount > 0 && selectedCount < totalCount
})

const showPreview = (image: Image) => {
  previewImage.value = image
  previewVisible.value = true
}

const deleteImage = async (hash: string) => {
  try {
    await ElMessageBox.confirm('确定要删除这张图片吗？', '确认删除', {
      type: 'warning'
    })

    await imageStore.deleteImage(hash)
    ElMessage.success('删除成功')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const handleSelectAll = (value: boolean) => {
  if (value) {
    imageStore.selectAllImages()
  } else {
    imageStore.clearSelection()
  }
}

const downloadSelected = async () => {
  if (!imageStore.hasSelection) return

  downloading.value = true
  try {
    await imageStore.downloadSelectedImages()
    ElMessage.success('下载完成')
  } catch (error) {
    ElMessage.error('下载失败')
  } finally {
    downloading.value = false
  }
}

const deleteSelected = async () => {
  if (!imageStore.hasSelection) return

  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${imageStore.selectedImages.size} 张图片吗？`,
      '批量删除确认',
      { type: 'warning' }
    )

    deleting.value = true
    await imageStore.deleteSelectedImages()
    ElMessage.success('删除成功')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  } finally {
    deleting.value = false
  }
}

const handleSizeChange = (size: number) => {
  imageStore.updateQuery({ pageSize: size })
}

const handleCurrentChange = (page: number) => {
  imageStore.goToPage(page)
}

const formatSize = (bytes: number) => {
  const sizes = ['B', 'KB', 'MB', 'GB']
  if (bytes === 0) return '0 B'
  const i = Math.floor(Math.log(bytes) / Math.log(1024))
  return Math.round(bytes / Math.pow(1024, i) * 100) / 100 + ' ' + sizes[i]
}

const getRatingText = (rating: string) => {
  const ratingMap: Record<string, string> = {
    'general': '一般',
    'sensitive': '敏感',
    'explicit': '明确',
    'all': '全部'
  }
  return ratingMap[rating] || rating
}

const formatDate = (dateString: string) => {
  try {
    return new Date(dateString).toLocaleString('zh-CN')
  } catch {
    return dateString
  }
}

onMounted(() => {
  imageStore.loadImages()
})
</script>

<style scoped>
.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
  padding: 0 20px;
}

.logo h2 {
  color: #e4e4e7;
  margin: 0;
}

.header-stats {
  display: flex;
  gap: 16px;
  color: #a1a1aa;
  font-size: 14px;
}

.selection-info {
  color: #3b82f6;
  font-weight: 500;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid #27272a;
  margin-bottom: 20px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.selection-count {
  color: #3b82f6;
  font-size: 14px;
  font-weight: 500;
}

.empty-state {
  text-align: center;
  padding: 80px 20px;
  color: #71717a;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.empty-title {
  font-size: 18px;
  font-weight: 500;
  margin-bottom: 8px;
  color: #a1a1aa;
}

.empty-subtitle {
  font-size: 14px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: 40px 0;
}

.preview-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.preview-image-container {
  display: flex;
  justify-content: center;
  background: #000;
  border-radius: 8px;
  overflow: hidden;
}

.preview-image {
  max-width: 100%;
  max-height: 70vh;
  object-fit: contain;
}

.preview-info {
  background: #18181b;
  padding: 20px;
  border-radius: 8px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
}

.info-label {
  font-weight: 500;
  color: #a1a1aa;
}

.info-value {
  color: #e4e4e7;
  font-weight: 500;
}

.info-value.rating {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.rating-general {
  background: #22c55e;
  color: white;
}

.rating-sensitive {
  background: #f59e0b;
  color: white;
}

.rating-explicit {
  background: #ef4444;
  color: white;
}

.preview-tags {
  border-top: 1px solid #27272a;
  padding-top: 16px;
}

.tags-label {
  font-weight: 500;
  color: #a1a1aa;
  margin-bottom: 8px;
}

.tags-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag {
  background: #3f3f46;
  color: #d4d4d8;
  padding: 4px 12px;
  border-radius: 16px;
  font-size: 12px;
}

:deep(.preview-dialog) {
  background: #0a0a0a;
}

:deep(.preview-dialog .el-dialog__body) {
  padding: 20px;
}
</style>
