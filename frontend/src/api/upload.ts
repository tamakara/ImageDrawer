import axios from 'axios'

const apiClient = axios.create({
  baseURL: '/api',
})

export interface UploadTask {
  id: string
  filename: string
  size: number
  status: 'PENDING' | 'UPLOADING' | 'PROCESSING' | 'TAGGING' | 'SAVING' | 'COMPLETED' | 'FAILED'
  errorMessage?: string
  createdAt: string
  updatedAt: string
}

export const uploadApi = {
  uploadFile: async (file: File, taggerServerId?: number) => {
    const formData = new FormData()
    formData.append('file', file)
    if (taggerServerId) {
      formData.append('taggerServerId', taggerServerId.toString())
    }

    const response = await apiClient.post<UploadTask>('/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    return response.data
  },

  listTasks: async () => {
    const response = await apiClient.get<UploadTask[]>('/upload/tasks')
    return response.data
  }
}

