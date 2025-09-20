import { MOBILE_REGEX, TABLET_REGEX } from '@/constants'

/**
 * 设备类型枚举
 */
export enum DeviceType {
  MOBILE = 'mobile',
  TABLET = 'tablet',
  DESKTOP = 'desktop'
}

/**
 * 检测设备类型
 */
export function getDeviceType(): DeviceType {
  const userAgent = navigator.userAgent

  if (MOBILE_REGEX.test(userAgent)) {
    return DeviceType.MOBILE
  }

  if (TABLET_REGEX.test(userAgent) || (window.innerWidth >= 768 && window.innerWidth <= 1024)) {
    return DeviceType.TABLET
  }

  return DeviceType.DESKTOP
}

/**
 * 检测是否为移动设备（包含平板）
 */
export function isMobileDevice(): boolean {
  const deviceType = getDeviceType()
  return deviceType === DeviceType.MOBILE || deviceType === DeviceType.TABLET
}

/**
 * 检测是否为触摸设备
 */
export function isTouchDevice(): boolean {
  return 'ontouchstart' in window || navigator.maxTouchPoints > 0
}

/**
 * 获取响应式列数
 */
export function getResponsiveColumns(): number {
  const width = window.innerWidth
  const deviceType = getDeviceType()

  if (deviceType === DeviceType.MOBILE) {
    return width < 480 ? 2 : 3
  }

  if (deviceType === DeviceType.TABLET) {
    return width < 900 ? 4 : 5
  }

  // 桌面端
  if (width < 1200) return 4
  if (width < 1600) return 5
  if (width < 2000) return 6
  return 8
}

/**
 * 格式化文件大小
 * @param bytes 字节数
 * @returns 格式化后的文件大小字符串
 */
export function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B'

  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))

  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(2))} ${sizes[i]}`
}

/**
 * 生成随机种子
 */
export function generateSeed(): string {
  return Date.now().toString()
}

/**
 * 下载文件
 * @param blob 文件blob
 * @param filename 文件名
 */
export function downloadFile(blob: Blob, filename: string): void {
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  window.URL.revokeObjectURL(url)
}

/**
 * 防抖函数
 * @param func 要防抖的函数
 * @param wait 等待时间（毫秒）
 */
export function debounce<T extends (...args: any[]) => any>(
  func: T,
  wait: number
): (...args: Parameters<T>) => void {
  let timeout: ReturnType<typeof setTimeout>
  return function(this: any, ...args: Parameters<T>) {
    clearTimeout(timeout)
    timeout = setTimeout(() => func.apply(this, args), wait)
  }
}

/**
 * 节流函数
 * @param func 要节流的函数
 * @param limit 限制时间（毫秒）
 */
export function throttle<T extends (...args: any[]) => any>(
  func: T,
  limit: number
): (...args: Parameters<T>) => void {
  let inThrottle: boolean
  return function(this: any, ...args: Parameters<T>) {
    if (!inThrottle) {
      func.apply(this, args)
      inThrottle = true
      setTimeout(() => inThrottle = false, limit)
    }
  }
}
