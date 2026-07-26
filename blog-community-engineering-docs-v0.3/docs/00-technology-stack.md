# 技术栈与工程基线

> 文档版本：v0.3  
> 项目代号：`pxczxn-community`  
> 后台基础工程：`pxczxn-admin`。原始脚手架来源为 Mars Admin，仅保留来源记录；工程命名、包名、模块名和产品品牌均归属 `pxczxn`。  
> 当前阶段：模块化单体，优先完成 V1 核心闭环。

---

## 1. 技术选型原则

项目技术选型遵循以下原则：

1. **业务优先**：先完成注册、个人博客、文章编辑、审核和发布闭环，不为了展示技术而堆基础设施。
2. **模块化单体优先**：V1 不拆微服务，不引入 Spring Cloud、服务注册中心和分布式事务。
3. **复用成熟能力**：后台管理、RBAC、文件、日志和定时任务优先复用 `pxczxn-admin` 已有实现。
4. **品牌与脚手架分离**：`pxczxn-admin` 是本项目后台基础工程，项目对外和工程内部统一使用 `pxczxn` 命名。
5. **MySQL 是事实来源**：核心业务数据必须落 MySQL；Redis 仅承担缓存、会话、限流、计数和锁等增强职责。
6. **可替换但不滥换**：核心技术一旦进入开发阶段，不因“某个库更火”就随意替换。
7. **先可维护，再谈规模化**：只有出现真实流量、并发和多实例需求后，才增加搜索引擎、消息队列等组件。

---

## 2. 总体架构

```text
pxczxn-community
├── pxczxn-backend       Java 后端，基于 `pxczxn-admin` 工程开发
├── pxczxn-web           普通用户端与公开博客页面
├── pxczxn-admin         平台运营管理端
├── database             版本化 SQL、初始化数据与数据库说明
├── deploy               Docker Compose、Nginx 与部署配置
└── docs                 产品、架构、接口和任务文档
```

系统采用：

```text
前后端分离
+ 模块化单体后端
+ 独立用户端前端
+ 独立管理端前端
+ MySQL 核心存储
+ Redis 可选增强
```

### 2.1 运行关系

```text
pxczxn-web
      │
      ├── /api/v1/**
      ▼
pxczxn-backend
      ▲
      ├── /admin-api/**
      │
pxczxn-admin

pxczxn-backend ── MySQL
pxczxn-backend ── Redis（增强能力）
pxczxn-backend ── 本地 / MinIO / OSS（文件存储）
```

---

## 3. 工程命名规范

### 3.1 项目与仓库

```text
项目根名称：pxczxn-community
后端目录：pxczxn-backend
用户端：pxczxn-web
管理端：pxczxn-admin
```

未来正式品牌确定后，可以修改页面标题、Logo、域名和展示文案，但不要求立即重命名所有底层代码。

### 3.2 Java 包名

统一使用：

```java
top.pxczxn.community
```

示例：

```text
top.pxczxn.community.user
top.pxczxn.community.blog
top.pxczxn.community.article
top.pxczxn.community.moderation
```

### 3.3 Maven 模块

目标模块命名：

```text
pxczxn-common
pxczxn-infra
pxczxn-system
pxczxn-auth
pxczxn-file
pxczxn-biz
pxczxn-admin-api
pxczxn-web-api
pxczxn-job
pxczxn-starter
```

若工程仍保留原脚手架目录，可以分阶段重命名；所有新增业务代码统一使用 `pxczxn` 命名。

### 3.4 Token 与配置前缀

```text
管理员 Token：pxczxn-admin-token
社区用户 Token：pxczxn-community-token
配置前缀：pxczxn.community
```

### 3.5 数据库命名

建议数据库名：

```text
pxczxn_community
```

后台系统表可以保留中性的 `sys_` 前缀；社区业务表使用明确的领域名称，例如：

```text
community_user
blog
article
article_version
comment
content_like
content_favorite
```

新业务表不使用旧脚手架品牌前缀。

---

## 4. 后端技术栈

### 4.1 Java 与构建

| 项目 | 选型 |
|---|---|
| Java | **Java 21** |
| 构建工具 | Maven Wrapper 优先 |
| 编码 | UTF-8 |
| 时间存储 | UTC |

要求：

- 所有后端模块统一使用 Java 21。
- 本地、CI 和服务器使用相同的 Java 主版本。
- 仓库保留 Maven Wrapper，避免开发环境 Maven 版本漂移。
- 不在部分模块中继续使用 Java 17 编译目标。

### 4.2 核心框架

| 领域 | 技术 |
|---|---|
| 后台基础工程 | `pxczxn-admin` |
| Web 框架 | Spring Boot 3.x |
| 核心容器 | Spring Framework 6.x |
| 数据访问 | MyBatis-Plus |
| 认证鉴权 | Sa-Token |
| 参数校验 | Jakarta Bean Validation |
| 定时任务 | Quartz |
| API 文档 | Springdoc OpenAPI |
| 对象转换 | 沿用脚手架现有方式，优先 MapStruct |

### 4.3 `pxczxn-admin` 的使用边界

`pxczxn-admin` 负责提供：

- 管理员登录和 RBAC
- 菜单、角色与权限标识
- 系统配置与字典
- 操作日志和登录日志
- 文件管理基础能力
- Quartz 定时任务基础
- 通用响应、异常和 CRUD 脚手架

`pxczxn-admin` **不决定**：

- 项目品牌名称
- 社区用户模型
- 博客和文章业务权限
- 团队角色模型
- 文章审核和版本流程
- 前台页面设计

### 4.4 管理员与社区用户隔离

系统保留两套账号体系：

```text
平台管理员
→ sys_user / 后台 RBAC

社区用户
→ community_user / 业务关系权限
```

管理员和社区用户：

- 使用不同 Token 名称；
- 使用不同登录入口；
- 使用不同权限判断；
- 不能通过一套 Token 直接访问另一套接口；
- 社区用户不能进入管理后台；
- 管理员也不能自动冒充社区用户。

### 4.5 后端领域模块

社区业务集中于 `pxczxn-biz`，内部按领域分包：

```text
top.pxczxn.community
├── user
├── blog
├── article
├── taxonomy
├── series
├── moment
├── interaction
├── follow
├── favorite
├── submission
├── collaboration
├── moderation
├── notification
├── operation
└── shared
```

V1 不把每个领域拆成独立 Maven 模块，避免模块数量膨胀。

### 4.6 业务事件

V1 使用：

```text
Spring ApplicationEvent
@TransactionalEventListener
```

适用于：

- 发布文章后发送通知；
- 审核完成后更新状态；
- 更新统计与缓存；
- 清理搜索数据；
- 记录业务事件。

V1 不引入 Kafka 或 RabbitMQ。

---

## 5. 数据库与 SQL 管理

### 5.1 主数据库

| 项目 | 选型 |
|---|---|
| 数据库 | MySQL 8.x |
| 存储引擎 | InnoDB |
| 字符集 | utf8mb4 |
| 时间字段 | DATETIME(3)，统一保存 UTC |
| 主键 | BIGINT，由后端生成 |

MySQL 是以下数据的唯一事实来源：

- 用户与登录账号；
- 个人博客与团队博客；
- 文章、正文版本和发布状态；
- 分类、标签和系列；
- 评论、关注、点赞和收藏关系；
- 投稿、共创和转载关系；
- 审核、举报、处罚和申诉记录；
- 通知记录和重要审计数据。

### 5.2 数据库变更方式

项目**不使用 Flyway**。

使用仓库内版本化 SQL 脚本：

```text
database/
├── README.md
├── migrations/
│   ├── V001__init_core_schema.sql
│   ├── V002__add_article_version.sql
│   ├── V003__add_review_tables.sql
│   └── V004__add_indexes.sql
├── seeds/
│   ├── dev_seed.sql
│   └── base_dictionary.sql
└── rollback/
    └── 按需提供高风险变更回滚脚本
```

每个迁移脚本必须写明：

- 版本号和用途；
- 依赖的前置版本；
- 是否会锁表；
- 是否会修改或迁移历史数据；
- 是否需要备份；
- 是否可重复执行；
- 执行后的验证 SQL。

`database/README.md` 维护：

- 当前数据库版本；
- 新环境初始化顺序；
- 已部署环境升级顺序；
- 执行记录；
- 失败处理方式。

V1 可以人工执行或由部署脚本顺序执行 SQL，不额外引入迁移框架。

### 5.3 数据原则

- 所有重要关系在数据库层建立唯一约束；
- 文章正文只存于版本表；
- 公开版本和编辑版本分离；
- 用户名、slug 和标题不能作为外键；
- 核心关系不塞进 JSON；
- 审核、举报和处罚记录不允许普通业务物理删除。

---

## 6. Redis

### 6.1 定位

Redis 可以接入，但属于增强组件：

```text
MySQL = 业务事实来源
Redis = 会话、缓存、限流、计数和锁
```

项目可以在 V1 使用 Redis，但不得把核心业务绑死在 Redis 上。

### 6.2 适合使用的场景

- Sa-Token 登录会话；
- 邮箱验证码；
- 接口限流；
- 防重复提交；
- 临时预览 Token；
- 热门文章和公开页面缓存；
- 未读通知数量；
- 浏览量等高频计数缓冲；
- 定时发布锁；
- 多实例部署后的分布式锁。

### 6.3 禁止作为唯一存储

以下数据不能只存在 Redis：

- 账号和密码凭证；
- 文章正文与版本；
- 点赞、收藏和关注关系；
- 团队成员和角色；
- 审核结果和文章状态；
- 举报、处罚和申诉记录。

### 6.4 使用约束

业务代码不在各处直接调用 `RedisTemplate`，统一通过抽象服务：

```text
CacheService
RateLimitService
LockService
CounterService
VerificationCodeService
```

Redis 不可用时：

- 已发布文章仍可访问；
- 核心数据不得丢失；
- 缓存可以回源 MySQL；
- 非关键限流和计数允许降级；
- 发布、审核和权限判断仍以数据库为准。

---

## 7. 用户端前端

### 7.1 核心技术

| 领域 | 技术 |
|---|---|
| 框架 | Next.js |
| UI 基础 | React |
| 语言 | TypeScript |
| 样式 | Tailwind CSS |
| 包管理 | pnpm |
| Node.js | 使用 LTS 版本，并通过 `.nvmrc` 或 Volta 锁定 |

用户端项目名称：

```text
pxczxn-web
```

### 7.2 用户端职责

- 首页和发现；
- 搜索；
- 个人博客和团队博客主页；
- 文章、系列、标签和动态页面；
- 注册登录；
- 创作工作台；
- 富文本与 Markdown 编辑；
- 通知和个人设置；
- 团队工作台。

### 7.3 渲染策略

公开内容优先使用 Next.js 的服务端渲染、缓存或静态生成能力：

- 博客主页；
- 文章详情；
- 标签和系列页面；
- SEO 元数据；
- Sitemap 与 RSS。

登录后的编辑器和管理工作台以客户端交互为主。

### 7.4 请求与状态

- 默认使用标准 `fetch` 封装 API 客户端；
- 无明确必要时不额外引入 Axios；
- 服务端状态与本地 UI 状态分离；
- 不把全部接口数据塞进一个全局 Store；
- BIGINT ID 始终按字符串处理。

---

## 8. 管理端前端

管理端项目名称：

```text
pxczxn-admin
```

`pxczxn-admin` 管理前端沿用现有技术：

| 领域 | 技术 |
|---|---|
| 框架 | Vue 3 |
| 语言 | TypeScript |
| 构建 | Vite |
| UI | Naive UI |
| 状态管理 | Pinia |
| 路由 | Vue Router + 动态菜单 |

管理端只服务平台运营人员，负责：

- 社区用户和博客管理；
- 团队博客申请；
- 文章、动态、评论和系列审核；
- 标签、专题和推荐位；
- 举报、处罚和申诉；
- 公告、关键词和系统配置；
- 文件、日志和统计。

不使用 React 或 Next.js 重写管理端。

---

## 9. 编辑器与内容渲染

### 9.1 富文本编辑器

```text
Tiptap / ProseMirror
```

作为普通用户默认编辑模式。

富文本文章的真实来源为结构化 JSON，不以 HTML 作为唯一源数据。

### 9.2 Markdown 编辑器

```text
CodeMirror 6
```

作为高级写作模式，适合技术作者和长文档。

### 9.3 Markdown 处理链

```text
unified
remark
mdast
rehype
```

负责：

- Markdown 解析；
- 目录生成；
- 安全 HTML 输出；
- 链接、图片和自定义语法处理。

### 9.4 代码与公式

| 能力 | 技术 |
|---|---|
| 代码高亮 | Shiki |
| 数学公式 | KaTeX |

### 9.5 内容安全

- 不允许任意脚本和事件属性；
- 不允许未经清洗的 HTML；
- iframe 使用严格白名单；
- 服务端重新校验和渲染正文；
- HTML 是展示缓存，不是文章唯一源数据。

---

## 10. 文件与对象存储

复用脚手架已有文件能力，按部署环境选择：

```text
本地存储
MinIO
兼容 S3 的对象存储
云厂商 OSS
```

业务表和正文只保存 `file_id`，不保存 Base64，也不把完整 URL 当作永久业务标识。

V1 支持：

- JPG、PNG、WebP、GIF；
- 经严格清洗的 SVG；
- 白名单文档附件。

V1 不提供平台原生视频上传和转码，视频先使用外部链接卡片。

---

## 11. 搜索

### 11.1 V1

使用 MySQL 完成基础搜索：

- 标题；
- 摘要；
- 正文纯文本；
- 标签；
- 作者与博客名称；
- 系列名称。

可以根据实际效果评估 MySQL FULLTEXT，但不作为必须条件。

### 11.2 后续扩展

只有在内容数量和搜索需求明显增长后，再评估：

```text
OpenSearch
Elasticsearch
```

搜索引擎不作为 V1 启动依赖。

---

## 12. 测试技术

### 12.1 后端

| 类型 | 技术 |
|---|---|
| 单元测试 | JUnit 5 |
| Spring 集成测试 | Spring Boot Test |
| Mock | Mockito |
| API 测试 | MockMvc |
| 真实数据库测试 | Testcontainers，按需使用 |

重点覆盖：

- 注册事务；
- 管理员与社区用户隔离；
- 文章权限；
- 文章版本；
- 审核和发布状态机；
- 定时发布；
- 数据唯一约束。

### 12.2 前端

| 类型 | 技术 |
|---|---|
| 单元与组件测试 | Vitest |
| React 组件测试 | React Testing Library |
| 端到端测试 | Playwright |

核心 E2E：

```text
注册
→ 自动创建个人博客
→ 创建文章
→ 保存草稿
→ 提交审核
→ 后台审核
→ 公开访问文章
```

---

## 13. API 与工程规范

### 13.1 API 前缀

```text
用户端：/api/v1/**
管理端：/admin-api/**
```

### 13.2 API 文档

使用 Springdoc OpenAPI，并将用户端和管理端接口分组。

### 13.3 接口规则

- Controller 不直接调用 Mapper；
- Controller 不承担复杂权限和事务；
- 资源权限由专门 PermissionService 判断；
- 错误码按业务领域划分；
- 所有 BIGINT ID 在 JSON 中按字符串返回；
- 所有写操作考虑幂等和并发冲突。

---

## 14. 日志与可观测性

V1 沿用 Spring Boot 与脚手架日志体系：

- 应用日志；
- 管理员操作日志；
- 登录日志；
- 审核和处罚日志；
- 定时任务日志；
- 业务异常日志。

日志中禁止输出：

- 明文密码；
- 完整 Token；
- 验证码；
- 完整手机号和身份凭据；
- 私密文章正文。

后续可按实际需要接入：

```text
Spring Boot Actuator
Prometheus
Grafana
Sentry
```

这些不作为 V1 强制依赖。

---

## 15. 部署技术

### 15.1 V1 推荐部署

```text
Linux
Docker Compose
Nginx
MySQL
Redis
Java 后端
pxczxn-web
pxczxn-admin
本地存储或 MinIO
```

Redis 可以启用；如果部署资源有限，也可以在初期关闭非必要缓存能力。

### 15.2 Nginx 职责

- HTTPS；
- 反向代理；
- 用户端和管理端域名分流；
- 静态资源缓存；
- 上传大小限制；
- 基础安全响应头。

### 15.3 环境配置

至少区分：

```text
local
开发者本地

dev
联调环境

prod
正式环境
```

密钥和密码不得写入仓库。

---

## 16. V1 明确不引入

- Spring Cloud；
- 微服务和服务注册中心；
- Kubernetes；
- Kafka；
- RabbitMQ；
- MongoDB；
- 图数据库；
- Elasticsearch / OpenSearch 强依赖；
- Flyway 和 Liquibase；
- 实时多人协作编辑；
- WebSocket 自由聊天系统；
- 原生视频转码；
- 支付、钱包和分账；
- 自研 ORM；
- 自研认证框架。

---

## 17. 技术栈总表

| 领域 | 技术与决定 |
|---|---|
| 项目代号 | `pxczxn-community` |
| Java 包名 | `top.pxczxn.community` |
| 后端语言 | Java 21 |
| 后端基础 | 基于 `pxczxn-admin` 工程开发 |
| 后端框架 | Spring Boot 3.x |
| 数据访问 | MyBatis-Plus |
| 身份认证 | Sa-Token，多账号体系 |
| 构建工具 | Maven Wrapper |
| 主数据库 | MySQL 8.x |
| SQL 迁移 | 仓库内版本化 SQL，不使用 Flyway |
| Redis | 可选增强：会话、缓存、限流、计数和锁 |
| 定时任务 | Quartz |
| 业务事件 | Spring ApplicationEvent |
| 用户端 | Next.js + React + TypeScript |
| 用户端样式 | Tailwind CSS |
| 用户端包管理 | pnpm |
| 管理端 | Vue 3 + TypeScript + Vite |
| 管理端 UI | Naive UI |
| 富文本编辑器 | Tiptap / ProseMirror |
| Markdown 编辑器 | CodeMirror 6 |
| Markdown 处理 | unified + remark + rehype |
| 代码高亮 | Shiki |
| 数学公式 | KaTeX |
| 文件存储 | 本地 / MinIO / OSS |
| 搜索 | V1 使用 MySQL，后期再评估搜索引擎 |
| 后端测试 | JUnit 5 + Spring Boot Test + Mockito |
| 前端测试 | Vitest + Playwright |
| 部署 | Docker Compose + Nginx |
| 架构 | 前后端分离的模块化单体 |

---

## 18. 最终技术定位

```text
Java 21 + Spring Boot 3 + MyBatis-Plus + Sa-Token
负责社区业务与平台接口

pxczxn-admin
只作为后台和基础能力脚手架

Next.js + TypeScript + Tailwind CSS
负责普通用户端和公开博客

Vue 3 + Naive UI
负责平台运营管理端

MySQL
负责全部核心业务数据

Redis
负责会话、缓存、限流、计数和锁等增强能力
```

项目当前坚持：

> `pxczxn-admin` 是项目唯一后台工程名称；原始脚手架来源只作为技术来源记录。

> V1 先把模块化单体做稳，再根据真实用户量决定是否增加新的基础设施。
