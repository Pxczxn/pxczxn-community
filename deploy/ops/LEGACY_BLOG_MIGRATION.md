# 旧版博客数据迁移

该迁移将服务器上已停用的 `pxczxn-blog` 内容导入新的 `pxczxn_community` 社区库。它不是常规 DDL 迁移：不新建表、不改表结构，只在一次数据库事务中写入历史内容。

## 迁移范围

- 社区用户与可继续使用的邮箱登录账号（保留原 bcrypt 密码哈希）；
- 个人博客、偏好、博客设置、默认收藏夹；
- 分类、标签、已发布文章及文章版本；
- 已发布动态、已通过的文章评论和动态评论；
- 原始作者、发布时间、阅读量、分类、标签与评论关系。

未迁移旧版封面文件、旧版点赞/收藏关系和未发布内容：它们在新模型中依赖不同的对象存储或交互审计语义，不能安全地直接复制。

## 安全执行

在服务器上执行前先完成数据库备份。脚本默认只进行事务演练，结束时回滚，不会修改任何数据：

```bash
cd /app/pxczxn
bash deploy/ops/migrate-legacy-blog-data.sh --dry-run
```

只有演练中的 `VERIFY_*` 计数全部合理、孤儿计数均为 `0` 时，才执行正式导入：

```bash
LEGACY_BLOG_MIGRATION_CONFIRM=1 \
  bash deploy/ops/migrate-legacy-blog-data.sh --commit
```

脚本仅接受 `pxczxn-blog -> pxczxn_community`，并要求目标社区内容表全为空。只要目标已经有内容，它会拒绝执行；这使其不可用于合并两个已投入使用的社区库。
