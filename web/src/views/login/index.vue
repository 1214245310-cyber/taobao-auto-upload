<template>
  <div class="login-page">
    <div class="login-card">
      <h2>欢迎回来 👋🏻</h2>
      <p class="subtitle">请输入账户信息</p>
      <el-form :model="form" @submit.prevent="handleLogin">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <div class="captcha-row">
            <el-input v-model="form.code" placeholder="验证码" :prefix-icon="Key" />
            <img :src="codeImg" class="captcha-img" alt="验证码" @click="refreshCode" />
          </div>
        </el-form-item>
        <el-button type="primary" :loading="loading" native-type="submit" class="login-btn">
          登录
        </el-button>
      </el-form>
      <div class="switch-tip">
        还没有账号？
        <el-link type="primary" :underline="false" @click="openRegister">立即注册</el-link>
      </div>
    </div>

    <el-dialog
      v-model="registerVisible"
      title="注册新账号"
      width="400px"
      :close-on-click-modal="false"
      @closed="resetRegisterForm"
    >
      <el-form :model="regForm" label-position="top">
        <el-form-item label="用户名">
          <el-input v-model="regForm.username" placeholder="3-20 位字母、数字或下划线" :prefix-icon="User" />
        </el-form-item>
        <el-form-item label="昵称（选填）">
          <el-input v-model="regForm.nickname" placeholder="不填则与用户名相同" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="regForm.password"
            type="password"
            show-password
            placeholder="6-32 位"
            :prefix-icon="Lock"
          />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input
            v-model="regForm.confirmPassword"
            type="password"
            show-password
            placeholder="再次输入密码"
            :prefix-icon="Lock"
          />
        </el-form-item>
        <el-form-item label="验证码">
          <div class="captcha-row">
            <el-input v-model="regForm.code" placeholder="请输入验证码" :prefix-icon="Key" />
            <img :src="regCodeImg" class="captcha-img" alt="验证码" @click="refreshRegCode" />
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="registerVisible = false">取消</el-button>
        <el-button type="primary" :loading="regLoading" @click="handleRegister">注册</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { User, Lock, Key } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { register as apiRegister } from '@/api/auth'

const user = useUserStore()
const router = useRouter()
const route = useRoute()

const loading = ref(false)
const codeImg = ref('')
const uuid = ref('')
const form = reactive({ username: '', password: '', code: '' })

const registerVisible = ref(false)
const regLoading = ref(false)
const regCodeImg = ref('')
const regUuid = ref('')
const regForm = reactive({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: '',
  code: '',
})

async function refreshCode() {
  const res = await user.fetchCode()
  codeImg.value = 'data:image/png;base64,' + res.data.img
  uuid.value = res.data.uuid as string
}

async function refreshRegCode() {
  const res = await user.fetchCode()
  regCodeImg.value = 'data:image/png;base64,' + res.data.img
  regUuid.value = res.data.uuid as string
}

async function handleLogin() {
  loading.value = true
  try {
    await user.login(form.username, form.password, form.code, uuid.value)
    ElMessage.success('登录成功')
    const redirect = (route.query.redirect as string) || '/company-screen'
    router.replace(redirect)
  } catch {
    // 错误提示已由 request 拦截器统一弹出
  } finally {
    loading.value = false
    form.code = ''
    refreshCode()
  }
}

function openRegister() {
  registerVisible.value = true
  refreshRegCode()
}

function resetRegisterForm() {
  regForm.username = ''
  regForm.nickname = ''
  regForm.password = ''
  regForm.confirmPassword = ''
  regForm.code = ''
}

async function handleRegister() {
  if (!/^[a-zA-Z0-9_]{3,20}$/.test(regForm.username)) {
    return ElMessage.warning('用户名需为 3-20 位字母、数字或下划线')
  }
  if (regForm.password.length < 6 || regForm.password.length > 32) {
    return ElMessage.warning('密码长度需为 6-32 位')
  }
  if (regForm.password !== regForm.confirmPassword) {
    return ElMessage.warning('两次输入的密码不一致')
  }
  if (!regForm.code) {
    return ElMessage.warning('请输入验证码')
  }
  regLoading.value = true
  try {
    await apiRegister({
      username: regForm.username,
      nickname: regForm.nickname,
      password: regForm.password,
      confirmPassword: regForm.confirmPassword,
      code: regForm.code,
      uuid: regUuid.value,
    })
    ElMessage.success('注册成功，请使用新账号登录')
    registerVisible.value = false
    form.username = regForm.username
    form.password = ''
  } catch {
    // 错误提示已由 request 拦截器统一弹出
  } finally {
    regLoading.value = false
    refreshRegCode()
  }
}

onMounted(refreshCode)
</script>

<style scoped>
.login-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1f2937, #374151);
}
.login-card {
  width: 360px;
  background: #fff;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}
.login-card h2 {
  margin: 0 0 4px;
}
.subtitle {
  color: #909399;
  margin: 0 0 24px;
}
.captcha-row {
  display: flex;
  gap: 12px;
  width: 100%;
}
.captcha-img {
  width: 120px;
  height: 40px;
  border-radius: 4px;
  cursor: pointer;
  flex-shrink: 0;
}
.login-btn {
  width: 100%;
}
.switch-tip {
  margin-top: 16px;
  text-align: center;
  font-size: 13px;
  color: #909399;
}
</style>
