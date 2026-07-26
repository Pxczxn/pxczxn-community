# M1-T011 管理端审核 API 交付说明

## API 与权限

```http
GET  /admin-api/community/reviews
GET  /admin-api/community/reviews/{taskId}
POST /admin-api/community/reviews/{taskId}/claim
POST /admin-api/community/reviews/{taskId}/approve
POST /admin-api/community/reviews/{taskId}/revision
POST /admin-api/community/reviews/{taskId}/reject
```

接口使用运营管理端的 `StpUtil` 会话和 `@SaCheckPermission`，不接受社区
`pxczxn-community-token`。V004 注册以下 RBAC：

```text
community:review:list
community:review:query
community:review:claim
community:review:approve
community:review:revision
community:review:reject
community:article:review
```

最后一项是统一文章权限服务使用的审核域权限，菜单中隐藏；其余权限分别控制
工作台和审核动作。默认 `admin` 角色获得完整权限。

## 队列与固定版本

- 列表支持按审核状态、风险等级、提交起止时间分页查询，单页最多 100 条。
- 列表批量加载文章、作者和博客，避免逐行查询。
- 详情通过 `content_review_task.fixed_version_id` 读取不可变
  `article_version`，不读取 `article.current_version_id` 指向的后续草稿。
- 管理员可查看固定版本的内容模式、源内容、安全 HTML、纯文本、目录、哈希、
  字数和阅读时间，以及必要的文章、作者、博客和自动审核元数据。
- 所有 BIGINT 业务 ID 在管理 API 中返回字符串，避免 JavaScript 精度损失。

## 领取与裁决状态机

领取要求任务为 `QUEUED`、处理人为空且任务锁版本匹配：

```text
QUEUED -> MANUAL_REVIEWING
```

审核任务和文章在同一事务中分别使用自己的乐观锁。两个管理员同时领取时只有
一个更新成功；同一处理人的领取重试是幂等的。

裁决只允许领取该任务的管理员执行：

```text
MANUAL_REVIEWING -> APPROVED
MANUAL_REVIEWING -> REVISION_REQUIRED
MANUAL_REVIEWING -> REJECTED
```

- 退修和驳回必须填写原因；通过可使用默认说明。
- 任务写入处理人、结果码、原因、完成时间并递增锁版本。
- 文章审核状态、发布状态和锁版本在同一事务切换。
- 首次发布文章通过后进入 `APPROVED`；退修或驳回回到 `DRAFT`。
- 已有公开版本时，新版本退修或驳回仍保留旧 `PUBLISHED`/`HIDDEN` 公开版本。
- 固定审核版本和历史任务始终保留，不做物理删除。

## 提交后通知

审核事务提交后发布领域事件，通知服务用独立 `REQUIRES_NEW` 事务写入：

- `community_notification`
- `community_notification_recipient`

通知用 `article-review:{taskId}:{decision}` 去重，收件人是投稿用户。通知失败只
记录任务、文章和决定元数据，不传播异常、不回滚或误报已经完成的审核事务。

## 验证

```text
Community business automated tests: 74 passed
Community web API automated tests: 13 passed
pxczxn Admin API automated tests: 4 passed
T011-specific automated tests: 15 passed
V004 first run / repeat run: PASS
V004 menus / permissions / admin grants: 7 / 7 / 8, PASS
Admin list queued task: 200
Admin detail uses fixed version: PASS
Claim: MANUAL_REVIEWING / task lock 1
Stale decision lock: 409
Approve: task APPROVED / article APPROVED / task lock 2
Post-commit notification / unread recipient: 1 / 1
Temporary verification data: removed, remaining 0
```
