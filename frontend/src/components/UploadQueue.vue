<template>
  <div class="upload-queue">
    <!-- Summary -->
    <div class="summary">
      <div class="progress-text">
        上传进度：{{ completedCount }} / {{ uploadQueue.length }}
      </div>
      <el-button
          type="text"
          class="setting-button"
          @click="taggerDialogVisible = true"
          size="small"
      >
        <el-icon class="setting-icon">
          <Setting/>
        </el-icon>
      </el-button>
    </div>

    <!-- Queue list -->
    <div class="queue-list">
      <div
          v-for="item in uploadQueue"
          :key="item.uid"
          class="queue-item"
          :class="item.status"
      >
        <span class="filename">{{ item.file.name }}</span>
        <span class="icon">
          <el-icon v-if="item.status === 'pending'" color="#faad14">
            <Clock/>
          </el-icon>
          <el-icon v-else-if="item.status === 'uploading'" color="#409eff">
            <Loading/>
          </el-icon>
          <el-icon v-else-if="item.status === 'success'" color="#52c41a">
            <CircleCheckFilled/>
          </el-icon>
          <el-icon v-else-if="item.status === 'error'" color="#f5222d">
            <CircleCloseFilled/>
          </el-icon>
        </span>
      </div>
    </div>

    <!-- Actions -->
    <div class="actions">
      <el-button-group style="width: 100%">
        <el-button
            type="primary"
            size="large"
            style="width: 50%"
            @click="triggerFileSelect"
        >
          上传图片
        </el-button>
        <el-button
            type="primary"
            size="large"
            style="width: 50%"
            @click="triggerFolderSelect"
        >
          选择文件夹
        </el-button>
      </el-button-group>

      <!-- Hidden Inputs -->
      <input
          ref="fileInputRef"
          type="file"
          accept="image/*"
          multiple
          style="display: none"
          @change="onFileInputChange"
      />
      <input
          ref="folderInputRef"
          type="file"
          webkitdirectory
          multiple
          style="display: none"
          @change="onFolderInputChange"
      />
    </div>

    <!-- Tagger Dialog -->
    <el-dialog
        v-model="taggerDialogVisible"
        title="设置 Tagger 服务器"
        width="500px"
    >
      <el-form>
        <el-form-item label="服务器地址">
          <el-input v-model="taggerUrl" placeholder="http://localhost:5000"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="taggerDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveTaggerUrl">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {ref, computed} from 'vue'
import {ElMessage} from 'element-plus'
import {
  Clock,
  Loading,
  CircleCheckFilled,
  CircleCloseFilled,
  Setting
} from '@element-plus/icons-vue'
import {addImage, setTaggerUrl} from '../api'
import type {Image} from '../api'

// ----------------------------
// Types
// ----------------------------

interface UploadItem {
  uid: string
  file: File
  status: 'pending' | 'uploading' | 'success' | 'error'
  response?: Image
  errorMessage?: string
}

// ----------------------------
// State
// ----------------------------

const uploadQueue = ref<UploadItem[]>([])
const isUploading = ref(false)
const currentIndex = ref(0)

const fileInputRef = ref<HTMLInputElement | null>(null)
const folderInputRef = ref<HTMLInputElement | null>(null)

const completedCount = computed(() =>
    uploadQueue.value.filter(i => i.status === 'success').length
)

// ----------------------------
// Upload Logic
// ----------------------------

function triggerFileSelect() {
  fileInputRef.value?.click()
}

function triggerFolderSelect() {
  folderInputRef.value?.click()
}

function onFileInputChange(e: Event) {
  const input = e.target as HTMLInputElement
  if (input.files) {
    addFiles(Array.from(input.files))
    input.value = ''
  }
}

function onFolderInputChange(e: Event) {
  const input = e.target as HTMLInputElement
  if (input.files) {
    const files = Array.from(input.files).map(f => {
      return new File([f], f.name, {type: f.type})
    })
    addFiles(files)
    input.value = ''
  }
}

function addFiles(files: File[]) {
  files.forEach(file => {
    uploadQueue.value.push({
      uid: `${Date.now()}-${file.name}-${Math.random()}`,
      file,
      status: 'pending'
    })
  })

  if (!isUploading.value) {
    currentIndex.value = 0
    startUploadSerial()
  }
}

async function startUploadSerial() {
  if (currentIndex.value >= uploadQueue.value.length) {
    isUploading.value = false
    return
  }

  const item = uploadQueue.value[currentIndex.value]
  if (item.status !== 'pending') {
    currentIndex.value++
    return startUploadSerial()
  }

  isUploading.value = true
  item.status = 'uploading'

  try {
    const result = await addImage(item.file) // 严格串行上传
    item.status = 'success'
    item.response = result
  } catch (e) {
    item.status = 'error'
    item.errorMessage = e instanceof Error ? e.message : String(e)
    console.error(e)
  } finally {
    currentIndex.value++
    await startUploadSerial()
  }
}

// ----------------------------
// Tagger Config
// ----------------------------

const taggerDialogVisible = ref(false)
const taggerUrl = ref('')

async function saveTaggerUrl() {
  try {
    await setTaggerUrl(taggerUrl.value)
    ElMessage.success('Tagger 服务器地址已保存！')
    taggerDialogVisible.value = false
  } catch (e) {
    console.error(e)
    ElMessage.error(e instanceof Error ? e.message : String(e))
  }
}
</script>

<style scoped>
.upload-queue {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.summary {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
}

.progress-text {
  display: flex;
  align-items: center;
  gap: 12px;
  font-weight: bold;
}

.setting-button {
  width: 32px;
  height: 32px;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: transparent;
}

.setting-icon {
  color: #000;
  font-size: 18px;
}

.queue-list {
  flex: 1;
  overflow-y: auto;
  background: #f0f2f5;
  padding: 10px;
  border-radius: 6px;
}

.queue-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border: 1px solid #e4e7ed;
  background: #fff;
  margin-bottom: 6px;
  padding: 6px 10px;
  border-radius: 4px;
  font-size: 13px;
  max-width: 350px;
}

.queue-item.pending {
  border-color: #faad14;
}

.queue-item.uploading {
  border-color: #409eff;
}

.queue-item.success {
  border-color: #52c41a;
}

.queue-item.error {
  border-color: #f5222d;
}

.filename {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.icon {
  flex-shrink: 0;
  margin-left: 10px;
}

.actions {
  margin-top: 10px;
}
</style>
