# 阶段 5：数据库名称与迁移记录验收

验收日期：2026-07-26

## 1. 提交与操作边界

本阶段只处理数据库逻辑复制、运行时连接切换和迁移历史治理，没有混入 Spring
Boot 升级、模块重命名或脚手架模块删除。稳定的 `sys_*` 表名和公开 API 路径
保持不变，也未引入 Flyway 或 Liquibase。

相关提交：

| 提交 | 内容 |
| --- | --- |
| `4899345` | 增加安全数据库克隆与 checksum 迁移工具 |
| `fbad35e` | 修复 PowerShell 单值结果解析 |
| `7d12336` | 增加只读迁移历史门禁 |
| `9db79b8` | 将运行时、E2E 和文档切换到 `pxczxn_community` |

## 2. 迁移方法

迁移使用 `scripts/migrate-database-name.ps1` 完成：

1. 验证源库存在且目标库不存在。
2. 使用 `mysqldump --single-transaction` 创建一致性逻辑备份。
3. 读取并复用源库字符集与排序规则。
4. 新建 `pxczxn_community` 并导入备份。
5. 比较 71 张源表和目标表的名称及逐表精确 `COUNT(*)`。
6. 在目标库执行 V001 至 V014 全部在线验证。
7. 验证通过后将 14 个版本及 SHA-256 登记为成功基线。

迁移没有使用 `RENAME DATABASE`，没有删除、改名或修改源库。脚本对已存在目标
库拒绝覆盖；目标创建后若失败会保留现场，不自动执行 `DROP DATABASE`。

## 3. 备份与回退点

- 备份目录：
  `D:\Coding\project\java-code\pxczxn-backups\20260726-stage5-database-migration`
- 备份文件：`legacy-database-before-stage5.sql`
- 文件大小：985,572 bytes
- SHA-256：
  `194E27C165A5A9DF9D7FE0F812D36EA340225D3645067D4C902570BF0B817ED1`
- 源库：保留且可读
- 目标库：`pxczxn_community`
- 字符集/排序规则：`utf8mb4` / `utf8mb4_general_ci`

备份目录内的 `migration-manifest.txt` 保存源库、目标库、表数、checksum、完成时间
和源库保留状态。

## 4. 迁移历史

目标库新增 `pxczxn_schema_version`，字段满足：

| 字段 | 类型 | 可空 |
| --- | --- | --- |
| `version` | `varchar(32)` | 否 |
| `description` | `varchar(255)` | 否 |
| `checksum` | `char(64)` | 否 |
| `executed_at` | `datetime(3)` | 否 |
| `success` | `tinyint(1)` | 否 |

当前共有 14 条记录，V001 至 V014 全部 `success=1`。

`scripts/invoke-database-migrations.ps1` 的契约验证结果：

- 对已完成的 14 个版本执行 `-CheckOnly`：14 个全部校验 checksum 并跳过，
  未重复执行 SQL。
- 使用迁移副本改变 V001 内容：脚本在执行任何 SQL 前报告
  `Checksum mismatch for V001` 并失败。
- 失败记录、缺失记录、额外记录、校验文件缺失和历史数量不一致都会阻断门禁。

## 5. 连接切换

以下默认值统一切换为 `pxczxn_community`：

- `application-local.yml` 的本地 JDBC URL。
- 根目录 `scripts/verify.ps1`。
- 数据库迁移在线检查脚本。
- 评论、动态、通知和治理 E2E。
- 治理 Node E2E 的 `PXCZXN_DB_NAME` 回退值。
- 根目录与数据库运维文档。

生产环境继续通过 `PXCZXN_DB_URL`、`PXCZXN_DB_USERNAME` 和
`PXCZXN_DB_PASSWORD` 显式注入，不写死生产凭据。

## 6. 数据验证

迁移导入完成时：

- 源表数量：71。
- 目标业务/平台表数量：71。
- 表集合差异：0。
- 逐表精确行数差异：0。
- V001 至 V014 在线结构与数据约束验证：14/14 通过。

全量 E2E 完成后的再次比对只有三张审计表按预期增长：

- `sys_api_access_log`：新增 159 条验证访问记录。
- `sys_login_log`：新增 2 条验证登录记录。
- `sys_oper_log`：新增 12 条验证操作记录。

其他源表与目标表行数一致。新增的 `pxczxn_schema_version` 是目标库专属迁移治理
表，不属于源库复制表集合。

## 7. 阶段全量门禁

最终 `scripts/verify.ps1` 输出 `ALL VERIFICATION GATES PASSED`：

- 后端 Maven `verify` 通过。
- 博客端 typecheck、Lint、测试和构建通过。
- 管理端 typecheck、Lint、测试和构建通过。
- 依赖漏洞扫描与三份 CycloneDX SBOM 通过。
- 14 个在线迁移验证和 14 个 checksum 历史校验通过。
- CORS、WebSocket Ticket、管理员密码、生产配置和统一鉴权检查通过。
- 评论、动态、通知和治理真实 E2E 全部通过。
- 验证后验证码配置已恢复，临时后端进程已停止。

## 8. 结论

`pxczxn_community` 已成为本地唯一运行时业务事实库；旧库和迁移前一致性备份
作为回退点保留。阶段 5 验收通过，可以进入阶段 6 的脚手架引用审计与瘦身。
