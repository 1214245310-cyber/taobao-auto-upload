<template>
  <div class="screen">
    <h2>公司作战大屏</h2>
    <el-row :gutter="16">
      <el-col v-for="c in cards" :key="c.label" :span="6">
        <el-card shadow="hover">
          <div class="label">{{ c.label }}</div>
          <div class="value">{{ c.value }}</div>
        </el-card>
      </el-col>
    </el-row>
    <el-card class="block" shadow="never">
      <template #header>本月客服排行（128 人）</template>
      <el-table :data="rankRows" height="320" stripe>
        <el-table-column prop="rank" label="排名" width="80" />
        <el-table-column prop="name" label="客服" />
        <el-table-column prop="amount" label="业绩" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
// 大屏为骨架：示例数据后续由 /tb/publish/stat 等接口填充。
const cards = [
  { label: '本月团队总业绩', value: '¥394,113.71' },
  { label: '今日业绩', value: '¥16,065.68' },
  { label: '在册客服', value: '128' },
  { label: '团队数', value: '10' },
]
const rankRows = [
  { rank: 1, name: '李四', amount: '14,664' },
  { rank: 2, name: '王五', amount: '14,209' },
  { rank: 3, name: '赵六', amount: '13,161' },
]
</script>

<style scoped>
.screen {
  padding: 16px;
}
.value {
  font-size: 24px;
  font-weight: 700;
  margin-top: 8px;
  color: #409eff;
}
.block {
  margin-top: 16px;
}
</style>
