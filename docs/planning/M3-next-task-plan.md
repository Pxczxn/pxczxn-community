# M3 下一步任务计划

> 基线：从干净的 `M3-dev`（`bdc301a`）创建新分支。当前根工作区和 `feature/m3-t002-team-application-review` 中的未跟踪 T002 代码只作问题定位参考，不得直接合并。

## 优先级 0：M3-T002-R1 团队申请审核修复

### 目标

完成可合并的“团队申请 → 平台审核 → 原子创建团队博客”闭环。

### 允许范围

- `V024` 迁移、verify、rollback；团队申请服务、mapper、web/admin API、通知、博客端申请页、管理端审核页和测试。
- 仅为实现这些能力所需的既有 blog/team/notification 模块。

### 必须修复

1. 删除或隔离未通过测试的试验代码；以 `M3-dev` 为基线重建任务分支。
2. 服务层不直接依赖 Mockito 环境无法初始化的 `LambdaQueryWrapper/LambdaUpdateWrapper`。为申请查询、状态条件更新、幂等键查询和 slug 检查增加明确 Mapper SQL；返回受影响行数作为并发判定依据。
3. 所有 V024 文件必须为 UTF-8：
   - `database/migrations/V024__m3_team_application_review.sql`
   - `database/verify/V024__verify_team_application_review.sql`
   - `database/rollback/R024__rollback_team_application_review.sql`
4. 管理菜单文案使用“团队申请审核”，不出现乱码、组织、岗位、部门或 OA 语义。
5. 审核通过必须在同一事务创建 TEAM blog、team、OWNER 成员、默认 blog setting/分类和团队审计；任何一步失败均回滚。
6. 审核通过/拒绝后通过事务后事件通知申请人；通知失败不得回滚审核主事务，但必须记录日志。
7. 交付真实页面：
   - 博客端：`/team-applications` 申请表、我的申请状态、撤销、Loading/Empty/Error。
   - 管理端：真实待审列表、详情、通过/拒绝与原因。必须受 `community:team:review` 保护。

### 验收

- `TeamApplicationServiceTest` 和 `TeamApplicationReviewServiceTest` 全部通过。
- 覆盖幂等、重复 PENDING、slug 冲突、撤销、拒绝、并发审批、无权限 403、审批原子创建与通知。
- 后端 Maven、博客端 typecheck/lint/test/build、管理端 typecheck/lint/test/build 均实际执行并通过。
- 在真实 MySQL 执行 V024 和 verify；不通过不能提交。

## 优先级 1：M3-T003 团队成员与邀请

实现邀请、接受/拒绝、退出、移除、角色调整、Owner 转让和解散。

- 邀请补齐幂等键、token 哈希、有效邀请唯一性及过期处理。
- OWNER/ADMIN/EDITOR/AUTHOR 权限按矩阵服务端校验；修复 V023 中 EDITOR/AUTHOR 删除文章权限，`canDeleteArticle` 只能允许 OWNER/ADMIN。
- 复用 `TeamAuthorityService`，所有修改写审计。
- 验收：跨团队越权、角色越权、Owner 转让后的权限回收、解散只读、邀请幂等与并发测试。

## 优先级 2：M3-T004 团队主页与工作台

交付 `/teams`、`/teams/{teamSlug}`、`/workspace/teams/{teamId}`。

- 以 `docs/design/references/pxczxn-web-prototype.png` 为布局参考；仅显示真实 API 数据。
- 公开主页展示团队资料、文章、成员和动态；工作台展示概览、成员、文章、分类和设置。
- AUTHOR 无成员管理/团队设置入口；篡改 teamId 不泄露数据。

## 优先级 3：M3-T005 外部投稿与双层审核

实现不可变的固定文章版本投稿、团队审核、平台审核和最终团队发布。状态迁移必须条件更新、审计、通知与可恢复失败处理。

## 优先级 4：M3-T006 文章共创

实现文章范围协作邀请、接受/拒绝、贡献类型、署名排序与审计。共创默认不授予编辑、发布、删除、审核或团队管理权。

## 优先级 5：M3-T007 社区即时聊天

复用现有短时单次 WebSocket ticket 和治理能力，交付社区用户端聊天页。必须验证 401、403、跨会话、ticket 重用、空/错/加载状态与三主题。

## 优先级 6：M3-T008 首页、发现与文案收口

以首页原型为参考，收口社区导航、发现流、团队/系列入口、通知与聊天入口。用户端强调发现、阅读、创作和互动；管理端强调运营、审核和治理。禁止机械字符串替换或把平台内部组织能力放进核心导航。

## 统一交付规则

每项任务单独 worktree、单独 commit。Claude 完成后必须提供完整 diff、文件清单、API 契约、迁移/verify/rollback、实际命令输出和未完成风险；Reviewer 通过后才进入下一项。
