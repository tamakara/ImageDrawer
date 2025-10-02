<template>
  <div class="upload-section">
    <!-- Tagger配置 -->
    <div class="filter-section">
      <div class="filter-title">Tagger配置</div>
      <el-input
        v-model="taggerUrl"
        placeholder="输入Tagger服务URL"
        @blur="updateTaggerUrl"
        @keyup.enter="updateTaggerUrl"
      >
        <template #append>
          <el-button @click="updateTaggerUrl" type="primary">保存</el-button>
        </template>
      </el-input>
      <div class="tagger-hint">配置后上传图片时会自动标记标签</div>
    </div>

    <!-- 上传区域 -->
    <div
      class="upload-area"
      :class="{ dragover: isDragOver }"
      @click="triggerFileInput"
      @drop="handleDrop"
      @dragover.prevent="isDragOver = true"
      @dragleave="isDragOver = false"
    >
      <div class="upload-icon">📁</div>
      <div class="upload-text">点击或拖拽文件到此处上传</div>
      <div class="upload-hint">支持 JPG、PNG、GIF 格式，可批量上传</div>
    </div>

    <input
      ref="fileInput"
      type="file"
      multiple
      accept="image/*"
      style="display: none"
      @change="handleFileSelect"
    />

    <!-- 上传队列 -->
    <div v-if="uploadQueue.length > 0" class="upload-queue">
      <div class="queue-header">
        <span class="queue-title">上传队列 ({{ uploadQueue.length }})</span>
        <el-button
          size="small"
          type="danger"
          @click="clearQueue"
          :disabled="uploadQueue.some(item => item.status === 'uploading')"
        >
          清空
        </el-button>
      </div>

      <div class="queue-list">
        <div v-for="item in uploadQueue" :key="item.id" class="queue-item">
          <div class="file-info">
            <div class="file-name">{{ item.file.name }}</div>
            <div class="file-size">{{ formatSize(item.file.size) }}</div>
          </div>

          <el-progress
            :percentage="item.progress"
            :status="item.status"
            :stroke-width="6"
            style="flex: 1; margin: 0 12px"
          />

          <div class="item-actions">
            <el-button
              v-if="item.status === 'exception'"
              size="small"
              type="primary"
              @click="retryUpload(item)"
            >
              重试
            </el-button>
            <el-button
              v-if="item.status !== 'uploading'"
              size="small"
              type="danger"
              @click="removeFromQueue(item.id)"
            >
              移除
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 批量操作 -->
    <div v-if="imageStore.hasSelection" class="batch-actions">
      <div class="filter-title">批量操作 ({{ imageStore.selectedImages.size }}张)</div>
      <div class="action-buttons">
        <el-button
          type="primary"
          @click="downloadSelected"
          :loading="downloading"
          style="width: 100%; margin-bottom: 8px"
        >
          下载选中图片
        </el-button>
        <el-button
          type="danger"
          @click="deleteSelected"
          :loading="deleting"
          style="width: 100%; margin-bottom: 8px"
        >
          删除选中图片
        </el-button>
        <el-button
          @click="imageStore.selectAllImages"
          style="width: 48%; margin-right: 4%"
        >
          全选
        </el-button>
        <el-button
          @click="imageStore.clearSelection"
          style="width: 48%"
        >
          取消选择
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useImageStore } from './store'
import { tagAPI } from './api'
import { ElMessage, ElMessageBox } from 'element-plus'

interface UploadItem {
  id: string
  file: File
  progress: number
  status: 'uploading' | 'success' | 'exception'
}

const imageStore = useImageStore()
const fileInput = ref<HTMLInputElement>()
const isDragOver = ref(false)
const uploadQueue = ref<UploadItem[]>([])
const taggerUrl = ref('')
const downloading = ref(false)
const deleting = ref(false)

const triggerFileInput = () => {
  fileInput.value?.click()
}

const handleFileSelect = (event: Event) => {
  const target = event.target as HTMLInputElement
  if (target.files) {
    handleFiles(Array.from(target.files))
  }
}

const handleDrop = (event: DragEvent) => {
  event.preventDefault()
  isDragOver.value = false

  if (event.dataTransfer?.files) {
    handleFiles(Array.from(event.dataTransfer.files))
  }
}

const handleFiles = (files: File[]) => {
  const imageFiles = files.filter(file => file.type.startsWith('image/'))

  if (imageFiles.length !== files.length) {
    ElMessage.warning('只能上传图片文件')
  }

  if (imageFiles.length === 0) return

  imageFiles.forEach(file => {
    const uploadItem: UploadItem = {
      id: Date.now() + Math.random().toString(),
      file,
      progress: 0,
      status: 'uploading'
    }
    uploadQueue.value.push(uploadItem)
    uploadFile(uploadItem)
  })
}

const uploadFile = async (item: UploadItem) => {
  try {
    // 模拟上传进度
    const progressInterval = setInterval(() => {
      if (item.progress < 90) {
        item.progress += Math.random() * 20
      }
    }, 200)

    await imageStore.uploadImage(item.file)

    clearInterval(progressInterval)
    item.progress = 100
    item.status = 'success'

    ElMessage.success(`${item.file.name} 上传成功`)

    // 3秒后从队列中移除成功的项目
    setTimeout(() => {
      removeFromQueue(item.id)
    }, 3000)
  } catch (error) {
    item.status = 'exception'
    ElMessage.error(`${item.file.name} 上传失败`)
  }
}

const retryUpload = (item: UploadItem) => {
  item.progress = 0
  item.status = 'uploading'
  uploadFile(item)
}

const removeFromQueue = (id: string) => {
  const index = uploadQueue.value.findIndex(item => item.id === id)
  if (index > -1) {
    uploadQueue.value.splice(index, 1)
  }
}

const clearQueue = () => {
  uploadQueue.value = uploadQueue.value.filter(item => item.status === 'uploading')
}

const updateTaggerUrl = async () => {
  if (!taggerUrl.value.trim()) return

  try {
    await tagAPI.setTaggerUrl(taggerUrl.value.trim())
    ElMessage.success('Tagger URL配置成功')
  } catch (error) {
    ElMessage.error('配置失败')
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

const formatSize = (bytes: number) => {
  const sizes = ['B', 'KB', 'MB', 'GB']
  if (bytes === 0) return '0 B'
  const i = Math.floor(Math.log(bytes) / Math.log(1024))
  return Math.round(bytes / Math.pow(1024, i) * 100) / 100 + ' ' + sizes[i]
}
</script>

<style scoped>
.upload-section {
  padding: 20px;
  height: 100%;
  overflow-y: auto;
}

.filter-section {
  margin-bottom: 24px;
}

.filter-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 12px;
  color: #e4e4e7;
}

.tagger-hint {
  font-size: 12px;
  color: #a1a1aa;
  margin-top: 8px;
}

.upload-area {
  border: 2px dashed #3f3f46;
  border-radius: 8px;
  padding: 40px 20px;
  text-align: center;
  transition: border-color 0.3s;
  cursor: pointer;
  margin-bottom: 20px;
}

.upload-area:hover,
.upload-area.dragover {
  border-color: #3b82f6;
  background: rgba(59, 130, 246, 0.05);
}

.upload-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.upload-text {
  color: #e4e4e7;
  margin-bottom: 8px;
  font-weight: 500;
}

.upload-hint {
  color: #a1a1aa;
  font-size: 12px;
}

.upload-queue {
  background: #18181b;
  border-radius: 8px;
  border: 1px solid #27272a;
  overflow: hidden;
  margin-bottom: 20px;
}

.queue-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #27272a;
  border-bottom: 1px solid #3f3f46;
}

.queue-title {
  font-weight: 600;
  color: #e4e4e7;
}

.queue-list {
  max-height: 300px;
  overflow-y: auto;
}

.queue-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #27272a;
}

.queue-item:last-child {
  border-bottom: none;
}

.file-info {
  min-width: 0;
  margin-right: 12px;
}

.file-name {
  font-size: 13px;
  font-weight: 500;
  color: #e4e4e7;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-size {
  font-size: 12px;
  color: #a1a1aa;
}

.item-actions {
  display: flex;
  gap: 8px;
}

.batch-actions {
  background: #18181b;
  border-radius: 8px;
  border: 1px solid #27272a;
  padding: 16px;
}

.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
</style>
