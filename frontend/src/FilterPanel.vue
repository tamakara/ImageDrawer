<template>
  <div class="filter-panel">
    <div class="filter-section">
      <div class="filter-title">标签筛选</div>
      <el-select
        v-model="selectedTags"
        multiple
        filterable
        remote
        placeholder="搜索并选择标签"
        style="width: 100%"
        :remote-method="searchTags"
        :loading="tagsLoading"
        @change="updateTags"
      >
        <el-option
          v-for="tag in availableTags"
          :key="tag"
          :label="tag"
          :value="tag"
        />
      </el-select>
    </div>

    <div class="filter-section">
      <div class="filter-title">排序方式</div>
      <el-select
        v-model="sortType"
        placeholder="选择排序"
        style="width: 100%"
        @change="updateSort"
      >
        <el-option label="随机" value="random" />
        <el-option label="文件大小" value="size" />
        <el-option label="上传时间" value="time" />
      </el-select>
    </div>

    <div class="filter-section">
      <div class="filter-title">内容评级</div>
      <el-select
        v-model="rating"
        placeholder="选择评级"
        style="width: 100%"
        @change="updateRating"
      >
        <el-option label="全部" value="all" />
        <el-option label="一般" value="general" />
        <el-option label="敏感" value="sensitive" />
        <el-option label="明确" value="explicit" />
      </el-select>
    </div>

    <div class="filter-section">
      <div class="filter-title">每页显示</div>
      <el-select
        v-model="pageSize"
        placeholder="选择数量"
        style="width: 100%"
        @change="updatePageSize"
      >
        <el-option label="20张" :value="20" />
        <el-option label="50张" :value="50" />
        <el-option label="100张" :value="100" />
        <el-option label="200张" :value="200" />
      </el-select>
    </div>

    <div class="filter-section">
      <el-button type="primary" @click="refresh" style="width: 100%" :loading="imageStore.loading">
        刷新
      </el-button>
    </div>

    <!-- 分页控制 -->
    <div class="filter-section" v-if="imageStore.totalPages > 1">
      <div class="filter-title">分页导航</div>
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="imageStore.queryParams.pageSize"
        :total="imageStore.total"
        layout="prev, pager, next"
        small
        @current-change="handlePageChange"
      />
    </div>

    <!-- 统计信息 -->
    <div class="filter-section">
      <div class="filter-title">统计信息</div>
      <div class="stats">
        <div class="stat-item">
          <span class="stat-label">总图片:</span>
          <span class="stat-value">{{ imageStore.total }}</span>
        </div>
        <div class="stat-item" v-if="imageStore.hasSelection">
          <span class="stat-label">已选择:</span>
          <span class="stat-value">{{ imageStore.selectedImages.size }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">当前页:</span>
          <span class="stat-value">{{ imageStore.currentPage }}/{{ imageStore.totalPages }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useImageStore } from './store'

const imageStore = useImageStore()

const selectedTags = ref<string[]>([])
const sortType = ref('random')
const rating = ref('all')
const pageSize = ref(50)
const currentPage = ref(1)
const availableTags = ref<string[]>([])
const tagsLoading = ref(false)

// 搜索标签
const searchTags = async (query: string) => {
  if (query) {
    tagsLoading.value = true
    try {
      await imageStore.loadTags(query)
      availableTags.value = imageStore.allTags
    } finally {
      tagsLoading.value = false
    }
  } else {
    availableTags.value = imageStore.allTags
  }
}

const updateTags = () => {
  imageStore.updateQuery({ tags: selectedTags.value })
}

const updateSort = () => {
  imageStore.updateQuery({ sort: sortType.value })
}

const updateRating = () => {
  imageStore.updateQuery({ rating: rating.value })
}

const updatePageSize = () => {
  imageStore.updateQuery({ pageSize: pageSize.value })
}

const handlePageChange = (page: number) => {
  imageStore.goToPage(page)
}

const refresh = () => {
  imageStore.refresh()
}

// 监听store中的当前页变化
watch(() => imageStore.currentPage, (newPage) => {
  currentPage.value = newPage
})

// 监听store中的查询参数变化
watch(() => imageStore.queryParams, (newParams) => {
  if (newParams.pageSize) pageSize.value = newParams.pageSize
  if (newParams.sort) sortType.value = newParams.sort
  if (newParams.rating) rating.value = newParams.rating
  if (newParams.tags) selectedTags.value = [...newParams.tags]
}, { deep: true })

onMounted(async () => {
  // 初始加载所有标签
  await imageStore.loadTags()
  availableTags.value = imageStore.allTags

  // 同步初始参数
  const params = imageStore.queryParams
  if (params.pageSize) pageSize.value = params.pageSize
  if (params.sort) sortType.value = params.sort
  if (params.rating) rating.value = params.rating
  if (params.tags) selectedTags.value = [...params.tags]
})
</script>

<style scoped>
.filter-panel {
  padding: 20px;
  height: 100%;
  overflow-y: auto;
}

.filter-section {
  margin-bottom: 24px;
}

.filter-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 12px;
  color: #e4e4e7;
}

.stats {
  background: #27272a;
  border-radius: 6px;
  padding: 12px;
}

.stat-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.stat-item:last-child {
  margin-bottom: 0;
}

.stat-label {
  color: #a1a1aa;
  font-size: 13px;
}

.stat-value {
  color: #e4e4e7;
  font-weight: 500;
  font-size: 13px;
}

:deep(.el-pagination) {
  justify-content: center;
}

:deep(.el-pagination .el-pager li) {
  background: #27272a;
  color: #e4e4e7;
  border: 1px solid #3f3f46;
}

:deep(.el-pagination .el-pager li.is-active) {
  background: #3b82f6;
  color: white;
}
</style>
