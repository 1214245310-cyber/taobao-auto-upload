# taobao-auto-upload · 淘宝自动上货系统

店铺运营后台：源链接解析 → 商品结构化 → 违禁词校验 → 批量上货。

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Java 17 + Spring Boot 3.2.5 + Spring Data JPA + Maven |
| 数据库 | H2 文件库（MySQL 兼容模式），可平滑迁移 MySQL |
| 前端 | Vue3 + Vite + TypeScript + Pinia + Element Plus |
| 静态演示 | 原生 HTML（`static-demo/`，26 个页面） |

## 目录

```
backend/       Spring Boot 后端（端口 8081）
web/           管理端前端（Vue3 + Vite）
static-demo/   静态演示页（26 页，无需构建）
```

## 快速开始

### 后端

```bash
cd backend
mvn spring-boot:run
# http://localhost:8081
```

首次启动会自动建表，并写入管理员账号与违禁词种子数据。

默认账号：`admin / admin123`

数据库文件位于 `backend/data/loa.mv.db`（H2 文件库，MySQL 兼容模式）。

### 前端

```bash
cd web
npm install
npm run dev
# http://localhost:5173
```

前端固定以 `/api` 为请求前缀。本地开发时如遇跨端口，请在 `vite.config.ts` 中把 `/api` 代理到 `http://localhost:8081`。

## 已实现接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/auth/code` | 算术验证码（Base64 PNG） |
| POST | `/auth/login` | 登录，返回 access_token |
| POST | `/auth/logout` | 登出 |
| GET | `/system/user/getInfo` | 当前用户信息 |
| POST | `/goods/parse-item` | 解析单个链接并落库 |
| GET | `/goods/list` | 解析历史分页列表 |
| GET | `/goods/parse-channel/list` | 支持的平台渠道 |
| POST | `/goods/parse-task/submit-context` | 批量任务上下文 |
| POST | `/goods/parse-task/upload` | 批量任务文件上传 |
| GET | `/tb/publish/*` | 上货模块（占位，待实现） |

## 数据模型

| 表 | 说明 |
|---|---|
| `users` | 账号 |
| `source_links` | 源链接（平台、原始 URL、抓取状态） |
| `parsed_items` | 标准化商品 JSON + 处理状态 |
| `forbidden_words` | 违禁词库 |

## 解析渠道

通过 `loa.parse.provider` 切换实现：

- `mock`（默认）：本地演示，按链接生成模拟数据，不访问外网
- `third-party`：调用第三方聚合 API，需配置 `loa.parse.third-party.base-url` 与 `access-token`

## 关于淘宝开放平台

个人开发者账号仅具备**只读**接口权限；发布类接口（`taobao.item.add` 等）需要企业或个体工商户主体，且当前控制台已无「自用型应用」入口。

因此上货链路的可选路径：

1. 以个体工商户主体申请应用，走官方 API；
2. 接入第三方代理 API（请求参数与 `taobao.item.add` 一致）。

## 部署

生产环境为 Ubuntu + Caddy：

```
80 /        -> 落地页
80 /app/    -> web 构建产物
80 /demo/   -> static-demo
80 /api/*   -> 反向代理到 127.0.0.1:8081（Spring Boot）
```

后端以 systemd 常驻，2G 内存机型建议 JVM 参数 `-Xmx320m`。
