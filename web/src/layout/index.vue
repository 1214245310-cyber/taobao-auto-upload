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
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span class="title">{{ title }}</span>
        <el-button text :icon="SwitchButton" @click="onLogout">退出登录</el-button>
      </el-header>
      <el-main><router-view /></el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { DataLine, Goods, SwitchButton } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const activeMenu = computed(() => route.path)
const title = computed(() => (route.meta.title as string) || '')

function onLogout() {
  user.logout()
  router.replace('/auth/login')
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
</style>
