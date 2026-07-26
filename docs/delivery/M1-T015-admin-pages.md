# M1-T015 管理端页面与三色主题交付记录

## 交付范围

- 保留 Mars Admin 的 Vue 3、Vite、TypeScript、Naive UI、RBAC、动态菜单和通用布局。
- 新增社区工作台、社区用户、博客、文章、文章审核和平台标签六个运营页面。
- 新增管理端社区查询 API、权限菜单迁移和浅色、深色、星空三套主题。
- 完成 `pxczxn` 品牌、登录页、站点标题、图标和社区运营文案替换。

## 后端与权限

- 新增管理查询服务，提供全局统计、近七日趋势、用户、博客、文章分页和文章详情。
- 管理接口统一位于 `/admin-api/community/**`，继续使用 Mars Admin 管理员会话与权限拦截。
- 文章详情只返回服务端清洗后的安全 HTML，不暴露编辑器源数据或未发布内容。
- V006 注册社区工作台、用户、博客和文章菜单及查询权限，并授权默认管理员角色。
- 所有 BIGINT 业务 ID 在 JSON 中继续使用字符串，避免前端精度损失。
- 默认平台标签列表排除软删除记录；显式状态筛选仍可供审计查询使用。

## 页面能力

- 社区工作台：真实总量、发布/审核健康度、近七日趋势和快捷入口。
- 社区用户：状态、认证、关键词筛选和分页。
- 博客管理：类型、状态、可见性、所有者和关键词筛选及分页。
- 文章管理：发布、审核、内容模式、博客和关键词筛选，支持安全正文详情抽屉。
- 文章审核：队列、详情、领取、通过、退修和驳回，完整携带乐观锁版本。
- 平台标签：列表、筛选、创建、编辑、隐藏和逻辑删除。
- 六个页面均包含加载、空数据、错误重试、权限按钮和响应式布局状态。

## 三色主题与品牌

- 浅色主题保持高密度白底运营界面。
- 深色主题使用 Naive UI 深色变量和统一卡片层级。
- 星空主题在深色基础上增加克制的深蓝、星点与星云层次，同时保持表格可读性。
- 主题设置面板提供浅色、深色、星空三个明确选项并持久化选择。
- 登录页、站点标题、菜单品牌、版权和 favicon 已统一为 `pxczxn 运营中心`。

## 构建与验收

```text
PlatformTagService regression tests: 4 passed
Mars Admin API automated tests: 8 passed
Admin production build: 4932 modules / PASS
Backend Maven package: 26 modules / SUCCESS
V006 menus / permissions / admin grants: 9 / 5 / 10, PASS
Admin login after final package: PASS
Community menu order and six routes: PASS
Light / dark / starry visual tokens: PASS
Community token calling admin dashboard: 401 / isolated
Unauthenticated admin dashboard: 401 / protected
Soft-deleted tag hidden from default list: PASS
Temporary verification tag: removed, remaining 0
Runtime health on 8849: UP
```

## 已知脚手架债务

- Mars Admin 原脚手架仍有一批旧页面的严格 TypeScript/noUnused 报错；社区新增页面已修正自身类型问题，Vite 生产构建正常。
- 原脚手架的少量大体积公共 chunk 和 Sass legacy API 会产生构建警告，留待最终性能与依赖治理阶段处理。
