SET NAMES utf8mb4;

-- 团队门户设置列就绪(分类/内容方向/主题/SEO/公开成员/开放投稿/投稿说明/联系方式)
SELECT CASE WHEN COUNT(*) = 9 THEN 1 ELSE 0 END AS team_portal_settings_columns_ready
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'team'
  AND column_name IN (
    'category', 'content_direction', 'theme', 'seo_title', 'seo_description',
    'public_members', 'allow_submissions', 'submission_guideline', 'contact_info'
  );

-- 默认值:公开成员列表=1、开放外部投稿=1
SELECT CASE WHEN EXISTS (
  SELECT 1 FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'team'
    AND column_name = 'public_members' AND column_default = '1'
) THEN 1 ELSE 0 END AS public_members_default_ready;

SELECT CASE WHEN EXISTS (
  SELECT 1 FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'team'
    AND column_name = 'allow_submissions' AND column_default = '1'
) THEN 1 ELSE 0 END AS allow_submissions_default_ready;
