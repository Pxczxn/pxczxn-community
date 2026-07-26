# M1-T012 文章发布交付说明

## API

```http
POST /api/v1/articles/{articleId}/publish
Content-Type: application/json
pxczxn-community-token: <community token>

{
  "expectedLockVersion": 1
}
```

响应中的 BIGINT 业务 ID 使用字符串，包含原公开版本、当前公开版本、
canonical 地址、发布时间、新锁版本和 `idempotentReplay`。

## 发布方式

- `MANUAL`：审核通过后保持 `APPROVED`，作者调用发布 API。
- `IMMEDIATE`：自动审核或管理员审核通过时，在审核事务内直接发布。
- `SCHEDULED`：手动发布 API 明确拒绝，交由 M1-T013 的定时任务处理。

无论自动审核、管理员审核还是作者手动发布，都复用
`ArticlePublicationPolicy` 生成相同 canonical：

```text
/{blogSlug}/{articleId}/{articleSlug}
```

## 公开版本切换

发布前必须同时满足：

- 当前社区用户具有统一权限中心的 `PUBLISH` 权限。
- 审核状态为 `APPROVED`。
- `review_version_id` 与 `current_version_id` 一致。
- 审核版本真实存在且属于当前文章。
- 文章锁版本与客户端提交值一致。

发布事务使用带条件的单条文章更新，原子写入：

```text
published_version_id = review_version_id
publish_status       = PUBLISHED
canonical_path       = stable canonical
published_at         = UTC now
scheduled_publish_at = null
lock_version         = lock_version + 1
```

如果文章已有公开版本，新版本通过前不修改旧
`published_version_id`；新版本发布时才原子替换公开指针。旧版本继续保存在
`article_version` 历史中。M1-T009 的公开访问边界只允许读取
`published_version_id`；具体公开详情与列表 API 在 M1-T014 交付。

## 幂等、并发与事件

- 已发布同一审核版本且 canonical、发布时间完整时，重复调用直接返回原结果，
  `idempotentReplay=true`，即使客户端仍携带发布前的旧锁版本。
- 其他旧锁、审核版本漂移或并发更新返回 409，不覆盖新数据。
- 发布成功后产生 `ArticlePublishedEvent`，包含旧/新公开版本、canonical、
  UTC 发布时间和触发来源：
  `USER`、`AUTO_REVIEW` 或 `ADMIN_REVIEW`。
- 事件只携带业务元数据，不记录 Token 或文章正文。

## 验证

```text
Community business automated tests: 83 passed
Community web API automated tests: 15 passed
pxczxn Admin API automated tests: 4 passed
T012-specific automated tests: 11 passed

Real registration / login: PASS
MANUAL review / publish: APPROVED / PUBLISHED
Repeated manual publish: idempotentReplay=true
IMMEDIATE auto review / publish: APPROVED / PUBLISHED
MySQL current/review/published version consistency: PASS
Canonical path and published_at persistence: PASS
Temporary verification data: removed, remaining 0

Runtime health: UP
Unauthenticated admin API: 401
captchaEnabled / encryptEnabled / encryptScope:
true / true / global
```
