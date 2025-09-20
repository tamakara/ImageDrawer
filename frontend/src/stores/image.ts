import { defineStore } from 'pinia'
import { QueryService } from '@/api/services/queryService'
import { IMAGE_BASE_URL, THUMBNAIL_BASE_URL, DEFAULT_PAGE_SIZE } from '@/constants'
import { generateSeed } from '@/utils'
import type { Image, ImageQueryParams } from '@/types'

export const useImageStore = defineStore('image', {
    state: () => ({
        params: {
            rating: 'all',
            sort: 'random',
            tags: [],
            page: 1,
            pageSize: DEFAULT_PAGE_SIZE,
            seed: generateSeed()
        } as ImageQueryParams,
        images: [] as Image[],
        totalElements: 1,
        totalPages: 1,
        loading: false,
    }),

    getters: {
        /**
         * 获取图片完整URL
         */
        getImageUrl: () => (hash: string): string => {
            return `${IMAGE_BASE_URL}/${hash}`
        },

        /**
         * 获取图片缩略图URL
         */
        getImageThumbnailUrl: () => (hash: string, size: number = 300): string => {
            return `${THUMBNAIL_BASE_URL}/${hash}?size=${size}`
        },

        /**
         * 是否有图片数据
         */
        hasImages: (state): boolean => {
            return state.images.length > 0
        },

        /**
         * 是否正在加载
         */
        isLoading: (state): boolean => {
            return state.loading
        }
    },

    actions: {
        /**
         * 查询图片
         */
        async query(): Promise<void> {
            try {
                this.loading = true
                const page = await QueryService.queryImages(this.params)
                this.images = page.records
                this.totalElements = page.totalElements
                this.totalPages = page.totalPages
            } catch (error) {
                console.error('查询图片失败:', error)
                throw error
            } finally {
                this.loading = false
            }
        },

        /**
         * 重置查询参数
         */
        resetParams(): void {
            this.params = {
                rating: 'all',
                sort: 'random',
                tags: [],
                page: 1,
                pageSize: DEFAULT_PAGE_SIZE,
                seed: generateSeed()
            }
        },

        /**
         * 更新查询参数
         */
        updateParams(newParams: Partial<ImageQueryParams>): void {
            this.params = { ...this.params, ...newParams }
        },

        /**
         * 添加标签
         */
        addTag(tag: string): void {
            if (!this.params.tags.includes(tag)) {
                this.params.tags.push(tag)
            }
        },

        /**
         * 移除标签
         */
        removeTag(tag: string): void {
            const index = this.params.tags.indexOf(tag)
            if (index > -1) {
                this.params.tags.splice(index, 1)
            }
        },

        /**
         * 清空标签
         */
        clearTags(): void {
            this.params.tags = []
        }
    }
})
