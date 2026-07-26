# 阶段 4：pxczxn 命名迁移验收

验收日期：2026-07-26

## 1. 提交边界

本阶段只处理当前 Reactor 模块、真实运行代码、构建产物、配置路径、浏览器
存储键、运行时 UI 和工程文档中的命名。未混入 Spring Boot 升级、数据库库名
迁移或脚手架模块删除：

- Spring Boot 3.5.16 升级已在独立提交 `9d9c729` 完成。
- 数据库仍临时使用旧库名，留待阶段 5 以新建、复制、切换和验证方式迁移。
- 三个未进入 Reactor 的旧目录保留到阶段 6，先完成真实引用审计再删除。
- `sys_*` 表名和稳定公开 API 路径保持不变。

## 2. 模块映射

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
| `mars-sms` | `pxczxn-sms` |
| `mars-push` | `pxczxn-push` |
| `mars-pay` | `pxczxn-pay` |
| `mars-wechat` | `pxczxn-wechat` |
| `mars-social` | `pxczxn-social` |
| `mars-crypto` | `pxczxn-crypto` |
| `mars-mail` | `pxczxn-mail` |
| `mars-system`（模块） | `pxczxn-system` |
| `mars-file` | `pxczxn-file` |
| `mars-gen` | `pxczxn-gen` |
| `mars-message` | `pxczxn-message` |
| `mars-auth` | `pxczxn-auth` |
| `mars-admin-api` | `pxczxn-admin-api` |

社区业务模块继续使用 `pxczxn-biz` 和 `pxczxn-web-api`。

## 3. 包名、配置与运行时品牌

- 平台 Java 包由 `com.mars.*` 迁移为 `top.pxczxn.platform.*`。
- 社区业务包保持 `top.pxczxn.community.*`。
- 活跃源码中 `com.mars` 包声明计数为 0。
- 配置前缀统一为 `pxczxn.*`。
- 管理员浏览器持久化键改为 `pxczxn-admin-user`。
- 管理端首页、启动日志和默认站点名称统一为“星语社区”。
- 管理端构建输出目录改为
  `pxczxn-backend/pxczxn-starter/src/main/resources/static`。
- 后端构建产物为 `pxczxn-starter-1.0.0.jar`，未生成旧名称 JAR。
- 脚手架来源和原始 MIT 版权统一保留在根目录
  `THIRD_PARTY_NOTICES.md`。

## 4. 分组提交

| 提交 | 内容 |
| --- | --- |
| `ddad264` | 顶层后端模块迁移 |
| `8d8e818` | 基础设施模块迁移 |
| `ee522d0` | Core 与管理 API 模块迁移 |
| `12339c7` | 活跃平台 Java 包迁移 |
| `c2b51aa` | 运行时品牌、路径、存储键和第三方声明迁移 |
| `03bc8a1` | 交付文档路径与产品命名同步 |

每组代码迁移后均执行编译或测试，未采用一次性全局替换后集中排错。

## 5. 阶段全量验证

`scripts/verify.ps1` 最终结果为 `ALL VERIFICATION GATES PASSED`：

- 后端 26 个 Reactor 项目 Maven `verify` 通过。
- 博客端 typecheck、Lint、测试和构建通过。
- 管理端 typecheck、Lint、测试和构建通过；Lint 为 0 error、72 warning。
- Maven/前端依赖漏洞门禁和三份 CycloneDX SBOM 输出通过。
- V001 至 V014 共 14 个迁移及 14 个在线校验脚本通过。
- CORS、WebSocket Ticket、管理员随机一次性密码、生产配置和统一社区鉴权检查通过。
- 评论、动态、通知、治理真实 E2E 全部通过。
- 验证结束后验证码配置已恢复，临时后端进程已停止。

## 6. 明确延期项

- 数据库旧库名及脚本默认值：阶段 5 迁移为 `pxczxn_community`。
- 未进入 Reactor 的 `mars-app-api`、`mars-web-api` 和 `mars-biz` 目录：
  阶段 6 完成引用审计和门禁验证后删除，不在命名阶段直接改名。
- 已执行的历史 SQL 迁移保持不可变；其中的历史兼容数据不在本阶段改写。

## 7. 阶段结论

当前活跃模块、Java 包、配置前缀、JAR、运行时 UI、日志和浏览器存储键已经完成
pxczxn 统一命名。阶段 4 验收通过，可以进入阶段 5 的数据库名称与迁移记录治理。
