<template>
  <div class="publish">
    <div class="toolbar">
      <el-button type="primary" :icon="Plus" @click="onCreate">新建上货任务</el-button>
      <el-button :icon="Refresh" @click="load">刷新</el-button>
    </div>
    <el-table :data="rows" v-loading="loading" border>
      <el-table-column prop="batchNo" label="批次号" />
      <el-table-column prop="shopName" label="目标店铺" />
      <el-table-column prop="status" label="状态" />
      <el-table-column prop="publishedCount" label="已上架" />
      <el-table-column prop="totalCount" label="总数" />
      <el-table-column label="操作" width="240">
        <template #default="{ row }">
          <el-button link type="primary" @click="onPause(row)">暂停</el-button>
          <el-button link type="primary" @click="onResume(row)">继续</el-button>
          <el-button link type="danger" @click="onTerminate(row)">终止</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" title="新建上货任务（骨架）" width="480px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="批次名称"><el-input v-model="form.batchName" /></el-form-item>
        <el-form-item label="目标店铺">
          <el-select v-model="form.shopId" placeholder="选择店铺">
            <el-option label="示例店铺" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="上货模板">
          <el-select v-model="form.templateId" placeholder="选择模板">
            <el-option label="默认模板" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="执行方式">
          <el-radio-group v-model="form.execMode">
            <el-radio value="once">一次性</el-radio>
            <el-radio value="schedule">定时持续</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="dialog = false">提交（骨架）</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { publishList, publishPause, publishResume, publishTerminate } from '@/api/publish'

const loading = ref(false)
const dialog = ref(false)
const rows = ref<any[]>([])
const form = reactive({ batchName: '', shopId: undefined, templateId: undefined, execMode: 'once' })

async function load() {
  loading.value = true
  try {
    const res = await publishList({ pageNum: 1, pageSize: 20 })
    rows.value = res.data?.rows || []
  } finally {
    loading.value = false
  }
}
function onCreate() {
  dialog.value = true
}
async function onPause(row: any) {
  await publishPause(row.id)
  load()
}
async function onResume(row: any) {
  await publishResume(row.id)
  load()
}
async function onTerminate(row: any) {
  await publishTerminate(row.id)
  load()
}

onMounted(load)
</script>

<style scoped>
.publish {
  padding: 16px;
}
.toolbar {
  margin-bottom: 12px;
  display: flex;
  gap: 8px;
}
</style>
