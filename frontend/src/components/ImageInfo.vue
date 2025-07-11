<template>
  <div class="info-panel">
    <!-- 标题 -->
    <el-text class="title">{{ image.filename }}</el-text>

    <!-- 信息块 -->
    <div class="info-block">
      <el-text class="label">类型</el-text>
      <el-text class="value">{{ image.mimetype }}</el-text>
    </div>

    <div class="info-block">
      <el-text class="label">文件大小</el-text>
      <el-text class="value">{{ formatSize(image.size) }}</el-text>
    </div>

    <div class="info-block">
      <el-text class="label">分辨率</el-text>
      <el-text class="value">{{ image.width }} x {{ image.height }}</el-text>
    </div>

    <div class="info-block">
      <el-text class="label">评分</el-text>
      <el-text class="value">{{ image.rating }}</el-text>
    </div>

    <div class="info-block">
      <el-text class="label">Hash</el-text>
      <el-text class="value truncate">{{ image.hash }}</el-text>
    </div>

    <!-- 标签区 -->
    <div class="tags-wrapper" v-if="image.tags.length">
      <span class="label">标签</span>
      <div class="tags">
        <el-tag
            v-for="tag in image.tags"
            :key="tag"
            type="info"
            size="small"
            effect="light"
        >
          {{ tag }}
        </el-tag>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Image } from "../api"

defineProps<{
  image: Image
}>()

function formatSize(size: number) {
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}
</script>

<style scoped>
.info-panel {
  padding: 24px;
  display: flex;
  flex-direction: column;
  height: 100%;
  color: var(--el-text-color-primary);
  box-sizing: border-box;
  overflow-y: auto;
}

.title {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 12px;
  max-width: 100%;
  text-overflow: ellipsis;
}

.info-block {
  display: flex;
  justify-content: space-between;
  border-bottom: 1px solid var(--el-border-color-light);
  padding: 8px 0;
}

.label {
  color: var(--el-text-color-secondary);
  font-weight: 500;
}

.value {
  color: var(--el-text-color-primary);
  word-break: break-all;
}

.value.truncate {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 220px;
  display: inline-block;
}

.tags-wrapper {
  margin-top: 16px;
  margin-bottom: 16px;
  background: var(--el-bg-color-overlay);
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  padding: 12px;
  display: flex;
  flex-direction: column;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  width: 100%;
}

.tags .el-tag {
  font-size: 14px;
  padding: 0 8px;
  height: 24px;
  line-height: 22px;
  max-width: 100%;
  box-sizing: border-box;
  white-space: nowrap;
  text-overflow: ellipsis;
  overflow: hidden;
}
</style>
