# 数据库迁移

核心数据库使用 MySQL 8.x、InnoDB 和 `utf8mb4`。SQL 迁移由仓库文件管理，
不使用 Flyway 或 Liquibase。

## 当前版本

```text
V011
```

V001 创建 15 张 M1 社区业务基础表；V002 注册后台社区标签菜单和 RBAC；
V003 创建自动内容审核关键词规则表；V004 注册后台文章审核工作台及动作权限；
V005 创建文章定时发布持久任务并注册 Quartz 扫描任务；V006 注册社区运营
工作台、用户、博客与文章查询页面及 RBAC。
V007 创建关注、动态、评论、点赞、收藏与收藏夹的 M2 互动基础表。
V008 为用户偏好增加喜欢列表公开范围。
V009 增加评论范围约束与评论治理事件表。
V010 增加动态公开流索引，以及链接、引用和纯转发的内容形状检查约束。
V011 增加通知分类、重要等级、聚合字段、活动时间、稳定去重与收件箱索引。
后台 `sys_*` 表来自管理脚手架，当前
本地环境暂时与社区表共用同一数据库。

## 新环境

先创建数据库：

```sql
CREATE DATABASE `pxczxn_community`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
```

初始化后台系统表后，进入 `database` 目录并按版本号顺序执行：

```powershell
Set-Location .\database
mysql --user=root --password --database=pxczxn_community `
  --execute="source ./migrations/V001__m1_core_schema.sql"
mysql --user=root --password --database=pxczxn_community `
  --execute="source ./migrations/V002__community_admin_tag_permissions.sql"
mysql --user=root --password --database=pxczxn_community `
  --execute="source ./migrations/V003__content_keyword_rules.sql"
mysql --user=root --password --database=pxczxn_community `
  --execute="source ./migrations/V004__community_admin_review_permissions.sql"
mysql --user=root --password --database=pxczxn_community `
  --execute="source ./migrations/V005__scheduled_article_publication.sql"
mysql --user=root --password --database=pxczxn_community `
  --execute="source ./migrations/V006__community_admin_query_permissions.sql"
mysql --user=root --password --database=pxczxn_community `
  --execute="source ./migrations/V007__community_interaction_foundation.sql"
mysql --user=root --password --database=pxczxn_community `
  --execute="source ./migrations/V008__like_list_privacy.sql"
mysql --user=root --password --database=pxczxn_community `
  --execute="source ./migrations/V009__comment_scope_and_moderation.sql"
mysql --user=root --password --database=pxczxn_community `
  --execute="source ./migrations/V010__moment_publication_constraints.sql"
mysql --user=root --password --database=pxczxn_community `
  --execute="source ./migrations/V011__community_notification_inbox.sql"
```

密码通过 MySQL 的交互提示或环境变量提供，不写入脚本。

## 已部署环境升级

1. 确认当前数据库与版本。
2. 完成全量备份。
3. 在维护窗口执行下一个版本 SQL。
4. 执行对应 `verify` 文件。
5. 在本文件的执行记录中追加环境、时间、版本和结果。

V001 只创建新表，不修改历史数据；主要风险是创建表时取得元数据锁。

## 可重复执行

V001 使用 `CREATE TABLE IF NOT EXISTS`，在结构一致时可重复执行。它不会尝试
自动修复一个已经存在但字段不一致的同名表；出现这种情况必须停止部署并人工
核对，不能用 `IF NOT EXISTS` 掩盖结构漂移。

V002、V004 和 V006 使用固定菜单 ID 和受保护的角色授权插入；V003、V005 与 V007
使用 `CREATE TABLE IF NOT EXISTS`，V008 通过元数据检查保护重复 `ALTER TABLE`，
V009 使用 `CREATE TABLE IF NOT EXISTS` 并通过元数据检查保护评论范围约束，
V010 通过元数据检查保护动态流索引和内容形状约束，V005 的 Quartz 任务也使用
受保护插入；V011 通过元数据检查保护通知字段、索引和约束，并在建立唯一键前
安全处理历史重复去重键。每次重复
执行后仍必须运行对应 `verify` 文件，确认已有结构没有漂移。

## 失败处理

- 任一建表语句失败后立即停止后续业务部署。
- 使用 `verify/V001__verify_m1_core_schema.sql` 核对实际结构。
- 空库可在确认无业务数据后使用受保护的 rollback 脚本清理。
- 已有数据的环境优先从备份恢复，不直接执行 DROP。

## 回滚保护

`rollback/V001__rollback_m1_core_schema.sql` 默认拒绝执行。只有在同一 MySQL
会话显式设置确认变量后才会删除表。

## 执行记录

| 环境 | 数据库 | 版本 | 时间 | 结果 |
|---|---|---|---|---|
| local-test | `pxczxn_community_m1_test` | V001 | 2026-07-25 | 首次与重复执行通过，结构验证通过 |
| local | `mars-system`（过渡） | V001 | 2026-07-25 | 首次与重复执行通过，15 张表验证通过 |
| local | `mars-system`（过渡） | V002 | 2026-07-25 | 菜单、权限与管理员授权验证通过 |
| local | `mars-system`（过渡） | V003 | 2026-07-25 | 首次与重复执行通过，表、字段和索引验证通过 |
| local | `mars-system`（过渡） | V004 | 2026-07-25 | 文章审核菜单、动作权限与 admin 角色授权验证通过 |
| local | `mars-system`（过渡） | V005 | 2026-07-25 | 定时发布任务表与 Quartz 注册验证通过 |
| local | `mars-system`（过渡） | V006 | 2026-07-25 | 社区运营页面、权限、admin 授权与菜单排序验证通过 |
| local | `mars-system`（过渡） | V007 | 2026-07-25 | 首次与重复执行通过，7 张互动表、5 个业务唯一约束和 14 个外键验证通过 |
| local | `mars-system`（过渡） | V008 | 2026-07-25 | 首次与重复执行通过，喜欢列表公开范围字段、默认值与检查约束验证通过 |
| local | `mars-system`（过渡） | V009 | 2026-07-25 | 重复执行通过，治理事件表、8 个必需字段、评论范围约束和历史值验证通过 |
| local | `mars-system`（过渡） | V010 | 2026-07-26 | 重复执行通过，1 个动态流索引、3 个内容形状约束和历史值验证通过 |
| local | `mars-system`（过渡） | V011 | 2026-07-26 | 首次与重复执行通过，4 个收件箱字段、2 个索引、3 个约束及历史值验证通过 |
