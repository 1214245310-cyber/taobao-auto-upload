<template>
  <div class="nf">
    <h1>404</h1>
    <p>页面不存在</p>
    <el-button type="primary" @click="$router.replace('/company-screen')">返回首页</el-button>
  </div>
</template>

<script setup lang="ts"></script>

<style scoped>
.nf {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
}
.nf h1 {
  font-size: 64px;
  margin: 0;
  color: #909399;
}
</style>
