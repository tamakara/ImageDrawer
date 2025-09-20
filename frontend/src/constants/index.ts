/** API 基础URL配置 */
export const API_BASE_URL = import.meta.env.MODE === 'production'
  ? '/'
  : 'http://localhost:8080'

/** 图片服务URL配置 */
export const IMAGE_BASE_URL = import.meta.env.MODE === 'production'
  ? '/images'
  : 'http://localhost:8080/images'

/** 缩略图服务URL配置 */
export const THUMBNAIL_BASE_URL = import.meta.env.MODE === 'production'
  ? '/thumbnail'
  : 'http://localhost:8080/thumbnail'

/** 图片评级选项 */
export const IMAGE_RATINGS = [
  { label: '全部', value: 'all' },
  { label: '一般', value: 'general' },
  { label: '敏感', value: 'sensitive' },
  { label: '裸露', value: 'explicit' }
] as const

/** 排序选项 */
export const SORT_OPTIONS = [
  { label: '随机', value: 'random' },
  { label: '文件大小', value: 'size' },
  { label: '添加时间', value: 'time' }
] as const

/** 默认分页配置 */
export const DEFAULT_PAGE_SIZE = 100

/** 移动端检测正则 */
export const MOBILE_REGEX = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i

/** API 路径常量 */
export const API_PATHS = {
  IMAGE: '/image',
  TAG: '/tag',
  QUERY: '/query',
  FILE: '/file'
} as const
