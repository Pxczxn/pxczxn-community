# M2-T003 内容点赞与喜欢列表

## 状态

完成。

## 交付范围

- 文章、动态、评论和回复统一使用 `community_content_like` 关系。
- 新增点赞、取消点赞、当前关系查询和本人喜欢列表 API。
- 点赞与目标 `like_count` 在同一事务中写入；首次点赞原子加一，取消点赞使用
  `GREATEST(like_count - 1, 0)`，重复操作幂等。
- 数据库唯一约束保证同一用户对同一类型和目标只存在一条关系，并处理并发重复键。
- 文章点赞复用已发布文章权限；`FOLLOWERS_ONLY` 文章已真实接入博客关注关系。
- 动态点赞校验发布状态、软删除、博客和作者状态、公开范围、关联文章及转发源。
- 评论点赞同时校验评论状态、根评论/父评论范围和底层文章或动态可见性。
- 喜欢列表返回文章、动态或评论的安全摘要，不返回编辑源、审核内部状态或不可见内容。
- V008 为用户偏好增加 `likes_visibility`，默认 `PRIVATE`，支持：
  - `PRIVATE`
  - `PUBLIC`
  - `FOLLOWERS_ONLY`
  - `MUTUAL_ONLY`
- 新增本人隐私设置和用户喜欢列表 API；无权访问时统一返回业务 404。
- 所有 BIGINT 业务 ID 继续以字符串返回。

## API

```text
POST   /api/v1/interactions/{targetType}/{targetId}/like
DELETE /api/v1/interactions/{targetType}/{targetId}/like
GET    /api/v1/interactions/{targetType}/{targetId}/like
GET    /api/v1/social/me/likes
GET    /api/v1/users/{userId}/likes
GET    /api/v1/social/me/likes/privacy
PATCH  /api/v1/social/me/likes/privacy
```

`targetType` 支持 `ARTICLE`、`MOMENT` 和 `COMMENT`，输入不区分大小写。

## 迁移验证

```text
V008 first run / repeat run: PASS / PASS
likes_visibility columns: 1
NOT NULL / default PRIVATE: PASS / PASS
CHECK constraints: 1
Invalid existing values: 0
```

## 自动化验证

```text
Community business tests: 134 passed
Community web API tests: 25 passed
Full Maven reactor: 26 modules SUCCESS
All backend automated tests: 168 passed
Failures / errors / skipped: 0 / 0 / 0
Like service tests: 7 passed
Content access tests: 5 passed
Like privacy tests: 6 passed
Like controller tests: 4 passed
Follow permission resolver tests: 2 passed
```

## 真实运行时闭环

```text
Backend: http://127.0.0.1:8849 / UP
Temporary article publication: PUBLISHED
First like: liked=true / count=1
Repeated like: liked=true / count=1
Relationship query: liked=true
Private liked page: 1 record
Unliked: liked=false / count=0
Repeated unlike: liked=false / count=0
MySQL final state: article.like_count=0 / relation=0

PRIVATE viewed by another user: 404
FOLLOWERS_ONLY before / after follow: 404 / 200
MUTUAL_ONLY before / after mutual follow: 404 / 200
PUBLIC viewed anonymously: 200

Temporary users / articles: 0 / 0
Orphan likes / follows: 0 / 0
```

## 主要文件

- `database/migrations/V008__like_list_privacy.sql`
- `database/verify/V008__verify_like_list_privacy.sql`
- `pxczxn-backend/pxczxn-core/pxczxn-biz/src/main/java/top/pxczxn/community/social/`
- `pxczxn-backend/pxczxn-api/pxczxn-web-api/src/main/java/top/pxczxn/community/web/social/`

## 安全与一致性结论

- 写接口先要求有效社区登录，再校验目标可见性。
- 关系唯一约束、目标原子计数和事务回滚共同保证重复与并发请求安全。
- 受限文章、动态、转发源以及隐藏评论均不能通过互动或喜欢列表泄漏。
- 喜欢列表默认私密；关注者和互关范围使用真实关注关系判定。
- 取消点赞不产生通知；点赞通知统一在 M2-T007 接入聚合规则。
