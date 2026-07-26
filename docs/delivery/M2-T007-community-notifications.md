# M2-T007 站内通知与未读计数

## 状态

完成。

## 交付范围

- 完成关注、点赞、收藏、评论、回复、`@提及`、动态转发和文章审核结果通知。
- 文章和动态发布后按博客关注设置分发更新通知：
  - `ALL` 接收全部允许公开给关注者的更新。
  - `IMPORTANT` 只接收文章、项目进展和团队公告等重要更新。
  - `MUTED` 不接收博客更新。
  - 特别关注允许接收普通更新，并以 `HIGH` 重要等级展示。
- 评论中的用户名提及使用注册用户名规则解析，单条评论最多通知 20 个不同用户；
  回复者、内容作者和被提及者去重，避免同一动作产生多条重复通知。
- 自关注已在关注服务阻止；自点赞、自收藏、自评论和自转发均不产生通知。
- 重复点赞、收藏和关注依赖既有关系幂等，不重复生成通知；取消点赞、取消收藏和
  取消关注不发送通知。
- 相同通知按类型、收件人、目标聚合键和十分钟时间桶合并，保存聚合次数和最后
  活动时间；已读通知在同一时间桶内收到新动作时重新变为未读。
- 通知使用唯一去重键作为并发最终防线，关系收件箱使用唯一约束防止重复收件。
- 通知事件只在主事务提交后处理，写入使用独立 `REQUIRES_NEW` 事务；通知失败
  记录安全元数据，不回滚已经成功的关注或互动动作。
- 通知列表支持分类、未读/已读筛选、分页、单条已读、分类全部已读、全部已读和
  按八类汇总的未读计数。
- 通知目标读取时重新执行当前可见性检查；内容删除、隐藏、下架或权限收紧后，
  响应隐藏发送人、旧摘要、目标类型、目标 ID 和链接，防止通过历史通知泄漏。
- 审核结果通知统一归入 `REVIEW` 分类和 `HIGH` 重要等级；审核失败隔离逻辑保持
  不变。
- 所有 BIGINT 业务 ID 在 Web API 响应中以字符串返回。

## 分类与类型

通知分类：

```text
INTERACTION / FOLLOW / COMMENT / COAUTHOR
SUBMISSION / TEAM / REVIEW / SYSTEM
```

当前已生成的通知类型：

```text
FOLLOW / LIKE / FAVORITE / COMMENT / REPLY / MENTION / REPOST
ARTICLE_PUBLISHED / MOMENT_PUBLISHED / REVIEW
```

共创、投稿、团队和系统分类已在数据库与 API 中保留，分别由后续业务模块发布
对应事件。

## API

```text
GET   /api/v1/notifications
GET   /api/v1/notifications/unread-count
PATCH /api/v1/notifications/{notificationId}/read
PATCH /api/v1/notifications/read-all
```

列表参数：

```text
category: 可选，八类通知之一
status:   可选，UNREAD 或 READ
pageNum:  默认 1
pageSize: 默认 20，最大 50
```

全部已读可通过 `category` 只处理一个分类；不传时处理全部未读通知。

## 数据库迁移

V011 为 `community_notification` 增加：

- `category`
- `importance`
- `aggregate_count`
- `last_activity_at`
- 唯一去重索引 `uk_notification_deduplication`
- 分类活动索引 `idx_notification_category_activity`
- 分类、重要等级和聚合次数检查约束

既有审核通知迁移为 `REVIEW`，其余历史通知保留并默认归入 `SYSTEM`。若历史环境
存在重复去重键，保留最早通知的键并清空其余键，不删除任何历史收件箱记录。

验证结果：

```text
V011 first run / repeat run: PASS / PASS
Notification inbox columns: 4
Notification inbox indexes: 2
Notification inbox checks: 3
Invalid notification rows: 0
Duplicate deduplication keys: 0
```

## 自动化验证

```text
Notification dispatch tests: 4 passed
Notification inbox tests: 5 passed
Post-commit failure isolation tests: 1 passed
Notification controller tests: 3 passed
Article review notification tests: 3 passed
Notification-focused tests: 16 passed
Community business tests: 188 passed
Community web API tests: 39 passed
Full Maven reactor: 26 modules SUCCESS
All backend automated tests: 236 passed
Failures / errors / skipped: 0 / 0 / 0
```

## 真实运行时闭环

```text
Backend: http://127.0.0.1:8849 / UP
Anonymous inbox: 401
Three follow actors: 1 notification / aggregate_count 3
ALL follower: text + project update -> 1 record / aggregate_count 2 / HIGH
IMPORTANT follower: project update only / HIGH
MUTED follower: 0 publication notifications
Direct types: LIKE / FAVORITE / COMMENT / REPLY / MENTION / REPOST
Self notification: suppressed
Repeated idempotent like: aggregate_count remains 1
Read / repeated read: idempotentReplay false / true
Unlike then relike: aggregate_count 2 / status UNREAD
Author unread categories: FOLLOW 1 / INTERACTION 3 / COMMENT 1
Read INTERACTION then all: affected 3 then 2 / final unread 0
Unlike / unfavorite / unfollow notifications: 0
Deleted target redaction: sender / summary / target ID / path hidden
Notification / recipient rows during evidence: 9 / 9
Maximum aggregate count: 3
Temporary users / moments / notifications / comments: 0 / 0 / 0 / 0
```

真实闭环可重复执行：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass `
  -File .\scripts\e2e\m2-t007-notifications.ps1
```

脚本在 `finally` 中按外键顺序清理通知、互动、动态、博客和用户，并输出最终
清理计数。

## 主要文件

- `database/migrations/V011__community_notification_inbox.sql`
- `database/verify/V011__verify_community_notification_inbox.sql`
- `pxczxn-backend/pxczxn-core/pxczxn-biz/src/main/java/top/pxczxn/community/notification/application/CommunityNotificationDispatchService.java`
- `pxczxn-backend/pxczxn-core/pxczxn-biz/src/main/java/top/pxczxn/community/notification/application/CommunityNotificationInboxService.java`
- `pxczxn-backend/pxczxn-core/pxczxn-biz/src/main/java/top/pxczxn/community/notification/application/CommunityNotificationEventListener.java`
- `pxczxn-backend/pxczxn-api/pxczxn-web-api/src/main/java/top/pxczxn/community/web/notification/CommunityNotificationController.java`
- `scripts/e2e/m2-t007-notifications.ps1`

## 安全与一致性结论

- 通知是主业务提交后的派生数据，通知存储故障不会反向破坏关系或内容事务。
- 唯一去重键、关系幂等与收件人唯一约束共同防止重复通知。
- 未读状态以 MySQL 为最终依据，不依赖 Redis；分类未读计数可直接由收件箱恢复。
- 历史通知不能绕过当前内容权限；不可访问目标的发送人和业务字段一并脱敏。
- V1 只提供站内通知，不发送邮件、短信或移动推送。
