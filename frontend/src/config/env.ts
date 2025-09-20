/**
 * 环境配置管理
 */
export class EnvConfig {
  /** 是否为生产环境 */
  static readonly isProduction = import.meta.env.MODE === 'production'

  /** 是否为开发环境 */
  static readonly isDevelopment = import.meta.env.MODE === 'development'

  /** API基础URL */
  static readonly apiBaseUrl = EnvConfig.isProduction
    ? '/'
    : 'http://localhost:8080'

  /** 应用标题 */
  static readonly appTitle = 'ImageDrawer'

  /** 默认分页大小 */
  static readonly defaultPageSize = 100

  /** 上传文件最大大小 (MB) */
  static readonly maxFileSize = 10
}

/**
 * 获取环境变量值
 * @param key 环境变量键名
 * @param defaultValue 默认值
 */
export function getEnvVar(key: string, defaultValue?: string): string {
  return import.meta.env[key] || defaultValue || ''
}
