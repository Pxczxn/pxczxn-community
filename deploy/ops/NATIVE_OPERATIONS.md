# 原生部署运维脚本

这些脚本用于不使用 Docker 的生产主机。密钥只保存在主机的受限文件中，不能提交到仓库。

`/etc/pxczxn/ops.env` 示例（权限建议 `root:pxczxn 0640`）：

```dotenv
PXCZXN_BACKUP_DIR=/var/backups/pxczxn
PXCZXN_DATABASE=pxczxn_community
PXCZXN_BACKEND_SERVICE=pxczxn-community.service
PXCZXN_RELEASES_DIR=/app/pxczxn/releases
PXCZXN_BACKEND_UNIT_FILE=/etc/systemd/system/pxczxn-community.service
PXCZXN_WEB_SERVICE=pxczxn-web.service
PXCZXN_WEB_UNIT_FILE=/etc/systemd/system/pxczxn-web.service
PXCZXN_NGINX_SERVICE=nginx.service
PXCZXN_NGINX_SITE_FILE=/etc/nginx/sites-enabled/pxczxn
PXCZXN_BACKEND_HEALTH_URL=http://127.0.0.1:8849/api/v1/health
PXCZXN_WEB_HEALTH_URL=http://127.0.0.1:8847/
PXCZXN_MINIO_ENV=/etc/pxczxn/minio.env
PXCZXN_MINIO_ENDPOINT=http://127.0.0.1:9000
PXCZXN_MINIO_BUCKET=pxczxn-community
BACKUP_RETENTION_DAYS=14
```

MySQL 通过受限运维账户或本机 socket 认证。若不能使用 socket，在该文件中额外配置 `PXCZXN_MYSQL_DEFAULTS_FILE`，并将其指向一个不在仓库内的 MySQL defaults 文件。

- `native-backup.sh`：生成 MySQL 一致性逻辑备份、私有 MinIO 镜像和 SHA-256 清单；仅清理命名符合 UTC 时间戳的过期恢复点。
- `native-restore.sh <UTC备份编号> <数据库名>`：需要完整确认短语，校验清单后停止后端、替换指定生产库、回灌私有桶并重新启动服务。
- `native-rollback-release.sh <发布版本>`：只切换后端 JAR 并做健康检查，绝不回退数据库。
- `native-deploy-release.sh <发布版本> <发布包>`：校验 SHA-256 清单后，原子切换后端 JAR、React 服务和管理端静态资源；若 Nginx 校验或健康检查失败，自动恢复三个原来的服务配置。发布包不包含数据库变更，迁移必须先经评审的迁移流程完成。

恢复与回退必须在已记录维护窗口内执行。恢复失败时保留 `pxczxn_schema_version` 原样；从匹配的恢复点复原，不手工修改迁移历史。
