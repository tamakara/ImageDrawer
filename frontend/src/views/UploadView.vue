<script setup lang="ts">
import {h, ref} from 'vue'
import {useMutation, useQuery, useQueryClient} from '@tanstack/vue-query'
import {uploadApi, type UploadTask} from '../api/upload'
import {
  NButton,
  NCard,
  NCheckbox,
  NDataTable,
  NIcon,
  NP,
  NTag,
  NText,
  NUpload,
  NUploadDragger,
  type UploadCustomRequestOptions,
  type UploadFileInfo,
  useMessage
} from 'naive-ui'
import {
  Archive24Regular as ArchiveIcon,
  Delete24Regular as DeleteIcon,
  Dismiss24Regular as DismissIcon
} from '@vicons/fluent'
import PQueue from 'p-queue'


const message = useMessage()
const queryClient = useQueryClient()

const enableTagging = ref(true)
const recursiveScan = ref(true)
const uploadFileList = ref<UploadFileInfo[]>([])

// 上传逻辑
const fileInput = ref<HTMLInputElement | null>(null)

const queue = new PQueue({ concurrency: 3 })

function triggerFolderUpload() {
  fileInput.value?.click()
}

const uploadMutation = useMutation({
  mutationFn: (file: File) => uploadApi.uploadFile(file, enableTagging.value),
  onSuccess: () => {
    queryClient.invalidateQueries({queryKey: ['uploadTasks']})
  },
  onError: (error) => {
    message.error('上传失败: ' + error.message)
  }
})

const customRequest = ({file}: UploadCustomRequestOptions) => {
  if (file.file && file.file.type.startsWith('image/')) {
    uploadMutation.mutate(file.file)
  }
  // 上传后立即从列表中移除，防止重复上传
  const index = uploadFileList.value.findIndex(f => f.id === file.id)
  if (index > -1) {
    uploadFileList.value.splice(index, 1)
  }
}

async function handleFiles(files: FileList | null) {
  if (!files) return

  let fileArray = Array.from(files)
      .filter(f => f.type.startsWith('image/') && !f.name.startsWith('.'))

  if (!recursiveScan.value) {
    fileArray = fileArray.filter(f => {
      const parts = f.webkitRelativePath?.split('/') ?? []
      return parts.length <= 2
    })
  }

  message.info(`发现 ${fileArray.length} 张图片，开始排队上传`)

  for (const file of fileArray) {
    queue.add(() => uploadMutation.mutateAsync(file))
  }
}

function onFileChange(e: Event) {
  const target = e.target as HTMLInputElement
  handleFiles(target.files)
  target.value = ''
}

// 任务队列
const {data: tasks} = useQuery({
  queryKey: ['uploadTasks'],
  queryFn: uploadApi.listTasks,
  refetchInterval: 500 // 每0.5秒轮询一次
})

const deleteTaskMutation = useMutation({
  mutationFn: (id: string) => uploadApi.deleteTask(id),
  onSuccess: () => {
    queryClient.invalidateQueries({queryKey: ['uploadTasks']})
  },
  onError: (error: any) => {
    message.error('删除任务失败: ' + error.message)
  }
})

const clearTasksMutation = useMutation({
  mutationFn: () => uploadApi.clearTasks(),
  onSuccess: () => {
    queryClient.invalidateQueries({queryKey: ['uploadTasks']})
    message.success('任务列表已清空')
  },
  onError: (error: any) => {
    message.error('清空任务失败: ' + error.message)
  }
})

const columns = [
  {title: '文件名', key: 'filename'},
  {title: '大小', key: 'size', render: (row: UploadTask) => (row.size / 1024 / 1024).toFixed(2) + ' MB'},
  {
    title: '状态',
    key: 'status',
    render: (row: UploadTask) => {
      let type: 'default' | 'success' | 'info' | 'warning' | 'error' = 'default'
      if (row.status === 'COMPLETED') type = 'success'
      if (row.status === 'FAILED') type = 'error'
      if (row.status === 'PROCESSING' || row.status === 'TAGGING') type = 'info'

      return h(NTag, {type}, {default: () => row.status})
    }
  },
  {title: '消息', key: 'errorMessage'},
  {
    title: '操作',
    key: 'actions',
    render: (row: UploadTask) => {
      return h(NButton, {
        size: 'small',
        quaternary: true,
        circle: true,
        onClick: () => deleteTaskMutation.mutate(row.id)
      }, {
        icon: () => h(NIcon, null, {default: () => h(DismissIcon)})
      })
    }
  }
]

</script>

<template>
  <div class="h-full flex flex-col gap-4">
    <n-card title="上传图片">
      <div class="flex flex-col gap-4">
        <div class="flex flex-wrap items-center gap-4">
          <n-checkbox v-model:checked="enableTagging">自动打标</n-checkbox>
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
            v-model:file-list="uploadFileList"
            multiple
            accept="image/*"
            :show-file-list="false"
            :custom-request="customRequest"
        >
          <n-upload-dragger>
            <div style="margin-bottom: 12px">
              <n-icon size="48" :depth="3">
                <archive-icon/>
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
      <template #header-extra>
        <n-button quaternary circle @click="clearTasksMutation.mutate()">
          <template #icon>
            <n-icon>
              <delete-icon/>
            </n-icon>
          </template>
        </n-button>
      </template>
      <n-data-table
          :columns="columns"
          :data="tasks || []"
          class="h-full"
          flex-height
      />
    </n-card>
  </div>
</template>

