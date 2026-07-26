# M1-T017 自动化测试与 M1 完成验收

## 状态

完成。

## 后端全量测试

在 Java 21、MySQL 8、Redis 未启动的本地环境执行：

```powershell
cd pxczxn-backend
& "D:\Coding\software\environment\apache-maven-3.9.9\bin\mvn.cmd" verify
```

Maven Reactor 的 26 个模块全部成功。Surefire 报告汇总如下：

| 模块 | 测试类 | 测试 | 失败 | 错误 | 跳过 |
| --- | ---: | ---: | ---: | ---: | ---: |
| `mars-core/pxczxn-biz` | 19 | 107 | 0 | 0 | 0 |
| `mars-api/pxczxn-web-api` | 7 | 19 | 0 | 0 | 0 |
| `mars-api/mars-admin-api` | 2 | 8 | 0 | 0 | 0 |
| `mars-job` | 1 | 1 | 0 | 0 | 0 |
| **合计** | **29** | **135** | **0** | **0** | **0** |

## 需求覆盖矩阵

| 验收域 | 自动化证据 |
| --- | --- |
| 注册 | 用户、邮箱登录账号、偏好、个人博客、设置和默认分类的事务创建与回滚 |
| 会话与隔离 | 社区登录类型和 Token 名独立；管理员 Token 与社区 Token 不能互用 |
| 权限 | 匿名 401、跨用户 403、资源隐藏 404、工作流冲突 409；个人与团队角色矩阵 |
| 文章版本 | 手动/自动保存、去重、恢复、乐观锁冲突、不可变版本和文件引用 |
| 审核 | 自动通过、警告、人工队列、阻断、领取、退修、驳回和幂等提交 |
| 发布 | 手动和立即发布、公开版本原子切换、canonical、重复发布幂等 |
| 定时发布 | 计划、取消、Quartz 领取、二次校验、并发幂等、退避与永久失败 |
| 公开安全 | 私密、草稿、未审核、删除、下架内容隐藏；UNLISTED 不进入公开列表 |
| 通知与审计 | 审核决定通知、固定审核版本和历史任务；权限拒绝记录脱敏安全日志 |

## 数据库结构验证

对本地过渡数据库 `mars-system` 只读执行 V001 至 V006 的全部
`database/verify` 脚本：

```text
V001: 15 core tables / indexes / foreign keys / unique constraints / checks PASS
V002: 6 menus / 4 permissions / admin grants PASS
V003: keyword rule table / 12 columns / 2 indexes PASS
V004: 7 review menus / 7 permissions / 8 admin grants PASS
V005: publish task table / 11 required columns / Quartz job PASS
V006: 9 menus / 5 permissions / 10 admin grants / page order / brand PASS
```

## 前端与运行时门禁

```text
pxczxn-web production build: PASS
pxczxn-web rendered route tests: 3 passed
pxczxn-web ESLint: PASS
pxczxn-admin production build: PASS / 4932 modules
backend health: http://127.0.0.1:8849/api/v1/health -> UP
Redis: intentionally unavailable
```

浏览器已完成最终端口上的真实注册、文章编辑、审核、发布、公开博客与公开文章闭环；
验证数据已清理。M1 完成定义中的浏览器闭环、权限隔离、无 Redis 运行、非公开内容
防泄漏、核心测试和关键审计记录均有对应证据。
