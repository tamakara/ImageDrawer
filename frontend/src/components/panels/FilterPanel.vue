<template>
  <div class="filter-panel">
    <!-- 图片分级筛选 -->
    <div class="filter-section">
      <div class="section-header">
        <h4 class="section-title">内容分级</h4>
        <div class="section-description">按内容类型筛选图片</div>
      </div>
      <div class="section-content">
        <div class="rating-selector border-secondary bg-surface">
          <button
            v-for="option in IMAGE_RATINGS"
            :key="option.value"
            :class="['rating-option', { active: params.rating === option.value }]"
            @click="params.rating = option.value"
          >
            {{ option.label }}
          </button>
        </div>
      </div>
    </div>

    <!-- 排序方式 -->
    <div class="filter-section">
      <div class="section-header">
        <h4 class="section-title">排序方式</h4>
        <div class="section-description">选择图片展示顺序</div>
      </div>
      <div class="section-content">
        <el-select
          v-model="params.sort"
          placeholder="选择排序方式"
          class="sort-selector"
        >
          <el-option
            v-for="option in SORT_OPTIONS"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </div>
    </div>

    <!-- 标签筛选 -->
    <div class="filter-section">
      <div class="section-header">
        <h4 class="section-title">标签筛选</h4>
        <div class="section-description">通过标签精确查找</div>
      </div>
      <div class="section-content">
        <div class="tag-input-container">
          <el-autocomplete
            v-model="tagText"
            :fetch-suggestions="fetchTagsDebounced"
            :trigger-on-focus="false"
            placeholder="输入标签名称..."
            class="tag-input"
            clearable
            @keydown.enter.prevent="handleAddTag"
            @select="onTagSelect"
          >
            <template #suffix>
              <el-button
                :icon="Plus"
                text
                @click="handleAddTag"
                :disabled="!tagText.trim()"
                class="add-tag-button"
              />
            </template>
          </el-autocomplete>
        </div>

        <!-- 已选标签 -->
        <div v-if="params.tags.length > 0" class="selected-tags">
          <transition-group name="tag" tag="div" class="tags-container">
            <div
              v-for="tag in params.tags"
              :key="tag"
              class="tag-item border-primary bg-surface"
            >
              <span class="tag-text">{{ tag }}</span>
              <button
                class="tag-remove"
                @click="handleRemoveTag(tag)"
                title="移除标签"
              >
                <el-icon><Close /></el-icon>
              </button>
            </div>
          </transition-group>
        </div>
      </div>
    </div>

    <!-- 快速筛选 -->
    <div class="filter-section">
      <div class="section-header">
        <h4 class="section-title">快速筛选</h4>
        <div class="section-description">常用筛选条件</div>
      </div>
      <div class="section-content">
        <div class="quick-filters">
          <button
            v-for="filter in quickFilters"
            :key="filter.label"
            :class="['quick-filter-btn', { active: isQuickFilterActive(filter) }]"
            @click="applyQuickFilter(filter)"
          >
            {{ filter.label }}
          </button>
        </div>
      </div>
    </div>

    <!-- 操作按钮 -->
    <div class="filter-actions">
      <el-button
        type="primary"
        :loading="imageStore.loading"
        @click="handleQuery"
        :icon="Search"
        class="action-button query-button"
      >
        {{ imageStore.loading ? '查询中...' : '应用筛选' }}
      </el-button>

      <el-button
        v-if="hasActiveFilters"
        @click="handleReset"
        :icon="Refresh"
        class="action-button reset-button"
      >
        重置筛选
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Close, Search, Refresh } from '@element-plus/icons-vue'
import { storeToRefs } from 'pinia'
import { useImageStore } from '@/stores/image'
import { queryTags } from '@/api'
import { debounce } from '@/utils'
import { IMAGE_RATINGS, SORT_OPTIONS } from '@/constants'

const imageStore = useImageStore()
const { params } = storeToRefs(imageStore)

const tagText = ref('')

// 快速筛选选项
const quickFilters = [
  { label: '最新', rating: 'all', sort: 'time', tags: [] },
  { label: '随机', rating: 'all', sort: 'random', tags: [] },
  { label: '大图', rating: 'all', sort: 'size', tags: [] },
  { label: '一般', rating: 'general', sort: 'random', tags: [] },
]

const hasActiveFilters = computed(() => {
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
const handleQuery = async () => {
  try {
    await imageStore.query()
    ElMessage.success('筛选应用成功')
  } catch (error) {
    console.error('查询失败:', error)
    ElMessage.error('查询失败，请重试')
  }
}

const handleReset = () => {
  params.value.rating = 'all'
  params.value.sort = 'random'
  params.value.tags = []
  params.value.page = 1
  tagText.value = ''
  ElMessage.info('筛选条件已重置')
}

const handleAddTag = () => {
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
  handleAddTag()
}

const handleRemoveTag = (tag: string) => {
  params.value.tags = params.value.tags.filter(t => t !== tag)
  ElMessage.info(`已移除标签: ${tag}`)
}

const applyQuickFilter = (filter: any) => {
  params.value.rating = filter.rating
  params.value.sort = filter.sort
  params.value.tags = [...filter.tags]
  params.value.page = 1
  ElMessage.success(`已应用筛选: ${filter.label}`)
  handleQuery()
}

const isQuickFilterActive = (filter: any) => {
  return params.value.rating === filter.rating &&
         params.value.sort === filter.sort &&
         JSON.stringify(params.value.tags) === JSON.stringify(filter.tags)
}
</script>

<style scoped>
.filter-panel {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xl);
}

/* 筛选区块 */
.filter-section {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.section-header {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.section-title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--dark-text-primary);
}

.section-description {
  font-size: 12px;
  color: var(--dark-text-tertiary);
  line-height: 1.4;
}

.section-content {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

/* 评级选择器 */
.rating-selector {
  display: flex;
  border-radius: var(--radius-md);
  overflow: hidden;
  padding: 0;
}

.rating-option {
  flex: 1;
  padding: var(--spacing-sm) var(--spacing-md);
  background: transparent;
  border: none;
  color: var(--dark-text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: all var(--transition-fast);
  position: relative;
}

.rating-option:not(:last-child)::after {
  content: '';
  position: absolute;
  right: 0;
  top: 20%;
  bottom: 20%;
  width: 1px;
  background: var(--dark-border-secondary);
}

.rating-option:hover {
  background: var(--dark-surface-hover);
  color: var(--dark-text-primary);
}

.rating-option.active {
  background: var(--dark-primary);
  color: white;
}

.rating-option.active::after {
  display: none;
}

/* 排序选择器 */
.sort-selector {
  width: 100%;
}

/* 标签输入 */
.tag-input-container {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.tag-input {
  width: 100%;
}

.add-tag-button {
  padding: 0;
  margin-right: var(--spacing-sm);
}

/* 已选标签 */
.selected-tags {
  margin-top: var(--spacing-sm);
}

.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-sm);
}

.tag-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  padding: var(--spacing-xs) var(--spacing-sm);
  border-radius: var(--radius-sm);
  font-size: 12px;
  transition: all var(--transition-fast);
}

.tag-item:hover {
  transform: translateY(-1px);
  box-shadow: var(--dark-shadow-light);
}

.tag-text {
  color: var(--dark-text-primary);
  font-weight: 500;
}

.tag-remove {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  border: none;
  background: var(--dark-border-secondary);
  color: var(--dark-text-tertiary);
  border-radius: 50%;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.tag-remove:hover {
  background: var(--dark-error);
  color: white;
}

/* 快速筛选 */
.quick-filters {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--spacing-sm);
}

.quick-filter-btn {
  padding: var(--spacing-sm) var(--spacing-md);
  background: var(--dark-surface-secondary);
  border: 1px solid var(--dark-border-secondary);
  color: var(--dark-text-secondary);
  border-radius: var(--radius-sm);
  font-size: 12px;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.quick-filter-btn:hover {
  background: var(--dark-surface-hover);
  border-color: var(--dark-border-accent);
  color: var(--dark-text-primary);
}

.quick-filter-btn.active {
  background: var(--dark-primary);
  border-color: var(--dark-primary);
  color: white;
}

/* 操作按钮 */
.filter-actions {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
  margin-top: var(--spacing-lg);
  padding-top: var(--spacing-lg);
  border-top: 1px solid var(--dark-border-secondary);
}

.action-button {
  width: 100%;
}

.query-button {
  order: 1;
}

.reset-button {
  order: 2;
}

/* 动画 */
.tag-enter-active,
.tag-leave-active {
  transition: all var(--transition-base);
}

.tag-enter-from,
.tag-leave-to {
  opacity: 0;
  transform: scale(0.8);
}

.tag-move {
  transition: transform var(--transition-base);
}

/* Element Plus 组件样式覆盖 */
:deep(.el-select) {
  width: 100%;
}

:deep(.el-input__wrapper) {
  background-color: var(--dark-surface-secondary);
  border-color: var(--dark-border-secondary);
}

:deep(.el-input__wrapper:hover) {
  border-color: var(--dark-border-accent);
}

:deep(.el-input__wrapper.is-focus) {
  border-color: var(--dark-primary);
}

:deep(.el-autocomplete-suggestion) {
  background: var(--dark-bg-elevated);
  border: 1px solid var(--dark-border-secondary);
}
</style>
