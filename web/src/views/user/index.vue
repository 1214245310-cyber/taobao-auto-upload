<template>
  <div class="user-page">
    <div class="page-head">
      <div>
        <h2>用户管理</h2>
        <p class="sub">新增用户只需填邮箱并完成验证，登录名自动取邮箱前缀；角色、备注可直接改（仅管理员可操作）</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openAdd">新增用户</el-button>
    </div>

    <el-card shadow="never">
      <el-table :data="users" v-loading="loading" stripe>
        <el-table-column label="头像" width="64">
          <template #default="{ row }">
            <img :src="row.avatar || defaultAvatar" class="avatar" alt="" />
          </template>
        </el-table-column>
        <el-table-column prop="username" label="登录名" min-width="110" />
        <el-table-column prop="nickname" label="昵称" min-width="100" />
        <el-table-column label="邮箱" min-width="200">
          <template #default="{ row }">
            <span v-if="row.email" class="email">{{ row.email }}</span>
            <span v-else class="muted">未绑定</span>
            <el-tag v-if="row.email && row.emailVerified !== '1'" size="small" type="warning" class="vt">
              未验证
            </el-tag>
            <el-tag v-else-if="row.email" size="small" type="success" class="vt">已验证</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="120">
          <template #default="{ row }">
            <el-select
              v-model="row.role"
              size="small"
              class="cell-select"
              :disabled="row.id === myId"
              @change="(v: string) => patch(row, { role: v })"
            >
              <el-option label="普通用户" value="USER" />
              <el-option label="管理员" value="ADMIN" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="104">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              active-value="0"
              inactive-value="1"
              inline-prompt
              active-text="正常"
              inactive-text="停用"
              :disabled="row.id === myId"
              @change="(v: any) => toggleStatus(row, v)"
            />
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="160">
          <template #default="{ row }">
            <el-input
              v-model="row.remark"
              size="small"
              class="remark-input"
              placeholder="点击填写"
              maxlength="255"
              @change="(v: string) => saveRemark(row, v)"
            />
          </template>
        </el-table-column>
        <el-table-column label="最近登录" width="158">
          <template #default="{ row }">
            <span class="muted">{{ formatTime(row.lastLoginAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="206" fixed="right">
          <template #default="{ row }">
            <div class="ops">
              <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button link type="primary" @click="openReset(row)">重置密码</el-button>
              <el-button link type="danger" :disabled="row.id === myId" @click="remove(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增用户：邮箱 + 验证码 + 角色 + 密码 -->
    <el-dialog v-model="addVisible" title="新增用户" width="470px" class="word-dialog" @closed="resetAdd">
      <el-form :model="addForm" label-width="92px" label-position="right">
        <el-form-item label="邮箱" required>
          <el-input v-model="addForm.email" placeholder="user@qq.com（用于登录与找回密码）" />
          <p class="hint">登录名自动使用邮箱前缀：<b>{{ usernamePreview }}</b></p>
        </el-form-item>
        <el-form-item label="邮箱验证码" required>
          <div class="code-row">
            <el-input v-model="addForm.emailCode" placeholder="6 位数字" maxlength="6" />
            <el-button
              class="code-btn"
              :loading="addCodeSending"
              :disabled="addCodeLeft > 0"
              @click="sendCode('add')"
            >
              {{ addCodeLeft > 0 ? addCodeLeft + 's' : '获取验证码' }}
            </el-button>
          </div>
          <p class="hint">验证通过才会绑定该邮箱，未收到看下垃圾箱。</p>
        </el-form-item>
        <el-form-item label="角色" required>
          <el-select v-model="addForm.role" style="width: 100%">
            <el-option label="普通用户" value="USER" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="初始密码" required>
          <div class="code-row">
            <el-input v-model="addForm.password" type="password" show-password placeholder="6-32 位" />
            <el-button class="code-btn" @click="fillRandomPwd">随机生成</el-button>
          </div>
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="addForm.nickname" placeholder="选填，默认与登录名相同" maxlength="64" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" :loading="addLoading" @click="handleAdd">确定</el-button>
      </template>
    </el-dialog>

    <!-- 编辑用户 -->
    <el-dialog v-model="editVisible" title="编辑用户" width="480px" class="word-dialog" @closed="resetEdit">
      <el-form :model="editForm" label-width="92px" label-position="right">
        <el-form-item label="登录名">
          <el-input :model-value="editForm.username" disabled />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="editForm.nickname" placeholder="不填则与登录名相同" maxlength="64" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="editForm.email" placeholder="留空表示解绑邮箱" />
        </el-form-item>
        <el-form-item v-if="emailChanged && editForm.email" label="邮箱验证码" required>
          <div class="code-row">
            <el-input v-model="editForm.emailCode" placeholder="6 位数字" maxlength="6" />
            <el-button
              class="code-btn"
              :loading="editCodeSending"
              :disabled="editCodeLeft > 0"
              @click="sendCode('edit')"
            >
              {{ editCodeLeft > 0 ? editCodeLeft + 's' : '获取验证码' }}
            </el-button>
          </div>
          <p class="hint">换绑邮箱必须验证通过，验证后邮箱标记为「已验证」。</p>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="editForm.role" style="width: 100%">
            <el-option label="普通用户" value="USER" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="editForm.status">
            <el-radio value="0">正常</el-radio>
            <el-radio value="1">停用（踢下线且不能登录）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editForm.remark" placeholder="例如：财务-李四" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="editLoading" @click="handleEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码 -->
    <el-dialog v-model="resetVisible" title="重置密码" width="420px" class="word-dialog" @closed="resetReset">
      <p class="reset-tip">将把用户 <b>{{ resetRow?.username }}</b> 的密码重置为下方新密码。</p>
      <el-form :model="resetForm" label-width="80px">
        <el-form-item label="新密码" required>
          <el-input v-model="resetForm.password" type="password" show-password placeholder="6-32 位" />
        </el-form-item>
        <el-form-item label="确认密码" required>
          <el-input v-model="resetForm.confirm" type="password" show-password placeholder="再次输入" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" :loading="resetLoading" @click="handleReset">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUsers, addUser, deleteUser, resetUserPwd, updateUser, sendBindCode } from '@/api/user'
import { useUserStore } from '@/store/user'
import loginPhoto from '@/assets/login-photo.webp'

const defaultAvatar = loginPhoto
const userStore = useUserStore()
const myId = computed(() => Number(userStore.userInfo.userId || 0))

const users = ref<any[]>([])
const loading = ref(false)
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/

/* ---------------- 新增 ---------------- */
const addVisible = ref(false)
const addLoading = ref(false)
const addCodeSending = ref(false)
const addCodeLeft = ref(0)
const addForm = reactive({ email: '', emailCode: '', password: '', nickname: '', role: 'USER' })

/* ---------------- 编辑 ---------------- */
const editVisible = ref(false)
const editLoading = ref(false)
const editCodeSending = ref(false)
const editCodeLeft = ref(0)
const editRow = ref<any>(null)
const editForm = reactive({
  id: 0,
  username: '',
  nickname: '',
  email: '',
  emailCode: '',
  remark: '',
  role: 'USER',
  status: '0',
})

/* ---------------- 重置密码 ---------------- */
const resetVisible = ref(false)
const resetLoading = ref(false)
const resetRow = ref<any>(null)
const resetForm = reactive({ password: '', confirm: '' })

/* ---------------- 倒计时 ---------------- */
let addTimer: number | undefined
let editTimer: number | undefined

function startCountdown(kind: 'add' | 'edit', seconds: number) {
  const left = kind === 'add' ? addCodeLeft : editCodeLeft
  left.value = seconds
  const tick = () => {
    left.value -= 1
    if (left.value <= 0) {
      if (kind === 'add' && addTimer) window.clearInterval(addTimer)
      if (kind === 'edit' && editTimer) window.clearInterval(editTimer)
    }
  }
  if (kind === 'add') {
    if (addTimer) window.clearInterval(addTimer)
    addTimer = window.setInterval(tick, 1000)
  } else {
    if (editTimer) window.clearInterval(editTimer)
    editTimer = window.setInterval(tick, 1000)
  }
}

const usernamePreview = computed(() => {
  const email = addForm.email.trim()
  if (!EMAIL_RE.test(email)) return '填写邮箱后自动生成'
  const prefix = email.split('@')[0].replace(/[^a-zA-Z0-9_]/g, '')
  return prefix.length >= 3 ? prefix : prefix + 'user'
})

async function sendCode(kind: 'add' | 'edit') {
  const email = (kind === 'add' ? addForm.email : editForm.email).trim()
  if (!EMAIL_RE.test(email)) {
    return ElMessage.warning('请先填写正确的邮箱')
  }
  if (kind === 'add') addCodeSending.value = true
  else editCodeSending.value = true
  try {
    const res = await sendBindCode(email)
    ElMessage.success('验证码已发送，请查收邮件（没收到看下垃圾箱）')
    startCountdown(kind, (res.data?.cooldown as number) || 60)
  } catch {
    // 错误提示已由 request 拦截器统一弹出
  } finally {
    addCodeSending.value = false
    editCodeSending.value = false
  }
}

function fillRandomPwd() {
  const chars = 'abcdefghjkmnpqrstuvwxyzABCDEFGHJKMNPQRSTUVWXYZ23456789'
  let s = ''
  for (let i = 0; i < 10; i++) s += chars[Math.floor(Math.random() * chars.length)]
  addForm.password = s
  ElMessage.success(`已生成初始密码：${s}`)
}

function openAdd() {
  addVisible.value = true
}

function resetAdd() {
  addForm.email = ''
  addForm.emailCode = ''
  addForm.password = ''
  addForm.nickname = ''
  addForm.role = 'USER'
  addCodeLeft.value = 0
  if (addTimer) window.clearInterval(addTimer)
}

async function handleAdd() {
  if (!EMAIL_RE.test(addForm.email.trim())) {
    return ElMessage.warning('请填写正确的邮箱')
  }
  if (!/^\d{6}$/.test(addForm.emailCode)) {
    return ElMessage.warning('请获取并填写 6 位邮箱验证码')
  }
  if (!addForm.password || addForm.password.length < 6 || addForm.password.length > 32) {
    return ElMessage.warning('初始密码长度需为 6-32 位')
  }
  addLoading.value = true
  try {
    await addUser({
      email: addForm.email.trim(),
      emailCode: addForm.emailCode.trim(),
      password: addForm.password,
      nickname: addForm.nickname,
      role: addForm.role,
    })
    ElMessage.success('新增成功，邮箱已验证并绑定')
    addVisible.value = false
    loadUsers()
  } catch {
    // 错误提示已由 request 拦截器统一弹出
  } finally {
    addLoading.value = false
  }
}

/* ---------------- 编辑 ---------------- */
const emailChanged = computed(() => {
  if (!editRow.value) return false
  const origin = (editRow.value.email || '').trim().toLowerCase()
  return editForm.email.trim().toLowerCase() !== origin
})

function openEdit(row: any) {
  editRow.value = row
  editForm.id = row.id
  editForm.username = row.username
  editForm.nickname = row.nickname || ''
  editForm.email = row.email || ''
  editForm.emailCode = ''
  editForm.remark = row.remark || ''
  editForm.role = row.role || 'USER'
  editForm.status = row.status || '0'
  editVisible.value = true
}

function resetEdit() {
  editRow.value = null
  editForm.id = 0
  editForm.email = ''
  editForm.emailCode = ''
  editForm.nickname = ''
  editForm.remark = ''
  editCodeLeft.value = 0
  if (editTimer) window.clearInterval(editTimer)
}

async function handleEdit() {
  const email = editForm.email.trim()
  if (email && !EMAIL_RE.test(email)) {
    return ElMessage.warning('邮箱格式不正确')
  }
  if (emailChanged.value && email && !/^\d{6}$/.test(editForm.emailCode)) {
    return ElMessage.warning('换绑邮箱需要先获取并填写验证码')
  }
  editLoading.value = true
  try {
    await updateUser({
      id: editForm.id,
      nickname: editForm.nickname,
      email,
      emailCode: emailChanged.value && email ? editForm.emailCode : undefined,
      remark: editForm.remark,
      role: editForm.role,
      status: editForm.status,
    })
    ElMessage.success('已保存')
    editVisible.value = false
    loadUsers()
  } catch {
    // 错误提示已由 request 拦截器统一弹出
  } finally {
    editLoading.value = false
  }
}

/* ---------------- 表格内直接改 ---------------- */
async function patch(row: any, data: Record<string, any>) {
  try {
    await updateUser({ id: row.id, ...data })
    ElMessage.success('已更新')
  } catch {
    loadUsers()
  }
}

async function toggleStatus(row: any, value: any) {
  try {
    await updateUser({ id: row.id, status: value })
    ElMessage.success(value === '1' ? '已停用，该账号会被强制下线' : '已启用')
  } catch {
    row.status = value === '1' ? '0' : '1'
  }
}

const remarkOrigin = new Map<number, string>()

async function saveRemark(row: any, value: string) {
  const origin = remarkOrigin.get(row.id) ?? ''
  const next = (value ?? '').trim()
  if (next === origin) return
  try {
    await updateUser({ id: row.id, remark: next })
    remarkOrigin.set(row.id, next)
    row.remark = next
    ElMessage.success('备注已保存')
  } catch {
    row.remark = origin
  }
}

/* ---------------- 删除 ---------------- */
async function remove(row: any) {
  try {
    await ElMessageBox.confirm(
      `确定删除用户「${row.username}」吗？删除后该账号的登录态立即失效，且不可恢复。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  try {
    await deleteUser(row.id)
    ElMessage.success('已删除')
    loadUsers()
  } catch {
    // 错误提示已由 request 拦截器统一弹出
  }
}

/* ---------------- 重置密码 ---------------- */
function openReset(row: any) {
  resetRow.value = row
  resetVisible.value = true
}
function resetReset() {
  resetForm.password = ''
  resetForm.confirm = ''
  resetRow.value = null
}
async function handleReset() {
  if (!resetForm.password || resetForm.password.length < 6 || resetForm.password.length > 32) {
    return ElMessage.warning('新密码长度需为 6-32 位')
  }
  if (resetForm.password !== resetForm.confirm) {
    return ElMessage.warning('两次输入的密码不一致')
  }
  resetLoading.value = true
  try {
    await resetUserPwd(resetRow.value.id, resetForm.password)
    ElMessage.success('密码已重置')
    resetVisible.value = false
  } catch {
    // 错误提示已由 request 拦截器统一弹出
  } finally {
    resetLoading.value = false
  }
}

/* ---------------- 数据加载 ---------------- */
function formatTime(v: string) {
  return v ? v.replace('T', ' ').slice(0, 19) : '从未登录'
}

async function loadUsers() {
  loading.value = true
  try {
    const res = await getUsers()
    users.value = res.data || []
    remarkOrigin.clear()
    users.value.forEach((u: any) => remarkOrigin.set(u.id, u.remark || ''))
  } catch {
    // 错误提示已由 request 拦截器统一弹出
  } finally {
    loading.value = false
  }
}

onMounted(loadUsers)
onUnmounted(() => {
  if (addTimer) window.clearInterval(addTimer)
  if (editTimer) window.clearInterval(editTimer)
})
</script>

<style scoped>
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.page-head h2 {
  margin: 0;
}
.sub {
  margin: 4px 0 0;
  color: #909399;
  font-size: 13px;
}
.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  background: #f0f2f5;
}
.email {
  color: #303133;
}
.muted {
  color: #a8abb2;
}
.vt {
  margin-left: 6px;
  transform: scale(0.92);
}
.ops {
  display: flex;
  align-items: center;
  gap: 10px;
  white-space: nowrap;
}
.ops :deep(.el-button + .el-button) {
  margin-left: 0;
}
.cell-select {
  width: 100%;
}
.cell-select :deep(.el-select__wrapper) {
  box-shadow: none;
  background: transparent;
  padding-left: 0;
}
.remark-input :deep(.el-input__wrapper) {
  box-shadow: none;
  background: transparent;
  padding-left: 0;
}
.remark-input :deep(.el-input__wrapper):hover,
.remark-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #dcdfe6 inset;
  background: #fff;
  border-radius: 4px;
  padding-left: 8px;
}
.code-row {
  display: flex;
  gap: 8px;
  width: 100%;
}
.code-btn {
  flex: 0 0 auto;
  min-width: 96px;
}
.hint {
  margin: 4px 0 0;
  font-size: 12px;
  line-height: 1.5;
  color: #909399;
}
.reset-tip {
  margin: 0 0 16px;
  color: #606266;
}
</style>
