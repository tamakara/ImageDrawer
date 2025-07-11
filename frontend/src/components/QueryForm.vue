<template>
  <el-form>
    <el-form-item class="rating" label="图片分级" label-position="top">
      <el-radio-group v-model="params.rating">
        <el-radio-button label="全部" value="all"/>
        <el-radio-button label="一般" value="general"/>
        <el-radio-button label="敏感" value="sensitive"/>
        <el-radio-button label="裸露" value="explicit"/>
      </el-radio-group>
    </el-form-item>

    <el-form-item class="sort" label="排序方式" label-position="top">
      <el-select
          v-model="params.sort"
          placeholder="选择排序方式"
      >
        <el-option label="随机" value="random"/>
        <el-option label="文件大小" value="size"/>
        <el-option label="添加时间" value="time"/>
      </el-select>
    </el-form-item>

    <el-form-item class="tag_input" label="标签筛选" label-position="top">
      <el-autocomplete
          v-model="tagText"
          @keydown.enter="onSubmit"
          :trigger-on-focus="false"
          :fetch-suggestions="fetchTags"
      >
        <template #suffix>
          <el-icon class="el-input__icon" @click="onSubmit">
            <Right/>
          </el-icon>
        </template>
      </el-autocomplete>
    </el-form-item>

    <el-form-item class="tag_list">
      <el-tag
          v-for="tag in params.tags"
          :key="tag"
          closable
          @close="handleTagClose(tag)"
          style="margin: 3px"
      >
        {{ tag }}
      </el-tag>
    </el-form-item>

    <el-form-item class="actions">
      <el-button @click="onQuery" type="primary" size="large" style="width: 100%;">
        查询
      </el-button>
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import {ref} from 'vue'
import {queryTags} from "../api";
import {useImageStore} from "../stores/image.ts";
import {Right} from '@element-plus/icons-vue'
import {storeToRefs} from "pinia";

const imageStore = useImageStore()

const {params} = storeToRefs(imageStore)

function onQuery() {
  imageStore.query()
}

const tagText = ref('')

function onSubmit() {
  const tag = tagText.value.trim()
  if (tag && !params.value.tags.includes(tag)) {
    params.value.tags.push(tag)
  }
  tagText.value = ''
}

function handleTagClose(tag: string) {
  params.value.tags = params.value.tags.filter(t => t !== tag)
}

function fetchTags(queryString: string, cb: (results: { value: string }[]) => void) {
  if (!queryString) {
    cb([])
    return
  }

  queryTags(queryString)
      .then(tags => {
        cb(tags.map(t => ({value: t})))
      })
      .catch(() => cb([]))
}

</script>

<style scoped>
.el-form {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.rating {
  height: 60px;
}

.sort {
  height: 60px;
}

.tag_input {
  height: 60px;
}

.tag_list {
  flex: 1;
  display: block;
  padding: 10px;
  border-radius: 5px;
  border: 1px solid #dcdfe6;
  justify-content: start;
}

.actions {
  height: 30px;

}

</style>