# M2-T006 动态发布、详情与分享

## 状态

完成。

## 交付范围

- 完成文本、链接、视频链接、文章分享、项目更新、代码、纯转发和引用动态发布。
- 图片、投票和团队公告动态继续保留在数据库枚举中，分别等待文件、投票和团队
  模块接入，当前 API 明确拒绝尚未实现的类型。
- 发布身份默认使用用户个人博客，并预留团队博客发布者解析扩展点；个人用户不能
  伪造其他博客身份。
- 发布前校验账号状态与动态发布限制，对正文和链接复用关键词
  `BLOCK`、`REVIEW`、`WARN` 治理。
- 动态正文执行 NFKC 规范化、长度与控制字符校验、HTML 转义、HTTP(S)
  安全链接化和 Jsoup 白名单净化；代码动态使用安全的 `pre/code` 渲染。
- 外链只允许 HTTP(S)，必须有合法主机且不能包含用户信息；外链最长 2048 字符。
- 文章分享、纯转发和引用动态均复用统一内容可见性服务；转发链递归校验源内容，
  公开包装不能扩大关注者、私密或已删除源内容的访问范围。
- 完成动态公开流、博客动态列表、本人动态列表、详情和稳定分享路径。
- 动态详情直接返回作者、博客、文章卡片、转发源卡片、互动计数和当前用户关系。
- 复用已有动态点赞、收藏和评论 API；关系写入和目标计数保持事务一致。
- 纯转发禁止正文，引用动态必须填写观点；发布与删除在同一事务内增减源动态
  `repost_count`，使用 `GREATEST` 防止负数。
- 用户删除使用软删除和乐观锁；重复删除幂等，平台下架内容不能由用户删除。
- 所有 BIGINT 业务 ID 在 Web API 响应中以字符串返回。

## API

```text
POST   /api/v1/moments
GET    /api/v1/moments/{momentId}
GET    /api/v1/moments
GET    /api/v1/blogs/{blogId}/moments
GET    /api/v1/social/me/moments
DELETE /api/v1/moments/{momentId}?expectedLockVersion={version}
POST   /api/v1/moments/{momentId}/share-link
```

动态互动继续使用：

```text
POST/DELETE /api/v1/interactions/MOMENT/{momentId}/like
POST/DELETE /api/v1/interactions/MOMENT/{momentId}/favorite
POST/GET    /api/v1/interactions/MOMENT/{momentId}/comments
```

## 数据库迁移

V010 为 `community_moment` 增加：

- 公开动态流索引
  `idx_community_moment_public_feed(status, visibility, created_at, id)`。
- LINK/VIDEO_LINK 必须有外链的检查约束。
- QUOTE 必须有引用观点的检查约束。
- REPOST 不允许带正文的检查约束。

`article_id` 和 `repost_moment_id` 参与 `ON DELETE SET NULL` 外键，MySQL 8.0
不允许这些列同时出现在 CHECK 中；两类引用的必填、互斥和动态类型匹配由
`MomentService` 在同一发布事务内校验，既有外键继续保证目标引用合法。

验证结果：

```text
V010 repeat run: PASS
Public feed indexes: 1
Moment shape check constraints: 3
Invalid historical rows: 0
```

## 自动化验证

```text
Moment content renderer tests: 4 passed
Moment service tests: 11 passed
Moment controller tests: 4 passed
T006-focused tests: 19 passed
Community business tests: 178 passed
Community web API tests: 36 passed
Full Maven reactor: 26 modules SUCCESS
All backend automated tests: 223 passed
Failures / errors / skipped: 0 / 0 / 0
```

## 真实运行时闭环

```text
Backend: http://127.0.0.1:8849 / UP
Temporary article: APPROVED / PUBLISHED
Safe HTML escaping / HTTP link rel protection: PASS
Unsafe javascript link: 400
Article share card: available
REPOST / QUOTE initial source count: 2
REPOST with text / QUOTE without text: 400 / 400
Stable share path: /moments/{momentId}
Moment like / favorite / comment counts: 1 / 1 / 1
Public feed / blog feed / own feed: PASS
FOLLOWERS_ONLY before / after follow: 404 / 200
Public repost of restricted source for anonymous user: 404
PRIVATE owner / other user: 200 / 404
Active publish restriction: 403
Keyword BLOCK / REVIEW / WARN: 400 / PENDING_REVIEW / PUBLISHED
Repeated quote / repost delete: idempotent
Source repost count after both deletions: 0
Temporary users / articles / moments / rules / comments: 0 / 0 / 0 / 0 / 0
```

真实闭环可重复执行：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass `
  -File .\scripts\e2e\m2-t006-moments.ps1
```

脚本在 `finally` 中按外键顺序清理临时关系与内容，并输出最终清理计数。

## 主要文件

- `database/migrations/V010__moment_publication_constraints.sql`
- `database/verify/V010__verify_moment_publication_constraints.sql`
- `pxczxn-backend/mars-core/pxczxn-biz/src/main/java/top/pxczxn/community/social/application/MomentService.java`
- `pxczxn-backend/mars-core/pxczxn-biz/src/main/java/top/pxczxn/community/social/application/MomentContentRenderer.java`
- `pxczxn-backend/mars-api/pxczxn-web-api/src/main/java/top/pxczxn/community/web/social/MomentController.java`
- `scripts/e2e/m2-t006-moments.ps1`

## 安全与一致性结论

- 私密、关注者限定、待审、删除、下架以及不可见转发源均不会从动态详情、列表、
  分享或互动接口泄漏。
- 动态正文不作为原始 HTML 输出，外链协议和链接属性受到白名单保护。
- `BLOCK` 在写入前终止，`REVIEW` 不进入公开流，`WARN` 明确返回风险标记。
- 点赞、收藏、评论和转发计数均与对应关系或内容状态在事务中同步更新。
- 动态平台审核、恢复和批量治理由 M2-T009 管理端治理任务接入。
