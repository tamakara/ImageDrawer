<script setup lang="ts">
import {computed, ref, watch} from 'vue'
import {
  NButton,
  NDivider,
  NIcon,
  NImage,
  NInput,
  NInputGroup,
  NModal,
  NPopconfirm,
  NSelect,
  NTag,
  NTooltip,
  useMessage
} from 'naive-ui'
import {
  AddOutline,
  CloseOutline,
  DocumentTextOutline,
  DownloadOutline,
  HardwareChipOutline,
  ImageOutline,
  InformationCircleOutline,
  PencilOutline,
  PricetagOutline,
  RefreshOutline,
  ResizeOutline,
  TimeOutline,
  TrashOutline
} from '@vicons/ionicons5'
import {galleryApi, type ImageDto, type TagDto} from '../../api/gallery.ts'
import {tagsApi} from '../../api/tags.ts'
import {useDateFormat} from '@vueuse/core'

const props = defineProps<{
  show: boolean
  imageId: number | null
}>()

const emit = defineEmits<{
  (e: 'update:show', value: boolean): void
  (e: 'refresh'): void
}>()

const message = useMessage()
const showInfo = ref(false)
const image = ref<ImageDto | null>(null)
const loading = ref(false)
const editingName = ref(false)
const newName = ref('')
const newTagName = ref('')
const newTagType = ref('general')
const regenerating = ref(false)
const isEditingTags = ref(false)


const tagTypeOrder = ['copyright', 'character', 'artist', 'general', 'meta', 'rating']

const tagTypeMap: Record<string, string> = {
  custom: '自定义',
  copyright: '版权',
  character: '角色',
  artist: '作者',
  general: '一般',
  meta: '元数据',
  rating: '分级'
}

const tagOptions = computed(() => {
  return tagTypeOrder.map(type => ({
    label: tagTypeMap[type],
    value: type
  }))
})

const formattedSize = computed(() => {
  if (!image.value) return ''
  const size = image.value.size
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(2) + ' KB'
  return (size / (1024 * 1024)).toFixed(2) + ' MB'
})

const fetchImage = async () => {
  if (!props.imageId) return
  loading.value = true
  try {
    image.value = await galleryApi.getImage(props.imageId)
    newName.value = image.value.title
  } catch (e) {
    message.error('加载图片详情失败')
  } finally {
    loading.value = false
  }
}

watch(() => props.show, (val) => {
  if (val && props.imageId) {
    fetchImage()
  } else {
    image.value = null
    showInfo.value = false
  }
})

const handleClose = () => {
  emit('update:show', false)
}

const toggleInfo = () => {
  showInfo.value = !showInfo.value
}

const saveName = async () => {
  if (!image.value || !newName.value || newName.value === image.value.title) {
    editingName.value = false
    return
  }
  try {
    image.value = await galleryApi.updateImage(image.value.id, {title: newName.value})
    message.success('名称已更新')
    emit('refresh')
  } catch (e) {
    message.error('更新名称失败')
  } finally {
    editingName.value = false
  }
}

const handleDelete = async () => {
  if (!image.value) return
  try {
    await galleryApi.deleteImage(image.value.id)
    message.success('图片已删除')
    emit('refresh')
    handleClose()
  } catch (e) {
    message.error('删除图片失败')
  }
}

const handleRegenerate = async () => {
  if (!image.value) return
  regenerating.value = true
  try {
    image.value = await galleryApi.regenerateTags(image.value.id)
    message.success('标签已重新生成')
  } catch (e) {
    message.error('重新生成标签失败')
  } finally {
    regenerating.value = false
  }
}

const handleAddTag = async () => {
  if (!image.value || !newTagName.value.trim()) {
    return
  }

  try {
    const existingTags = await tagsApi.listTags(newTagName.value.trim())
    if (existingTags.some(t => t.name === newTagName.value.trim())) {
      message.error('添加失败：标签已存在')
      return
    }

    image.value = await galleryApi.addTag(image.value.id, {
      name: newTagName.value.trim(),
      type: newTagType.value
    })
    message.success('标签添加成功')
    newTagName.value = ''
  } catch (e) {
    message.error('标签添加失败')
  }
}

const handleRemoveTag = async (tag: TagDto) => {
  if (!image.value) return
  try {
    image.value = await galleryApi.removeTag(image.value.id, tag.id)
    message.success('标签已移除')
  } catch (e) {
    message.error('移除标签失败')
  }
}

const handleDownload = () => {
  if (!image.value) return
  const link = document.createElement('a')
  link.href = image.value.url
  link.download = image.value.fileName || (image.value.title + '.' + image.value.extension)
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

const groupedTags = computed(() => {
  if (!image.value || !image.value.tags) return {}

  const groups: Record<string, TagDto[]> = {}
  tagTypeOrder.forEach(t => groups[t] = [])

  image.value.tags.forEach(tag => {
    const type = tag.type || 'general'
    if (groups[type]) {
      groups[type].push(tag)
    } else {
      if (!groups['general']) groups['general'] = []
      groups['general'].push(tag)
    }
  })

  return groups
})

const getTagColor = (type: string) => {
  switch (type) {
    case 'custom': return { color: 'rgba(0, 188, 212, 0.15)', textColor: '#00bcd4' }
    case 'copyright': return { color: 'rgba(213, 0, 249, 0.15)', textColor: '#e040fb' }
    case 'character': return { color: 'rgba(0, 200, 83, 0.15)', textColor: '#69f0ae' }
    case 'artist': return { color: 'rgba(255, 23, 68, 0.15)', textColor: '#ff5252' }
    case 'meta': return { color: 'rgba(255, 145, 0, 0.15)', textColor: '#ffab40' }
    case 'rating': return { color: 'rgba(158, 158, 158, 0.15)', textColor: '#bdbdbd' }
    case 'general':
    default: return { color: 'rgba(59, 130, 246, 0.15)', textColor: '#93c5fd' }
  }
}

</script>

<template>
  <n-modal :show="show" @update:show="(v) => emit('update:show', v)" class="custom-modal" :auto-focus="false">
    <div class="flex h-[90vh] w-[90vw] bg-black/90 rounded-lg overflow-hidden relative">

      <!-- 主图片区域 -->
      <div class="flex-1 flex items-center justify-center relative p-4 overflow-hidden">
        <n-image
            v-if="image"
            :src="image.url"
            :alt="image.title"
            class="w-full h-full flex items-center justify-center"
            :img-props="{ class: 'max-h-full max-w-full shadow-lg' }"
        />

        <!-- 工具栏 -->
        <div class="absolute top-4 right-4 flex gap-2">
          <n-button circle secondary type="primary" @click="toggleInfo">
            <template #icon>
              <n-icon :component="InformationCircleOutline"/>
            </template>
          </n-button>
          <n-button circle secondary type="error" @click="handleClose">
            <template #icon>
              <n-icon :component="CloseOutline"/>
            </template>
          </n-button>
        </div>
      </div>

      <!-- 信息面板 -->
      <div v-if="showInfo && image"
           class="w-[28rem] bg-gray-900/95 backdrop-blur-md text-white p-6 flex flex-col gap-6 border-l border-gray-700 overflow-y-auto transition-all duration-300">

        <!-- 标题部分 -->
        <div class="flex flex-col gap-2">
          <div class="text-sm text-gray-400 uppercase font-bold tracking-wider">标题</div>
          <div v-if="!editingName" @click="editingName = true"
               class="text-2xl font-semibold cursor-pointer hover:text-primary-400 truncate transition-colors"
               title="点击编辑">
            {{ image.title }}
          </div>
          <n-input v-else v-model:value="newName" @blur="saveName" @keyup.enter="saveName" autofocus
                   placeholder="输入名称" size="large"/>
        </div>

        <n-divider class="my-0 bg-gray-700"/>

        <!-- 标签部分 -->
        <div class="flex flex-col gap-3">
          <div class="flex items-center justify-between">
            <div class="text-sm text-gray-400 uppercase font-bold tracking-wider flex items-center gap-1">
              <n-icon :component="PricetagOutline"/>
              标签
            </div>
            <div class="flex gap-2">
              <n-button
                  size="tiny"
                  secondary
                  circle
                  :type="isEditingTags ? 'warning' : 'tertiary'"
                  @click="isEditingTags = !isEditingTags"
              >
                <template #icon>
                  <n-icon :component="PencilOutline"/>
                </template>
              </n-button>
              <n-button
                  size="tiny"
                  secondary
                  circle
                  type="info"
                  :loading="regenerating"
                  @click="handleRegenerate"
              >
                <template #icon>
                  <n-icon :component="RefreshOutline"/>
                </template>
              </n-button>
            </div>
          </div>

          <div v-if="isEditingTags" class="mb-2">
            <n-input-group>
              <n-select
                  v-model:value="newTagType"
                  :options="tagOptions"
                  :style="{ width: '30%' }"
                  size="small"
              />
              <n-input
                  v-model:value="newTagName"
                  placeholder="标签名称"
                  size="small"
                  autofocus
                  @keyup.enter="handleAddTag"
              />
              <n-button size="small" type="primary" secondary @click="handleAddTag">
                <template #icon>
                  <n-icon :component="AddOutline"/>
                </template>
              </n-button>
            </n-input-group>
          </div>

          <div v-if="image.tags?.length" class="flex flex-col gap-3">
            <template v-for="type in tagTypeOrder" :key="type">
              <div v-if="groupedTags[type]?.length" class="flex flex-col gap-1">
                <div class="text-xs text-gray-500 uppercase font-semibold tracking-wider ml-1">{{ tagTypeMap[type] || type }}</div>
                <div class="flex flex-wrap gap-2">
                  <n-tag
                      v-for="tag in groupedTags[type]"
                      :key="tag.id"
                      size="medium"
                      round
                      :bordered="false"
                      :color="getTagColor(type)"
                      :closable="isEditingTags"
                      @close="handleRemoveTag(tag)"
                      class="hover:opacity-80 transition-opacity"
                  >
                    {{ tag.name }}
                  </n-tag>
                </div>
              </div>
            </template>
          </div>
          <span v-else class="text-gray-500 text-sm italic py-1">暂无标签</span>
        </div>

        <n-divider class="my-0 bg-gray-700"/>

        <!-- 详细信息 -->
        <div class="flex flex-col gap-4">
          <div class="text-sm text-gray-400 uppercase font-bold tracking-wider">详细信息</div>
          <div class="grid grid-cols-2 gap-y-5 gap-x-4 text-base">
            <!-- Size -->
            <div class="flex flex-col gap-1">
                 <span class="text-gray-500 text-sm flex items-center gap-1">
                    <n-icon :component="ResizeOutline"/> 尺寸
                 </span>
              <span class="text-gray-200 font-mono">{{ image.width }} × {{ image.height }}</span>
            </div>
            <!-- File Size -->
            <div class="flex flex-col gap-1">
                 <span class="text-gray-500 text-sm flex items-center gap-1">
                    <n-icon :component="HardwareChipOutline"/> 大小
                 </span>
              <span class="text-gray-200 font-mono">{{ formattedSize }}</span>
            </div>
            <!-- Format -->
            <div class="flex flex-col gap-1">
                 <span class="text-gray-500 text-sm flex items-center gap-1">
                    <n-icon :component="ImageOutline"/> 格式
                 </span>
              <span class="text-gray-200 uppercase font-mono">{{ image.extension }}</span>
            </div>
            <!-- Date -->
            <div class="flex flex-col gap-1">
                 <span class="text-gray-500 text-sm flex items-center gap-1">
                    <n-icon :component="TimeOutline"/> 创建时间
                 </span>
              <span class="text-gray-200 font-mono">{{ useDateFormat(image.createdAt, 'YYYY-MM-DD').value }}</span>
            </div>
          </div>

          <!-- Full Filename & Hash -->
          <div class="flex flex-col gap-3 mt-2">
            <div class="flex flex-col gap-1">
                <span class="text-gray-500 text-sm flex items-center gap-1">
                   <n-icon :component="DocumentTextOutline"/> 文件名
                </span>
              <n-tooltip trigger="hover" placement="top">
                <template #trigger>
                  <span
                      class="text-gray-300 text-sm truncate font-mono bg-gray-800/50 p-2 rounded border border-gray-700/50 select-all">{{
                      image.fileName
                    }}</span>
                </template>
                {{ image.fileName }}
              </n-tooltip>
            </div>

            <div class="flex flex-col gap-1">
                <span class="text-gray-500 text-sm flex items-center gap-1">
                   <span class="font-bold text-[10px]">#</span> 哈希
                </span>
              <n-tooltip trigger="hover" placement="top">
                <template #trigger>
                  <span
                      class="text-gray-300 text-sm truncate font-mono bg-gray-800/50 p-2 rounded border border-gray-700/50 select-all">{{
                      image.hash
                    }}</span>
                </template>
                {{ image.hash }}
              </n-tooltip>
            </div>
          </div>
        </div>

        <!-- 操作 -->
        <div class="flex flex-col gap-3 mt-auto">
          <n-button block secondary type="info" @click="handleDownload">
            <template #icon>
              <n-icon :component="DownloadOutline"/>
            </template>
            下载原图
          </n-button>

          <n-popconfirm @positive-click="handleDelete">
            <template #trigger>
              <n-button block secondary type="error">
                <template #icon>
                  <n-icon :component="TrashOutline"/>
                </template>
                删除图片
              </n-button>
            </template>
            确定要删除这张图片吗？
          </n-popconfirm>
        </div>
      </div>
    </div>
  </n-modal>
</template>

<style scoped>
/* Custom scrollbar for info panel */
::-webkit-scrollbar {
  width: 6px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: #4b5563;
  border-radius: 3px;
}
</style>

