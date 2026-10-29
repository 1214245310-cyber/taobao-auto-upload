<template>
  <div class="forbidden-page">
    <el-breadcrumb separator="/" class="crumb">
      <el-breadcrumb-item>店铺商品管理</el-breadcrumb-item>
      <el-breadcrumb-item>违禁词库</el-breadcrumb-item>
    </el-breadcrumb>

    <div class="toolbar">
      <div class="left">
        <el-select v-model="categoryFilter" class="cat" @change="applySearch">
          <el-option label="全部分类" value="all" />
          <el-option v-for="c in categoryOptions" :key="c" :label="c" :value="c" />
        </el-select>
        <el-input
          v-model="keywordInput"
          class="kw"
          placeholder="搜索关键词"
          clearable
          @keyup.enter="applySearch"
          @clear="applySearch"
        />
        <el-button type="primary" class="search-btn" @click="applySearch">搜索</el-button>
      </div>
      <div class="right">
        <el-button class="ghost-btn" @click="copyAll">一键复制全部违禁词</el-button>
        <el-button class="ghost-btn" @click="openImport">导入</el-button>
        <el-button type="primary" @click="openAdd">添加违禁词</el-button>
      </div>
    </div>

    <div class="table-wrap">
      <el-table
        :data="pageRows"
        v-loading="loading"
        class="word-table"
        empty-text="没有匹配的违禁词"
      >
        <el-table-column label="违禁词" min-width="280">
          <template #default="{ row }">
            <div class="word-cell">
              <el-switch
                v-model="row.enabled"
                active-value="1"
                inactive-value="0"
                size="small"
                @change="(v: string) => patch(row, 'enabled', v)"
              />
              <span class="word" :class="{ off: row.enabled !== '1' }">{{ row.word }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="标签" width="108">
          <template #default="{ row }">
            <el-select
              v-model="row.category"
              size="small"
              filterable
              allow-create
              default-first-option
              placeholder="未分类"
              class="cell-select tag"
              @change="(v: string) => patch(row, 'category', v)"
            >
              <el-option v-for="c in categoryOptions" :key="c" :label="c" :value="c" />
            </el-select>
          </template>
        </el-table-column>

        <el-table-column label="匹配方式" width="100">
          <template #default="{ row }">
            <el-select
              v-model="row.matchType"
              size="small"
              class="cell-select plain"
              @change="(v: string) => patch(row, 'matchType', v)"
            >
              <el-option label="等于" value="等于" />
              <el-option label="包含" value="包含" />
            </el-select>
          </template>
        </el-table-column>

        <el-table-column label="词库" width="104">
          <template #default="{ row }">
            <el-select
              v-model="row.targetLibrary"
              size="small"
              class="cell-select lib"
              @change="(v: string) => patch(row, 'targetLibrary', v)"
            >
              <el-option label="标题库" value="标题库" />
              <el-option label="品牌库" value="品牌库" />
            </el-select>
          </template>
        </el-table-column>

        <el-table-column label="级别" width="112">
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

        <el-table-column label="原因备注" min-width="190">
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

        <el-table-column label="添加人" width="96">
          <template #default="{ row }">
            <span :class="['adder', { sys: row.createdBy === 'system' }]">{{ row.createdBy || '—' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="添加时间" width="158">
          <template #default="{ row }">
            <span class="time">{{ formatTime(row.createdAt) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          :page-size="pageSize"
          :total="filtered.length"
          layout="total, prev, pager, next, jumper"
          background
        />
      </div>
    </div>

    <!-- 添加 / 编辑 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑违禁词' : '添加违禁词'"
      width="660px"
      class="word-dialog"
      align-center
      @closed="resetForm"
    >
      <el-form :model="form" label-width="96px" label-position="right" class="word-form">
        <template v-if="!editingId">
          <el-form-item label="加到哪个库">
            <el-radio-group v-model="form.targetLibrary" class="radio-line">
              <el-radio value="标题库">标题库（比商品标题）</el-radio>
              <el-radio value="品牌库">品牌库（比品牌属性）</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="匹配方式">
            <div class="col">
              <el-radio-group v-model="form.matchType" class="radio-line">
                <el-radio value="等于">等于</el-radio>
                <el-radio value="包含">包含</el-radio>
              </el-radio-group>
              <p class="hint">{{ matchHint }}</p>
            </div>
          </el-form-item>
        </template>

        <el-form-item label="违禁词" required>
          <el-input
            v-model="form.word"
            placeholder="不区分大小写和空格"
            maxlength="64"
            @keyup.enter="handleSubmit"
          />
        </el-form-item>

        <el-form-item v-if="!editingId" label="原因备注" required>
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="2"
            resize="none"
            maxlength="255"
            placeholder="请填写禁用原因，比如「客户投诉过」——将来别人想删时能看到为什么禁"
          />
        </el-form-item>
        <p v-else class="tip">词库、匹配方式、标签、级别、原因备注请在表格里直接选择修改。</p>
      </el-form>
      <template #footer>
        <el-button class="cancel-btn" @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" class="submit-btn" :loading="submitting" @click="handleSubmit">
          {{ editingId ? '保存' : '添加' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 批量导入 -->
    <el-dialog v-model="importVisible" title="导入违禁词" width="520px" @closed="resetImport">
      <p class="import-tip">每行一个违禁词；也可写成「违禁词,标签,备注」，已存在的词会自动跳过。</p>
      <el-input
        v-model="importForm.text"
        type="textarea"
        :rows="8"
        placeholder="最便宜&#10;绝对,极限词,不线下&#10;包治百病,功效词,售后无保障"
      />
      <div class="import-opts">
        <el-select v-model="importForm.category" placeholder="默认标签" style="width: 150px">
          <el-option v-for="c in categoryOptions" :key="c" :label="c" :value="c" />
        </el-select>
        <el-select v-model="importForm.matchType" style="width: 130px">
          <el-option label="匹配：等于" value="等于" />
          <el-option label="匹配：包含" value="包含" />
        </el-select>
        <el-select v-model="importForm.targetLibrary" style="width: 130px">
          <el-option label="词库：标题库" value="标题库" />
          <el-option label="词库：品牌库" value="品牌库" />
        </el-select>
      </div>
      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="primary" :loading="importing" @click="handleImport">开始导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  addForbiddenWord,
  batchAddForbiddenWord,
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
  matchType: string
  targetLibrary: string
  remark: string
  createdBy: string
  createdAt: string
}

const words = ref<WordRow[]>([])
const loading = ref(false)
const categoryFilter = ref('all')
const keywordInput = ref('')
const keyword = ref('')
const page = ref(1)
const pageSize = 20

const dialogVisible = ref(false)
const submitting = ref(false)
const editingId = ref<number | null>(null)
const form = reactive({
  word: '',
  category: '极限词',
  level: 2,
  matchType: '等于',
  targetLibrary: '标题库',
  remark: '',
})

const matchHint = computed(() =>
  form.matchType === '等于'
    ? '整个标题（或整个品牌值）一模一样才拦，范围窄得多'
    : '标题里只要出现这个词就算命中，范围宽得多',
)

const importVisible = ref(false)
const importing = ref(false)
const importForm = reactive({ text: '', category: '极限词', matchType: '等于', targetLibrary: '标题库' })

const categoryOptions = computed(() => {
  const set = new Set(['极限词', '资质词', '功效词', '品牌词'])
  words.value.forEach((w) => w.category && set.add(w.category))
  return Array.from(set)
})

const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return words.value.filter((w) => {
    const hitCat = categoryFilter.value === 'all' || w.category === categoryFilter.value
    const hitKw =
      !kw ||
      w.word.toLowerCase().includes(kw) ||
      (w.remark || '').toLowerCase().includes(kw) ||
      (w.createdBy || '').toLowerCase().includes(kw)
    return hitCat && hitKw
  })
})

const pageRows = computed(() => {
  const start = (page.value - 1) * pageSize
  return filtered.value.slice(start, start + pageSize)
})

// 筛选结果变少时回到第一页，避免停在空白页
watch(
  () => filtered.value.length,
  (len) => {
    const maxPage = Math.max(1, Math.ceil(len / pageSize))
    if (page.value > maxPage) page.value = maxPage
  },
)

function formatTime(v: string) {
  if (!v) return '—'
  return v.replace('T', ' ').slice(0, 19)
}

function applySearch() {
  keyword.value = keywordInput.value
  page.value = 1
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

/** 表格内直接改单个字段：失败则重载回滚 */
async function patch(
  row: WordRow,
  field: 'category' | 'level' | 'enabled' | 'remark' | 'matchType' | 'targetLibrary',
  value: any,
) {
  try {
    const res = await updateForbiddenWord({ id: row.id, [field]: value })
    if (res.data && typeof res.data === 'object') Object.assign(row, res.data)
    ElMessage.success('已更新')
  } catch {
    load()
  }
}

async function copyAll() {
  const list = filtered.value.map((w) => w.word)
  if (!list.length) return ElMessage.warning('没有可复制的违禁词')
  const text = list.join('\n')
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(text)
    } else {
      const ta = document.createElement('textarea')
      ta.value = text
      ta.style.position = 'fixed'
      ta.style.opacity = '0'
      document.body.appendChild(ta)
      ta.select()
      document.execCommand('copy')
      document.body.removeChild(ta)
    }
    ElMessage.success(`已复制 ${list.length} 个违禁词`)
  } catch {
    ElMessage.error('复制失败，请手动选择')
  }
}

function openAdd() {
  editingId.value = null
  form.word = ''
  form.category = '极限词'
  form.level = 2
  form.matchType = '等于'
  form.targetLibrary = '标题库'
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
  if (!editingId.value && !form.remark.trim()) {
    return ElMessage.warning('请填写原因备注，将来别人想删时才知道为什么禁')
  }
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
        matchType: form.matchType,
        targetLibrary: form.targetLibrary,
        remark: form.remark.trim() || undefined,
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

function openImport() {
  importForm.text = ''
  importForm.category = '极限词'
  importForm.matchType = '等于'
  importForm.targetLibrary = '标题库'
  importVisible.value = true
}

function resetImport() {
  importForm.text = ''
}

async function handleImport() {
  const lines = importForm.text.split(/\r?\n/).filter((l) => l.trim())
  if (!lines.length) return ElMessage.warning('请先粘贴要导入的违禁词')
  importing.value = true
  try {
    const res = await batchAddForbiddenWord({
      lines,
      category: importForm.category,
      matchType: importForm.matchType,
      targetLibrary: importForm.targetLibrary,
    })
    const d = res.data || {}
    ElMessage.success(`导入完成：新增 ${d.added || 0} 条，跳过 ${d.skippedCount || 0} 条`)
    importVisible.value = false
    load()
  } catch {
    // 拦截器已提示
  } finally {
    importing.value = false
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
.forbidden-page {
  padding: 2px 0 8px;
}
.crumb {
  margin-bottom: 14px;
  font-size: 13px;
}
.crumb :deep(.el-breadcrumb__inner) {
  color: #909399;
  font-weight: 400;
}
.crumb :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
  color: #303133;
  font-weight: 500;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  padding: 12px 16px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  margin-bottom: 12px;
}
.toolbar .left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.cat {
  width: 130px;
}
.kw {
  width: 240px;
}
.search-btn {
  min-width: 76px;
}
.toolbar .right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.ghost-btn {
  color: #2f6bff;
  border-color: #d8e3ff;
  background: #f2f6ff;
}
.ghost-btn:hover {
  color: #2f6bff;
  border-color: #2f6bff;
  background: #e8f0ff;
}

.table-wrap {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 0 0 12px;
}
.word-table {
  font-size: 13px;
}
.word-table :deep(th.el-table__cell) {
  background: #fafbfc;
  color: #606266;
  font-weight: 500;
  font-size: 13px;
  padding: 9px 0;
}
.word-table :deep(td.el-table__cell) {
  padding: 8px 0;
  border-bottom: 1px solid #f0f2f5;
}
.word-table :deep(.el-table__row:hover > td.el-table__cell) {
  background: #f7f9fc;
}

.word-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}
.word {
  color: #303133;
  line-height: 1.5;
  word-break: break-all;
}
.word.off {
  color: #b1b3b8;
  text-decoration: line-through;
}

.time {
  color: #909399;
}
.adder {
  color: #606266;
}
.adder.sys {
  color: #a8abb2;
}

/* 单元格内下拉 / 输入：无边框，点一下才生效 */
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
  background: #f2f5fa;
}
.cell-select :deep(.el-select__selected-item) {
  font-size: 13px;
}
.cell-select.tag :deep(.el-select__wrapper) {
  background: #eef3ff;
  border-radius: 4px;
}
.cell-select.tag :deep(.el-select__selected-item) {
  color: #2f6bff;
}
.cell-select.plain :deep(.el-select__selected-item) {
  color: #606266;
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
  background: #f2f5fa;
}

.pager {
  display: flex;
  justify-content: flex-end;
  padding: 14px 16px 2px;
}

/* 弹窗表单 */
.word-form :deep(.el-form-item) {
  margin-bottom: 22px;
}
.word-form :deep(.el-form-item:last-child) {
  margin-bottom: 10px;
}
.word-form :deep(.el-form-item__label) {
  font-size: 14px;
  color: #1f2329;
  padding-right: 16px;
}
.word-form :deep(.el-form-item.is-required > .el-form-item__label::before) {
  color: #f56c6c;
  margin-right: 4px;
}
.word-form :deep(.el-input__wrapper),
.word-form :deep(.el-textarea__inner) {
  box-shadow: 0 0 0 1px #dcdfe6 inset;
  border-radius: 4px;
}
.word-form :deep(.el-input__wrapper.is-focus),
.word-form :deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 1px #2f6bff inset;
}
.word-form :deep(.el-input__inner),
.word-form :deep(.el-textarea__inner) {
  font-size: 14px;
}
.word-form .col {
  width: 100%;
}

.radio-line :deep(.el-radio) {
  height: 24px;
  margin-right: 40px;
}
.radio-line :deep(.el-radio__label) {
  font-size: 14px;
  color: #1f2329;
  padding-left: 8px;
}
.radio-line :deep(.el-radio.is-checked .el-radio__label) {
  color: #2f6bff;
  font-weight: 500;
}
.hint {
  margin: 8px 0 0;
  font-size: 12.5px;
  line-height: 1.5;
  color: #9aa0a6;
}
.word-dialog .cancel-btn,
.word-dialog .submit-btn {
  min-width: 76px;
  height: 34px;
}
.word-dialog .submit-btn {
  background: #2f6bff;
  border-color: #2f6bff;
}

.tip {
  margin: 0;
  font-size: 12px;
  color: #909399;
}
.import-tip {
  margin: 0 0 10px;
  font-size: 12px;
  color: #909399;
}
.import-opts {
  display: flex;
  gap: 10px;
  margin-top: 10px;
}

@media (max-width: 1100px) {
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }
  .toolbar .right {
    justify-content: flex-end;
  }
}
</style>

<style>
/* 弹窗外壳（dialog 被 teleport 到 body，用全局样式保证生效） */
.word-dialog .el-dialog__header {
  padding: 20px 24px 4px;
}
.word-dialog .el-dialog__title {
  font-size: 17px;
  font-weight: 500;
  color: #1f2329;
}
.word-dialog .el-dialog__headerbtn {
  top: 16px;
  right: 16px;
}
.word-dialog .el-dialog__body {
  padding: 14px 24px 2px;
}
.word-dialog .el-dialog__footer {
  padding: 4px 24px 20px;
}
.word-dialog .cancel-btn,
.word-dialog .submit-btn {
  min-width: 76px;
  height: 34px;
}
.word-dialog .submit-btn {
  background: #2f6bff;
  border-color: #2f6bff;
}
</style>
