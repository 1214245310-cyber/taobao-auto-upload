<template>
  <div class="user-page">
    <div class="page-head">
      <div>
        <h2>用户管理</h2>
        <p class="sub">管理系统账号：编辑资料与邮箱、分配角色、停用或删除（仅管理员可操作）</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openAdd">新增用户</el-button>
    </div>

    <el-card shadow="never">
      <el-table :data="users" v-loading="loading" stripe>
        <el-table-column label="头像" width="68">
          <template #default="{ row }">
            <img :src="row.avatar || defaultAvatar" class="avatar" alt="" />
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="nickname" label="昵称" min-width="110" />
        <el-table-column label="邮箱" min-width="190">
          <template #default="{ row }">
            <span v-if="row.email" class="email">{{ row.email }}</span>
            <span v-else class="muted">未绑定</span>
            <el-tag v-if="row.email && row.emailVerified !== '1'" size="small" type="warning" class="vt">
              未验证
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="96">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'" size="small">
              {{ row.role === 'ADMIN' ? '管理员' : '普通用户' }}
            </el-tag>
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
        <el-table-column label="备注" min-width="130">
          <template #default="{ row }">
            <span v-if="row.remark">{{ row.remark }}</span>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="最近登录" width="160">
          <template #default="{ row }">
            <span class="muted">{{ formatTime(row.lastLoginAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="176" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="primary" @click="openReset(row)">重置密码</el-button>
            <el-button link type="danger" :disabled="row.id === myId" @click="remove(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增用户 -->
    <el-dialog v-model="addVisible" title="新增用户" width="440px" @closed="resetAdd">
      <el-form :model="addForm" label-width="80px" label-position="right">
        <el-form-item label="用户名" required>
          <el-input v-model="addForm.username" placeholder="3-20 位字母、数字或下划线" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="addForm.nickname" placeholder="不填则与用户名相同" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="addForm.email" placeholder="选填，填了才能用「忘记密码」" />
        </el-form-item>
        <el-form-item label="密码" required>
          <el-input v-model="addForm.password" type="password" show-password placeholder="6-32 位" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="addForm.role" style="width: 100%">
            <el-option label="普通用户" value="USER" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" :loading="addLoading" @click="handleAdd">确定</el-button>
      </template>
    </el-dialog>

    <!-- 编辑用户 -->
    <el-dialog v-model="editVisible" title="编辑用户" width="460px" @closed="resetEdit">
      <el-form :model="editForm" label-width="86px" label-position="right">
        <el-form-item label="用户名">
          <el-input :model-value="editForm.username" disabled />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="editForm.nickname" placeholder="不填则与用户名相同" maxlength="64" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="editForm.email" placeholder="留空表示解绑；填了才能用「忘记密码」" />
          <p class="hint">
            管理员代填的邮箱标记为未验证，用户可在登录页「忘记密码」发一次验证码自证。
          </p>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editForm.remark" placeholder="例如：财务-李四" maxlength="255" />
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
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="editLoading" @click="handleEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码 -->
    <el-dialog v-model="resetVisible" title="重置密码" width="420px" @closed="resetReset">
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
import { computed, onMounted, reactive, ref } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUsers, addUser, deleteUser, resetUserPwd, updateUser } from '@/api/user'
import { useUserStore } from '@/store/user'
import loginPhoto from '@/assets/login-photo.webp'

const defaultAvatar = loginPhoto
const userStore = useUserStore()
const myId = computed(() => Number(userStore.userInfo.userId || 0))

const users = ref<any[]>([])
const loading = ref(false)

const addVisible = ref(false)
const addLoading = ref(false)
const addForm = reactive({ username: '', nickname: '', email: '', password: '', role: 'USER' })

const editVisible = ref(false)
const editLoading = ref(false)
const editRow = ref<any>(null)
const editForm = reactive({
  id: 0,
  username: '',
  nickname: '',
  email: '',
  remark: '',
  role: 'USER',
  status: '0',
})

const resetVisible = ref(false)
const resetLoading = ref(false)
const resetRow = ref<any>(null)
const resetForm = reactive({ password: '', confirm: '' })

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/

async function loadUsers() {
  loading.value = true
  try {
    const res = await getUsers()
    users.value = res.data || []
  } catch {
    // 错误提示已由 request 拦截器统一弹出
  } finally {
    loading.value = false
  }
}

function formatTime(v: string) {
  return v ? v.replace('T', ' ').slice(0, 19) : '从未登录'
}

/* ---------------- 新增 ---------------- */
function openAdd() {
  addVisible.value = true
}
function resetAdd() {
  addForm.username = ''
  addForm.nickname = ''
  addForm.email = ''
  addForm.password = ''
  addForm.role = 'USER'
}
async function handleAdd() {
  if (!/^[a-zA-Z0-9_]{3,20}$/.test(addForm.username)) {
    return ElMessage.warning('用户名需为 3-20 位字母、数字或下划线')
  }
  if (addForm.email && !EMAIL_RE.test(addForm.email)) {
    return ElMessage.warning('邮箱格式不正确')
  }
  if (!addForm.password || addForm.password.length < 6 || addForm.password.length > 32) {
    return ElMessage.warning('密码长度需为 6-32 位')
  }
  addLoading.value = true
  try {
    await addUser({
      username: addForm.username,
      nickname: addForm.nickname,
      email: addForm.email,
      password: addForm.password,
      role: addForm.role,
    })
    ElMessage.success('新增成功')
    addVisible.value = false
    loadUsers()
  } catch {
    // 错误提示已由 request 拦截器统一弹出
  } finally {
    addLoading.value = false
  }
}

/* ---------------- 编辑 ---------------- */
function openEdit(row: any) {
  editRow.value = row
  editForm.id = row.id
  editForm.username = row.username
  editForm.nickname = row.nickname || ''
  editForm.email = row.email || ''
  editForm.remark = row.remark || ''
  editForm.role = row.role || 'USER'
  editForm.status = row.status || '0'
  editVisible.value = true
}
function resetEdit() {
  editRow.value = null
  editForm.id = 0
  editForm.email = ''
  editForm.nickname = ''
  editForm.remark = ''
}
async function handleEdit() {
  if (editForm.email && !EMAIL_RE.test(editForm.email)) {
    return ElMessage.warning('邮箱格式不正确')
  }
  editLoading.value = true
  try {
    await updateUser({
      id: editForm.id,
      nickname: editForm.nickname,
      email: editForm.email,
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

/* ---------------- 状态开关 ---------------- */
async function toggleStatus(row: any, value: any) {
  try {
    await updateUser({ id: row.id, status: value })
    ElMessage.success(value === '1' ? '已停用，该账号会被强制下线' : '已启用')
  } catch {
    // 失败时把开关拨回去
    row.status = value === '1' ? '0' : '1'
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

onMounted(loadUsers)
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
.reset-tip {
  margin: 0 0 16px;
  color: #606266;
}
.hint {
  margin: 4px 0 0;
  font-size: 12px;
  line-height: 1.5;
  color: #909399;
}
</style>
