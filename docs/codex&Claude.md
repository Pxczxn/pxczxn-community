最稳的做法不是让两个模型同时改同一份代码，而是：

> **Codex 当技术负责人和验收员，Claude Code 在独立 worktree 里开发，Codex 再读取 diff、跑测试、开浏览器验收，失败就把日志重新派给 Claude，最后由 Codex 收口。**

Codex 本身支持用独立 worktree 隔离多个代理任务；Claude Code 可以通过 CLI 非交互运行，并用 JSON 输出结果，Hooks 还能在编辑或任务结束时自动执行格式化、测试等动作。[OpenAI](https://openai.com/index/introducing-the-codex-app/?utm_source=chatgpt.com)

# 一、先准备目录

在项目根目录增加：

```
pxczxn/
├─ AGENTS.md
├─ CLAUDE.md
├─ outputs/
│  ├─ agent-tasks/
│  └─ agent-results/
└─ scripts/
   ├─ run-claude-task.ps1
   └─ verify.ps1
```

## `AGENTS.md`

这是给 Codex 看的总指挥规则：

```
# Codex 总指挥规则

你是 pxczxn 星语社区的技术负责人和最终质量负责人。

Claude Code 是开发执行代理。你负责：

1. 阅读路线图、工程文档和当前代码。
2. 每次只拆分一个可独立验收的任务。
3. 为任务创建独立 Git 分支和 worktree。
4. 在 worktree 内调用 Claude Code 开发。
5. 审查 Claude 产生的所有 diff。
6. 执行类型检查、Lint、测试、构建、数据库验证和浏览器 E2E。
7. 发现问题后：
   - 明确问题原因；
   - 把失败日志和限定修改范围交给 Claude 修复；
   - 或由你直接修复小问题。
8. 修复后必须复测。
9. 所有门禁通过后才能提交和合并。

禁止：

- Codex 和 Claude 同时修改同一个工作区。
- Claude 直接修改 main。
- 跳过测试或关闭类型检查。
- 使用 any、忽略错误或删除测试来让门禁通过。
- 在提示词中发送密钥、Token、数据库密码或真实用户隐私数据。
- 擅自增加微服务、MQ、Elasticsearch、Flyway 或 Liquibase。
```

## `CLAUDE.md`

这是 Claude 的开发边界：

```
# Claude 开发规则

你是 pxczxn 星语社区的开发执行代理。

## 项目定位

星语社区是博客社区，不是企业 OA。

- 用户端强调发现、阅读、创作和互动。
- 管理端强调运营、审核和治理。
- MySQL 是唯一业务事实来源。
- Redis 只作可选增强。
- 团队业务角色不得复用平台 sys_role。

## 开发要求

1. 只完成当前任务文件规定的内容。
2. 只修改允许范围内的模块。
3. 不擅自重构无关代码。
4. 开始前先阅读现有同类实现。
5. 遵守现有包结构、命名、异常和响应规范。
6. 数据库迁移必须可重复执行。
7. 写操作必须考虑：
   - 权限
   - 幂等
   - 乐观锁
   - 审计
   - 事务
8. 必须补充必要测试。
9. 不得通过扩大 any、关闭检查或删除测试规避问题。

完成后输出：

- 实现摘要
- 修改文件清单
- 数据库变化
- API 变化
- 执行过的测试
- 尚存风险
```

# 二、用 PowerShell 调 Claude

创建 `scripts/run-claude-task.ps1`：

```
param(
    [Parameter(Mandatory = $true)]
    [string]$Worktree,

    [Parameter(Mandatory = $true)]
    [string]$TaskFile,

    [Parameter(Mandatory = $false)]
    [string]$ResultFile = "outputs/agent-results/latest.json"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $Worktree)) {
    throw "Worktree 不存在：$Worktree"
}

$absoluteTask = Resolve-Path $TaskFile
$resultParent = Split-Path $ResultFile -Parent

if ($resultParent -and -not (Test-Path $resultParent)) {
    New-Item -ItemType Directory -Path $resultParent -Force | Out-Null
}

Push-Location $Worktree

try {
    Write-Host "开始调用 Claude Code..."
    Write-Host "工作区：$Worktree"
    Write-Host "任务文件：$absoluteTask"

    $prompt = @"
请先阅读项目根目录 CLAUDE.md。

然后阅读任务文件：
@$absoluteTask

严格按照任务文件开发。

完成后：
1. 检查 git diff；
2. 执行任务要求的测试；
3. 输出修改文件、实现结果、测试结果和风险；
4. 不要提交 Git，由 Codex 负责 Review 和提交。
"@

    claude -p $prompt --output-format json |
        Tee-Object -FilePath $ResultFile

    if ($LASTEXITCODE -ne 0) {
        throw "Claude Code 执行失败，退出码：$LASTEXITCODE"
    }

    Write-Host "Claude Code 任务完成。"
}
finally {
    Pop-Location
}
```

Claude Code 官方支持通过 `-p` 非交互调用，并可用 `--output-format json` 供外部程序读取结果，因此很适合被 Codex 或脚本当成执行代理。[Claude Platform Docs](https://docs.anthropic.com/en/docs/claude-code/cli-reference?utm_source=chatgpt.com)

# 三、让 Codex 按固定流水线执行

直接把下面这段交给 Codex：

```
# 目标：自动编排 Claude Code 开发星语社区

你是总指挥，不要直接连续开发全部业务。

## 工作流程

对每一个任务严格执行：

### 1. 分析

- 阅读开发状态和交付规划。
- 选择当前最优先、可独立验收的一个任务。
- 明确业务规则、数据库变化、接口和验收标准。

### 2. 创建任务文件

在：

outputs/agent-tasks/

生成任务文档，例如：

M3-T001-team-application.md

任务文档必须包含：

- 背景
- 目标
- 允许修改范围
- 禁止修改范围
- 数据模型
- API 契约
- 权限规则
- UI 要求
- 测试要求
- 完成定义

### 3. 创建隔离工作区

为任务创建独立分支和 worktree，例如：

git worktree add ../pxczxn-m3-t001 -b feature/m3-t001-team-application

Claude 只能在该 worktree 中工作。

### 4. 调用 Claude Code

调用：

powershell -ExecutionPolicy Bypass -File scripts/run-claude-task.ps1 `
  -Worktree "../pxczxn-m3-t001" `
  -TaskFile "outputs/agent-tasks/M3-T001-team-application.md" `
  -ResultFile "outputs/agent-results/M3-T001-claude.json"

模型使用 Opus 4.8 Max。

### 5. Review

Claude 完成后，你必须：

- 查看 git status；
- 查看完整 git diff；
- 检查是否超出任务范围；
- 检查产品语义；
- 检查数据库和 API 兼容性；
- 检查权限、幂等、事务和越权风险；
- 检查是否产生重复代码和无关重构。

### 6. 自动验证

根据修改范围执行：

- Maven 测试
- 博客端 typecheck / lint / test / build
- 管理端 typecheck / lint / test / build
- 数据库迁移和 checksum 验证
- scripts/verify.ps1
- 当前新增业务 E2E
- 聊天、评论、动态、通知、治理回归 E2E

### 7. 浏览器验收

启动真实三端：

- 博客端 8847
- 管理端 8848
- 后端 8849

使用内置浏览器模拟真实用户和管理员完成业务流程。

重点验证：

- 401 未登录
- 403 无权限
- 404 不泄露资源
- 409 状态或乐观锁冲突
- 跨用户和跨团队越权
- Loading、Empty、Error 状态
- 浅色、深色、星空主题

### 8. 修复循环

发现问题后：

1. 创建聚焦修复文件：
   outputs/agent-tasks/M3-T001-fix-01.md
2. 写明：
   - 实际现象
   - 复现步骤
   - 错误日志
   - 失败测试
   - 允许修改范围
   - 预期结果
3. 再次调用 Claude Code。
4. 重新 Review。
5. 重新运行失败测试和全部相关回归。

最多允许 Claude 修复两轮。

两轮仍失败时：

- 由你亲自分析和修复；
- 或建议升级到 Fable 5 Ultra 专项处理；
- 不得无限循环消耗额度。

### 9. 提交

全部门禁通过后：

- 创建独立 Git commit；
- 更新开发状态和验收文档；
- 合并回 main；
- 删除 worktree；
- 输出最终交付报告。

当前从 M3-T001 开始。
```

Codex App 的 worktree 能让不同代理在同一仓库的隔离副本中工作，避免 Claude 的开发过程污染稳定分支，也方便 Codex独立 Review 和回滚。[OpenAI](https://openai.com/index/introducing-the-codex-app/?utm_source=chatgpt.com)

# 四、自动测试脚本

你的项目已经有 `scripts/verify.ps1`，让 Codex继续复用。可以再增加一个较轻的 Claude 本地门禁：

```
param(
    [ValidateSet("backend", "web", "admin", "all")]
    [string]$Scope = "all"
)

$ErrorActionPreference = "Stop"

if ($Scope -in @("backend", "all")) {
    Push-Location "pxczxn-backend"
    try {
        ./mvnw test
        if ($LASTEXITCODE -ne 0) {
            throw "后端测试失败"
        }
    }
    finally {
        Pop-Location
    }
}

if ($Scope -in @("web", "all")) {
    Push-Location "pxczxn-web"
    try {
        pnpm typecheck
        pnpm lint
        pnpm test
        pnpm build
    }
    finally {
        Pop-Location
    }
}

if ($Scope -in @("admin", "all")) {
    Push-Location "pxczxn-admin"
    try {
        pnpm typecheck
        pnpm lint
        pnpm test
        pnpm build
    }
    finally {
        Pop-Location
    }
}

Write-Host "基础质量门禁全部通过。"
```

Claude Code Hooks 也可以在文件修改后或任务结束时自动执行命令，例如格式化、测试和规则校验；不过你这里更建议让 Claude 先跑轻量门禁，完整 E2E 留给 Codex，避免两边重复烧大量额度。[Claude Platform Docs](https://docs.anthropic.com/en/docs/claude-code/hooks?utm_source=chatgpt.com)

# 五、实际分工

```
Codex
├─ 阅读路线图
├─ 设计任务
├─ 创建 worktree
├─ 调用 Claude
├─ Review diff
├─ 运行全量门禁
├─ 浏览器 E2E
├─ 修复或重新派单
└─ 提交与合并

Claude Opus 4.8 Max
├─ 阅读单个任务
├─ 实现代码
├─ 补测试
├─ 执行局部验证
└─ 输出交付说明

Fable 5 Ultra
└─ 只处理 Opus 连续失败的疑难问题
```

最关键的三条纪律：

1. **Claude 不碰 main。**
2. **Codex 和 Claude 不同时写同一个 worktree。**
3. **Claude 说完成不算完成，Codex 浏览器复测通过才算。**

这样才是真正的联合自动化开发，不是 Codex 把一句话转发给 Claude，然后两个模型一起把仓库搅成蛋炒饭 😂