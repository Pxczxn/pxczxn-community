# 阶段 3：安全与生产配置硬化

完成时间：2026-07-26

## 独立提交边界

本阶段严格拆分为以下独立变更，不包含脚手架命名迁移、数据库库名迁移或模块删除：

| 提交 | 内容 |
| --- | --- |
| `9c22ef2` | 新增管理员密码生命周期字段（V014） |
| `f480c9f` | 鉴权、CORS、WebSocket Ticket、密码策略和生产配置硬化 |
| `9d9c729` | Spring Boot 独立升级到 3.5.16 |
| 当前依赖治理提交 | 依赖修复、漏洞扫描、前端审计和 SBOM 门禁 |

## 已修复的高优先级问题

- CORS：
  - 本地环境仅允许博客端 `http://localhost:8847` 和管理端 `http://localhost:8848`。
  - 生产环境从环境变量读取明确的 Origin 白名单。
  - 不再组合任意 Origin 与凭据请求。
- 管理员密码：
  - 删除运行环境中的固定默认密码。
  - 新增管理员时生成随机一次性密码。
  - 一次性密码登录后必须先修改密码，未修改前受保护接口返回 428。
  - `admin/admin123` 仅由 `local` profile 的初始化器提供。
- WebSocket：
  - 长期登录 Token 不再放入 WebSocket URL。
  - 已认证 HTTP 接口签发 30 秒有效、一次性消费的 Ticket。
  - Redis 可用时使用 Redis 原子消费；本地环境可降级为单机内存存储。
  - 生产多实例配置要求 Redis，不允许依赖单机降级。
- 统一鉴权：
  - `/api/v1/**` 由统一社区鉴权入口和公开路由白名单控制。
  - 业务 Service 的资源级权限校验继续保留。
- 生产配置：
  - 应用名统一为 `pxczxn-community`。
  - 生产环境关闭 demo mode、Druid 控制台和不必要的 Actuator 暴露。
  - 数据库、Redis、OSS、密码和密钥均由环境变量注入。
  - 全局上传限制收紧，社区文件仍保持 20 MB 业务上限。
  - 删除旧平台包的调试日志配置。

## 依赖安全门禁

新增 `scripts/verify-dependencies.ps1`，并接入根目录 `scripts/verify.ps1`：

- 后端使用 OWASP Dependency-Check 12.2.2，未抑制漏洞达到 CVSS 7.0 时构建失败。
- 后端使用 CycloneDX Maven Plugin 2.9.2 生成聚合 JSON SBOM。
- 两个前端对生产依赖执行 high 级别门禁，对完整依赖图执行 critical 级别门禁。
- 两个前端分别生成 CycloneDX JSON SBOM。
- 支持通过 `NVD_API_KEY` 环境变量加速 NVD 数据更新。
- Maven 安全扫描使用无凭据的 Maven Central 专用 settings；普通构建仍沿用开发机既有 Maven 配置。

依赖修复包括：

- Spring Boot 3.5.16。
- Tomcat 10.1.57。
- Netty 4.1.136.Final。
- Log4j 2.25.5。
- Okio 3.6.0。
- MyBatis-Plus 3.5.6。
- 移除 Alipay SDK 带入的旧 `dom4j:dom4j:1.6.1`，改用 `org.dom4j:dom4j:2.1.4`。

`CVE-2026-53914` 只对 Kotlin 标准库的精确 package URL 做了抑制。该问题影响 Kotlin 编译器/构建缓存元数据反序列化，当前项目仅通过 OkHttp/Okio 引入运行时标准库，不包含 Kotlin 编译器或构建缓存；JetBrains CNA 评分为 6.7，低于本项目 7.0 门禁。未使用通配 CVE 或文件级宽泛抑制。

## 最终全量验证

执行：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify.ps1
```

结果：

- Maven Reactor：26 个模块成功。
- 后端测试：全部通过。
- 博客端：typecheck、lint、test、build 全部通过。
- 管理端：typecheck、lint、test、build 全部通过。
- 数据库：V001-V014 迁移版本与实时校验全部通过。
- 评论、动态、通知、治理真实 E2E 全部通过，测试数据清理为 0。
- OWASP Dependency-Check：扫描 287 个依赖，16 个依赖含低/中风险记录，未抑制 CVSS 7.0 及以上漏洞为 0。
- 博客端生产依赖漏洞为 0；完整依赖图 13 个，其中 high 9、critical 0。
- 管理端生产依赖漏洞为 0；完整依赖图 5 个，其中 high 5、critical 0。
- 后端及两个前端的 SBOM 均成功生成。
- 验证结束后 `captchaEnabled=true` 已恢复，8849 验证进程已停止。

安全报告和 SBOM 输出到被 Git 忽略的 `outputs/security/`，避免把易变扫描产物提交到版本库。

## 保留的技术债

- 两个前端完整依赖图中的 high 风险均位于 ESLint、minimatch、brace-expansion 等开发工具链；生产依赖为 0，完整依赖图也没有 critical。升级到 ESLint 10 会超出当前 Next/Vue 插件的 peer 范围，应在兼容版本发布后单独治理。
- 管理端仍有 72 条既有 ESLint warning，没有 error；未通过扩大 `any`、关闭 strict 或跳过检查规避。
- 管理端构建存在超过 500 kB 的 chunk，需在后续阶段进行路由级拆包和依赖拆分。
- Mockito 当前通过 Byte Buddy 动态加载 Agent；未来 JDK 默认禁止动态 Agent 前需要切换为显式 `-javaagent`。

## 版本与工具依据

- [Spring Boot 发布记录](https://spring.io/blog/category/releases/)
- [Spring 支持策略](https://spring.io/support-policy)
- [OWASP Dependency-Check Maven Plugin](https://dependency-check.github.io/DependencyCheck/dependency-check-maven/plugin-info.html)
- [CycloneDX Maven Plugin](https://cyclonedx.github.io/cyclonedx-maven-plugin/index.html)
- [Apache Log4j 安全公告](https://logging.apache.org/log4j/2.x/security.html)
- [Apache Tomcat 10 安全公告](https://tomcat.apache.org/security-10.html)
- [Netty 4.1.136.Final](https://github.com/netty/netty/releases/tag/netty-4.1.136.Final)
- [Okio 安全问题说明](https://github.com/square/okio/issues/1323)
