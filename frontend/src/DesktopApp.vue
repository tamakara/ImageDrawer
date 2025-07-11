<template>
  <el-container>
    <el-aside>
      <QueryForm/>
    </el-aside>
    <el-main>
      <ImageGrid/>
    </el-main>
    <el-aside>
      <UploadQueue/>
    </el-aside>
  </el-container>
</template>

<script setup lang="ts">
import QueryForm from './components/QueryForm.vue'
import UploadQueue from './components/UploadQueue.vue'
import ImageGrid from './components/ImageGrid.vue'
</script>

<style scoped>
.el-container {
  height: 100vh;
}

.el-main {
  padding: 0;
}

.el-aside {
  padding: 10px 20px;
  min-width: 300px;
  border-left: 1px solid var(--el-border-color);
  border-right: 1px solid var(--el-border-color);
}
</style>


<style>
body {
  margin: 0;
}
</style>
