<template>
  <div class="image-card" :class="{ selected: isSelected }">
    <!-- 选择复选框 -->
    <div class="selection-overlay">
      <el-checkbox
        :model-value="isSelected"
        @change="handleSelection"
        size="large"
      />
    </div>

    <img
      :src="thumbnailUrl"
      :alt="image.filename"
      @click="$emit('preview', image)"
      loading="lazy"
    />

    <div class="image-info">
      <div class="image-title" :title="image.filename">{{ image.filename }}</div>
      <div class="image-meta">
        <span>{{ formatSize(image.size) }}</span>
        <span>{{ image.width }}×{{ image.height }}</span>
        <span class="rating" :class="`rating-${image.rating}`">{{ getRatingText(image.rating) }}</span>
      </div>
      <div class="image-tags" v-if="image.tags.length">
        <span class="tag" v-for="tag in image.tags.slice(0, 3)" :key="tag">
          {{ tag }}
        </span>
        <span class="tag" v-if="image.tags.length > 3">
          +{{ image.tags.length - 3 }}
        </span>
      </div>
      <div class="image-actions">
        <el-button
          size="small"
          type="primary"
          @click="downloadSingle"
          :icon="Download"
          title="下载"
        />
        <el-button
          size="small"
          type="danger"
          @click="$emit('delete', image.hash)"
          :icon="Delete"
          title="删除"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Delete, Download } from '@element-plus/icons-vue'
import { imageAPI } from './api'
import { useImageStore } from './store'
import type { Image } from './types'

interface Props {
  image: Image
}

const props = defineProps<Props>()
const imageStore = useImageStore()

defineEmits<{
  preview: [image: Image]
  delete: [hash: string]
}>()

const thumbnailUrl = computed(() => imageAPI.getThumbnailUrl(props.image.hash))
const isSelected = computed(() => imageStore.isImageSelected(props.image.hash))

const handleSelection = () => {
  imageStore.toggleImageSelection(props.image.hash)
}

const downloadSingle = async () => {
  try {
    const blob = await imageAPI.downloadImage(props.image.hash)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = props.image.filename
    a.click()
    URL.revokeObjectURL(url)
  } catch (error) {
    console.error('下载失败:', error)
  }
}

const formatSize = (bytes: number) => {
  const sizes = ['B', 'KB', 'MB', 'GB']
  if (bytes === 0) return '0 B'
  const i = Math.floor(Math.log(bytes) / Math.log(1024))
  return Math.round(bytes / Math.pow(1024, i) * 100) / 100 + ' ' + sizes[i]
}

const getRatingText = (rating: string) => {
  const ratingMap: Record<string, string> = {
    'general': '一般',
    'sensitive': '敏感',
    'explicit': '明确',
    'all': '全部'
  }
  return ratingMap[rating] || rating
}
</script>

<style scoped>
.image-card {
  position: relative;
  background: #18181b;
  border-radius: 8px;
  overflow: hidden;
  transition: all 0.2s;
  border: 1px solid #27272a;
}

.image-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.3);
}

.image-card.selected {
  border-color: #3b82f6;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.3);
}

.selection-overlay {
  position: absolute;
  top: 8px;
  left: 8px;
  z-index: 2;
  background: rgba(0, 0, 0, 0.7);
  border-radius: 4px;
  padding: 4px;
}

.image-card img {
  width: 100%;
  height: 200px;
  object-fit: cover;
  cursor: pointer;
}

.image-info {
  padding: 12px;
}

.image-title {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.image-meta {
  display: flex;
  gap: 8px;
  font-size: 12px;
  color: #a1a1aa;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.rating {
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: 500;
}

.rating-general {
  background: #22c55e;
  color: white;
}

.rating-sensitive {
  background: #f59e0b;
  color: white;
}

.rating-explicit {
  background: #ef4444;
  color: white;
}

.image-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 8px;
}

.tag {
  background: #3f3f46;
  color: #d4d4d8;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
}

.image-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
