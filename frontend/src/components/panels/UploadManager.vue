<template>
  <div class="upload-manager">
    <!-- 上传区域 -->
    <div class="upload-section">
      <div class="section-header">
        <h4 class="section-title">文件上传</h4>
        <div class="section-description">拖拽文件或点击选择</div>
      </div>

      <div class="upload-area border-secondary bg-surface"
           :class="{ 'dragover': isDragover }"
           @drop="handleDrop"
           @dragover="handleDragOver"
           @dragleave="handleDragLeave"
           @click="triggerFileInput">
        <div class="upload-content">
          <el-icon class="upload-icon" size="32">
            <UploadFilled />
          </el-icon>
          <div class="upload-text">
            <div class="upload-title">拖拽文件到此处</div>
            <div class="upload-subtitle">或点击选择文件</div>
          </div>
        </div>
        <input
          ref="fileInputRef"
          type="file"
          multiple
          accept="image/*"
          @change="handleFileSelect"
          style="display: none;"
        />
      </div>
    </div>

    <!-- 上传队列 -->
    <div class="queue-section" v-if="uploadQueue.length > 0">
      <div class="section-header">
        <h4 class="section-title">上传队列</h4>
        <div class="section-description">{{ uploadQueue.length }} 个文件</div>
      </div>

      <div class="queue-list">
        <div
          v-for="item in uploadQueue"
          :key="item.id"
          class="queue-item border-primary bg-surface"
        >
          <div class="item-preview">
            <img
              v-if="item.preview"
              :src="item.preview"
              alt="预览"
              class="preview-image"
            />
            <el-icon v-else class="preview-placeholder">
              <Picture />
            </el-icon>
          </div>

          <div class="item-info">
            <div class="item-name">{{ item.name }}</div>
            <div class="item-size">{{ formatFileSize(item.size) }}</div>
            <div class="item-progress">
              <el-progress
                :percentage="item.progress"
                :status="getProgressStatus(item.status)"
                :stroke-width="4"
                text-inside
              />
            </div>
          </div>

          <div class="item-actions">
            <el-button
              v-if="item.status === 'pending'"
              type="danger"
              size="small"
              circle
              @click="removeFromQueue(item.id)"
            >
              <el-icon><Close /></el-icon>
            </el-button>

            <el-button
              v-if="item.status === 'error'"
              type="primary"
              size="small"
              circle
              @click="retryUpload(item.id)"
            >
              <el-icon><Refresh /></el-icon>
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 控制按钮 -->
    <div class="upload-controls" v-if="uploadQueue.length > 0">
      <el-button
        type="primary"
        :loading="isUploading"
        :disabled="!hasPendingFiles"
        @click="startUpload"
        class="control-button"
      >
        {{ isUploading ? '上传中...' : '开始上传' }}
      </el-button>

      <el-button
        v-if="hasCompletedFiles"
        @click="clearCompleted"
        class="control-button"
      >
        清除已完成
      </el-button>

      <el-button
        @click="clearAll"
        class="control-button"
      >
        清空队列
      </el-button>
    </div>

    <!-- 统计信息 -->
    <div class="upload-stats" v-if="uploadStats.total > 0">
      <div class="stats-item">
        <span class="stats-label">总数:</span>
        <span class="stats-value">{{ uploadStats.total }}</span>
      </div>
      <div class="stats-item">
        <span class="stats-label">成功:</span>
        <span class="stats-value success">{{ uploadStats.success }}</span>
      </div>
      <div class="stats-item">
        <span class="stats-label">失败:</span>
        <span class="stats-value error">{{ uploadStats.failed }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled, Picture, Close, Refresh } from '@element-plus/icons-vue'
import { formatFileSize } from '@/utils'

interface UploadItem {
  id: string
  file: File
  name: string
  size: number
  preview?: string
  progress: number
  status: 'pending' | 'uploading' | 'success' | 'error'
  error?: string
}

const fileInputRef = ref<HTMLInputElement>()
const isDragover = ref(false)
const isUploading = ref(false)
const uploadQueue = ref<UploadItem[]>([])

const hasPendingFiles = computed(() =>
  uploadQueue.value.some(item => item.status === 'pending')
)

const hasCompletedFiles = computed(() =>
  uploadQueue.value.some(item => item.status === 'success' || item.status === 'error')
)

const uploadStats = computed(() => {
  const total = uploadQueue.value.length
  const success = uploadQueue.value.filter(item => item.status === 'success').length
  const failed = uploadQueue.value.filter(item => item.status === 'error').length
  return { total, success, failed }
})

// 文件处理
const handleFileSelect = (event: Event) => {
  const target = event.target as HTMLInputElement
  if (target.files) {
    addFilesToQueue(Array.from(target.files))
  }
}

const handleDrop = (event: DragEvent) => {
  event.preventDefault()
  isDragover.value = false

  if (event.dataTransfer?.files) {
    addFilesToQueue(Array.from(event.dataTransfer.files))
  }
}

const handleDragOver = (event: DragEvent) => {
  event.preventDefault()
  isDragover.value = true
}

const handleDragLeave = (event: DragEvent) => {
  event.preventDefault()
  isDragover.value = false
}

const triggerFileInput = () => {
  fileInputRef.value?.click()
}

// 队列管理
const addFilesToQueue = (files: File[]) => {
  const imageFiles = files.filter(file => file.type.startsWith('image/'))

  if (imageFiles.length !== files.length) {
    ElMessage.warning('只能上传图片文件')
  }

  imageFiles.forEach(file => {
    const item: UploadItem = {
      id: Date.now() + Math.random().toString(36),
      file,
      name: file.name,
      size: file.size,
      progress: 0,
      status: 'pending'
    }

    // 生成预览
    if (file.type.startsWith('image/')) {
      const reader = new FileReader()
      reader.onload = (e) => {
        item.preview = e.target?.result as string
      }
      reader.readAsDataURL(file)
    }

    uploadQueue.value.push(item)
  })

  ElMessage.success(`已添加 ${imageFiles.length} 个文件到队列`)
}

const removeFromQueue = (id: string) => {
  uploadQueue.value = uploadQueue.value.filter(item => item.id !== id)
}

const clearCompleted = () => {
  uploadQueue.value = uploadQueue.value.filter(
    item => item.status === 'pending' || item.status === 'uploading'
  )
}

const clearAll = () => {
  uploadQueue.value = []
  ElMessage.info('队列已清空')
}

// 上传处理
const startUpload = async () => {
  isUploading.value = true

  const pendingItems = uploadQueue.value.filter(item => item.status === 'pending')

  for (const item of pendingItems) {
    try {
      item.status = 'uploading'
      await uploadFile(item)
      item.status = 'success'
      item.progress = 100
    } catch (error) {
      item.status = 'error'
      item.error = error instanceof Error ? error.message : '上传失败'
    }
  }

  isUploading.value = false
  ElMessage.success('上传完成')
}

const uploadFile = async (item: UploadItem): Promise<void> => {
  return new Promise((resolve, reject) => {
    const formData = new FormData()
    formData.append('file', item.file)

    const xhr = new XMLHttpRequest()

    xhr.upload.onprogress = (event) => {
      if (event.lengthComputable) {
        item.progress = Math.round((event.loaded / event.total) * 100)
      }
    }

    xhr.onload = () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        resolve()
      } else {
        reject(new Error(`上传失败: ${xhr.statusText}`))
      }
    }

    xhr.onerror = () => {
      reject(new Error('网络错误'))
    }

    xhr.open('POST', '/api/upload')
    xhr.send(formData)
  })
}

const retryUpload = async (id: string) => {
  const item = uploadQueue.value.find(item => item.id === id)
  if (item) {
    item.status = 'pending'
    item.progress = 0
    item.error = undefined
  }
}

const getProgressStatus = (status: UploadItem['status']) => {
  switch (status) {
    case 'success':
      return 'success'
    case 'error':
      return 'exception'
    default:
      return undefined
  }
}
</script>

<style scoped>
.upload-manager {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xl);
}

/* 区块样式 */
.upload-section,
.queue-section {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.section-header {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.section-title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--dark-text-primary);
}

.section-description {
  font-size: 12px;
  color: var(--dark-text-tertiary);
}

/* 上传区域 */
.upload-area {
  padding: var(--spacing-xl);
  border-radius: var(--radius-lg);
  border: 2px dashed var(--dark-border-secondary);
  text-align: center;
  cursor: pointer;
  transition: all var(--transition-base);
  min-height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.upload-area:hover,
.upload-area.dragover {
  border-color: var(--dark-primary);
  background: var(--dark-surface-hover);
}

.upload-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-md);
}

.upload-icon {
  color: var(--dark-primary);
}

.upload-text {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.upload-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--dark-text-primary);
}

.upload-subtitle {
  font-size: 12px;
  color: var(--dark-text-secondary);
}

/* 队列列表 */
.queue-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
  max-height: 300px;
  overflow-y: auto;
}

.queue-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-md);
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
}

.queue-item:hover {
  transform: translateY(-1px);
  box-shadow: var(--dark-shadow-light);
}

/* 预览图 */
.item-preview {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border-radius: var(--radius-sm);
  overflow: hidden;
  background: var(--dark-surface-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
}

.preview-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.preview-placeholder {
  color: var(--dark-text-tertiary);
  font-size: 18px;
}

/* 文件信息 */
.item-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
  min-width: 0;
}

.item-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--dark-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-size {
  font-size: 11px;
  color: var(--dark-text-tertiary);
}

.item-progress {
  margin-top: var(--spacing-xs);
}

/* 操作按钮 */
.item-actions {
  flex-shrink: 0;
}

/* 控制按钮 */
.upload-controls {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
  padding-top: var(--spacing-lg);
  border-top: 1px solid var(--dark-border-secondary);
}

.control-button {
  width: 100%;
}

/* 统计信息 */
.upload-stats {
  display: flex;
  justify-content: space-between;
  padding: var(--spacing-md);
  background: var(--dark-surface-primary);
  border-radius: var(--radius-md);
  border: 1px solid var(--dark-border-secondary);
}

.stats-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-xs);
}

.stats-label {
  font-size: 11px;
  color: var(--dark-text-tertiary);
}

.stats-value {
  font-size: 14px;
  font-weight: 600;
  color: var(--dark-text-primary);
}

.stats-value.success {
  color: var(--dark-success);
}

.stats-value.error {
  color: var(--dark-error);
}

/* 滚动条 */
.queue-list::-webkit-scrollbar {
  width: 4px;
}

.queue-list::-webkit-scrollbar-track {
  background: var(--dark-bg-secondary);
}

.queue-list::-webkit-scrollbar-thumb {
  background: var(--dark-border-primary);
  border-radius: var(--radius-sm);
}

/* Element Plus 组件样式覆盖 */
:deep(.el-progress-bar__outer) {
  background-color: var(--dark-surface-secondary);
}

:deep(.el-progress-bar__inner) {
  background-color: var(--dark-primary);
}

:deep(.el-progress__text) {
  color: var(--dark-text-secondary) !important;
  font-size: 10px !important;
}
</style>
