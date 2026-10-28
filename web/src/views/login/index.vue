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
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { User, Lock, Key } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const user = useUserStore()
const router = useRouter()
const route = useRoute()
const loading = ref(false)
const codeImg = ref('')
const uuid = ref('')
const form = reactive({ username: '', password: '', code: '' })

async function refreshCode() {
  const res = await user.fetchCode()
  codeImg.value = 'data:image/png;base64,' + res.data.img
  uuid.value = res.data.uuid
}

async function handleLogin() {
  loading.value = true
  try {
    await user.login(form.username, form.password, form.code, uuid.value)
    const redirect = (route.query.redirect as string) || '/company-screen'
    router.replace(redirect)
  } finally {
    loading.value = false
    refreshCode()
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
}
.captcha-img {
  width: 120px;
  height: 40px;
  border-radius: 4px;
  cursor: pointer;
}
.login-btn {
  width: 100%;
}
</style>
