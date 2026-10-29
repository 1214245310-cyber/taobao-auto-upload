<template>
  <div class="forbidden-page">
    <div class="page-head">
      <div>
        <h2>违禁词管理</h2>
        <p class="sub">解析商品时自动校验标题，命中即提示。此处记录每个词是谁添加的。</p>
      </div>
      <el-button v-if="isAdmin" type="primary" :icon="Plus" @click="openAdd">新增违禁词</el-button>
    </div>

    <el-card shadow="never">
      <el-table :data="words" v-loading="loading" stripe>
        <el-table-column prop="word" label="违禁词" min-width="140">
          <template #default="{ row }">
            <span class="word">{{ row.word }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="110">
          <template #default="{ row }">{{ row.category || '—' }}</template>
        </el-table-column>
        <el-table-column label="等级" width="120">
          <template #default="{ row }">
            <el-tag :type="levelTag(row.level)" size="small">{{ levelText(row.level) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.enabled === '1' ? 'success' : 'info'" size="small">
              {{ row.enabled === '1' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="添加人" width="130">
          <template #default="{ row }">
            <span :class="{ sys: row.createdBy === 'system' }">{{ row.createdBy || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="添加时间" min-width="165">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="140">
          <template #default="{ row }">{{ row.remark || '—' }}</template>
        </el-table-column>
        <el-table-column v-if="isAdmin" label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link :type="row.enabled === '1' ? 'warning' : 'success'" @click="toggle(row)">
              {{ row.enabled === '1' ? '停用' : '启用' }}
            </el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="addVisible" title="新增违禁词" width="440px" @closed="resetAdd">
      <el-form :model="addForm" label-width="80px">
        <el-form-item label="违禁词" required>
          <el-input v-model="addForm.word" placeholder="如：最便宜、绝对、包治百病" maxlength="64" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="addForm.category" style="width: 100%" filterable allow-create default-first-option placeholder="选择或直接输入">
            <el-option label="极限词" value="极限词" />
            <el-option label="资质词" value="资质词" />
            <el-option label="功效词" value="功效词" />
            <el-option label="品牌词" value="品牌词" />
          </el-select>
        </el-form-item>
        <el-form-item label="等级">
          <el-select v-model="addForm.level" style="width: 100%">
            <el-option label="违规词（最严重）" :value="3" />
            <el-option label="敏感词" :value="2" />
            <el-option label="平台禁用词" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="addForm.remark" type="textarea" :rows="2" placeholder="选填，如：来源、依据" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" :loading="addLoading" @click="handleAdd">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { addForbiddenWord, deleteForbiddenWord, getForbiddenWords, toggleForbiddenWord } from '@/api/forbidden-word'
import { useUserStore } from '@/store/user'

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

const user = useUserStore()
const isAdmin = computed(() => user.userInfo.role === 'ADMIN')

const words = ref<WordRow[]>([])
const loading = ref(false)
const addVisible = ref(false)
const addLoading = ref(false)
const addForm = reactive({ word: '', category: '极限词', level: 2, remark: '' })

function levelText(level: number) {
  if (level === 3) return '违规词'
  if (level === 1) return '平台禁用词'
  return '敏感词'
}
function levelTag(level: number) {
  if (level === 3) return 'danger'
  if (level === 2) return 'warning'
  return 'info'
}
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

function openAdd() {
  addVisible.value = true
}
function resetAdd() {
  addForm.word = ''
  addForm.category = '极限词'
  addForm.level = 2
  addForm.remark = ''
}

async function handleAdd() {
  const word = addForm.word.trim()
  if (!word) return ElMessage.warning('请输入违禁词')
  addLoading.value = true
  try {
    await addForbiddenWord({
      word,
      category: addForm.category || undefined,
      level: addForm.level,
      remark: addForm.remark || undefined,
    })
    ElMessage.success('已添加')
    addVisible.value = false
    load()
  } catch {
    // 拦截器已提示
  } finally {
    addLoading.value = false
  }
}

async function toggle(row: WordRow) {
  try {
    await toggleForbiddenWord(row.id, row.enabled !== '1')
    ElMessage.success(row.enabled === '1' ? '已停用' : '已启用')
    load()
  } catch {
    // 拦截器已提示
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

onMounted(async () => {
  if (!user.userInfo.role) {
    try {
      await user.fetchInfo()
    } catch {
      // 忽略，按非管理员渲染
    }
  }
  load()
})
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
.word {
  font-weight: 600;
}
.sys {
  color: #909399;
}
</style>
