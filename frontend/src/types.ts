// 图片数据类型 (对应后端ImageResponse)
export interface Image {
  filename: string
  mimetype: string
  hash: string
  size: number
  width: number
  height: number
  rating: string
  tags: string[]
  uploadTime?: string
}

// 分页数据类型 (对应后端PageResult)
export interface Page<T> {
  records: T[]
  totalElements: number
  totalPages: number
  page: number
  pageSize: number
}

// 查询参数类型 (对应后端ImageQueryParams)
export interface QueryParams {
  tags: string[]
  sort: string
  rating: string
  page: number
  pageSize: number
  seed: string
}

// 评级类型
export type Rating = 'all' | 'general' | 'sensitive' | 'explicit'

// 排序类型
export type SortType = 'random' | 'size' | 'time'

// 文件响应类型
export interface FileResponse {
  filename: string
  mimeType: string
  resource: Blob
}

// Tagger响应类型
export interface TaggerResponse {
  tags: string[]
  rating: string
}
