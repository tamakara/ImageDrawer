import {defineStore} from 'pinia'
import type {Image, ImageQueryParams} from '../api'
import {queryImages} from '../api'

export const useImageStore = defineStore('image', {
    state: () => ({
        params: {
            rating: 'all',
            sort: 'random',
            tags: [],
            page: 1,
            pageSize: 100,
            seed: Date.now().toString()
        } as ImageQueryParams,
        images: [] as Image[],
        totalElements: 1,
        totalPages: 1,
    }),

    actions: {
        async query() {
            const page = await queryImages(this.params)
            this.images = page.records
            this.totalElements = page.totalElements
            this.totalPages = page.totalPages
        },

        getImageUrl(hash: string): string {
            return import.meta.env.MODE === 'production'
                ? `/images/${hash}`
                : `http://localhost:8080/images/${hash}`
        },

        getImageThumbnailUrl(hash: string): string {
            return import.meta.env.MODE === 'production'
                ? `/thumbnail/${hash}`
                : `http://localhost:8080/thumbnail/${hash}`
        }
    }
})
