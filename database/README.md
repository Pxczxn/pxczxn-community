# 数据库迁移

星语社区业务数据库使用 MySQL 8.x、InnoDB 和 `utf8mb4`。MySQL 是唯一业务
事实来源；Redis 只提供可选增强。SQL 迁移继续由仓库中的
`database/migrations/Vxxx__description.sql` 管理，不引入 Flyway 或 Liquibase。

## 当前基线

- 当前数据库：`pxczxn_community`
- 当前迁移：V015
- 迁移历史表：`pxczxn_schema_version`
- 在线校验：V001 至 V015 各有一个 `database/verify` 脚本
- 后台兼容表：继续保留稳定的 `sys_*` 表名

V001 创建 M1 社区业务基础表；V002 至 V006 建立标签、审核、定时发布和运营
查询能力；V007 至 V012 建立互动、隐私、评论治理、动态、通知和运营治理能力；
V013 写入星语社区管理端品牌；V014 增加管理员密码生命周期字段；V015 默认
关闭并脱敏待删除的支付、短信、微信和第三方集成。

## 迁移历史表

`scripts/invoke-database-migrations.ps1` 自动创建并维护：

| 字段 | 含义 |
| --- | --- |
| `version` | 唯一迁移版本，如 `V015` |
| `description` | 从迁移文件名提取的描述 |
| `checksum` | SQL 文件原始内容的 SHA-256 |
| `executed_at` | 最近一次执行或登记时间 |
| `success` | `1` 表示迁移和在线校验均成功 |

已成功版本不会重复执行。脚本会先比较仓库文件与历史表中的 checksum；任一已
登记文件发生变化会立即中止，不会继续执行后续 SQL。

## 正常升级

完成数据库备份后，在仓库根目录执行：

```powershell
.\scripts\invoke-database-migrations.ps1 `
  -Database pxczxn_community `
  -DatabaseUser root
```

密码通过参数、MySQL 交互环境或部署系统的安全变量提供，不提交到仓库。脚本按
版本排序，每个版本依次执行迁移和对应在线校验，二者都成功后才写入
`success=1`。

只读检查版本、checksum 和实际结构：

```powershell
.\scripts\check-database-migrations.ps1 `
  -Database pxczxn_community `
  -DatabaseUser root
```

该检查会验证：

1. 文件名、版本连续性和重复版本。
2. 迁移与在线校验脚本一一对应。
3. V001 至 V015 的真实数据库结构与数据约束。
4. 历史记录数量、成功状态和 SHA-256 checksum。

## 已有数据库建立基线

`-BaselineExisting` 只用于已经完成迁移但尚无历史表的受控数据库。它不会直接
信任现状，而是先执行每个版本的在线校验，全部通过后才登记对应 checksum：

```powershell
.\scripts\invoke-database-migrations.ps1 `
  -Database pxczxn_community `
  -DatabaseUser root `
  -BaselineExisting
```

存在失败记录、校验失败或 checksum 冲突时禁止建立基线。

## 库名迁移

阶段 5 使用新建数据库、逻辑备份、导入、逐表精确行数比对、在线结构验证和
checksum 基线登记完成库名迁移：

```powershell
.\scripts\migrate-database-name.ps1 `
  -SourceDatabase <legacy_database> `
  -TargetDatabase pxczxn_community `
  -DatabaseUser root `
  -BackupDirectory <external_backup_directory>
```

脚本具有以下保护：

- 目标库已存在时拒绝覆盖。
- 不使用 `RENAME DATABASE`。
- 不删除或修改源库。
- 创建目标后发生失败时保留现场，不自动执行 `DROP DATABASE`。
- 使用 `mysqldump --single-transaction` 创建一致性备份并记录 SHA-256。
- 比较所有业务表名称和逐表 `COUNT(*)` 精确行数。
- 通过 V001 至 V015 在线校验后才登记迁移历史。

## 不可变迁移

已登记成功的 `Vxxx` 文件不可修改。需要调整数据库结构时必须新增下一个版本。
`CREATE TABLE IF NOT EXISTS` 和元数据保护只能保证脚本可重入，不能替代 checksum
和在线结构校验，也不能用于掩盖结构漂移。

## 失败处理

- 任一迁移或校验失败后立即停止后续部署。
- 保留失败记录和数据库现场，核对执行输出、备份和对应 `verify` 文件。
- 空测试库可以在确认无业务数据后使用受保护 rollback 脚本。
- 已有业务数据的环境优先切回保留的源库或从一致性备份恢复。
- 禁止直接改写历史表把失败状态伪装为成功。

## 回滚保护

已有 rollback SQL 默认拒绝破坏性执行。只有在同一 MySQL 会话显式设置确认变量
后才允许删除对应对象；生产或含业务数据的库不应使用结构删除作为首选回滚。

## 执行记录

| 环境 | 数据库 | 版本 | 时间 | 结果 |
| --- | --- | --- | --- | --- |
| local | `pxczxn_community` | V001–V014 | 2026-07-26 | 源库逻辑复制、71 张表逐表精确行数一致、14 个在线校验通过、checksum 基线登记成功 |
