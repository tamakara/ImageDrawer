import axios from 'axios'
import type { TagDto } from './gallery'

const apiClient = axios.create({
  baseURL: '/api',
})

export const tagsApi = {
  listTags: async (query?: string) => {
    const response = await apiClient.get<TagDto[]>('/tags', {
      params: { query }
    })
    return response.data
  }
}

