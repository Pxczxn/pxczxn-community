SET NAMES utf8mb4;

SELECT COUNT(DISTINCT INDEX_NAME) AS `moment_public_feed_indexes`
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'community_moment'
  AND INDEX_NAME = 'idx_community_moment_public_feed';

SELECT COUNT(*) AS `moment_publication_check_constraints`
FROM information_schema.TABLE_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE()
  AND TABLE_NAME = 'community_moment'
  AND CONSTRAINT_TYPE = 'CHECK'
  AND CONSTRAINT_NAME IN (
      'chk_community_moment_link_required',
      'chk_community_moment_quote_text',
      'chk_community_moment_repost_text'
  );

SELECT COUNT(*) AS `invalid_moment_reference_rows`
FROM community_moment
WHERE (`article_id` IS NOT NULL AND `repost_moment_id` IS NOT NULL)
   OR (`article_id` IS NOT NULL AND `moment_type` <> 'ARTICLE_SHARE')
   OR (
        `repost_moment_id` IS NOT NULL
        AND `moment_type` NOT IN ('REPOST', 'QUOTE')
   )
   OR (`moment_type` = 'ARTICLE_SHARE' AND `article_id` IS NULL)
   OR (
        `moment_type` IN ('REPOST', 'QUOTE')
        AND `repost_moment_id` IS NULL
   )
   OR (
        `moment_type` IN ('LINK', 'VIDEO_LINK')
        AND (`link_url` IS NULL OR CHAR_LENGTH(TRIM(`link_url`)) = 0)
   )
   OR (
        `moment_type` = 'QUOTE'
        AND (
            `text_content` IS NULL
            OR CHAR_LENGTH(TRIM(`text_content`)) = 0
        )
   )
   OR (
        `moment_type` = 'REPOST'
        AND `text_content` IS NOT NULL
        AND CHAR_LENGTH(TRIM(`text_content`)) > 0
   );
