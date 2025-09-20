<template>
  <el-form :model="params" class="query-form" :class="formClass" label-position="top">
    <!-- 图片分级 -->
    <el-form-item label="图片分级" class="form-item rating-item">
      <el-radio-group
        v-model="params.rating"
        class="rating-group"
        :size="controlSize"
      >
        <el-radio-button
          v-for="option in IMAGE_RATINGS"
          :key="option.value"
          :value="option.value"
          :label="option.label"
        />
      </el-radio-group>
    </el-form-item>

    <!-- 排序方式 -->
    <el-form-item label="排序方式" class="form-item sort-item">
      <el-select
        v-model="params.sort"
        placeholder="选择排序方式"
        :size="controlSize"
        class="sort-select"
      >
        <el-option
          v-for="option in SORT_OPTIONS"
          :key="option.value"
          :label="option.label"
          :value="option.value"
        />
      </el-select>
    </el-form-item>

    <!-- 标签筛选 -->
    <el-form-item label="标签筛选" class="form-item tag-item">
      <div class="tag-input-wrapper">
        <el-autocomplete
          v-model="tagText"
          :fetch-suggestions="fetchTagsDebounced"
          :trigger-on-focus="false"
          placeholder="输入标签名称..."
          :size="controlSize"
          class="tag-input"
          clearable
          @keydown.enter.prevent="onSubmit"
          @select="onTagSelect"
        >
          <template #suffix>
            <el-button
              :icon="Plus"
              text
              @click="onSubmit"
              :disabled="!tagText.trim()"
              class="add-tag-btn"
            />
          </template>
        </el-autocomplete>
      </div>
    </el-form-item>

    <!-- 已选标签 -->
    <el-form-item v-if="params.tags.length > 0" class="form-item tag-list-item">
      <div class="tag-list">
        <el-tag
          v-for="tag in params.tags"
          :key="tag"
          closable
          :size="tagSize"
          @close="handleTagClose(tag)"
          class="tag-item"
          :effect="isMobile ? 'plain' : 'light'"
        >
          {{ tag }}
        </el-tag>
      </div>
    </el-form-item>

    <!-- 操作按钮 -->
    <el-form-item class="form-item actions-item">
      <div class="action-buttons">
        <el-button
          @click="onQuery"
          type="primary"
          :size="controlSize"
          :loading="imageStore.loading"
          :icon="Search"
          class="query-btn"
        >
          {{ imageStore.loading ? '查询中...' : '查询图片' }}
        </el-button>

        <el-button
          @click="onReset"
          :size="controlSize"
          :icon="Refresh"
          class="reset-btn"
          v-if="hasFilters"
        >
          重置筛选
        </el-button>
      </div>
    </el-form-item>

    <!-- 快速筛选 -->
    <el-form-item v-if="!isMobile" label="快速筛选" class="form-item quick-filter-item">
      <div class="quick-filters">
        <el-button-group :size="controlSize">
          <el-button
            v-for="filter in quickFilters"
            :key="filter.label"
            @click="applyQuickFilter(filter)"
            :type="isQuickFilterActive(filter) ? 'primary' : 'default'"
            :size="controlSize"
          >
            {{ filter.label }}
          </el-button>
        </el-button-group>
      </div>
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Plus, Refresh } from '@element-plus/icons-vue'
import { storeToRefs } from 'pinia'
import { useImageStore } from '@/stores/image'
import { queryTags } from '@/api'
import { debounce, getDeviceType, DeviceType } from '@/utils'
import { IMAGE_RATINGS, SORT_OPTIONS } from '@/constants'

const imageStore = useImageStore()
const { params } = storeToRefs(imageStore)

const tagText = ref('')
const deviceType = ref<DeviceType>(getDeviceType())

// 快速筛选选项
const quickFilters = [
  { label: '最新', rating: 'all', sort: 'time', tags: [] },
  { label: '随机', rating: 'all', sort: 'random', tags: [] },
  { label: '大图', rating: 'all', sort: 'size', tags: [] },
  { label: '一般', rating: 'general', sort: 'random', tags: [] },
]

// 计算属性
const isMobile = computed(() => deviceType.value === DeviceType.MOBILE)
const isTablet = computed(() => deviceType.value === DeviceType.TABLET)

const formClass = computed(() => ({
  'form-mobile': isMobile.value,
  'form-tablet': isTablet.value,
  'form-desktop': deviceType.value === DeviceType.DESKTOP
}))

const controlSize = computed(() => {
  if (isMobile.value) return 'large'
  if (isTablet.value) return 'default'
  return 'default'
})

const tagSize = computed(() => {
  if (isMobile.value) return 'large'
  return 'default'
})

const hasFilters = computed(() => {
  return params.value.rating !== 'all' ||
         params.value.sort !== 'random' ||
         params.value.tags.length > 0
})

// 防抖的标签搜索
const fetchTagsDebounced = debounce(async (queryString: string, cb: (results: { value: string }[]) => void) => {
  if (!queryString.trim()) {
    cb([])
    return
  }

  try {
    const tags = await queryTags(queryString)
    cb(tags.map(t => ({ value: t })))
  } catch (error) {
    console.error('获取标签失败:', error)
    cb([])
  }
}, 300)

// 事件处理
const onQuery = async () => {
  try {
    await imageStore.query()
    ElMessage.success('查询完成')
  } catch (error) {
    console.error('查询失败:', error)
    ElMessage.error('查询失败，请重试')
  }
}

const onReset = () => {
  params.value.rating = 'all'
  params.value.sort = 'random'
  params.value.tags = []
  params.value.page = 1
  tagText.value = ''
  ElMessage.info('已重置筛选条件')
}

const onSubmit = () => {
  const tag = tagText.value.trim()
  if (!tag) {
    ElMessage.warning('请输入标签名称')
    return
  }

  if (params.value.tags.includes(tag)) {
    ElMessage.warning('标签已存在')
    return
  }

  params.value.tags.push(tag)
  tagText.value = ''
  ElMessage.success(`已添加标签: ${tag}`)
}

const onTagSelect = (item: { value: string }) => {
  tagText.value = item.value
  onSubmit()
}

const handleTagClose = (tag: string) => {
  params.value.tags = params.value.tags.filter(t => t !== tag)
  ElMessage.info(`已移除标签: ${tag}`)
}

const applyQuickFilter = (filter: any) => {
  params.value.rating = filter.rating
  params.value.sort = filter.sort
  params.value.tags = [...filter.tags]
  params.value.page = 1
  ElMessage.success(`已应用筛选: ${filter.label}`)
  onQuery()
}

const isQuickFilterActive = (filter: any) => {
  return params.value.rating === filter.rating &&
         params.value.sort === filter.sort &&
         JSON.stringify(params.value.tags) === JSON.stringify(filter.tags)
}

// 响应式处理
const handleResize = debounce(() => {
  deviceType.value = getDeviceType()
}, 300)

onMounted(() => {
  window.addEventListener('resize', handleResize)
})
</script>

<style scoped>
.query-form {
  width: 100%;
}

.form-item {
  margin-bottom: 20px;
}

.form-mobile .form-item {
  margin-bottom: 16px;
}

/* 分级选��器 */
.rating-group {
  width: 100%;
  display: flex;
  border-radius: 6px;
  border: 1px solid var(--el-border-color);
  overflow: hidden;
  background: var(--el-bg-color);
}

.rating-group :deep(.el-radio-button) {
  flex: 1;
  border: none !important;
}

.rating-group :deep(.el-radio-button__inner) {
  width: 100%;
  border: none !important;
  border-radius: 0 !important;
  margin: 0 !important;
  padding: 8px 12px;
  background: transparent;
  position: relative;
}

.rating-group :deep(.el-radio-button:not(:last-child) .el-radio-button__inner::after) {
  content: '';
  position: absolute;
  right: 0;
  top: 20%;
  bottom: 20%;
  width: 1px;
  background: var(--el-border-color-lighter);
}

.rating-group :deep(.el-radio-button.is-active .el-radio-button__inner) {
  background: var(--el-color-primary);
  color: white;
}

.rating-group :deep(.el-radio-button.is-active .el-radio-button__inner::after) {
  display: none;
}

.rating-group :deep(.el-radio-button.is-active + .el-radio-button .el-radio-button__inner::after) {
  display: none;
}

.rating-group :deep(.el-radio-button__original) {
  display: none;
}

/* 排序选择器 */
.sort-select {
  width: 100%;
}

/* 标签输入 */
.tag-input-wrapper {
  width: 100%;
}

.tag-input {
  width: 100%;
}

.add-tag-btn {
  padding: 0;
  margin-right: 8px;
}

/* 标签列表 */
.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  width: 100%;
}

.form-mobile .tag-list {
  gap: 6px;
}

.tag-item {
  margin: 0;
}

/* 操作按钮 */
.action-buttons {
  display: flex;
  gap: 12px;
  width: 100%;
}

.form-mobile .action-buttons {
  flex-direction: column;
  gap: 8px;
}

.query-btn {
  flex: 1;
}

.form-mobile .query-btn,
.form-mobile .reset-btn {
  width: 100%;
}

/* 快速筛选 */
.quick-filters {
  width: 100%;
}

.quick-filters :deep(.el-button-group) {
  display: flex;
  flex-wrap: wrap;
  width: 100%;
  gap: 0; /* 移除间隔 */
}

.quick-filters :deep(.el-button) {
  flex: 1;
  margin: 0; /* 移除外边距 */
  border-radius: 0; /* 移除圆角 */
}

.quick-filters :deep(.el-button:first-child) {
  border-top-left-radius: 6px;
  border-bottom-left-radius: 6px;
}

.quick-filters :deep(.el-button:last-child) {
  border-top-right-radius: 6px;
  border-bottom-right-radius: 6px;
}

.quick-filters :deep(.el-button:not(:first-child)) {
  border-left: 0; /* 移除中间按钮的左边框 */
}

/* 响应式调整 */
@media (max-width: 480px) {
  .form-item {
    margin-bottom: 12px;
  }

  .rating-group {
    flex-direction: column;
  }

  .rating-group :deep(.el-radio-button) {
    flex: none;
  }
}

@media (min-width: 481px) and (max-width: 768px) {
  .action-buttons {
    flex-direction: row;
  }

  .query-btn {
    min-width: 120px;
  }
}

/* 深色模式适配 */
@media (prefers-color-scheme: dark) {
  .query-form {
    background: transparent;
  }
}

/* 标签项动画 */
.tag-item {
  transition: all 0.3s ease;
}

.tag-item:hover {
  transform: translateY(-1px);
}

/* 表单标签样式 */
:deep(.el-form-item__label) {
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: 8px;
}

.form-mobile :deep(.el-form-item__label) {
  font-size: 14px;
  margin-bottom: 6px;
}
</style>