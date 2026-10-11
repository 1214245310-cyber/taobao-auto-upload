# taobao-auto-upload · 淘宝自动上货系统（复刻框架）

复刻「链接 → 标准化商品 JSON（js 格式）→ 解码上货」与「批量上新」业务闭环的前端框架。

技术栈：Vue3 + Vite + TypeScript + Pinia + vue-router + Element Plus（参照原系统 vue-vben-admin 5）。

## 进度

| 模块 | 状态 | 说明 |
|---|---|---|
| 前端框架 `web/` | ✅ 骨架 | 登录 / 大屏 / 批量上新 / 解析裂变 页面骨架 + API 层 |
| 解析模块 `parse_service/` | ✅ 可运行 | 单/批量解析、违禁词校验、解码上货桥接、本地演示页 |
| 解析服务商 | ⬜ 未接入 | `WanxiangProvider` 留空，当前用 MockProvider 联调 |
| 上新执行器 | ⬜ 未开始 | 接收 payload 上架 + 任务状态机 |

## 运行前端

```bash
cd web
npm install
npm run dev
# http://localhost:5173
```

## 运行后端雏形（解析模块）

```bash
python -m parse_service.demo
python -m parse_service.webui   # http://127.0.0.1:8765
```

## 目录

```
web/            前端框架（Vue3 + Vite + TS）
parse_service/  Python 解析模块（后端雏形）
docs/           ⚠️ 本地保留，未随仓库发布（含第三方系统逆向分析）
```

> 注：`docs/` 含对第三方系统的逆向研究，已通过 `.gitignore` 排除，不推送到公开远端；
> 如需发布请改用私有仓库后再取消忽略。
