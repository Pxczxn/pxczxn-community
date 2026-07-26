# M1-T006 分类与平台标签交付说明

## API

```http
GET    /api/v1/blogs/me/categories
POST   /api/v1/blogs/me/categories
PATCH  /api/v1/blogs/me/categories/{categoryId}
DELETE /api/v1/blogs/me/categories/{categoryId}

GET    /api/v1/tags

GET    /admin-api/community/tags
POST   /admin-api/community/tags
PATCH  /admin-api/community/tags/{tagId}
DELETE /admin-api/community/tags/{tagId}
```

博客分类接口只接受独立的 `pxczxn-community-token`。平台标签公开查询只返回
`ACTIVE` 标签；管理接口使用 Mars Admin 的 `Authorization` Token 和 RBAC。

## 博客分类规则

- 分类只能由个人博客所有者管理，博客必须处于 `ACTIVE` 或 `HIDDEN` 状态。
- 每个博客最多 100 个未删除分类。
- 分类 slug 规范化为小写，只允许字母、数字、下划线和连字符。
- 同一博客内 slug 唯一，重复创建或修改返回 409。
- 注册时生成的“未分类”是默认分类，不允许删除。
- 删除普通分类时，同一事务把其中的文章迁移到默认分类，重新计算默认分类
  文章数，再逻辑删除原分类。

## 平台标签规则

- 用户端支持按名称或 slug 搜索，最多返回 100 个启用标签。
- 管理端支持 `ACTIVE`、`HIDDEN` 状态切换和名称、slug、描述维护。
- 删除标签采用 `DELETED` 状态；仍被文章使用时返回 409。
- 文章最多绑定五个不重复的启用标签，关系顺序被显式保存。
- 标签关系替换后重新计算受影响标签的使用次数。

## 管理端权限

V002 数据库迁移注册“社区管理 / 平台标签”菜单和四项权限：

```text
community:tag:list
community:tag:add
community:tag:edit
community:tag:delete
```

`/admin-api/**` 已加入 Sa-Token 登录与注解权限拦截链；跨域配置同时允许
`PATCH`。V002 为管理员角色建立菜单授权，并提供独立校验与回滚脚本。

## 验证

```text
Community business automated tests: 30 passed
Community API automated tests: 7 passed
Category default protection: 409 / PASS
Category create, update and logical delete: PASS
Duplicate category slug: 409 / PASS
Unauthenticated category access: 401 / PASS
Unauthenticated admin tag access: 401 / PASS
Admin tag list/create/update/hide/delete: PASS
Active public tag visibility: PASS
Hidden public tag exclusion: PASS
V002 menu rows: 6 / PASS
V002 distinct permissions: 4 / PASS
Admin role grants: 6 / PASS
Temporary verification data: removed
Login captcha configuration: restored
```
