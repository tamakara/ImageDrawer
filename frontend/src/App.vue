<script setup lang="ts">
import {NConfigProvider, NGlobalStyle, NMessageProvider, NDialogProvider, darkTheme} from 'naive-ui'
import {useOsTheme} from 'naive-ui'
import {computed, onMounted, onUnmounted} from 'vue'
import MainLayout from './components/layout/MainLayout.vue'

const osTheme = useOsTheme()
const theme = computed(() => (osTheme.value === 'dark' ? darkTheme : null))

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
  <n-config-provider :theme="theme">
    <n-global-style/>
    <n-message-provider>
      <n-dialog-provider>
        <MainLayout/>
      </n-dialog-provider>
    </n-message-provider>
  </n-config-provider>
</template>

