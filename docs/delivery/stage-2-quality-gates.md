# 阶段 2：工程质量门禁

完成时间：2026-07-26

## 完成内容

- 管理端新增 `typecheck`、`lint`、`test`、`build` 四个独立脚本。
- 修复管理端全部 `vue-tsc --noEmit` 诊断，保留 `strict`、
  `noUnusedLocals` 和 `noUnusedParameters`。
- 管理端新增端口拓扑、社区治理路由/API、三色主题契约测试。
- 博客端新增独立 `typecheck` 脚本，修复递归富文本解析、可空审核任务和
  Cloudflare Worker 绑定类型。
- 新增数据库迁移版本检查，验证 V001-V013 连续、无重复、迁移与校验脚本
  一一对应，并对实时数据库执行全部校验 SQL。
- 修复 V006 菜单排序校验的时点耦合：保留工作台到文章管理的固定顺序，
  对后续审核、标签页使用相对顺序约束。
- 新增根目录 `scripts/verify.ps1`，统一执行后端、两端、数据库和四条真实
  E2E 门禁。脚本会在本地验证期间临时关闭管理端验证码，并在 `finally`
  中恢复原值；由脚本启动的后端进程也会自动停止。

## 全量验证结果

执行命令：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify.ps1
```

结果：

- Maven Reactor：26 个模块成功。
- Surefire：52 份报告，246 项测试，0 失败，0 错误。
- 博客端：typecheck、lint、4 项测试、build 全部通过。
- 管理端：typecheck、lint、3 项测试、build 全部通过。
- 数据库：13 个连续迁移和 13 个实时校验脚本全部通过。
- 评论 E2E：通过，测试数据清理为 0。
- 动态 E2E：通过，测试数据清理为 0。
- 通知 E2E：通过，测试数据清理为 0。
- 治理 E2E：通过，覆盖管理员认证、评论/动态生命周期、批量治理、
  互动查询、RBAC 和操作事件证据，测试数据清理为 0。
- `captchaEnabled` 已恢复为 `true`。
- 后端验证进程已停止，8849 端口已释放。

## 已记录但不阻断本阶段的告警

- 管理端 ESLint 仍报告 72 个既有告警，主要是脚手架历史 `any` 和四处
  `v-html`。本阶段没有新增或扩大 `any`，相关类型债与输出净化进入后续
  安全、瘦身阶段。
- 管理端构建仍有两个超过 500KB 的 chunk，进入最终技术债清单。
- Maven 测试输出包含 Byte Buddy 动态 agent 的 JDK 未来兼容提示，不影响
  当前测试结果。
