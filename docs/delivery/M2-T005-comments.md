# M2-T005 评论与回复

## 状态

完成。

## 交付范围

- 文章和动态统一支持一级评论与平铺回复；回复始终保存根评论、直接父评论和
  被回复用户，不产生无限嵌套展示。
- 评论列表按一级评论分页，每条一级评论预览最早 3 条公开回复，并提供完整回复数
  与独立回复分页。
- 评论和回复均可复用已有 `COMMENT` 点赞关系；收藏接口继续拒绝评论目标。
- 发表评论必须使用独立社区 Token，且用户状态正常、未处于评论限制期。
- 个人博客评论范围支持：
  - `ALL_LOGGED_IN`
  - `FOLLOWERS_ONLY`
  - `MUTUAL_ONLY`
  - `BLOGGER_FOLLOWING`
  - `DISABLED`
- 团队博客范围支持 `TEAM_FOLLOWERS` 和 `TEAM_MEMBERS`；团队成员关系通过
  `TeamBlogMemberResolver` 扩展点接入，团队模块未启用时默认拒绝成员范围。
- 评论提交先执行 NFKC 规范化、长度和控制字符检查，再转义 HTML、把 HTTP(S)
  链接转为带 `nofollow noopener noreferrer` 的安全链接，并使用 Jsoup 白名单
  进行最终净化。
- 文本和 Emoji 原样安全保存；`@提及` 当前作为文本保存，提及通知在
  M2-T007 通知任务中接入。
- 复用内容关键词规则，对评论执行：
  - `BLOCK`：拒绝写入并返回业务 400。
  - `REVIEW`：写入 `PENDING_REVIEW`，不公开且不增加内容评论数。
  - `WARN`：公开评论，并在创建响应中返回风险提示。
- 用户只能删除自己的评论；已公开评论删除后显示匿名墓碑，回复仍可阅读。
- 内容作者或博客所有者可隐藏他人评论；隐藏一级评论时整条公开讨论一起隐藏。
- 删除和隐藏均保留不可变治理事件，作者不能恢复平台状态，也不能修改他人正文。
- 评论创建、治理事件和文章/动态 `comment_count` 在同一事务内更新；
  `GREATEST` 防止计数变为负数，重复删除幂等。
- 所有 BIGINT 业务 ID 在 Web API 响应中以字符串返回。

## API

```text
POST   /api/v1/interactions/{targetType}/{targetId}/comments
GET    /api/v1/interactions/{targetType}/{targetId}/comments
POST   /api/v1/comments/{commentId}/replies
GET    /api/v1/comments/{rootCommentId}/replies
DELETE /api/v1/comments/{commentId}
POST   /api/v1/comments/{commentId}/hide
```

`targetType` 支持 `ARTICLE` 和 `MOMENT`，输入不区分大小写。

## 数据库迁移

V009 新增：

- `community_comment_moderation_event` 不可变评论治理事件表。
- `blog_setting.comment_scope` 的完整检查约束。

验证结果：

```text
V009 repeat run: PASS
Comment moderation tables: 1
Required moderation columns: 8
Comment scope check constraints: 1
Invalid comment scope rows: 0
```

## 自动化验证

```text
Comment content renderer tests: 4 passed
Comment scope tests: 7 passed
Comment service tests: 6 passed
Comment controller tests: 4 passed
Community business tests: 163 passed
Community web API tests: 32 passed
Full Maven reactor: 26 modules SUCCESS
All backend automated tests: 204 passed
Failures / errors / skipped: 0 / 0 / 0
```

## 真实运行时闭环

```text
Backend: http://127.0.0.1:8849 / UP
Temporary article: APPROVED / PUBLISHED
Safe HTML escaping and safe HTTP link rendering: PASS
Root comment / flat reply target count: 2
Comment like / unlike count: 1 / 0
Hide root affected comments / final target count: 2 / 0

Keyword BLOCK: 400 / no comment written
Keyword REVIEW: PENDING_REVIEW / target count 0
Keyword WARN: AUTO_APPROVED_WITH_WARNING / PUBLISHED

FOLLOWERS_ONLY before / after follow: 403 / 200
MUTUAL_ONLY before / after mutual follow: 403 / 200
BLOGGER_FOLLOWING: 200
DISABLED: 403
Active comment restriction: 403
First / repeated self-delete affected rows: 1 / 0

Governance events:
  total = 15
  published = 7
  pending = 1
  hidden = 2
  deleted = 5

Temporary users / articles / keyword rules / comments: 0 / 0 / 0 / 0
```

真实闭环可重复执行：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass `
  -File .\scripts\e2e\m2-t005-comments.ps1
```

脚本在 `finally` 中按外键顺序清理临时数据，并输出最终清理计数。

## 主要文件

- `database/migrations/V009__comment_scope_and_moderation.sql`
- `database/verify/V009__verify_comment_scope_and_moderation.sql`
- `pxczxn-backend/mars-core/pxczxn-biz/src/main/java/top/pxczxn/community/social/application/CommentService.java`
- `pxczxn-backend/mars-core/pxczxn-biz/src/main/java/top/pxczxn/community/social/application/CommentScopeService.java`
- `pxczxn-backend/mars-core/pxczxn-biz/src/main/java/top/pxczxn/community/social/application/CommentContentRenderer.java`
- `pxczxn-backend/mars-api/pxczxn-web-api/src/main/java/top/pxczxn/community/web/social/CommentController.java`
- `scripts/e2e/m2-t005-comments.ps1`

## 安全与一致性结论

- 匿名用户只能读取底层内容允许公开的已发布评论，不能发表评论。
- 私密、关注者限定、已删除、已隐藏、待审和下架内容不会通过评论接口扩大可见范围。
- 用户输入不会作为原始 HTML 返回，链接协议和属性受到双重白名单限制。
- `BLOCK` 在评论和治理事件写入前终止；`REVIEW` 不计入公开评论数；
  隐藏一级评论会按实际成功更新的公开评论数原子扣减。
- 删除后的墓碑不暴露作者资料、原文或点赞关系。
- 平台下架、恢复和批量治理的管理端 API/UI 在 M2-T009 接入现有不可变事件表。
