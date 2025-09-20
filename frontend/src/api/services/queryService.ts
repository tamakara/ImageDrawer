import { httpClient } from '../http'
import { API_PATHS } from '@/constants'
import type { Page, Image, ImageQueryParams } from '@/types'
import qs from 'qs'

/**
 * 查询相关API服务
 */
export class QueryService {
  /**
   * 查询图片
   * @param params 查询条件
   */
  static async queryImages(params: ImageQueryParams): Promise<Page<Image>> {
    const response = await httpClient.get<Page<Image>>(API_PATHS.QUERY, {
      params,
      paramsSerializer: params => qs.stringify(params, { arrayFormat: 'repeat' })
    })
    return response.data
  }
}
