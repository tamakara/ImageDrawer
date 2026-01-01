<script setup lang="ts">
import { useInfiniteQuery, useQuery } from '@tanstack/vue-query'
import { galleryApi } from '../../api/gallery'
import { searchApi } from '../../api/search'
import { tagsApi } from '../../api/tags'
import { computed, ref, watch } from 'vue'
import { useIntersectionObserver } from '@vueuse/core'
import { NSpin, NEmpty, NSelect, NCollapse, NCollapseItem } from 'naive-ui'
import ImageDetail from '../../components/business/ImageDetail.vue'

// 搜索状态
const searchTags = ref<string[]>([])
const excludedTags = ref<string[]>([])
const anyTags = ref<string[]>([])
const isSearching = computed(() => searchTags.value.length > 0 || excludedTags.value.length > 0 || anyTags.value.length > 0)

// 标签自动补全
const tagQuery = ref('')
const { data: availableTags } = useQuery({
  queryKey: ['tags', tagQuery],
  queryFn: () => tagsApi.listTags(tagQuery.value),
  enabled: true
})

const tagOptions = computed(() => availableTags.value?.map(t => ({ label: t.name, value: t.name })) || [])

function handleTagSearch(query: string) {
  tagQuery.value = query
}

const {
  data,
  fetchNextPage,
  hasNextPage,
  isFetchingNextPage,
  isLoading,
  refetch
} = useInfiniteQuery({
  queryKey: ['images', searchTags, excludedTags, anyTags],
  queryFn: ({ pageParam = 0 }) => {
    if (isSearching.value) {
      return searchApi.search({
        includedTags: searchTags.value,
        excludedTags: excludedTags.value,
        anyTags: anyTags.value
      }, pageParam, 20)
    }
    return galleryApi.listImages(pageParam, 20)
  },
  getNextPageParam: (lastPage) => {
    return lastPage.last ? undefined : lastPage.number + 1
  },
  initialPageParam: 0
})

// 搜索变更时重新获取
watch([searchTags, excludedTags, anyTags], () => {
  refetch()
})

const images = computed(() => {
  return data.value?.pages.flatMap(page => page.content) || []
})

const loadMoreTrigger = ref<HTMLElement | null>(null)

useIntersectionObserver(
  loadMoreTrigger,
  (entries) => {
    const entry = entries[0]
    if (entry && entry.isIntersecting && hasNextPage.value) {
      fetchNextPage()
    }
  }
)

// 详情模态框
const showDetail = ref(false)
const selectedImage = ref<any>(null)

function openDetail(image: any) {
  selectedImage.value = image
  showDetail.value = true
}
</script>

<template>
  <div class="p-4 h-full flex flex-col">
    <!-- Search Bar -->
    <div class="mb-4">
      <n-collapse>
        <n-collapse-item title="搜索" name="1">
          <div class="flex flex-col gap-2">
            <n-select
              v-model:value="searchTags"
              multiple
              filterable
              placeholder="包含标签 (AND)"
              :options="tagOptions"
              @search="handleTagSearch"
              remote
              clearable
            />
            <n-select
              v-model:value="excludedTags"
              multiple
              filterable
              placeholder="排除标签 (NOT)"
              :options="tagOptions"
              @search="handleTagSearch"
              remote
              clearable
            />
             <n-select
              v-model:value="anyTags"
              multiple
              filterable
              placeholder="任意标签 (OR)"
              :options="tagOptions"
              @search="handleTagSearch"
              remote
              clearable
            />
          </div>
        </n-collapse-item>
      </n-collapse>
    </div>

    <div v-if="isLoading" class="flex justify-center items-center h-full">
      <n-spin size="large" />
    </div>

    <div v-else-if="images.length === 0" class="flex justify-center items-center h-full">
      <n-empty description="未找到图片" />
    </div>

    <div v-else class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-4">
      <div
        v-for="image in images"
        :key="image.id"
        class="relative group cursor-pointer rounded-lg overflow-hidden shadow-sm hover:shadow-md transition-shadow bg-gray-100 dark:bg-gray-800 aspect-square"
        @click="openDetail(image)"
      >
        <img
          :src="image.thumbnailUrl || image.url"
          :alt="image.title || 'image'"
          class="w-full h-full object-cover transition-transform duration-300 group-hover:scale-105"
          loading="lazy"
        />
        <div class="absolute bottom-0 left-0 right-0 bg-gradient-to-t from-black/70 to-transparent p-2 opacity-0 group-hover:opacity-100 transition-opacity">
          <p class="text-white text-xs truncate">{{ image.title }}</p>
        </div>
      </div>
    </div>

    <div ref="loadMoreTrigger" class="h-10 flex justify-center items-center mt-4">
      <n-spin v-if="isFetchingNextPage" size="small" />
    </div>

    <ImageDetail
      v-model:show="showDetail"
      :image-id="selectedImage?.id"
      @refresh="refetch"
    />
  </div>
</template>


