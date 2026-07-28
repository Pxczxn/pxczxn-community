# M6-T001 生产环境运行手册

生产拓扑为 `community`（用户站）和 `admin`（运营站）两个 HTTPS 域名；Nginx 是唯一对外入口。MySQL、Redis、MinIO、Spring Boot 与用户站均只在 Compose 内网通信，宿主机仅暴露 `80`、`443`。

## 首次部署

1. 将 `deploy/.env.example` 复制为 `deploy/.env`，替换所有 `REPLACE_WITH` 值，并设置真实域名。不要将 `.env` 提交到仓库。
2. 将两个域名的证书放在 `${LETSENCRYPT_DIR}/live/<domain>/fullchain.pem` 与 `privkey.pem`；可先用 Certbot 的 webroot 模式签发，webroot 为 `${CERTBOT_WEBROOT}`。证书续期后运行 `docker compose -f deploy/docker-compose.yml restart nginx`。
3. 在仓库根目录运行 `docker compose --env-file deploy/.env -f deploy/docker-compose.yml up -d --build`。
4. 完成数据库迁移：`powershell -ExecutionPolicy Bypass -File scripts/invoke-database-migrations.ps1 -DatabaseName <生产库名>`。仅在已验证备份且维护窗口内执行。
5. 在 MinIO 控制台创建私有 bucket，并在运营后台配置存储 endpoint、bucket、access key 与 secret；不要将 OSS 密钥写入 Compose 文件。

## 验证与日志

- `docker compose --env-file deploy/.env -f deploy/docker-compose.yml ps` 中所有服务应为 healthy。
- 从外网验证 `https://<用户域名>/`、登录与发布流程；验证 `https://<管理域名>/` 运营登录。
- Nginx 和容器日志使用 Docker local logging driver，单文件 10 MiB、保留 5 个文件；使用 `docker compose ... logs --since 15m <service>` 排查。
- Spring Boot 健康检查仅供 Compose 通过内部网络访问：`http://backend:8849/actuator/health`。

## 安全边界

- 生产必须使用 `prod` profile；该 profile 强制 Redis 并要求 CORS 来源显式配置。
- Nginx 仅允许 TLS 1.2/1.3，启用 HSTS、nosniff、SAMEORIGIN 和转发协议头。
- 证书、`.env`、卷数据和备份均不得进入 Git；数据库 root 密码仅供 MySQL 初始化与运维恢复使用。
