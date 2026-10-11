<template>
  <div class="parse">
    <el-card shadow="never">
      <template #header>链接解析</template>
      <el-input v-model="url" placeholder="粘贴淘宝 / 1688 商品链接" class="url-input">
        <template #prepend>
          <el-select v-model="platform" style="width: 110px">
            <el-option label="淘宝" value="TAOBAO" />
            <el-option label="1688" value="ALI1688" />
            <el-option label="拼多多" value="PDD" />
            <el-option label="京东" value="JD" />
          </el-select>
        </template>
        <template #append>
          <el-button type="primary" :loading="loading" @click="onParse">解析</el-button>
        </template>
      </el-input>
    </el-card>

    <el-card v-if="result" shadow="never" class="result">
      <template #header>解析结果（标准化商品 JSON）</template>
      <pre>{{ JSON.stringify(result, null, 2) }}</pre>
      <el-button type="success" @click="onPublish">解码上货</el-button>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { parseItem } from '@/api/parse'
import { ElMessage } from 'element-plus'

const url = ref('')
const platform = ref('TAOBAO')
const loading = ref(false)
const result = ref<any>(null)

async function onParse() {
  if (!url.value) return ElMessage.warning('请先粘贴商品链接')
  loading.value = true
  try {
    const res = await parseItem({ url: url.value, platform: platform.value })
    result.value = res.data
  } finally {
    loading.value = false
  }
}
function onPublish() {
  ElMessage.success('已生成上货 payload（骨架，待接执行器）')
}
</script>

<style scoped>
.parse {
  padding: 16px;
}
.url-input {
  max-width: 720px;
}
.result {
  margin-top: 16px;
}
.result pre {
  background: #0d1117;
  color: #c9d1d9;
  padding: 16px;
  border-radius: 8px;
  overflow: auto;
  max-height: 420px;
}
</style>
