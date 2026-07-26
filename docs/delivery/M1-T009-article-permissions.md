# M1-T009 文章权限服务交付说明

## 统一权限入口

文章权限集中在 `ArticlePermissionService`，应用服务和后续控制器只选择动作，
不再自行拼接所有者、角色、状态和可见性条件。

当前动作包括：

```text
VIEW_EDITOR / VIEW_DETAIL / LIST_PUBLIC
EDIT / DELETE / SUBMIT_REVIEW / PUBLISH / PLATFORM_REVIEW
```

拒绝结果统一映射为：

```text
401 UNAUTHENTICATED  未登录或登录主体已经不存在
403 FORBIDDEN        身份存在，但账号、角色或平台权限不允许
404 NOT_FOUND        资源不存在，或公开访问时资源必须保持不可见
409 CONFLICT         身份和角色允许，但文章、博客或审核状态冲突
```

权限拒绝会记录动作、文章 ID、操作者 ID 和拒绝类型，不记录 Token 或正文。

## 社区作者与博客角色

- 个人博客所有者由 `PersonalBlogArticleRoleResolver` 解析为 `OWNER`。
- 文章实际作者可以查看和编辑自己的草稿。
- 团队 `OWNER`、`ADMIN`、`EDITOR`、`AUTHOR` 使用同一
  `BlogArticleRoleResolver` 扩展点；团队模块只需增加解析器，不需要修改文章
  服务。
- 团队 `EDITOR` 可以编辑团队文章，`AUTHOR` 只能管理自己实际创作的文章。
- 删除由实际作者、`OWNER` 或 `ADMIN` 执行。
- 受发布限制的 `LIMITED` 用户仍可保存草稿，但不能提交审核或发布。
- `PENDING_REVIEW` 和 `SCHEDULED` 文章编辑、删除返回 409；平台下架文章返回
  403。
- 冻结博客返回 403，关闭博客返回 409；隐藏博客可以继续维护草稿，但不能
  提交审核或发布。

`ArticleDraftService` 的编辑读取、保存、恢复、版本查询和逻辑删除均已接入
该权限入口。版本的实际操作者记录当前用户，不再固定写文章原作者，为未来
团队编辑保留正确审计信息。

## 公开访问边界

- 没有 `published_version_id` 的文章一律不可公开读取。
- 删除、平台下架以及不满足博客/作者公开状态的文章统一返回 404。
- `PUBLIC` 可进入详情、列表、搜索和后续 SEO 数据源。
- `UNLISTED` 可通过详情地址访问，但不能进入公开列表。
- `PRIVATE` 仅允许作者与必要博客管理角色读取。
- `FOLLOWERS_ONLY` 默认拒绝；关注模块可通过 `BlogFollowerResolver` 接入，
  在关系表尚未交付前不会误放行。
- 已存在公开版本的文章在新版本审核期间继续展示旧公开版本，不会因当前编辑
  流程进入 `PENDING_REVIEW` 而下线。

公开详情服务将在 M1-T014 使用 `VIEW_DETAIL`，公开列表使用
`LIST_PUBLIC`，确保两种可见性语义不会混用。

## 平台管理员边界

- 平台审核使用运营管理端独立会话，权限标识为
  `community:article:review`。
- 管理员未登录返回 401，缺少 RBAC 权限返回 403。
- 只有固定了审核版本，且审核状态为 `QUEUED`、`AUTO_REVIEWING` 或
  `MANUAL_REVIEWING` 时允许审核；首次发布文章通常处于
  `PENDING_REVIEW`，已有公开版本的文章保留原发布状态。
- 社区 Token 不会被当作管理员身份，管理员 Token 也不会被当作社区作者。

## 验证

```text
Community business automated tests: 51 passed
Community API automated tests: 10 passed
T009-specific automated tests: 11 passed
Owner editor access: 200 / PASS
Cross-user editor access: 403 / PASS
Anonymous editor access: 401 / PASS
Pending-review edit: 409 / PASS
Rejected edit created no version and changed no lock: PASS
Public/private/unlisted/follower-only decisions: PASS
Published-version continuity during new review: PASS
Personal and future team-role decisions through one service: PASS
Platform review 401 / 403 / 409 separation: PASS
Temporary verification data: removed
```
