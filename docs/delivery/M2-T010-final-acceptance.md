# M2-T010 最终自动化与浏览器验收

> 验收日期：2026-07-26  
> 结论：通过

## 1. 验收范围

本次验收覆盖星语社区三个运行端、M1/M2 全部后端能力、数据库 V001-V013、
互动治理 E2E、真实浏览器登录与依赖安全审计。

固定运行地址：

| 服务 | 地址 |
| --- | --- |
| 博客端 | `http://localhost:8847` |
| 管理端 | `http://localhost:8848` |
| 后端 | `http://127.0.0.1:8849` |
| 博客端私有生产预览 | `https://xingyu-community-pxczxn.pxczxn.chatgpt.site` |

## 2. 自动化测试

### 后端

```text
Maven Reactor: 26 modules SUCCESS
Backend automated tests: 246 passed
Business tests: 194
Job tests: 1
pxczxn Admin API tests: 12
Community Web API tests: 39
Failed / errors / skipped: 0 / 0 / 0
Final executable JAR package: SUCCESS
```

覆盖注册、会话、文件、分类、文章版本、权限、审核、发布、定时发布、公开读取、
关注、点赞、收藏、评论、动态、通知和平台治理。

### 博客端

```text
Vinext production build: PASS / Vite 8.1.5
ESLint: PASS
Rendered route and API-wiring tests: 4 passed
Prototype and product routes: 16
```

### 管理端

```text
Vite production build: PASS / Vite 7.3.6
Transformed modules: 4957
AES-GCM LAN fallback: PASS
```

## 3. 真实 E2E

| 脚本 | 关键结果 | 清理 |
| --- | --- | --- |
| `m2-t005-comments.ps1` | 15 条不可变事件；发布/待审/隐藏/删除状态通过 | 0 |
| `m2-t006-moments.ps1` | 10 条动态；公开流索引与三项形状约束通过 | 0 |
| `m2-t007-notifications.ps1` | 9 条通知与收件关系；聚合最大值 3；最终未读 0 | 0 |
| `m2-t009-admin-governance.mjs` | 评论/动态生命周期、批量治理、隐私和 RBAC 通过 | 0 |

M2-T009 进一步验证：

```text
Comment immutable events: 12
Moment immutable events: 7
Operation logs: 56
LIKE / FAVORITE / FOLLOW query: PASS
Read-only role list / mutation: 200 / 403
Favorite folder metadata leak: none
```

## 4. 数据库验收

- V001-V013 已应用到当时的本地过渡库；阶段 5 已迁移为 `pxczxn_community`。
- V012 验证：事件表 1、必需列 9、治理检查约束 2、菜单 18、管理员授权 18。
- V013 验证：站点名称、描述和版权均为星语社区；治理菜单乱码数 0。
- 治理事件动作合法，前后状态相同的无效事件数 0。
- E2E 临时社区用户、博客、后台用户、角色和关键词规则残留数均为 0。

## 5. 真实浏览器验收

管理端：

- 公共配置返回 `captchaEnabled=true`。
- 使用真实图片验证码和管理员账号登录后进入 `/dashboard`。
- 标题、登录页和侧栏均显示“星语社区运营中心”，不存在旧品牌。
- `/community/comments` 可正常进入并显示治理事件区域。
- 评论、动态、LIKE、FAVORITE、FOLLOW 页面及浅色、深色、星空主题已通过。

博客端：

- `/login` 返回“登录 / 注册 · 星语社区”。
- 注册、登录、创作、提交审核、管理端通过、发布和公开阅读闭环已通过。
- 动态、点赞、收藏、评论、关注、粉丝、通知和三色主题均使用真实 API。

## 6. 安全与依赖审计

- 博客端生产依赖：`npm audit --omit=dev` 为 0。
- 管理端全部依赖：`npm audit` 为 0。
- Next.js 升级到 16.2.11，React/RSC 升级到 19.2.8。
- 管理端升级至 Vite 7，并升级 Vue 工具链；Axios、ECharts 和传递依赖已修复。
- 生产配置不再内置数据库、Druid 或 Redis 凭据，统一从环境变量读取。
- 源码扫描未发现生产私钥、固定 Token 或生产数据库明文密码。
- 博客端仍有 13 项仅开发工具链告警，来源为 `eslint-config-next` 与
  `drizzle-kit` 的传递依赖；不进入生产依赖或浏览器包。未使用
  `npm audit fix --force` 进行不兼容降级。
- Maven 已完成运行时依赖树解析；项目当前未集成 OWASP/NVD CVE 数据库扫描，
  上线前应在 CI 中增加 SBOM 和持续 CVE 扫描。

开发环境中的 `admin123` 仅用于本地运营管理端/Druid 演示账号；生产环境必须
通过环境变量和独立凭据替换。

## 7. 性能与可维护性审计

- 博客端五阶段 Vinext 构建稳定通过。
- 管理端存在两个大于 1 MB 的懒加载前端块，Vite 给出分包提示；当前功能和
  首屏验收不受影响，生产公网优化阶段应对图标、ECharts 和后台通用组件做
  `manualChunks` 分包。
- 社区关系以 MySQL 为最终依据；Redis 不可用时会话、验证码和配置具有本机降级。
- 点赞、收藏、关注、审核和通知均有幂等约束、事务计数和可重复 E2E。

## 8. 启动

```powershell
# 后端
cd pxczxn-backend
$env:PXCZXN_DB_USERNAME = "root"
$env:PXCZXN_DB_PASSWORD = "root"
& "D:\Coding\software\environment\apache-maven-3.9.9\bin\mvn.cmd" package
java -jar .\pxczxn-starter\target\pxczxn-starter-1.0.0.jar

# 博客端
cd ..\pxczxn-web
npm.cmd install
npm.cmd run dev

# 管理端
cd ..\pxczxn-admin
npm.cmd install
npm.cmd run dev -- --host 0.0.0.0 --port 8848
```

## 9. 迁移与回滚

1. 备份目标数据库。
2. 按 V001 至 V013 顺序、使用 `utf8mb4` 执行 `database/migrations`。
3. 执行对应 `database/verify`，所有计数满足脚本说明后再启动新版本。
4. 回滚前先停止写流量并备份；仅使用 `database/rollback` 中受保护脚本。
5. 内容、互动和治理事件属于审计数据，不允许在未确认备份时直接删除。

## 10. 最终结论

星语社区 V1（M1 + M2）功能、数据、权限、治理、三端运行和交付文档达到当前
工程文档定义的完成条件。遗留项均属于生产硬化与性能优化，不阻塞本地完整运行
和功能验收。
