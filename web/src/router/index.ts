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
router.beforeEach((to) => {
  const user = useUserStore()
  if (!to.meta.public && !user.token) {
    return { path: '/auth/login', query: { redirect: to.fullPath } }
  }
})

export default router
