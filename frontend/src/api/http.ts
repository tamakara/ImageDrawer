import axios, { type AxiosInstance, type AxiosResponse } from 'axios'
import { API_BASE_URL } from '@/constants'

/**
 * 创建 Axios 实例
 */
const httpClient: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
})

/**
 * 请求拦截器
 */
httpClient.interceptors.request.use(
  (config) => {
    // 可以在这里添加认证token等
    return config
  },
  (error) => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

/**
 * 响应拦截器
 */
httpClient.interceptors.response.use(
  (response: AxiosResponse) => {
    return response
  },
  (error) => {
    console.error('API 请求错误:', error)
    // 可以在这里添加全局错误处理
    return Promise.reject(error)
  }
)

export { httpClient }
