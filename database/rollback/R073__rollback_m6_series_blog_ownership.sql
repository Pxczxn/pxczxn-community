SET NAMES utf8mb4;

-- 反向迁移（尽力而为）：series -> team_series。
-- 注意：个人系列（无对应 team）回溯后 team_id 将为 NULL；团队系列通过 blog -> team 还原。
-- 仅在需要回滚 V073 时使用，正常情况下不应执行。

-- 1. 还原表名
RENAME TABLE `series` TO `team_series`, `series_article` TO `team_series_article`;

-- 2. series 还原 team_id 并从 blog -> team 回填
ALTER TABLE `team_series` ADD COLUMN `team_id` BIGINT UNSIGNED NULL AFTER `id`;
UPDATE `team_series` ts JOIN `blog` b ON ts.`blog_id` = b.`id` JOIN `team` t ON t.`blog_id` = b.`id` SET ts.`team_id` = t.`id`;
ALTER TABLE `team_series` DROP FOREIGN KEY `fk_series_blog`;
ALTER TABLE `team_series` DROP COLUMN `blog_id`;
ALTER TABLE `team_series` ADD CONSTRAINT `fk_team_series_team` FOREIGN KEY (`team_id`) REFERENCES `team` (`id`) ON DELETE RESTRICT;

-- 3. series_article 还原 blog_id 列
ALTER TABLE `team_series_article` DROP COLUMN `blog_id`;

-- 4. 还原索引 / 唯一键名
ALTER TABLE `team_series` RENAME INDEX `uk_series_blog_slug` TO `uk_team_series_team_slug`;
ALTER TABLE `team_series` RENAME INDEX `idx_series_public` TO `idx_team_series_public`;
ALTER TABLE `team_series_article` RENAME INDEX `uk_series_article_article` TO `uk_team_series_article_article`;
ALTER TABLE `team_series_article` RENAME INDEX `uk_series_article_order` TO `uk_team_series_article_order`;
ALTER TABLE `team_series_article` RENAME INDEX `idx_series_article_series` TO `idx_team_series_article_series`;

-- 5. 还原注释
ALTER TABLE `team_series` COMMENT = 'Team article series and serialization state';
ALTER TABLE `team_series_article` COMMENT = 'Team series article sorting relation';
