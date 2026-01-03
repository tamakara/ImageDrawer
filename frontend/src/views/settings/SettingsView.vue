<script setup lang="ts">
import {ref, h} from 'vue'
import {useQuery, useMutation, useQueryClient} from '@tanstack/vue-query'
import {taggerApi, type TaggerServerConfig} from '../../api/tagger'
import {systemApi} from '../../api/system'
import {NCard, NButton, NInput, NForm, NFormItem, NDataTable, NSpace, NModal, useMessage, NUpload, NPopconfirm, NSelect} from 'naive-ui'

const message = useMessage()
const queryClient = useQueryClient()

// --- Tagger 服务器 ---
const {data: servers} = useQuery({
  queryKey: ['taggerServers'],
  queryFn: taggerApi.listServers
})

const showAddServer = ref(false)
const newServer = ref<Omit<TaggerServerConfig, 'id'>>({name: '', url: '', active: true})

const addServerMutation = useMutation({
  mutationFn: taggerApi.addServer,
  onSuccess: () => {
    queryClient.invalidateQueries({queryKey: ['taggerServers']})
    showAddServer.value = false
    newServer.value = {name: '', url: '', active: true}
    message.success('服务器已添加')
  }
})

function handleAddServer() {
  addServerMutation.mutate(newServer.value as any)
}

const deleteServerMutation = useMutation({
  mutationFn: taggerApi.deleteServer,
  onSuccess: () => {
    queryClient.invalidateQueries({queryKey: ['taggerServers']})
    message.success('服务器已删除')
  }
})

const serverColumns = [
  {title: '名称', key: 'name', ellipsis: { tooltip: true }},
  {title: '地址', key: 'url', ellipsis: { tooltip: true }},
  {
    title: '操作',
    key: 'actions',
    render: (row: TaggerServerConfig) => h(NButton, {
      type: 'error',
      size: 'small',
      onClick: () => deleteServerMutation.mutate(row.id)
    }, {default: () => '删除'})
  }
]

// --- 系统设置 ---
const {data: settings} = useQuery({
  queryKey: ['settings'],
  queryFn: systemApi.getSettings,
})

const settingsForm = ref<Record<string, string>>({
  'upload.max-file-size': '',
  'upload.allowed-extensions': '',
  'thumbnail.quality': '',
  'thumbnail.max-size': '800'
})

const thumbnailSizeOptions = [
  { label: '500x500', value: '500' },
  { label: '800x800', value: '800' },
  { label: '1000x1000', value: '1000' },
  { label: '1500x1500', value: '1500' },
  { label: '2000x2000', value: '2000' }
]

// 监听数据加载
import {watch} from 'vue'

watch(settings, (newVal) => {
  if (newVal) {
    const form = {...settingsForm.value, ...newVal}
    if (form['upload.max-file-size']) {
      const bytes = parseInt(form['upload.max-file-size'])
      if (!isNaN(bytes)) {
        form['upload.max-file-size'] = (bytes / (1024 * 1024)).toString()
      }
    }
    settingsForm.value = form
  }
}, { immediate: true })

const updateSettingsMutation = useMutation({
  mutationFn: systemApi.updateSettings,
  onSuccess: () => {
    queryClient.invalidateQueries({queryKey: ['settings']})
    message.success('设置已更新')
  }
})

function handleSaveSettings() {
  const form = {...settingsForm.value}
  if (form['upload.max-file-size']) {
    const mb = parseFloat(form['upload.max-file-size'])
    if (!isNaN(mb)) {
      form['upload.max-file-size'] = Math.floor(mb * 1024 * 1024).toString()
    }
  }
  updateSettingsMutation.mutate(form)
}

const clearCacheMutation = useMutation({
  mutationFn: systemApi.clearCache,
  onSuccess: () => {
    message.success('缓存已清空')
  },
  onError: () => {
    message.error('清空缓存失败')
  }
})

// --- 备份 ---
const restoreMutation = useMutation({
  mutationFn: systemApi.restoreBackup,
  onSuccess: () => {
    message.success('还原完成，请刷新页面。')
  },
  onError: () => {
    message.error('还原失败')
  }
})

</script>

<template>
  <div class="flex flex-col gap-4 h-full">

    <!-- Tagger Servers -->
    <n-card title="Tagger 服务器">
      <template #header-extra>
        <n-button type="primary" @click="showAddServer = true">添加服务器</n-button>
      </template>
      <n-data-table :columns="serverColumns" :data="servers || []"/>
    </n-card>

    <!-- System Settings -->
    <n-card title="系统设置">
      <n-form>
        <n-form-item label="最大上传大小 (MB)">
          <n-input v-model:value="settingsForm['upload.max-file-size']"/>
        </n-form-item>
        <n-form-item label="允许的扩展名 (逗号分隔)">
          <n-input v-model:value="settingsForm['upload.allowed-extensions']"/>
        </n-form-item>
        <n-form-item label="缩略图质量 (1-100)">
          <n-input v-model:value="settingsForm['thumbnail.quality']" placeholder="80"/>
        </n-form-item>
        <n-form-item label="缩略图最大分辨率">
          <n-select v-model:value="settingsForm['thumbnail.max-size']" :options="thumbnailSizeOptions" />
        </n-form-item>
        <n-space>
          <n-button type="primary" @click="handleSaveSettings">保存设置</n-button>
          <n-popconfirm @positive-click="clearCacheMutation.mutate()">
            <template #trigger>
              <n-button type="warning">清空缓存</n-button>
            </template>
            确定要清空临时文件缓存吗？
          </n-popconfirm>
        </n-space>
      </n-form>
    </n-card>

    <!-- Backup & Restore -->
    <n-card title="备份与还原">
      <n-space>
        <n-button @click="systemApi.downloadBackup">下载备份</n-button>
        <n-upload
            :custom-request="({ file }) => restoreMutation.mutate(file.file as File)"
            :show-file-list="false"
        >
          <n-button type="warning">还原备份</n-button>
        </n-upload>
      </n-space>
    </n-card>

    <!-- Add Server Modal -->
    <n-modal v-model:show="showAddServer" preset="dialog" title="添加 Tagger 服务器">
      <n-form class="mt-4">
        <n-form-item label="名称">
          <n-input v-model:value="newServer.name"/>
        </n-form-item>
        <n-form-item label="地址">
          <n-input v-model:value="newServer.url" placeholder="http://localhost:5000/tag"/>
        </n-form-item>
      </n-form>
      <template #action>
        <n-button @click="showAddServer = false">取消</n-button>
        <n-button type="primary" @click="handleAddServer">添加</n-button>
      </template>
    </n-modal>

  </div>
</template>

