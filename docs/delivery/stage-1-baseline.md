# 阶段 1：统一版本基线

> 执行日期：2026-07-26  
> 基线标签：`v1.0.0-local-accepted`

## 备份

备份保存在工作区外：

```text
D:\Coding\project\java-code\pxczxn-backups\20260726-stage1-baseline
```

| 文件 | SHA-256 |
| --- | --- |
| `pxczxn-full-before-stage1.tar` | `8535E7724E3D78322D1B9DD9C25111BF8CD7C1FE946F2625A7C1C702C92F4590` |
| `pxczxn-web-history.bundle` | `5F3490157C6C4C3E5D6E538FE715E22B3CF54643E8AE05C439898D0A335C56B0` |
| `mars-system-before-stage1.sql` | `23DEDA3BDB86C37A0D64CDA5C97FDACC65A57969DE699812EF849BA57590EE99` |

博客端 bundle 已通过 `git bundle verify`，包含原 `main`、`sites/main` 和
`HEAD` 的完整历史。旧根目录和博客端 `.git` 元数据也保存在备份目录的
`git-metadata` 子目录中。

## 统一仓库

- 移出不完整的根目录 `.git`。
- 移出 `pxczxn-web/.git`，避免嵌套仓库。
- 在项目根目录初始化 `main` 分支。
- 博客端、管理端、后端、数据库脚本、工程文档和验证脚本统一纳入根仓库。
- `.gitignore` 排除依赖、构建产物、缓存、运行日志、上传目录、本地密钥、
  IDE 和 Codex 本地状态。

## 基线验证

```text
Maven Reactor: 26 modules SUCCESS
Backend tests: 246 passed / 0 failed / 0 skipped
Blog lint: PASS
Blog production build and rendered tests: PASS / 4 passed
Admin production build: PASS
Comments E2E: PASS / cleanup 0
Moments E2E: PASS / cleanup 0
Notifications E2E: PASS / cleanup 0
Admin governance E2E: PASS / cleanup 0
```

治理 E2E 执行期间临时关闭图片验证码，执行完成后已恢复
`captchaEnabled=true`。这只影响隔离的本地验收窗口，不改变基线配置。

## 恢复说明

项目文件可以从完整 tar 归档恢复；数据库可以导入
`mars-system-before-stage1.sql`；原博客端历史可以使用：

```powershell
git clone `
  D:\Coding\project\java-code\pxczxn-backups\20260726-stage1-baseline\pxczxn-web-history.bundle `
  pxczxn-web-history
```
