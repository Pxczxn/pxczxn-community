SET NAMES utf8mb4;

-- 团队门户设置:分类/内容方向/主题/SEO/公开规则/投稿规则
-- 与 team 表 1:1 扩展,复用乐观锁(lock_version)统一更新
-- 注:MySQL 8.0 不支持 ADD COLUMN IF NOT EXISTS,迁移按版本一次性应用
ALTER TABLE `team`
  ADD COLUMN `category` VARCHAR(50) NULL COMMENT '团队分类' AFTER `owner_user_id`,
  ADD COLUMN `content_direction` VARCHAR(500) NULL COMMENT '内容方向' AFTER `category`,
  ADD COLUMN `theme` VARCHAR(50) NULL COMMENT '主页主题' AFTER `content_direction`,
  ADD COLUMN `seo_title` VARCHAR(200) NULL COMMENT 'SEO 标题' AFTER `theme`,
  ADD COLUMN `seo_description` VARCHAR(500) NULL COMMENT 'SEO 描述' AFTER `seo_title`,
  ADD COLUMN `public_members` TINYINT NOT NULL DEFAULT 1 COMMENT '是否公开成员列表 1=公开 0=隐藏' AFTER `seo_description`,
  ADD COLUMN `allow_submissions` TINYINT NOT NULL DEFAULT 1 COMMENT '是否开放外部投稿 1=开放 0=仅成员' AFTER `public_members`,
  ADD COLUMN `submission_guideline` VARCHAR(2000) NULL COMMENT '投稿说明' AFTER `allow_submissions`,
  ADD COLUMN `contact_info` VARCHAR(500) NULL COMMENT '联系方式' AFTER `submission_guideline`;
