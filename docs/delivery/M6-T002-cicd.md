# M6-T002 CI/CD 交付说明

`Release Gates` 在推送、合并请求和每周例行检查时执行后端验证、两个前端的类型检查/lint/test/build、迁移清单校验和依赖安全扫描。配置受控的数据库基线后，它还会启动真实 Spring Boot 实例执行公开接口 E2E，并严格核对该基线的 SHA-256、迁移历史和在线 verify 脚本。

当生产 Dockerfile 已合入时，流水线还会构建后端和用户站镜像。仅 `main` 上手动触发 `deploy=true` 才能进入 `production` environment；应在 GitHub 环境设置中配置 required reviewers。部署作业需要四个仓库 secret：`PXCZXN_DEPLOY_HOST`、`PXCZXN_DEPLOY_USER`、`PXCZXN_DEPLOY_PATH`、`PXCZXN_DEPLOY_SSH_PRIVATE_KEY`。

历史迁移链依赖 Mars Admin 脚手架表，不能从空库重放。为启用 E2E，必须在 GitHub repository variables 配置经过脱敏、不可变的基线 SQL 地址 `PXCZXN_CI_BASELINE_SQL_URL` 和其 `PXCZXN_CI_BASELINE_SQL_SHA256`；该 SQL 不得包含用户、日志、令牌或上传文件数据。迁移步骤不再忽略 SQL 错误：基线导入后每个 verify 脚本必须通过，且写入的 SHA-256 历史必须和仓库一致。
