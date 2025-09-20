import { httpClient } from '../http'
import { API_PATHS } from '@/constants'

/**
 * 标签相关API服务
 */
export class TagService {
  /**
   * 设置tagger服务器地址
   * @param url 服务器地址
   */
  static async setTaggerUrl(url: string): Promise<void> {
    await httpClient.put(`${API_PATHS.TAG}/tagger`, url, {
      headers: {
        'Content-Type': 'text/plain'
      }
    })
  }

  /**
   * 查询匹配标签
   * @param keyword 关键词
   */
  static async queryTags(keyword: string): Promise<string[]> {
    const response = await httpClient.get<string[]>(`${API_PATHS.TAG}/query`, {
      params: { keyword }
    })
    return response.data
  }
}
