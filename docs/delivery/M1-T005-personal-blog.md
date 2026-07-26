# M1-T005 个人博客资料交付说明

## API

```http
GET   /api/v1/blogs/me
PATCH /api/v1/blogs/me
PATCH /api/v1/blogs/me/settings
GET   /api/v1/public/blogs/{blogSlug}
```

前三个接口必须使用 `pxczxn-community-token` 请求头。公开博客详情不要求
登录。

## 资料与设置规则

- 当前用户只能读取和修改注册事务为自己创建的 `PERSONAL` 博客。
- 同时校验用户的 `personal_blog_id`、博客所有者和博客类型，避免伪造关系
  越权。
- `ACTIVE`、`HIDDEN` 博客允许本人编辑；冻结、关闭或删除状态禁止编辑。
- 博客公开 slug 在资料编辑阶段保持稳定，不能通过 PATCH 修改。
- 主题设置只接受 `light`、`dark`、`starry`。
- 评论范围、默认可见性与转载策略使用白名单校验。
- 头像与背景文件 ID 暂只校验为正数；文件归属和引用关系由 M1-T007
  统一校验。

注册规则允许用户名使用下划线，注册时又以用户名创建个人博客 slug，因此
公开博客地址同步支持下划线，避免注册成功后主页无法访问。

## 公开字段边界

公开响应只包含博客展示资料、公开计数和所有者的公开展示资料，不返回：

- 邮箱；
- 账号状态和认证状态；
- 登录信息；
- 个人博客关系字段；
- 博客设置和权限信息。

只有 `ACTIVE` 且未删除、所有者状态允许公开展示的博客可以从公开接口读取。
其他情况统一返回“博客不存在”，避免泄露隐藏资源的存在性。

## 验证

```text
Community business automated tests: 17 passed
Community API automated tests: 7 passed
T005-specific automated tests: 9 passed
Authenticated profile read/update: 200 / PASS
Theme update to starry: 200 / PASS
Public blog lookup with underscore slug: 200 / PASS
Public response private-field scan: no leaks
Slug change attempt: 400 / rejected
Unauthenticated private profile read: 401 / rejected
Redis: intentionally unavailable
Temporary verification data: removed
```
