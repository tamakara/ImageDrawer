<template>
  <el-image
      :src="url"
      fit="contain"
      preview-teleported
      class="image-preview"
      :preview-src-list="[url]"
      :z-index="1001"
  />
</template>

<script setup lang="ts">
defineProps<{
  url: string
}>()
</script>

<style scoped>
.image-preview {
  height: 100%;
  object-fit: contain;
  border-radius: 4px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.3);
}
</style>
