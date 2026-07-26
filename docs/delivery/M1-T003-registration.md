# M1-T003 社区用户注册交付说明

## API

```http
GET  /api/v1/auth/check-username?username={username}
GET  /api/v1/auth/check-email?email={email}
POST /api/v1/auth/register
```

注册请求：

```json
{
  "username": "alice",
  "email": "alice@example.com",
  "password": "replace-with-a-strong-password",
  "displayName": "Alice"
}
```

成功响应中的 `userId` 和 `blogId` 均为字符串，避免 JavaScript 丢失
BIGINT 精度。注册成功不自动登录，会话由 M1-T004 负责。

## 注册事务

单一事务按以下顺序执行：

```text
community_user
  -> community_user_login_account
  -> community_user_preference
  -> blog(PERSONAL)
  -> blog_setting
  -> blog_category(未分类)
  -> community_user.personal_blog_id
```

任一步失败，整个事务回滚。应用预先检查用户名和邮箱以提供及时反馈，
数据库唯一约束仍作为并发竞争的最终防线。

## 输入约定

- 用户名：3-32 位字母、数字、下划线或连字符，存储为小写。
- 邮箱：去除首尾空白并转为小写，最长 320 个字符。
- 密码：至少 8 个字符，UTF-8 编码后不超过 72 字节，使用 BCrypt。
- 显示名称：可选，默认使用规范化用户名，最长 80 个字符。
- 个人博客 slug：注册时使用规范化用户名，此后作为稳定公开地址。

## 验证

- 单元测试覆盖完整聚合创建、重复用户名、并发邮箱冲突映射和 BCrypt
  字节边界。
- 控制器测试覆盖字符串 ID 和可用性接口。
- 真实 MySQL 验证覆盖成功注册、重复注册和中途唯一键冲突后的事务回滚。
- 验证用用户、博客和冲突占位记录已全部清理。
