<template>
  <el-container class="desktop-layout">
    <!-- 左侧筛选面板 -->
    <el-aside class="sidebar left-panel border-accent shadow-medium">
      <div class="panel-header">
        <h3 class="panel-title">图片筛选</h3>
        <div class="panel-subtitle">精确查找您需要的图片</div>
      </div>
      <div class="panel-content">
        <FilterPanel />
      </div>
    </el-aside>

    <!-- 主内容区域 -->
    <el-main class="main-content-area">
      <div class="content-header border-secondary bg-elevated shadow-light">
        <div class="content-info">
          <h2 class="content-title">图片库</h2>
          <div class="content-stats">
            <span class="stats-item">
              <i class="el-icon-picture"></i>
              共 {{ imageStore.totalElements }} 张图片
            </span>
            <span class="stats-item" v-if="imageStore.images.length > 0">
              <i class="el-icon-view"></i>
              当前显示 {{ imageStore.images.length }} 张
            </span>
          </div>
        </div>
        <div class="content-actions">
          <el-button-group>
            <el-button :icon="Refresh" @click="handleRefresh" title="刷新">
              刷新
            </el-button>
            <el-button :icon="Setting" @click="showSettingsDialog = true" title="设置">
              设置
            </el-button>
          </el-button-group>
        </div>
      </div>

      <div class="content-body">
        <ImageGallery />
      </div>
    </el-main>

    <!-- 右侧操作面板 -->
    <el-aside class="sidebar right-panel border-accent shadow-medium">
      <div class="panel-header">
        <h3 class="panel-title">上传管理</h3>
        <div class="panel-subtitle">批量上传和队列管理</div>
      </div>
      <div class="panel-content">
        <UploadManager />
      </div>
    </el-aside>

    <!-- 设置对话框 -->
    <SettingsDialog v-model="showSettingsDialog" />
  </el-container>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Refresh, Setting } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useImageStore } from '@/stores/image'
import FilterPanel from '@/components/panels/FilterPanel.vue'
import UploadManager from '@/components/panels/UploadManager.vue'
import ImageGallery from '@/components/gallery/ImageGallery.vue'
import SettingsDialog from '@/components/dialogs/SettingsDialog.vue'

const imageStore = useImageStore()
const showSettingsDialog = ref(false)

const handleRefresh = async () => {
  try {
    await imageStore.query()
    ElMessage.success('刷新完成')
  } catch (error) {
    ElMessage.error('刷新失败')
  }
}
</script>

<style scoped>
.desktop-layout {
  height: 100vh;
  background: var(--dark-bg-primary);
  gap: var(--spacing-sm);
  padding: var(--spacing-sm);
}

/* 侧边栏样式 */
.sidebar {
  width: 320px;
  min-width: 280px;
  max-width: 400px;
  background: var(--dark-bg-elevated);
  border-radius: var(--radius-lg);
  padding: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.left-panel {
  border-right: none;
}

.right-panel {
  border-left: none;
}

/* 面板头部 */
.panel-header {
  padding: var(--spacing-lg);
  background: var(--dark-surface-primary);
  border-bottom: 1px solid var(--dark-border-secondary);
}

.panel-title {
  margin: 0 0 var(--spacing-xs) 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--dark-text-primary);
}

.panel-subtitle {
  font-size: 12px;
  color: var(--dark-text-tertiary);
  line-height: 1.4;
}

/* 面板内容 */
.panel-content {
  flex: 1;
  padding: var(--spacing-lg);
  overflow-y: auto;
}

/* 主内容��域 */
.main-content-area {
  flex: 1;
  padding: 0;
  background: var(--dark-bg-secondary);
  border-radius: var(--radius-lg);
  border: 1px solid var(--dark-border-primary);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 内容头部 */
.content-header {
  padding: var(--spacing-lg) var(--spacing-xl);
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--dark-border-secondary);
  background: var(--dark-surface-primary);
}

.content-info {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.content-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: var(--dark-text-primary);
}

.content-stats {
  display: flex;
  gap: var(--spacing-md);
  font-size: 13px;
  color: var(--dark-text-secondary);
}

.stats-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
}

.stats-item i {
  font-size: 14px;
  color: var(--dark-primary);
}

/* 内容操作区 */
.content-actions {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

/* 内容主体 */
.content-body {
  flex: 1;
  overflow: visible; /* 改为 visible，让子组件自己处理滚动 */
  background: var(--dark-bg-secondary);
  min-height: 0; /* 确保 flex 子项可以收缩 */
}

/* 按钮组优化 */
:deep(.el-button-group) {
  box-shadow: var(--dark-shadow-light);
}

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

/* 响应式调整 */
@media (max-width: 1200px) {
  .sidebar {
    width: 280px;
  }

  .panel-header,
  .panel-content {
    padding: var(--spacing-md);
  }

  .content-header {
    padding: var(--spacing-md) var(--spacing-lg);
  }
}

@media (max-width: 1024px) {
  .desktop-layout {
    gap: var(--spacing-xs);
    padding: var(--spacing-xs);
  }

  .sidebar {
    width: 260px;
  }

  .content-title {
    font-size: 18px;
  }
}

/* 滚动条优化 */
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

.panel-content::-webkit-scrollbar-thumb:hover {
  background: var(--dark-border-secondary);
}
</style>
