<script setup lang="ts">
import { ref } from 'vue'
import { useQuery, useMutation, useQueryClient } from '@tanstack/vue-query'
import { uploadApi, type UploadTask } from '../../api/upload'
import { taggerApi } from '../../api/tagger'
import { NCard, NButton, NSelect, NUpload, NUploadDragger, NIcon, NText, NP, NDataTable, NTag, useMessage, NCheckbox } from 'naive-ui'
import { Archive24Regular as ArchiveIcon } from '@vicons/fluent'

const message = useMessage()
const queryClient = useQueryClient()

// Tagger 服务器
const { data: taggerServers } = useQuery({
  queryKey: ['taggerServers'],
  queryFn: taggerApi.listServers
})

const selectedTagger = ref<number | null>(null)
const recursiveScan = ref(true)

const taggerOptions = computed(() =>
  taggerServers.value?.map(s => ({ label: s.name, value: s.id })) || []
)

// 上传逻辑
const fileInput = ref<HTMLInputElement | null>(null)

function triggerFolderUpload() {
  fileInput.value?.click()
}

const uploadMutation = useMutation({
  mutationFn: (file: File) => uploadApi.uploadFile(file, selectedTagger.value || undefined),
  onSuccess: () => {
    queryClient.invalidateQueries({ queryKey: ['uploadTasks'] })
  },
  onError: (error) => {
    message.error('上传失败: ' + error.message)
  }
})

function uploadSingle(file: File) {
  uploadMutation.mutate(file)
}

async function handleFiles(files: FileList | null) {
  if (!files) return

  let fileArray = Array.from(files).filter(f => f.type.startsWith('image/'))

  if (!recursiveScan.value) {
    fileArray = fileArray.filter(f => {
      if (!f.webkitRelativePath) return true;
      const parts = f.webkitRelativePath.split('/');
      return parts.length <= 2;
    })
  }

  if (fileArray.length === 0) {
    message.warning('未找到图片文件')
    return
  }

  message.info(`发现 ${fileArray.length} 张图片。`)

  // 顺序上传或并行上传（限制并发）
  // 为简单起见，我们直接全部触发。浏览器会限制并发请求。
  for (const file of fileArray) {
    uploadMutation.mutate(file)
  }
}

function onFileChange(e: Event) {
  const target = e.target as HTMLInputElement
  handleFiles(target.files)
  target.value = ''
}

// 任务队列
const { data: tasks } = useQuery({
  queryKey: ['uploadTasks'],
  queryFn: uploadApi.listTasks,
  refetchInterval: 2000 // 每 2 秒轮询一次
})

const columns = [
  { title: '文件名', key: 'filename' },
  { title: '大小', key: 'size', render: (row: UploadTask) => (row.size / 1024 / 1024).toFixed(2) + ' MB' },
  {
    title: '状态',
    key: 'status',
    render: (row: UploadTask) => {
      let type: 'default' | 'success' | 'info' | 'warning' | 'error' = 'default'
      if (row.status === 'COMPLETED') type = 'success'
      if (row.status === 'FAILED') type = 'error'
      if (row.status === 'PROCESSING' || row.status === 'TAGGING') type = 'info'

      return h(NTag, { type }, { default: () => row.status })
    }
  },
  { title: '消息', key: 'errorMessage' }
]

import { computed, h } from 'vue'

</script>

<template>
  <div class="p-4 h-full flex flex-col gap-4">
    <n-card title="上传图片">
      <div class="flex flex-col gap-4">
        <div class="flex items-center gap-4">
          <div class="w-64">
            <n-select v-model:value="selectedTagger" :options="taggerOptions" placeholder="选择 Tagger (可选)" clearable />
          </div>
          <n-checkbox v-model:checked="recursiveScan">递归扫描</n-checkbox>
          <n-button @click="triggerFolderUpload">
            上传文件夹
          </n-button>
          <input
            type="file"
            ref="fileInput"
            webkitdirectory
            directory
            multiple
            class="hidden"
            @change="onFileChange"
          />
        </div>

        <n-upload
          multiple
          :show-file-list="false"
          :custom-request="({ file }) => uploadSingle(file.file as File)"
        >
          <n-upload-dragger>
            <div style="margin-bottom: 12px">
              <n-icon size="48" :depth="3">
                <archive-icon />
              </n-icon>
            </div>
            <n-text style="font-size: 16px">
              点击或拖拽文件/文件夹到此处上传
            </n-text>
            <n-p depth="3" style="margin: 8px 0 0 0">
              严禁上传敏感数据或其他违禁文件
            </n-p>
          </n-upload-dragger>
        </n-upload>
      </div>
    </n-card>

    <n-card title="上传任务" class="flex-1 overflow-hidden flex flex-col">
      <n-data-table
        :columns="columns"
        :data="tasks || []"
        :pagination="{ pageSize: 10 }"
        class="h-full"
        flex-height
      />
    </n-card>
  </div>
</template>

