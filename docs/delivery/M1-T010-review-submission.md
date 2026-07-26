# M1-T010 文章审核提交交付说明

## API

```http
POST /api/v1/articles/{articleId}/submit-review
POST /api/v1/articles/{articleId}/withdraw-review
GET  /api/v1/articles/{articleId}/review-status
```

三个接口使用独立的 `pxczxn-community-token`。提交和撤回均要求文章
`expectedLockVersion`；提交还要求 8-80 位客户端幂等键。

## 固定审核版本与事务

- 提交前由统一文章权限服务重新校验账号、博客角色和文章工作流状态。
- 提交时把当前不可变 `article_version` 固定到
  `content_review_task.fixed_version_id` 和 `article.review_version_id`。
- 审核任务插入、文章审核状态、发布状态、固定版本和文章锁版本在同一事务
  切换。
- 更新使用 `WHERE lock_version = ? AND current_version_id = ?`；并发修改返回
  409，候选审核任务随事务回滚。
- 审核中禁止编辑和删除；必须先撤回，避免审核内容与继续编辑的内容错位。
- 私密文章不进入公开审核；其正文仍经过编辑阶段的安全解析、HTML 清洗和文件
  校验。
- 空正文不能提交审核。

## 自动关键词审核

V003 新增 `content_keyword_rule`，规则数据由运营政策维护，迁移脚本不预置
可能随地区和运营策略变化的生产关键词。

扫描使用 NFKC、统一大小写和空白归一化，降低全角字符、大小写和重复空白造成
的简单绕过。扫描范围为标题、摘要和固定版本纯文本，不扫描已清洗 HTML，也
不把正文写入日志。

决策矩阵：

| 命中级别 | 审核任务 | 风险 | 文章审核结果 |
|---|---|---|---|
| 无命中 | `APPROVED` / `AUTO` | `LOW` | 自动通过 |
| `WARN` | `APPROVED` / `AUTO` | `MEDIUM` | 带告警自动通过 |
| `REVIEW` | `QUEUED` / `MANUAL` | `HIGH` | 进入平台人工队列 |
| `BLOCK` | `REJECTED` / `AUTO` | `CRITICAL` | 自动拒绝 |

多条规则同时命中时采用最高严重级别。审核原因只记录命中规则 ID 和数量，不
复制敏感正文或 Token。

## 幂等、旧公开版本与撤回

- `content_review_task.idempotency_key` 数据库唯一，同一用户对同一文章重复
  使用同一键时返回原任务，不新增任务，也不要求旧锁版本再次成功。
- 同一幂等键被其他文章或用户占用时返回 409。
- 活动审核任务继续由 V001 的 `active_article_id` 生成列保持每篇文章至多一个。
- 首次发布的人工审核文章进入 `PENDING_REVIEW`。
- 已有 `published_version_id` 的文章提交新版本时保留 `PUBLISHED` 或
  `HIDDEN` 状态，旧公开版本继续服务；审核状态单独进入队列。
- 撤回把活动任务改为 `CANCELLED`，保留完整历史任务与固定版本，不执行物理
  删除。
- 撤回后清空文章当前审核版本，恢复草稿编辑；已有公开版本保持原公开状态。

## 验证

```text
Community business automated tests: 63 passed
Community API automated tests: 13 passed
T010-specific automated tests: 15 passed
V003 first run / repeat run / structure verification: PASS
No-match submission: APPROVED / LOW
WARN submission: APPROVED / MEDIUM
REVIEW submission: PENDING_REVIEW + QUEUED / HIGH
BLOCK submission: DRAFT + REJECTED / CRITICAL
Same idempotency key: same task / task count unchanged
Review status read: fixed task and version returned
Edit during review: 409
Withdraw review: CANCELLED / article lock advanced
Edit after withdraw: 200
Private article submission: 409
Review task audit rows all retain fixed versions: PASS
Temporary users, blogs, articles, tasks and keyword rules: removed
```
