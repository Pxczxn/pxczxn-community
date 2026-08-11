SET NAMES utf8mb4;

-- 将系列从「团队专属」泛化为「博客归属」：team_series -> series, team_id -> blog_id。
-- 现有团队系列数据通过 team.blog_id 回填到 series.blog_id，个人系列无团队归属。

-- 1. team_series 增加 blog_id 并从 team.blog_id 回填
ALTER TABLE `team_series` ADD COLUMN `blog_id` BIGINT UNSIGNED NULL AFTER `team_id`;
UPDATE `team_series` ts JOIN `team` t ON ts.`team_id` = t.`id` SET ts.`blog_id` = t.`blog_id`;
ALTER TABLE `team_series` MODIFY COLUMN `blog_id` BIGINT UNSIGNED NOT NULL;

-- 2. team_series_article 增加 blog_id 并从 team_series.blog_id 回填
ALTER TABLE `team_series_article` ADD COLUMN `blog_id` BIGINT UNSIGNED NULL AFTER `series_id`;
UPDATE `team_series_article` sa JOIN `team_series` ts ON sa.`series_id` = ts.`id` SET sa.`blog_id` = ts.`blog_id`;
ALTER TABLE `team_series_article` MODIFY COLUMN `blog_id` BIGINT UNSIGNED NOT NULL;
CREATE INDEX `idx_series_article_blog` ON `team_series_article` (`blog_id`);

-- 3. 重命名表
RENAME TABLE `team_series` TO `series`, `team_series_article` TO `series_article`;

-- 4. 删除旧的 team 外键与 team_id 列（series 只保留 blog_id 一个归属事实）
ALTER TABLE `series` DROP FOREIGN KEY `fk_team_series_team`;
ALTER TABLE `series` DROP COLUMN `team_id`;

-- 5. 新增 blog 外键
ALTER TABLE `series` ADD CONSTRAINT `fk_series_blog` FOREIGN KEY (`blog_id`) REFERENCES `blog` (`id`) ON DELETE RESTRICT;

-- 6. 重命名索引 / 唯一键（保留 idx_team_series_public_search 原名，V036 校验依赖它）
ALTER TABLE `series` RENAME INDEX `uk_team_series_team_slug` TO `uk_series_blog_slug`;
ALTER TABLE `series` RENAME INDEX `idx_team_series_public` TO `idx_series_public`;
ALTER TABLE `series_article` RENAME INDEX `uk_team_series_article_article` TO `uk_series_article_article`;
ALTER TABLE `series_article` RENAME INDEX `uk_team_series_article_order` TO `uk_series_article_order`;
ALTER TABLE `series_article` RENAME INDEX `idx_team_series_article_series` TO `idx_series_article_series`;

-- 7. 修正注释
ALTER TABLE `series` COMMENT = 'Blog article series and serialization state';
ALTER TABLE `series_article` COMMENT = 'Ordered article membership of a series';
ALTER TABLE `series` MODIFY COLUMN `blog_id` BIGINT UNSIGNED NOT NULL COMMENT 'Owning blog ID';
ALTER TABLE `series_article` MODIFY COLUMN `blog_id` BIGINT UNSIGNED NOT NULL COMMENT 'Owning blog ID copied from series';
