# M2-T009 管理端互动治理

## 状态

完成。

## 交付范围

- 在 pxczxn 运营管理端内新增评论治理、动态治理和互动查询三个页面。
- 评论与动态支持关键词、状态、目标、发布者等条件分页查询。
- 待审核内容支持通过和驳回；已发布内容支持平台下架；平台下架内容支持恢复。
- 评论根线程治理会级联处理回复，并在事务内校准目标评论数。
- 动态治理同步维护转发源计数和源内容可见性约束。
- 评论和动态均支持最多 100 条的批量治理、重复 ID 拒绝和逐条乐观锁。
- 详情抽屉展示内容、Snowflake ID、锁版本和不可变治理事件时间线。
- 互动查询覆盖 LIKE、FAVORITE、FOLLOW，仅返回关系和公开目标摘要。
- 收藏查询不会返回收藏夹 ID、名称或目录结构。
- 管理端写操作接入 pxczxn `@Log` 操作日志。
- 新增评论、动态和互动的菜单与细粒度 RBAC 权限，并授予管理员角色。
- 只读角色可查询但无法执行治理动作。
- 管理端品牌统一为“星语社区运营中心”，保留浅色、深色和星空三色主题。

## 管理端 API

```text
GET  /admin-api/community/comments
GET  /admin-api/community/comments/{commentId}
POST /admin-api/community/comments/{commentId}/approve
POST /admin-api/community/comments/{commentId}/reject
POST /admin-api/community/comments/{commentId}/take-down
POST /admin-api/community/comments/{commentId}/restore
POST /admin-api/community/comments/batch/{action}/execute

GET  /admin-api/community/moments
GET  /admin-api/community/moments/{momentId}
POST /admin-api/community/moments/{momentId}/approve
POST /admin-api/community/moments/{momentId}/reject
POST /admin-api/community/moments/{momentId}/take-down
POST /admin-api/community/moments/{momentId}/restore
POST /admin-api/community/moments/batch/{action}/execute

GET  /admin-api/community/interactions
```

## 权限点

```text
community:comment:list
community:comment:query
community:comment:approve
community:comment:reject
community:comment:takeDown
community:comment:restore
community:comment:batch

community:moment:list
community:moment:query
community:moment:approve
community:moment:reject
community:moment:takeDown
community:moment:restore
community:moment:batch

community:interaction:list
```

## 数据库迁移

- `V012__community_interaction_governance.sql`
  - 新增 `community_moment_moderation_event`。
  - 扩展评论平台审核事件动作。
  - 新增治理查询索引、菜单、权限和管理员授权。
- `V013__xingyu_admin_brand.sql`
  - 将管理端品牌统一为“星语社区运营中心”。
  - 对已执行环境中的治理菜单中文名称进行幂等修复。

验证结果：

```text
moment moderation table: 1
moment governance columns: 9
governance CHECK constraints: 2
governance menu rows: 18
admin governance permissions: 18
invalid governance events: 0
invalid menu names: 0
```

## 自动化验证

```text
Backend targeted governance tests: 15 passed
Backend full regression: 246 passed
pxczxn Admin production build: PASS
Database migrations and verification: PASS

Approve idempotent replay: true
Comment lifecycle: PENDING_REVIEW → PUBLISHED → TAKEN_DOWN → PUBLISHED
Moment lifecycle: PENDING_REVIEW → PUBLISHED → TAKEN_DOWN → PUBLISHED
Rejected comment: TAKEN_DOWN
Batch comments / moments: 2 / 2
LIKE / FAVORITE / FOLLOW query: 1 / 1 / 1
Favorite folder privacy: PASS
RBAC list / mutation: 200 / 403
Immutable comment / moment events: 12 / 7
Successful governance operation logs: 44
Temporary users / keyword rules: 0 / 0
```

端到端脚本：

```text
node scripts/e2e/m2-t009-admin-governance.mjs
```

启用图片验证码时，可通过 `PXCZXN_ADMIN_TOKEN` 传入已登录管理 Token；脚本默认
执行完毕后清理社区测试数据。`--keep-data` 可保留数据供浏览器验收，并使用输出的
`--cleanup-stamp` 命令精确清理。

## 浏览器真实闭环

使用局域网地址访问 8848，完成：

```text
Admin RSA login: PASS
LAN HTTP encrypted response fallback: PASS
Comment governance list / detail timeline: PASS / 4 events
Moment governance list and interaction counters: PASS
LIKE / FAVORITE / FOLLOW tabs: PASS
Favorite folder metadata absent: PASS
Light / dark / starry theme: PASS
Theme body backgrounds:
  light  rgb(243, 244, 246)
  dark   rgb(16, 16, 20)
  starry rgb(7, 11, 29)
Browser console errors: 0
```

管理端原实现仅依赖 Web Crypto 解密 AES-GCM 响应，在非安全的局域网 HTTP 地址
会卡在登录导航。现已增加 `@noble/ciphers` 纯 JavaScript AES-GCM 降级实现，
同时保留 HTTPS/localhost 下的原生 Web Crypto 快速路径。

## 主要文件

- `database/migrations/V012__community_interaction_governance.sql`
- `database/migrations/V013__xingyu_admin_brand.sql`
- `pxczxn-backend/pxczxn-core/pxczxn-biz/.../AdminInteractionGovernanceService.java`
- `pxczxn-backend/pxczxn-api/pxczxn-admin-api/.../AdminInteractionGovernanceController.java`
- `pxczxn-admin/src/views/community/comments/index.vue`
- `pxczxn-admin/src/views/community/moments/index.vue`
- `pxczxn-admin/src/views/community/interactions/index.vue`
- `pxczxn-admin/src/utils/request.ts`
- `scripts/e2e/m2-t009-admin-governance.mjs`

## 设计结论

- 管理员和社区用户继续使用两套 Token，管理权限不复用社区作者身份。
- 所有写操作同时受 RBAC、乐观锁和数据库事务约束。
- 治理事件为追加写，不通过更新覆盖历史。
- BIGINT ID 在管理 API 和 Vue 页面中保持字符串，避免浏览器精度丢失。
- 收藏夹是用户私密结构，运营查询只显示收藏关系。
