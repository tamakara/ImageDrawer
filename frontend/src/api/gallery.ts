import axios from 'axios'

const apiClient = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json'
  }
})

export interface ImageDto {
  id: number
  title: string
  fileName: string
  extension: string
  size: number
  width: number
  height: number
  hash: string
  createdAt: string
  updatedAt: string
  url: string
  thumbnailUrl: string
  tags: TagDto[]
}

export interface TagDto {
  id: number
  name: string
  type: string
}

export interface Page<T> {
  content: T[]
  totalPages: number
  totalElements: number
  size: number
  number: number
  first: boolean
  last: boolean
  empty: boolean
}

export const galleryApi = {
  listImages: async (page: number, size: number) => {
    const response = await apiClient.get<Page<ImageDto>>('/images', {
      params: { page, size }
    })
    return response.data
  },

  getImage: async (id: number) => {
    const response = await apiClient.get<ImageDto>(`/images/${id}`)
    return response.data
  },

  deleteImage: async (id: number) => {
    await apiClient.delete(`/images/${id}`)
  },

  updateImage: async (id: number, dto: Partial<ImageDto>) => {
    const response = await apiClient.put<ImageDto>(`/images/${id}`, dto)
    return response.data
  },

  updateImageFile: async (id: number, file: File, updateName: boolean) => {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('updateName', updateName.toString())
    const response = await apiClient.post<ImageDto>(`/images/${id}/file`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    return response.data
  }
}


