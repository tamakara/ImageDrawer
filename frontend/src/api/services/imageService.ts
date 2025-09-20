import { httpClient } from '../http'
import { API_PATHS } from '@/constants'
import type { Image } from '@/types'

/**
 * 图片相关API服务
 */
export class ImageService {
  /**
   * 添加图片
   * @param file 图片文件
   */
  static async addImage(file: File): Promise<Image> {
    const formData = new FormData()
    formData.append('file', file)

    const response = await httpClient.post<Image>(API_PATHS.IMAGE, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
    return response.data
  }

  /**
   * 获取图片信息
   * @param hash 图片hash
   */
  static async getImage(hash: string): Promise<Image> {
    const response = await httpClient.get<Image>(`${API_PATHS.IMAGE}/${hash}`)
    return response.data
  }

  /**
   * 删除图片
   * @param hash 图片hash
   */
  static async deleteImage(hash: string): Promise<void> {
    await httpClient.delete(`${API_PATHS.IMAGE}/${hash}`)
  }
}
