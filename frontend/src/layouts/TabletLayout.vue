<template>
  <div class="tablet-layout">
    <!-- 顶部导航栏 -->
    <header class="tablet-header border-secondary bg-elevated shadow-light">
      <div class="header-content">
        <div class="header-left">
          <h1 class="app-title">图片库</h1>
          <div class="breadcrumb-stats">
            <span class="stats-item">共 {{ imageStore.totalElements }} 张</span>
            <span class="stats-separator">|</span>
            <span class="stats-item">第 {{ imageStore.params.page }} ���</span>
          </div>
        </div>

        <div class="header-center">
          <div class="quick-actions">
            <el-button-group>
              <el-tooltip content="刷新">
                <el-button :icon="Refresh" @click="handleRefresh" />
              </el-tooltip>
              <el-tooltip content="网格视图">
                <el-button :icon="Grid" :type="viewMode === 'grid' ? 'primary' : 'default'" @click="setViewMode('grid')" />
              </el-tooltip>
              <el-tooltip content="列表视图">
                <el-button :icon="List" :type="viewMode === 'list' ? 'primary' : 'default'" @click="setViewMode('list')" />
              </el-tooltip>
            </el-button-group>
          </div>
        </div>

        <div class="header-right">
          <el-button-group class="panel-toggles">
            <el-tooltip content="筛选面板">
              <el-button
                :icon="Filter"
                :type="showFilterPanel ? 'primary' : 'default'"
                @click="toggleFilterPanel"
              />
            </el-tooltip>
            <el-tooltip content="上传面板">
              <el-button
                :icon="Upload"
                :type="showUploadPanel ? 'primary' : 'default'"
                @click="toggleUploadPanel"
              />
            </el-tooltip>
          </el-button-group>
        </div>
      </div>
    </header>

    <!-- 主内容区域 -->
    <main class="tablet-main">
      <!-- 左侧筛选面板 -->
      <aside
        class="side-panel left-panel border-accent bg-elevated"
        :class="{ 'panel-visible': showFilterPanel }"
      >
        <div class="panel-header">
          <h3 class="panel-title">图片筛选</h3>
          <el-button
            text
            :icon="Close"
            @click="showFilterPanel = false"
            class="panel-close"
          />
        </div>
        <div class="panel-content">
          <FilterPanel />
        </div>
      </aside>

      <!-- 中央图片区域 -->
      <section class="content-area" :class="contentAreaClass">
        <ImageGallery :view-mode="viewMode" />
      </section>

      <!-- 右侧上传面板 -->
      <aside
        class="side-panel right-panel border-accent bg-elevated"
        :class="{ 'panel-visible': showUploadPanel }"
      >
        <div class="panel-header">
          <h3 class="panel-title">上传管理</h3>
          <el-button
            text
            :icon="Close"
            @click="showUploadPanel = false"
            class="panel-close"
          />
        </div>
        <div class="panel-content">
          <UploadManager />
        </div>
      </aside>
    </main>

    <!-- 批量操作工具栏 -->
    <div class="batch-toolbar" v-if="selectedCount > 0">
      <div class="toolbar-content border-secondary bg-surface shadow-medium">
        <div class="selection-info">
          <el-checkbox
            v-model="isAllSelected"
            :indeterminate="isIndeterminate"
            @change="handleSelectAll"
          >
            已选择 {{ selectedCount }} 张图片
          </el-checkbox>
        </div>

        <div class="batch-actions">
          <el-button-group>
            <el-button type="primary" :icon="Download" @click="handleBatchDownload">
              批量下载
            </el-button>
            <el-button type="danger" :icon="Delete" @click="handleBatchDelete">
              批量删除
            </el-button>
            <el-button :icon="Close" @click="clearSelection">
              取消选择
            </el-button>
          </el-button-group>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import {
  Refresh,
  Grid,
  Menu as List, // 替换 List
  Filter,
  Upload,
  Close,
  Download,
  Delete
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useImageStore } from '@/stores/image'
import FilterPanel from '@/components/panels/FilterPanel.vue'
import UploadManager from '@/components/panels/UploadManager.vue'
import ImageGallery from '@/components/gallery/ImageGallery.vue'

const imageStore = useImageStore()
const showFilterPanel = ref(true)  // 平板端默认显示筛选面板
const showUploadPanel = ref(false)
const viewMode = ref<'grid' | 'list'>('grid')

// 临时数据，实际应该从ImageGallery组件获取
const selectedCount = ref(0)
const isAllSelected = ref(false)
const isIndeterminate = ref(false)

// 计算属性
const contentAreaClass = computed(() => ({
  'filter-visible': showFilterPanel.value,
  'upload-visible': showUploadPanel.value,
  'both-visible': showFilterPanel.value && showUploadPanel.value
}))

// 事件处理
const handleRefresh = async () => {
  try {
    await imageStore.query()
    ElMessage.success('刷新完成')
  } catch (error) {
    ElMessage.error('刷新失败')
  }
}

const setViewMode = (mode: 'grid' | 'list') => {
  viewMode.value = mode
}

const toggleFilterPanel = () => {
  showFilterPanel.value = !showFilterPanel.value
  // 如果两个面板都显示，关闭上传面板
  if (showFilterPanel.value && showUploadPanel.value) {
    showUploadPanel.value = false
  }
}

const toggleUploadPanel = () => {
  showUploadPanel.value = !showUploadPanel.value
  // 如果两个面板都显示，关闭筛选面板
  if (showFilterPanel.value && showUploadPanel.value) {
    showFilterPanel.value = false
  }
}

const handleSelectAll = (selected: boolean) => {
  isAllSelected.value = selected
  // 实现全选逻辑
}

const handleBatchDownload = () => {
  ElMessage.info('批量下载功能')
}

const handleBatchDelete = () => {
  ElMessageBox.confirm(
    `确定要删除选中的 ${selectedCount.value} 张图片吗？`,
    '批量删除确认',
    {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(() => {
    ElMessage.success('删��成功')
  }).catch(() => {
    ElMessage.info('已取消')
  })
}

const clearSelection = () => {
  selectedCount.value = 0
  isAllSelected.value = false
  isIndeterminate.value = false
}
</script>

<style scoped>
.tablet-layout {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--dark-bg-primary);
}

/* 头部导航 */
.tablet-header {
  flex-shrink: 0;
  background: var(--dark-surface-primary);
  border-bottom: 2px solid var(--dark-border-accent);
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--spacing-md) var(--spacing-lg);
  height: 72px;
}

.header-left {
  flex: 1;
  min-width: 0;
}

.app-title {
  margin: 0 0 var(--spacing-xs) 0;
  font-size: 22px;
  font-weight: 700;
  color: var(--dark-text-primary);
}

.breadcrumb-stats {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  font-size: 13px;
  color: var(--dark-text-tertiary);
}

.stats-separator {
  color: var(--dark-border-secondary);
}

.header-center {
  flex: 0 0 auto;
  margin: 0 var(--spacing-lg);
}

.header-right {
  flex: 0 0 auto;
}

/* 主内容区 */
.tablet-main {
  flex: 1;
  display: flex;
  overflow: hidden;
}

/* 侧边面板 */
.side-panel {
  width: 0;
  background: var(--dark-bg-elevated);
  border-radius: var(--radius-lg);
  margin: var(--spacing-sm);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: all var(--transition-base);
  opacity: 0;
  transform: translateX(-100%);
}

.side-panel.panel-visible {
  width: 320px;
  opacity: 1;
  transform: translateX(0);
}

.right-panel.panel-visible {
  transform: translateX(0);
}

.right-panel {
  transform: translateX(100%);
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--spacing-lg);
  background: var(--dark-surface-primary);
  border-bottom: 1px solid var(--dark-border-secondary);
}

.panel-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--dark-text-primary);
}

.panel-close {
  color: var(--dark-text-tertiary);
}

.panel-content {
  flex: 1;
  padding: var(--spacing-lg);
  overflow-y: auto;
}

/* 内容区域 */
.content-area {
  flex: 1;
  margin: var(--spacing-sm);
  background: var(--dark-bg-secondary);
  border-radius: var(--radius-lg);
  border: 1px solid var(--dark-border-primary);
  overflow: hidden;
  transition: all var(--transition-base);
}

.content-area.filter-visible {
  margin-left: 0;
}

.content-area.upload-visible {
  margin-right: 0;
}

.content-area.both-visible {
  margin-left: 0;
  margin-right: 0;
}

/* 批量操作工具栏 */
.batch-toolbar {
  position: fixed;
  bottom: var(--spacing-lg);
  left: 50%;
  transform: translateX(-50%);
  z-index: 1000;
  max-width: 90%;
}

.toolbar-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--spacing-md) var(--spacing-lg);
  background: var(--dark-bg-elevated);
  border-radius: var(--radius-xl);
  backdrop-filter: blur(12px);
  min-width: 480px;
}

.selection-info {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.batch-actions {
  display: flex;
  gap: var(--spacing-sm);
}

/* 响应式调整 */
@media (max-width: 1024px) {
  .side-panel.panel-visible {
    width: 280px;
  }

  .header-content {
    padding: var(--spacing-sm) var(--spacing-md);
  }

  .app-title {
    font-size: 20px;
  }

  .toolbar-content {
    min-width: 400px;
    flex-direction: column;
    gap: var(--spacing-md);
  }
}

@media (max-width: 900px) {
  .header-center {
    display: none;
  }

  .side-panel.panel-visible {
    width: 260px;
  }

  .toolbar-content {
    min-width: 320px;
  }
}

/* 滚动条样式 */
.panel-content::-webkit-scrollbar {
  width: 6px;
}

.panel-content::-webkit-scrollbar-track {
  background: var(--dark-bg-secondary);
}

.panel-content::-webkit-scrollbar-thumb {
  background: var(--dark-border-primary);
  border-radius: var(--radius-sm);
}

/* Element Plus 组件样式覆盖 */
:deep(.el-button-group .el-button) {
  background: var(--dark-surface-secondary);
  border-color: var(--dark-border-secondary);
  color: var(--dark-text-secondary);
}

:deep(.el-button-group .el-button:hover) {
  background: var(--dark-surface-hover);
  border-color: var(--dark-border-accent);
  color: var(--dark-text-primary);
}

:deep(.el-button-group .el-button.is-active) {
  background: var(--dark-primary);
  border-color: var(--dark-primary);
  color: white;
}

:deep(.el-checkbox__label) {
  color: var(--dark-text-primary);
  font-weight: 500;
}
</style>
