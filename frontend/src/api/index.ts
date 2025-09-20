// 重新导出类型定义
export type * from '@/types'

// 重新导出所有API服务
export * from './services'

// 重新导出HTTP客户端
export { httpClient } from './http'

// 导出具体的方法以保持向后兼容
import { ImageService, TagService, QueryService, FileService } from './services'

export const addImage = ImageService.addImage
export const getImage = ImageService.getImage
export const deleteImage = ImageService.deleteImage

export const setTaggerUrl = TagService.setTaggerUrl
export const queryTags = TagService.queryTags

export const queryImages = QueryService.queryImages

export const getImageFile = FileService.getImageFile
export const getImageFileZip = FileService.getImageFileZip
