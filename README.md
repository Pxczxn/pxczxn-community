# 星语社区（pxczxn）

博客与社区一体化平台，包含 Java 后端、Vue 管理后台、React 用户前台、数据库迁移和部署配置。

## 仓库结构

```
pxczxn/
├── pxczxn-backend/          Java 21 + Spring Boot 3.5 模块化单体后端
│   ├── pxczxn-common/       公共基础（异常、统一返回实体、BaseEntity）
│   ├── pxczxn-infra/        基础设施层
│   │   ├── pxczxn-db/       数据库配置（Druid 连接池、MyBatis-Plus 拦截器）
│   │   ├── pxczxn-redis/    Redis 配置
│   │   ├── pxczxn-oss/      对象存储（MinIO / S3 兼容）
│   │   ├── pxczxn-websocket/ WebSocket 通信
│   │   ├── pxczxn-sms/      短信服务
│   │   ├── pxczxn-push/     消息推送
│   │   ├── pxczxn-wechat/   微信集成
│   │   ├── pxczxn-social/   第三方社交登录
│   │   ├── pxczxn-crypto/   加解密工具
│   │   └── pxczxn-mail/     邮件服务
│   ├── pxczxn-core/         业务核心层
│   │   ├── pxczxn-system/   平台系统服务（用户、角色、菜单、字典、配置等）
│   │   ├── pxczxn-biz/      社区业务逻辑（按领域划分）
│   │   ├── pxczxn-message/  消息中心（通知、站内信、聊天）
│   │   ├── pxczxn-file/     文件服务
│   │   ├── pxczxn-gen/      代码生成器（Velocity 模板）
│   │   └── pxczxn-auth/     认证策略（密码、短信、微信多策略）
│   ├── pxczxn-api/          接口层
│   │   ├── pxczxn-admin-api/ 管理端 REST API
│   │   └── pxczxn-web-api/  用户端 REST API
│   ├── pxczxn-job/          定时任务（Quartz）
│   └── pxczxn-starter/      Spring Boot 启动入口与配置
├── pxczxn-admin/            Vue 3 + Naive UI 运营管理后台
├── pxczxn-web/              React 19 + Next.js 16 + Cloudflare Workers 用户侧前端
├── database/                MySQL 迁移脚本（V001–V075）与校验脚本
├── deploy/                  Nginx、Prometheus 监控、运维自动化脚本
├── scripts/                 构建、验证、E2E 测试脚本
└── docs/                    设计文档
```

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Java 21、Spring Boot 3.5.16、MyBatis-Plus 3.5.6、Sa-Token 1.37、Druid 1.2.23 |
| 管理端 | Vue 3.4、Naive UI 2.37、Pinia、Vue Router 4.2、Vite |
| 用户端 | React 19、Next.js 16.2、Tailwind CSS 4.2、Drizzle ORM、Cloudflare Workers |
| 数据库 | MySQL 8.x（InnoDB / utf8mb4）、Redis |
| 对象存储 | MinIO / S3 兼容 |
| 部署 | Nginx、Prometheus、Grafana、systemd |

---

## 快速开始

### 后端

```powershell
cd pxczxn-backend
mvn clean package -DskipTests
java -jar pxczxn-starter/target/pxczxn-starter-1.0.0.jar
```

后端默认端口 `8080`，通过 `SPRING_PROFILES_ACTIVE` 切换环境。配置文件说明：

- `application.yml`：基础配置
- `application-local.yml`：本地开发配置（不提交）
- `application-secret.yml`：密钥配置（不提交）

### 管理后台

```powershell
cd pxczxn-admin
npm install
npm run dev
```

管理端默认监听 `http://localhost:9528`，代理 API 请求到后端 `http://localhost:8080`。

### 用户前台

```powershell
cd pxczxn-web
npm install
npm run dev
```

用户端默认监听 `http://localhost:8847`，默认连接 `http://127.0.0.1:8849` 社区 API。其他环境可复制 `.env.example` 后设置：

```text
NEXT_PUBLIC_COMMUNITY_API_BASE_URL=https://api.example.com
```

### 数据库

MySQL 8.x / InnoDB / utf8mb4，业务库名 `pxczxn_community`。不引入 Flyway 或 Liquibase，迁移由仓库 SQL 文件 + `scripts/` 下 PowerShell 脚本自行管理。

```powershell
.\scripts\invoke-database-migrations.ps1 `
  -Database pxczxn_community `
  -DatabaseUser root
```

数据库目录结构：

```
database/
├── migrations/        V001–V075 版本化迁移 SQL（严格升序，不可修改）
├── rollback/          对应回滚脚本（默认拒绝破坏性执行，需显式确认变量）
├── seeds/             种子数据（初始化菜单、角色等）
└── verify/            每个迁移版本的在线结构校验脚本（与 migrations 一一对应）
```

#### 迁移历史表

`pxczxn_schema_version` 由 `scripts/invoke-database-migrations.ps1` 自动创建和维护：

| 字段 | 含义 |
|------|------|
| `version` | 唯一迁移版本，如 `V038` |
| `description` | 从文件名提取的描述 |
| `checksum` | SQL 文件 UTF-8 + LF 规范化后的 SHA-256 |
| `executed_at` | 最近一次执行或登记时间 |
| `success` | `1` 表示迁移和校验均通过 |

已成功版本不会重复执行。文件 checksum 与历史表不一致时立即中止。

#### 迁移版本里程碑

| 版本范围 | 里程碑 |
|----------|--------|
| V001 | M1 社区业务基础表（71 张表） |
| V002–V006 | 标签、审核、定时发布、运营查询 |
| V007–V012 | 互动、隐私、评论治理、动态、通知、运营治理 |
| V013 | 管理端品牌写入 |
| V014 | 管理员密码生命周期字段 |
| V015 | 默认关闭支付/短信/微信/第三方集成并脱敏 |
| V016–V018 | 脚手架清理决策、菜单元数据恢复、集成配置占位 |
| V019 | 聊天状态、群聊游标、查询索引 |
| V020–V022 | M2.5 信息架构与保留脚手架 |
| V023–V029 | M3 团队、投稿、协作、聊天、系列 |
| V030–V035 | M4 治理与反滥用 |
| V036–V038 | M5 搜索索引、运营合集、创作者分析 |
| V039–V075 | 在线校验脚本（结构验证、权限修复、菜单冲突修复、品牌重命名等） |

#### 数据库常用命令

```powershell
# 只读校验（验证文件名连续性、校验一一对应、数据库结构、checksum 一致性）
.\scripts\check-database-migrations.ps1 -Database pxczxn_community -DatabaseUser root

# 已有数据库建立基线（逐版本在线校验，通过后才登记 checksum）
.\scripts\invoke-database-migrations.ps1 -Database pxczxn_community -DatabaseUser root -BaselineExisting

# 库名迁移（一致性备份 + 逐表行数精确比对 + 在线校验 + checksum 登记）
.\scripts\migrate-database-name.ps1 -SourceDatabase <旧库名> -TargetDatabase pxczxn_community -DatabaseUser root -BackupDirectory <备份目录>
```

#### 数据库约束与保护

- **不可变迁移**：已登记成功的 `Vxxx` 文件禁止修改，结构调整必须新增版本
- **回滚保护**：rollback SQL 默认拒绝执行，需在同一会话设置确认变量
- **失败处理**：任一迁移或校验失败后立即停止，保留现场，禁止伪装成功
- **重入安全**：`CREATE TABLE IF NOT EXISTS` 和元数据保护保证脚本可重入，但不能替代 checksum 校验

---

## 后端架构详解

### 模块依赖关系

```
pxczxn-common （最底层，无内部依赖）
    ↑
pxczxn-infra （依赖 common）
    ↑
pxczxn-core （依赖 common + infra）
    ↑
pxczxn-api （依赖 core，不直接依赖 infra）
    ↑
pxczxn-starter （聚合所有模块，Spring Boot 启动入口）
```

### REST API 分离

| 模块 | 路径前缀 | 用途 |
|------|----------|------|
| admin-api | `/admin/` | 管理后台接口（社区治理、用户管理、系统配置） |
| web-api | `/api/v1/` | 用户侧接口（文章、社交、团队、通知等） |

两套 API 共享 `pxczxn-core` 业务逻辑，通过独立 Controller 暴露不同权限级别的端点。

### 社区业务领域（pxczxn-biz）

每个领域遵循 `application`（应用服务）→ `model`（领域模型）→ `persistence`（持久化）分层：

| 领域 | 职责 |
|------|------|
| article | 文章发布、草稿、审核、定时发布、权限控制 |
| blog | 个人博客管理 |
| series | 书架 / 连载内容管理 |
| team | 团队创建、成员管理、投稿、协作 |
| social | 关注、粉丝、点赞、收藏、动态、发现 |
| notification | 站内通知、系统消息 |
| moderation | 内容审核、举报处理 |
| governance | 社区治理规则与策略 |
| sanction | 账号处罚与申诉 |
| abuse | 反滥用检测 |
| search | 全文搜索索引 |
| analytics | 创作者数据分析 |
| chat | 聊天与私信 |
| taxonomy | 标签与分类体系 |
| file | 文件上传与管理 |
| rss | RSS 订阅 |
| report | 举报工单 |
| block | 用户屏蔽 |
| editorial | 编辑推荐 |
| collaboration | 共创协作编辑 |
| user | 用户资料与账号管理 |

### 安全扫描

启用 `security` profile 执行 OWASP 依赖漏洞扫描和 CycloneDX SBOM 生成：

```powershell
mvn clean package -Psecurity
```

输出位于 `outputs/security/` 目录，包含 HTML / JSON / SARIF 格式报告。CVSS >= 7.0 时构建失败。

---

## 管理后台详解（pxczxn-admin）

### 技术栈

| 库 | 版本 | 用途 |
|----|------|------|
| Vue | 3.4 | UI 框架 |
| Naive UI | 2.37 | 组件库 |
| Pinia | 2.1 | 状态管理（含持久化插件） |
| Vue Router | 4.2 | 路由 |
| Vite | — | 构建工具 |
| Axios | 1.6 | HTTP 请求 |
| ECharts | 6.0 | 数据可视化 |
| xterm.js | 6.0 | 终端模拟器（在线调试） |
| JSEncrypt | 3.3 | RSA 加密 |
| @noble/ciphers | 1.3 | 对称加密 |
| disable-devtool | 0.3 | 开发工具禁用（生产环境） |

### 页面模块

| 模块 | 功能 |
|------|------|
| community/dashboard | 社区运营仪表盘 |
| community/articles | 文章审核与管理 |
| community/blogs | 博客管理 |
| community/series | 系列 / 书架管理 |
| community/teams | 团队管理 |
| community/team-applications | 团队申请审核 |
| community/team-submissions | 团队投稿管理 |
| community/comments | 评论审核 |
| community/interactions | 互动数据管理 |
| community/moments | 动态管理 |
| community/tags | 标签管理 |
| community/reviews | 内容审核 |
| community/reports | 举报处理 |
| community/appeals | 申诉处理 |
| community/content-rules | 内容规则配置 |
| community/account-enforcements | 账号执行管理 |
| community/users | 用户管理 |
| system/* | 系统配置（菜单、角色、字典、参数、文件等） |
| org/* | 组织管理（部门、岗位） |
| monitor/* | 系统监控（服务器、缓存、在线用户、定时任务、API 访问、Druid） |
| message/* | 消息管理（通知、聊天） |
| log/* | 操作日志、登录日志 |
| tool/gen | 代码生成工具 |

### 构建与验证

```powershell
npm run lint        # ESLint 检查
npm run typecheck   # TypeScript 类型检查（vue-tsc）
npm run build       # Vite 生产构建
npm test            # 单元测试
```

### 项目结构

```
pxczxn-admin/src/
├── api/            接口定义（按模块拆分）
├── components/     公共组件（Watermark 等）
├── layout/         布局组件
├── router/         路由配置
├── stores/         Pinia 状态管理（用户、权限、字典等）
├── styles/         全局样式（SCSS）
├── utils/          工具函数（请求封装、加密、权限校验等）
└── views/          页面组件（按功能模块目录组织）
```

---

## 用户前台详解（pxczxn-web）

### 技术栈

| 库 | 版本 | 用途 |
|----|------|------|
| React | 19.2 | UI 框架 |
| Next.js | 16.2 | SSR / 路由 / 构建 |
| Tailwind CSS | 4.2 | 样式 |
| Drizzle ORM | 0.45 | 数据库访问（Cloudflare D1） |
| Cloudflare Workers | — | 边缘运行时部署 |
| Lucide React | 0.468 | 图标库 |

### 页面路由

| 路由 | 功能 |
|------|------|
| `/login` | 注册、登录与会话反馈 |
| `/home` | 首页 / 推荐流 |
| `/discover` | 发现页（多路召回、分区推荐） |
| `/search` | 全局搜索 |
| `/articles/:articleId` | 文章详情 |
| `/blogs/:slug` | 博客主页 |
| `/editor/new`、`/editor/:articleId` | 文章编辑器 |
| `/series/:seriesId` | 书架 / 连载详情 |
| `/moments` | 动态流 |
| `/moments/:momentId` | 动态详情 |
| `/chat` | 私信聊天 |
| `/notifications` | 通知中心 |
| `/teams` | 团队入口（我的团队 / 发现 / 邀请） |
| `/teams/:teamSlug` | 团队主页 |
| `/teams/:teamSlug/workspace/*` | 团队工作台（内容 / 系列 / 投稿 / 成员 / 设置） |
| `/submissions/ai-agent` | 投稿审核状态 |
| `/collaboration/articles/:articleId` | 共创协作编辑 |
| `/me/*` | 个人中心（收藏 / 博客 / 系列 / 数据分析） |
| `/settings` | 账号设置、主题切换（浅色 / 深色 / 星空） |
| `/editorial` | 编辑推荐 |
| `/reports` | 举报管理 |
| `/sanctions` | 处罚状态 |
| `/appeals` | 申诉 |
| `/blocks` | 屏蔽列表 |
| `/tags` | 标签浏览 |
| `/team-applications` | 团队申请 |
| `/team-invitations` | 团队邀请 |

### 构建与验证

```powershell
npm run lint        # ESLint 检查
npm run typecheck   # TypeScript 类型检查
npm run build       # 生产构建（Vinext）
npm test            # 构建 + 全部路由 SSR 检查 + 契约测试
```

### 数据库

Drizzle ORM 管理 Cloudflare D1 数据库，schema 位于 `db/` 目录：

```powershell
npm run db:generate  # 根据 schema 生成迁移
```

### Cloudflare Workers 部署

构建产物部署到 Cloudflare Workers，配置见 `worker-configuration.d.ts` 和 `worker/index.ts`。

生产预览环境：`https://xingyu-community-pxczxn.pxczxn.chatgpt.site`

> 预览环境未配置线上社区 API 时会显示明确提示，本地三个固定端口提供完整业务闭环。

---

## 部署与运维

部署配置和运维脚本位于 `deploy/` 目录：

```
deploy/
├── nginx/              Nginx 配置
│   ├── community.conf  社区 API 反向代理
│   ├── admin.conf      管理后台反向代理
│   ├── web.conf        用户前台反向代理
│   └── snippets/       安全头、SSL 等可复用片段
├── monitoring/         监控配置
│   ├── prometheus.yml  Prometheus 采集配置
│   ├── alertmanager.yml（.example）  告警配置模板
│   └── rules/          告警规则（YAML）
└── ops/                运维脚本
    ├── *.sh            服务管理、日志清理、备份等 Shell 脚本
    ├── *.service       systemd 服务单元文件
    └── log-rotation.py 日志轮转 Python 脚本
```

### Nginx 反向代理

| 配置文件 | 代理目标 | 默认监听 |
|----------|----------|----------|
| community.conf | 后端 API `:8080/api/v1/*` | 443 (HTTPS) |
| admin.conf | 管理后台静态文件 + API 代理 | 443 |
| web.conf | 用户前台静态文件 + API 代理 | 443 |

### 监控

- Prometheus 采集后端 Actuator 指标
- Grafana 仪表盘（需自行导入）
- Alertmanager 告警通知（`alertmanager.yml.example` 为模板，需复制为 `alertmanager.yml` 并填入真实配置）

### 部署步骤

1. 配置 Nginx（复制 conf 文件，修改 `server_name` 和 `ssl_certificate` 路径）
2. 配置 Prometheus + Alertmanager（参考 `.example` 文件）
3. 安装 systemd 服务：`sudo cp deploy/ops/*.service /etc/systemd/system/`
4. 启动服务：`sudo systemctl start pxczxn-backend`
5. 验证：访问 `/actuator/health` 确认后端健康

---

## 安全特性

- Sa-Token 认证授权 + Redis 会话共享
- 多策略认证（密码、短信、微信）
- OWASP 依赖漏洞扫描（`mvn -Psecurity` CycloneDX SBOM + OWASP Dependency-Check）
- RBAC 权限模型，菜单 / 按钮级控制
- 数据库迁移 checksum 不可变校验
- `alertmanager.yml` 和其他包含真实密钥的配置文件不提交到仓库

## 脚本工具

`scripts/` 目录包含构建、验证和 E2E 测试脚本：

| 脚本 | 用途 |
|------|------|
| `invoke-database-migrations.ps1` | 执行数据库迁移 |
| `check-database-migrations.ps1` | 只读校验迁移状态 |
| `migrate-database-name.ps1` | 库名迁移 |
| `validate-database-migration-manifest.ps1` | 校验迁移清单 |
| `verify-dependencies.ps1` | 依赖检查 |
| `verify-security.ps1` | 安全验证 |
| `verify.ps1` | 综合验证 |
| `start-backend.ps1` | 启动后端 |
| `run-claude-task.ps1` | Claude 任务执行 |
| `scripts/e2e/` | E2E 浏览器和 API 测试脚本 |

## 许可

私有项目，未经授权禁止使用。