# M6-T004 监控与告警

生产 profile 暴露内部网络限定的 `/actuator/prometheus`。Compose 启动 Prometheus、Alertmanager、MySQL/Redis/Node exporter；`deploy/monitoring/prometheus.yml` 抓取 Spring Boot、MySQL、Redis 和宿主机指标，`platform-alerts.yml` 对服务不可用、5xx 比率、MySQL/Redis、磁盘和内存产生告警。部署前必须将 `alertmanager.yml.example` 复制为未提交的 `alertmanager.yml` 并配置受控 Webhook。

审核队列和通知未读积压由后端定时聚合为 `pxczxn_community_review_queue_depth`、`pxczxn_community_review_queue_oldest_seconds` 与 `pxczxn_community_notification_unread_depth`；它们不带用户、IP 或内容标签。登录异常使用 `/api/v1/auth/login` 的 4xx 聚合，慢查询使用 MySQL exporter 的 `mysql_global_status_slow_queries` 增量。`platform-alerts.yml` 已为上述信号配置阈值；接入 Alertmanager 的受控 Webhook 后，仍须在预发触发并确认告警送达，才可完成监控验收。
