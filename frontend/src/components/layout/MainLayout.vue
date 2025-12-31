<script setup lang="ts">
import { h, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { NLayout, NLayoutSider, NLayoutContent, NMenu, NIcon } from 'naive-ui'
import type { MenuOption } from 'naive-ui'
import {
  Image24Regular as GalleryIcon,
  ArrowUpload24Regular as UploadIcon,
  Settings24Regular as SettingsIcon
} from '@vicons/fluent'

const collapsed = ref(false)
const route = useRoute()

function renderIcon(icon: any) {
  return () => h(NIcon, null, { default: () => h(icon) })
}

const menuOptions: MenuOption[] = [
  {
    label: () => h(RouterLink, { to: '/gallery' }, { default: () => '图库' }),
    key: 'gallery',
    icon: renderIcon(GalleryIcon)
  },
  {
    label: () => h(RouterLink, { to: '/upload' }, { default: () => '上传' }),
    key: 'upload',
    icon: renderIcon(UploadIcon)
  },
  {
    label: () => h(RouterLink, { to: '/settings' }, { default: () => '设置' }),
    key: 'settings',
    icon: renderIcon(SettingsIcon)
  }
]
</script>

<template>
  <n-layout has-sider class="h-screen">
    <n-layout-sider
      bordered
      collapse-mode="width"
      :collapsed-width="64"
      :width="240"
      :collapsed="collapsed"
      show-trigger
      @collapse="collapsed = true"
      @expand="collapsed = false"
    >
      <div class="p-4 flex items-center justify-center h-16">
        <span v-if="!collapsed" class="text-xl font-bold truncate">ImageDrawer</span>
        <span v-else class="text-xl font-bold">ID</span>
      </div>
      <n-menu
        :collapsed="collapsed"
        :collapsed-width="64"
        :collapsed-icon-size="22"
        :options="menuOptions"
        :value="String(route.name)"
      />
    </n-layout-sider>
    <n-layout-content class="bg-gray-50 dark:bg-gray-900">
      <div class="p-6 h-full overflow-auto">
        <router-view />
      </div>
    </n-layout-content>
  </n-layout>
</template>

