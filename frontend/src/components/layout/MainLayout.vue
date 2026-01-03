<script setup lang="ts">
import { h } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { NLayout, NLayoutHeader, NLayoutContent, NMenu, NIcon, NDropdown, NButton } from 'naive-ui'
import type { MenuOption } from 'naive-ui'
import {
  Image24Regular as GalleryIcon,
  ArrowUpload24Regular as UploadIcon,
  Settings24Regular as SettingsIcon,
  Navigation24Regular as MenuIcon
} from '@vicons/fluent'

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
  <n-layout class="h-screen">
    <n-layout-header bordered class="h-16 flex items-center px-4 justify-between">
      <div class="flex items-center shrink-0 mr-4">
        <span class="text-xl font-bold">ImageDrawer</span>
      </div>
      <div class="hidden sm:flex flex-1 min-w-0 justify-end">
        <n-menu
          mode="horizontal"
          :options="menuOptions"
          :value="String(route.name)"
          responsive
        />
      </div>
      <div class="sm:hidden">
        <n-dropdown trigger="click" :options="menuOptions">
          <n-button text style="font-size: 24px">
            <n-icon>
              <MenuIcon />
            </n-icon>
          </n-button>
        </n-dropdown>
      </div>
    </n-layout-header>
    <n-layout-content class="bg-gray-50 dark:bg-gray-900" style="height: calc(100vh - 64px)">
      <div class="h-full overflow-auto p-2">
        <router-view />
      </div>
    </n-layout-content>
  </n-layout>
</template>

