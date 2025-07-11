<template>
  <el-card
      shadow="hover"
      class="image-card"
      body-style="padding: 0px;height: 85%"
      footer-class="footer"
      @click="$emit('click')"
  >
    <el-image
        :src="url"
        :alt="title"
        fit="cover"
        :preview-teleported="true"
        style="width: 100%; height: 100%"
    >
    </el-image>

    <div class="footer-content">
      <el-text truncated class="filename">
        {{ title }}
      </el-text>
      <el-checkbox
          class="footer-checkbox"
          :model-value="checked"
          @change="(val:boolean) => $emit('check', val)"
          @click.stop
      />
    </div>

  </el-card>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  url: string
  checked?: boolean
}>()

defineEmits(['click', 'check'])
</script>

<style scoped>
.image-card {
  margin: 3px;
}

.footer-content {
  display: flex;;
  padding: 0 15px;
}

.filename {
  font-size: 15px;
  color: var(--el-text-color-primary);
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.footer-checkbox {
  margin-left: 8px;
}


</style>

