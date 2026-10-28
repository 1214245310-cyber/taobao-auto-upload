<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">淘宝自动上货</div>
      <el-menu :default-active="activeMenu" router class="menu" background-color="#1f2937" text-color="#cbd5e1" active-text-color="#409eff">
        <el-menu-item index="/company-screen">
          <el-icon><DataLine /></el-icon>
          <span>公司作战大屏</span>
        </el-menu-item>
        <el-sub-menu index="goods">
          <template #title>
            <el-icon><Goods /></el-icon>
            <span>店铺商品管理</span>
          </template>
          <el-menu-item index="/goods/tb-item-publish">批量上新</el-menu-item>
          <el-menu-item index="/goods/parse">解析裂变</el-menu-item>
        </el-sub-menu>
        <el-menu-item index="/system/user">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span class="title">{{ title }}</span>
        <el-dropdown trigger="click" @command="onCommand">
          <span class="user-area">
            <img :src="avatar" class="hd-avatar" alt="" />
            <span class="hd-name">{{ userName }}</span>
            <el-icon class="hd-arrow"><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="pwd"><el-icon><Key /></el-icon>修改密码</el-dropdown-item>
              <el-dropdown-item command="logout" divided>
                <el-icon><SwitchButton /></el-icon>退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main><router-view /></el-main>
    </el-container>
  </el-container>

  <el-dialog v-model="pwdVisible" title="修改密码" width="420px" @closed="resetPwdForm">
    <el-form :model="pwdForm" label-width="92px">
      <el-form-item label="原密码" required>
        <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入当前密码" />
      </el-form-item>
      <el-form-item label="新密码" required>
        <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="6-32 位" />
      </el-form-item>
      <el-form-item label="确认新密码" required>
        <el-input v-model="pwdForm.confirm" type="password" show-password placeholder="再次输入" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="pwdVisible = false">取消</el-button>
      <el-button type="primary" :loading="pwdLoading" @click="handleChangePwd">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { DataLine, Goods, SwitchButton, User, Key, ArrowDown } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { changePwd } from '@/api/user'
import loginPhoto from '@/assets/login-photo.webp'

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const activeMenu = computed(() => route.path)
const title = computed(() => (route.meta.title as string) || '')

const avatar = computed(() => (user.userInfo.avatar as string) || loginPhoto)
const userName = computed(
  () => (user.userInfo.nickName as string) || (user.userInfo.userName as string) || '未登录',
)

const pwdVisible = ref(false)
const pwdLoading = ref(false)
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirm: '' })

onMounted(() => {
  if (user.token) user.fetchInfo()
})

function onCommand(cmd: string) {
  if (cmd === 'logout') {
    user.logout()
    router.replace('/auth/login')
  } else if (cmd === 'pwd') {
    pwdVisible.value = true
  }
}

function resetPwdForm() {
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
  pwdForm.confirm = ''
}

async function handleChangePwd() {
  if (!pwdForm.oldPassword) {
    return ElMessage.warning('请输入原密码')
  }
  if (!pwdForm.newPassword || pwdForm.newPassword.length < 6 || pwdForm.newPassword.length > 32) {
    return ElMessage.warning('新密码长度需为 6-32 位')
  }
  if (pwdForm.newPassword !== pwdForm.confirm) {
    return ElMessage.warning('两次输入的密码不一致')
  }
  pwdLoading.value = true
  try {
    await changePwd(pwdForm.oldPassword, pwdForm.newPassword)
    ElMessage.success('密码修改成功，请重新登录')
    pwdVisible.value = false
    user.logout()
    router.replace('/auth/login')
  } catch {
    // 错误提示已由 request 拦截器统一弹出
  } finally {
    pwdLoading.value = false
  }
}
</script>

<style scoped>
.layout {
  height: 100%;
}
.aside {
  background: #1f2937;
}
.logo {
  height: 56px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  font-weight: 700;
  color: #fff;
}
.menu {
  border-right: none;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
}
.title {
  font-weight: 600;
}
.user-area {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}
.hd-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  background: #f0f2f5;
}
.hd-name {
  font-size: 14px;
  color: #303133;
}
.hd-arrow {
  font-size: 12px;
  color: #909399;
}
</style>
