# 阶段 7：最终验收与交付结论

验收日期：2026-07-26
验收版本：`main`，即时聊天提交 `e56db48`

## 1. 架构与模块结构

星语社区当前是单仓库、模块化单体架构：

| 运行端 | 技术栈 | 端口 | 职责 |
| --- | --- | --- | --- |
| `pxczxn-web` | React 19、Vinext、Vite 8 | 8847 | 博客与社区用户端 |
| `pxczxn-admin` | Vue 3、Naive UI、Vite 7 | 8848 | 运营管理与即时聊天 |
| `pxczxn-backend` | Java 21、Spring Boot 3.5.16、MyBatis-Plus | 8849 | 管理 API、社区 API、任务与 WebSocket |
| `pxczxn_community` | MySQL 8 | 3306 | 唯一业务事实来源 |
| Redis | 可选增强 | 6379 | 缓存、分布式一次性 Ticket；不可用时允许单机降级 |

后端保持六个顶层 Reactor 聚合模块：

```text
pxczxn-backend
├─ pxczxn-common
├─ pxczxn-infra
│  ├─ pxczxn-db / pxczxn-redis / pxczxn-oss
│  ├─ pxczxn-websocket / pxczxn-crypto / pxczxn-mail
│  └─ pxczxn-sms / pxczxn-push / pxczxn-pay
│     / pxczxn-wechat / pxczxn-social
├─ pxczxn-core
│  ├─ pxczxn-system / pxczxn-file / pxczxn-gen
│  ├─ pxczxn-message / pxczxn-auth
│  └─ pxczxn-biz
├─ pxczxn-api
│  ├─ pxczxn-admin-api
│  └─ pxczxn-web-api
├─ pxczxn-job
└─ pxczxn-starter
```

迁移前六个顶层模块为 `mars-common`、`mars-infra`、`mars-core`、
`mars-api`、`mars-job`、`mars-starter`。当前可执行产物为
`pxczxn-starter-1.0.0.jar`。

## 2. 完整命名迁移映射

| 迁移前 | 迁移后 |
| --- | --- |
| `mars-common` | `pxczxn-common` |
| `mars-infra` | `pxczxn-infra` |
| `mars-core` | `pxczxn-core` |
| `mars-api` | `pxczxn-api` |
| `mars-job` | `pxczxn-job` |
| `mars-starter` | `pxczxn-starter` |
| `mars-db` | `pxczxn-db` |
| `mars-redis` | `pxczxn-redis` |
| `mars-oss` | `pxczxn-oss` |
| `mars-websocket` | `pxczxn-websocket` |
| `mars-crypto` | `pxczxn-crypto` |
| `mars-mail` | `pxczxn-mail` |
| `mars-sms` | `pxczxn-sms` |
| `mars-push` | `pxczxn-push` |
| `mars-pay` | `pxczxn-pay` |
| `mars-wechat` | `pxczxn-wechat` |
| `mars-social` | `pxczxn-social` |
| `mars-system` | `pxczxn-system` |
| `mars-file` | `pxczxn-file` |
| `mars-gen` | `pxczxn-gen` |
| `mars-message` | `pxczxn-message` |
| `mars-auth` | `pxczxn-auth` |
| `mars-admin-api` | `pxczxn-admin-api` |
| `com.mars.*` | `top.pxczxn.platform.*` |
| `mars.*` 配置前缀 | `pxczxn.*` |
| `mars-user` 浏览器键 | `pxczxn-admin-user` |
| `mars-system` 数据库 | `pxczxn_community` |

社区业务包保持 `top.pxczxn.community.*`，稳定的 `sys_*` 表名和公开 API
路径未修改。对当前 26 个 Reactor 项目、运行配置、管理端和博客端执行品牌扫描，
旧品牌匹配数为 0。脚手架来源只在 `THIRD_PARTY_NOTICES.md` 和交付文档中说明。

## 3. 安全修复结果

- CORS 开发环境仅允许本地 8847、8848；生产环境使用明确白名单环境变量，
  不允许任意 Origin 携带凭据。
- 删除生产固定管理员密码；新账号使用随机一次性密码并强制首次修改。
  `admin/admin123` 仅存在于 local profile。
- WebSocket 使用 30 秒短期、一次性 Ticket；长期会话 Token 不进入 URL。
- `/api/v1/**` 已建立统一社区鉴权入口和公开路由白名单，资源级权限继续保留。
- 管理端聊天统一要求 `sys:chat:list`，群详情、成员、历史、管理操作均校验实际
  群成员和角色。
- 生产数据库、Redis、OSS、密码和密钥全部从环境变量读取；Druid 默认关闭，
  demo mode 关闭，Actuator 暴露面收紧。
- Spring Boot 独立升级至 3.5.16；Maven OWASP 扫描、npm audit 和三份
  CycloneDX SBOM 已进入总门禁。
- 历史集成配置已脱敏；恢复的扩展配置只包含 `enabled=false` 的无密钥占位。

## 4. 脚手架能力：保留与隔离

按修订后的产品决策，本阶段没有删除脚手架模块、前端页面、源码或依赖。

继续保留：

- 用户、组织、RBAC、文件、Redis、Quartz、审计、登录日志和操作日志。
- 支付、微信、短信、第三方登录、邮件、推送、代码生成、SSH 管理。
- student、customer、test 等示例扩展能力。
- 系统通知、服务监控和即时聊天。

默认关闭并由 404 功能边界保护：

- 短信验证码、支付/短信测试、微信、SSH、代码生成、student、customer 和
  test 等未进入社区主业务的端点。
- 对应菜单保留但隐藏、停用；集成配置保留但无密钥、未启用。

已经清理且不恢复：

- 演示业务数据和历史示例记录。
- 旧 Mars 外链菜单。
- 历史第三方密钥、令牌和明文集成配置。

三个未进入 Reactor 的旧目录 `mars-app-api`、`mars-web-api`、`mars-biz`
继续作为只读扩展参考保留，不参与编译、依赖解析、运行、打包或 UI。若未来启用，
必须先完成 pxczxn 命名、依赖升级、安全审查和独立 E2E。

## 5. 即时聊天完整模块

### 私聊

- 用户列表、最近联系人、在线状态、历史分页和分页上限。
- 文本、图片和文件消息；聊天文件前端限制 20 MB。
- 总未读、按联系人未读、已读状态。
- 单方清空会话，不删除或隐藏对方记录。
- 黑名单双向发送约束。

### 群聊

- 创建、列表、详情、资料和公告。
- 成员添加/移除、管理员设置、禁言。
- 群主转让、成员退出、群主解散。
- 文本、图片、文件和系统消息历史。
- 持久化 `last_read_message_id` 群已读游标和群/总未读统计。
- 所有查询和管理动作均执行成员级、角色级资源权限校验。

### 实时与一致性

- HTTP 是唯一业务写入口，先鉴权并写入 MySQL，再投递 WebSocket 事件。
- WebSocket 不接受直接业务消息，只支持心跳和已持久化事件投递。
- 一个用户支持多个浏览器标签或设备会话。
- Redis 不可用时 Ticket 可单机内存降级；生产多实例必须配置 Redis。

## 6. 数据库迁移结果

- 源库通过一致性逻辑备份复制为 `pxczxn_community`，未使用
  `RENAME DATABASE`，旧库继续保留为回退点。
- 原 71 张表的表集合差异和逐表行数差异均为 0；当前目标库为 72 张表，
  额外一张是 `pxczxn_schema_version`。
- `pxczxn_schema_version` 已记录 V001 至 V019 共 19 个成功版本，包含
  version、description、checksum、executed_at 和 success。
- 迁移工具拒绝重复执行成功版本，并在 checksum 变化时失败。
- V019 为聊天增加私聊双方可见状态、群已读游标、查询索引和六个检查约束。
- 迁移前备份保存在
  `D:\Coding\project\java-code\pxczxn-backups\20260726-stage5-database-migration`。

## 7. 最终质量门禁

最终执行 `scripts/verify.ps1`，输出
`ALL VERIFICATION GATES PASSED`：

| 门禁 | 结果 |
| --- | --- |
| Maven 26 项目 Reactor | 通过 |
| 后端 Surefire | 264/264，通过；失败/错误/跳过均为 0 |
| 聊天服务单元测试 | 8/8，通过 |
| 博客端 typecheck / lint / test / build | 全部通过，渲染测试 4/4 |
| 管理端 typecheck / lint / test / build | 全部通过，契约测试 6/6 |
| 数据库迁移与在线校验 | V001-V019，19/19 |
| 安全配置门禁 | 通过 |
| 即时聊天真实 E2E | 通过，临时用户清理为 0 |
| 评论真实 E2E | 通过，临时数据清理为 0 |
| 动态真实 E2E | 通过，临时数据清理为 0 |
| 通知真实 E2E | 通过，临时数据清理为 0 |
| 治理真实 E2E | 通过，临时数据清理为 0 |

聊天 E2E 实际覆盖一次性 Ticket、拒绝 WebSocket 直写、私聊落库推送、已读、
拉黑、单方清空、群越权、群未读游标、禁言、管理员、群主转让、退出和解散。
聊天验收后原社区四组 E2E 全部继续通过，因此没有发现对社区业务语义的破坏。

依赖门禁结果：

- 博客端和管理端生产依赖漏洞均为 0。
- Maven OWASP 报告有 19 个低于 7.0 阈值的 medium 结果，无 high/critical
  阻断项。
- 博客端完整开发依赖有 13 个非 critical 告警，管理端有 5 个非 critical
  告警；均不进入生产浏览器依赖。
- 后端、博客端和管理端 CycloneDX SBOM 均已生成到 `outputs/security`。

验证结束后验证码配置恢复为 `true`，临时验证后端已停止。

## 8. 剩余技术债

1. 三个未进入 Reactor 的旧参考目录仍含历史 Mars 包名；它们被严格隔离，
   未来启用前必须先迁移，不能直接加入 Reactor。
2. 管理端 Lint 当前为 0 error、72 warning，主要是脚手架历史 `any` 和四处
   `v-html` 风险提示；本轮没有新增或扩大 `any`。
3. 管理端仍有约 1.59 MB 和 1.13 MB 的大块构建告警，应拆分图标、图表和
   通用后台组件。
4. OWASP medium 结果和两个前端的开发依赖告警需要持续升级治理，尤其是在启用
   SSH、支付、微信或代码生成等保留扩展前。
5. Mockito 仍使用动态自附加 agent，未来 JDK 禁止动态加载前应改为显式测试
   agent 配置。
6. 聊天文件目前复用统一文件服务；生产环境可继续增加病毒扫描、内容检测和
   对象存储生命周期策略。
7. WebSocket 内存 Ticket 降级只适用于单实例；生产多实例部署必须启用 Redis。

## 9. M3 是否可以开始

**可以开始。**

M1、M2 社区主链路和新的即时聊天模块均通过全量自动化及真实 E2E，安全高优先级
问题、运行时命名和数据库迁移门禁已经完成。M3 应继续遵守以下边界：

- MySQL 保持唯一业务事实来源，Redis 只作可选增强。
- 保留扩展默认关闭，启用任一扩展时单独做安全、依赖和业务隔离验收。
- 每个 M3 功能独立提交，并持续执行聊天和原社区五组真实 E2E。
