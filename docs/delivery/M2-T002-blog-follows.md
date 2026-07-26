# M2-T002 博客关注

## 状态

完成。

## 交付范围

- 新增博客关注、取消关注、更新关注设置和查询当前关系 API。
- 支持 `ALL`、`IMPORTANT`、`MUTED` 三种通知等级。
- 个人博客支持特别关注；团队博客拒绝特别关注设置。
- 禁止关注自己的个人博客，并隐藏已删除、停用博客或不可用用户。
- 重复关注、重复取消均按幂等结果处理；并发唯一键冲突会读取最终关系。
- 关注关系写入与博客 `follower_count` 原子增减处于同一事务，递减不低于零。
- 新增当前用户的关注数、粉丝数、互关数以及关注、粉丝分页列表。
- 列表响应包含用户资料、个人博客资料、是否关注、是否被关注、是否互关、
  特别关注和通知等级。
- 所有 BIGINT 业务 ID 继续以字符串返回，社区 Token 与管理端 Token 保持隔离。
- 关注关系已接入 `FOLLOWERS_ONLY` 文章权限扩展点；团队博客不产生个人式互关语义。
- 关注通知的实际生成统一留在 M2-T007，避免在通知聚合规则确定前重复实现。

## API

```text
POST   /api/v1/blogs/{blogId}/follow
PATCH  /api/v1/blogs/{blogId}/follow
DELETE /api/v1/blogs/{blogId}/follow
GET    /api/v1/blogs/{blogId}/follow
GET    /api/v1/social/me/counts
GET    /api/v1/social/me/following
GET    /api/v1/social/me/followers
```

## 验证结果

```text
BlogFollowService tests: 7 passed
Follow permission resolver tests: 2 passed
Blog follow controller tests: 2 passed
Full Maven test reactor: 26 modules SUCCESS
Backend automated tests after T003 regression: 168 passed / 0 failed / 0 errors / 0 skipped
Real user A -> B follow: IMPORTANT / special=true
Real user B -> A follow: mutual=true
A social counts: following=1 / followers=1 / mutual=1
Following and follower pages: 1 record each
Update settings: MUTED / special=false
Unfollow: following=false / target followerCount=0
Temporary users: removed, remaining 0
Orphan follow rows: 0
Runtime backend port: 8849
```

## 主要文件

- `pxczxn-backend/mars-core/pxczxn-biz/src/main/java/top/pxczxn/community/social/`
- `pxczxn-backend/mars-api/pxczxn-web-api/src/main/java/top/pxczxn/community/web/social/`
- `pxczxn-backend/mars-core/pxczxn-biz/src/test/java/top/pxczxn/community/social/application/BlogFollowServiceTest.java`

## 安全与一致性结论

- 匿名用户只能读取公开博客的关注关系摘要，不能读取当前用户社交列表。
- 不可用博客和用户统一按不存在处理，不通过关注接口泄漏内部状态。
- 关注列表只返回仍可公开展示的用户及博客。
- 关系表唯一约束、业务幂等处理和目标计数事务共同保证重复请求安全。
