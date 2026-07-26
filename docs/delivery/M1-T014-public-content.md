# M1-T014 公开博客与文章交付说明

## API

```http
GET /api/v1/public/blogs/{blogSlug}
GET /api/v1/public/blogs/{blogSlug}/categories
GET /api/v1/public/blogs/{blogSlug}/articles
    ?categorySlug={categorySlug}&pageNum=1&pageSize=10
GET /api/v1/public/articles/{articleId}
GET /api/v1/public/files/{fileId}/content
```

博客详情在原公开资料基础上增加博客类型、三色主题、受控主题配置和 SEO
标题/描述。公开文章数使用实时公开条件统计，不把草稿、私密或不列出文章
计入公开主页。

## 公开详情边界

公开文章详情先经过 M1-T009 的统一 `VIEW_DETAIL` 权限，再且只读取：

```text
article.published_version_id -> article_version
```

响应包含安全 HTML、目录、作者、博客、分类、启用标签、公开时间、互动计数、
canonical 与 SEO 数据，不包含：

```text
markdown_content
rich_text_json
current_version_id
review_version_id
review_status
审核任务与结果
```

公开版本不存在、版本归属错误或安全 HTML 缺失时统一返回 404。当前编辑版本
产生新草稿后，公开详情仍读取旧 `published_version_id`，不会泄露未审核正文。

## 可见性和列表

- `PUBLIC`：可进入详情和博客公开列表。
- `UNLISTED`：允许通过文章 ID 访问详情，但不进入列表、公开文章数和分类筛选。
- `FOLLOWERS_ONLY`：统一权限服务要求有效关注关系；关系模块交付前安全拒绝。
- `PRIVATE`：仅作者或必要博客协作者可读取，匿名访问统一 404。
- 草稿、删除、下架、隐藏博客、不可公开作者与缺失资源统一不泄露存在性。

列表 SQL 同时连接作者状态，按 `published_at DESC, id DESC` 稳定排序，支持
博客内分类 slug 筛选；页码从 1 开始，单页最多 50 条。返回内容只取公开
版本的内容模式、字数和阅读时长，不读取当前草稿。

## 公开文件

公开文件读取不再仅凭“存在引用”放行，而是校验引用目标：

- 博客头像/背景：博客必须为 `ACTIVE`，所有者状态可公开。
- 文章封面：目标文章必须通过 `VIEW_DETAIL`。
- 正文图片：引用版本必须是文章当前 `published_version_id`，且文章可查看。
- 草稿版本图片和普通附件默认不公开。

因此文件 ID 即使被猜中，也不能绕过博客、文章和版本可见性。

## 验证

```text
Community business automated tests: 95 passed
Community web API automated tests: 18 passed
Mars Admin API automated tests: 4 passed
T014-specific automated tests: 15 passed

Public blog theme / SEO / live article count: PASS
Public category filter / stable page: PASS
Public safe HTML / source-field scan: PASS / no leaks
UNLISTED detail / list: 200 / hidden
Draft / private anonymous detail: 404 / 404
Missing category: 404
Canonical and BIGINT string IDs: PASS
New draft while published: old published body remains visible
Temporary verification data: removed, remaining 0

Runtime health: UP
Unauthenticated admin API: 401
captchaEnabled / encryptEnabled / encryptScope:
true / true / global
```
