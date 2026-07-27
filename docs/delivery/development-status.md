# 开发状态

> 更新日期：2026-07-27

## 路线状态总览

| 阶段 | 状态 | 说明 |
| --- | --- | --- |
| V1（M1 + M2） | 完成 | 个人博客、发布审核、社区互动、通知、运营治理均已验收 |
| 即时聊天 | 完成 | 单聊、群聊、未读状态与安全 WebSocket Ticket 已纳入当前基线 |
| M2.5 产品语义与信息架构校正 | 完成 | 发现页、入口回跳、动态路由、管理端运营信息架构及运行时演示文案已完成收口 |
| M3 团队博客与协作创作 | 已规划 | 团队、投稿、系列、共创 |
| M4 平台治理与内容安全 | 已规划 | 举报、屏蔽、申诉、处罚、反滥用 |
| M5 发现、搜索与创作者体验 | 已规划 | 搜索、可解释发现、SEO、RSS、统计、性能 |
| M6 公网发布与持续运营 | 已规划 | 生产环境、CI/CD、备份、监控、灰度 |

正式路线与任务清单见：

- `docs/planning/M2.5-M6-delivery-plan.md`
- `docs/delivery/M2.5-M6-task-breakdown.md`
- `docs/delivery/M2.5-acceptance.md`
- `blog-community-engineering-docs-v0.3/docs/01-product-scope.md`

## M1-T001 社区业务模块初始化

状态：完成

交付内容：

- 基于本机后台脚手架建立 `pxczxn-backend`。
- 后端编译目标升级为 Java 21，Maven 坐标使用 `top.pxczxn`。
- 新增 `pxczxn-biz`，根包为 `top.pxczxn.community`。
- 建立 `user`、`blog`、`article`、`taxonomy`、`moderation`、
  `notification` 和 `shared` 基础领域包。
- 新增 `pxczxn-web-api` 与公开探活接口 `GET /api/v1/health`。
- 社区用户使用独立登录类型 `community` 和 Token 名称
  `pxczxn-community-token`，不复用管理员 `StpUtil`。
- Redis 关闭时，系统配置使用本地缓存，Sa-Token 使用内存持久层，
  验证码与登录重试限制使用进程内降级存储。
- 旧后台登录、退出和社区探活均已通过真实启动验证。

验证结果：

```text
Java: 21.0.11
MySQL: 8.0.46
Maven Reactor: 26 modules SUCCESS
Automated tests: 2 passed
GET /api/v1/health: 200 / UP
POST /api/auth/login: 200 / admin / token issued
POST /api/auth/logout: 200
Redis: intentionally unavailable during verification
```

## M1-T002 核心数据库迁移

状态：完成

交付内容：

- 建立 `database/migrations`、`seeds`、`verify` 和 `rollback`。
- V001 创建文档规定的 15 张 M1 基础表。
- 建立个人博客、登录标识、slug、默认分类、文章标签和活动审核任务等
  关键唯一约束。
- 建立状态检查约束、关系外键和查询索引。
- 迁移记录版本、执行顺序、锁风险、备份要求、失败处理与受保护回滚。
- MySQL 中全部时间按 UTC 写入，业务主键由后端生成。

验证结果：

```text
MySQL: 8.0.46
Disposable database: first run PASS / repeat run PASS
Local database: first run PASS / repeat run PASS
Core table count: 15
Indexes, foreign keys, unique constraints and checks: PASS
```

## M1-T003 社区用户注册

状态：完成

交付内容：

- 新增用户名与邮箱可用性检查、注册 API。
- 用户名和邮箱在业务层统一规范化，接口层和业务层双重校验。
- 密码使用 BCrypt，并拒绝超过 BCrypt 72 字节安全边界的输入。
- 单一事务创建用户、邮箱登录账号、偏好、个人博客、博客设置和默认
  “未分类”，最后回写个人博客 ID。
- 预检查提供友好反馈，数据库唯一约束处理并发竞争。
- BIGINT ID 在 API 响应中序列化为字符串。
- 旧后台管理员拦截器和响应加密不再处理 `/api/v1/**`，社区接口域保持独立。

验证结果：

```text
Registration-related automated tests: 6 passed
Successful real registration: PASS
Username/email availability before and after registration: PASS
Duplicate registration result: 409 / PASS
Created aggregate rows: 1 user + 1 login + 1 preference
                        + 1 personal blog + 1 setting + 1 default category
Password storage: BCrypt ($2a$)
Forced mid-transaction slug conflict: 409 / all prior writes rolled back
Temporary verification data: removed
```

## M1-T004 登录与会话

状态：完成

交付内容：

- 邮箱密码登录、社区 Token 签发、退出与当前用户 API。
- 社区会话固定使用 `pxczxn-community-token` 请求头和 `community`
  登录类型，Token 有效期 7 天，30 分钟无操作超时。
- 管理员 `Authorization` Token 与社区 Token 双向隔离。
- 登录和当前用户读取均校验社区用户状态。
- 连续 5 次密码错误后锁定 15 分钟；失败计数在异常响应时仍提交。
- 登录成功清除失败计数并以 UTC 更新用户与登录账号时间。
- 当前用户响应包含本人的邮箱与个人博客摘要，所有 BIGINT 为字符串。

验证结果：

```text
Community business and API automated tests: 15 passed
Email/password login: 200 / PASS
Community token name and 7-day timeout: PASS
GET /api/v1/account/me: 200 / PASS
Admin Authorization used as community token: 401 / isolated
Logout then reuse old token: 401 / invalidated
Wrong-password response sequence: 401, 401, 401, 401, 423
Correct password during lock: 423
Database failure count / lock time: 5 / 15 minutes
Redis: intentionally unavailable
Temporary verification data: removed
```

## M1-T005 个人博客资料

状态：完成

交付内容：

- 新增本人博客读取、资料更新、设置更新和公开博客详情 API。
- 同时校验个人博客 ID、所有者和博客类型，阻断伪造关系越权。
- 博客公开 slug 在资料编辑中保持稳定。
- 公开查询只允许活动且未删除的博客，并校验所有者公开状态。
- 公开响应不包含邮箱、账号状态、认证状态、登录信息和博客权限设置。
- 浅色、深色、星空主题使用固定白名单保存。
- 对评论范围、可见性、转载策略、长度和文件 ID 进行业务校验。
- 对齐注册规则，公开博客 slug 支持用户名中的下划线。

验证结果：

```text
Community business automated tests: 17 passed
Community API automated tests: 7 passed
T005-specific automated tests: 9 passed
Authenticated profile read/update: 200 / PASS
Theme update to starry: 200 / PASS
Public blog lookup with underscore slug: 200 / PASS
Public response private-field scan: no leaks
Slug change attempt: 400 / rejected
Unauthenticated private profile read: 401 / rejected
Redis: intentionally unavailable
Temporary verification data: removed
```

## M1-T007 社区文件引用能力

状态：完成

交付内容：

- 复用 pxczxn 平台文件存储策略，但使用独立的社区文件元数据和身份边界。
- 新增社区文件上传、本人详情、本人内容读取、逻辑删除和受控公开读取 API。
- 基于真实文件签名、DOCX 容器和 UTF-8 内容识别类型，不信任客户端 MIME。
- 文件大小限制为 20MB，并拒绝扩展名与真实类型不一致的输入。
- SVG 使用严格安全白名单，拒绝脚本、事件、外链、实体和危险嵌入。
- 文件对象记录社区所有者、随机对象键、真实 MIME 和 SHA-256。
- 博客头像、背景与文件引用在同一数据库事务内更新。
- 跨用户引用返回 403，存在引用时删除返回 409。
- 删除采用逻辑状态，物理对象留给延迟清理。
- 只有活动的博客头像和背景引用可以通过公开文件接口读取。

验证结果：

```text
Community business automated tests: 24 passed
Community API automated tests: 7 passed
T007-specific automated tests: 7 passed
Real PNG upload with falsely claimed video MIME: detected image/png
Public read before business reference: 404
Blog avatar reference and public binary read: 200 / image/png
Cross-user file reference: 403
Delete while referenced: 409
Clear reference then logical delete: 200
Public read after logical delete: 404
Temporary database and storage objects: removed
```

## M1-T006 分类与平台标签

状态：完成

交付内容：

- 新增本人博客分类列表、创建、更新和删除 API。
- 分类管理校验个人博客所有权，限制最多 100 个分类并规范化 slug。
- 默认“未分类”不可删除；删除普通分类时在事务内迁移文章并重算数量。
- 新增公开平台标签查询，只暴露 `ACTIVE` 标签。
- 新增管理端标签列表、创建、更新、隐藏和逻辑删除 API。
- 文章标签最多五个，必须来自启用标签，并在替换关系后重算使用次数。
- V002 注册社区管理菜单、平台标签页面和四项 RBAC 权限。
- `/admin-api/**` 纳入 Sa-Token 登录与权限拦截，跨域请求支持 `PATCH`。

验证结果：

```text
Community business automated tests: 30 passed
Community API automated tests: 7 passed
Category create/update/delete/default protection: PASS
Duplicate category slug: 409 / PASS
Unauthenticated category access: 401 / PASS
Unauthenticated admin tag access: 401 / PASS
Admin tag CRUD and status transitions: PASS
Active tag public visibility / hidden exclusion: PASS
V002 menus / permissions / admin grants: 6 / 4 / 6
Temporary verification data: removed
Login captcha configuration: restored
```

## M1-T008 文章草稿与版本

状态：完成

交付内容：

- 新增文章创建、编辑读取、手动保存、自动保存、逻辑删除和版本 API。
- 富文本 JSON 与 Markdown 坚持单一主内容源，模式切换必须提交完整目标源。
- 服务端完成安全渲染、HTML 白名单清洗、目录、纯文本、哈希、字数和阅读时长
  派生。
- 每次显式保存产生不可变版本；无变化自动保存不产生重复版本。
- 历史版本恢复通过创建 `RESTORE` 新版本完成，不修改旧版本。
- 更新、恢复和删除使用 `expectedLockVersion` 乐观锁，冲突返回 409 并回滚
  候选版本。
- 文章标签、封面和正文版本文件引用在同一事务保存。
- 逻辑删除保留版本与文件引用作为回收链路。
- BIGINT 请求支持字符串输入，所有响应 ID 均序列化为字符串。

验证结果：

```text
Community business automated tests: 40 passed
Community API automated tests: 10 passed
T008-specific automated tests: 13 passed
Markdown and rich-text create/save/switch: PASS
Dangerous script and javascript URL removal: PASS
Unchanged autosave deduplication: PASS
Optimistic conflict and inserted-version rollback: PASS
Version list/detail/restore: PASS
Cross-user access: rejected
Logical delete / version preservation: PASS
Content file link/copy/draft privacy: PASS
Temporary database, storage and file objects: removed
```

## M1-T009 文章权限服务

状态：完成

交付内容：

- 新增统一文章权限服务，集中处理编辑查看、公开详情、公开列表、编辑、删除、
  提交审核、发布和平台审核动作。
- 拒绝结果明确区分未登录 401、身份无权 403、资源不可见 404 和工作流冲突
  409。
- 个人博客所有者和未来团队 `OWNER`、`ADMIN`、`EDITOR`、`AUTHOR` 角色使用
  同一角色解析扩展点。
- 新增关注关系扩展点；关系模块未交付时，`FOLLOWERS_ONLY` 默认安全拒绝。
- 公开读取要求固定公开版本；删除、下架和未公开内容统一隐藏资源存在性。
- `UNLISTED` 允许直接详情访问但不进入公开列表，`PRIVATE` 仅允许必要协作者。
- 已发布文章的新版本审核期间继续允许旧公开版本展示。
- 社区作者与运营管理平台审核使用各自独立会话；后台审核要求
  `community:article:review`。
- 草稿、版本和删除流程均已接入统一权限入口，团队编辑产生的新版本记录真实
  操作者。
- 权限拒绝写入安全日志，但不记录 Token 和正文。

验证结果：

```text
Community business automated tests: 51 passed
Community API automated tests: 10 passed
T009-specific automated tests: 11 passed
Owner / cross-user / anonymous editor: 200 / 403 / 401
Pending-review edit: 409
Rejected edit leaves version and lock unchanged: PASS
Public/private/unlisted/follower-only matrix: PASS
Published-version continuity during review: PASS
Personal owner and future team role matrix: PASS
Platform review 401 / 403 / 409 separation: PASS
Temporary verification data: removed
```

## M1-T010 文章审核提交

状态：完成

交付内容：

- 新增文章提交审核、撤回审核和本人审核状态 API。
- 提交在事务内固定当前不可变版本，同时创建不可删除的审核任务并切换文章
  审核状态。
- 提交与撤回使用文章乐观锁，审核任务使用独立锁版本。
- 8-80 位客户端幂等键受数据库唯一约束；相同提交重复调用返回原任务。
- V003 新增可配置关键词规则表，不在迁移中硬编码地区相关运营规则。
- 自动审核对标题、摘要和固定版本纯文本执行 NFKC、大小写及空白归一化。
- 无命中自动通过；`WARN` 带告警通过；`REVIEW` 进入人工队列；`BLOCK`
  自动拒绝，最高严重级别优先。
- 结果只记录命中规则 ID 和数量，不把 Token 或敏感正文写入日志。
- 私密文章和空正文不能进入公开审核。
- 审核中禁止继续编辑或删除；撤回后保留历史任务并恢复编辑。
- 已有公开版本的文章提交新版本时继续展示旧公开版本，发布状态与审核状态
  独立维护。

验证结果：

```text
Community business automated tests: 63 passed
Community API automated tests: 13 passed
T010-specific automated tests: 15 passed
V003 first run / repeat run / structure verification: PASS
AUTO APPROVE / WARN / REVIEW / BLOCK matrix: PASS
Idempotent replay returns same task: PASS
Fixed review version on every audit task: PASS
Edit during review: 409
Withdraw then resume editing: 200
Private article submission: 409
Temporary verification data and policy rules: removed
```

## M1-T011 管理端审核 API

状态：完成

交付内容：

- 新增审核队列与历史分页 API，支持状态、风险等级和提交时间筛选。
- 审核详情只读取任务固定的不可变文章版本，不读取后续当前草稿。
- 领取任务同时切换任务和文章状态，分别使用乐观锁防止并发抢单。
- 同一管理员重复领取幂等；其他管理员或旧锁操作返回 409。
- 通过、要求修改和驳回只允许领取人操作；退修、驳回必须填写说明。
- 审核任务和文章发布/审核状态在同一事务切换，保留固定版本和全部历史。
- 已有公开版本的文章审核新版本时，退修或驳回不影响旧公开版本。
- 审核结果在主事务提交后通过独立事务生成站内通知和未读收件人。
- 通知失败被隔离并记录元数据，不回滚或误报已经成功的审核决定。
- V004 注册运营管理端审核菜单、7 项权限并授权默认 admin 角色。
- 所有接口使用运营管理端会话和 RBAC，社区 Token 不能调用。

验证结果：

```text
Community business automated tests: 74 passed
Community web API automated tests: 13 passed
pxczxn Admin API automated tests: 4 passed
T011-specific automated tests: 15 passed
V004 first run / repeat run: PASS
Menus / permissions / admin grants: 7 / 7 / 8, PASS
Admin list / fixed-version detail: 200 / PASS
Claim / stale lock: MANUAL_REVIEWING / 409
Approve: task APPROVED / article APPROVED / task lock 2
Post-commit notification / unread recipient: 1 / 1
Temporary verification data: removed, remaining 0
```

## M1-T012 文章发布

状态：完成

交付内容：

- 新增作者手动发布 API，接入统一文章发布权限。
- 仅允许审核通过、审核版本与当前版本一致且固定版本真实存在的文章发布。
- 使用文章乐观锁和条件更新原子切换 `published_version_id`，并递增锁版本。
- `MANUAL` 文章审核通过后由作者发布；`IMMEDIATE` 文章在自动或人工审核
  通过时直接发布；`SCHEDULED` 留给定时发布任务。
- canonical 统一生成为
  `/{blogSlug}/{articleId}/{articleSlug}`。
- 已有公开版本在新版本审核期间保持可见，发布成功时才原子替换公开指针，
  旧版本继续保留在不可变版本历史。
- 同一审核版本重复发布幂等，返回已有发布时间、公开指针和
  `idempotentReplay=true`。
- 其他旧锁、审核版本漂移和并发覆盖返回 409。
- 发布成功产生只含业务元数据的 `ArticlePublishedEvent`，区分作者、
  自动审核和管理员审核触发来源。
- 所有 API 响应中的 BIGINT 业务 ID 均序列化为字符串。

验证结果：

```text
Community business automated tests: 83 passed
Community web API automated tests: 15 passed
pxczxn Admin API automated tests: 4 passed
T012-specific automated tests: 11 passed
MANUAL review / publish: APPROVED / PUBLISHED
Repeated manual publish: idempotentReplay=true
IMMEDIATE auto review / publish: APPROVED / PUBLISHED
Published pointer / canonical / UTC publish time: PASS
Runtime health / unauthenticated admin boundary: UP / 401
Captcha / encryption / scope: true / true / global
Temporary verification data: removed, remaining 0
```

## M1-T014 公开博客与文章

状态：完成

交付内容：

- 扩展公开博客资料，返回博客类型、三色主题、受控主题配置和 SEO 数据。
- 公开文章数使用实时公开条件统计，不计入草稿、私密和不列出文章。
- 新增公开博客分类、分类文章分页和文章详情 API。
- 分类分页连接作者公开状态，按发布时间和文章 ID 稳定倒序，单页最多 50 条。
- 公开详情接入统一 `VIEW_DETAIL` 权限，并且只读取
  `published_version_id` 指向的不可变安全版本。
- 公开响应只返回安全 HTML、目录、作者、博客、分类、启用标签、时间、
  计数、canonical 和 SEO，不返回编辑器源内容、审核状态或未发布版本。
- `PUBLIC` 进入详情与列表；`UNLISTED` 只允许直接详情；私密、草稿、删除、
  下架、隐藏博客和不可公开作者统一按 404 隐藏。
- 已发布文章产生新草稿后继续展示旧公开正文，直到新版本审核并发布。
- 公开文件读取校验真实业务目标：只允许活动博客头像/背景、可公开文章封面
  和当前公开版本正文图片，草稿版本图片不能通过文件 ID 越权读取。
- 所有 BIGINT 业务 ID 继续以字符串返回，canonical 保持
  `/{blogSlug}/{articleId}/{articleSlug}`。

验证结果：

```text
Community business automated tests: 95 passed
Community web API automated tests: 18 passed
pxczxn Admin API automated tests: 4 passed
T014-specific automated tests: 15 passed
Public blog theme / SEO / live article count: PASS
Category filter / public page / detail: PASS
Safe HTML / source-field scan: PASS / no leaks
UNLISTED detail / list: 200 / hidden
Draft / private anonymous detail: 404 / 404
Published pointer continuity with new draft: PASS
Runtime health / unauthenticated admin boundary: UP / 401
Captcha / encryption / scope: true / true / global
Temporary verification data: removed, remaining 0
```

## M1-T013 定时发布

状态：完成

交付内容：

- 定时发布继续使用统一 `POST /api/v1/articles/{articleId}/publish`，支持设置、
  修改和取消 UTC 计划时间，并返回当前计划时间。
- 仅审核通过且当前版本等于审核固定版本的 `SCHEDULED` 文章可以进入待发布
  状态；相同计划重复请求幂等。
- V005 新增 `article_publish_task`，持久化任务版本、计划时间、尝试次数、
  下次重试时间、状态、结构化错误码和独立锁版本。
- 活动任务生成列唯一约束保证每篇文章最多一个活动任务；修改或取消计划与文章
  乐观锁更新处于同一事务，旧任务不能覆盖新状态。
- V005 在运营管理平台 `sys_job` 注册每分钟 Quartz 扫描任务，禁止 Job 并发，
  Redis 不可用时仍由 MySQL 驱动。
- Quartz 领取后再次校验文章、审核状态、固定版本、计划时间、博客和作者状态，
  然后以文章乐观锁原子切换 `published_version_id`、canonical 和发布时间。
- 并发或重复执行只有一个公开指针更新成功；相同版本已发布按幂等成功处理。
- 基础设施异常进入最多 3 次的 1/2/4 分钟退避；运行中进程退出可在 5 分钟后
  恢复。不可重试业务失败和重试耗尽进入 `PUBLISH_FAILED`。
- 失败任务只保存稳定错误码与通用说明，不保存正文、密码或 Token；已有旧公开
  版本在新版本等待或失败时继续可用。

验证结果：

```text
Community business automated tests: 106 passed
Community web API automated tests: 19 passed
pxczxn Job automated tests: 1 passed
pxczxn Admin API automated tests: 4 passed
T013-specific automated tests: 14 passed
V005 first run / repeat run: PASS
V005 table / required columns / Quartz job: 1 / 11 / PASS
Schedule cancellation: APPROVED / CANCELLED
Quartz success: PUBLISHED / SUCCEEDED / attempt 1
Permanent business failure: PUBLISH_FAILED / AUTHOR_NOT_PUBLISHABLE
Repeated publication: idempotentReplay=true
Runtime health / unauthenticated admin boundary: UP / 401
Captcha / encryption / scope: true / true / global
Temporary verification data: removed, remaining 0
```

## M1-T015 管理端页面与三色主题

状态：完成

交付内容：

- 在 pxczxn 运营管理端 Vue 3、Vite、TypeScript、Naive UI、RBAC 和动态菜单基础上，
  完成社区工作台、用户、博客、文章、审核和标签六个运营页面。
- 新增管理端社区统计、用户、博客、文章分页和安全文章详情 API。
- V006 注册社区查询菜单与权限，默认管理员角色可按权限访问页面与操作按钮。
- 工作台接入真实总量、七日趋势、发布审核健康度和快捷入口。
- 用户、博客和文章页面提供组合筛选、分页、加载、空状态和错误重试。
- 审核页面完成队列、详情、领取、通过、退修和驳回闭环，并携带乐观锁版本。
- 标签页面完成创建、编辑、隐藏和软删除；默认列表排除已删除记录。
- 管理端提供浅色、深色、星空三套可持久化主题，登录页、站点标题、菜单品牌、
  favicon 和版权统一为 `星语社区运营中心`。
- 社区 Token 与管理员 Token 继续隔离，管理接口统一由登录和 RBAC 拦截。

验证结果：

```text
PlatformTagService regression tests: 4 passed
pxczxn Admin API automated tests: 8 passed
Admin production build: 4932 modules / PASS
Backend Maven package: 26 modules / SUCCESS
V006 menus / permissions / admin grants: 9 / 5 / 10, PASS
Final packaged admin login and six routes: PASS
Light / dark / starry visual states: PASS
Community token calling admin dashboard: 401 / isolated
Soft-deleted tag hidden / verification row cleanup: PASS / remaining 0
Runtime health on 8849: UP
```

## M1-T016 星语社区博客端

状态：完成

交付内容：

- 按用户提供的八组原型完成登录注册、团队主页、团队工作台、投稿、共创文章、
  动态、收藏和个人设置页面，并统一品牌为“星语社区”。
- 补充真实个人博客、公开文章和文章编辑路由，接入 `/api/v1/**` 社区接口和
  独立社区会话。
- 完成注册并自动登录、自动创建个人博客、文章保存、提交审核、审核通过、手动发布、
  公开文章和公开博客列表的真实浏览器闭环。
- 使用浅色、深色和星空三套持久化主题，并完成桌面布局与响应式降级。
- 博客端、管理端、后端最终固定端口分别为 `8847`、`8848`、`8849`。
- 发布 Sites 私有生产预览：
  `https://xingyu-community-pxczxn.pxczxn.chatgpt.site`。

验证结果：

```text
vinext production build: PASS / 12 routes
Rendered route tests: 3 passed
ESLint: PASS / 0 issues
Eight prototype routes: browser PASS
Real register -> review -> publish -> public flow: PASS
CORS from http://localhost:8847 to backend 8849: PASS
Temporary verification data: removed, remaining 0
```

详细记录见 `docs/delivery/M1-T016-community-web.md`。

## M1-T017 自动化测试与 M1 完成验收

状态：完成

交付内容：

- 执行后端 26 模块全量 Maven `verify`，覆盖注册、会话、权限、版本、审核、发布、
  定时发布、公开安全、通知和审计链路。
- 汇总 29 份 Surefire 报告，共 135 个测试，失败、错误和跳过均为 0。
- 对当时的本地过渡库只读执行 V001 至 V006 全部数据库结构验证脚本。
- 复核 Redis 不可用降级、管理员与社区 Token 隔离、私密/未审核/删除/下架内容
  不泄漏，以及关键操作的固定版本、历史任务和安全日志。
- 复核博客端测试、博客端 lint、管理端生产构建和后端运行时健康检查。

验证结果：

```text
Maven Reactor: 26 modules SUCCESS
Backend automated tests: 135 passed / 0 failed / 0 errors / 0 skipped
Database verify V001-V006: PASS
Web build / tests / lint: PASS
Admin production build: PASS
Runtime health on 8849: UP
Redis: intentionally unavailable
M1 completion definition: PASS
```

详细记录见 `docs/delivery/M1-T017-automated-tests.md`。

## M2-T001 社区互动数据结构

状态：完成

交付内容：

- V007 新增关注、动态、评论、点赞、收藏条目、收藏夹与收藏夹映射七张互动表。
- 建立幂等唯一约束、目标查询索引、状态字段、软删除字段、计数字段与 14 个外键。
- 迁移可重复执行，并提供独立结构核验脚本。

验证结果：

```text
V007 first run / repeat run: PASS / PASS
Interaction tables: 7
Required follow columns: 8
Business unique constraints: 5
Foreign keys: 14
```

详细记录见 `docs/delivery/M2-T001-interaction-schema.md`。

## M2-T002 博客关注

状态：完成

交付内容：

- 完成博客关注、取消、关系读取和关注设置更新。
- 支持全部、重要、静默三档通知与个人博客特别关注。
- 完成关注、粉丝、互关计数及两类分页列表。
- 关系与 `follower_count` 在同一事务写入，重复操作幂等，BIGINT ID 以字符串返回。
- 匿名读取、社区登录、博客/用户状态、自关注和特别关注范围均已校验。

验证结果：

```text
Blog follow service tests: 7 passed
Follow permission resolver tests: 2 passed
Blog follow controller tests: 2 passed
Backend automated tests after T003 regression: 168 passed
Real bidirectional follow and mutual relationship: PASS
Settings update and idempotent unfollow: PASS
Temporary users / orphan follow rows: 0 / 0
Runtime backend port: 8849
```

详细记录见 `docs/delivery/M2-T002-blog-follows.md`。

## M2-T003 内容点赞与喜欢列表

状态：完成

交付内容：

- 完成文章、动态、评论与回复点赞、取消、关系查询和喜欢列表。
- 关系唯一约束与目标计数处于同一事务，重复点赞和重复取消均幂等。
- 文章、动态、评论、关联文章和转发源统一经过内容可见性检查。
- `FOLLOWERS_ONLY` 文章已接入真实博客关注关系。
- V008 新增喜欢列表公开范围，默认私密，支持公开、关注者和互关。
- 完成本人隐私设置与按用户读取喜欢列表，无权访问统一按不存在处理。

验证结果：

```text
V008 first run / repeat run: PASS / PASS
Community business tests: 134 passed
Community web API tests: 25 passed
Backend automated tests: 168 passed / 0 failed / 0 errors / 0 skipped
Real like counter flow: 0 -> 1 -> 1 -> 0 -> 0
PRIVATE / FOLLOWERS_ONLY / MUTUAL_ONLY / PUBLIC: PASS
Temporary users / articles / orphan interactions: 0 / 0 / 0
Runtime backend port: 8849 / UP
```

详细记录见 `docs/delivery/M2-T003-content-likes.md`。

## M2-T004 收藏与收藏夹

状态：完成

交付内容：

- 新用户注册时创建私密“全部收藏”，旧用户首次使用时幂等补建。
- 完成自定义收藏夹创建、编辑、排序、软删除和四级公开范围。
- 文章与动态使用唯一收藏关系，一个内容可加入多个收藏夹。
- 收藏关系、收藏夹映射、收藏夹条目数与内容收藏数在同一事务保持一致。
- 收藏列表复用内容可见性检查；作者可见收藏用户但看不到私密收藏夹结构。

验证结果：

```text
Community business tests: 146 passed
Community web API tests: 28 passed
Backend automated tests: 183 passed / 0 failed / 0 errors / 0 skipped
Real favorite item / two folder mappings: 1 / 2
Repeated favorite target count: 1
PRIVATE / PUBLIC / FOLLOWERS_ONLY / MUTUAL_ONLY: PASS
Final target / item / mapping counts: 0 / 0 / 0
Temporary users / articles / orphan interactions: 0 / 0 / 0
Runtime backend port: 8849 / UP
```

详细记录见 `docs/delivery/M2-T004-favorites.md`。

## M2-T005 评论与回复

状态：完成

交付内容：

- 完成文章和动态的一级评论、平铺回复、评论分页、回复预览和独立回复分页。
- 评论和回复复用已有评论点赞关系，目标评论数与评论写入处于同一事务。
- 完成个人博客全部登录用户、关注者、互关、博主关注和关闭评论五种范围。
- 预留团队关注者和团队成员范围，团队成员通过扩展解析器接入。
- 评论提交执行 NFKC、长度、控制字符、HTML 转义、HTTP(S) 链接化和 Jsoup
  最终白名单净化。
- 复用关键词规则完成 `BLOCK`、`REVIEW`、`WARN` 三路治理。
- 完成用户幂等删除、作者/博客隐藏整条讨论和不可变治理事件。
- V009 新增评论治理事件表并为完整评论范围增加数据库检查约束。
- 提供可重复的真实 E2E 脚本，失败或成功后均清理临时数据。

验证结果：

```text
V009 repeat run / structure verification: PASS / PASS
Community business tests: 163 passed
Community web API tests: 32 passed
Backend automated tests: 204 passed / 0 failed / 0 errors / 0 skipped
Safe rendering / root and flat reply / comment like: PASS
BLOCK / REVIEW / WARN: 400 / PENDING_REVIEW / AUTO_APPROVED_WITH_WARNING
FOLLOWERS_ONLY before / after follow: 403 / 200
MUTUAL_ONLY before / after mutual follow: 403 / 200
BLOGGER_FOLLOWING / DISABLED / restriction: 200 / 403 / 403
First / repeated self-delete affected rows: 1 / 0
Governance event rows: 15
Temporary users / articles / rules / comments: 0 / 0 / 0 / 0
Runtime backend port: 8849 / UP
```

详细记录见 `docs/delivery/M2-T005-comments.md`。

## M2-T006 动态发布、动态详情与分享

状态：完成

交付内容：

- 完成文本、链接、视频链接、文章分享、项目更新、代码、纯转发和引用动态发布。
- 完成公开动态流、博客动态列表、本人动态列表、详情、稳定分享路径和幂等软删除。
- 正文经过 NFKC、HTML 转义、安全链接化与 Jsoup 白名单净化；外链只允许
  HTTP(S) 合法主机。
- 发布身份、账号限制、内容关键词、文章引用和递归转发源均执行统一权限校验。
- 关注者限定、私密、待审、删除和下架源内容不能通过公开转发扩大访问范围。
- 动态复用点赞、收藏和评论能力，互动关系与目标计数在同一事务更新。
- 纯转发与引用发布、删除时事务化维护源动态转发计数，重复删除幂等。
- V010 增加公开动态流索引与链接、引用、纯转发内容形状检查约束。
- 提供完整自动化测试和可重复真实 E2E 脚本，临时数据清理为零。

验证结果：

```text
V010 repeat run / structure verification: PASS / PASS
T006-focused automated tests: 19 passed
Backend automated tests: 223 passed / 0 failed / 0 errors / 0 skipped
Safe rendering / unsafe URL rejection / article card: PASS / 400 / PASS
REPOST / QUOTE shape validation: 400 / 400
Moment like / favorite / comment counts: 1 / 1 / 1
FOLLOWERS_ONLY before / after follow: 404 / 200
Restricted source broadening / private other user: 404 / 404
BLOCK / REVIEW / WARN: 400 / PENDING_REVIEW / AUTO_APPROVED_WITH_WARNING
Repeated delete / final source repost count: idempotent / 0
Temporary users / articles / moments / rules / comments: 0 / 0 / 0 / 0 / 0
Runtime backend port: 8849 / UP
```

详细记录见 `docs/delivery/M2-T006-moments-and-sharing.md`。

## M2-T007 站内通知与未读计数

状态：完成

交付内容：

- 完成关注、点赞、收藏、评论、回复、提及、转发、文章/动态发布和审核结果通知。
- 按 `ALL`、`IMPORTANT`、`MUTED` 与特别关注设置分发博客更新，并保留
  `NORMAL`、`HIGH` 两级重要性。
- 相同通知在十分钟内聚合，关系与唯一去重键防止重复；新动作可把已读聚合通知
  重新置为未读。
- 通知只在主事务提交后以独立事务写入，失败不会回滚已经成功的核心业务。
- 完成八类通知分页、状态筛选、单条已读、分类全部已读、全部已读和分类未读计数。
- 自通知和取消操作通知均被抑制，评论提及最多解析 20 个有效用户名并去重。
- 读取通知时重新校验目标权限；不可访问内容隐藏发送人、摘要、目标 ID 与链接。
- V011 增加分类、重要等级、聚合次数、活动时间、唯一去重索引和检查约束。
- 提供自动化测试和可重复真实 E2E 脚本，临时数据清理为零。

验证结果：

```text
V011 first run / repeat run / structure verification: PASS / PASS / PASS
Notification-focused automated tests: 16 passed
Backend automated tests: 236 passed / 0 failed / 0 errors / 0 skipped
Three follows: 1 notification / aggregate count 3
ALL / IMPORTANT / MUTED publication notifications: 2 / 1 / 0
LIKE / FAVORITE / COMMENT / REPLY / MENTION / REPOST: PASS
Self notification / cancellation notification: suppressed / 0
Read replay / relike reopened unread: true / PASS
Read interactions then all / final unread: 3 then 2 / 0
Deleted target sender / summary / ID / link redaction: PASS
Temporary users / moments / notifications / comments: 0 / 0 / 0 / 0
Runtime backend port: 8849 / UP
```

详细记录见 `docs/delivery/M2-T007-community-notifications.md`。

## M2-T008 博客端互动页面真实接入

状态：完成

交付内容：

- 动态广场、详情、发布、点赞、收藏、分享和评论全部接入 8849 真实 API。
- 个人中心接入本人资料、收藏夹、收藏内容、喜欢、关注、粉丝、互关和真实计数。
- 新增通知中心，完成分类、未读计数、单条/全部已读、安全跳转与顶部徽标同步。
- 公开博客关注和文章喜欢/收藏不再使用前端模拟状态。
- 统一补齐 M2 前端类型、加载/空/错误状态、会话失效和私有网络 API 推导。
- 保持原型双栏布局、响应式规则、星语社区品牌和浅色/深色/星空三色主题。

验证结果：

```text
Vinext production build / ESLint: PASS / PASS
Frontend automated tests: 4 passed
Register / login / publish moment: PASS
Moment like / favorite / comment: 2 / 1 / 1
Favorite center real synchronization: PASS
Notification unread categories / read all: 2 / final 0
Blog relationship load / unfollow: true / true
Browser console errors: 0
Temporary users / moments / comments / notifications: 0 / 0 / 0 / 0
Runtime ports blog / admin / backend: 8847 / 8848 / 8849
```

详细记录见 `docs/delivery/M2-T008-blog-social-ui-integration.md`。

## M2-T009 管理端互动治理

状态：完成

交付内容：

- 新增评论治理、动态治理和互动查询页面与管理 API。
- 支持审核通过、驳回、平台下架、恢复、批量事务和逐条乐观锁。
- 详情抽屉展示不可变治理事件；写操作进入 pxczxn 操作日志。
- 新增 15 个治理权限点、18 条菜单/按钮记录和管理员授权。
- LIKE、FAVORITE、FOLLOW 查询不暴露收藏夹名称与结构。
- 完成只读角色查询 `200`、治理动作 `403` 的真实 RBAC 隔离验证。
- 管理端统一“星语社区运营中心”品牌并保留浅色、深色、星空三色主题。
- 修复局域网 HTTP 环境缺少 Web Crypto 时的 AES-GCM 响应解密问题。

验证结果：

```text
Backend targeted / full tests: 15 / 246 passed
pxczxn Admin production build: PASS
Comment / moment lifecycle: PASS / PASS
Batch comment / moment governance: 2 / 2
LIKE / FAVORITE / FOLLOW: 1 / 1 / 1
RBAC list / mutation: 200 / 403
Immutable comment / moment events: 12 / 7
Governance operation logs: 44
Light / dark / starry themes: PASS
Browser console errors: 0
Temporary users / keyword rules: 0 / 0
Runtime ports blog / admin / backend: 8847 / 8848 / 8849
```

详细记录见 `docs/delivery/M2-T009-admin-interaction-governance.md`。

## M2-T010 自动化与浏览器验收

状态：完成

交付内容：

- 汇总执行 M1、M2 后端、博客端、管理端和四组真实 E2E。
- 完成验证码登录、星语社区品牌、治理页面和博客端登录页浏览器验收。
- 完成 V012/V013 结构与品牌校验，并确认全部 E2E 临时数据清理为零。
- 修复两端生产依赖漏洞，管理端全部依赖与博客端生产依赖审计为零。
- 生产配置移除固定数据库、Druid 和 Redis 凭据，改用环境变量。
- 升级 Next.js、React/RSC、Vite、Vue 工具链并重新通过构建。
- 输出启动、迁移、回滚、安全和性能审计说明。

验证结果：

```text
Backend full tests: 246 passed
Blog build / lint / tests: PASS / PASS / 4 passed
Admin production build: PASS / Vite 7.3.6
M2 T005 / T006 / T007 / T009 E2E: PASS
Database V012 / V013 verification: PASS
Browser admin captcha login / brand / governance: PASS
Browser blog login page / brand: PASS
Admin all-dependency audit: 0
Blog production-dependency audit: 0
Temporary E2E data: 0
Runtime ports: 8847 / 8848 / 8849
```

详细记录见 `docs/delivery/M2-T010-final-acceptance.md`。

## 当前版本

星语社区当前基线为 V1（M1 + M2）加即时聊天、M2.5 产品语义与信息架构校正和 M3-T001/T002，状态：持续开发中。

M2.5 已将已有内容、互动、通知和聊天能力放到正确的平台入口与导航中：首次进入博客端为社区发现页，进入管理端为社区运营中心；原型团队、动态和协作文章不再作为写死产品路由运行。

M3 已完成团队权限地基与团队博客申请审核闭环，下一项为 **M3-T003 团队成员与邀请**；之后按 M3 的依赖顺序再进入 M4 → M5 → M6，阶段不得并行混入同一提交。

## M3-T002 团队博客申请与平台审核

状态：完成（本地数据库迁移待在具备凭据的环境执行）

交付内容：

- 社区用户可提交、查看和撤销团队博客申请；相同幂等键重放返回同一申请，申请人与 slug 的有效占用均受数据库约束保护。
- 平台审核员可在“团队申请审核”中查看待审项、批准或拒绝；批准操作在一个事务内创建团队博客、团队、OWNER 成员、默认设置、默认分类与不可变审计事件。
- 审核结果在主事务提交后以独立事务创建站内通知，通知失败不会回滚审核决定。
- 博客端 `/team-applications` 与管理端审核页面均接入真实 API，并提供加载、空、错误与权限状态。
- V024 增加有效 slug 的条件唯一索引、审核菜单、按钮权限及 admin 授权，并提供 verify、rollback 与无凭据示例的执行说明。

验证结果：

```text
Backend full test reports: 320 passed / 0 failed / 0 errors / 0 skipped
M3-T002 focused service tests: 15 passed
Blog typecheck / lint / test: PASS / PASS / 5 passed
Admin typecheck / lint / test / production build: PASS / PASS with existing warnings / PASS / PASS
Browser: /team-applications renders navigation, empty/error state and submit entry; console errors: 0
Database migration: script review PASS; real execution requires local MySQL credentials and was not run
```

## 当前工程债务与后续处理

- 团队、系列和联合创作入口已采用正式路由，但真实团队数据、资源级权限、投稿和工作台仍是 M3 范围；当前页面必须保持明确的规划提示。
- 发现页已提供可解释的公开时间、互动热度与已关注博客文章流；编辑精选、跨对象关注聚合、统一搜索和创作者统计仍是 M5 范围。
- 原脚手架的支付、微信、第三方登录等扩展能力保持默认关闭；它们不是当前社区路线的功能承诺，也不得干扰社区运行。
- 生产部署、备份恢复、监控和灰度开放不提前并入 M2.5，统一由 M6 完成。
