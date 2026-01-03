import axios from 'axios'
import type { Page, ImageDto } from './gallery'

const apiClient = axios.create({
  baseURL: '/api',
})

export interface SearchRequestDto {
  includedTags: string[]
  excludedTags: string[]
  anyTags: string[]
  keyword?: string
}

export const searchApi = {
  search: async (request: SearchRequestDto, page: number, size: number, sort?: string) => {
    const params: any = { page, size }
    if (sort) {
      params.sort = sort
    }
    const response = await apiClient.post<Page<ImageDto>>('/search', request, {
      params
    })
    return response.data
  }
}
