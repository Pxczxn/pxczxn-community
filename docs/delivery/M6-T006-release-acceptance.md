# M6-T006 发布验收记录

## 已通过的仓库门禁

- 集成分支完整 `mvnw.cmd test`：通过。
- 博客端 `typecheck`、`lint`、`test`、`build`：通过；测试契约已更新为 M5 的 `discoverRankedArticles` 真实接口。
- 管理端 `typecheck`、`lint`、`test`、`build`：通过。Lint 报告了既有 `any` warning，未新增 error。
- `scripts/validate-database-migration-manifest.ps1`：38 个迁移及 verify 文件一一匹配；V036–V038 已在验收数据库执行并通过。
- 生产 Compose 以替换后的非敏感变量执行 `docker compose config --quiet`：通过。

## 发布前必须在受控生产/预发环境完成

1. Docker Desktop/生产 Docker 引擎可用后，构建两份生产镜像并验证 Compose 所有服务 healthy。
2. 导入经脱敏、版本化、带 SHA-256 的 Mars Admin 基线，再执行迁移历史与真实 E2E。现有本地旧备份包含运行数据，不能进入仓库或 CI。
3. 执行并留存 MySQL/MinIO 备份、恢复和 Redis staging 演练证据。
4. 配置 Alertmanager 的未提交接收端文件，触发并确认服务、数据库、缓存和队列告警。
5. 以 `PXCZXN_CANARY_MAX_REGISTERED_USERS=100` 开始邀请灰度；满足运营准入指标后才能设置为 `0` 正式开放。

上述外部演练完成前，M6 不应标记为发布完成。
