# M6-T004 监控与告警

生产 profile 暴露内部网络限定的 `/actuator/prometheus`。Compose 启动 Prometheus、Alertmanager、MySQL/Redis/Node exporter；`deploy/monitoring/prometheus.yml` 抓取 Spring Boot、MySQL、Redis 和宿主机指标，`platform-alerts.yml` 对服务不可用、5xx 比率、MySQL/Redis、磁盘和内存产生告警。部署前必须将 `alertmanager.yml.example` 复制为未提交的 `alertmanager.yml` 并配置受控 Webhook。

审核积压、通知积压、登录异常和慢查询需要从业务库生成受控的聚合指标，不能以用户数据或 IP 明细作为 Prometheus label。上线前在 SQL exporter 中增加仅包含计数/分位数的查询，并为这些队列配置阈值和告警负责人；这是生产监控验收的必要前置条件。
