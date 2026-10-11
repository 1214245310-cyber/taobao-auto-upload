# 前端 · 淘宝自动上货系统

参照原系统（vue-vben-admin 5 + Vue3）打出的**框架骨架**。
技术栈：Vue3 + Vite + TypeScript + Pinia + vue-router + Element Plus。

## 启动

```bash
npm install
npm run dev
# 打开 http://localhost:5173
```

## 目录结构

```
web/
  src/
    api/        axios 封装 + 接口定义（auth / publish / parse）
    store/      Pinia 用户态（登录、token 持久化）
    router/     路由（登录 / 大屏 / 批量上新 / 解析裂变）
    views/      页面骨架
      login/        登录页（用户名/密码/验证码，参照原 OA）
      dashboard/    公司作战大屏（统计卡片 + 客服排行）
      goods/        批量上新列表、解析裂变
      error/        404
    layout/     主布局（侧边栏菜单 + 顶栏）
```

## 接口约定

- 所有请求走 `/prod-api`，vite 开发代理到后端网关（见 `vite.config.ts`），接自有后端改 `target` 即可。
- 响应统一包装 `{ code, msg, data }`，`code !== 200` 视为失败。
- 请求头携带 `Authorization: Bearer <token>` 与 `clientid`（原系统约定）。

## 说明

当前为**框架骨架**：页面已就位，但数据多为占位/示例；接口层已连通原系统可供联调。
后端执行器（真正上架）与解析服务商尚未接入，详见根目录 README。
