<template>
  <teleport to="body">
    <div class="image-viewer-overlay" @click="onClose">
      <div class="viewer-content" @click.stop>
        <div class="image-area">
          <ImagePreview :url="imageStore.getImageUrl(image.hash)"/>
        </div>
        <div class="info-area">
          <ImageInfo :image="image" @close="onClose"/>
        </div>
      </div>
    </div>
  </teleport>
</template>


<script setup lang="ts">
import type {Image} from "../api"
import ImagePreview from "./ImagePreview.vue"
import {useImageStore} from "../stores/image"
import ImageInfo from "./ImageInfo.vue"

defineProps<{
  image: Image
}>()

const emit = defineEmits(["close"])

const imageStore = useImageStore()

function onClose() {
  emit("close")
}
</script>

<style scoped>
.image-viewer-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(8px);
  z-index: 1000;
  display: flex;
  justify-content: center;
  align-items: center;
}

.viewer-content {
  width: 90%;
  height: 90%;
  background: var(--el-bg-color-overlay);
  box-shadow: var(--el-box-shadow);
  display: flex;
  border-radius: 8px;
  overflow: hidden;
}

.image-area {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  background: var(--el-bg-color-overlay);
  padding: 24px;
}

.info-area {
  width: 360px;
  max-width: 400px;
  background: var(--el-bg-color);
  border-left: 1px solid var(--el-border-color-light);
  overflow-y: auto;
}
</style>
