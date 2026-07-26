# 星语社区博客端

用户侧前端根据 `blog-community-engineering-docs-v0.3/pxczxn-ui-design-system.md`
和用户提供的八组原型实现，独立于 Mars Admin 管理端。

## 页面

- `/login`：注册、登录与会话反馈
- `/teams/ai-explorers`：团队博客主页
- `/workspace/team`：团队工作台
- `/submissions/ai-agent`：投稿详情与审核状态
- `/collaboration/articles/agent-patterns`：共创文章协作
- `/moments/agent-architecture`：动态发布与详情
- `/me/favorites`：收藏、喜欢、关注和粉丝
- `/settings`：账号与浅色/深色/星空主题设置
- `/editor/new`、`/editor/:articleId`：真实文章编辑、保存、投稿和发布
- `/blogs/:slug`、`/articles/:articleId`：真实公开博客与安全文章详情

## 本地运行

```powershell
npm.cmd install
npm.cmd run dev
```

博客端固定监听 `http://localhost:8847`，默认访问
`http://127.0.0.1:8849` 的社区 API。其他环境可复制 `.env.example` 后设置：

```text
NEXT_PUBLIC_COMMUNITY_API_BASE_URL=https://api.example.com
```

## 验证

```powershell
npm.cmd run lint
npm.cmd test
```

`npm test` 会执行生产构建、全部原型路由服务端渲染检查，以及社区 API、
会话、编辑、审核、发布和三色主题的源级契约检查。

## 私有生产预览

<https://xingyu-community-pxczxn.pxczxn.chatgpt.site>

预览环境没有把本地 `127.0.0.1:8849` 当作生产 API；未配置线上社区 API 时
会显示明确提示。本地三个固定端口提供完整业务闭环。
