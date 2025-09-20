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
export const MOBILE_REGEX = /Android|webOS|iPhone|iPod|BlackBerry|IEMobile|Opera Mini/i

/** 平板设备检测正则 */
export const TABLET_REGEX = /iPad|Android(?=.*Mobile)|Tablet|PlayBook|Kindle|Silk/i

/** 响应式断点 */
export const BREAKPOINTS = {
  xs: 0,      // 超小屏手机
  sm: 576,    // 小屏手机
  md: 768,    // 平板
  lg: 992,    // 小型桌面
  xl: 1200,   // 大型桌面
  xxl: 1600   // 超大桌面
} as const

/** 网格列数配置 */
export const GRID_COLUMNS = {
  mobile: { min: 2, max: 3 },
  tablet: { min: 4, max: 5 },
  desktop: { min: 4, max: 8 }
} as const

/** API 路径常量 */
export const API_PATHS = {
  IMAGE: '/image',
  TAG: '/tag',
  QUERY: '/query',
  FILE: '/file'
} as const
