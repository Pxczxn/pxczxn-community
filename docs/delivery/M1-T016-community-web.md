# M1-T016 星语社区博客端

## 状态

完成。

## 交付范围

博客端使用 vinext、React 19 和 TypeScript 独立实现，不复用 Mars Admin
管理端页面。品牌统一为“星语社区”，本地固定端口如下：

| 服务 | 地址 |
| --- | --- |
| 博客端 | `http://localhost:8847` |
| 管理端 | `http://localhost:8848` |
| 后端 | `http://127.0.0.1:8849` |

## 原型页面

按用户提供的八组桌面端原型和
`blog-community-engineering-docs-v0.3/pxczxn-ui-design-system.md`
完成以下页面：

- `/login`：登录、注册、会话反馈和原型浏览入口。
- `/teams/ai-explorers`：团队博客封面、资料、内容流与侧栏。
- `/workspace/team`：团队工作台、统计、待办、成员角色和趋势。
- `/submissions/ai-agent`：投稿详情、审核时间线与操作反馈。
- `/collaboration/articles/agent-patterns`：共创成员、版本、规则和讨论。
- `/moments/agent-architecture`：动态发布、动态详情和评论。
- `/me/favorites`：个人资料、收藏夹、喜欢、关注与粉丝。
- `/settings`：账号、安全、通知以及浅色、深色、星空主题设置。

同时补充 M1 真实业务路由：

- `/editor/new`、`/editor/:articleId`
- `/blogs/:slug`
- `/articles/:articleId`

登录页与团队主页使用为本项目生成的山景视觉资源：

- `pxczxn-web/public/images/login-mountain.png`
- `pxczxn-web/public/images/team-mountain-cover.png`
- `pxczxn-web/public/og.png`

## 真实业务闭环

博客端接入 `/api/v1/**`，使用独立请求头
`pxczxn-community-token`，完成：

```text
注册并自动登录
→ 自动创建个人博客
→ 新建并保存文章版本
→ 提交自动审核
→ 审核通过
→ 作者手动发布
→ 公开文章详情
→ 公开博客文章列表
```

文章正文只展示后端返回的安全 HTML；编辑源、未发布版本和审核内部字段不会进入
公开详情。设置页把浅色、深色和星空主题写回个人博客设置，并在浏览器中持久化。

团队、共创、动态、收藏和关注属于产品路线图 M2/M3。本任务先按原型提供完整页面
与交互演示数据，真实持久化接口将在对应阶段接入，不将演示数据冒充后端数据。

## 验证

```text
vinext production build: PASS / 12 routes
ESLint: PASS / 0 issues
Rendered HTML tests: 3 passed
Eight prototype routes: browser PASS
Light / dark / starry themes: PASS
Final local ports: 8847 / 8848 / 8849
CORS from http://localhost:8847: PASS
Backend health on 8849: UP
```

真实浏览器验收使用一次性账号完成注册、创作、审核、发布、公开访问和投稿状态查询，
随后按外键顺序清理用户、博客、文章、版本、审核任务和登录账号，相关临时记录剩余
数量均为 0。

## 线上预览

Sites 私有生产版本：

- 站点：`星语社区`
- 版本：`3`
- 源提交：`a4cd707fd6e91776511f171cfb1f37afc45e4509`
- 地址：<https://xingyu-community-pxczxn.pxczxn.chatgpt.site>

线上站点保持所有原型页面可浏览，但没有把本地 `127.0.0.1:8849` 暴露为生产 API。
未配置生产 API 时会给出明确提示；完整业务闭环在本地三个固定端口运行。
