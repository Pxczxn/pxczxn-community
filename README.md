# 星语社区（pxczxn）

星语社区是一个“注册即拥有个人博客”的博客社区，按
`blog-community-engineering-docs-v0.3` 的产品、权限、内容、审核、数据和
非功能规范实现。

## 工程目录

```text
pxczxn-backend   Java 21 / Spring Boot 模块化单体与社区 API
pxczxn-admin     Vue 3 / Vite / Naive UI 社区运营管理端
pxczxn-web       vinext / React 19 博客用户端
database         MySQL 迁移、种子、验证与受保护回滚
docs             决策、交付记录和开发状态
```

## 固定端口

| 服务 | 地址 | 说明 |
| --- | --- | --- |
| 博客端 | `http://localhost:8847` | 用户注册、创作、投稿与公开博客 |
| 管理端 | `http://localhost:8848` | 星语社区运营中心 |
| 后端 | `http://127.0.0.1:8849` | `/api/v1/**` 与 `/admin-api/**` |

博客端私有生产预览：
<https://xingyu-community-pxczxn.pxczxn.chatgpt.site>

线上预览用于查看页面和三色主题；完整注册、创作、审核与发布闭环使用本地
`8847`、`8848`、`8849` 三个端口。

## 本地环境

- Java 21
- Maven 3.9+
- Node.js 22.13+
- MySQL 8
- 数据库：`pxczxn_community`
- 本地数据库账号/密码：`root` / `root`
- Redis 可不启动，社区会话、验证码和配置读取具有本机降级实现

生产部署不要使用本地示例凭据。`application-prod.yml` 要求通过环境变量提供
数据库、Druid 和 Redis 配置，具体变量和最终验收证据见
`docs/delivery/M2-T010-final-acceptance.md`。

## 启动

后端：

```powershell
cd pxczxn-backend
$env:PXCZXN_DB_USERNAME = "root"
$env:PXCZXN_DB_PASSWORD = "root"
& "D:\Coding\software\environment\apache-maven-3.9.9\bin\mvn.cmd" package
java -jar .\pxczxn-starter\target\pxczxn-starter-1.0.0.jar
```

博客端：

```powershell
cd pxczxn-web
npm.cmd install
npm.cmd run dev
```

管理端：

```powershell
cd pxczxn-admin
npm.cmd install
npm.cmd run dev
```

健康检查：

```powershell
Invoke-RestMethod http://127.0.0.1:8849/api/v1/health
```

## 质量门禁

```powershell
cd pxczxn-web
npm.cmd run lint
npm.cmd test

cd ..\pxczxn-admin
npm.cmd run build

cd ..\pxczxn-backend
& "D:\Coding\software\environment\apache-maven-3.9.9\bin\mvn.cmd" verify
```

详细完成度和逐任务证据见 `docs/delivery/development-status.md`，最终验收见
`docs/delivery/M2-T010-final-acceptance.md`。
