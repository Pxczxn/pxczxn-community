# M6-T006 发布验收记录

## 已通过的仓库门禁

- 集成分支完整 `mvnw.cmd test`：通过。
- 博客端 `typecheck`、`lint`、`test`、`build`：通过；测试契约已更新为 M5 的 `discoverRankedArticles` 真实接口。
- 管理端 `typecheck`、`lint`、`test`、`build`：通过。Lint 报告了既有 `any` warning，未新增 error。
- `scripts/validate-database-migration-manifest.ps1`：38 个迁移及 verify 文件一一匹配；V036–V038 已在验收数据库执行并通过。
- 2026-07-29：本机 `pxczxn_community` 已在仓库外一致性备份后从 V022 升级至 V038；`check-database-migrations.ps1` 通过 38 个在线 verify 与全部历史 checksum。迁移校验已按 UTF-8/LF 规范化，避免 Windows CRLF 工作区产生伪 checksum 漂移。
- 2026-07-29：临时生产 profile 实例在 `127.0.0.1:8861` 通过 `/api/v1/health` 与 `/actuator/prometheus` 检查；`pxczxn_community_review_queue_depth` 和 `pxczxn_community_notification_unread_depth` 均已注册。该临时进程已在验证后停止。
- 生产 Compose 以替换后的非敏感变量执行 `docker compose config --quiet`：通过。
- 2026-07-29：Windows 本机直接运行链路已验证。已确认本机 MySQL `pxczxn_community` 为 V001–V038；后端在 `8849` 返回健康 `UP`，博客端 `/discover`（`8847`）与运营端（`8848`）均返回 HTTP 200，`scripts/e2e/m6-public-smoke.mjs` 对健康、公开搜索、RSS ETag/304 与匿名统计权限边界通过。该项是本机联调证据，不替代受控预发/生产发布演练。
- 2026-07-29：`Release Gates` 工作流已使用标准 YAML 解析器复核；修正 GitHub 表达式在行内映射中的无效写法后，工作流、Compose 配置和 38 个迁移清单均可通过结构校验。
- 2026-07-29：无 Docker 的本地发布演练完成。主库已应用 V039/V040 权限修复；M3 团队全流程与 M4 举报申诉、屏蔽、处罚、内容规则、反滥用 E2E 均在一次性数据库中通过，临时库已删除。`pxczxn_community` 的一致性备份已恢复至一次性库，表数 99/99、迁移历史 40/40 一致，40 个 migration verify、恢复后独立后端健康检查与 M6 公开烟测均通过。该记录仅证明本机恢复链路，不替代受控预发/生产演练。
- 2026-07-29：原生隔离全量验证已更新至 V042。`scripts/verify-isolated-local.ps1` 使用 `mysqldump --single-transaction` 从 `pxczxn_community` 创建快照，并仅向临时库 `pxczxn_local_verify` 恢复；随后在 8852 端口执行后端 Maven、博客端/运营端类型检查、Lint、测试与构建、依赖安全与 SBOM、42 个迁移校验、安全检查，以及聊天、评论、动态、通知、治理五组 E2E。全部通过后，脚本恢复临时库验证码配置、停止临时后端并删除临时库。主库 V001–V042、99 张表注释均已检查为无空注释、无纯英文注释、无问号占位乱码；本地后端 8849 健康检查为 `UP`。

## 发布前必须在受控生产/预发环境完成

1. Docker Desktop/生产 Docker 引擎可用后，构建两份生产镜像并验证 Compose 所有服务 healthy。
2. 导入经脱敏、版本化、带 SHA-256 的 Mars Admin 基线，再执行迁移历史与真实 E2E。现有本地旧备份包含运行数据，不能进入仓库或 CI。
3. 执行并留存 MySQL/MinIO 备份、恢复和 Redis staging 演练证据。
4. 配置 Alertmanager 的未提交接收端文件，触发并确认服务、数据库、缓存和队列告警。
5. 以 `PXCZXN_CANARY_MAX_REGISTERED_USERS=100` 开始邀请灰度；满足运营准入指标后才能设置为 `0` 正式开放。

上述外部演练完成前，M6 不应标记为发布完成。
