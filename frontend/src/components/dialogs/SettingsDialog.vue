<template>
  <el-dialog
    v-model="visible"
    title="应用设置"
    width="500px"
    :before-close="handleClose"
    class="settings-dialog"
  >
    <el-form
      ref="formRef"
      :model="settingsForm"
      :rules="formRules"
      label-position="top"
      class="settings-form"
    >
      <!-- 标签服务器配置 -->
      <div class="settings-section">
        <div class="section-header">
          <h4 class="section-title">标签服务器配置</h4>
          <div class="section-description">配置AI标签识别服务器地址</div>
        </div>

        <el-form-item
          label="标签服务器地址"
          prop="taggerUrl"
          class="form-item"
        >
          <el-input
            v-model="settingsForm.taggerUrl"
            placeholder="http://localhost:5000"
            :prefix-icon="Link"
            clearable
          >
            <template #append>
              <el-button
                :icon="Connection"
                @click="testConnection"
                :loading="testing"
                title="测试连接"
              >
                测试
              </el-button>
            </template>
          </el-input>
          <div class="form-help-text">
            用于自动识别图片标签的AI服务地址，留空则禁用自动标签功能
          </div>
        </el-form-item>
      </div>

      <!-- 显示设置 -->
      <div class="settings-section">
        <div class="section-header">
          <h4 class="section-title">显示设置</h4>
          <div class="section-description">自定义界面显示选项</div>
        </div>

        <el-form-item label="默认网格大小" class="form-item">
          <el-slider
            v-model="settingsForm.defaultGridSize"
            :min="2"
            :max="8"
            :step="1"
            show-input
            :show-input-controls="false"
            style="width: 100%;"
          />
          <div class="form-help-text">
            设置默认的图片网格列数，范围2-8列
          </div>
        </el-form-item>

        <el-form-item label="默认分页大小" class="form-item">
          <el-select v-model="settingsForm.defaultPageSize" style="width: 100%;">
            <el-option label="20 张/页" :value="20" />
            <el-option label="50 张/页" :value="50" />
            <el-option label="100 张/页" :value="100" />
            <el-option label="200 张/页" :value="200" />
          </el-select>
          <div class="form-help-text">
            设置每页显示的图片数量
          </div>
        </el-form-item>
      </div>

      <!-- 缓存设置 -->
      <div class="settings-section">
        <div class="section-header">
          <h4 class="section-title">缓存设置</h4>
          <div class="section-description">管理本地缓存和存储</div>
        </div>

        <el-form-item class="form-item">
          <div class="cache-actions">
            <el-button @click="clearThumbnailCache" :icon="Delete">
              清除缩略图缓存
            </el-button>
            <el-button @click="clearAllCache" :icon="Refresh">
              清除所有缓存
            </el-button>
          </div>
          <div class="form-help-text">
            清除缓存可以释放存储空间，但会导致图片重新加载
          </div>
        </el-form-item>
      </div>

      <!-- 关于信息 -->
      <div class="settings-section">
        <div class="section-header">
          <h4 class="section-title">关于</h4>
        </div>

        <div class="about-info">
          <div class="info-row">
            <span class="info-label">应用版本：</span>
            <span class="info-value">v1.0.0</span>
          </div>
          <div class="info-row">
            <span class="info-label">构建时间：</span>
            <span class="info-value">{{ buildTime }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">技术栈：</span>
            <span class="info-value">Vue 3 + Element Plus + Spring Boot</span>
          </div>
        </div>
      </div>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleReset">重置</el-button>
        <el-button @click="handleClose">取消</el-button>
        <el-button
          type="primary"
          @click="handleSave"
          :loading="saving"
        >
          保存设置
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Link, Connection, Delete, Refresh } from '@element-plus/icons-vue'
import { setTaggerUrl } from '@/api'

interface Props {
  modelValue: boolean
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const formRef = ref<FormInstance>()
const testing = ref(false)
const saving = ref(false)

// 设置表单数据
const settingsForm = reactive({
  taggerUrl: '',
  defaultGridSize: 4,
  defaultPageSize: 100
})

// 表单验证规则
const formRules: FormRules = {
  taggerUrl: [
    {
      pattern: /^(https?:\/\/)?([\da-z\.-]+)\.?([a-z\.]{0,6})?([\/\w \.-]*)*\/?(\?[;&a-z\d%_\.~+=-]*)?$/,
      message: '请输入有效的URL地址',
      trigger: 'blur'
    }
  ]
}

// 计算属性
const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const buildTime = computed(() => {
  return new Date().toLocaleDateString('zh-CN')
})

// 事件处理
const handleClose = () => {
  visible.value = false
}

const handleReset = () => {
  settingsForm.taggerUrl = ''
  settingsForm.defaultGridSize = 4
  settingsForm.defaultPageSize = 100
  ElMessage.info('设置已重置为默认值')
}

const handleSave = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    saving.value = true

    // 保存标签服务器地址
    if (settingsForm.taggerUrl) {
      await setTaggerUrl(settingsForm.taggerUrl)
    }

    // 保存其他设置到本地存储
    localStorage.setItem('app-settings', JSON.stringify({
      defaultGridSize: settingsForm.defaultGridSize,
      defaultPageSize: settingsForm.defaultPageSize,
      taggerUrl: settingsForm.taggerUrl
    }))

    ElMessage.success('设置保存成功')
    visible.value = false
  } catch (error) {
    if (error !== false) { // 不是验证失败
      console.error('保存设置失败:', error)
      ElMessage.error('保存设置失败，请重试')
    }
  } finally {
    saving.value = false
  }
}

const testConnection = async () => {
  if (!settingsForm.taggerUrl.trim()) {
    ElMessage.warning('请先输入标签服务器地址')
    return
  }

  testing.value = true

  try {
    // 这里可以添加测试连接的逻辑
    // 暂时模拟测试
    await new Promise(resolve => setTimeout(resolve, 1000))

    const controller = new AbortController()
    const timeoutId = setTimeout(() => controller.abort(), 5000)

    const response = await fetch(`${settingsForm.taggerUrl}/health`, {
      method: 'GET',
      signal: controller.signal
    }).catch(() => {
      clearTimeout(timeoutId)
      return null
    })

    clearTimeout(timeoutId)

    if (response?.ok) {
      ElMessage.success('连接测试成功！标签服务器正常运行')
    } else {
      ElMessage.warning('无法连接到标签服务器，请检查地址是否正确')
    }
  } catch (error) {
    ElMessage.error('连接测试失败，请检查服务器地址')
  } finally {
    testing.value = false
  }
}

const clearThumbnailCache = () => {
  ElMessageBox.confirm(
    '确定要清除缩略图缓存吗？这将删除所有已缓存的缩略图。',
    '清除缓存确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(() => {
    // 清除缩略图缓存的逻辑
    if ('caches' in window) {
      caches.delete('thumbnail-cache')
    }
    ElMessage.success('缩略图缓存已清除')
  }).catch(() => {
    // 用户取消
  })
}

const clearAllCache = () => {
  ElMessageBox.confirm(
    '确定要清除所有缓存吗？这将删除所有本地存储的数据。',
    '清除缓存确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(() => {
    // 清除所有缓存
    localStorage.removeItem('app-settings')
    if ('caches' in window) {
      caches.keys().then(cacheNames => {
        return Promise.all(
          cacheNames.map(cacheName => caches.delete(cacheName))
        )
      })
    }
    ElMessage.success('所有缓存已清除')
  }).catch(() => {
    // 用户取消
  })
}

// 生命周期 - 加载保存的设置
onMounted(() => {
  const savedSettings = localStorage.getItem('app-settings')
  if (savedSettings) {
    try {
      const settings = JSON.parse(savedSettings)
      settingsForm.defaultGridSize = settings.defaultGridSize || 4
      settingsForm.defaultPageSize = settings.defaultPageSize || 100
      settingsForm.taggerUrl = settings.taggerUrl || ''
    } catch (error) {
      console.warn('加载本地设置失败:', error)
    }
  }
})
</script>

<style scoped>
/* 设置对话框样式 */
:deep(.settings-dialog) {
  --el-dialog-bg-color: var(--dark-bg-elevated);
  --el-dialog-border-radius: var(--radius-lg);
}

:deep(.el-dialog__header) {
  background: var(--dark-surface-primary);
  border-bottom: 1px solid var(--dark-border-secondary);
  padding: var(--spacing-lg);
}

:deep(.el-dialog__title) {
  color: var(--dark-text-primary);
  font-weight: 600;
}

:deep(.el-dialog__body) {
  padding: var(--spacing-lg);
  max-height: 70vh;
  overflow-y: auto;
}

:deep(.el-dialog__footer) {
  border-top: 1px solid var(--dark-border-secondary);
  background: var(--dark-surface-primary);
}

/* 设置表单样式 */
.settings-form {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xl);
}

.settings-section {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.section-header {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.section-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--dark-text-primary);
}

.section-description {
  font-size: 13px;
  color: var(--dark-text-secondary);
  line-height: 1.4;
}

.form-item {
  margin-bottom: 0;
}

.form-help-text {
  font-size: 12px;
  color: var(--dark-text-tertiary);
  margin-top: var(--spacing-xs);
  line-height: 1.4;
}

/* 缓存操作按钮 */
.cache-actions {
  display: flex;
  gap: var(--spacing-md);
  flex-wrap: wrap;
}

.cache-actions .el-button {
  flex: 1;
}

/* 关于信息 */
.about-info {
  background: var(--dark-surface-secondary);
  border: 1px solid var(--dark-border-secondary);
  border-radius: var(--radius-md);
  padding: var(--spacing-lg);
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-sm) 0;
}

.info-row:not(:last-child) {
  border-bottom: 1px solid var(--dark-border-primary);
}

.info-label {
  font-weight: 500;
  color: var(--dark-text-secondary);
}

.info-value {
  color: var(--dark-text-primary);
  font-family: 'JetBrains Mono', 'Consolas', monospace;
}

/* 对话框底部 */
.dialog-footer {
  display: flex;
  gap: var(--spacing-md);
  justify-content: flex-end;
}

/* Element Plus 组件样式覆盖 */
:deep(.el-input__wrapper) {
  background-color: var(--dark-surface-secondary);
  border-color: var(--dark-border-secondary);
}

:deep(.el-input__wrapper:hover) {
  border-color: var(--dark-border-accent);
}

:deep(.el-input__wrapper.is-focus) {
  border-color: var(--dark-primary);
}

:deep(.el-select .el-input__wrapper) {
  background-color: var(--dark-surface-secondary);
}

:deep(.el-slider__runway) {
  background-color: var(--dark-surface-secondary);
}

:deep(.el-slider__bar) {
  background-color: var(--dark-primary);
}

:deep(.el-slider__button) {
  border-color: var(--dark-primary);
}

:deep(.el-form-item__label) {
  color: var(--dark-text-primary);
  font-weight: 600;
}

/* 响应式调整 */
@media (max-width: 600px) {
  :deep(.settings-dialog) {
    width: 95vw !important;
    margin: 5vh auto;
  }

  .cache-actions {
    flex-direction: column;
  }

  .info-row {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--spacing-xs);
  }
}
</style>
