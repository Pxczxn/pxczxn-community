# M1-T001 Codex 开发提示词

你正在维护 `pxczxn-community` 博客社区项目，后台工程为 `pxczxn-admin`。本任务只完成“社区业务模块初始化”，不要提前开发注册、文章、数据库业务表或前端页面。

## 目标

1. 阅读现有 Maven 模块和启动配置。
2. 确认或创建 `pxczxn-biz` 博客社区业务入口。
3. 建立社区业务基础包结构。
4. 在 `pxczxn-web-api` 增加最小社区 API 探活接口。
5. 为社区用户预留独立 Sa-Token 登录工具和 Token 名称。
6. 保证 pxczxn 管理端原登录和启动行为不受影响。

## 建议包结构

```text
top.pxczxn.community
├── user
├── blog
├── article
├── taxonomy
├── moderation
├── notification
└── shared
```

## 接口

增加一个无需社区登录的健康检查接口，例如：

```http
GET /api/v1/health
```

返回当前服务状态、模块名称和版本，不返回敏感配置。

## 账号隔离

- 管理员继续使用现有管理员 Token。
- 社区用户 Token 名称使用 `community-token`。
- 本任务不实现真实社区登录，只建立隔离工具或配置骨架。
- 管理员 Token 不得被社区登录工具识别为社区用户会话。

## 禁止事项

- 不删除 `pxczxn-admin` 现有基础模块。
- 不修改管理员用户表和登录流程。
- 不创建社区业务数据库表。
- 不引入微服务、消息队列或新 ORM。
- 不把社区用户塞入 `sys_user`。
- 不为了编译通过注释原有代码。
- 不实现文章、博客、评论等 CRUD。

## 验证

完成后必须：

1. 执行 Maven 编译或测试。
2. 启动后端。
3. 验证 pxczxn 管理端登录接口仍可用。
4. 调用 `/api/v1/health` 验证社区 API。
5. 检查管理员 Token 与社区 Token 配置互不覆盖。

## 完成报告

按以下格式报告：

```text
1. 修改摘要
2. 新增/修改文件
3. 模块依赖变化
4. 执行的命令
5. 编译与启动结果
6. 接口验证结果
7. 未完成项或风险
```

## 固定技术与命名约束

- 使用 Java 21。
- Java 根包使用 `top.pxczxn.community`。
- 新增模块和配置统一使用 `pxczxn` 命名；pxczxn-admin 只作为脚手架来源。
- Redis 允许接入但不是核心强依赖。
- 数据库迁移使用版本化 SQL，不引入 Flyway 或 Liquibase。
