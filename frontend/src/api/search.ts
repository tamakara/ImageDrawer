import apiClient from './client'
import type { Page, ImageDto } from './gallery'

export interface SearchRequestDto {
  tagSearch?: string
  keyword?: string
  randomSeed?: string
  widthMin?: number
  widthMax?: number
  heightMin?: number
  heightMax?: number
  sizeMin?: number
  sizeMax?: number
  page?: number
  size?: number
  sort?: string
}

export const searchApi = {
  search: async (request: SearchRequestDto) => {
    const response = await apiClient.post<Page<ImageDto>>('/search', request)
    return response.data
  },

  parseLlm: async (query: string): Promise<SearchRequestDto> => {
    const response = await apiClient.post<SearchRequestDto>('/search/parse-llm', { query })
    return response.data
  }
}
