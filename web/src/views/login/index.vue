<template>
  <div class="login-page">
    <div class="bg-layer" :style="{ backgroundImage: `url(${loginBg})` }"></div>
    <div class="bg-mask"></div>

    <div class="login-card">
      <div class="brand">
        <div class="brand-logo">LOA</div>
        <div class="brand-name">淘宝自动上货系统</div>
      </div>

      <h2 class="title">欢迎回来 👋🏻</h2>
      <p class="subtitle">请输入账户信息以继续</p>

      <el-form :model="form" @submit.prevent="handleLogin">
        <el-form-item>
          <el-input
            v-model="form.username"
            placeholder="用户名 / 注册邮箱"
            :prefix-icon="User"
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            :prefix-icon="Lock"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <div class="remember-row">
          <span class="remember-tip">默认已记住账号</span>
          <div class="remember-right">
            <el-checkbox v-model="rememberPassword">记住密码</el-checkbox>
            <el-link type="primary" :underline="false" class="forgot-link" @click="openForgot">
              忘记密码？
            </el-link>
          </div>
        </div>
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

      <p class="login-hint">用户名和注册邮箱都能登录</p>

      <div class="switch-tip">
        还没有账号？
        <el-link type="primary" :underline="false" @click="openRegister">立即注册</el-link>
      </div>

      <div class="copyright">© 2026 LOA · 淘宝自动上货系统</div>
    </div>

    <el-dialog
      v-model="registerVisible"
      title="注册新账号"
      width="420px"
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
        <el-form-item label="邮箱">
          <el-input v-model="regForm.email" placeholder="例如 yourname@qq.com" :prefix-icon="Message" />
        </el-form-item>
        <el-form-item label="邮箱验证码">
          <div class="captcha-row">
            <el-input
              v-model="regForm.emailCode"
              placeholder="6 位数字"
              maxlength="6"
              :prefix-icon="Key"
            />
            <el-button
              class="code-btn"
              :disabled="countdown > 0"
              :loading="codeSending"
              @click="sendCode"
            >
              {{ countdown > 0 ? countdown + 's' : '获取验证码' }}
            </el-button>
          </div>
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
      </el-form>
      <template #footer>
        <el-button @click="registerVisible = false">取消</el-button>
        <el-button type="primary" :loading="regLoading" @click="handleRegister">注册</el-button>
      </template>
    </el-dialog>

    <!-- 忘记密码：注册邮箱 + 邮箱验证码 -->
    <el-dialog
      v-model="forgotVisible"
      title="忘记密码"
      width="420px"
      :close-on-click-modal="false"
      @closed="resetForgotForm"
    >
      <p class="forgot-tip">
        需要同时填对<strong>账号</strong>和该账号绑定的<strong>邮箱</strong>，我们才会发验证码；
        验证通过即可设置新密码。未绑定邮箱的账号请联系管理员重置。
      </p>
      <el-form :model="forgotForm" label-position="top">
        <el-form-item label="账号">
          <el-input
            v-model="forgotForm.username"
            placeholder="用户名，例如 xby1111"
            :prefix-icon="User"
          />
        </el-form-item>
        <el-form-item label="绑定邮箱">
          <el-input
            v-model="forgotForm.email"
            placeholder="该账号注册时填写的邮箱"
            :prefix-icon="Message"
          />
        </el-form-item>
        <el-form-item label="邮箱验证码">
          <div class="captcha-row">
            <el-input
              v-model="forgotForm.emailCode"
              placeholder="6 位数字"
              maxlength="6"
              :prefix-icon="Key"
            />
            <el-button
              class="code-btn"
              :disabled="forgotCountdown > 0"
              :loading="forgotSending"
              @click="sendForgotCode"
            >
              {{ forgotCountdown > 0 ? forgotCountdown + 's' : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="新密码">
          <el-input
            v-model="forgotForm.newPassword"
            type="password"
            show-password
            placeholder="6-32 位"
            :prefix-icon="Lock"
          />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input
            v-model="forgotForm.confirmPassword"
            type="password"
            show-password
            placeholder="再次输入新密码"
            :prefix-icon="Lock"
            @keyup.enter="handleResetPassword"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="forgotVisible = false">取消</el-button>
        <el-button type="primary" :loading="forgotLoading" @click="handleResetPassword">
          重置密码
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { User, Lock, Key, Message } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { register as apiRegister, resetPasswordByEmail, sendEmailCode, sendResetCode } from '@/api/auth'
import loginBg from '@/assets/login-bg.webp'

const user = useUserStore()
const router = useRouter()
const route = useRoute()

const loading = ref(false)
const codeImg = ref('')
const uuid = ref('')
const form = reactive({ username: '', password: '', code: '' })

const registerVisible = ref(false)
const regLoading = ref(false)
const codeSending = ref(false)
const countdown = ref(0)
let countdownTimer: number | undefined
const regForm = reactive({
  username: '',
  nickname: '',
  email: '',
  emailCode: '',
  password: '',
  confirmPassword: '',
})

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/

// 忘记密码
const forgotVisible = ref(false)
const forgotLoading = ref(false)
const forgotSending = ref(false)
const forgotCountdown = ref(0)
let forgotTimer: number | undefined
const forgotForm = reactive({
  username: '',
  email: '',
  emailCode: '',
  newPassword: '',
  confirmPassword: '',
})
const EMAIL_CACHE_KEY = 'loa_last_email'

// 记住密码（账号默认始终记住）
const rememberPassword = ref(false)

const REMEMBER_USER_KEY = 'loa_remember_username'
const REMEMBER_PWD_KEY = 'loa_remember_password'
// 前端对称混淆密钥（仅为避免密码明文落盘，非真正加密；体验型方案）
const OBF_KEY = 'LOA@2026#login'

function obfuscate(s: string): string {
  let out = ''
  for (let i = 0; i < s.length; i++) {
    out += String.fromCharCode(s.charCodeAt(i) ^ OBF_KEY.charCodeAt(i % OBF_KEY.length))
  }
  return btoa(unescape(encodeURIComponent(out)))
}

function deobfuscate(b64: string): string {
  try {
    const raw = decodeURIComponent(escape(atob(b64)))
    let out = ''
    for (let i = 0; i < raw.length; i++) {
      out += String.fromCharCode(raw.charCodeAt(i) ^ OBF_KEY.charCodeAt(i % OBF_KEY.length))
    }
    return out
  } catch {
    return ''
  }
}

// 读取已记住的账号/密码（账号默认始终回填）
function loadRemembered() {
  const u = localStorage.getItem(REMEMBER_USER_KEY)
  if (u) form.username = u
  const p = localStorage.getItem(REMEMBER_PWD_KEY)
  if (p) {
    const d = deobfuscate(p)
    if (d) {
      form.password = d
      rememberPassword.value = true
    }
  }
}

async function refreshCode() {
  const res = await user.fetchCode()
  codeImg.value = 'data:image/png;base64,' + res.data.img
  uuid.value = res.data.uuid as string
}

async function handleLogin() {
  if (!form.username) {
    ElMessage.warning('请输入用户名')
    return
  }
  if (!form.password) {
    ElMessage.warning('请输入密码')
    return
  }
  loading.value = true
  try {
    await user.login(form.username, form.password, form.code, uuid.value)
    ElMessage.success('登录成功')
    // 账号默认始终记住；密码按勾选状态保存
    localStorage.setItem(REMEMBER_USER_KEY, form.username)
    if (rememberPassword.value) {
      localStorage.setItem(REMEMBER_PWD_KEY, obfuscate(form.password))
    } else {
      localStorage.removeItem(REMEMBER_PWD_KEY)
    }
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
}

function resetRegisterForm() {
  regForm.username = ''
  regForm.nickname = ''
  regForm.email = ''
  regForm.emailCode = ''
  regForm.password = ''
  regForm.confirmPassword = ''
}

function startCountdown(seconds: number) {
  countdown.value = seconds
  if (countdownTimer) window.clearInterval(countdownTimer)
  countdownTimer = window.setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0 && countdownTimer) {
      window.clearInterval(countdownTimer)
      countdownTimer = undefined
    }
  }, 1000)
}

async function sendCode() {
  if (!EMAIL_RE.test(regForm.email)) {
    return ElMessage.warning('请输入正确的邮箱地址')
  }
  codeSending.value = true
  try {
    const res = await sendEmailCode(regForm.email)
    ElMessage.success('验证码已发送，请查收邮件（没收到就看下垃圾箱）')
    startCountdown((res.data?.cooldown as number) || 60)
  } catch {
    // 错误提示已由 request 拦截器统一弹出
  } finally {
    codeSending.value = false
  }
}

async function handleRegister() {
  if (!/^[a-zA-Z0-9_]{3,20}$/.test(regForm.username)) {
    return ElMessage.warning('用户名需为 3-20 位字母、数字或下划线')
  }
  if (!EMAIL_RE.test(regForm.email)) {
    return ElMessage.warning('请输入正确的邮箱地址')
  }
  if (!/^\d{6}$/.test(regForm.emailCode)) {
    return ElMessage.warning('请输入 6 位邮箱验证码')
  }
  if (regForm.password.length < 6 || regForm.password.length > 32) {
    return ElMessage.warning('密码长度需为 6-32 位')
  }
  if (regForm.password !== regForm.confirmPassword) {
    return ElMessage.warning('两次输入的密码不一致')
  }
  regLoading.value = true
  try {
    await apiRegister({
      username: regForm.username,
      nickname: regForm.nickname,
      email: regForm.email,
      emailCode: regForm.emailCode,
      password: regForm.password,
      confirmPassword: regForm.confirmPassword,
    })
    ElMessage.success('注册成功，请使用新账号登录')
    registerVisible.value = false
    localStorage.setItem(EMAIL_CACHE_KEY, regForm.email.trim())
    form.username = regForm.username
    form.password = ''
  } catch {
    // 错误提示已由 request 拦截器统一弹出
  } finally {
    regLoading.value = false
  }
}

/* ---------------- 忘记密码 ---------------- */
function openForgot() {
  // 登录框里填的是用户名就先带过来；填的是邮箱就带邮箱
  const account = form.username.trim()
  if (!forgotForm.username && account && !account.includes('@')) {
    forgotForm.username = account
  }
  const cachedEmail = localStorage.getItem(EMAIL_CACHE_KEY)
  if (!forgotForm.email && (account.includes('@') || cachedEmail)) {
    forgotForm.email = account.includes('@') ? account : cachedEmail || ''
  }
  forgotVisible.value = true
}

function resetForgotForm() {
  forgotForm.username = ''
  forgotForm.email = ''
  forgotForm.emailCode = ''
  forgotForm.newPassword = ''
  forgotForm.confirmPassword = ''
}

function startForgotCountdown(seconds: number) {
  forgotCountdown.value = seconds
  if (forgotTimer) window.clearInterval(forgotTimer)
  forgotTimer = window.setInterval(() => {
    forgotCountdown.value -= 1
    if (forgotCountdown.value <= 0 && forgotTimer) {
      window.clearInterval(forgotTimer)
      forgotTimer = undefined
    }
  }, 1000)
}

async function sendForgotCode() {
  if (!forgotForm.username.trim()) {
    return ElMessage.warning('请输入账号')
  }
  if (!EMAIL_RE.test(forgotForm.email)) {
    return ElMessage.warning('请输入该账号绑定的邮箱')
  }
  forgotSending.value = true
  try {
    const res = await sendResetCode({
      username: forgotForm.username.trim(),
      email: forgotForm.email.trim(),
    })
    ElMessage.success('验证码已发送，请查收邮件（没收到就看下垃圾箱）')
    startForgotCountdown((res.data?.cooldown as number) || 60)
  } catch {
    // 错误提示已由 request 拦截器统一弹出（含账号不存在 / 邮箱不匹配）
  } finally {
    forgotSending.value = false
  }
}

async function handleResetPassword() {
  if (!forgotForm.username.trim()) {
    return ElMessage.warning('请输入账号')
  }
  if (!EMAIL_RE.test(forgotForm.email)) {
    return ElMessage.warning('请输入该账号绑定的邮箱')
  }
  if (!/^\d{6}$/.test(forgotForm.emailCode)) {
    return ElMessage.warning('请输入 6 位邮箱验证码')
  }
  if (forgotForm.newPassword.length < 6 || forgotForm.newPassword.length > 32) {
    return ElMessage.warning('新密码长度需为 6-32 位')
  }
  if (forgotForm.newPassword !== forgotForm.confirmPassword) {
    return ElMessage.warning('两次输入的新密码不一致')
  }
  forgotLoading.value = true
  try {
    const res = await resetPasswordByEmail({
      username: forgotForm.username.trim(),
      email: forgotForm.email.trim(),
      emailCode: forgotForm.emailCode,
      newPassword: forgotForm.newPassword,
      confirmPassword: forgotForm.confirmPassword,
    })
    const username = (res.data?.username as string) || ''
    ElMessage.success('密码已重置，请用新密码登录')
    forgotVisible.value = false
    localStorage.setItem(EMAIL_CACHE_KEY, forgotForm.email.trim())
    // 重置后自动带出账号，并把之前记住的旧密码清掉
    if (username) {
      form.username = username
      localStorage.setItem(REMEMBER_USER_KEY, username)
    }
    form.password = ''
    form.code = ''
    localStorage.removeItem(REMEMBER_PWD_KEY)
    rememberPassword.value = false
    refreshCode()
  } catch {
    // 错误提示已由 request 拦截器统一弹出
  } finally {
    forgotLoading.value = false
  }
}

onMounted(() => {
  loadRemembered()
  refreshCode()
  // 由拦截器判定登录失效跳转过来时，给一句明确提示
  try {
    if (sessionStorage.getItem('loa_auth_expired')) {
      sessionStorage.removeItem('loa_auth_expired')
      ElMessage.warning('登录状态已过期，请重新登录')
    }
  } catch {
    // 忽略
  }
})
onUnmounted(() => {
  if (countdownTimer) window.clearInterval(countdownTimer)
  if (forgotTimer) window.clearInterval(forgotTimer)
})
</script>

<style scoped>
.login-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  box-sizing: border-box;
  overflow: hidden;
}

/* 整页背景图 */
.bg-layer {
  position: absolute;
  inset: 0;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  transform: scale(1.05);
}

/* 压暗 + 暗角，保证卡片文字对比度 */
.bg-mask {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(1200px 700px at 50% 40%, rgba(10, 14, 30, 0.35), rgba(6, 10, 24, 0.78) 75%),
    linear-gradient(135deg, rgba(8, 12, 30, 0.55), rgba(20, 16, 48, 0.5));
}

/* 玻璃拟态登录卡 */
.login-card {
  position: relative;
  z-index: 2;
  width: 400px;
  max-width: 100%;
  box-sizing: border-box;
  padding: 40px 36px 26px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.10);
  backdrop-filter: blur(22px) saturate(160%);
  -webkit-backdrop-filter: blur(22px) saturate(160%);
  border: 1px solid rgba(255, 255, 255, 0.22);
  box-shadow: 0 24px 70px rgba(0, 0, 0, 0.45);
  color: #fff;
}

.brand {
  text-align: center;
  margin-bottom: 18px;
}
.brand-logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 15px;
  background: linear-gradient(135deg, #6366f1, #22d3ee);
  font-weight: 800;
  letter-spacing: 2px;
  font-size: 20px;
  color: #fff;
  box-shadow: 0 10px 26px rgba(99, 102, 241, 0.5);
}
.brand-name {
  margin-top: 10px;
  font-size: 13px;
  letter-spacing: 3px;
  color: rgba(255, 255, 255, 0.82);
}

.title {
  text-align: center;
  margin: 4px 0 4px;
  font-size: 22px;
  color: #fff;
}
.subtitle {
  text-align: center;
  color: rgba(255, 255, 255, 0.7);
  margin: 0 0 22px;
  font-size: 13px;
}

/* 输入框改为透明玻璃风，适配深色卡片 */
.login-card :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.12);
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.22) inset;
}
.login-card :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px rgba(34, 211, 238, 0.7) inset;
}
.login-card :deep(.el-input__inner) {
  color: #fff;
}
.login-card :deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.55);
}
.login-card :deep(.el-input__prefix) {
  color: rgba(255, 255, 255, 0.7);
}

.captcha-row {
  display: flex;
  gap: 12px;
  width: 100%;
}
.captcha-img {
  width: 120px;
  height: 40px;
  border-radius: 6px;
  cursor: pointer;
  flex-shrink: 0;
  border: 1px solid rgba(255, 255, 255, 0.22);
}
.code-btn {
  width: 116px;
  flex-shrink: 0;
}

.remember-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: -4px 0 14px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
}
.remember-tip {
  color: rgba(255, 255, 255, 0.55);
  font-size: 12px;
}
.remember-right {
  display: flex;
  align-items: center;
  gap: 14px;
}
.forgot-link {
  font-size: 12.5px;
  color: rgba(255, 255, 255, 0.85);
}
.forgot-link:hover {
  color: #22d3ee;
}
.forgot-tip {
  margin: 0 0 14px;
  font-size: 12.5px;
  line-height: 1.6;
  color: #909399;
  background: #f5f7fa;
  border-radius: 6px;
  padding: 10px 12px;
}
.remember-row :deep(.el-checkbox__label) {
  color: rgba(255, 255, 255, 0.8);
}
.remember-row :deep(.el-checkbox__inner) {
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(255, 255, 255, 0.4);
}

.login-btn {
  width: 100%;
  font-weight: 600;
  letter-spacing: 2px;
}
.login-btn.el-button--primary {
  background: linear-gradient(135deg, #6366f1, #22d3ee);
  border: none;
  box-shadow: 0 10px 24px rgba(99, 102, 241, 0.45);
}
.login-btn.el-button--primary:hover {
  filter: brightness(1.08);
}

.switch-tip {
  margin-top: 16px;
  text-align: center;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
}
.login-hint {
  margin: 10px 0 0;
  text-align: center;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

.copyright {
  margin-top: 18px;
  text-align: center;
  font-size: 11px;
  letter-spacing: 1px;
  color: rgba(255, 255, 255, 0.45);
}

@media (max-width: 480px) {
  .login-card {
    padding: 32px 22px 22px;
    border-radius: 16px;
  }
}
</style>
