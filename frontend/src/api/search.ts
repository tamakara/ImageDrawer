import axios from 'axios'
import type { Page, ImageDto } from './gallery'

const apiClient = axios.create({
  baseURL: '/api',
})

export interface SearchRequestDto {
  includedTags: string[]
  excludedTags: string[]
  anyTags: string[]
}

export const searchApi = {
  search: async (request: SearchRequestDto, page: number, size: number) => {
    const response = await apiClient.post<Page<ImageDto>>('/search', request, {
      params: { page, size }
    })
    return response.data
  }
}

