import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export type UploadStatus = 'pending' | 'uploading' | 'queued' | 'tagging' | 'saving' | 'completed' | 'error'

export interface UploadTask {
  id: string
  file: File
  status: UploadStatus
  progress: number
  thumbnailUrl?: string
  error?: string
  tags?: any[]
}

export const useQueueStore = defineStore('queue', () => {
  const tasks = ref<UploadTask[]>([])

  const pendingTasks = computed(() => tasks.value.filter(t => t.status === 'pending'))
  const activeTasks = computed(() => tasks.value.filter(t => ['uploading', 'queued', 'tagging', 'saving'].includes(t.status)))
  const completedTasks = computed(() => tasks.value.filter(t => t.status === 'completed'))
  const errorTasks = computed(() => tasks.value.filter(t => t.status === 'error'))

  function addTask(file: File) {
    const task: UploadTask = {
      id: crypto.randomUUID(),
      file,
      status: 'pending',
      progress: 0,
      thumbnailUrl: URL.createObjectURL(file)
    }
    tasks.value.push(task)
    return task
  }

  function updateTaskStatus(id: string, status: UploadStatus, progress?: number) {
    const task = tasks.value.find(t => t.id === id)
    if (task) {
      task.status = status
      if (progress !== undefined) {
        task.progress = progress
      }
    }
  }

  function removeTask(id: string) {
    const index = tasks.value.findIndex(t => t.id === id)
    if (index !== -1) {
      const task = tasks.value[index]
      if (task && task.thumbnailUrl) {
        URL.revokeObjectURL(task.thumbnailUrl)
      }
      tasks.value.splice(index, 1)
    }
  }

  function clearCompleted() {
    tasks.value = tasks.value.filter(t => t.status !== 'completed')
  }

  return {
    tasks,
    pendingTasks,
    activeTasks,
    completedTasks,
    errorTasks,
    addTask,
    updateTaskStatus,
    removeTask,
    clearCompleted
  }
})

