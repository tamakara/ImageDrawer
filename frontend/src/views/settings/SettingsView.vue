<script setup lang="ts">
import { NTabs, NTabPane, NForm, NFormItem, NInput, NButton, NInputNumber, NSwitch } from 'naive-ui'
</script>

<template>
  <div class="h-full flex flex-col">
    <h1 class="text-2xl font-bold mb-4">设置</h1>
    <n-tabs type="line" animated>
      <n-tab-pane name="general" tab="常规设置">
        <n-form label-placement="left" label-width="auto" class="max-w-xl">
          <n-form-item label="深色模式">
            <n-switch />
          </n-form-item>
        </n-form>
      </n-tab-pane>
      <n-tab-pane name="tagger" tab="Tagger 服务">
        <n-form label-placement="left" label-width="auto" class="max-w-xl">
          <n-form-item label="服务器地址">
            <n-input placeholder="http://localhost:5000" />
          </n-form-item>
          <n-form-item>
            <n-button type="primary">保存</n-button>
          </n-form-item>
        </n-form>
      </n-tab-pane>
      <n-tab-pane name="backup" tab="备份与还原">
        <div class="space-y-4">
          <div>
            <h3 class="text-lg font-medium mb-2">备份</h3>
            <n-button>导出数据</n-button>
          </div>
          <div>
            <h3 class="text-lg font-medium mb-2">还原</h3>
            <n-button type="warning">导入备份</n-button>
          </div>
        </div>
      </n-tab-pane>
    </n-tabs>
  </div>
</template>

