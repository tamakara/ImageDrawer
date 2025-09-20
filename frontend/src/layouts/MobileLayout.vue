<template>
  <div class="mobile-layout">
    <!-- 顶部导航栏 -->
    <header class="mobile-header border-secondary bg-elevated shadow-medium">
      <div class="header-content">
        <div class="header-left">
          <h1 class="app-title">图片库</h1>
          <div class="stats-info" v-if="imageStore.totalElements > 0">
            {{ imageStore.totalElements }} 张图片
          </div>
        </div>
        <div class="header-actions">
          <el-button
            circle
            :icon="Filter"
            @click="showFilterPanel = true"
            title="筛选"
            class="action-button"
          />
          <el-button
            circle
            :icon="Upload"
            @click="showUploadPanel = true"
            title="上传"
            class="action-button"
          />
        </div>
      </div>
    </header>

    <!-- 主内容区域 -->
    <main class="mobile-main">
      <ImageGallery />
    </main>

    <!-- 底部抽屉 - 筛选面板 -->
    <el-drawer
      v-model="showFilterPanel"
      title="图片筛选"
      direction="btt"
      size="75%"
      class="mobile-drawer"
    >
      <template #header="{ titleId, titleClass }">
        <div class="drawer-header">
          <h3 :id="titleId" :class="titleClass">图片���选</h3>
          <div class="drawer-subtitle">精确查找您需要的图片</div>
        </div>
      </template>

      <div class="drawer-content">
        <FilterPanel />
      </div>
    </el-drawer>

    <!-- 底部抽屉 - 上传面板 -->
    <el-drawer
      v-model="showUploadPanel"
      title="上传管理"
      direction="btt"
      size="75%"
      class="mobile-drawer"
    >
      <template #header="{ titleId, titleClass }">
        <div class="drawer-header">
          <h3 :id="titleId" :class="titleClass">上传管理</h3>
          <div class="drawer-subtitle">批量上传和队列管理</div>
        </div>
      </template>

      <div class="drawer-content">
        <UploadManager />
      </div>
    </el-drawer>

    <!-- 快速操作悬浮按钮 -->
    <div class="fab-container" v-if="selectedCount > 0">
      <el-button-group class="fab-group">
        <el-button
          type="primary"
          :icon="Download"
          @click="handleBatchDownload"
          class="fab-button"
        >
          下载 {{ selectedCount }}
        </el-button>
        <el-button
          type="danger"
          :icon="Delete"
          @click="handleBatchDelete"
          class="fab-button"
        />
      </el-button-group>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Filter, Upload, Download, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useImageStore } from '@/stores/image'
import FilterPanel from '@/components/panels/FilterPanel.vue'
import UploadManager from '@/components/panels/UploadManager.vue'
import ImageGallery from '@/components/gallery/ImageGallery.vue'

const imageStore = useImageStore()
const showFilterPanel = ref(false)
const showUploadPanel = ref(false)

// 这里需要从ImageGallery组件获取选中的图片数量
const selectedCount = computed(() => {
  // 临时实现，实际需要从ImageGallery组件获取
  return 0
})

const handleBatchDownload = () => {
  // 实现批量下载逻辑
  ElMessage.info('批量下载功能')
}

const handleBatchDelete = () => {
  // 实现批量删除逻辑
  ElMessageBox.confirm(
    '确定要删除选中的图片吗？',
    '确认删除',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(() => {
    ElMessage.success('删除成功')
  }).catch(() => {
    ElMessage.info('已取消')
  })
}
</script>

<style scoped>
.mobile-layout {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--dark-bg-primary);
  overflow: hidden;
}

/* 头部导航 */
.mobile-header {
  flex-shrink: 0;
  background: var(--dark-surface-primary);
  border-bottom: 2px solid var(--dark-border-accent);
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--spacing-md) var(--spacing-lg);
  min-height: 64px;
}

.header-left {
  flex: 1;
  min-width: 0;
}

.app-title {
  margin: 0 0 var(--spacing-xs) 0;
  font-size: 20px;
  font-weight: 700;
  color: var(--dark-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stats-info {
  font-size: 12px;
  color: var(--dark-text-tertiary);
}

.header-actions {
  display: flex;
  gap: var(--spacing-sm);
  margin-left: var(--spacing-md);
}

.action-button {
  width: 40px;
  height: 40px;
  background: var(--dark-surface-secondary);
  border: 1px solid var(--dark-border-secondary);
  color: var(--dark-text-secondary);
}

.action-button:hover {
  background: var(--dark-surface-hover);
  border-color: var(--dark-border-accent);
  color: var(--dark-text-primary);
}

/* 主内容区 */
.mobile-main {
  flex: 1;
  overflow: hidden;
  background: var(--dark-bg-secondary);
}

/* 抽屉样式 */
.drawer-header {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
  padding: var(--spacing-lg);
  background: var(--dark-surface-primary);
  border-bottom: 1px solid var(--dark-border-secondary);
}

.drawer-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--dark-text-primary);
}

.drawer-subtitle {
  font-size: 13px;
  color: var(--dark-text-tertiary);
}

.drawer-content {
  padding: var(--spacing-lg);
  height: 100%;
  overflow-y: auto;
}

/* 悬浮操作按钮 */
.fab-container {
  position: fixed;
  bottom: var(--spacing-xl);
  left: 50%;
  transform: translateX(-50%);
  z-index: 1000;
}

.fab-group {
  box-shadow: var(--dark-shadow-heavy);
  border-radius: var(--radius-xl);
  overflow: hidden;
  backdrop-filter: blur(10px);
}

.fab-button {
  height: 48px;
  padding: 0 var(--spacing-lg);
  font-weight: 600;
  border: none;
}

/* Element Plus 组件样式覆盖 */
:deep(.mobile-drawer) {
  --el-drawer-bg-color: var(--dark-bg-elevated);
}

:deep(.el-drawer__header) {
  padding: 0;
  margin-bottom: 0;
}

:deep(.el-drawer__body) {
  padding: 0;
}

/* 安全区域适配 */
@supports (padding-top: env(safe-area-inset-top)) {
  .mobile-header {
    padding-top: env(safe-area-inset-top);
  }
}

@supports (padding-bottom: env(safe-area-inset-bottom)) {
  .fab-container {
    bottom: calc(var(--spacing-xl) + env(safe-area-inset-bottom));
  }
}

/* 横屏适配 */
@media (orientation: landscape) and (max-height: 500px) {
  .app-title {
    font-size: 18px;
  }

  .header-content {
    min-height: 56px;
    padding: var(--spacing-sm) var(--spacing-lg);
  }
}

/* 触摸优化 */
@media (hover: none) {
  .action-button {
    background: var(--dark-surface-hover);
  }
}
</style>
