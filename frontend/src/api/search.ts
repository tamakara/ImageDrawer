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
