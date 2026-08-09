/* M5-T001: filter and ordering indexes used by the public unified MySQL search. */
SET NAMES utf8mb4;
CREATE INDEX `idx_article_public_search` ON `article` (`visibility`, `publish_status`, `deleted_at`, `published_at`);
CREATE INDEX `idx_moment_public_search` ON `community_moment` (`visibility`, `status`, `deleted_at`, `created_at`);
CREATE INDEX `idx_blog_public_search` ON `blog` (`status`, `deleted_at`, `updated_at`);
CREATE INDEX `idx_team_series_public_search` ON `team_series` (`review_status`, `deleted_at`, `published_at`);
CREATE INDEX `idx_community_user_public_search` ON `community_user` (`status`, `updated_at`);
