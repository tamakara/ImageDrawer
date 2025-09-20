/** 图片实体 */
export interface Image {
  filename: string
  mimetype: string
  hash: string
  size: number
  width: number
  height: number
  rating: string
  tags: string[]
  uploadTime?: string // 添加上传时间字段
}

/** 分页信息 */
export interface Page<T> {
  records: T[]
  totalElements: number
  totalPages: number
  page: number
  pageSize: number
}

/** 图片查询条件 */
export interface ImageQueryParams {
  tags: string[]
  sort: string
  rating: string
  page: number
  pageSize: number
  seed: string
}

/** 图片评级类型 */
export type ImageRating = 'all' | 'general' | 'sensitive' | 'explicit'

/** 排序方式类型 */
export type SortType = 'random' | 'size' | 'time'

/** API响应类型 */
export interface ApiResponse<T = any> {
  data: T
  message?: string
  code?: number
}

/** 文件下载响应 */
export interface FileDownloadResponse {
  blob: Blob
  filename: string
}
