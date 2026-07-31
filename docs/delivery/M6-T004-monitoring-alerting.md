# M6-T004 监控与告警

生产 profile 暴露内部网络限定的 `/actuator/prometheus`。原生 Prometheus、Alertmanager、MySQL/Redis/Node exporter 仅在回环地址监听；`deploy/monitoring/prometheus.yml` 抓取 Spring Boot、MySQL、Redis 和宿主机指标，`platform-alerts.yml` 对服务不可用、5xx 比率、MySQL/Redis、磁盘和内存产生告警。

飞书自定义机器人的消息格式与 Alertmanager webhook 不兼容，因此使用仅回环监听的 `pxczxn-alert-relay.service` 转换消息。Webhook 只存放于主机 `/etc/pxczxn/alertmanager-feishu.env`（`root:pxczxn 0640`），不得提交、显示在日志中或写入 Alertmanager 配置。`alertmanager.yml.example` 指向该本地 relay；relay 只接受 `/alertmanager`，并且只允许 `https://open.feishu.cn/open-apis/bot/v2/hook/` 地址。

审核队列和通知未读积压由后端定时聚合为 `pxczxn_community_review_queue_depth`、`pxczxn_community_review_queue_oldest_seconds` 与 `pxczxn_community_notification_unread_depth`；它们不带用户、IP 或内容标签。登录异常使用 `/api/v1/auth/login` 的 4xx 聚合，慢查询使用 MySQL exporter 的 `mysql_global_status_slow_queries` 增量。`platform-alerts.yml` 已为上述信号配置阈值；接入 Alertmanager 的受控 Webhook 后，仍须在预发触发并确认告警送达，才可完成监控验收。
