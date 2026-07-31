#!/usr/bin/env bash
# Imports the retired pxczxn-blog content into an empty pxczxn_community database.
#
# Safety contract:
#   * dry-run is the default and always rolls the transaction back;
#   * --commit requires LEGACY_BLOG_MIGRATION_CONFIRM=1;
#   * the target content tables must all be empty;
#   * the source and target schema names are intentionally fixed to avoid
#     accidentally importing from or overwriting an unrelated database.

set -euo pipefail

MODE="dry-run"
case "${1:-}" in
  ""|--dry-run) ;;
  --commit) MODE="commit" ;;
  *) echo "Usage: $0 [--dry-run|--commit]" >&2; exit 64 ;;
esac

SOURCE_DATABASE="${SOURCE_DATABASE:-pxczxn-blog}"
TARGET_DATABASE="${TARGET_DATABASE:-pxczxn_community}"
MYSQL_BIN="${MYSQL_BIN:-mysql}"
MYSQL_HOST="${MYSQL_HOST:-}"
MYSQL_PORT="${MYSQL_PORT:-}"
MYSQL_USER="${MYSQL_USER:-root}"

if [[ "$SOURCE_DATABASE" != "pxczxn-blog" || "$TARGET_DATABASE" != "pxczxn_community" ]]; then
  echo "Refusing unexpected source/target schemas: $SOURCE_DATABASE -> $TARGET_DATABASE" >&2
  exit 64
fi

if [[ "$MODE" == "commit" && "${LEGACY_BLOG_MIGRATION_CONFIRM:-}" != "1" ]]; then
  echo "Set LEGACY_BLOG_MIGRATION_CONFIRM=1 before using --commit." >&2
  exit 64
fi

mysql_args=(--default-character-set=utf8mb4 --batch --raw --skip-column-names "--user=$MYSQL_USER" "--database=$TARGET_DATABASE")
[[ -n "$MYSQL_HOST" ]] && mysql_args+=("--host=$MYSQL_HOST")
[[ -n "$MYSQL_PORT" ]] && mysql_args+=("--port=$MYSQL_PORT")
mysql_import_args=("${mysql_args[@]}" "--init-command=SET @legacy_import_commit = $([[ "$MODE" == "commit" ]] && echo 1 || echo 0)")

mysql_query() {
  "$MYSQL_BIN" "${mysql_args[@]}" --execute "$1"
}

scalar() {
  local value
  value="$(mysql_query "$1")"
  printf '%s' "$value"
}

require_zero() {
  local label="$1"
  local value="$2"
  if [[ "$value" != "0" ]]; then
    echo "Preflight failed: $label = $value (expected 0)." >&2
    exit 1
  fi
}

echo "[INFO] Legacy import $MODE: $SOURCE_DATABASE -> $TARGET_DATABASE"

source_users="$(scalar "SELECT COUNT(*) FROM \`pxczxn-blog\`.community_user")"
source_articles="$(scalar "SELECT COUNT(*) FROM \`pxczxn-blog\`.article WHERE status='PUBLISHED'")"
source_moments="$(scalar "SELECT COUNT(*) FROM \`pxczxn-blog\`.community_post WHERE status='PUBLISHED'")"
if [[ "$source_users" == "0" || ( "$source_articles" == "0" && "$source_moments" == "0" ) ]]; then
  echo "Preflight failed: the retired blog source has no importable content." >&2
  exit 1
fi

target_occupied="$(scalar "SELECT (SELECT COUNT(*) FROM community_user) + (SELECT COUNT(*) FROM community_user_login_account) + (SELECT COUNT(*) FROM community_user_preference) + (SELECT COUNT(*) FROM favorite_folder) + (SELECT COUNT(*) FROM blog) + (SELECT COUNT(*) FROM blog_setting) + (SELECT COUNT(*) FROM blog_category) + (SELECT COUNT(*) FROM article) + (SELECT COUNT(*) FROM article_version) + (SELECT COUNT(*) FROM platform_tag) + (SELECT COUNT(*) FROM article_tag) + (SELECT COUNT(*) FROM community_moment) + (SELECT COUNT(*) FROM community_comment)")"
require_zero "target content rows" "$target_occupied"

require_zero "published article with missing author" "$(scalar "SELECT COUNT(*) FROM \`pxczxn-blog\`.article a LEFT JOIN \`pxczxn-blog\`.community_user u ON u.id=a.community_author_id WHERE a.status='PUBLISHED' AND u.id IS NULL")"
require_zero "published moment with missing author" "$(scalar "SELECT COUNT(*) FROM \`pxczxn-blog\`.community_post p LEFT JOIN \`pxczxn-blog\`.community_user u ON u.id=p.author_id WHERE p.status='PUBLISHED' AND u.id IS NULL")"
require_zero "approved article comment with missing author or article" "$(scalar "SELECT COUNT(*) FROM \`pxczxn-blog\`.comment c LEFT JOIN \`pxczxn-blog\`.community_user u ON u.id=c.community_user_id LEFT JOIN \`pxczxn-blog\`.article a ON a.id=c.article_id AND a.status='PUBLISHED' WHERE c.status='APPROVED' AND (u.id IS NULL OR a.id IS NULL)")"
require_zero "approved moment comment with missing author or moment" "$(scalar "SELECT COUNT(*) FROM \`pxczxn-blog\`.community_post_comment c LEFT JOIN \`pxczxn-blog\`.community_user u ON u.id=c.community_user_id LEFT JOIN \`pxczxn-blog\`.community_post p ON p.id=c.post_id AND p.status='PUBLISHED' WHERE c.status='APPROVED' AND (u.id IS NULL OR p.id IS NULL)")"
require_zero "legacy markdown exceeding target moment limit" "$(scalar "SELECT COUNT(*) FROM \`pxczxn-blog\`.community_post WHERE status='PUBLISHED' AND CHAR_LENGTH(CONCAT('## ', title, CHAR(10), CHAR(10), content)) > 4000")"
require_zero "legacy article comment nesting beyond one reply" "$(scalar "SELECT COUNT(*) FROM \`pxczxn-blog\`.comment c JOIN \`pxczxn-blog\`.comment parent ON parent.id=c.parent_id WHERE parent.parent_id IS NOT NULL AND c.status='APPROVED'")"
require_zero "legacy moment comment nesting beyond one reply" "$(scalar "SELECT COUNT(*) FROM \`pxczxn-blog\`.community_post_comment c JOIN \`pxczxn-blog\`.community_post_comment parent ON parent.id=c.parent_id WHERE parent.parent_id IS NOT NULL AND c.status='APPROVED'")"
require_zero "duplicate legacy email accounts" "$(scalar "SELECT COUNT(*) FROM (SELECT LOWER(TRIM(email)) identifier FROM \`pxczxn-blog\`.community_user WHERE NULLIF(TRIM(email),'') IS NOT NULL GROUP BY LOWER(TRIM(email)) HAVING COUNT(*)>1) duplicates")"
require_zero "duplicate legacy tag names" "$(scalar "SELECT COUNT(*) FROM (SELECT name FROM \`pxczxn-blog\`.tag GROUP BY name HAVING COUNT(*)>1) duplicates")"
require_zero "duplicate legacy tag slugs" "$(scalar "SELECT COUNT(*) FROM (SELECT slug FROM \`pxczxn-blog\`.tag GROUP BY slug HAVING COUNT(*)>1) duplicates")"

echo "[INFO] Source rows: users=$source_users published_articles=$source_articles published_moments=$source_moments"

"$MYSQL_BIN" "${mysql_import_args[@]}" <<'SQL'
START TRANSACTION;

INSERT INTO community_user (
  id, username, display_name, bio, status, verification_status, last_login_at, created_at, updated_at
)
SELECT
  u.id,
  CONCAT('legacy-user-', u.id),
  COALESCE(NULLIF(TRIM(u.display_name), ''), NULLIF(TRIM(u.username), ''), CONCAT('旧版用户 ', u.id)),
  NULLIF(TRIM(u.bio), ''),
  CASE WHEN u.status = 'ACTIVE' THEN 'NORMAL' ELSE 'BANNED' END,
  'UNVERIFIED',
  u.last_login_at,
  u.created_at,
  u.updated_at
FROM `pxczxn-blog`.community_user u;

INSERT INTO community_user_login_account (
  id, user_id, login_type, normalized_identifier, password_hash, verified_at, last_login_at, created_at, updated_at
)
SELECT
  910000000 + u.id,
  u.id,
  'EMAIL',
  LOWER(TRIM(u.email)),
  u.password_hash,
  CASE WHEN u.status = 'ACTIVE' THEN u.created_at ELSE NULL END,
  u.last_login_at,
  u.created_at,
  u.updated_at
FROM `pxczxn-blog`.community_user u
WHERE NULLIF(TRIM(u.email), '') IS NOT NULL;

INSERT INTO community_user_preference (id, user_id, locale, time_zone, email_notification_enabled, content_language, likes_visibility, created_at, updated_at)
SELECT 920000000 + u.id, u.id, 'zh-CN', 'Asia/Shanghai', 1, 'zh-CN', 'PRIVATE', u.created_at, u.updated_at
FROM `pxczxn-blog`.community_user u;

INSERT INTO favorite_folder (id, owner_user_id, name, description, visibility, is_default, item_count, sort_order, created_at, updated_at)
SELECT 930000000 + u.id, u.id, '全部收藏', '迁移自旧版博客的默认收藏夹', 'PRIVATE', 1, 0, 0, u.created_at, u.updated_at
FROM `pxczxn-blog`.community_user u;

INSERT INTO blog (id, blog_type, owner_user_id, name, slug, summary, status, article_count, follower_count, lock_version, created_at, updated_at)
SELECT
  900000000 + u.id,
  'PERSONAL',
  u.id,
  CONCAT(COALESCE(NULLIF(TRIM(u.display_name), ''), NULLIF(TRIM(u.username), ''), CONCAT('旧版用户 ', u.id)), ' 的博客'),
  CONCAT('legacy-user-', u.id),
  NULLIF(TRIM(u.bio), ''),
  'ACTIVE',
  0,
  0,
  0,
  u.created_at,
  u.updated_at
FROM `pxczxn-blog`.community_user u;

INSERT INTO blog_setting (id, blog_id, comment_scope, default_visibility, allow_repost, theme_key, created_at, updated_at)
SELECT 940000000 + u.id, 900000000 + u.id, 'ALL_LOGGED_IN', 'PUBLIC', 'ALLOW', 'default', u.created_at, u.updated_at
FROM `pxczxn-blog`.community_user u;

UPDATE community_user u
SET personal_blog_id = 900000000 + u.id;

INSERT INTO blog_category (id, blog_id, name, slug, description, sort_order, is_default, article_count, created_at, updated_at)
SELECT
  950000000 + u.id * 1000 + c.id,
  900000000 + u.id,
  c.name,
  c.slug,
  NULL,
  c.id,
  CASE WHEN c.id = (SELECT MIN(id) FROM `pxczxn-blog`.category) THEN 1 ELSE 0 END,
  0,
  c.created_at,
  c.updated_at
FROM `pxczxn-blog`.community_user u
CROSS JOIN `pxczxn-blog`.category c;

INSERT INTO platform_tag (id, name, slug, description, status, usage_count, created_at, updated_at)
SELECT t.id, t.name, t.slug, NULL, 'ACTIVE', 0, t.created_at, t.updated_at
FROM `pxczxn-blog`.tag t;

INSERT INTO article (
  id, blog_id, author_user_id, category_id, title, slug, summary, content_mode, visibility, publish_method,
  publish_status, review_status, view_count, like_count, favorite_count, comment_count, lock_version,
  published_at, canonical_path, created_at, updated_at
)
SELECT
  a.id,
  900000000 + a.community_author_id,
  a.community_author_id,
  950000000 + a.community_author_id * 1000 + a.category_id,
  a.title,
  a.slug,
  NULLIF(TRIM(a.summary), ''),
  'MARKDOWN',
  'PUBLIC',
  'MANUAL',
  'PUBLISHED',
  'APPROVED',
  GREATEST(a.view_count, 0),
  0,
  0,
  0,
  0,
  a.published_at,
  CONCAT('/articles/', a.id),
  a.created_at,
  a.updated_at
FROM `pxczxn-blog`.article a
WHERE a.status = 'PUBLISHED';

INSERT INTO article_version (
  id, article_id, version_no, content_mode, markdown_content, rendered_html, plain_text, toc_json,
  content_hash, word_count, reading_time_minutes, created_by_user_id, creation_type, created_at
)
SELECT
  960000000 + a.id,
  a.id,
  1,
  'MARKDOWN',
  a.content,
  CONCAT('<p>', REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(a.content, '&', '&amp;'), '<', '&lt;'), '>', '&gt;'), CHAR(13), ''), CHAR(10), '<br>'), '</p>'),
  a.content,
  JSON_ARRAY(),
  SHA2(CONCAT('MARKDOWN', CHAR(10), a.content), 256),
  GREATEST(1, CHAR_LENGTH(a.content)),
  GREATEST(1, CEILING(CHAR_LENGTH(a.content) / 300)),
  a.community_author_id,
  'MANUAL_SAVE',
  a.created_at
FROM `pxczxn-blog`.article a
WHERE a.status = 'PUBLISHED';

UPDATE article target
JOIN `pxczxn-blog`.article source ON source.id = target.id
SET target.current_version_id = 960000000 + target.id,
    target.published_version_id = 960000000 + target.id,
    target.review_version_id = 960000000 + target.id,
    target.updated_at = source.updated_at;

INSERT INTO article_tag (article_id, tag_id, sort_order, created_at)
SELECT at.article_id, at.tag_id, 0, a.created_at
FROM `pxczxn-blog`.article_tag at
JOIN `pxczxn-blog`.article a ON a.id = at.article_id
WHERE a.status = 'PUBLISHED';

INSERT INTO community_moment (
  id, actor_user_id, blog_id, moment_type, text_content, rendered_html, visibility, status,
  like_count, favorite_count, comment_count, repost_count, lock_version, created_at, updated_at
)
SELECT
  p.id,
  p.author_id,
  900000000 + p.author_id,
  'TEXT',
  CONCAT('## ', p.title, CHAR(10), CHAR(10), p.content),
  CONCAT('<p><strong>', REPLACE(REPLACE(REPLACE(p.title, '&', '&amp;'), '<', '&lt;'), '>', '&gt;'), '</strong></p><p>', REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(p.content, '&', '&amp;'), '<', '&lt;'), '>', '&gt;'), CHAR(13), ''), CHAR(10), '<br>'), '</p>'),
  'PUBLIC',
  'PUBLISHED',
  0,
  0,
  0,
  0,
  0,
  COALESCE(p.published_at, p.created_at),
  p.updated_at
FROM `pxczxn-blog`.community_post p
WHERE p.status = 'PUBLISHED';

INSERT INTO community_comment (
  id, author_user_id, target_type, target_id, content_text, rendered_html, status, like_count,
  lock_version, created_at, updated_at
)
SELECT
  970000000 + c.id,
  c.community_user_id,
  'ARTICLE',
  c.article_id,
  c.content,
  CONCAT('<p>', REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(c.content, '&', '&amp;'), '<', '&lt;'), '>', '&gt;'), CHAR(13), ''), CHAR(10), '<br>'), '</p>'),
  'PUBLISHED',
  0,
  0,
  c.created_at,
  c.created_at
FROM `pxczxn-blog`.comment c
JOIN `pxczxn-blog`.article a ON a.id = c.article_id AND a.status = 'PUBLISHED'
WHERE c.status = 'APPROVED';

UPDATE community_comment target
JOIN `pxczxn-blog`.comment source ON target.id = 970000000 + source.id
LEFT JOIN `pxczxn-blog`.comment parent ON parent.id = source.parent_id
SET target.parent_comment_id = CASE WHEN source.parent_id IS NULL THEN NULL ELSE 970000000 + source.parent_id END,
    target.root_comment_id = CASE WHEN source.parent_id IS NULL THEN NULL ELSE 970000000 + source.parent_id END,
    target.reply_to_user_id = parent.community_user_id;

INSERT INTO community_comment (
  id, author_user_id, target_type, target_id, content_text, rendered_html, status, like_count,
  lock_version, created_at, updated_at
)
SELECT
  980000000 + c.id,
  c.community_user_id,
  'MOMENT',
  c.post_id,
  c.content,
  CONCAT('<p>', REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(c.content, '&', '&amp;'), '<', '&lt;'), '>', '&gt;'), CHAR(13), ''), CHAR(10), '<br>'), '</p>'),
  'PUBLISHED',
  0,
  0,
  c.created_at,
  c.updated_at
FROM `pxczxn-blog`.community_post_comment c
JOIN `pxczxn-blog`.community_post p ON p.id = c.post_id AND p.status = 'PUBLISHED'
WHERE c.status = 'APPROVED';

UPDATE community_comment target
JOIN `pxczxn-blog`.community_post_comment source ON target.id = 980000000 + source.id
LEFT JOIN `pxczxn-blog`.community_post_comment parent ON parent.id = source.parent_id
SET target.parent_comment_id = CASE WHEN source.parent_id IS NULL THEN NULL ELSE 980000000 + source.parent_id END,
    target.root_comment_id = CASE WHEN source.parent_id IS NULL THEN NULL ELSE 980000000 + source.parent_id END,
    target.reply_to_user_id = parent.community_user_id;

UPDATE article a
SET comment_count = (
  SELECT COUNT(*) FROM community_comment c
  WHERE c.target_type = 'ARTICLE' AND c.target_id = a.id AND c.status = 'PUBLISHED' AND c.deleted_at IS NULL
),
    updated_at = a.updated_at;

UPDATE community_moment m
SET comment_count = (
  SELECT COUNT(*) FROM community_comment c
  WHERE c.target_type = 'MOMENT' AND c.target_id = m.id AND c.status = 'PUBLISHED' AND c.deleted_at IS NULL
),
    updated_at = m.updated_at;

UPDATE blog b
SET article_count = (
  SELECT COUNT(*) FROM article a WHERE a.blog_id = b.id AND a.deleted_at IS NULL
);

UPDATE blog_category c
SET article_count = (
  SELECT COUNT(*) FROM article a WHERE a.category_id = c.id AND a.deleted_at IS NULL
);

UPDATE platform_tag t
SET usage_count = (
  SELECT COUNT(*) FROM article_tag at WHERE at.tag_id = t.id
);

-- MySQL's automatic update timestamps would otherwise replace historical
-- modification times while the relationship counters above are rebuilt.
UPDATE community_user target
JOIN `pxczxn-blog`.community_user source ON source.id = target.id
SET target.updated_at = source.updated_at;

UPDATE blog target
JOIN `pxczxn-blog`.community_user source ON source.id = target.owner_user_id
SET target.updated_at = source.updated_at;

UPDATE blog_category target
JOIN `pxczxn-blog`.community_user user_source ON target.blog_id = 900000000 + user_source.id
JOIN `pxczxn-blog`.category category_source ON target.id = 950000000 + user_source.id * 1000 + category_source.id
SET target.updated_at = category_source.updated_at;

UPDATE platform_tag target
JOIN `pxczxn-blog`.tag source ON source.id = target.id
SET target.updated_at = source.updated_at;

UPDATE article target
JOIN `pxczxn-blog`.article source ON source.id = target.id
SET target.updated_at = source.updated_at;

UPDATE community_moment target
JOIN `pxczxn-blog`.community_post source ON source.id = target.id
SET target.updated_at = source.updated_at;

UPDATE community_comment target
JOIN `pxczxn-blog`.comment source ON target.id = 970000000 + source.id
SET target.updated_at = source.created_at;

UPDATE community_comment target
JOIN `pxczxn-blog`.community_post_comment source ON target.id = 980000000 + source.id
SET target.updated_at = source.updated_at;

SELECT 'VERIFY_IMPORTED_USERS', COUNT(*) FROM community_user;
SELECT 'VERIFY_IMPORTED_ARTICLES', COUNT(*) FROM article WHERE publish_status = 'PUBLISHED' AND published_version_id IS NOT NULL;
SELECT 'VERIFY_IMPORTED_MOMENTS', COUNT(*) FROM community_moment WHERE status = 'PUBLISHED';
SELECT 'VERIFY_ORPHAN_ARTICLE_VERSION', COUNT(*) FROM article a LEFT JOIN article_version v ON v.id = a.published_version_id WHERE a.publish_status = 'PUBLISHED' AND v.id IS NULL;
SELECT 'VERIFY_ORPHAN_COMMENT_TARGET', COUNT(*) FROM community_comment c LEFT JOIN article a ON c.target_type = 'ARTICLE' AND a.id = c.target_id LEFT JOIN community_moment m ON c.target_type = 'MOMENT' AND m.id = c.target_id WHERE (c.target_type = 'ARTICLE' AND a.id IS NULL) OR (c.target_type = 'MOMENT' AND m.id IS NULL);

SET @legacy_import_finish = IF(@legacy_import_commit = 1, 'COMMIT', 'ROLLBACK');
PREPARE legacy_import_finish FROM @legacy_import_finish;
EXECUTE legacy_import_finish;
DEALLOCATE PREPARE legacy_import_finish;
SQL

if [[ "$MODE" == "commit" ]]; then
  echo "[PASS] Legacy blog data committed."
else
  echo "[PASS] Dry-run completed and rolled back. Review the VERIFY_* counters before committing."
fi
