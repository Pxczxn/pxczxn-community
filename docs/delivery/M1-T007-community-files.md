# M1-T007 社区文件引用能力交付说明

## API

```http
POST   /api/v1/files
GET    /api/v1/files/{fileId}
GET    /api/v1/files/{fileId}/content
DELETE /api/v1/files/{fileId}

GET    /api/v1/public/files/{fileId}/content
```

私有文件接口只读取 `pxczxn-community-token`，不复用管理员身份。文件上传使用
`multipart/form-data` 的 `file` 字段。

## 存储复用与社区隔离

- 复用 Mars Admin 的 `FileStorageFactory` 和本地、MinIO、OSS 等存储策略。
- 社区文件单独写入 `file_object`，不写管理员 `sys_file`。
- `created_by_user_id` 记录社区所有者，所有查看、引用和删除都重新校验
  当前社区用户。
- 对象键使用 `community/{userId}/{UTC date}/{UUID}.{safe extension}`，
  不使用客户端文件名作为物理路径。
- 业务只保存 `file_id`，响应中的内容地址由社区 API 生成。

## 安全检查

- 单文件最大 20MB，并在读取请求体前后各校验一次。
- 不信任客户端 MIME；使用文件签名、容器结构和 UTF-8 校验识别真实类型。
- V1 接受 JPG/JPEG、PNG、GIF、WebP、PDF、DOCX、TXT、Markdown 和安全
  SVG。
- SVG 禁止脚本、事件属性、外部引用、DOCTYPE/ENTITY、内嵌样式和
  `foreignObject` 等高风险能力。
- 拒绝扩展名与真实类型不一致的文件；不接受平台原生视频。
- 文件名移除客户端路径，元数据保存 SHA-256。

## 引用与删除

- `community_file_reference` 记录文件、所有者、目标和用途。
- 博客头像与背景更新会在同一数据库事务内校验文件所有权并替换引用。
- 其他社区用户不能引用文件 ID。
- 仍有有效引用时返回 409，禁止删除。
- 删除只把文件标记为 `DELETED`，物理对象进入延迟清理流程。
- 公开文件读取当前只允许活动的博客头像和博客背景引用，未引用上传文件
  不可公开访问。

## 验证

```text
Community business automated tests: 24 passed
Community API automated tests: 7 passed
T007-specific automated tests: 7 passed
Real PNG upload with falsely claimed video MIME: detected image/png
SHA-256 metadata: 64 hexadecimal characters
Public read before business reference: 404
Blog avatar reference and public binary read: 200 / image/png
Cross-user file reference: 403
Delete while referenced: 409
Clear reference then logical delete: 200
Public read after logical delete: 404
Redis: intentionally unavailable
Temporary database and storage objects: removed
```
