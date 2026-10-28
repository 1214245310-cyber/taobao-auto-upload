<template>
  <div class="user-page">
    <div class="page-head">
      <div>
        <h2>用户管理</h2>
        <p class="sub">管理系统账号，可新增员工账号、重置密码</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openAdd">新增用户</el-button>
    </div>

    <el-card shadow="never">
      <el-table :data="users" v-loading="loading" stripe>
        <el-table-column label="头像" width="72">
          <template #default="{ row }">
            <img :src="row.avatar || defaultAvatar" class="avatar" alt="" />
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="nickname" label="昵称" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'" size="small">
              {{ row.role === 'ADMIN' ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === '1' ? 'warning' : 'success'" size="small">
              {{ row.status === '1' ? '已停用' : '正常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="160">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openReset(row)">重置密码</el-button>
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
          <el-input v-model="addForm.email" placeholder="选填" />
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
import { onMounted, reactive, ref } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getUsers, addUser, resetUserPwd } from '@/api/user'
import loginPhoto from '@/assets/login-photo.webp'

const defaultAvatar = loginPhoto
const users = ref<any[]>([])
const loading = ref(false)

const addVisible = ref(false)
const addLoading = ref(false)
const addForm = reactive({ username: '', nickname: '', email: '', password: '', role: 'USER' })

const resetVisible = ref(false)
const resetLoading = ref(false)
const resetRow = ref<any>(null)
const resetForm = reactive({ password: '', confirm: '' })

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
  return v ? v.replace('T', ' ').slice(0, 19) : '-'
}

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
.reset-tip {
  margin: 0 0 16px;
  color: #606266;
}
</style>
