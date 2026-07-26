# 博客社区工程文档包 v0.3

> 状态：V1（M1 + M2）与即时聊天已完成；M2.5 已规划待开发<br>
> 整理日期：2026-07-27<br>
> 工程项目代号：`pxczxn-community`

## 1. 项目定义

这是一个以“注册即拥有个人博客”为核心的博客社区平台。

平台不是单纯的个人博客系统，也不是短视频式内容流。它由四部分组成：

1. **个人博客**：用户唯一公开主页，注册后自动创建。
2. **团队博客**：通过申请创建，支持成员、角色、投稿和内部审核。
3. **内容社区**：连接文章、动态、系列、标签、关注、评论、收藏和转发。
4. **平台后台**：由 `pxczxn-admin` 负责审核、举报、运营、风控和系统管理。

## 2. 统一产品原则

- 能借鉴抖音成熟交互逻辑的功能，优先参考抖音并做博客场景适配。
- 内容组织参考掘金、知乎、Medium、GitHub、Notion 等成熟产品。
- 不为差异化重复造轮子，坚持“取其精华、去其糟粕、集百家之长”。
- 用户内容默认属于实际作者，平台只获得必要的展示、传播和治理授权。
- V1 不收费、不做收益分成、不做广告和支付系统。
- 不允许成人、违法及其他高风险内容。
- 正式公开运营前，协议、隐私与身份认证方案必须按实际运营地区和现行规则复核。

## 3. 文档索引

| 文件 | 内容 |
|---|---|
| `docs/00-technology-stack.md` | 技术栈、工程命名、Redis 与 SQL 管理规则 |
| `docs/01-product-scope.md` | 产品定位、V1 范围、阶段规划 |
| `docs/02-domain-and-permissions.md` | 用户、博客、团队、角色与内容归属 |
| `docs/03-content-system.md` | 文章、编辑器、分类、标签、系列、共创与转载 |
| `docs/04-social-interaction.md` | 动态、关注、评论、点赞、收藏、分享与通知 |
| `docs/05-moderation-compliance.md` | 审核、举报、处罚、隐私与合规边界 |
| `docs/06-information-architecture.md` | 前台页面、后台菜单与信息架构 |
| `docs/07-data-model.md` | 数据库领域、核心表与关系 |
| `docs/08-backend-architecture.md` | pxczxn-admin 模块边界和代码目录 |
| `docs/09-api-contract-m1.md` | 第一阶段接口契约 |
| `docs/10-m1-task-breakdown.md` | M1 任务编号、依赖和验收标准 |
| `docs/11-state-and-enum-catalog.md` | 状态机与枚举总表 |
| `docs/12-non-functional-requirements.md` | 安全、性能、可用性和工程约束 |
| `docs/13-open-decisions.md` | 尚未决定或上线前必须复核的事项 |
| `docs/14-decision-log.md` | 已定关键决策记录 |
| `docs/15-repository-document-placement.md` | 文档放入代码仓库的建议结构 |
| `PROJECT_CONTEXT_FOR_AI.md` | 可直接交给 AI/Codex 的稳定上下文 |
| `ALL_IN_ONE.md` | 全部专题文档的完整合并版 |
| `prompts/M1-T001-codex-prompt.md` | 第一个 Codex 开发任务提示词 |
| `ENGINEERING_DOCUMENTATION.md` | 上述文档的合并版摘要 |
| `../docs/planning/M2.5-M6-delivery-plan.md` | 当前正式产品演进计划与共同门禁 |
| `../docs/delivery/M2.5-M6-task-breakdown.md` | M2.5 至 M6 可执行任务、依赖与验收标准 |
| `../docs/delivery/development-status.md` | 当前工程交付状态和阶段证据 |

## 4. 文档使用规则

- 产品规则发生变化时，先修改对应专题文档，再更新 `14-decision-log.md`。
- 技术栈与工程基线以 `docs/00-technology-stack.md` 为准。
- 数据库字段以 `07-data-model.md` 为设计源，最终以 `database/migrations` 中的版本化 SQL 为准。
- 接口以 `09-api-contract-m1.md` 为第一阶段契约源。
- 当前开发顺序以 `../docs/delivery/M2.5-M6-task-breakdown.md` 为准；历史 M1 任务拆分保留为 V1 设计记录。
- 法律与合规内容属于产品设计假设，不替代正式法律意见。

## 5. 当前开发起点

当前 V1 已完成，下一项实际开发任务：

```text
M2.5-T001：平台首页与发现页
```

M2.5 阶段目标：

```text
首次进入博客端
→ 真实 /discover 内容流
→ 清晰的平台导航与创作入口
→ 参数化内容、团队与协作路由
→ 管理端社区运营中心
```
