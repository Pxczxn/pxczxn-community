# 阶段 6：脚手架瘦身引用审计

审计日期：2026-07-26

## 1. 审计边界

本审计覆盖 Maven Reactor、Java 包引用、Spring MVC/WebSocket 端点、管理端固定
路由与 API、MySQL 动态菜单、配置组和遗留数据表。社区博客端没有引用以下后台
脚手架能力。

保留能力：

- 用户、组织、RBAC 和 Sa-Token 管理员认证。
- 社区独立用户会话和资源级权限检查。
- 文件管理、OSS 抽象和社区文件 20 MB 业务限制。
- Redis 可选增强与 WebSocket Ticket Redis/内存降级。
- Quartz、文章定时发布任务和调度日志。
- 操作日志、登录日志、API 访问日志和审计能力。
- 系统公告与站内通知；仅移除即时聊天子域。
- 邮件、飞书和钉钉通知适配器；移除短信、支付和微信集成。

## 2. Maven 与代码引用结论

| 能力 | 当前引用 | 处置 |
| --- | --- | --- |
| 支付 | `pxczxn-pay` 仅被管理 API 的配置测试端点引用 | 删除端点、配置 UI、依赖和模块 |
| 微信 | `pxczxn-wechat` 仅被小程序登录策略引用 | 删除策略、配置 UI、依赖和模块 |
| 短信 | `pxczxn-sms` 被管理登录、配置测试和系统公告短信通道引用 | 管理登录只保留密码；删除短信通道、端点、依赖和模块 |
| 第三方登录 | `pxczxn-social` 仅被社交登录策略引用 | 删除策略、依赖和模块 |
| 代码生成 | `pxczxn-gen` 仅被 `GenController` 和管理端代码生成页引用 | 删除端点、页面、依赖和模块 |
| 即时聊天 | 位于保留的 `pxczxn-message` 内，与系统公告共享模块 | 只删除聊天实体、Mapper、Service、Controller、WebSocket 分支和页面 |
| SSH 管理 | `SysServer*`、`SshWebSocketHandler` 和服务器管理页闭环自引用 | 删除端点、WebSocket 注册、页面、API 和 SSH 依赖 |
| customer | 系统模块中的四个类型和管理端页面/API；无社区引用 | 删除 |
| student | 只存在于非 Reactor 旧模块和管理端示例页/API | 删除 |
| test/sample | 固定测试页面、`TestTask`、`SampleTask` | 删除，不删除真实测试代码 |

## 3. 非 Reactor 目录

以下三个目录未被任何父 POM 的 `<modules>` 引用，也没有活跃模块依赖其坐标：

- `pxczxn-backend/pxczxn-api/mars-app-api`
- `pxczxn-backend/pxczxn-api/mars-web-api`
- `pxczxn-backend/pxczxn-core/mars-biz`

它们不参与当前 26 模块构建，是阶段 4 明确延期到本阶段处理的旧代码。完成菜单、
页面和活跃依赖清理并验证后直接删除，不再改名。

## 4. 运行时入口

需要移除的管理 API：

- `/api/auth/sms-code`
- `/api/sys/config-group/test-payment`
- `/api/sys/config-group/test-sms`
- `/api/sys/config-group/sms-logs/**`
- `/api/sys/chat/**`
- `/api/chat/group/**`
- `/api/monitor/server-manager/**`
- `/api/tool/gen/**`
- `/api/system/customer/**`

需要移除的 WebSocket/消息入口：

- `/ws/ssh`
- `/ws/message` 中的 `chat` 消息类型

`/ws/message` 的公告与未读通知能力继续保留。

## 5. 管理端页面与路由

固定路由中存在：

- `/message/chat`
- `/monitor/server-manager`
- `/test/test`
- `/tool/gen`
- `/system/customer`

另有未注册固定路由但仍存在的 `system/student` 页面，以及对应
`student.ts`、`customer.ts`、`gen.ts`、`wechat.ts` 和 `server.ts` API。

登录页仍包含短信表单；系统配置页仍包含短信、第三方登录、小程序、公众号和
支付配置。以上 UI 与接口调用全部删除，密码登录和保留配置不受影响。

## 6. 数据库审计

动态菜单发现：

- 即时聊天、服务器管理、测试菜单、开发工具/代码生成、customer。
- 多批重复 student 菜单，其中大部分虽逻辑删除但仍保留授权关系。
- 一个指向旧脚手架站点的顶层外链菜单。

遗留表及审计时近似行数：

- `gen_table` / `gen_table_column`
- `student`
- `customer`
- `sys_chat_message`
- `sys_chat_group*`
- `sys_server`
- `sys_sms_log`

支付、短信、第三方登录、小程序和公众号配置组仍处于启用状态，且存在非空敏感
配置。迁移将先禁用并清空敏感值，再删除 UI/端点；任何原值都不写入仓库或交付
文档。阶段 5 的旧库和一致性备份提供恢复点。

## 7. 分组执行顺序

1. 新增功能开关并默认关闭待删除端点；V015 禁用并脱敏集成配置。
2. V016 删除动态菜单、授权、已确认的示例数据和无用配置组；删除管理端页面。
3. 删除活跃后端控制器、策略、聊天/SSH/customer/sample 代码和无用依赖。
4. 删除支付、微信、短信、社交、代码生成模块和三个非 Reactor 旧目录。
5. 每组执行编译/测试；阶段末执行完整 `scripts/verify.ps1` 和端点不存在检查。

不会通过关闭类型检查、跳过测试或扩大 `any` 完成瘦身。
