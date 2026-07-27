# V024 数据库迁移执行指南

## 前提条件
确保 MySQL 服务已启动，并且 `pxczxn_community` 数据库已创建。

## 执行步骤

### 1. 执行 V024 迁移
```bash
mysql -h localhost -P 3306 -u root -p pxczxn_community < database/migrations/V024__m3_team_application_review.sql
```

**预期结果**: 无错误输出

### 2. 验证迁移结果
```bash
mysql -h localhost -P 3306 -u root -p pxczxn_community < database/verify/V024__verify_team_application_review.sql
```

**预期输出**:
```
+----------------------------------------------+
| step                                         |
+----------------------------------------------+
| Checking team application review menus...    |
+----------------------------------------------+

+------------+
| menu_count |
+------------+
|          3 |
+------------+

+------+----------+-----------------------+--------------------------+
| id   | parent_id| name                  | permission               |
+------+----------+-----------------------+--------------------------+
| 9111 | 9110     | 团队申请审核          | community:team:review    |
| 9112 | 9111     | 查询申请              | community:team:review    |
| 9113 | 9111     | 审批申请              | community:team:review    |
+------+----------+-----------------------+--------------------------+

+----------------------------------------------+
| step                                         |
+----------------------------------------------+
| Checking admin role permissions...           |
+----------------------------------------------+

+----------------+
| role_menu_count|
+----------------+
|              3 |
+----------------+

+-------------------------------+
| result                        |
+-------------------------------+
| V024 verification complete    |
+-------------------------------+
```

### 3. 检查唯一索引是否创建成功
```bash
mysql -h localhost -P 3306 -u root -p pxczxn_community -e "SHOW INDEX FROM team_application WHERE Key_name = 'uk_team_slug_active';"
```

**预期输出**: 应显示 `uk_team_slug_active` 索引，包含 `team_slug` 和 `is_slug_active` 两个列；
`is_slug_active` 是仅在 `PENDING`、`APPROVED` 状态取值为 `1` 的生成列。

### 4. 测试唯一约束（可选）
```sql
-- 插入测试数据
INSERT INTO team_application (applicant_user_id, team_name, team_slug, status, lock_version, created_at, updated_at)
VALUES (1, 'Test Team 1', 'test-slug', 'PENDING', 0, NOW(), NOW());

-- 尝试插入相同 slug 的 APPROVED 申请（应该失败；不能同时有 PENDING 与 APPROVED）
INSERT INTO team_application (applicant_user_id, team_name, team_slug, status, lock_version, created_at, updated_at)
VALUES (2, 'Test Team 2', 'test-slug', 'APPROVED', 0, NOW(), NOW());
-- 预期: ERROR 1062 (23000): Duplicate entry 'test-slug-1' for key 'uk_team_slug_active'

-- 插入相同 slug 但不同状态（应该成功）
INSERT INTO team_application (applicant_user_id, team_name, team_slug, status, lock_version, created_at, updated_at)
VALUES (3, 'Test Team 3', 'test-slug', 'REJECTED', 0, NOW(), NOW());
-- 预期: Query OK, 1 row affected

-- 清理测试数据
DELETE FROM team_application WHERE team_slug = 'test-slug';
```

## 如果需要回滚
```bash
mysql -h localhost -P 3306 -u root -p pxczxn_community < database/rollback/R024__rollback_team_application_review.sql
```

## 故障排查

### 问题1: 表不存在
**错误**: `Table 'pxczxn_community.team_application' doesn't exist`
**原因**: V023 未执行
**解决**: 先执行 `V023__m3_team_foundation.sql`

### 问题2: 索引已存在
**错误**: `Duplicate key name 'uk_team_slug_active'`
**原因**: V024 已经执行过
**解决**: 跳过或先执行 rollback

### 问题3: 菜单ID冲突
**错误**: `Duplicate entry '9111' for key 'PRIMARY'`
**原因**: 菜单已存在
**解决**: V024 是幂等的，使用 `INSERT INTO ... ON DUPLICATE KEY UPDATE`，不应该报错

## 执行完成后
请在此文件末尾添加执行结果：

```
执行时间: YYYY-MM-DD HH:MM:SS
执行人: 
V024迁移: [ ] 成功 [ ] 失败
Verify脚本: [ ] 成功 [ ] 失败
唯一索引: [ ] 已创建
备注: 
```
