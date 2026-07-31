# M6-T002 CI/CD 交付说明

`Release Gates` 在推送、合并请求和每周例行检查时执行后端验证、两个前端的类型检查/lint/test/build、迁移清单校验和依赖安全扫描。配置受控的数据库基线后，它还会启动真实 Spring Boot 实例执行公开接口 E2E，并严格核对该基线的 SHA-256、迁移历史和在线 verify 脚本。

所有门禁通过后，流水线会构建一个原生发布包：Spring Boot JAR、React 服务构建产物与其运行时依赖、管理端静态文件，以及覆盖全部文件的 SHA-256 清单。发布包保留 14 天。仅 `main` 上手动触发 `deploy=true` 才能进入 `production` environment；应在 GitHub 环境设置中配置 required reviewers。

生产发布不使用 Docker。部署作业上传经门禁验证的发布包，并调用主机的 `/usr/local/lib/pxczxn/ops/native-deploy-release.sh`；该脚本会校验清单、切换 systemd 后端和 React 服务以及 Nginx 管理端资源，健康检查失败会恢复原配置。生产环境需要以下 GitHub repository secret：

- `PXCZXN_DEPLOY_HOST`：部署主机名或地址。
- `PXCZXN_DEPLOY_USER`：只用于发布的 SSH 账户；该账户只应具备执行该发布脚本所需的无密码 `sudo` 权限。
- `PXCZXN_DEPLOY_SSH_PRIVATE_KEY`：此专用发布账户的私钥。
- `PXCZXN_DEPLOY_KNOWN_HOSTS`：已从可信控制台核对过的完整 OpenSSH 主机指纹行；流水线始终使用 `StrictHostKeyChecking=yes`，不得以 `accept-new` 绕过校验。

数据库迁移不会被发布作业自动执行。涉及迁移的版本必须先完成已评审的备份、迁移和 verify 流程，再允许触发应用发布；这样应用版本回退不会暗中回退或破坏数据库历史。

历史迁移链依赖 Mars Admin 脚手架表，不能从空库重放。为启用 E2E，必须在 GitHub repository variables 配置经过脱敏、不可变的基线 SQL 地址 `PXCZXN_CI_BASELINE_SQL_URL` 和其 `PXCZXN_CI_BASELINE_SQL_SHA256`；该 SQL 不得包含用户、日志、令牌或上传文件数据。迁移步骤不再忽略 SQL 错误：基线导入后每个 verify 脚本必须通过，且写入的 SHA-256 历史必须和仓库一致。
