# M1-T013 定时发布交付说明

## API

定时发布沿用统一发布入口：

```http
POST /api/v1/articles/{articleId}/publish
```

设置或修改计划时间：

```json
{
  "expectedLockVersion": 5,
  "scheduledPublishAt": "2026-07-25 10:00:00",
  "cancelScheduled": false
}
```

取消计划：

```json
{
  "expectedLockVersion": 6,
  "scheduledPublishAt": null,
  "cancelScheduled": true
}
```

时间按工程统一约定使用 UTC。只有审核通过、当前版本与审核固定版本一致且发布
方式为 `SCHEDULED` 的文章可以设置计划时间。重复提交相同计划或重复读取已完成
发布返回 `idempotentReplay=true`。

## 持久任务和 Quartz

V005 新增 `article_publish_task`，保存：

```text
article_id / article_version_id / scheduled_publish_at
status / attempt_count / max_attempts / next_attempt_at
last_error_code / last_error_message / lock_version
```

活动状态 `WAITING / RUNNING / RETRY_WAIT` 通过生成列唯一约束，保证同一文章
最多一个活动发布任务。取消或修改计划会先终结旧任务，再在同一事务中创建新
任务；文章乐观锁冲突时整个事务回滚。

V005 同时向 Mars Admin 的 `sys_job` 注册
`articleScheduledPublishTask.runDueBatch`，Quartz 每分钟扫描到期任务，禁止
同一 Job 并发，并采用 fire-and-proceed 的 misfire 策略。任务状态完全落
MySQL，不依赖 Redis。

## 到点二次校验

Quartz 领取任务和公开版本切换使用独立事务。发布前重新校验：

- 任务仍对应文章当前计划时间，旧计划不能覆盖新状态。
- 发布方式和发布状态仍为 `SCHEDULED`。
- 审核状态仍为 `APPROVED`。
- 当前版本、审核固定版本和任务版本完全一致。
- 固定版本仍存在且归属当前文章。
- 博客仍为 `ACTIVE`，作者仍具备发布资格。
- 文章未删除，计划时间已经到达。

发布成功时在一个事务中把 `published_version_id` 切到固定版本，写入
canonical、UTC 发布时间和 `PUBLISHED`，清空计划时间，并把任务改为
`SUCCEEDED`。文章更新同时检查文章锁版本和全部关键状态；并发执行只有一个
更新成功，后续执行识别相同公开版本后按幂等成功处理。

## 重试与失败

- 数据库或基础设施异常属于可重试故障，任务进入 `RETRY_WAIT`。
- 重试使用 1、2、4 分钟的有限退避，默认最多 3 次。
- 进程在 `RUNNING` 中退出时，5 分钟后的 Quartz 扫描可以恢复该次尝试。
- 业务二次校验失败不可重试，文章进入 `PUBLISH_FAILED`，任务进入 `FAILED`。
- 重试耗尽也原子进入 `PUBLISH_FAILED / FAILED`。
- 失败只记录稳定错误码和通用描述，例如
  `AUTHOR_NOT_PUBLISHABLE`、`VERSION_CHANGED`、`RETRY_EXHAUSTED`；
  不记录正文、密码或 Token。

已有旧公开版本的文章在新定时版本失败或等待期间继续通过
`published_version_id` 提供旧内容。

## 验证

```text
Community business automated tests: 106 passed
Community web API automated tests: 19 passed
Mars Job automated tests: 1 passed
Mars Admin API automated tests: 4 passed
T013-specific automated tests: 14 passed

V005 first run / repeat run: PASS
V005 table / required columns / Quartz job: 1 / 11 / PASS
Cancel schedule: APPROVED / task CANCELLED
Quartz success: PUBLISHED / SUCCEEDED / attempt 1
Second-check failure: PUBLISH_FAILED / AUTHOR_NOT_PUBLISHABLE
Repeated publication: idempotentReplay=true
Public detail after Quartz publication: PASS
Quartz job logs: success
Temporary verification data: removed, remaining 0

Runtime health: UP
Unauthenticated admin API: 401
captchaEnabled / encryptEnabled / encryptScope:
true / true / global
```
