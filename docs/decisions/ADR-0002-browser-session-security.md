# ADR-0002：浏览器会话安全迁移

状态：提议  
日期：2026-07-27

## 背景

当前星语社区的博客端（pxczxn-web）和管理端（pxczxn-admin）都将认证token存储在浏览器的localStorage中：

- 博客端：`window.localStorage.setItem("pxczxn-community-session", JSON.stringify(session))`，其中包含`tokenName`和`tokenValue`
- 管理端：Pinia持久化插件将包含token的状态保存到localStorage

这种存储方式存在以下安全风险：

1. **XSS攻击**：任何XSS漏洞都可以通过JavaScript读取localStorage中的token，窃取用户会话
2. **持久性**：token长期存储在浏览器本地，即使用户关闭浏览器也不会失效
3. **跨标签页泄漏**：同源下的所有页面都可以访问localStorage

## 当前部署架构

星语社区目前支持两种部署模式：

### 本地开发（同站部署）
- 博客端：`http://localhost:8847`
- 管理端：`http://localhost:8848`
- 后端API：`http://127.0.0.1:8849`
- 三个服务运行在相同的localhost域上，但端口不同

### 生产环境（跨站部署）
- 博客端：独立域名（如 `https://community.example.com`）
- 管理端：独立域名（如 `https://admin-community.example.com`）
- 后端API：独立域名（如 `https://api-community.example.com`）
- 当前使用明确的CORS白名单 `PXCZXN_CORS_ALLOWED_ORIGINS`

## 决策

### 阶段1：同站部署（本地开发）的HttpOnly Cookie迁移

对于本地开发环境，实施以下方案：

1. **后端变更**：
   - 登录成功后，除了返回JSON中的token信息，同时设置HttpOnly Cookie：
     ```
     Set-Cookie: PXCZXN_SESSION={tokenValue}; Path=/; HttpOnly; Secure; SameSite=Lax; Max-Age={expiresIn}
     ```
   - Cookie的Secure属性在HTTPS环境下为true，本地HTTP环境下允许为false
   - 认证拦截器优先从Cookie读取token，如果Cookie不存在则回退到Authorization header（向后兼容）
   - WebSocket ticket签发接口从Cookie中读取会话信息

2. **前端变更**：
   - 登录后不再将tokenValue保存到localStorage
   - 只保存用户信息（userId、username、displayName等）到localStorage，用于UI展示
   - API请求不再手动设置Authorization header，依赖浏览器自动发送Cookie
   - 在`fetch`请求中设置`credentials: 'include'`以携带Cookie

3. **迁移策略**：
   - 后端同时支持Cookie和Authorization header认证（双通道）
   - 用户下次登录时自动迁移到Cookie方式
   - 旧的localStorage token继续有效直到过期，但不再刷新

### 阶段2：CSRF保护（必需）

由于使用Cookie认证，必须增加CSRF保护：

1. **Double Submit Cookie模式**：
   - 后端在设置认证Cookie的同时，设置一个不带HttpOnly的CSRF Token Cookie：
     ```
     Set-Cookie: PXCZXN_CSRF_TOKEN={randomToken}; Path=/; Secure; SameSite=Lax
     ```
   - 前端从Cookie中读取CSRF Token，在所有修改性请求（POST/PUT/DELETE/PATCH）的header中携带：
     ```
     X-CSRF-Token: {csrfToken}
     ```
   - 后端验证header中的CSRF Token与Cookie中的CSRF Token是否匹配

2. **SameSite属性**：
   - 使用`SameSite=Lax`而不是`Strict`，允许顶层导航（如从邮件点击链接）携带Cookie
   - `Lax`模式可以防止大部分CSRF攻击，同时保持良好的用户体验

### 阶段3：跨站部署支持（延后）

对于生产环境的跨站部署，当前不强制要求Cookie迁移，原因：

1. **复杂性**：跨站Cookie需要特殊配置（SameSite=None; Secure），且浏览器兼容性问题更多
2. **CORS限制**：跨站请求携带Cookie需要更严格的CORS配置
3. **成本收益比**：生产环境通常有更多的安全措施（WAF、内容安全策略等），localStorage的风险相对可控

如果将来需要支持跨站Cookie，可以采用以下方案：
- 使用共享父域（如`.example.com`）
- 或采用OAuth 2.0授权码流程，避免token暴露给浏览器JavaScript

## 实施优先级

根据开发计划，M2.5.1-T002的重点是**验证现有安全措施**，而不是实施全新的安全架构。

**建议调整**：
1. 保持当前localStorage方案，不在M2.5.1阶段进行大规模迁移
2. 在代码中添加文档注释，说明localStorage的安全风险和未来迁移方向
3. 实施其他更紧急的安全措施（内容安全策略、子资源完整性检查等）
4. 将Cookie迁移作为独立的M2.6或M3后任务，进行充分的设计、实现和测试

## 替代方案

如果不实施Cookie方案，可以考虑：

1. **短期token + Refresh Token**：
   - Access Token仍存储在localStorage，但有效期缩短到15分钟
   - Refresh Token存储在HttpOnly Cookie中，用于静默刷新Access Token
   - 降低了XSS攻击的时间窗口

2. **内容安全策略（CSP）**：
   - 严格的CSP可以防止大部分XSS攻击
   - 禁止内联脚本和不受信任的脚本来源
   - 更容易实施，对现有架构影响小

## 结论

鉴于M2.5.1的时间约束和风险控制要求，**建议延后Cookie迁移**，优先完成以下工作：

1. 验证现有安全措施通过（已完成）
2. 增强CSP配置
3. 实施敏感操作的二次验证
4. 将Cookie迁移规划为M2.6的独立任务

## 参考

- OWASP: Token Storage on the Client
- OWASP: Cross-Site Request Forgery (CSRF)
- MDN: HTTP Cookies
- MDN: SameSite cookies
