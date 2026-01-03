<script setup lang="ts">
import { useQuery } from '@tanstack/vue-query'
import { searchApi } from '../../api/search'
import { tagsApi } from '../../api/tags'
import { computed, ref, reactive } from 'vue'
import { NSpin, NEmpty, NSelect, NInput, NButton, NForm, NFormItem, NSpace, NRadioGroup, NRadioButton, NIcon, NLayout, NLayoutSider, NLayoutContent, NLayoutFooter, NPagination } from 'naive-ui'
import ImageDetail from '../../components/business/ImageDetail.vue'
import { Search24Regular, Dismiss24Regular } from '@vicons/fluent'

// Form State
const formState = reactive({
  keyword: '',
  includedTags: [] as string[],
  excludedTags: [] as string[],
  anyTags: [] as string[],
  sortBy: 'createdAt',
  sortDirection: 'DESC'
})

// Active Search State (applied on search click)
const activeSearchState = ref({...formState})
const page = ref(1)
const pageSize = ref(20)

// Tag Autocomplete
const tagQuery = ref('')
const { data: availableTags } = useQuery({
  queryKey: ['tags', tagQuery],
  queryFn: () => tagsApi.listTags(tagQuery.value),
  enabled: true
})
const tagOptions = computed(() => availableTags.value?.map(t => ({ label: t.name, value: t.name })) || [])
function handleTagSearch(query: string) { tagQuery.value = query }

// Search Action
function handleSearch() {
  page.value = 1
  activeSearchState.value = { ...formState }
}

function handleReset() {
  formState.keyword = ''
  formState.includedTags = []
  formState.excludedTags = []
  formState.anyTags = []
  formState.sortBy = 'createdAt'
  formState.sortDirection = 'DESC'
  handleSearch()
}

// Query
const {
  data,
  isLoading,
  refetch
} = useQuery({
  queryKey: ['images', activeSearchState, page, pageSize],
  queryFn: () => {
    const sort = `${activeSearchState.value.sortBy},${activeSearchState.value.sortDirection}`
    return searchApi.search({
      keyword: activeSearchState.value.keyword,
      includedTags: activeSearchState.value.includedTags,
      excludedTags: activeSearchState.value.excludedTags,
      anyTags: activeSearchState.value.anyTags
    }, page.value - 1, pageSize.value, sort)
  }
})

const images = computed(() => data.value?.content || [])
const totalCount = computed(() => data.value?.totalElements || 0)

// Detail Modal
const showDetail = ref(false)
const selectedImage = ref<any>(null)
function openDetail(image: any) {
  selectedImage.value = image
  showDetail.value = true
}

const sortOptions = [
  { label: '创建时间', value: 'createdAt' },
  { label: '修改时间', value: 'updatedAt' },
  { label: '文件大小', value: 'size' },
  { label: '标题', value: 'title' }
]
</script>

<template>
  <n-layout has-sider class="h-full">
    <n-layout-sider
      bordered
      collapse-mode="transform"
      :collapsed-width="0"
      show-trigger="bar"
      :native-scrollbar="false"
      class="z-10"
    >
      <div class="flex flex-col h-full">
        <div class="p-4 border-b border-gray-100 dark:border-gray-800">
          <h2 class="text-lg font-medium">搜索与筛选</h2>
        </div>

        <div class="flex-1 overflow-y-auto p-4">
          <n-form size="small" label-placement="top">
            <n-form-item label="关键字">
              <n-input v-model:value="formState.keyword" placeholder="标题或文件名" @keydown.enter="handleSearch" />
            </n-form-item>

            <n-form-item label="包含标签 (AND)">
              <n-select v-model:value="formState.includedTags" multiple filterable remote :options="tagOptions" @search="handleTagSearch" placeholder="选择标签" />
            </n-form-item>

            <n-form-item label="排除标签 (NOT)">
              <n-select v-model:value="formState.excludedTags" multiple filterable remote :options="tagOptions" @search="handleTagSearch" placeholder="选择标签" />
            </n-form-item>

            <n-form-item label="任意标签 (OR)">
              <n-select v-model:value="formState.anyTags" multiple filterable remote :options="tagOptions" @search="handleTagSearch" placeholder="选择标签" />
            </n-form-item>

            <n-form-item label="排序依据">
              <n-select v-model:value="formState.sortBy" :options="sortOptions" />
            </n-form-item>

            <n-form-item label="排序方向">
              <n-radio-group v-model:value="formState.sortDirection" name="sortDirection">
                <n-space>
                  <n-radio-button value="ASC">升序</n-radio-button>
                  <n-radio-button value="DESC">降序</n-radio-button>
                </n-space>
              </n-radio-group>
            </n-form-item>
          </n-form>
        </div>

        <div class="p-4 border-t border-gray-100 dark:border-gray-800 flex gap-2">
          <n-button type="primary" class="flex-1" @click="handleSearch">
            <template #icon><n-icon><Search24Regular /></n-icon></template>
            搜索
          </n-button>
          <n-button class="flex-1" @click="handleReset">
            <template #icon><n-icon><Dismiss24Regular /></n-icon></template>
            重置
          </n-button>
        </div>
      </div>
    </n-layout-sider>

    <n-layout class="h-full" content-style="display: flex; flex-direction: column; height: 100%;">
      <n-layout-content :native-scrollbar="false" content-style="padding: 16px;" class="flex-1">
        <div v-if="isLoading && !images.length" class="flex justify-center items-center h-full">
          <n-spin size="large" />
        </div>

        <div v-else-if="images.length === 0" class="flex justify-center items-center h-full">
          <n-empty description="未找到图片" />
        </div>

        <div v-else class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 2xl:grid-cols-6 gap-4">
          <div
            v-for="image in images"
            :key="image.id"
            class="relative group cursor-pointer rounded-lg overflow-hidden shadow-sm hover:shadow-md transition-shadow bg-gray-200 dark:bg-gray-800 aspect-square"
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
      </n-layout-content>
      <n-layout-footer bordered class="p-4">
        <div class="flex justify-center">
          <n-pagination
            v-model:page="page"
            v-model:page-size="pageSize"
            :item-count="totalCount"
            :page-sizes="[10, 20, 50, 100]"
            show-size-picker
            show-quick-jumper
          />
        </div>
      </n-layout-footer>
    </n-layout>
  </n-layout>

  <ImageDetail
    v-model:show="showDetail"
    :image-id="selectedImage?.id"
    @refresh="refetch"
  />
</template>

