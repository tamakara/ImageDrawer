<template>
  <div class="image-gallery-app" :class="appClasses">
    <el-config-provider>
      <component :is="currentLayout" />
    </el-config-provider>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElConfigProvider } from 'element-plus'
import { getDeviceType, DeviceType, throttle } from '@/utils'
import DesktopLayout from './layouts/DesktopLayout.vue'
import TabletLayout from './layouts/TabletLayout.vue'
import MobileLayout from './layouts/MobileLayout.vue'

const deviceType = ref<DeviceType>(getDeviceType())

const currentLayout = computed(() => {
  switch (deviceType.value) {
    case DeviceType.MOBILE:
      return MobileLayout
    case DeviceType.TABLET:
      return TabletLayout
    case DeviceType.DESKTOP:
    default:
      return DesktopLayout
  }
})

const appClasses = computed(() => ({
  [`device-${deviceType.value}`]: true,
  'dark-theme': true
}))

// 响应式处理窗口大小变���
const handleResize = throttle(() => {
  deviceType.value = getDeviceType()
}, 300)

onMounted(() => {
  window.addEventListener('resize', handleResize)
  // 应用暗黑主题到body
  document.body.classList.add('dark-theme')
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.image-gallery-app {
  width: 100%;
  min-height: 100vh;
  background: var(--dark-bg-primary);
  color: var(--dark-text-primary);
  transition: all var(--transition-base);
}

/* 设备特定样式 */
.image-gallery-app.device-mobile {
  font-size: 14px;
}

.image-gallery-app.device-tablet {
  font-size: 15px;
}

.image-gallery-app.device-desktop {
  font-size: 16px;
}

.dark-theme {
  color-scheme: dark;
}
</style>
