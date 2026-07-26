# 08 后端架构与 `pxczxn-admin`

## 0. 命名与脚手架边界

`pxczxn-admin` 是本项目后台基础工程。原始脚手架来源仅作技术来源记录；新增工程模块、Java 包、Token、前端项目及业务配置统一使用 `pxczxn` 命名。Java 根包统一为 `top.pxczxn.community`。

## 1. 总体结构

```text
pxczxn-community
├── pxczxn-common
├── pxczxn-infra
├── pxczxn-core
│   ├── pxczxn-system
│   ├── pxczxn-auth
│   ├── pxczxn-file
│   ├── pxczxn-message
│   └── pxczxn-biz        博客社区业务
├── pxczxn-api
│   ├── pxczxn-admin-api  平台后台接口
│   ├── pxczxn-web-api    社区与公开网页接口
│   └── pxczxn-app-api    V1 不扩展
├── pxczxn-job
├── pxczxn-starter
├── pxczxn-admin             平台运营后台
└── pxczxn-web       独立用户端前端
```

## 2. 模块职责

- `pxczxn-system`：后台管理员、角色、菜单、字典、配置和日志。
- `pxczxn-biz`：所有博客社区业务。
- `pxczxn-admin-api`：审核、运营和治理接口。
- `pxczxn-web-api`：注册、博客、文章、互动和公开接口。
- `pxczxn-admin`：平台运营管理端。
- `pxczxn-web`：Next.js 用户端。

## 3. pxczxn-biz 领域包

```text
user
blog
article
taxonomy
series
moment
interaction
follow
favorite
submission
collaboration
moderation
notification
operation
shared
```

V1 保持一个 Maven 业务模块，内部按领域分包，不提前拆成大量微模块。

## 4. 单领域结构

```text
entity / mapper / service / dto / vo / query / enums / convert / validator / event
```

Controller 放 API 层，只接收参数、调用 Service 和返回结果。禁止 Controller 直接操作 Mapper 或编排多表事务。

## 5. 文章服务建议拆分

- `ArticleCommandService`
- `ArticleQueryService`
- `ArticleVersionService`
- `ArticlePublishService`
- `ArticlePermissionService`
- `ArticleRenderService`
- `ArticleRepostService`
- `ArticleApplicationService`

## 6. 事件与异步

主事务完成后发布 Spring 应用事件：文章发布、审核通过、评论创建、用户关注、内容收藏等。

通知、统计、缓存和搜索索引失败不应导致主业务回滚。V1 不引入 Kafka/RabbitMQ。

## 7. Redis

Redis 通过 `CacheService`、`LockService`、`RateLimitService` 和 `CounterService` 抽象使用。关闭 Redis 时使用数据库、本机缓存和单机锁降级。

## 8. 暂不启用模块

支付、短信、推送、社交登录、微信、WebSocket、App API 和 UniApp 先关闭入口和依赖，不在项目起步阶段暴力删除源码。