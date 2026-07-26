# M2-T001 社区互动数据结构

## 状态

完成。

## 交付范围

- 新增 `V007__community_interaction_foundation.sql`，建立以下七张 M2 表：
  - `community_follow`
  - `community_moment`
  - `community_comment`
  - `community_content_like`
  - `favorite_folder`
  - `favorite_item`
  - `favorite_folder_item`
- 关注、点赞、收藏条目和收藏夹映射均建立业务唯一约束，支持幂等写入。
- 动态与评论保留可见性、审核状态、软删除、计数和时间字段。
- 收藏夹支持系统默认收藏夹与用户自定义收藏夹；同一用户只允许一个有效默认收藏夹。
- 全部关联使用外键约束；收藏夹所有者使用 `ON DELETE RESTRICT`，避免 MySQL
  对生成列参与级联更新的限制。
- 新增 `V007__verify_community_interaction_foundation.sql`，核对表、列、唯一约束和
  外键数量。
- 迁移使用 `CREATE TABLE IF NOT EXISTS`，允许结构一致时重复执行，但不会掩盖
  已存在表的结构漂移。

## 验证结果

```text
Database: 当时的本地过渡库（阶段 5 已迁移为 `pxczxn_community`）
Migration first run: PASS
Migration repeat run: PASS
M2 interaction tables: 7
community_follow required columns: 8
Business unique constraints: 5
Foreign keys: 14
V007 structure verification: PASS
```

## 主要文件

- `database/migrations/V007__community_interaction_foundation.sql`
- `database/verify/V007__verify_community_interaction_foundation.sql`
- `database/README.md`

## 后续约束

- MySQL 关系表是关注、点赞、收藏和通知的最终事实来源。
- 目标表计数必须与关系写入处于同一事务，并使用不小于零的原子递减。
- 私密内容的互动关系不得成为绕过内容可见性检查的入口。
- Redis 只能用于可丢失缓存，不能成为互动关系的唯一存储。
