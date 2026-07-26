# 09 第一阶段 API 契约

## 1. 通用约定

- 用户端：`/api/v1/**`
- 管理端：`/admin-api/community/**`
- 分页：`pageNum`、`pageSize`，默认 1/20，最大 100
- BIGINT 在 JSON 中使用字符串
- 复用 `pxczxn-admin` 现有统一响应结构

## 2. 认证

```http
GET  /api/v1/auth/check-username
GET  /api/v1/auth/check-email
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/logout
GET  /api/v1/account/me
```

注册事务必须同时创建社区用户、登录账号、偏好、个人博客、博客设置、默认分类并回写个人博客 ID。

## 3. 个人博客

```http
GET   /api/v1/blogs/me
PATCH /api/v1/blogs/me
PATCH /api/v1/blogs/me/settings
GET   /api/v1/public/blogs/{blogSlug}
```

## 4. 分类与标签

```http
GET    /api/v1/blogs/me/categories
POST   /api/v1/blogs/me/categories
PATCH  /api/v1/blogs/me/categories/{categoryId}
DELETE /api/v1/blogs/me/categories/{categoryId}
GET    /api/v1/tags
```

后台标签：

```http
GET    /admin-api/community/tags
POST   /admin-api/community/tags
PATCH  /admin-api/community/tags/{tagId}
DELETE /admin-api/community/tags/{tagId}
```

## 5. 文章草稿与版本

```http
POST   /api/v1/articles
GET    /api/v1/articles/{articleId}/editor
PUT    /api/v1/articles/{articleId}
PUT    /api/v1/articles/{articleId}/autosave
DELETE /api/v1/articles/{articleId}
GET    /api/v1/articles/{articleId}/versions
GET    /api/v1/articles/{articleId}/versions/{versionId}
POST   /api/v1/articles/{articleId}/versions/{versionId}/restore
```

保存文章流程：权限校验 → 解析正文 → 安全渲染 → 生成纯文本/目录/字数/阅读时间/哈希 → 创建版本 → 更新元数据、标签和文件引用。

## 6. 审核与发布

```http
POST /api/v1/articles/{articleId}/submit-review
POST /api/v1/articles/{articleId}/withdraw-review
GET  /api/v1/articles/{articleId}/review-status
POST /api/v1/articles/{articleId}/publish
```

后台审核：

```http
GET  /admin-api/community/reviews
GET  /admin-api/community/reviews/{reviewTaskId}
POST /admin-api/community/reviews/{reviewTaskId}/claim
POST /admin-api/community/reviews/{reviewTaskId}/approve
POST /admin-api/community/reviews/{reviewTaskId}/request-revision
POST /admin-api/community/reviews/{reviewTaskId}/reject
```

## 7. 公开访问

```http
GET /api/v1/public/articles/{articleId}
```

公开响应只返回已发布版本的安全 HTML、目录、作者、博客、分类、标签、时间、计数和 canonical 地址；不返回 Markdown 源、富文本 JSON、审核详情和未发布版本。

## 8. 管理端查询

```http
GET /admin-api/community/users
GET /admin-api/community/blogs
GET /admin-api/community/articles
GET /admin-api/community/articles/{articleId}
```
