<script setup lang="ts">
import { NUpload, NUploadDragger, NIcon, NText, NP, NList, NListItem, NThing, NProgress, NButton, NTag } from 'naive-ui'
import { Archive24Regular as ArchiveIcon, Delete24Regular as DeleteIcon } from '@vicons/fluent'
import { useQueueStore } from '../../stores/queue'
import { storeToRefs } from 'pinia'
import type { UploadCustomRequestOptions } from 'naive-ui'

const queueStore = useQueueStore()
const { tasks } = storeToRefs(queueStore)

const customRequest = ({ file, onFinish }: UploadCustomRequestOptions) => {
  if (file.file) {
    queueStore.addTask(file.file)
    // Simulate upload for now
    setTimeout(() => {
      onFinish()
    }, 1000)
  }
}

function formatSize(bytes: number) {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

function getStatusColor(status: string) {
  switch (status) {
    case 'completed': return 'success'
    case 'error': return 'error'
    case 'uploading':
    case 'tagging': return 'info'
    default: return 'default'
  }
}
</script>

<template>
  <div class="h-full flex flex-col gap-6">
    <div class="flex justify-between items-center">
      <h1 class="text-2xl font-bold">上传图片</h1>
      <n-button v-if="tasks.length > 0" @click="queueStore.clearCompleted">清除已完成</n-button>
    </div>

    <!-- Upload Area -->
    <div class="bg-white dark:bg-gray-800 rounded-lg p-6 shadow-sm">
      <n-upload
        multiple
        directory-dnd
        :custom-request="customRequest"
        :show-file-list="false"
        abstract
      >
        <n-upload-dragger>
          <div style="margin-bottom: 12px">
            <n-icon size="48" :depth="3">
              <ArchiveIcon />
            </n-icon>
          </div>
          <n-text style="font-size: 16px">
            点击或者拖动文件到该区域来上传
          </n-text>
          <n-p depth="3" style="margin: 8px 0 0 0">
            支持多文件、文件夹上传
          </n-p>
        </n-upload-dragger>
      </n-upload>
    </div>

    <!-- Queue List -->
    <div v-if="tasks.length > 0" class="flex-1 bg-white dark:bg-gray-800 rounded-lg shadow-sm overflow-hidden flex flex-col">
      <div class="p-4 border-b border-gray-100 dark:border-gray-700 font-medium">
        上传队列 ({{ tasks.length }})
      </div>
      <div class="flex-1 overflow-auto p-4">
        <n-list hoverable>
          <n-list-item v-for="task in tasks" :key="task.id">
            <template #prefix>
              <div class="w-16 h-16 rounded overflow-hidden bg-gray-100">
                <img
                  v-if="task.thumbnailUrl"
                  :src="task.thumbnailUrl"
                  class="w-full h-full object-cover"
                />
              </div>
            </template>
            <template #suffix>
              <n-button quaternary circle type="error" @click="queueStore.removeTask(task.id)">
                <template #icon><n-icon><DeleteIcon /></n-icon></template>
              </n-button>
            </template>
            <n-thing :title="task.file.name">
              <template #description>
                <div class="flex items-center gap-2 text-xs text-gray-500">
                  <span>{{ formatSize(task.file.size) }}</span>
                  <n-tag size="small" :type="getStatusColor(task.status)" :bordered="false">
                    {{ task.status }}
                  </n-tag>
                </div>
              </template>
              <div class="mt-2" v-if="task.status === 'uploading' || task.status === 'tagging'">
                <n-progress
                  type="line"
                  :percentage="task.progress"
                  :processing="true"
                  :height="4"
                  :show-indicator="false"
                />
              </div>
            </n-thing>
          </n-list-item>
        </n-list>
      </div>
    </div>
  </div>
</template>

