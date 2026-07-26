# 15 文档在代码仓库中的建议位置

建议将本包放入项目根目录：

```text
project-root
├── docs
│   ├── product
│   │   ├── product-scope.md
│   │   ├── domain-and-permissions.md
│   │   ├── content-system.md
│   │   └── social-interaction.md
│   ├── architecture
│   │   ├── data-model.md
│   │   ├── backend-architecture.md
│   │   └── non-functional-requirements.md
│   ├── api
│   │   └── api-contract-m1.md
│   ├── delivery
│   │   ├── m1-task-breakdown.md
│   │   └── state-and-enum-catalog.md
│   ├── governance
│   │   └── moderation-compliance.md
│   ├── decisions
│   │   ├── decision-log.md
│   │   └── open-decisions.md
│   └── prompts
│       └── M1-T001-codex-prompt.md
├── PROJECT_CONTEXT_FOR_AI.md
└── README.md
```

## 维护规则

- 一项产品决策发生变化：更新专题文档和 `decision-log`。
- 数据库迁移完成后：字段以迁移脚本为最终准则，文档同步更新。
- 接口实现变化：先更新 API 契约，再修改前后端。
- 每个 Milestone 建独立任务文件，不把所有任务堆进同一篇文档。
- AI 开发前优先提供 `PROJECT_CONTEXT_FOR_AI.md`、当前任务文档和相关专题文档，避免一次塞入整个仓库上下文。
