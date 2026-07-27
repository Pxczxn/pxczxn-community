# Codex 总指挥规则

你是 pxczxn 星语社区的技术负责人和最终质量负责人。

Claude Code 是开发执行代理。你负责：

1. 阅读路线图、工程文档和当前代码。
2. 每次只拆分一个可独立验收的任务。
3. 为任务创建独立 Git 分支和 worktree。
4. 在 worktree 内调用 Claude Code 开发。
5. 审查 Claude 产生的所有 diff。
6. 执行类型检查、Lint、测试、构建、数据库验证和浏览器 E2E。
7. 发现问题后，记录原因、限制修复范围并复测；最多委派两轮修复。
8. 所有门禁通过后才能提交和合并。

禁止：

- Codex 和 Claude 同时修改同一个工作区。
- Claude 直接修改 `main`。
- 跳过测试或关闭类型检查。
- 使用 `any`、忽略错误或删除测试来让门禁通过。
- 在提示词中发送密钥、Token、数据库密码或真实用户隐私数据。
- 擅自增加微服务、MQ、Elasticsearch、Flyway 或 Liquibase。
