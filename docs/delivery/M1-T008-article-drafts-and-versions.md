# M1-T008 文章草稿与版本交付说明

## API

```http
POST   /api/v1/articles
GET    /api/v1/articles/{articleId}/editor
PUT    /api/v1/articles/{articleId}
PUT    /api/v1/articles/{articleId}/autosave
DELETE /api/v1/articles/{articleId}?expectedLockVersion={lockVersion}

GET    /api/v1/articles/{articleId}/versions
GET    /api/v1/articles/{articleId}/versions/{versionId}
POST   /api/v1/articles/{articleId}/versions/{versionId}/restore
```

所有接口只接受独立的 `pxczxn-community-token`。文章、版本、博客和分类均在
服务端重新校验所有权，不信任请求中的资源 ID。

## 双编辑器与安全渲染

- 每个版本只有一个主内容源：`RICH_TEXT` 保存 Tiptap/ProseMirror JSON，
  `MARKDOWN` 保存 Markdown 文本。
- 模式切换必须提交目标模式的完整源，禁止同一版本同时提交两种源。
- Markdown 使用 CommonMark 与 GFM 表格、删除线、自动链接扩展解析。
- 富文本只解释明确支持的文档节点和 marks；未知节点返回 400，防止静默丢失
  内容。
- 服务端统一执行 HTML 白名单清洗，移除脚本、事件属性、危险 URL 和不受支持
  的标签。
- 服务端生成标题锚点、目录 JSON、纯文本、内容哈希、字数和预计阅读时长。
- 正文主源最多 100 万字符；富文本最多 64 层、20,000 个节点。

## 版本与并发

- 创建文章产生版本 1，并将 `current_version_id` 指向该版本。
- 显式保存始终产生不可变新版本。
- 自动保存仅在正文、元数据、标签或文件引用实际变化时产生新版本；相同内容
  重复自动保存保持版本号与锁版本不变。
- 恢复历史版本不会修改旧版本，而是复制出 `RESTORE` 类型的新版本。
- 更新、恢复和逻辑删除必须提交 `expectedLockVersion`。
- 文章更新使用 `WHERE lock_version = ?` 并原子递增；冲突返回 409，事务回滚
  已插入的候选版本。
- 已发布文章后续编辑保留旧 `published_version_id`，只移动当前编辑版本。

## 元数据与文件引用

- 分类必须属于当前博客；未指定分类时使用默认“未分类”。
- 文章 slug 在同一博客内唯一，未提供时使用文章 ID 生成稳定地址。
- 标签复用 M1-T006 的最多五个启用标签规则。
- 封面文件引用文章，正文文件引用固定文章版本。
- 新版本未显式提交正文文件列表时复制当前版本引用。
- 每个内容版本最多引用 100 个本人活动文件。
- 草稿正文文件不进入公开文件读取范围。

## 逻辑删除

删除把文章状态改为 `DELETED`，记录 `deleted_at` 并递增锁版本。正文版本与
文件引用保留在回收链路中，不执行物理级联删除；作者普通编辑接口不再返回该
文章。

## 验证

```text
Community business automated tests: 40 passed
Community API automated tests: 10 passed
T008-specific automated tests: 13 passed
Markdown create and safe derived HTML/TOC/hash: PASS
Dangerous script and javascript URL in rendered HTML: absent
Unchanged autosave: no extra version / lock unchanged
Manual save: version 2 / lock 1
Stale write: 409 / candidate version rolled back
Rich-text mode switch: version 3 / lock 2
Version list and owner-only source detail: PASS
Restore version 1: new version 4 / lock 3
Cross-user editor access: rejected
Stale delete: 409
Logical delete: DELETED / versions preserved
Owned content file reference: version 1 linked
Next version implicit file-reference copy: linked
Draft content file public read: 404
Temporary database, storage and file objects: removed
```
