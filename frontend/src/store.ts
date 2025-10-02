import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { imageAPI, tagAPI } from './api'
import type { Image, QueryParams } from './types'

export const useImageStore = defineStore('image', () => {
  // 状态
  const images = ref<Image[]>([])
  const loading = ref(false)
  const total = ref(0)
  const totalPages = ref(0)
  const currentPage = ref(1)
  const allTags = ref<string[]>([])
  const selectedImages = ref<Set<string>>(new Set())

  // 查询参数
  const queryParams = ref<Partial<QueryParams>>({
    tags: [],
    sort: 'random',
    rating: 'all',
    page: 1,
    pageSize: 50,
    seed: Math.floor(Math.random() * 1000000).toString() // 生成整数形式的随机种子
  })

  // 计算属性
  const hasImages = computed(() => images.value.length > 0)
  const isEmpty = computed(() => !loading.value && !hasImages.value)
  const hasSelection = computed(() => selectedImages.value.size > 0)

  // 方法
  const loadImages = async () => {
    loading.value = true
    try {
      const data = await imageAPI.query(queryParams.value)
      images.value = data.records
      total.value = data.totalElements
      totalPages.value = data.totalPages
      currentPage.value = data.page
    } catch (error) {
      console.error('加载图片失败:', error)
      images.value = []
      total.value = 0
    } finally {
      loading.value = false
    }
  }

  const loadTags = async (keyword?: string) => {
    try {
      allTags.value = await tagAPI.queryTags(keyword)
    } catch (error) {
      console.error('加载标签失败:', error)
      allTags.value = []
    }
  }

  const uploadImage = async (file: File) => {
    try {
      await imageAPI.upload(file)
      // 上传成功后重新加载图片列表
      await loadImages()
    } catch (error) {
      console.error('上传失败:', error)
      throw error
    }
  }

  const deleteImage = async (hash: string) => {
    try {
      await imageAPI.delete(hash)
      // 从本地列表中移除
      const index = images.value.findIndex(img => img.hash === hash)
      if (index > -1) {
        images.value.splice(index, 1)
        total.value--
      }
      // 从选中列表中移除
      selectedImages.value.delete(hash)
    } catch (error) {
      console.error('删除失败:', error)
      throw error
    }
  }

  const deleteSelectedImages = async () => {
    const hashes = Array.from(selectedImages.value)
    try {
      await Promise.all(hashes.map(hash => imageAPI.delete(hash)))
      // 从本地列表中移除
      images.value = images.value.filter(img => !selectedImages.value.has(img.hash))
      total.value -= hashes.length
      selectedImages.value.clear()
    } catch (error) {
      console.error('批量删除失败:', error)
      throw error
    }
  }

  const downloadSelectedImages = async () => {
    const hashes = Array.from(selectedImages.value)
    if (hashes.length === 0) return

    try {
      if (hashes.length === 1) {
        // 单个文件直接下载
        const blob = await imageAPI.downloadImage(hashes[0])
        const image = images.value.find(img => img.hash === hashes[0])
        const url = URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = image?.filename || 'image'
        a.click()
        URL.revokeObjectURL(url)
      } else {
        // 多个文件打包下载
        const blob = await imageAPI.downloadZip(hashes)
        const url = URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = `images_${Date.now()}.zip`
        a.click()
        URL.revokeObjectURL(url)
      }
    } catch (error) {
      console.error('下载失败:', error)
      throw error
    }
  }

  const updateQuery = (params: Partial<QueryParams>) => {
    Object.assign(queryParams.value, params)
    // 重置页码如果不是翻页操作
    if (!params.page) {
      queryParams.value.page = 1
    }
    loadImages()
  }

  const goToPage = (page: number) => {
    queryParams.value.page = page
    loadImages()
  }

  const refresh = () => {
    queryParams.value.seed = Math.floor(Math.random() * 1000000).toString() // 修复refresh中的seed生成
    queryParams.value.page = 1
    selectedImages.value.clear()
    loadImages()
  }

  const toggleImageSelection = (hash: string) => {
    if (selectedImages.value.has(hash)) {
      selectedImages.value.delete(hash)
    } else {
      selectedImages.value.add(hash)
    }
  }

  const selectAllImages = () => {
    images.value.forEach(img => selectedImages.value.add(img.hash))
  }

  const clearSelection = () => {
    selectedImages.value.clear()
  }

  const isImageSelected = (hash: string) => {
    return selectedImages.value.has(hash)
  }

  return {
    // 状态
    images,
    loading,
    total,
    totalPages,
    currentPage,
    allTags,
    selectedImages,
    queryParams,

    // 计算属性
    hasImages,
    isEmpty,
    hasSelection,

    // 方法
    loadImages,
    loadTags,
    uploadImage,
    deleteImage,
    deleteSelectedImages,
    downloadSelectedImages,
    updateQuery,
    goToPage,
    refresh,
    toggleImageSelection,
    selectAllImages,
    clearSelection,
    isImageSelected
  }
})
