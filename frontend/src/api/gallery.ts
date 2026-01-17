import apiClient from './client'

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


  regenerateTags: async (id: number) => {
    const response = await apiClient.post<ImageDto>(`/images/${id}/tags/regenerate`)
    return response.data
  },

  addTag: async (id: number, tag: Partial<TagDto>) => {
    const response = await apiClient.post<ImageDto>(`/images/${id}/tags`, tag)
    return response.data
  },

  removeTag: async (id: number, tagId: number) => {
    const response = await apiClient.delete<ImageDto>(`/images/${id}/tags/${tagId}`)
    return response.data
  },

  deleteImages: async (ids: number[]) => {
    await apiClient.post('/images/batch/delete', ids)
  },

  downloadImages: async (ids: number[]) => {
    const response = await apiClient.post('/images/batch/download', ids, {
      responseType: 'blob'
    })
    return response.data
  }
}


