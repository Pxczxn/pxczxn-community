# M2-T004 收藏与收藏夹

## 状态

完成。

## 交付范围

- 新用户注册事务直接创建私密的系统默认收藏夹“全部收藏”。
- V007 前已经存在的社区用户在首次使用收藏功能时幂等补建默认收藏夹。
- 支持自定义收藏夹创建、编辑、排序和软删除，最多 50 个。
- 默认收藏夹不可重命名、不可删除；自定义收藏夹删除时只移除映射，不删除仍在
  “全部收藏”中的唯一收藏关系。
- 文章和动态支持收藏，评论与回复明确拒绝收藏。
- 同一用户对同一内容只有一个 `favorite_item`，可通过
  `favorite_folder_item` 同时加入多个收藏夹。
- 首次收藏才增加内容 `favorite_count`；增删收藏夹映射不会重复改变目标计数。
- 收藏关系、收藏夹映射、收藏夹 `item_count` 与目标 `favorite_count` 在同一事务。
- 收藏、更新收藏夹集合、取消收藏均支持重复请求和数据库唯一约束并发保护。
- 收藏夹支持 `PRIVATE`、`PUBLIC`、`FOLLOWERS_ONLY`、`MUTUAL_ONLY`。
- 读取收藏夹内容时再次校验文章、动态、关联文章和转发源可见性，不因收藏扩大范围。
- 内容作者或博客所有者可读取收藏用户的公开资料，但响应不包含收藏夹 ID、名称或结构。
- 所有 BIGINT 业务 ID 继续以字符串返回。

## API

```text
GET    /api/v1/social/me/favorite-folders
POST   /api/v1/social/me/favorite-folders
PATCH  /api/v1/social/me/favorite-folders/{folderId}
DELETE /api/v1/social/me/favorite-folders/{folderId}
GET    /api/v1/users/{userId}/favorite-folders
GET    /api/v1/favorite-folders/{folderId}/items

POST   /api/v1/interactions/{targetType}/{targetId}/favorite
PUT    /api/v1/interactions/{targetType}/{targetId}/favorite
DELETE /api/v1/interactions/{targetType}/{targetId}/favorite
GET    /api/v1/interactions/{targetType}/{targetId}/favorite
GET    /api/v1/interactions/{targetType}/{targetId}/favoritors
```

## 自动化验证

```text
Community business tests: 146 passed
Community web API tests: 28 passed
Full Maven reactor: 26 modules SUCCESS
All backend automated tests: 183 passed
Failures / errors / skipped: 0 / 0 / 0
Favorite folder tests: 6 passed
Favorite relationship tests: 6 passed
Favorite controller tests: 3 passed
```

## 真实运行时闭环

```text
Backend: http://127.0.0.1:8849 / UP
New-user default folders: 1
Default name / visibility: 全部收藏 / PRIVATE
Temporary article publication: PUBLISHED

First favorite:
  target favorite_count = 1
  favorite_item = 1
  folder mappings = 2
  default item_count = 1
  custom item_count = 1
Repeated favorite:
  target favorite_count = 1

Move to default only:
  folder count = 1
Add custom folder again:
  target favorite_count = 1
Public custom folder / item list: 1 / 1
Author-visible favoritors: 1
Private custom folder public list / direct access: 0 / 404
Delete default folder: 400

Unfavorite / repeated unfavorite: 0 / 0
Final target / item / mapping / default count: 0 / 0 / 0 / 0
Custom folder soft deleted: PASS
Temporary users / articles: 0 / 0
Orphan favorite items / mappings: 0 / 0
```

## 主要文件

- `pxczxn-backend/mars-core/pxczxn-biz/src/main/java/top/pxczxn/community/social/application/FavoriteFolderService.java`
- `pxczxn-backend/mars-core/pxczxn-biz/src/main/java/top/pxczxn/community/social/application/FavoriteService.java`
- `pxczxn-backend/mars-core/pxczxn-biz/src/main/java/top/pxczxn/community/social/model/FavoriteFolder.java`
- `pxczxn-backend/mars-api/pxczxn-web-api/src/main/java/top/pxczxn/community/web/social/FavoriteController.java`

## 安全与一致性结论

- 私密收藏夹的名称、结构和内容不会向其他用户或匿名用户返回。
- 作者侧只获得收藏用户资料，不获得用户把内容放入了哪些收藏夹。
- 内容从一个自定义收藏夹移除后仍保留在默认“全部收藏”，除非执行取消收藏。
- 加入多个收藏夹不重复增加目标收藏数；取消收藏一次性清理全部映射并只减一次。
- 取消收藏不产生通知；收藏通知统一在 M2-T007 接入聚合规则。
