<script setup lang="ts">
import { NConfigProvider, NGlobalStyle, NMessageProvider, NDialogProvider } from 'naive-ui'
import { onMounted, onUnmounted } from 'vue'
import MainLayout from './components/layout/MainLayout.vue'
import { useThemeStore } from './stores/theme'

const themeStore = useThemeStore()

const preventDefaultContextMenu = (e: MouseEvent) => {
  e.preventDefault()
}

onMounted(() => {
  document.addEventListener('contextmenu', preventDefaultContextMenu)
})

onUnmounted(() => {
  document.removeEventListener('contextmenu', preventDefaultContextMenu)
})
</script>

<template>
  <n-config-provider :theme="themeStore.theme">
    <n-global-style/>
    <n-message-provider>
      <n-dialog-provider>
        <MainLayout/>
      </n-dialog-provider>
    </n-message-provider>
  </n-config-provider>
</template>

