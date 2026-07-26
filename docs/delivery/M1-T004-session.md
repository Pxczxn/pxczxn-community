# M1-T004 登录与会话交付说明

## API

```http
POST /api/v1/auth/login
POST /api/v1/auth/logout
GET  /api/v1/account/me
```

登录请求：

```json
{
  "email": "alice@example.com",
  "password": "replace-with-the-user-password"
}
```

登录成功返回：

```json
{
  "tokenName": "pxczxn-community-token",
  "tokenValue": "<opaque-token>",
  "expiresIn": 604799,
  "userId": "<string-bigint>",
  "username": "alice"
}
```

后续社区请求通过 `pxczxn-community-token` Header 携带 Token。
管理员使用的 `Authorization` Header 不会被社区会话读取。

## 状态与安全策略

- `NORMAL`、`LIMITED`：允许登录。
- `FROZEN`、`BANNED`、`DEACTIVATED`、`DELETED`：拒绝登录和继续读取
  当前账号。
- 邮箱不存在与密码错误统一返回“邮箱或密码错误”，避免账号枚举。
- 连续 5 次错误密码后锁定 15 分钟。
- 登录成功清空失败计数和锁定时间。
- 密码、完整 Token 不写入应用日志。
- 失败计数保存在 MySQL；Redis 不可用不影响核心登录链。

## 会话策略

```text
login type: community
header: pxczxn-community-token
cookie read: disabled
request-body token read: disabled
concurrent sessions: enabled
shared token between logins: disabled
token timeout: 7 days
active timeout: 30 minutes
token style: UUID
```

## 验证

真实 MySQL 与实际启动服务验证了登录、当前用户、Token 隔离、退出失效、
五次失败锁定和锁定期拒绝正确密码。所有验证数据已清理。
