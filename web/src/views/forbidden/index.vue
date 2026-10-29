<template>
  <div class="forbidden-page">
    <div class="page-head">
      <div>
        <h2>违禁词管理</h2>
        <p class="sub">解析商品时自动校验标题，命中即提示。分类、等级、状态、备注可在表格里直接改。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openAdd">新增违禁词</el-button>
    </div>

    <!-- 概览 -->
    <div class="stat-row">
      <div class="stat">
        <span class="stat-num">{{ stats.total }}</span>
        <span class="stat-label">总词数</span>
      </div>
      <div class="stat on">
        <span class="stat-num">{{ stats.on }}</span>
        <span class="stat-label">已启用</span>
      </div>
      <div class="stat off">
        <span class="stat-num">{{ stats.off }}</span>
        <span class="stat-label">已停用</span>
      </div>
      <div class="stat">
        <span class="stat-num">{{ stats.level3 }}</span>
        <span class="stat-label">违规词</span>
      </div>
    </div>

    <el-card shadow="never" class="table-card">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="搜索违禁词 / 分类 / 添加人"
          :prefix-icon="Search"
          clearable
          class="search"
        />
        <el-radio-group v-model="statusFilter" size="default">
          <el-radio-button value="all">全部</el-radio-button>
          <el-radio-button value="1">启用</el-radio-button>
          <el-radio-button value="0">停用</el-radio-button>
        </el-radio-group>
        <span class="count">共 {{ filtered.length }} 条</span>
      </div>

      <el-table :data="filtered" v-loading="loading" stripe empty-text="还没有违禁词，点右上角新增">
        <el-table-column label="违禁词" min-width="150">
          <template #default="{ row }">
            <span class="word">{{ row.word }}</span>
          </template>
        </el-table-column>

        <el-table-column label="分类" width="140">
          <template #default="{ row }">
            <el-select
              v-model="row.category"
              size="small"
              filterable
              allow-create
              default-first-option
              placeholder="未分类"
              class="cell-select"
              @change="(v: string) => patch(row, 'category', v)"
            >
              <el-option v-for="c in categoryOptions" :key="c" :label="c" :value="c" />
            </el-select>
          </template>
        </el-table-column>

        <el-table-column label="等级" width="140">
          <template #default="{ row }">
            <el-select
              v-model="row.level"
              size="small"
              class="cell-select"
              :class="'lv-' + row.level"
              @change="(v: number) => patch(row, 'level', v)"
            >
              <el-option label="违规词" :value="3" />
              <el-option label="敏感词" :value="2" />
              <el-option label="平台禁用词" :value="1" />
            </el-select>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="112">
          <template #default="{ row }">
            <el-select
              v-model="row.enabled"
              size="small"
              class="cell-select"
              :class="row.enabled === '1' ? 'st-on' : 'st-off'"
              @change="(v: string) => patch(row, 'enabled', v)"
            >
              <el-option label="启用" value="1" />
              <el-option label="停用" value="0" />
            </el-select>
          </template>
        </el-table-column>

        <el-table-column label="备注" min-width="170">
          <template #default="{ row }">
            <el-input
              v-model="row.remark"
              size="small"
              placeholder="点击填写"
              maxlength="255"
              class="cell-input"
              @change="(v: string) => patch(row, 'remark', v)"
            />
          </template>
        </el-table-column>

        <el-table-column label="添加人" width="110">
          <template #default="{ row }">
            <span :class="{ sys: row.createdBy === 'system' }">{{ row.createdBy || '—' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="添加时间" width="165">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>

        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增 / 改词 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑违禁词' : '新增违禁词'"
      width="440px"
      @closed="resetForm"
    >
      <el-form :model="form" label-width="80px">
        <el-form-item label="违禁词" required>
          <el-input
            v-model="form.word"
            placeholder="如：最便宜、绝对、包治百病"
            maxlength="64"
            @keyup.enter="handleSubmit"
          />
        </el-form-item>
        <template v-if="!editingId">
          <el-form-item label="分类">
            <el-select
              v-model="form.category"
              style="width: 100%"
              filterable
              allow-create
              default-first-option
              placeholder="选择或直接输入"
            >
              <el-option v-for="c in categoryOptions" :key="c" :label="c" :value="c" />
            </el-select>
          </el-form-item>
          <el-form-item label="等级">
            <el-select v-model="form.level" style="width: 100%">
              <el-option label="违规词（最严重）" :value="3" />
              <el-option label="敏感词" :value="2" />
              <el-option label="平台禁用词" :value="1" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="form.enabled" style="width: 100%">
              <el-option label="启用" value="1" />
              <el-option label="停用" value="0" />
            </el-select>
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="选填" maxlength="255" />
          </el-form-item>
        </template>
        <p v-else class="tip">分类、等级、状态、备注请在表格里直接选择修改。</p>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Plus, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  addForbiddenWord,
  deleteForbiddenWord,
  getForbiddenWords,
  updateForbiddenWord,
} from '@/api/forbidden-word'

interface WordRow {
  id: number
  word: string
  category: string
  level: number
  enabled: string
  remark: string
  createdBy: string
  createdAt: string
}

const words = ref<WordRow[]>([])
const loading = ref(false)
const keyword = ref('')
const statusFilter = ref('all')
const dialogVisible = ref(false)
const submitting = ref(false)
const editingId = ref<number | null>(null)
const form = reactive({ word: '', category: '极限词', level: 2, enabled: '1', remark: '' })

const categoryOptions = computed(() => {
  const set = new Set(['极限词', '资质词', '功效词', '品牌词'])
  words.value.forEach((w) => w.category && set.add(w.category))
  return Array.from(set)
})

const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return words.value.filter((w) => {
    const hitKw =
      !kw ||
      w.word.toLowerCase().includes(kw) ||
      (w.category || '').toLowerCase().includes(kw) ||
      (w.createdBy || '').toLowerCase().includes(kw)
    const hitSt = statusFilter.value === 'all' || w.enabled === statusFilter.value
    return hitKw && hitSt
  })
})

const stats = computed(() => ({
  total: words.value.length,
  on: words.value.filter((w) => w.enabled === '1').length,
  off: words.value.filter((w) => w.enabled !== '1').length,
  level3: words.value.filter((w) => w.level === 3).length,
}))

function formatTime(v: string) {
  if (!v) return '—'
  return v.replace('T', ' ').slice(0, 19)
}

async function load() {
  loading.value = true
  try {
    const res = await getForbiddenWords()
    words.value = (res.data as WordRow[]) || []
  } catch {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

/** 表格内直接改单个字段：乐观更新，失败回滚 */
async function patch(row: WordRow, field: 'category' | 'level' | 'enabled' | 'remark', value: any) {
  try {
    const res = await updateForbiddenWord({ id: row.id, [field]: value })
    if (res.data && typeof res.data === 'object') Object.assign(row, res.data)
    ElMessage.success('已更新')
  } catch {
    load()
  }
}

function openAdd() {
  editingId.value = null
  form.word = ''
  form.category = '极限词'
  form.level = 2
  form.enabled = '1'
  form.remark = ''
  dialogVisible.value = true
}

function openEdit(row: WordRow) {
  editingId.value = row.id
  form.word = row.word
  dialogVisible.value = true
}

function resetForm() {
  editingId.value = null
  form.word = ''
}

async function handleSubmit() {
  const word = form.word.trim()
  if (!word) return ElMessage.warning('请输入违禁词')
  submitting.value = true
  try {
    if (editingId.value) {
      await updateForbiddenWord({ id: editingId.value, word })
      ElMessage.success('已保存')
    } else {
      await addForbiddenWord({
        word,
        category: form.category || undefined,
        level: form.level,
        remark: form.remark || undefined,
        enabled: form.enabled,
      })
      ElMessage.success('已添加')
    }
    dialogVisible.value = false
    load()
  } catch {
    // 拦截器已提示
  } finally {
    submitting.value = false
  }
}

async function remove(row: WordRow) {
  try {
    await ElMessageBox.confirm(`确定删除违禁词「${row.word}」吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await deleteForbiddenWord(row.id)
    ElMessage.success('已删除')
    load()
  } catch {
    // 拦截器已提示
  }
}

onMounted(load)
</script>

<style scoped>
.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 16px;
}
.page-head h2 {
  margin: 0 0 4px;
  font-size: 20px;
}
.sub {
  margin: 0;
  color: #909399;
  font-size: 13px;
}

.stat-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}
.stat {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 12px 16px;
  border-radius: 10px;
  background: #fff;
  border: 1px solid #ebeef5;
}
.stat-num {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}
.stat-label {
  font-size: 12px;
  color: #909399;
}
.stat.on .stat-num {
  color: #67c23a;
}
.stat.off .stat-num {
  color: #909399;
}

.table-card {
  border-radius: 10px;
}
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.search {
  width: 260px;
}
.count {
  margin-left: auto;
  font-size: 13px;
  color: #909399;
}

.word {
  font-weight: 600;
}
.sys {
  color: #909399;
}
.tip {
  margin: 0;
  font-size: 12px;
  color: #909399;
}

/* 表格内下拉/输入：弱化成可点的单元格 */
.cell-select {
  width: 100%;
}
.cell-select :deep(.el-select__wrapper) {
  box-shadow: none;
  background: transparent;
  padding: 0 8px;
  min-height: 28px;
}
.cell-select :deep(.el-select__wrapper:hover) {
  background: #f5f7fa;
}
.cell-select :deep(.el-select__selected-item) {
  font-size: 13px;
}
.st-on :deep(.el-select__selected-item) {
  color: #67c23a;
  font-weight: 600;
}
.st-off :deep(.el-select__selected-item) {
  color: #909399;
}
.lv-3 :deep(.el-select__selected-item) {
  color: #f56c6c;
  font-weight: 600;
}
.lv-2 :deep(.el-select__selected-item) {
  color: #e6a23c;
  font-weight: 600;
}
.lv-1 :deep(.el-select__selected-item) {
  color: #909399;
}

.cell-input :deep(.el-input__wrapper) {
  box-shadow: none;
  background: transparent;
  padding: 0 8px;
}
.cell-input :deep(.el-input__wrapper:hover) {
  background: #f5f7fa;
}

@media (max-width: 900px) {
  .stat-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
