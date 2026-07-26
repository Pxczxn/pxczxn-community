# M2-T008 博客端互动页面真实接入

## 状态

完成。

## 交付范围

- 保留原型图的双栏动态广场、个人资料卡、收藏文件夹、通知分类和蓝色轻量视觉，
  将演示数据替换为 8849 后端真实 API。
- 新增 `/moments`、`/moments/{momentId}` 路由，并保留
  `/moments/agent-architecture` 原型兼容入口。
- 动态页完成公开流、详情选择、文本/链接发布、公开范围、点赞、收藏、分享链接、
  评论、加载、空状态和错误反馈。
- `/me/favorites` 完成本人资料、文章/动态/收藏/关注/粉丝统计、收藏夹切换与创建、
  收藏内容、喜欢内容、关注、粉丝和互关列表。
- 新增 `/notifications` 通知中心，完成八类通知中的常用分类标签、分类未读数、
  单条已读、分类/全部已读、安全目标跳转和不可见目标脱敏文案。
- 顶部通知铃铛接入真实未读数；通知状态变更后通过页面事件立即同步徽标。
- 公开博客的关注按钮接入真实关注关系、粉丝数和登录引导。
- 文章详情的喜欢与收藏按钮接入真实关系及服务端计数，不再使用本地模拟增量。
- 前端统一 API 层增加动态、评论、点赞、收藏、收藏夹、关注关系、社交资料和通知
  的 TypeScript 类型与调用方法。
- 本地 `localhost`、回环地址和私有网络访问会自动推导同主机 8849 API；线上预览
  仍通过 `NEXT_PUBLIC_COMMUNITY_API_BASE_URL` 显式配置后端。
- 保持“星语社区”品牌、浅色/深色/星空三色主题、桌面原型网格和已有响应式降级。

## 路由

```text
/moments
/moments/{momentId}
/moments/agent-architecture
/me/favorites
/notifications
/blogs/{slug}
/articles/{articleId}
```

## 真实 API

```text
GET/POST       /api/v1/moments
GET            /api/v1/moments/{momentId}
GET            /api/v1/social/me/moments
GET/POST       /api/v1/interactions/{type}/{id}/comments
GET/POST/DELETE /api/v1/interactions/{type}/{id}/like
GET/POST/DELETE /api/v1/interactions/{type}/{id}/favorite

GET/POST       /api/v1/social/me/favorite-folders
GET            /api/v1/favorite-folders/{folderId}/items
GET            /api/v1/social/me/likes
GET            /api/v1/social/me/counts
GET            /api/v1/social/me/following
GET            /api/v1/social/me/followers
GET/POST/DELETE /api/v1/blogs/{blogId}/follow

GET            /api/v1/notifications
GET            /api/v1/notifications/unread-count
PATCH          /api/v1/notifications/{notificationId}/read
PATCH          /api/v1/notifications/read-all
```

## 自动化验证

```text
vinext production build: PASS
ESLint: PASS / 0 errors / 0 warnings
Rendered prototype and API wiring tests: 4 passed
Backend health: 8849 / UP
Blog frontend runtime: 8847
Admin frontend runtime: 8848
```

项目级 `tsc --noEmit` 仍会命中编辑器、投稿页和 Cloudflare Worker 声明中的既有
类型债务；本任务新增文件通过 ESLint 和 Vinext 生产构建，未增加新的诊断。

## 浏览器真实闭环

在 1280 × 720 浏览器视口完成以下真实操作：

```text
Register / login: PASS
Publish TEXT moment: PASS
Moment like / favorite / comment: 2 / 1 / 1
Favorite folder and saved item synchronized: PASS
Personal moment count: 1
Follower actor LIKE + FOLLOW notifications: 2 unread
Notification categories INTERACTION / FOLLOW: 1 / 1
Read all / final unread: 2 / 0
Blog follow loaded / unfollowed: true / true
Desktop grid: 606.66px + 485.34px, main width 1160px
Browser console errors: 0
Temporary users / moments / comments / notifications: 0 / 0 / 0 / 0
```

浏览器验收使用局域网地址访问 8847，并由前端自动推导同主机 8849；这同时验证了
非 `localhost` 本地联调场景。测试用户、动态、评论、收藏、关注和通知已按外键
顺序清理。

## 主要文件

- `pxczxn-web/app/lib/community-api.ts`
- `pxczxn-web/app/components/prototype-ui.tsx`
- `pxczxn-web/app/moments/moments-community-page.tsx`
- `pxczxn-web/app/me/favorites/social-center-page.tsx`
- `pxczxn-web/app/notifications/notifications-page.tsx`
- `pxczxn-web/app/blogs/[slug]/public-blog-page.tsx`
- `pxczxn-web/app/articles/[articleId]/article-detail-page.tsx`
- `pxczxn-web/app/globals.css`
- `pxczxn-web/tests/rendered-html.test.mjs`

## 安全与一致性结论

- 前端不缓存或推导业务计数，所有点赞、收藏、关注和未读数均采用后端响应。
- 401 会清理失效本地会话；受保护操作显示统一登录提示，公开动态仍支持匿名读取。
- 动态和评论正文只渲染后端净化后的 HTML。
- 通知只对 `targetAvailable` 且存在安全路径的目标生成可点击链接。
- BIGINT ID 全程保持字符串，避免浏览器数值精度损失。
