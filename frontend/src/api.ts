import axios from 'axios'
import qs from 'qs'
import type { Image, Page, QueryParams } from './types'

// 创建axios实例
const api = axios.create({
  baseURL: '/', // 修改为根路径，不使用/api前缀
  timeout: 10000
})

// 图片API服务
export const imageAPI = {
  // 查询图片
  async query(params: Partial<QueryParams>): Promise<Page<Image>> {
    const response = await api.get('/query', {
      params,
      paramsSerializer: params => qs.stringify(params, { arrayFormat: 'repeat' })
    })
    return response.data
  },

  // 上传图片
  async upload(file: File): Promise<void> {
    const formData = new FormData()
    formData.append('file', file)
    await api.post('/image', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
  },

  // 删除图片
  async delete(hash: string): Promise<void> {
    await api.delete(`/image/${hash}`)
  },

  // 获取图片信息
  async getImage(hash: string): Promise<Image> {
    const response = await api.get(`/image/${hash}`)
    return response.data
  },

  // 获取图片文件URL
  getImageUrl(hash: string): string {
    return `/file?hash=${hash}`
  },

  // 获取缩略图URL (使用同样的文件接口)
  getThumbnailUrl(hash: string): string {
    return `/file?hash=${hash}`
  },

  // 下载图片文件
  async downloadImage(hash: string): Promise<Blob> {
    const response = await api.get(`/file?hash=${hash}`, {
      responseType: 'blob'
    })
    return response.data
  },

  // 批量下载图片为ZIP
  async downloadZip(hashes: string[]): Promise<Blob> {
    const response = await api.post('/file/zip', hashes, {
      responseType: 'blob'
    })
    return response.data
  }
}

// 标签API服务
export const tagAPI = {
  // 获取标签列表
  async queryTags(keyword?: string): Promise<string[]> {
    const response = await api.get('/tag/query', {
      params: keyword ? { keyword } : {}
    })
    return response.data
  },

  // 设置Tagger服务URL
  async setTaggerUrl(url: string): Promise<void> {
    await api.put('/tag/tagger', url, {
      headers: {
        'Content-Type': 'text/plain'
      }
    })
  }
}
