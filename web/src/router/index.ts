import { createRouter, createWebHashHistory, type RouteRecordRaw } from 'vue-router'
import Layout from '@/layout/index.vue'
import { useUserStore } from '@/store/user'

// 路由结构参照逆向出的原系统菜单（routers.json）：
// 公司作战大屏 + 店铺商品管理（批量上新 / 解析裂变）等。
const routes: RouteRecordRaw[] = [
  {
    path: '/auth/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', public: true },
  },
  {
    path: '/',
    component: Layout,
    redirect: '/company-screen',
    children: [
      {
        path: 'company-screen',
        name: 'CompanyScreen',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '公司作战大屏' },
      },
      {
        path: 'goods',
        meta: { title: '店铺商品管理' },
        children: [
          {
            path: 'tb-item-publish',
            name: 'TbItemPublish',
            component: () => import('@/views/goods/tb-item-publish/index.vue'),
            meta: { title: '批量上新' },
          },
          {
            path: 'parse',
            name: 'Parse',
            component: () => import('@/views/goods/parse/index.vue'),
            meta: { title: '解析裂变' },
          },
        ],
      },
      {
        path: 'system/user',
        name: 'UserManage',
        component: () => import('@/views/user/index.vue'),
        meta: { title: '用户管理' },
      },
      {
        path: 'system/forbidden',
        name: 'ForbiddenManage',
        component: () => import('@/views/forbidden/index.vue'),
        meta: { title: '违禁词管理' },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
  },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
})

// 登录拦截：未携带 token 跳登录页（参照原系统 hash 路由 + redirect 参数）
// 有 token 但还没拿到用户信息时，先校验一次，避免 token 已失效却停在后台页面
router.beforeEach(async (to) => {
  const user = useUserStore()
  if (to.meta.public) return true
  if (!user.token) {
    return { path: '/auth/login', query: { redirect: to.fullPath } }
  }
  if (!user.userInfo.role) {
    try {
      await user.fetchInfo()
    } catch {
      // 登录失效时拦截器已清 token 并跳登录页；这里再兜一层
      if (!localStorage.getItem('token')) {
        return { path: '/auth/login', query: { redirect: to.fullPath } }
      }
    }
  }
  return true
})

export default router
