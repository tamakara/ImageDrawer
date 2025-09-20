import { httpClient } from '../http'
import { API_PATHS } from '@/constants'
import type { FileDownloadResponse } from '@/types'

/**
 * 文件相关API服务
 */
export class FileService {
  /**
   * 下载单张图片
   * @param hash 图片哈希值
   * @returns blob 和后端生成的文件名
   */
  static async getImageFile(hash: string): Promise<FileDownloadResponse> {
    const response = await httpClient.get(API_PATHS.FILE, {
      params: { hash },
      responseType: 'blob'
    })

    const filename = response.headers['x-filename'] || hash + '.jpg'
    const blob = new Blob([response.data])
    return { blob, filename }
  }

  /**
   * 打包批量下载图片
   * @param hashes 图片哈希数组
   * @returns blob 和后端生成的文件名
   */
  static async getImageFileZip(hashes: string[]): Promise<FileDownloadResponse> {
    const response = await httpClient.post(`${API_PATHS.FILE}/zip`, hashes, {
      responseType: 'blob'
    })

    const filename = response.headers['x-filename'] || 'unknown.zip'
    const blob = new Blob([response.data], { type: 'application/zip' })

    return { blob, filename }
  }
}
