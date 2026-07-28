# M4 全量验收

状态：完成

## 验收范围

- 举报创建、运营领取与处理、目标用户申诉、申诉撤销。
- 用户/博客/内容/聊天屏蔽及其在公开内容、动态、评论、私聊和通知中的传播。
- 警告、限流、禁评、禁动态、暂停发布、暂停登录和永久封禁的生命周期。
- 内容规则的拦截、人工审核和警告策略。
- 注册、登录、动态、举报的反滥用阈值、重复内容指纹和不可变审计事件。

## 实际验证结果

- `mvn test`：后端完整 Reactor 通过。
- `m4-report-lifecycle.mjs`：通过。
- `m4-block-list-lifecycle.mjs`：通过。
- `m4-sanction-lifecycle.mjs`：通过。
- `m4-content-rules.mjs`：通过。
- `m4-anti-abuse.mjs`：通过。
- 管理端 typecheck、lint、test、build：在 M4-T007 合并前通过；M4-T008 不修改管理端代码。

## 验收环境说明

验收使用独立 MySQL 库 `pxczxn_m4_t006_acceptance` 和固定后端端口 8861。各 E2E
在启动前只清除本地验收 IP 的可变注册/登录窗口，避免跨脚本累积；`community_abuse_event`
不可变审计记录不被删除。
